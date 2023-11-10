package com.adieser.integration.contest.controllers;

import com.adieser.conntest.ConntestApplication;
import com.adieser.conntest.controllers.ConnTestController;
import com.adieser.conntest.controllers.responses.PingSessionExtract;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.service.ConnTestService;
import com.adieser.conntest.utils.HATEOASLinkRelValueTemplates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static com.adieser.utils.PingLogUtils.DEFAULT_LOG_DATE_TIME;
import static com.adieser.utils.PingLogUtils.DEFAULT_PING_TIME;
import static com.adieser.utils.PingLogUtils.LOCAL_IP_ADDRESS;
import static com.adieser.utils.PingLogUtils.getDefaultPingLog;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.stopTestSession.href")
                        .value(linkTo(methodOn(ConnTestController.class)
                                .stopTests()).toString()));

        // assert
        verify(connTestService, times(1)).testLocalISPInternet();
    }

    @Test
    void stopTests() throws Exception {
        // then
        mockMvc.perform(MockMvcRequestBuilders.post("/stop-tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.startTestSession.href")
                        .value(linkTo(methodOn(ConnTestController.class)
                                .testLocalIspCloud()).toString()));

        // assert
        verify(connTestService, times(1)).stopTests();
    }

    @ParameterizedTest
    @MethodSource("responseProvider")
    void getPings(PingSessionExtract pings) throws Exception {
        // when
        when(connTestService.getPings()).thenReturn(pings);

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings");

        ResultActions resultActions = performRequestAndBasicAssertPingLogControllerResponse(requestBuilder, pings);
        if(!pings.getPingLogs().isEmpty())
            AssertHATEOASLinks(resultActions, pings.getPingLogs().get(0));

        verify(connTestService, times(1)).getPings();
    }

    @ParameterizedTest
    @MethodSource("responseProvider")
    void getPingsByDateTimeRange(PingSessionExtract pings) throws Exception {
        // when
        when(connTestService.getPingsByDateTimeRange(START, END))
                .thenReturn(pings);

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings/{start}/{end}",
                START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        ResultActions resultActions = performRequestAndBasicAssertPingLogControllerResponse(requestBuilder, pings);
        if(!pings.getPingLogs().isEmpty())
            AssertHATEOASLinks(resultActions, pings.getPingLogs().get(0));

        verify(connTestService, times(1)).getPingsByDateTimeRange(START, END);
    }

    @ParameterizedTest
    @MethodSource("responseProvider")
    void getPingsByIp(PingSessionExtract pings) throws Exception {
        // when
        when(connTestService.getPingsByIp(LOCAL_IP_ADDRESS))
                .thenReturn(pings);

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings/{ipAddress}", LOCAL_IP_ADDRESS);

        ResultActions resultActions = performRequestAndBasicAssertPingLogControllerResponse(requestBuilder, pings);
        if(!pings.getPingLogs().isEmpty())
            AssertHATEOASLinks(resultActions, pings.getPingLogs().get(0));

        verify(connTestService, times(1)).getPingsByIp(LOCAL_IP_ADDRESS);
    }

    @ParameterizedTest
    @MethodSource("responseProvider")
    void getPingsByDateTimeRangeByIp(PingSessionExtract pings) throws Exception {
        // when
        when(connTestService.getPingsByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS))
                .thenReturn(pings);

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings/{ipAddress}/{start}/{end}",
                LOCAL_IP_ADDRESS,
                START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        ResultActions resultActions = performRequestAndBasicAssertPingLogControllerResponse(requestBuilder, pings);
        if(!pings.getPingLogs().isEmpty())
            AssertHATEOASLinks(resultActions, pings.getPingLogs().get(0));

        verify(connTestService, times(1)).getPingsByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS);
    }

    @Test
    void getPingsLostAvgByIp() throws Exception {
        // when
        when(connTestService.getPingsLostAvgByIp(LOCAL_IP_ADDRESS))
                .thenReturn(BigDecimal.valueOf(0.3));

        // then assert
        MockHttpServletRequestBuilder requestBuilder = get("/pings/{ipAddress}/avg", LOCAL_IP_ADDRESS);

        ResultActions resultActions = mockMvc.perform(requestBuilder)
                .andExpect( // [' and '] are meant for escaping the '.' due to json navigation errors
                    jsonPath("$._links.['%s'].href"
                            .formatted(HATEOASLinkRelValueTemplates.GET_BY_IP
                                    .formatted(LOCAL_IP_ADDRESS)
                            )
                    )
                    .value(linkTo(methodOn(ConnTestController.class).getPingsByIp(LOCAL_IP_ADDRESS)).toString())
                );

        assertAverage(resultActions);
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

        ResultActions resultActions = mockMvc.perform(requestBuilder)
                .andExpect( // [' and '] are meant for escaping the '.' due to json navigation errors
                    jsonPath("$._links.['%s'].href"
                            .formatted(HATEOASLinkRelValueTemplates.GET_30_MINS_NEIGHBORHOOD_BY_IP
                                    .formatted(LOCAL_IP_ADDRESS)))
                            .value(linkTo(methodOn(ConnTestController.class)
                                    .getPingsByDateTimeRangeByIp(LOCAL_IP_ADDRESS, START, END)).toString())
                );

        assertAverage(resultActions);

        verify(connTestService, times(1))
                .getPingsLostAvgByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS);
    }

    private ResultActions performRequestAndBasicAssertPingLogControllerResponse(MockHttpServletRequestBuilder requestBuilder,
                                                                                PingSessionExtract pings) throws Exception {
        String jsonFormatExpectedDate = DEFAULT_LOG_DATE_TIME.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        ResultActions result;
        if(pings.getPingLogs().isEmpty())
            result = mockMvc.perform(requestBuilder)
                    .andExpect(status().isNoContent())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.pingLogs", hasSize(0)));
        else
            result = mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.amountOfPings").value(1))
                    .andExpect(jsonPath("$.pingLogs", hasSize(1)))
                    .andExpect(jsonPath("$.pingLogs[0].dateTime").value(jsonFormatExpectedDate))
                    .andExpect(jsonPath("$.pingLogs[0].ipAddress").value(LOCAL_IP_ADDRESS))
                    .andExpect(jsonPath("$.pingLogs[0].pingTime").value(DEFAULT_PING_TIME));

        return result;
    }

    private void AssertHATEOASLinks(ResultActions resultActions, PingLog ping) {
        try {
            LocalDateTime floor = ping.getDateTime().minusMinutes(30);
            LocalDateTime roof = ping.getDateTime().plusMinutes(30);
            String ipAddress = ping.getIpAddress();

            resultActions
                    .andExpect(jsonPath("$.pingLogs[0].links[0].rel")
                            .value(HATEOASLinkRelValueTemplates
                                    .GET_BY_IP.formatted(ipAddress)))
                    .andExpect(jsonPath("$.pingLogs[0].links[0].href")
                            .value(linkTo(methodOn(ConnTestController.class)
                                    .getPingsByIp(ipAddress)).toString()))
                    .andExpect(jsonPath("$.pingLogs[0].links[1].rel")
                            .value(HATEOASLinkRelValueTemplates
                                    .GET_30_MINS_NEIGHBORHOOD))
                    .andExpect(jsonPath("$.pingLogs[0].links[1].href")
                            .value(linkTo(methodOn(ConnTestController.class)
                                    .getPingsByDateTimeRange(floor, roof)).toString()))
                    .andExpect(jsonPath("$.pingLogs[0].links[2].rel")
                            .value(HATEOASLinkRelValueTemplates
                                    .GET_30_MINS_NEIGHBORHOOD_BY_IP.formatted(ipAddress)))
                    .andExpect(jsonPath("$.pingLogs[0].links[2].href")
                            .value(linkTo(methodOn(ConnTestController.class)
                                    .getPingsByDateTimeRangeByIp(ipAddress, floor, roof)).toString()))
                    .andExpect(jsonPath("$.pingLogs[0].links[3].rel")
                            .value(HATEOASLinkRelValueTemplates
                                    .GET_LOST_AVG_BY_IP.formatted(ipAddress)))
                    .andExpect(jsonPath("$.pingLogs[0].links[3].href")
                            .value(linkTo(methodOn(ConnTestController.class)
                                    .getPingsLostAvgByIp(ipAddress)).toString()))
                    .andExpect(jsonPath("$.pingLogs[0].links[4].rel")
                            .value(HATEOASLinkRelValueTemplates
                                    .GET_30_MINS_NEIGHBORHOOD_LOST_AVG_BY_IP.formatted(ipAddress)))
                    .andExpect(jsonPath("$.pingLogs[0].links[4].href")
                            .value(linkTo(methodOn(ConnTestController.class)
                                    .getPingsLostAvgByDateTimeRangeByIp(ipAddress, floor, roof)).toString()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertAverage(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ipAddress").value(LOCAL_IP_ADDRESS))
                .andExpect(jsonPath("$.average").value("0.3"));
    }

    static Stream<PingSessionExtract> responseProvider() {
        return Stream.of(
                getEmptyResponse(),
                getDefaultResponse()
        );
    }

    private static PingSessionExtract getDefaultResponse() {
        return PingSessionExtract.builder()
                        .amountOfPings(1)
                        .pingLogs(List.of(getDefaultPingLog()))
                        .build();
    }

    private static PingSessionExtract getEmptyResponse() {
        return PingSessionExtract.builder()
                        .amountOfPings(0)
                        .pingLogs(List.of())
                        .build();
    }
}
