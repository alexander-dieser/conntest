package com.adieser.conntest.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

public class WindowsTracertProviderImpl implements TracertProvider {
    @Override
    public Optional<BufferedReader> executeTracert() throws IOException {
        Process process = getRuntime().exec("tracert -h 2 8.8.8.8");
        return Optional.of(getBufferedReader(process));
    }

    BufferedReader getBufferedReader(Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    Runtime getRuntime() {
        return Runtime.getRuntime();
    }
}
