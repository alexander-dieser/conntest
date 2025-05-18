package com.adieser.conntest.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotSupportedTracertProviderImplTest {
    @Mock
    Logger logger;

    @Test
    void executeTracert() {
        // when
        NotSupportedTracertProviderImpl tracertProvider = new NotSupportedTracertProviderImpl(logger);

        // then
        Optional<BufferedReader> bufferedReader = tracertProvider.executeTracert();

        // assert
        assertTrue(bufferedReader.isEmpty());
        verify(logger, times(1)).warn(contains("TracertProvider not supported for"), anyString());
    }
}
