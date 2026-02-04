package com.freelkee.btcbalancechecker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchJob {
    private String id;
    private String currency;
    private int offset;
    private List<String> addresses;
    private List<Wallet> results;
    private AtomicInteger processed = new AtomicInteger(0);
    private AtomicInteger failed = new AtomicInteger(0);
    private boolean done = false;
    private boolean cancelled = false;

    // per-address status and results
    private java.util.concurrent.ConcurrentMap<String, String> statuses = new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.concurrent.ConcurrentMap<String, Wallet> resultsMap = new java.util.concurrent.ConcurrentHashMap<>();

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public java.util.concurrent.ConcurrentMap<String, String> getStatuses() {
        return statuses;
    }

    public java.util.concurrent.ConcurrentMap<String, Wallet> getResultsMap() {
        return resultsMap;
    }
}
