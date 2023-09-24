package com.adieser.conntest.models.utils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JsonLog{
    private String dateTime;
    private String logLevel;
    private String message;
}
