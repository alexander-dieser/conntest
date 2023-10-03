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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
    private final String path;
    private final Logger logger;

    public CsvPingLogRepository(String path, Logger logger) {
        this.path = path;
        this.logger = logger;
    }

    @Override
    public void savePingLog(PingLog pingLog) {
        try (Writer writer  = new FileWriter(path + "/ping.log", true)) {

            CSVWriter csvWriter = new CSVWriter(writer,
                    ICSVWriter.DEFAULT_SEPARATOR,
                    ICSVWriter.NO_QUOTE_CHARACTER,
                    ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    ICSVWriter.DEFAULT_LINE_END
            );

            StatefulBeanToCsv<PingLog> sbc = new StatefulBeanToCsvBuilder<PingLog>(csvWriter)
                    .build();

            sbc.write(pingLog);
        } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            logger.error("Save Ping error", e);
        }
    }

    @Override
    public List<PingLog> findAllPingLogs() {
       return readAll();
    }

    @Override
    public List<PingLog> findPingLogByIp(String ipAddress) {
        return getPingLogsByIpStream(readAll().stream(), ipAddress)
                .toList();
    }

    @Override
    public List<PingLog> findPingLogsByDateTimeRange(LocalDateTime start, LocalDateTime end) {
        return getPingLogsByDateTimeRange(readAll().stream(), start, end)
                .toList();
    }

    @Override
    public List<PingLog> findPingLogsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) {
        return getPingLogsByDateTimeRangeByIp(readAll().stream(), start, end, ipAddress)
                .toList();
    }

    @Override
    public BigDecimal findLostPingLogsAvgByIP(String ipAddress) {
        List<PingLog> pingLogsByIp = getPingLogsByIpStream(readAll().stream(), ipAddress).toList();

        long lost = getLostPingLogs(pingLogsByIp.stream())
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
    public BigDecimal findLostPingLogsAvgByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) {
        List<PingLog> pingLogsInDateTimeRange = getPingLogsByDateTimeRangeByIp(readAll().stream(), start, end, ipAddress)
                .toList();

        long lost = getLostPingLogs(pingLogsInDateTimeRange.stream())
                .count();

        if(pingLogsInDateTimeRange.isEmpty())
            return BigDecimal.ZERO;
        else
            return BigDecimal.valueOf(lost).divide(
                    BigDecimal.valueOf(pingLogsInDateTimeRange.size()),
                    2,
                    RoundingMode.CEILING);
    }

    /**
     * Retrieve all the pings from the CSV file
     * @return List of pings
     */
    private List<PingLog> readAll(){
        try (Reader reader = Files.newBufferedReader(Path.of(path + "/ping.log"))) {
            CsvToBean<PingLog> cb = new CsvToBeanBuilder<PingLog>(reader)
                    .withType(PingLog.class)
                    .build();

            return cb.parse();
        } catch (IOException e) {
            logger.error("Tracerout error", e);
        }

        return List.of();
    }

    /**
     * Get a Stream of pings, applying a filter for retrieving only those pings related to a given ipAddress
     * @param st stream to apply the filtering
     * @param ipAddress IP address for filtering
     * @return Stream of pings with a filter applied
     */
    private Stream<PingLog> getPingLogsByIpStream(Stream<PingLog> st, String ipAddress){
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
    private Stream<PingLog> getPingLogsByDateTimeRangeByIp(Stream<PingLog> st, LocalDateTime start, LocalDateTime end, String ipAddress){
        return getPingLogsByDateTimeRange(
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
    private Stream<PingLog> getLostPingLogs(Stream<PingLog> st){
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
    private Stream<PingLog> getPingLogsByDateTimeRange(Stream<PingLog> st, LocalDateTime start, LocalDateTime end){
        return st
                .filter(pingLog ->
                        (pingLog.getDateTime().isAfter(start) || pingLog.getDateTime().isEqual(start))
                                && (pingLog.getDateTime().isBefore(end) || pingLog.getDateTime().isEqual(end))
                );
    }
}
