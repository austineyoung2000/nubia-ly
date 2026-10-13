package com.elitedarkkaiser.nubialy;
public final class ClockCaptureSmokeTest {
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        ClockCapture<String> ledger = new ClockCapture<>();
        require(!ledger.captured() && ledger.release() == null);
        ledger.begin("light"); require(ledger.unchanged());
        ledger.observe("dark"); require(!ledger.unchanged() && ledger.updates() == 1);
        ledger.observe("dark"); require(ledger.updates() == 1);
        require(ledger.release().equals("dark") && !ledger.captured());
        ledger.observe("late"); require(ledger.release() == null);
        ledger.begin("fresh"); require(ledger.updates() == 0 && ledger.release().equals("fresh"));
        boolean rejected = false; try { ledger.begin(null); } catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected); System.out.println("Clock capture ledger checks passed");
    }
}
