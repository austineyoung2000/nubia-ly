package com.elitedarkkaiser.nubialy;

/** Pure session state. A late validation can never restart a stopped or expired attempt. */
public final class PreviewLifecycle {
    public enum Phase { IDLE, CHECKING, ACTIVE, EXPIRED, STOPPED }
    private long generation;
    private Phase phase = Phase.IDLE;
    private PreviewPolicy.Permit permit;
    private boolean enabledMode;
    public long begin() { return begin(false); }
    public long begin(boolean enabled) { enabledMode = enabled; generation++; phase = Phase.CHECKING; permit = null; return generation; }
    public long generation() { return generation; }
    public boolean current(long ticket) { return ticket == generation; }
    public boolean activate(long ticket, PreviewPolicy.Permit candidate, long now) {
        if (!current(ticket) || phase != Phase.CHECKING) return false;
        if (!valid(candidate, now)) { stop(); return false; }
        permit = candidate; phase = Phase.ACTIVE; return true;
    }
    public boolean validate(long ticket, PreviewPolicy.Permit candidate, long now) {
        if (!current(ticket) || state(now) != Phase.ACTIVE) return false;
        if (!valid(candidate, now) || !(enabledMode ? candidate.id().equals(permit.id()) : candidate.equals(permit))) { stop(); return false; }
        if (enabledMode) permit = candidate;
        return true;
    }
    public Phase state(long now) {
        if (phase == Phase.ACTIVE && (now < 0 || now >= permit.deadlineMs())) {
            phase = Phase.EXPIRED; generation++;
        }
        return phase;
    }
    public long deadline() { return permit == null ? 0 : permit.deadlineMs(); }
    public void stop() { generation++; phase = Phase.STOPPED; permit = null; }
    private boolean valid(PreviewPolicy.Permit value, long now) {
        return value != null && PreviewPolicy.uuid(value.id()) && now >= 0
            && value.deadlineMs() > now && value.deadlineMs() - now <= (enabledMode ? ClockEnabledPolicy.HEALTH_MS : 60000);
    }
}
