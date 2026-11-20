# NothingWear Reverse Engineering Lab (`protocol-lab`)

This directory is strictly isolated from the main Android application (`app/`).
Research tools here (Python scripts, JADX notes, packet parsers, test vectors) must never be forced into the Android Gradle build.

## Subdirectories

- `apk-analysis/`: Notes and scripts for inspecting Mi Fitness APK compatibility checks (APKs themselves are git-ignored and must NEVER be committed).
- `ble-capture/`: Instructions and anonymized metadata for controlled Bluetooth HCI logs (raw captures `.btsnoop`, `.cfa`, `.pcap` are git-ignored and must NEVER be committed).
- `packet-parser/`: Desktop parser scripts built strictly from reproducible, verified captures.
- `protocol-docs/`: Evidence-backed findings and open uncertainties.
- `test-vectors/`: Redacted, sanitized test vectors for deterministic parser verification.

## Rules

- Do not commit APKs, credentials, MAC addresses, personal calendar contents, or raw captures.
- Do not fabricate protocol documentation.
- Do not start protocol writes to Xiaomi Watch S4 until M3 evidence is complete and approved.
