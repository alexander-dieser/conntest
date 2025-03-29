package com.adieser.integration.contest.models;

import com.adieser.conntest.configurations.AppProperties;
import com.adieser.conntest.models.CsvPingLogRepository;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.models.utils.PingLogFileValidator;
import com.adieser.conntest.service.writer.FileWriterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.adieser.utils.TestUtils.CLOUD_IP_ADDRESS;
import static com.adieser.utils.TestUtils.DEFAULT_LOG_DATE_TIME;
import static com.adieser.utils.TestUtils.DEFAULT_PING_TIME;
import static com.adieser.utils.TestUtils.LOCAL_IP_ADDRESS;
import static com.adieser.utils.TestUtils.deleteDir;
import static com.adieser.utils.TestUtils.getDefaultPingLog;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvPingLogRepositoryIntegrationTest{
    private static final String PINGLOGS_DIR = "src/test/resources/pingLogs/";
    private static final String PING_LOG_NAME = "ping.log";

    AppProperties appProperties;
    private CsvPingLogRepository csvPingLogRepository;

    @BeforeEach
    void setUp() throws IOException {
        appProperties = new AppProperties();
        appProperties.setPingLogsPath(PINGLOGS_DIR);
        appProperties.setPinglogsFilename(PING_LOG_NAME);
        appProperties.setFileMaxSizeRows(50000L);
        appProperties.setFileMaxSizeKbytes(2000L);

        Files.createDirectories(Paths.get(PINGLOGS_DIR));
        Files.createFile(Paths.get(PINGLOGS_DIR + "/" + PING_LOG_NAME));

        PingLogFileValidator pingLogFileValidator = new PingLogFileValidator(appProperties);

        FileWriterService fileWriterService = new FileWriterService(
                LoggerFactory.getLogger("testLogger"),
                new ThreadPoolExecutor(3,
                        5,
                        60L,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>()),
                appProperties,
                Clock.systemDefaultZone()
                );

        fileWriterService.init();

        csvPingLogRepository = new CsvPingLogRepository(
                pingLogFileValidator,
                appProperties,
                LoggerFactory.getLogger("testLogger"),
                fileWriterService
        );
    }

    @Test
    void testPingLogFile() throws IOException {
        savePingLog(csvPingLogRepository);

        await()
                .atMost(200, TimeUnit.MILLISECONDS)
                .until(() -> readPingLog().size(), equalTo(1));

        List<PingLog> pingLogs = readPingLog();

        assertEquals(1, pingLogs.size(), "Wrong amount of pings");
        assertEquals(getDefaultPingLog(), pingLogs.get(0));
    }

    @Test
    void testPingLogFileAppend() throws IOException {
        savePingLog(csvPingLogRepository);
        savePingLog(csvPingLogRepository);

        await()
                .atMost(200, TimeUnit.MILLISECONDS)
                .until(() -> readPingLog().size(), equalTo(2));

        long pingAmount = readPingLog().size();

        assertEquals(2, pingAmount, "Failed to write ping in file");
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

    @Test
    void testFindLostPingsByIp() throws IOException {
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, -1);

        long pingAmount = csvPingLogRepository.findLostPingsByIp(LOCAL_IP_ADDRESS).size();

        assertEquals(1, pingAmount, "No ping found");
    }

    @Test
    void testFindLostPingsByDateTimeRangeByIp() throws IOException {
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

        long pingAmount = csvPingLogRepository.findLostPingsByDateTimeRangeByIp(
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

    @ParameterizedTest
    @MethodSource("minMaxPinglogsProvider")
    void testFindMaxMinPingLog(List<Long> pingTimes, Long expectedMinPing, Long expectedMaxPing) throws IOException {
        for(Long time : pingTimes)
            writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, time);

        List<PingLog> maxMinPingLog = csvPingLogRepository.findMaxMinPingLogOfAll(LOCAL_IP_ADDRESS);

        if(expectedMinPing != null) {
            assertEquals(expectedMinPing, maxMinPingLog.get(0).getPingTime(), "wrong min ping");
            assertEquals(expectedMaxPing, maxMinPingLog.get(1).getPingTime(), "wrong max ping");
        }
        else
            assertTrue(maxMinPingLog.isEmpty());
    }

    @Test
    void testFindMaxMinPingLogByDateTimeRangeByIp() throws IOException {
        // in-range local ping
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, 2);
        // in-range local ping
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, 4);
        // in-range lost local ping
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, -1);
        // in-range cloud ping
        writePingLog(DEFAULT_LOG_DATE_TIME, CLOUD_IP_ADDRESS, 8);
        // out-of-range local ping
        writePingLog(LocalDateTime.of(2023, 10, 5, 0, 0, 0),
                LOCAL_IP_ADDRESS,
                10);

        List<PingLog> maxMinPingLog = csvPingLogRepository.findMaxMinPingLogByDateTimeRangeByIp(
                LocalDateTime.of(2023, 10, 6, 0, 0, 0),
                LocalDateTime.of(2023, 10, 6, 23, 59, 59),
                LOCAL_IP_ADDRESS);

        assertEquals(2, maxMinPingLog.size(), "Wrong amount of pings");
        assertEquals(2, maxMinPingLog.get(0).getPingTime(), "wrong min ping");
        assertEquals(4, maxMinPingLog.get(1).getPingTime(), "wrong max ping");
    }

    @Test
    void testFindMaxMinPingLogByDateTimeRangeByIp_Empty() throws IOException {
        // in-range local ping
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, 2);
        // in-range local ping
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, 4);
        // in-range lost local ping
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, -1);
        // in-range cloud ping
        writePingLog(DEFAULT_LOG_DATE_TIME, CLOUD_IP_ADDRESS, 8);
        // out-of-range local ping
        writePingLog(LocalDateTime.of(2023, 10, 5, 0, 0, 0),
                LOCAL_IP_ADDRESS,
                10);

        List<PingLog> maxMinPingLog = csvPingLogRepository.findMaxMinPingLogByDateTimeRangeByIp(
                LocalDateTime.of(2023, 10, 7, 0, 0, 0),
                LocalDateTime.of(2023, 10, 7, 23, 59, 59),
                LOCAL_IP_ADDRESS);

        assertTrue(maxMinPingLog.isEmpty());
    }

    @Test
    void testFindAvgLatencyByIp() throws IOException {
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, 10L);
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, 20L);
        writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, 30L);

        BigDecimal avgLatency = csvPingLogRepository.findAvgLatencyByIp(LOCAL_IP_ADDRESS);

        assertEquals(BigDecimal.valueOf(20L).setScale(2, RoundingMode.HALF_UP), avgLatency);
    }

    @ParameterizedTest
    @MethodSource("thereIsFailedPingsProvider")
    void testFindAvgLatencyByDateTimeRangeByIp(boolean emptyFailedPingLogs) throws IOException {
        if(!emptyFailedPingLogs) {
            // in-range local ping
            writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, 2);
            // in-range local ping
            writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, 4);
            // in-range lost local ping
            writePingLog(DEFAULT_LOG_DATE_TIME, LOCAL_IP_ADDRESS, -1);
            // in-range cloud ping
            writePingLog(DEFAULT_LOG_DATE_TIME, CLOUD_IP_ADDRESS, 8);
            // out-of-range local ping
            writePingLog(LocalDateTime.of(2023, 10, 5, 0, 0, 0),
                    LOCAL_IP_ADDRESS,
                    10);
        }

        BigDecimal pingLogsLatencyAvgByIP = csvPingLogRepository.findAvgLatencyByDateTimeRangeByIp(
                LocalDateTime.of(2023, 10, 6, 0, 0, 0),
                LocalDateTime.of(2023, 10, 6, 23, 59, 59),
                LOCAL_IP_ADDRESS);

        if(!emptyFailedPingLogs)
            assertEquals(new BigDecimal("3").setScale(2, RoundingMode.HALF_UP), pingLogsLatencyAvgByIP, "Wrong average");
        else
            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), pingLogsLatencyAvgByIP, "Wrong average");
    }

    @Test
    void clearPingLogFile_Success() throws Exception {
        writePingLog();
        csvPingLogRepository.clearPingLogFile();

        assertEquals(0, readPingLog().size(), "Failed to clear ping log file");
    }

    @Test
    void changeDatasource_Success()throws Exception {
        writePingLog();
        csvPingLogRepository.changeDatasource("newDatasource.log");
        writePingLog();

        assertEquals(1, readPingLog().size(), "Failed to change datasource");
        assertEquals("newDatasource.log", appProperties.getPinglogsFilename(), "Failed to change datasource");
    }

    /**
     * remove directories and ping.log file
     */
    @AfterEach
    void clean() throws IOException {
        deleteDir(Paths.get(PINGLOGS_DIR).toFile());
    }

    static Stream<Boolean> thereIsFailedPingsProvider() {
        return Stream.of(
                true,
                false
        );
    }

    static Stream<Arguments> minMaxPinglogsProvider() {
        return Stream.of(
                // Multiple pinglogs
                Arguments.of(
                        List.of(1L, 50L, 200L),
                        1L,
                        200L
                ),
                // Multiple pinglogs same pingtime
                Arguments.of(
                        List.of(1L, 1L),
                        1L,
                        1L
                ),
                // One pinglog only
                Arguments.of(
                        List.of(1L),
                        1L,
                        1L
                ),
                // No pinglogs
                Arguments.of(
                        List.of(),
                        null,
                        null
                ),
                // Lost pinglogs
                Arguments.of(
                        List.of(-1L, 1L, 5L),
                        1L,
                        5L
                )
        );
    }

    /**
     * Save ping using the repository method
     */
    private void savePingLog(CsvPingLogRepository csvPingLogRepository) {
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

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PINGLOGS_DIR + "/" + appProperties.getPinglogsFilename(), true))) {
            writer.write(pingLogDate + "," + ipAddress + "," + pingTime + "\n");
        }
    }

    /**
     * Read pings from a file, bypassing the repository
     */
    private List<PingLog> readPingLog() throws IOException {
        List<PingLog> pingLogList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(PINGLOGS_DIR + "/" + appProperties.getPinglogsFilename()))) {
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
