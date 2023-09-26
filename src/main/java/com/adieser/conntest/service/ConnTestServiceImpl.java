package com.adieser.conntest.service;

import com.adieser.conntest.controllers.responses.PingLog;
import com.adieser.conntest.controllers.responses.PingLogFile;
import com.adieser.conntest.models.Pingable;
import com.adieser.conntest.models.ConnTestLogFileImpl;
import com.adieser.conntest.service.utils.CsvReaderService;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import static com.adieser.conntest.service.utils.CsvReaderService.LEVEL;

@Service
public class ConnTestServiceImpl implements ConnTestService {

    private static final String PINGLOGS_PATH = "conntest.pinglogs.path";
    private final Logger logger;
    private List<ConnTestLogFileImpl> tests = new ArrayList<>();
    private final ExecutorService threadPoolExecutor;
    private final Environment env;

    private final CsvReaderService csvReaderService;

    public ConnTestServiceImpl(ExecutorService threadPoolExecutor, Logger logger, Environment env, CsvReaderService csvReaderService) {
        this.logger = logger;
        this.threadPoolExecutor = threadPoolExecutor;
        this.env = env;
        this.csvReaderService = csvReaderService;
    }

    @Override
    public void testLocalISPInternet(List<String> ipAddresses){
        tests = ipAddresses.stream()
                .map(s -> new ConnTestLogFileImpl(threadPoolExecutor, s, logger, env.getProperty(PINGLOGS_PATH)))
                .toList();

        tests.forEach(Pingable::startPingSession);
    }

    public void stopTests(){
        if(tests.isEmpty())
            logger.warn("No tests to stop");
        else
            tests.forEach(Pingable::stopPingSession);
    }

    @Override
    public List<PingLogFile> getPings() {
        List<PingLogFile> pingResponses = new ArrayList<>();

        try {
            Path directory = Paths.get(Objects.requireNonNull(env.getProperty(PINGLOGS_PATH)));
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    logger.info("reading file {}", file);

                    List<PingLog> pingLogs = csvReaderService.getAll(file.toFile());

                    pingResponses.add(
                            PingLogFile.builder()
                                .fileName(file.getFileName().toString())
                                .pingLogs(pingLogs)
                                .amountOfPings(pingLogs.size())
                                .build()
                    );

                    return FileVisitResult.CONTINUE;
                }
            });

            return pingResponses;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PingLogFile> getPingsLostAvg() {
        List<PingLogFile> pingResponses = new ArrayList<>();

        try {
            Path directory = Paths.get(Objects.requireNonNull(env.getProperty(PINGLOGS_PATH)));
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    logger.info("reading file {}", file);

                    List<PingLog> pingLogs = csvReaderService.getAvg(file.toFile(), line -> line[LEVEL].equals(Level.FINE.toString()));

                    pingResponses.add(
                            PingLogFile.builder()
                                    .fileName(file.getFileName().toString())
                                    .pingLogs(pingLogs)
                                    .amountOfPings(pingLogs.size())
                                    .build()
                    );

                    return FileVisitResult.CONTINUE;
                }
            });

            return pingResponses;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
