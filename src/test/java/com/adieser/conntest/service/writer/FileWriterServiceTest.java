package com.adieser.conntest.service.writer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.adieser.conntest.configurations.AppProperties;
import com.adieser.conntest.models.PingLog;
import com.adieser.utils.TestUtils;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static com.adieser.conntest.models.CsvPingLogRepository.SAVE_PING_ERROR_MSG;
import static com.adieser.conntest.service.writer.FileWriterService.FORMATTER;
import static com.adieser.conntest.service.writer.FileWriterService.ROTATED_FILENAME_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileWriterServiceTest {

    @Mock
    private ExecutorService threadPoolExecutor;

    @Mock
    private AppProperties appProperties;

    @Mock
    BlockingQueue<PingLog> mockedQueue;

    @Mock
    BufferedWriter mockedWriter;

    @Mock
    StatefulBeanToCsv<PingLog> mockedSbc;

    @Mock
    Logger mockedLogger;

    @InjectMocks
    @Spy
    FileWriterService fileWriterService;

    @TempDir
    Path tempDir;

    private ListAppender<ILoggingEvent> logWatcher;

    @BeforeEach
    void setup() {
        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(FileWriterService.class)).addAppender(logWatcher);

        Instant fixedInstant = LocalDateTime.of(2024, 12, 15, 11, 48)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        fileWriterService.clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        fileWriterService.queue = mockedQueue;
    }

    @Test
    void submit() {
        // Given
        when(mockedQueue.offer(any())).thenReturn(true);

        // When
        fileWriterService.submit(PingLog.builder().build());

        // Then
        verify(mockedQueue, times(1)).offer(any());
    }

    @Test
    void summit_error() {
        // Given
        when(mockedQueue.offer(any())).thenReturn(false);

        // When
        fileWriterService.submit(PingLog.builder().build());

        // Then
        verify(mockedQueue, times(1)).offer(any());

        assertEquals(1, logWatcher.list.size());
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains("Failed to add data to the queue");
    }

    @Test
    void init() throws IOException {
        // Given
        doReturn(mock(BufferedWriter.class)).when(fileWriterService).getWriter();
        doReturn(mock(CSVWriter.class)).when(fileWriterService).getCsvWriter(any());
        doNothing().when(fileWriterService).createPingLogsDirectory();
        doNothing().when(threadPoolExecutor).execute(any(Runnable.class));

        // When
        fileWriterService.init();

        // Then
        verify(threadPoolExecutor, times(1)).execute(any(Runnable.class));
    }

    @Test
    void init_IOException() throws IOException {
        // Given
        doNothing().when(fileWriterService).createPingLogsDirectory();
        doThrow(IOException.class).when(fileWriterService).getWriter();

        // When & Then
        assertThrows(IOException.class, () -> fileWriterService.init());
    }

    @Test
    @SuppressWarnings("squid:S2925")
    void startWriting_success() throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, InterruptedException {
        // Given
        doNothing().when(mockedWriter).flush();
        doReturn(mockedWriter).when(fileWriterService).getWriter();

        doNothing().when(mockedSbc).write(any(PingLog.class));
        doReturn(mockedSbc).when(fileWriterService).getStatefulBeanToCsv(any());

        doNothing().when(fileWriterService).createPingLogsDirectory();
        doNothing().when(threadPoolExecutor).execute(any(Runnable.class));

        doNothing().when(fileWriterService).checkFileSizeAndRotate();
        fileWriterService.queue = new LinkedBlockingQueue<>();

        fileWriterService.init();

        fileWriterService.submit(TestUtils.getDefaultPingLog());

        // When
        new Thread(fileWriterService::startWriting).start();
        Thread.sleep(1000);
        fileWriterService.running = false;

        // Then
        verify(fileWriterService, times(1)).checkFileSizeAndRotate();
        verify(mockedSbc, times(1)).write(any(PingLog.class));
        verify(mockedWriter, times(1)).flush();
    }

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    void startWriting_Exceptions(Exception thrown) throws IOException, CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        // Given
        if(thrown instanceof IOException)
            doThrow(thrown.getClass()).when(fileWriterService).checkFileSizeAndRotate();
        else {
            doReturn(mockedSbc).when(fileWriterService).getStatefulBeanToCsv(any());
            doReturn(mockedWriter).when(fileWriterService).getWriter();
            doNothing().when(fileWriterService).createPingLogsDirectory();
            doNothing().when(threadPoolExecutor).execute(any(Runnable.class));
            doNothing().when(fileWriterService).checkFileSizeAndRotate();

            doThrow(thrown.getClass()).when(mockedSbc).write(any(PingLog.class));

            LinkedBlockingQueue<PingLog> queue = new LinkedBlockingQueue<>();
            queue.add(TestUtils.getDefaultPingLog());
            fileWriterService.queue = queue;

            fileWriterService.init();
        }

        // When
        fileWriterService.startWriting();

        // Then
        assertEquals(1, logWatcher.list.size());
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains(SAVE_PING_ERROR_MSG);
    }

    @Test
    void startWriting_InterruptedException() throws InterruptedException {
        // Given
        doThrow(InterruptedException.class).when(mockedQueue).take();

        // When
        fileWriterService.startWriting();

        // Then
        assertEquals(1, logWatcher.list.size());
        assertThat(logWatcher.list.get(0).getFormattedMessage()).contains("File writing thread interrupted");
    }

    @Test
    void checkFileSizeAndRotate_notRotate() throws IOException {
        // Given
        Path testFilePath = tempDir.resolve("ping.log");
        Files.createFile(testFilePath);

        String content = "Test content\n";
        Files.write(testFilePath, content.getBytes());

        when(appProperties.getFileMaxSizeRows()).thenReturn(10L);
        when(appProperties.getPingLogsPath()).thenReturn(tempDir.toString());
        when(appProperties.getPinglogsFilename()).thenReturn("ping.log");

        // When
        fileWriterService.checkFileSizeAndRotate();

        // Then
        verify(mockedWriter, never()).close();
        try(var list = Files.list(tempDir)) {
            assertEquals(1, list.count());
        }
    }

    @Test
    void checkFileSizeAndRotate_rotate() throws IOException {
        // Given
        Path testFilePath = tempDir.resolve("ping.log");
        Files.createFile(testFilePath);

        String content = "Test content\nTest content\nTest content\n";
        Files.write(testFilePath, content.getBytes());

        when(appProperties.getFileMaxSizeRows()).thenReturn(2L);
        when(appProperties.getPingLogsPath()).thenReturn(tempDir.toString());
        when(appProperties.getPinglogsFilename()).thenReturn("ping.log");

        fileWriterService.filePath = testFilePath;
        fileWriterService.writer = mockedWriter;
        doReturn(mock(BufferedWriter.class)).when(fileWriterService).getWriter();

        // When
        fileWriterService.checkFileSizeAndRotate();

        // Then
        verify(mockedWriter).close();
        try(var list = Files.list(tempDir)) {
            assertEquals(1, list.count());
        }
        assertFalse(Files.exists(testFilePath));

        String timestamp = LocalDateTime.now(fileWriterService.clock).format(FORMATTER);
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                String.format(ROTATED_FILENAME_FORMAT,
                                        timestamp,
                                        1)
                        ))
        );

        // Verify new writer was created
        assertNotEquals(mockedWriter, fileWriterService.writer);
    }

    @Test
    void cleanUp_success() throws IOException {
        // Given
        BufferedWriter mockWriter = mock(BufferedWriter.class);
        doReturn(mockWriter).when(fileWriterService).getWriter();
        doNothing().when(fileWriterService).createPingLogsDirectory();

        fileWriterService.init();

        // When
        fileWriterService.cleanUp();

        // Then
        assertFalse(fileWriterService.running);
        verify(mockWriter, times(1)).close();
    }

    @Test
    void cleanUp_notNull() {
        // When
        fileWriterService.cleanUp();

        // Then
        assertFalse(fileWriterService.running);
    }

    @Test
    void cleanUp_ioException() throws IOException {
        // Given
        BufferedWriter mockWriter = mock(BufferedWriter.class);
        doReturn(mockWriter).when(fileWriterService).getWriter();
        doThrow(IOException.class).when(mockWriter).close();
        doNothing().when(fileWriterService).createPingLogsDirectory();

        fileWriterService.init();

        // When
        fileWriterService.cleanUp();

        // Then
        assertFalse(fileWriterService.running);
        assertEquals(2, logWatcher.list.size());
        assertThat(logWatcher.list.get(1).getFormattedMessage()).contains("Error closing the file");
    }

    @Test
    void getCsvWriter() {
        // Given
        BufferedWriter mockWriter = mock(BufferedWriter.class);

        // When
        CSVWriter csvWriter = fileWriterService.getCsvWriter(mockWriter);

        // Then
        assertThat(csvWriter).isNotNull();
    }

    @Test
    void getStatefulBeanToCsv() {
        // Given
        CSVWriter mockCsvWriter = mock(CSVWriter.class);

        // When
        StatefulBeanToCsv<PingLog> statefulBeanToCsv = fileWriterService.getStatefulBeanToCsv(mockCsvWriter);

        // When
        assertNotNull(statefulBeanToCsv);
        assertThat(statefulBeanToCsv).isInstanceOf(StatefulBeanToCsv.class);
    }

    @Test
    void createPingLogsDirectory_ShouldCreateDirectory_WhenDirectoryDoesNotExist() throws IOException {
        // Given
        Path logsPath = tempDir.resolve("ping-logs");
        when(appProperties.getPingLogsPath()).thenReturn(logsPath.toString());

        // When
        fileWriterService.createPingLogsDirectory();

        // Then
        assertTrue(Files.exists(logsPath));
        assertTrue(Files.isDirectory(logsPath));
    }

    @Test
    void createPingLogsDirectory_ShouldNotThrowException_WhenDirectoryAlreadyExists() throws IOException {
        // Given
        Path logsPath = tempDir.resolve("ping-logs");
        Files.createDirectory(logsPath);
        when(appProperties.getPingLogsPath()).thenReturn(logsPath.toString());

        // When & Then
        assertDoesNotThrow(() -> fileWriterService.createPingLogsDirectory());
        assertTrue(Files.exists(logsPath));
        assertTrue(Files.isDirectory(logsPath));
    }

    @Test
    void createPingLogsDirectory_ShouldThrowException_WhenPathIsFile() throws IOException {
        // Given
        Path filePath = tempDir.resolve("ping-logs");
        Files.createFile(filePath);
        appProperties.setPingLogsPath(filePath.toString());

        // When & Then
        assertThrows(IOException.class, () -> fileWriterService.createPingLogsDirectory());
    }

    @Test
    void createPingLogsDirectory_ShouldThrowException_WhenNoWritePermissions() throws IOException {
        // Given
        Path readOnlyPath = tempDir.resolve("readonly-dir");
        Files.createDirectory(readOnlyPath);

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            Files.setAttribute(readOnlyPath, "dos:readonly", true);
        } else {
            // Unix-like systems: remove write permissions
            Files.setPosixFilePermissions(readOnlyPath,
                    PosixFilePermissions.fromString("r-xr-xr-x"));
        }

        try {
            Path logsPath = readOnlyPath.resolve("ping-logs");
            appProperties.setPingLogsPath(logsPath.toString());

            // When & Then
            assertThrows(IOException.class, () -> fileWriterService.createPingLogsDirectory());
        } finally {
            // Cleanup
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                Files.setAttribute(readOnlyPath, "dos:readonly", false);
            } else {
                Files.setPosixFilePermissions(readOnlyPath,
                        PosixFilePermissions.fromString("rwxrwxrwx"));
            }
        }
    }

    @Test
    void createPingLogsDirectory_ShouldThrowException_WhenPathIsNull() {
        // Given
        when(appProperties.getPingLogsPath()).thenReturn(null);

        // When & Then
        Exception e = assertThrows(IOException.class, () -> fileWriterService.createPingLogsDirectory());
        assertEquals("Ping logs path not set", e.getMessage());
    }

    static Stream<Exception> exceptionProvider() {
        return Stream.of(
                new IOException("Test IO Exception"),
                new CsvRequiredFieldEmptyException("Test CSV Required Field Empty"),
                new CsvDataTypeMismatchException("Test CSV Data Type Mismatch", null, null)
        );
    }
}