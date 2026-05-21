package org.example.persion.service;

/**
 * Tracks external map API usage so location refreshes can stay within quotas.
 */
public interface ApiCallLimitService {

    boolean canCallApi();

    void recordApiCall();

    String getCallStats();

    int getRemainingDailyCalls();

    int getRemainingHourlyCalls();
}
