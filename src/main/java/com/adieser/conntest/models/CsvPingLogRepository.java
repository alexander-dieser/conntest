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

public class CsvPingLogRepository implements PingLogRepository {
    private final Logger logger;
    private final String path;

    public CsvPingLogRepository(Logger logger, String path) {
        this.logger = logger;
        this.path = path;
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
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PingLog> findAllPingLogs() {
       return readAll();
    }

    @Override
    public List<PingLog> findPingLogByIp(String ipAddress) {
        return getPingLogsByIpStream(ipAddress)
                .toList();
    }

    @Override
    public List<PingLog> findPingLogsByDateTimeRange(LocalDateTime start, LocalDateTime end) {
        return null;
    }

    @Override
    public List<PingLog> findPingLogsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress) {
        return getPingLogsByDateTimeRangeByIp(start, end, ipAddress)
                .toList();
    }

    @Override
    public BigDecimal findLostPingLogsAvgByIP(String ipAddress) {
        List<PingLog> pingLogsByIp = getPingLogsByIpStream(ipAddress).toList();

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
        List<PingLog> pingLogsInDateTimeRange = getPingLogsByDateTimeRangeByIp(start, end, ipAddress)
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

    private List<PingLog> readAll(){
        try (Reader reader = Files.newBufferedReader(Path.of(path + "/ping.log"))) {
            CsvToBean<PingLog> cb = new CsvToBeanBuilder<PingLog>(reader)
                    .withType(PingLog.class)
                    .build();

            return cb.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<PingLog> getPingLogsByIpStream(String ipAddress){
        return readAll().stream()
                .filter(pingLog -> pingLog.getIpAddress().equals(ipAddress));
    }

    private Stream<PingLog> getPingLogsByDateTimeRangeByIp(LocalDateTime start, LocalDateTime end, String ipAddress){
        return getPingLogsByIpStream(ipAddress)
                .filter(pingLog ->
                        (pingLog.getDateTime().isAfter(start) || pingLog.getDateTime().isEqual(start))
                        && (pingLog.getDateTime().isBefore(end) || pingLog.getDateTime().isEqual(end))
                );
    }

    private Stream<PingLog> getLostPingLogs(Stream<PingLog> st){
        return st
                .filter(pingLog -> pingLog.getPingTime() < 0);
    }
}
