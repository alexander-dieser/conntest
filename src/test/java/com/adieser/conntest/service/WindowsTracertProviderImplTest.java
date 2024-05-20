package com.adieser.conntest.service;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class WindowsTracertProviderImplTest {

    @Test
    void testExecuteTracert() throws IOException {
        // when
        WindowsTracertProviderImpl underTest = spy(new WindowsTracertProviderImpl());
        Process mockProcess = mock(Process.class);

        Runtime mockRuntime = mock(Runtime.class);
        when(mockRuntime.exec(any(String.class))).thenReturn(mockProcess);

        doReturn(mockRuntime).when(underTest).getRuntime();

        BufferedReader mockReader = mock(BufferedReader.class);
        when(mockReader.readLine()).thenReturn("test");

        doReturn(mockReader).when(underTest).getBufferedReader(mockProcess);

        // then
        Optional<BufferedReader> bufferedReader = underTest.executeTracert();

        // assert
        assertEquals("test", bufferedReader.orElseThrow(IOException::new).readLine());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testExecuteTracertUsingRuntime() throws IOException {
        try (MockedStatic<Runtime> runtime = Mockito.mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream()) .thenReturn(mock(InputStream.class));
            doReturn(mockProcess).when(mockRuntime).exec(anyString());

            runtime.when(Runtime::getRuntime).thenReturn(mockRuntime);
        }

        WindowsTracertProviderImpl underTest = spy(new WindowsTracertProviderImpl());
        Optional<BufferedReader> bufferedReader = underTest.executeTracert();

        assertTrue(bufferedReader.isPresent());
    }
}
