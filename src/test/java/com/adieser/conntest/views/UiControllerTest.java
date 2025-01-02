package com.adieser.conntest.views;

import com.adieser.conntest.service.ConnTestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UiControllerTest {
    @Mock
    ConnTestService connTestService;
    @Mock
    Logger logger;
    @Mock
    ScheduledExecutorService executorService;

    @Test
    void startTest() {
        //when
        UiController underTestSpy = spy(new UiController(connTestService, logger, executorService));
        List<String> mockIpAddressList = List.of("192.168.1.1", "192.168.1.2", "8.8.8.8");
        when(connTestService.getIpAddressesFromActiveTests()).thenReturn(mockIpAddressList);
        doNothing().when(underTestSpy).updateVisualControls();
        doNothing().when(underTestSpy).setTables();

        // then
        underTestSpy.start();

        // assert
        verify(connTestService, times(1)).testLocalISPInternet();
        verify(connTestService, times(1)).getIpAddressesFromActiveTests();
        verify(underTestSpy, times(1)).updateVisualControls();
        verify(underTestSpy, times(1)).setTables();
        verify(underTestSpy, times(1)).startExecutorService(any());
    }

    @Test
    void executorServiceTest(){
        // when
        UiController underTestSpy = spy(new UiController(connTestService, logger, executorService));

        // then
        underTestSpy.startExecutorService(1);

        // assert
        verify(executorService, atLeastOnce()).scheduleAtFixedRate(
                any(Runnable.class),
                eq(0L),
                eq(1L),
                eq(SECONDS)
        );
    }

    @Test
    void stopTest() {
        //when
        UiController underTest = new UiController(connTestService, logger, executorService);

        // then
        underTest.stopExecutorService();
        connTestService.stopTests();

        // assert
        verify(executorService, times(1)).shutdownNow();
    }

    @Test
    void stopExecutorServiceTest() {
        // Case 1: executorService is null and is shutdown
        //when
        UiController underTest = new UiController(connTestService, logger, null);

        //then
        underTest.stopExecutorService();

        // assert
        verify(executorService, never()).shutdownNow();

        // Case 2: executorService is not null and is shutdown
        //when
        reset(executorService);
        underTest = new UiController(connTestService, logger, executorService);
        when(executorService.isShutdown()).thenReturn(true);

        // then
        underTest.stopExecutorService();

        // assert
        verify(executorService, never()).shutdownNow();

        // Case 3: executorService is not null and is not shutdown
        //when
        reset(executorService);
        when(executorService.isShutdown()).thenReturn(false);

        //then
        underTest.stopExecutorService();

        // assert
        verify(executorService, times(1)).shutdownNow();


    }

    @Test
    void stopExecutorServiceTest2() {
        //when
        UiController underTest = new UiController(connTestService, logger, executorService);

        // then
        underTest.stopExecutorService();

        // assert
        verify(executorService, times(1)).shutdownNow();
    }
}