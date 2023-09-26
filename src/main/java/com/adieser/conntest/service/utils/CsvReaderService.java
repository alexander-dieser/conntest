package com.adieser.conntest.service.utils;

import com.adieser.conntest.controllers.responses.PingLog;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;

@Service
public class CsvReaderService {
    private static final int DATE = 0;
    public static final int LEVEL = 1;
    private static final int IP = 2;
    private static final int TIME = 3;
    public List<PingLog> getAll(File file) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {

            return reader.readAll().stream()
                    .map(line ->
                        PingLog.builder()
                                .date(parseDate(line[DATE]))
                                .logLevel(line[LEVEL])
                                .ipAddress(line[IP])
                                .time(Long.parseLong(line[TIME]))
                                .build()
                    )
                    .toList();

        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
    }

    public List<PingLog> getAvg(File file, Predicate<String[]> filterMethod) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {

            return reader.readAll().stream()
                    .filter(filterMethod)
                    .map(line ->
                            PingLog.builder()
                                    .date(parseDate(line[DATE]))
                                    .logLevel(line[LEVEL])
                                    .ipAddress(line[IP])
                                    .time(Long.parseLong(line[TIME]))
                                    .build()
                    )
                    .toList();

        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
    }

    public LocalDateTime parseDate(String dateTime){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return LocalDateTime.parse(dateTime, formatter);
    }
}
