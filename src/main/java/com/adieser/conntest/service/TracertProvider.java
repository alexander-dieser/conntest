package com.adieser.conntest.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

public interface TracertProvider {
    Optional<BufferedReader> executeTracert() throws IOException;
}
