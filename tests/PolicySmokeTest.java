package com.elitedarkkaiser.nubialy;
public final class PolicySmokeTest {
    public static void main(String[] args) {
        var good = CompatibilityPolicy.classify("NX809J", "NX809J", 36, "example/research:16/test", "synthetic-test-hash", "example/research:16/test", "synthetic-test-hash");
        require(good == CompatibilityPolicy.Status.RESEARCH_BASELINE);
        require(CompatibilityPolicy.classify("NX789J", "NX789J", 36, "example/research:16/test", "synthetic-test-hash", "example/research:16/test", "synthetic-test-hash") == CompatibilityPolicy.Status.UNSUPPORTED_DEVICE);
        require(CompatibilityPolicy.classify("NX809J", "NX809J", 36, "example/research:16/test", null, "example/research:16/test", "synthetic-test-hash") == CompatibilityPolicy.Status.INSPECTION_UNAVAILABLE);
        require(CompatibilityPolicy.classify("NX809J", "NX809J", 36, "other", "synthetic-test-hash", "example/research:16/test", "synthetic-test-hash") == CompatibilityPolicy.Status.UNRECOGNIZED_FIRMWARE);
        require(CompatibilityPolicy.classify("NX809J", "NX809J", 35, "example/research:16/test", "synthetic-test-hash", "example/research:16/test", "synthetic-test-hash") == CompatibilityPolicy.Status.UNRECOGNIZED_FIRMWARE);
        require(CompatibilityPolicy.classify("NX809J", "NX809J", 36, "example/research:16/test", "different", "example/research:16/test", "synthetic-test-hash") == CompatibilityPolicy.Status.UNRECOGNIZED_FIRMWARE);
        require(CompatibilityPolicy.classify("NX809J", "NX809J", 36, "example/research:16/test", "synthetic-test-hash", "", "") == CompatibilityPolicy.Status.UNRECOGNIZED_FIRMWARE);
        for (var feature : FeatureCatalog.FEATURES) require(!FeatureCatalog.canApply(feature.id(), good));
        require(!FeatureCatalog.canApply("unknown", good));
        for (int mask = 0; mask < 32; mask++) {
            boolean stopped = (mask & 1) != 0;
            boolean active = (mask & 2) != 0;
            boolean baseline = (mask & 4) != 0;
            boolean recovery = (mask & 8) != 0;
            boolean feature = (mask & 16) != 0;
            require(RecoveryPolicy.canApply(stopped, active, baseline, recovery, feature) == (mask == 30));
        }
        require(!ModuleStatus.isActive());
        String boot = "12345678-1234-1234-1234-123456789abc";
        String probe = "nubia-ly-probe-v1\n" + boot + "\n160\nprobe-only\n";
        require(GuardPolicy.probeValid(probe, boot, 100));
        require(!GuardPolicy.probeValid(probe, boot, 160));
        require(!GuardPolicy.probeValid(probe, boot, 99));
        require(!GuardPolicy.probeValid(probe, "87654321-1234-1234-1234-123456789abc", 100));
        require(!GuardPolicy.probeValid(probe.replace("probe-only", "apply"), boot, 100));
        require(!GuardPolicy.probeValid(null, boot, 100));
        require(!GuardPolicy.probeValid(probe + "unexpected", boot, 100));
        require(SystemUiGuardProtocol.accepts(boot, boot, 4, 4, 8000));
        require(!SystemUiGuardProtocol.accepts(boot, boot, 4, 4, 8001));
        require(!SystemUiGuardProtocol.accepts(boot, boot, 4, 3, 100));
        require(!SystemUiGuardProtocol.accepts(boot, null, 4, 4, 100));
        require(!SystemUiGuardProtocol.accepts(boot, "87654321-1234-1234-1234-123456789abc", 4, 4, 100));
        require(!SystemUiGuardProtocol.accepts(boot, boot, 4, 4, -1));
        require(GuardBrokerPolicy.permits(10042, 10042, "com.android.systemui"));
        require(!GuardBrokerPolicy.permits(10043, 10042, "com.android.systemui"));
        require(!GuardBrokerPolicy.permits(10042, 10042, "other.package"));
        require(!GuardBrokerPolicy.permits(10042, 10042, null));
        require(!GuardBrokerPolicy.permits(0, -1, "com.android.systemui"));
        require(!GuardBrokerPolicy.validStatus(GuardBrokerPolicy.UNAVAILABLE));
        require(!GuardBrokerPolicy.validStatus(999));
        System.out.println("Policy smoke tests passed: device, firmware, SDK, hash and no-apply gates.");
    }
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
}
