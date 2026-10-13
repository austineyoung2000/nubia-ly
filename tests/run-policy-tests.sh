#!/bin/sh
set -eu
repo=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
java com.sun.tools.javac.Main -d "$work" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/CompatibilityPolicy.java" \
 "$repo/app/src/main/java/com/elitedarkkaiser/nubialy/FeatureCatalog.java" \
 "$repo/tests/PolicySmokeTest.java"
java -cp "$work" com.elitedarkkaiser.nubialy.PolicySmokeTest
