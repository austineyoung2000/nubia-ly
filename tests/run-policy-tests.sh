#!/bin/sh
set -eu
repo=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
java com.sun.tools.javac.Main -d "$work" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/CompatibilityPolicy.java" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/FeatureCatalog.java" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/RecoveryPolicy.java" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/ModuleStatus.java" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/GuardPolicy.java" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/SystemUiGuardProtocol.java" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/GuardBrokerPolicy.java" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/PreviewPolicy.java" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/PreviewLifecycle.java" \
 "$repo/tests/PolicySmokeTest.java" \
 "$repo/tests/PreviewSmokeTest.java" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/ClockCapture.java" \
 "$repo/tests/ClockCaptureSmokeTest.java"
java -cp "$work" com.elitedarkkaiser.nubialy.PolicySmokeTest

java -cp "$work" com.elitedarkkaiser.nubialy.PreviewSmokeTest

java -cp "$work" com.elitedarkkaiser.nubialy.ClockCaptureSmokeTest
