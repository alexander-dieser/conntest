package com.adieser.conntest.models;

/**
 *  Defines the contract for objects or components that are capable of initiating and terminating a ping session,
 *  where a ping session is a series of ping-like commands
 *  Implementing classes must provide functionality to start and stop ping sessions.
 */
public interface Pingable {

    /**
     * Initiates a ping session.
     * This method triggers the process of sending ping-like requests to a target, such as a server or network device.
     */
    void startPingSession();

    /**
     * Terminates an ongoing ping session.
     * This method stops the process of sending ping requests. Any necessary cleanup and result processing should be
     * performed here.
     */
    void stopPingSession();
}
