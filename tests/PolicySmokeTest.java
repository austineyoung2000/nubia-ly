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
        System.out.println("Policy smoke tests passed: device, firmware, SDK, hash and no-apply gates.");
    }
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
}
