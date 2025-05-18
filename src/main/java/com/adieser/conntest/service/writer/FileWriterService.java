package com.adieser.conntest.service.writer;

import com.adieser.conntest.configurations.AppProperties;
import com.adieser.conntest.models.PingLog;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static com.adieser.conntest.models.CsvPingLogRepository.SAVE_PING_ERROR_MSG;

/**
 * Service to write pings to a file.
 */
@Slf4j
@Service
public class FileWriterService {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    public static final String ROTATED_FILENAME_FORMAT = "ping_%s_%d.log";

    private final Logger logger;
    protected BlockingQueue<PingLog> queue;
    BufferedWriter writer;
    Path filePath;
    int fileCount;
    protected volatile boolean running;
    private final ExecutorService threadPoolExecutor;
    private final AppProperties appProperties;
    Clock clock;
    private StatefulBeanToCsv<PingLog> sbc;

    public FileWriterService(Logger logger,
                             ExecutorService threadPoolExecutor,
                             AppProperties appProperties,
                             Clock clock) {
        this.logger = logger;
        this.threadPoolExecutor = threadPoolExecutor;
        this.appProperties = appProperties;
        this.clock = clock;
        this.queue = new LinkedBlockingQueue<>();
        this.running = true;
        this.fileCount = 1;

        setFilePathFromProperties();
    }

    /**
     * Method to add data to the queue.
     * @param data The data to be added to the queue.
     */
    public void submit(PingLog data) {
        if(!queue.offer(data))
            log.error("Failed to add data to the queue: {}", data);
    }

    /**
     * Method to write data to the file asynchronously.
     * It uses the ThreadPoolExecutor to execute the startWriting() method.
     */
    @PostConstruct
    public void init() throws IOException {
        createPingLogsDirectory();
        setWriter();
        threadPoolExecutor.execute(this::startWriting);
    }

    /**
     * Method to write data to the file asynchronously.
     * It checks the file size and rotates the file if necessary.
     * It also handles the InterruptedException and IOException.
     */
    protected void startWriting() {
        try {
            while (running) { // Controls the execution of the loop
                PingLog data = queue.take();
                checkFileSizeAndRotate();
                sbc.write(data);
                writer.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("File writing thread interrupted: {}", e.getMessage());
        } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            log.error(SAVE_PING_ERROR_MSG, e);
        }
    }

    /**
     * Method to check the file size and rotate the file if necessary.
     * It rotates the file if the file size is greater than the maximum file size.
     * It also handles the IOException.
     */
    protected void checkFileSizeAndRotate() throws IOException {
        try(final Stream<String> stream =
                    Files.lines(Path.of(appProperties.getPingLogsPath() + "/" + appProperties.getPinglogsFilename()))
        ) {
            if (stream.count() >= appProperties.getFileMaxSizeRows()) {
                writer.close();
                String timestamp = LocalDateTime.now(clock).format(FORMATTER);

                Path rotatedFile = Paths.get(filePath.getParent().toString() + "/" +
                        String.format(ROTATED_FILENAME_FORMAT, timestamp, fileCount++));
                Files.move(filePath, rotatedFile, StandardCopyOption.REPLACE_EXISTING);

                setWriter();

                log.info("Rotated file: {}", rotatedFile);
            }
        }
    }

    /**
     * Method to clean up resources before bean destruction.
     * It closes the file and stops the write loop.
     */
    @PreDestroy
    public void cleanUp() {
        running = false; // Stops the loop in startWriting()
        if (writer != null) {
            try {
                log.info("Closing file before bean destruction...");
                writer.close();
            } catch (IOException e) {
                log.error("Error closing the file: {}", e.getMessage());
            }
        }
    }

    /**
     * Method to get a CSVWriter instance.
     * @param writer The writer to be used by the CSVWriter.
     * @return The CSVWriter instance.
     */
    CSVWriter getCsvWriter(Writer writer) {
        return new CSVWriter(writer,
                ICSVWriter.DEFAULT_SEPARATOR,
                ICSVWriter.NO_QUOTE_CHARACTER,
                ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ICSVWriter.DEFAULT_LINE_END
        );
    }

    /**
     * Method to get a BufferedWriter instance.
     * @return The BufferedWriter instance.
     * @throws IOException If an I/O error occurs.
     */
    protected BufferedWriter getWriter() throws IOException {
        return Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Method to get a StatefulBeanToCsv instance.
     * @param csvWriter The CSVWriter to be used by the StatefulBeanToCsv.
     * @return The StatefulBeanToCsv instance.
     */
    StatefulBeanToCsv<PingLog> getStatefulBeanToCsv(CSVWriter csvWriter) {
        return new StatefulBeanToCsvBuilder<PingLog>(csvWriter)
                .build();
    }

    /**
     * Method to create the ping logs directory.
     * It creates the directory if it does not exist.
     * It handles the IOException.
     */
    protected void createPingLogsDirectory() throws IOException {
        String pingLogsPath = appProperties.getPingLogsPath();
        if (pingLogsPath != null) {
            try {
                Files.createDirectories(Paths.get(pingLogsPath));
            } catch (Exception e) {
                logger.error("Failed to create ping logs directory: {}", pingLogsPath, e);
            }
        } else {
            logger.warn("Property 'conntest.pinglogs-path' is not set. Ping logs will not be saved.");
            throw new IOException("Ping logs path not set");
        }
    }

    /**
     * Method to reset the file path on runtime.
     * It sets the file path from the properties and initializes the file.
     * It handles the IOException.
     * @throws IOException If an I/O error occurs.
     */
    public void resetPath() throws IOException {
        setFilePathFromProperties();
        setWriter();
    }

    /**
     * Method to set the file path from the properties.
     * It sets the file path from the properties and initializes the file.
     * It handles the IOException.
     */
    public void setFilePathFromProperties(){
        this.filePath = Paths.get(appProperties.getPingLogsPath() + appProperties.getPinglogsFilename());
    }

    /**
     * Method to set the writer and the StatefulBeanToCsv instance.
     * It handles the IOException.
     * @throws IOException If an I/O error occurs.
     */
    private void setWriter() throws IOException {
        writer = getWriter();
        sbc = getStatefulBeanToCsv(getCsvWriter(writer));
    }
}
