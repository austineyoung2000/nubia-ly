package com.elitedarkkaiser.nubialy;
import org.junit.Test;
import static org.junit.Assert.*;
public class CompatibilityPolicyTest {
    @Test public void launcherPlatformAllowsRm10AndRm11OnlyOnAndroid16() {
        assertTrue(CompatibilityPolicy.isLauncherPlatform("NX809J", "NX809J", 36));
        assertTrue(CompatibilityPolicy.isLauncherPlatform("NX789J", "NX789J", 36));
        assertTrue(CompatibilityPolicy.isLauncherPlatform("NX789J-UN", "NX789J", 36));
        assertFalse(CompatibilityPolicy.isLauncherPlatform("NX789J", "NX789J", 35));
        assertFalse(CompatibilityPolicy.isLauncherPlatform("NX809J", "NX809J", 37));
        assertFalse(CompatibilityPolicy.isLauncherPlatform("NX769J", "NX769J", 36));
        assertFalse(CompatibilityPolicy.isLauncherPlatform(null, null, 36));
    }
    @Test public void rejectsOtherModelsEvenWithMatchingFirmware() {
        assertEquals(CompatibilityPolicy.Status.UNSUPPORTED_DEVICE, CompatibilityPolicy.classify("NX769J", "NX769J", 36, "example/research:16/test", "synthetic-test-hash", "example/research:16/test", "synthetic-test-hash"));
    }
    @Test public void missingHashCannotMatch() {
        assertEquals(CompatibilityPolicy.Status.INSPECTION_UNAVAILABLE, CompatibilityPolicy.classify("NX809J", "NX809J", 36, "example/research:16/test", null, "example/research:16/test", "synthetic-test-hash"));
    }
    @Test public void changedFirmwareAndApkRequireValidation() {
        assertEquals(CompatibilityPolicy.Status.UNRECOGNIZED_FIRMWARE, CompatibilityPolicy.classify("NX809J", "NX809J", 36, "changed", "synthetic-test-hash", "example/research:16/test", "synthetic-test-hash"));
        assertEquals(CompatibilityPolicy.Status.UNRECOGNIZED_FIRMWARE, CompatibilityPolicy.classify("NX809J", "NX809J", 36, "example/research:16/test", "changed", "example/research:16/test", "synthetic-test-hash"));
    }
    @Test public void baselineNeverEnablesAnyFeature() {
        var status = CompatibilityPolicy.classify("NX809J", "NX809J", 36, "example/research:16/test", "synthetic-test-hash", "example/research:16/test", "synthetic-test-hash");
        assertEquals(CompatibilityPolicy.Status.RESEARCH_BASELINE, status);
        for (var feature : FeatureCatalog.FEATURES) assertFalse(FeatureCatalog.canApply(feature.id(), status));
        assertFalse(FeatureCatalog.canApply("unknown", status));
    }
}
