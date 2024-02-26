package com.adieser.conntest.views;

import com.adieser.conntest.service.ConnTestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    ScheduledExecutorService scheduledExecutorService;


    @Test
    @SuppressWarnings("unchecked")
    void testCreateExecutorService4() {
        scheduledExecutorService = mock(ScheduledExecutorService.class);
        when(scheduledExecutorService.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(),
                any(TimeUnit.class))).thenReturn(mock(ScheduledFuture.class));

        UiController underTest = spy(new UiController(connTestService, logger, executorService));

        // when
        underTest.createExecutorService(1);
        underTest.stop();

        // then
        verify(executorService, atLeastOnce()).scheduleAtFixedRate(
                any(Runnable.class),
                eq(0L),
                eq(1L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testCreateExecutorService3() {
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(12);
        Runnable task = new Runnable() {
            public void run() {
                try {
                    System.out.println(Thread.currentThread().getName() + ">> sleep..." + System.currentTimeMillis());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + ">> run....." + System.currentTimeMillis());
            }
        };
        ScheduledFuture<?> scheduleAtFixedRate = scheduledThreadPool.scheduleAtFixedRate(task, 0, 2, TimeUnit.SECONDS);
        scheduleAtFixedRate.cancel(false);

        verify(scheduledThreadPool, atLeastOnce()).scheduleAtFixedRate(
                any(Runnable.class),
                eq(0L),
                eq(1L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void testCreateExecutorService2() throws InterruptedException {
        // given
        UiController underTest = spy(new UiController(connTestService, logger, executorService));
        /*doNothing().doThrow(new RuntimeException()).when(executorService).scheduleAtFixedRate(
                any(Runnable.class),
                eq(0L),
                eq(1L),
                eq(TimeUnit.SECONDS)
        );

         */
        doNothing().when(underTest).loadLogs(any());
        doNothing().when(underTest).setAverageLost(any());

        // when
        underTest.createExecutorService(1);
        Thread.sleep(5000);
        underTest.stop();

        // then
        verify(executorService, atLeastOnce()).scheduleAtFixedRate(
                any(Runnable.class),
                eq(0L),
                eq(1L),
                eq(TimeUnit.SECONDS)
        );


    }

    @Test
    void testCreateExecutorService1() throws InterruptedException {
        // when
        UiController underTestSpy = spy(new UiController(connTestService, logger, executorService));
        //ScheduledFuture<?> scheduledFutureMock = mock(ScheduledFuture.class);

        when(executorService.scheduleAtFixedRate(
                any(Runnable.class),
                eq(0L),
                eq(1L),
                eq(SECONDS) )).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        });

        // then
        new Thread(() -> underTestSpy.createExecutorService(1)).start();
        Thread.sleep(5000);
        underTestSpy.stop();

        // assert
        verify(executorService, atLeastOnce()).scheduleAtFixedRate(
                any(Runnable.class),
                eq(0L),
                eq(1L),
                eq(SECONDS)
        );
    }

    @Test
    void testCreateExecutorService(){
        // when
        UiController underTest = new UiController(connTestService, logger, executorService);


        // then
        underTest.createExecutorService(1);

        // assert
        verify(executorService, atLeastOnce()).scheduleAtFixedRate(
                any(Runnable.class),
                eq(0L),
                eq(1L),
                eq(SECONDS)
        );
    }

    @Test
    void startTest() {
        //when
        UiController underTestSpy = spy(new UiController(connTestService, logger, executorService));
        List<String> mockIpAddressList = List.of("192.168.1.1", "192.168.1.2", "8.8.8.8");
        when(connTestService.getIpAddressesFromActiveTests()).thenReturn(mockIpAddressList);
        doNothing().when(underTestSpy).updateVisualControls();
        doNothing().when(underTestSpy).createExecutorService(any());

        // then
        underTestSpy.start(5);

        // assert
        verify(connTestService, times(1)).testLocalISPInternet();
        verify(connTestService, times(1)).getIpAddressesFromActiveTests();
        verify(underTestSpy, times(1)).updateVisualControls();
        verify(underTestSpy, times(1)).createExecutorService(any());
    }

    @Test
    void stopTest() {
        //when
        UiController underTest = new UiController(connTestService, logger, executorService);
        when(executorService.isShutdown()).thenReturn(false);

        // then
        underTest.stop();

        // assert
        verify(connTestService, times(1)).stopTests();
        verify(executorService, times(1)).shutdownNow();
    }
}