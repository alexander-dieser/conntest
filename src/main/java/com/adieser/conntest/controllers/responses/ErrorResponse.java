package com.adieser.conntest.controllers.responses;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    public static final ErrorResponse MAX_IP_ADDRESSES_EXCEEDED = ErrorResponse.builder()
            .errorCode(1L)
            .errorMessage("Max IP Addresses exceeded (3)")
            .build();

    private Long errorCode;
    private String errorMessage;
}
