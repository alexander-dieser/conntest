package com.adieser.conntest.models;

import com.adieser.conntest.service.writer.FileWriterService;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  Repository for pings stored in a text file. Pings are in CSV format
 *  <pre>
 *  Format: timestamp,ipAddress,time
 *
 *  Ex:
 *  2023-09-27 01:17:26,192.168.1.1,0
 *  2023-09-27 01:17:26,131.100.65.1,-1
 *  </pre>
 */
public class CsvPingLogRepository implements PingLogRepository {
    public static final String SAVE_PING_ERROR_MSG = "Save Ping error";
    public static final String CLEAN_PINGLOG_FILE_MSG = "Clear pinglog file error";
    public static final String PING_LOG_NAME = "ping.log";
    private final String path;

    private final Logger logger;
    private final FileWriterService fileWriterService;

    public CsvPingLogRepository(String path, Logger logger, FileWriterService fileWriterService) {
        this.path = path;
        this.logger = logger;
        this.fileWriterService = fileWriterService;
    }

    @Override
    public void savePingLog(PingLog pingLog) {
        fileWriterService.submit(pingLog);
    }

    @Override
    public List<PingLog> findAllPingLogs() throws IOException {
       return readAll();
    }

    @Override
    public List<PingLog> findPingLogByIp(String ipAddress) throws IOException {
        return getPingLogsByIpStream(readAll().stream(), ipAddress)
                .toList();
    }

    @Override
    public List<PingLog> findPingLogsByDateTimeRange(LocalDateTime start, LocalDateTime end) throws IOException {
        return getPingLogsByDateTimeRangeStream(readAll().stream(), start, end)
                .toList();
    }

    @Override
    public List<PingLog> findPingLogsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException {
        return getPingLogsByDateTimeRangeByIpStream(readAll().stream(), start, end, ipAddress)
                .toList();
    }

    @Override
    public List<PingLog> findLostPingsByIp(String ipAddress) throws IOException {
        return getLostPingLogsStream(getPingLogsByIpStream(readAll().stream(), ipAddress))
                .toList();
    }

    @Override
    public List<PingLog> findLostPingsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException {
        return getLostPingLogsStream(getPingLogsByDateTimeRangeByIpStream(readAll().stream(), start, end, ipAddress))
                .toList();
    }

    @Override
    public List<PingLog> findMaxMinPingLogOfAll(String ipAddress) throws IOException {
        return findMaxMinPingLog(getPingLogsByIpStream(readAll().stream(), ipAddress));
    }

    @Override
    public List<PingLog> findMaxMinPingLogByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException {
        return findMaxMinPingLog(getPingLogsByDateTimeRangeByIpStream(readAll().stream(), start, end, ipAddress));
    }

    @Override
    public BigDecimal findAvgLatencyByIp(String ipAddress) throws IOException {
        double averagePingTime = getPingLogsByIpStream(readAll().stream(), ipAddress)
                .map(PingLog::getPingTime)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        return BigDecimal.valueOf(averagePingTime).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal findAvgLatencyByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException {
        double averagePingTime = getPingLogsByDateTimeRangeByIpStream(readAll().stream(), start, end, ipAddress)
                .map(PingLog::getPingTime)
                .filter(time -> time != -1)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        return BigDecimal.valueOf(averagePingTime).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal findLostPingLogsAvgByIP(String ipAddress) throws IOException {

        List<PingLog> pingLogsByIp = getPingLogsByIpStream(readAll().stream(), ipAddress).toList();

        long lost = getLostPingLogsStream(pingLogsByIp.stream())
                .count();

        if(pingLogsByIp.isEmpty())
            return BigDecimal.ZERO;
        else
            return BigDecimal.valueOf(lost).divide(
                    BigDecimal.valueOf(pingLogsByIp.size()),
                    2,
                    RoundingMode.CEILING);
    }

    @Override
    public BigDecimal findLostPingLogsAvgByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) throws IOException {
        List<PingLog> pingLogsInDateTimeRange = getPingLogsByDateTimeRangeByIpStream(readAll().stream(), start, end, ipAddress)
                .toList();

        long lost = getLostPingLogsStream(pingLogsInDateTimeRange.stream())
                .count();

        if(pingLogsInDateTimeRange.isEmpty())
            return BigDecimal.ZERO;
        else
            return BigDecimal.valueOf(lost).divide(
                    BigDecimal.valueOf(pingLogsInDateTimeRange.size()),
                    2,
                    RoundingMode.CEILING);
    }

    @Override
    public void clearPingLogFile() throws InterruptedException {
        try (Writer writer  = getFileWriter(false)) {
            writer.write("");
            writer.flush();
        } catch (IOException e) {
            logger.error(CLEAN_PINGLOG_FILE_MSG, e);
            throw new InterruptedException(CLEAN_PINGLOG_FILE_MSG);
        }
    }

    FileWriter getFileWriter(boolean append) throws IOException {
        return new FileWriter(path + "/" + PING_LOG_NAME, append);
    }

    /**
     * Retrieve all the pings from the CSV file
     * @return List of pings
     */
    protected List<PingLog> readAll() throws IOException {
        Reader reader = getReader();
        CsvToBean<PingLog> cb = getCsvToBean(reader);

        return cb.parse();
    }

    BufferedReader getReader() throws IOException {
        return Files.newBufferedReader(Path.of(path + "/" + PING_LOG_NAME));
    }

    CsvToBean<PingLog> getCsvToBean(Reader reader) {
        return new CsvToBeanBuilder<PingLog>(reader)
                .withType(PingLog.class)
                .build();
    }

    /**
     * Get a Stream of pings, applying a filter for retrieving only those pings related to a given ipAddress
     * @param st stream to apply the filtering
     * @param ipAddress IP address for filtering
     * @return Stream of pings with a filter applied
     */
    protected Stream<PingLog> getPingLogsByIpStream(Stream<PingLog> st, String ipAddress){
        return st
                .filter(pingLog -> pingLog.getIpAddress().equals(ipAddress));
    }

    /**
     * Get a Stream of pings, applying a filter for retrieving only those pings related to a given ipAddress, within
     * a datetime range
     * @param st stream to apply the filtering
     * @param start start date and time of the range
     * @param end end date in the range
     * @param ipAddress IP address for filtering
     * @return Stream of pings with a filter applied
     */
    protected Stream<PingLog> getPingLogsByDateTimeRangeByIpStream(Stream<PingLog> st, LocalDateTime start, LocalDateTime end, String ipAddress){
        return getPingLogsByDateTimeRangeStream(
                getPingLogsByIpStream(st, ipAddress),
                start,
                end
        );
    }

    /**
     * Get a Stream of pings, applying a filter for retrieving only those pings that have failed
     * @param st stream to apply the filtering
     * @return Stream of pings with a filter applied
     */
    protected Stream<PingLog> getLostPingLogsStream(Stream<PingLog> st){
        return st
                .filter(pingLog -> pingLog.getPingTime() < 0);
    }

    /**
     * Get a Stream of pings, applying a filter for retrieving only those pings that have failed within a
     * datetime range
     * @param st stream to apply the filtering
     * @param start start date and time of the range
     * @param end end date in the range
     * @return Stream of pings with a filter applied
     */
    protected Stream<PingLog> getPingLogsByDateTimeRangeStream(Stream<PingLog> st, LocalDateTime start, LocalDateTime end){
        return st
                .filter(pingLog ->
                        (pingLog.getDateTime().isAfter(start) && pingLog.getDateTime().isBefore(end))
                        || pingLog.getDateTime().isEqual(start) || pingLog.getDateTime().isEqual(end)
                );
    }

    /**
     * Get the minimum and maximum ping times of a ping list. Lost pings are not taken into account.
     * If one of the pings is null, it means both are, so an empty list is returned.
     * @param pingLogStream stream of pings
     * @return List of two pings, the first one is the minimum and the second one is the maximum ping time
     */
    private List<PingLog> findMaxMinPingLog(Stream<PingLog> pingLogStream) {
        return pingLogStream
                .filter(pingLog -> pingLog.getPingTime() != -1)
                .collect(Collectors.teeing(
                        Collectors.minBy(Comparator.comparingLong(PingLog::getPingTime)),
                        Collectors.maxBy(Comparator.comparingLong(PingLog::getPingTime)),
                        (min, max) ->
                            //if one is empty, return an empty list
                            min.map(pingLog -> List.of(pingLog, max.get())).orElseGet(List::of)
                ));
    }
}
