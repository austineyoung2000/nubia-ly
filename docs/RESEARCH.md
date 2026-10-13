# Stock System UI research

Public source contains no owner-specific firmware fingerprints, APK hashes, uploaded logs or crash details. Keep research inputs outside the public repository. An optional ignored `local-baseline.properties` provides `fingerprint` and `systemUiSha256` for explicit local inspection. Without a configured baseline, the app reports unvalidated firmware.

Each feature must trace the actual stock implementation, its dependencies, layout constraints and lifecycle before activation. Class/resource presence is not proof that an override works. Quick Settings row changes remain blocked pending feature-specific compatibility and recovery validation.
