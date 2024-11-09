package com.adieser.conntest.models;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    public CsvPingLogRepository(String path, Logger logger) {
        this.path = path;
        this.logger = logger;
    }

    @Override
    public void savePingLog(PingLog pingLog) throws InterruptedException {
        try (Writer writer  = getFileWriter(true)) {
            CSVWriter csvWriter = getCsvWriter(writer);
            StatefulBeanToCsv<PingLog> sbc = getStatefulBeanToCsv(csvWriter);

            sbc.write(pingLog);
        } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            logger.error(SAVE_PING_ERROR_MSG, e);
            throw new InterruptedException(SAVE_PING_ERROR_MSG);
        }
    }

    FileWriter getFileWriter(boolean append) throws IOException {
        return new FileWriter(path + "/" + PING_LOG_NAME, append);
    }

    CSVWriter getCsvWriter(Writer writer) {
        return new CSVWriter(writer,
                ICSVWriter.DEFAULT_SEPARATOR,
                ICSVWriter.NO_QUOTE_CHARACTER,
                ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ICSVWriter.DEFAULT_LINE_END
        );
    }

    StatefulBeanToCsv<PingLog> getStatefulBeanToCsv(CSVWriter csvWriter) {
        return new StatefulBeanToCsvBuilder<PingLog>(csvWriter)
                .build();
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
    public List<PingLog> findMaxMinPingLog(String ipAddress) throws IOException {

        PingLog lowestLatency = getPingLogsByIpStream(readAll().stream(), ipAddress)
                .min(Comparator.comparingLong(PingLog::getPingTime))
                .orElse(null);

        // If one of the pingLogs is null, it means both will (the file is empty). Therefore, it returns and empty list
        if(lowestLatency == null)
            return List.of();

        PingLog highestLatency = getPingLogsByIpStream(readAll().stream(), ipAddress)
                .max(Comparator.comparingLong(PingLog::getPingTime))
                .orElse(null);

        List<PingLog> highestLowest = new ArrayList<>();
        highestLowest.add(lowestLatency);
        highestLowest.add(highestLatency);

        return highestLowest;
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
}
