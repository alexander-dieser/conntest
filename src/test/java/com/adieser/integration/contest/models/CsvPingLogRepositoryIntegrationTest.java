package com.adieser.integration.contest.models;

import com.adieser.conntest.models.CsvPingLogRepository;
import com.adieser.conntest.models.PingLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.adieser.conntest.models.CsvPingLogRepository.PING_LOG_NAME;
import static com.adieser.utils.TestUtils.CLOUD_IP_ADDRESS;
import static com.adieser.utils.TestUtils.DEFAULT_LOG_DATE_TIME;
import static com.adieser.utils.TestUtils.DEFAULT_PING_TIME;
import static com.adieser.utils.TestUtils.LOCAL_IP_ADDRESS;
import static com.adieser.utils.TestUtils.getDefaultPingLog;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CsvPingLogRepositoryIntegrationTest{

    private static final String PINGLOGS_DIR = "src/test/resources/pingLogs";

    private CsvPingLogRepository csvPingLogRepository = new CsvPingLogRepository(
            PINGLOGS_DIR,
            LoggerFactory.getLogger("testLogger"));

    @Test
    void testPingLogFile() throws IOException, InterruptedException {

        savePingLog(csvPingLogRepository);

        List<PingLog> pingLogs = readPingLog();

        assertEquals(1, pingLogs.size(), "Wrong amount of pings");
        assertEquals(getDefaultPingLog(), pingLogs.get(0));
    }

    @Test
    void testPingLogFileAppend() throws IOException, InterruptedException {
        savePingLog(csvPingLogRepository);
        savePingLog(csvPingLogRepository);

        long pingAmount = readPingLog().size();

        assertEquals(2, pingAmount, "Failed to write ping in file");
    }

    @Test
    void testMissingPingLogFile() throws IOException, InterruptedException {
        //Make sure the pingLog.log does not exist
        Files.deleteIfExists(Paths.get(PINGLOGS_DIR + "/" + PING_LOG_NAME));

        savePingLog(csvPingLogRepository);

        assertTrue(new File(PINGLOGS_DIR + "/" + PING_LOG_NAME).exists());
    }

    @Test
    void testWrongPath() {
        Logger testLogger = mock(Logger.class);
        csvPingLogRepository = new CsvPingLogRepository(
                "wrong/path/to/pingLogs",
                testLogger);

        assertThrows(InterruptedException.class, () -> savePingLog(csvPingLogRepository));
        verify(testLogger,
                times(1)).error(eq(CsvPingLogRepository.SAVE_PING_ERROR_MSG),
                any(FileNotFoundException.class)
        );
    }

    @Test
    void testFindAllPingLogs() throws IOException {
        writePingLog();

        long pingAmount = csvPingLogRepository.findAllPingLogs().stream()
                .filter(pingLog -> LOCAL_IP_ADDRESS.equals(pingLog.getIpAddress())
                        && DEFAULT_PING_TIME == pingLog.getPingTime()
                        && DEFAULT_LOG_DATE_TIME.isEqual(pingLog.getDateTime())
                )
                .count();

        assertEquals(1, pingAmount, "No ping found");
    }

    @Test
    void testFindPingLogByIp() throws IOException {
        writePingLog();

        long pingAmount = csvPingLogRepository.findPingLogByIp(LOCAL_IP_ADDRESS).size();

        assertEquals(1, pingAmount, "No ping found");
    }

    @Test
    void testFindPingLogsByDateTimeRange() throws IOException {
        writePingLog();

        long pingAmount = csvPingLogRepository.findPingLogsByDateTimeRange(
                DEFAULT_LOG_DATE_TIME.minusHours(1), DEFAULT_LOG_DATE_TIME.plusHours(1)
        ).size();

        assertEquals(1, pingAmount, "No ping found");
    }

    @Test
    void testFindPingLogsByDateTimeRangeByIp() throws IOException {
        writePingLog();

        long pingAmount = csvPingLogRepository.findPingLogsByDateTimeRangeByIp(
                DEFAULT_LOG_DATE_TIME.minusHours(1), DEFAULT_LOG_DATE_TIME.plusHours(1),
                LOCAL_IP_ADDRESS
        ).size();

        assertEquals(1, pingAmount, "No ping found");
    }

    @ParameterizedTest
    @MethodSource("thereIsFailedPingsProvider")
    void testFindLostPingLogsAvgByIP(boolean emptyFailedPingLogs) throws IOException {
        if(!emptyFailedPingLogs){
            writePingLog();
            writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, -1);
        }

        BigDecimal lostPingLogsAvgByIP = csvPingLogRepository.findLostPingLogsAvgByIP(LOCAL_IP_ADDRESS);

        if(!emptyFailedPingLogs)
            assertEquals(new BigDecimal("0.50"), lostPingLogsAvgByIP, "Wrong average");
        else
            assertEquals(BigDecimal.ZERO, lostPingLogsAvgByIP, "Wrong average");
    }

    @ParameterizedTest
    @MethodSource("thereIsFailedPingsProvider")
    void testFindLostPingLogsAvgByDateTimeRangeByIp(boolean emptyFailedPingLogs) throws IOException {
        if(!emptyFailedPingLogs) {
            // in-range success local ping
            writePingLog();
            // in-range lost local ping
            writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, -1);
            // in-range lost cloud ping
            writePingLog(DEFAULT_LOG_DATE_TIME, CLOUD_IP_ADDRESS, -1);
            // out-of-range lost local ping
            writePingLog(LocalDateTime.of(2023, 10, 5, 0, 0, 0),
                    LOCAL_IP_ADDRESS,
                    -1);
        }

        BigDecimal lostPingLogsAvgByIP = csvPingLogRepository.findLostPingLogsAvgByDateTimeRangeByIp(
                LocalDateTime.of(2023, 10, 6, 0, 0, 0),
                LocalDateTime.of(2023, 10, 6, 23, 59, 59),
                LOCAL_IP_ADDRESS);

        if(!emptyFailedPingLogs)
            assertEquals(new BigDecimal("0.50"), lostPingLogsAvgByIP, "Wrong average");
        else
            assertEquals(BigDecimal.ZERO, lostPingLogsAvgByIP, "Wrong average");
    }

    @Test
    void clearPingLogFile_Success() throws Exception {
        writePingLog();
        csvPingLogRepository.clearPingLogFile();

        assertEquals(0, readPingLog().size(), "Failed to clear ping log file");
    }

    /**
     * Create the directories and ping.log file
     */
    @BeforeEach
    public void setup() throws IOException {
        Files.createDirectories(Paths.get(PINGLOGS_DIR));
        Files.createFile(Paths.get(PINGLOGS_DIR + "/" + PING_LOG_NAME));
    }

    /**
     * remove directories and ping.log file
     */
    @AfterEach
    public void clean() throws IOException {
        deleteDir(Paths.get(PINGLOGS_DIR).toFile());
    }

    static Stream<Boolean> thereIsFailedPingsProvider() {
        return Stream.of(
                true,
                false
        );
    }

    /**
     * recursively delete directories, subdirectories and files
     */
   private void deleteDir(File fileOrDir) throws IOException {
        if (fileOrDir.isDirectory()) {
            File[] files = fileOrDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDir(file);
                }
            }
        }

        Files.deleteIfExists(fileOrDir.toPath());
   }

    /**
     * Save ping using the repository method
     */
    private void savePingLog(CsvPingLogRepository csvPingLogRepository) throws InterruptedException {
        csvPingLogRepository.savePingLog(PingLog.builder()
                .dateTime(DEFAULT_LOG_DATE_TIME)
                .ipAddress(LOCAL_IP_ADDRESS)
                .pingTime(DEFAULT_PING_TIME)
                .build()
        );
    }

    /**
     * Write a default ping directly in the file, bypassing the repository
     */
    private void writePingLog() throws IOException{
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, DEFAULT_PING_TIME);
    }

    /**
     * Write a ping directly in the file, bypassing the repository
     */
    private void writePingLog(LocalDateTime pingDateTime, String ipAddress, long pingTime) throws IOException{
        String pingLogDate = pingDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PINGLOGS_DIR + "/" + PING_LOG_NAME, true))) {
            writer.write(pingLogDate + "," + ipAddress + "," + pingTime + "\n");
        }
    }

    /**
     * Read pings from a file, bypassing the repository
     */
    private List<PingLog> readPingLog() throws IOException {
        List<PingLog> pingLogList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(PINGLOGS_DIR + "/" + PING_LOG_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] pinLogLine = line.split(",");

                pingLogList.add(
                    PingLog.builder()
                            .dateTime(LocalDateTime.parse(pinLogLine[0], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            .ipAddress(pinLogLine[1])
                            .pingTime(Long.parseLong(pinLogLine[2]))
                            .build()
                );
            }
        }

        return pingLogList;
    }
}
