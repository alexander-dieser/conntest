package com.adieser.conntest.models.utils;

import org.springframework.stereotype.Component;

@Component
public interface Reachable {
    boolean isReachable(String ip);
}
