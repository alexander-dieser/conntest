package com.adieser.conntest.views;

import com.adieser.conntest.service.ConnTestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UiControllerTest {
    @Mock
    ConnTestService connTestService;
    @Mock
    Logger logger;

    @Test
    void startTest() {
        //when
        UiController underTestSpy = spy(new UiController(connTestService, logger));
        List<String> mockIpAddressList = List.of("192.168.1.1", "192.168.1.2", "8.8.8.8");
        when(connTestService.getIpAddressesFromActiveTests()).thenReturn(mockIpAddressList);
        doNothing().when(underTestSpy).updateVisualControls();

        // then
        underTestSpy.start(5);

        // assert
        verify(underTestSpy, times(1)).updateVisualControls();
        verify(connTestService, times(1)).testLocalISPInternet();
        verify(connTestService, times(1)).getIpAddressesFromActiveTests();
    }

    @Test
    void stopTest() {
        //when
        UiController underTest = new UiController(connTestService, logger);

        // then
        underTest.stop();

        // assert
        verify(connTestService, times(1)).stopTests();
    }
}