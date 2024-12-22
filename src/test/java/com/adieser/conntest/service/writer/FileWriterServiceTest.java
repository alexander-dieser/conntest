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
        doNothing().when(threadPoolExecutor).execute(any(Runnable.class));

        // When
        fileWriterService.init();

        // Then
        verify(threadPoolExecutor, times(1)).execute(any(Runnable.class));
    }

    @Test
    void init_IOException() throws IOException {
        // Given
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

        when(appProperties.getFileMaxSize()).thenReturn(10L);
        when(appProperties.getPingLogsPath()).thenReturn(tempDir.toString());

        // When
        fileWriterService.checkFileSizeAndRotate();

        // Then
        verify(mockedWriter, never()).close();
        assertEquals(1, Files.list(tempDir).count());
    }

    @Test
    void checkFileSizeAndRotate_rotate() throws IOException {
        // Arrange
        Path testFilePath = tempDir.resolve("ping.log");
        Files.createFile(testFilePath);

        String content = "Test content\nTest content\nTest content\n";
        Files.write(testFilePath, content.getBytes());

        when(appProperties.getFileMaxSize()).thenReturn(2L);
        when(appProperties.getPingLogsPath()).thenReturn(tempDir.toString());

        fileWriterService.filePath = testFilePath;
        fileWriterService.writer = mockedWriter;
        doReturn(mock(BufferedWriter.class)).when(fileWriterService).getWriter();

        // Act
        fileWriterService.checkFileSizeAndRotate();

        // Assert
        verify(mockedWriter).close();
        assertEquals(1, Files.list(tempDir).count());
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

    static Stream<Exception> exceptionProvider() {
        return Stream.of(
                new IOException("Test IO Exception"),
                new CsvRequiredFieldEmptyException("Test CSV Required Field Empty"),
                new CsvDataTypeMismatchException("Test CSV Data Type Mismatch", null, null)
        );
    }
}