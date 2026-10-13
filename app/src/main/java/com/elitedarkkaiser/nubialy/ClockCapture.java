package com.elitedarkkaiser.nubialy;

/** Read-only rehearsal ledger. Keeps the latest stock value, never grants apply authority. */
public final class ClockCapture<T> {
    private T original, latest;
    private boolean captured;
    private long updates;
    public void begin(T value) {
        if (value == null) throw new IllegalArgumentException("Missing stock color");
        original = latest = value; updates = 0; captured = true;
    }
    public void observe(T value) {
        if (!captured || value == null) return;
        if (!value.equals(latest)) updates++;
        latest = value;
    }
    public T release() {
        T target = latest;
        captured = false; original = latest = null;
        return target;
    }
    public boolean captured() { return captured; }
    public long updates() { return updates; }
    public boolean unchanged() { return captured && original.equals(latest); }
}
