package com.adieser.integration.contest.controllers;

import com.adieser.conntest.ConntestApplication;
import com.adieser.conntest.controllers.responses.PingSessionResponseEntity;
import com.adieser.conntest.service.ConnTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.adieser.utils.PingLogUtils.DEFAULT_LOG_DATE_TIME;
import static com.adieser.utils.PingLogUtils.DEFAULT_PING_TIME;
import static com.adieser.utils.PingLogUtils.LOCAL_IP_ADDRESS;
import static com.adieser.utils.PingLogUtils.getDefaultPingLog;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ConntestApplication.class)
@AutoConfigureMockMvc
class ConnTestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ConnTestService connTestService;

    private final LocalDateTime START = DEFAULT_LOG_DATE_TIME.minusHours(1);
    private final LocalDateTime END = DEFAULT_LOG_DATE_TIME.plusHours(1);

    @Test
    void testLocalIspCloud() throws Exception {
        // then
        mockMvc.perform(MockMvcRequestBuilders.post("/test-local-isp-cloud"))
                .andExpect(status().isOk());

        // assert
        verify(connTestService, times(1)).testLocalISPInternet();
    }

    @Test
    void stopTests() throws Exception {
        // then
        mockMvc.perform(MockMvcRequestBuilders.post("/stop-tests"))
                .andExpect(status().isOk());

        // assert
        verify(connTestService, times(1)).stopTests();
    }

    @Test
    void getPings() throws Exception {
        // when
        when(connTestService.getPings()).thenReturn(getResponse());

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings");

        assertPingLogControllerResponse(requestBuilder);
        verify(connTestService, times(1)).getPings();
    }

    @Test
    void getPingsByDateTimeRange() throws Exception {
        // when
        when(connTestService.getPingsByDateTimeRange(START, END))
                .thenReturn(getResponse());

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings/{start}/{end}",
                START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        assertPingLogControllerResponse(requestBuilder);
        verify(connTestService, times(1)).getPingsByDateTimeRange(START, END);
    }

    @Test
    void getPingsByIp() throws Exception {
        // when
        when(connTestService.getPingsByIp(LOCAL_IP_ADDRESS))
                .thenReturn(getResponse());

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings/{ipAddress}", LOCAL_IP_ADDRESS);

        assertPingLogControllerResponse(requestBuilder);
        verify(connTestService, times(1)).getPingsByIp(LOCAL_IP_ADDRESS);
    }

    @Test
    void getPingsByDateTimeRangeByIp() throws Exception {
        // when
        when(connTestService.getPingsByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS))
                .thenReturn(getResponse());

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings/{ipAddress}/{start}/{end}",
                LOCAL_IP_ADDRESS,
                START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        assertPingLogControllerResponse(requestBuilder);
        verify(connTestService, times(1)).getPingsByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS);
    }

    @Test
    void getPingsLostAvgByIp() throws Exception {
        // when
        when(connTestService.getPingsLostAvgByIp(LOCAL_IP_ADDRESS))
                .thenReturn(BigDecimal.valueOf(0.3));

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings/{ipAddress}/avg", LOCAL_IP_ADDRESS);

        assertAverage(requestBuilder);
        verify(connTestService, times(1)).getPingsLostAvgByIp(LOCAL_IP_ADDRESS);
    }

    @Test
    void getPingsLostAvgByDateTimeRangeByIp() throws Exception {
        // when
        when(connTestService.getPingsLostAvgByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS))
                .thenReturn(BigDecimal.valueOf(0.3));

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings/{ipAddress}/avg/{start}/{end}",
                LOCAL_IP_ADDRESS,
                START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        assertAverage(requestBuilder);

        verify(connTestService, times(1)).getPingsLostAvgByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS);
    }

    private void assertPingLogControllerResponse(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        String jsonFormatExpectedDate = DEFAULT_LOG_DATE_TIME.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amountOfPings").value(1))
                .andExpect(jsonPath("$[0].pingLogs", hasSize(1)))
                .andExpect(jsonPath("$[0].pingLogs[0].dateTime").value(jsonFormatExpectedDate))
                .andExpect(jsonPath("$[0].pingLogs[0].ipAddress").value(LOCAL_IP_ADDRESS))
                .andExpect(jsonPath("$[0].pingLogs[0].pingTime").value(DEFAULT_PING_TIME));
    }

    private void assertAverage(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.average").value("0.3"));
    }

    private static List<PingSessionResponseEntity> getResponse() {
        return List.of(
                PingSessionResponseEntity.builder()
                        .amountOfPings(1)
                        .pingLogs(List.of(getDefaultPingLog()))
                        .build()
        );
    }
}
