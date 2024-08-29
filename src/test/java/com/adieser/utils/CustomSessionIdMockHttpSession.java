package com.adieser.utils;

import org.springframework.mock.web.MockHttpSession;

/**
 * This class extends MockHttpSession and provides a simple way to set the session ID.
 * However, it is important to note that this class should not be used in production environments
 * due to the potential security concerns caused by modifying the session ID.
 */
public class CustomSessionIdMockHttpSession extends MockHttpSession {

    public CustomSessionIdMockHttpSession(String sessionId) {
        super();
        setSessionId(sessionId);
    }

    private void setSessionId(String sessionId) {
        super.changeSessionId();
        try {
            java.lang.reflect.Field sessionIdField = MockHttpSession.class.getDeclaredField("id");
            sessionIdField.setAccessible(true);
            sessionIdField.set(this, sessionId);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}