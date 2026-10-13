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
        System.out.println("Policy smoke tests passed: device, firmware, SDK, hash and no-apply gates.");
    }
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
}
