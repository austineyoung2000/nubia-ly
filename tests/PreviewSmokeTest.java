package com.elitedarkkaiser.nubialy;
public final class PreviewSmokeTest {
    private static final String BOOT = "12345678-1234-1234-1234-123456789abc";
    private static final String OTHER = "87654321-1234-1234-1234-123456789abc";
    public static void main(String[] args) {
        String receipt = "nubia-ly-preview-v1\n" + BOOT + "\n61\nlifecycle-only\n" + OTHER + "\n";
        require(PreviewPolicy.parse(receipt, BOOT, 1000) != null);
        require(PreviewPolicy.parse(receipt, OTHER, 1000) == null);
        require(PreviewPolicy.parse(receipt, BOOT, 61000) == null);
        require(PreviewPolicy.parse(receipt, BOOT, 999) == null);
        require(PreviewPolicy.parse(receipt.replace("lifecycle-only", "cosmetic"), BOOT, 1000) == null);
        require(PreviewPolicy.parse("nubia-ly-probe-v1\n" + BOOT + "\n61\nprobe-only\n", BOOT, 1000) == null);
        require(PreviewPolicy.parse(receipt + "junk", BOOT, 1000) == null);
        PreviewLifecycle session = new PreviewLifecycle();
        require(session.state(1000) == PreviewLifecycle.Phase.IDLE);
        long first = session.begin();
        session.stop();
        require(!session.activate(first, new PreviewPolicy.Permit(OTHER, 61000), 1000));
        long second = session.begin();
        require(session.activate(second, new PreviewPolicy.Permit(OTHER, 61000), 1000));
        require(session.validate(second, new PreviewPolicy.Permit(OTHER, 61000), 2000));
        require(!session.validate(second, new PreviewPolicy.Permit(OTHER, 62000), 3000));
        require(session.state(3000) == PreviewLifecycle.Phase.STOPPED);
        long third = session.begin();
        require(session.activate(third, new PreviewPolicy.Permit(OTHER, 61000), 1000));
        require(session.state(61000) == PreviewLifecycle.Phase.EXPIRED);
        require(!session.activate(third, new PreviewPolicy.Permit(OTHER, 121000), 61000));
        long fourth = session.begin();
        long fifth = session.begin();
        require(!session.activate(fourth, new PreviewPolicy.Permit(OTHER, 61000), 1000));
        require(!session.activate(fifth, null, 1000));
        require(session.state(1000) == PreviewLifecycle.Phase.STOPPED);
        System.out.println("Preview lifecycle tests passed: distinct purpose/boot/deadline, late-worker fencing, stop, expiry and no permit replacement/extension.");
    }
    private static void require(boolean condition) { if (!condition) throw new AssertionError(); }
}
