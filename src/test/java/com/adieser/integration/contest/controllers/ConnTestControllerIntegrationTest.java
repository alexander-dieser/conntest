package com.adieser.integration.contest.controllers;

import com.adieser.conntest.ConntestApplication;
import com.adieser.conntest.controllers.ConnTestController;
import com.adieser.conntest.controllers.responses.PingSessionExtract;
import com.adieser.conntest.models.PingLog;
import com.adieser.conntest.service.ConnTestService;
import com.adieser.conntest.utils.HATEOASLinkRelValueTemplates;
import com.adieser.utils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static com.adieser.conntest.controllers.responses.ErrorResponse.MAX_IP_ADDRESSES_EXCEEDED;
import static com.adieser.utils.TestUtils.DEFAULT_LOG_DATE_TIME;
import static com.adieser.utils.TestUtils.DEFAULT_PING_TIME;
import static com.adieser.utils.TestUtils.LOCAL_IP_ADDRESS;
import static com.adieser.utils.TestUtils.getDefaultPingLog;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
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
        // when
        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.post("/test-local-isp-cloud")
        );

        // then
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.stopTestSession.href")
                        .value(linkTo(methodOn(ConnTestController.class)
                                .stopTests()).toString()));

        // assert
        verify(connTestService, times(1)).testLocalISPInternet();
    }

    @ParameterizedTest
    @MethodSource("ipAddressesProvider")
    void testCustomIps(List<String> ipAddresses) throws Exception {
        // when
        String requestBody = new ObjectMapper().writeValueAsString(ipAddresses);

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.post("/test-custom-ips")
        );

        // then
        ResultActions resultActions = mockMvc.perform(requestBuilder
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON)
        );

        // assert
        if(ipAddresses.size() < 4){
            resultActions
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.stopTestSession.href")
                            .value(linkTo(methodOn(ConnTestController.class)
                                    .stopTests()).toString()));

            verify(connTestService, times(1)).testCustomIps(ipAddresses);
        } else{
            resultActions
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(1))
                    .andExpect(jsonPath("$.errorMessage").value(MAX_IP_ADDRESSES_EXCEEDED.getErrorMessage()));
        }
    }

    @Test
    void stopTests() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.post("/stop-tests")
        );

        // then
        mockMvc.perform(requestBuilder)
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
        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings")
        );

        ResultActions resultActions = performRequestAndBasicAssertPingLogControllerResponse(requestBuilder, pings);
        if(!pings.getPingLogs().isEmpty())
            AssertHATEOASLinks(resultActions, pings.getPingLogs().get(0));

        verify(connTestService, times(1)).getPings();
    }

    @Test
    void testGetPingsIOException() throws Exception {
        // when
        when(connTestService.getPings()).thenThrow(IOException.class);
        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings")
        );

        // then assert
        mockMvc.perform(requestBuilder)
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("Internal Server Error"));
    }

    @ParameterizedTest
    @MethodSource("responseProvider")
    void getPingsByDateTimeRange(PingSessionExtract pings) throws Exception {
        // when
        when(connTestService.getPingsByDateTimeRange(START, END))
                .thenReturn(pings);

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings/{start}/{end}",
                        START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        );

        // then assert
        ResultActions resultActions = performRequestAndBasicAssertPingLogControllerResponse(requestBuilder, pings);
        if(!pings.getPingLogs().isEmpty())
            AssertHATEOASLinks(resultActions, pings.getPingLogs().get(0));

        verify(connTestService, times(1)).getPingsByDateTimeRange(START, END);
    }

    @Test
    void testGetPingsByDateTimeRangeIOException() throws Exception {
        // when
        when(connTestService.getPingsByDateTimeRange(any(), any())).thenThrow(IOException.class);

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings/{start}/{end}",
                        START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        );

        // then assert
        mockMvc.perform(requestBuilder)
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("Internal Server Error"));
    }

    @ParameterizedTest
    @MethodSource("responseProvider")
    void getPingsByIp(PingSessionExtract pings) throws Exception {
        // when
        when(connTestService.getPingsByIp(LOCAL_IP_ADDRESS))
                .thenReturn(pings);

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings/{ipAddress}", LOCAL_IP_ADDRESS)
        );

        // then assert
        ResultActions resultActions = performRequestAndBasicAssertPingLogControllerResponse(requestBuilder, pings);
        if(!pings.getPingLogs().isEmpty())
            AssertHATEOASLinks(resultActions, pings.getPingLogs().get(0));

        verify(connTestService, times(1)).getPingsByIp(LOCAL_IP_ADDRESS);
    }

    @Test
    void testGetPingsByIpIOException() throws Exception {
        // when
        when(connTestService.getPingsByIp(LOCAL_IP_ADDRESS)).thenThrow(IOException.class);

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings/{ipAddress}", LOCAL_IP_ADDRESS)
        );

        // then assert
        mockMvc.perform(requestBuilder)
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("Internal Server Error"));
    }

    @ParameterizedTest
    @MethodSource("responseProvider")
    void getPingsByDateTimeRangeByIp(PingSessionExtract pings) throws Exception {
        // when
        when(connTestService.getPingsByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS))
                .thenReturn(pings);

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings/{ipAddress}/{start}/{end}",
                        LOCAL_IP_ADDRESS,
                        START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        );

        // then assert
        ResultActions resultActions = performRequestAndBasicAssertPingLogControllerResponse(requestBuilder, pings);
        if(!pings.getPingLogs().isEmpty())
            AssertHATEOASLinks(resultActions, pings.getPingLogs().get(0));

        verify(connTestService, times(1)).getPingsByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS);
    }

    @Test
    void testGetPingsByDateTimeRangeByIpIOException() throws Exception {
        // when
        when(connTestService.getPingsByDateTimeRangeByIp(any(), any(), anyString())).thenThrow(IOException.class);

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings/{ipAddress}/{start}/{end}",
                        LOCAL_IP_ADDRESS,
                        START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        );

        // then assert
        mockMvc.perform(requestBuilder)
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("Internal Server Error"));
    }

    @Test
    void getPingsLostAvgByIp() throws Exception {
        // when
        when(connTestService.getPingsLostAvgByIp(LOCAL_IP_ADDRESS))
                .thenReturn(BigDecimal.valueOf(0.3));

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings/{ipAddress}/avg", LOCAL_IP_ADDRESS)
        );

        // then assert
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
    void testGetPingsLostAvgByIpIOException() throws Exception {
        // when
        when(connTestService.getPingsLostAvgByIp(LOCAL_IP_ADDRESS)).thenThrow(IOException.class);

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings/{ipAddress}/avg", LOCAL_IP_ADDRESS)
        );

        // then assert
        mockMvc.perform(requestBuilder)
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("Internal Server Error"));
    }

    @Test
    void getPingsLostAvgByDateTimeRangeByIp() throws Exception {
        // when
        when(connTestService.getPingsLostAvgByDateTimeRangeByIp(START, END, LOCAL_IP_ADDRESS))
                .thenReturn(BigDecimal.valueOf(0.3));

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings/{ipAddress}/avg/{start}/{end}",
                        LOCAL_IP_ADDRESS,
                        START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        );

        // then assert
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

    @Test
    void testGetPingsLostAvgByDateTimeRangeByIp() throws Exception {
        // when
        when(connTestService.getPingsLostAvgByDateTimeRangeByIp(any(), any(), anyString())).thenThrow(IOException.class);

        MockHttpServletRequestBuilder requestBuilder = TestUtils.addCustomSessionId(
                MockMvcRequestBuilders.get("/pings/{ipAddress}/avg/{start}/{end}",
                        LOCAL_IP_ADDRESS,
                        START.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        END.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        );

        // then assert
        mockMvc.perform(requestBuilder)
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("Internal Server Error"));
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

    static Stream<List<String>> ipAddressesProvider() {
        return Stream.of(
                List.of("1.1.1.1", "2.2.2.2", "3.3.3.3"),
                List.of("1.1.1.1", "2.2.2.2", "3.3.3.3", "4.4.4.4")
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
