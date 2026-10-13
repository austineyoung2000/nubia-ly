package com.elitedarkkaiser.nubialy;
public final class ClockColorSmokeTest {
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        String id = "12345678-1234-1234-1234-123456789abc";
        String receipt = "nubia-ly-clock-v1\n" + id + "\n130\nclock-color-only\n" + id + "\n";
        require(ClockPermitPolicy.parse(receipt, id, 100000) != null);
        require(ClockPermitPolicy.parse(receipt, id, 99000) == null);
        require(ClockPermitPolicy.parse(receipt, id, 130000) == null);
        require(ClockPermitPolicy.parse(receipt.replace("clock-color-only", "lifecycle-only"), id, 100000) == null);
        require(ClockPermitPolicy.parse(receipt.replace("nubia-ly-clock-v1", "nubia-ly-preview-v1"), id, 100000) == null);
        require(PreviewPolicy.parse(receipt, id, 100000) == null);
        require(ClockPermitPolicy.parse(receipt, "00000000-1234-1234-1234-123456789abc", 100000) == null);
        ColorRestore<String> restore = new ColorRestore<>();
        restore.capture("light-state-list");
        require(restore.target().equals("light-state-list"));
        restore.stockWrite("dark-state-list");
        require(restore.target().equals("dark-state-list"));
        // An override is applied to the view, never recorded as a stock write.
        require(restore.target().equals("dark-state-list"));
        restore.restored(); restore.stockWrite("late"); require(restore.target() == null);
        restore.capture("new-view"); require(restore.target().equals("new-view"));
        System.out.println("Clock visual permit and latest-stock restoration checks passed");
    }
}
