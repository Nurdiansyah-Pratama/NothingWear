#!/usr/bin/env python3
"""
NothingWear M3 Read-Only APK Metadata & Interoperability Symbol Scanner.

Inspects a legitimately obtained local APK (stored inside git-ignored
protocol-lab/apk-analysis/) using Python's standard library only:
  - Computes SHA-256 hash and archive structure summary.
  - Parses DEX string tables (read-only) to locate class descriptors and
    system-property / protocol identifiers relevant to calendar/event
    interoperability (MIUI/HyperOS checks, CalendarContract, 0xFDAB, SPP).

Never modifies or repackages APKs and never exports proprietary code dumps.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import struct
import sys
import zipfile
from pathlib import Path

DEX_MAGIC_PREFIX = b"dex\n"

CLASS_PATTERNS = {
    "calendar_classes": re.compile(r"^L[^;]*(?:calendar|schedule|event)[^;]*;$", re.IGNORECASE),
    "os_check_classes": re.compile(r"^L[^;]*(?:miui|hyperos|osutil|romutil|deviceutil|brand|compat)[^;]*;$", re.IGNORECASE),
    "ble_spp_classes": re.compile(r"^L[^;]*(?:fdab|spp|rfcomm|gatt|miwear|wearable|fitness).*(?:sync|calendar|schedule|event|transport|channel|protocol)[^;]*;$", re.IGNORECASE),
}

STRING_PATTERNS = {
    "system_properties": re.compile(r"^(?:ro\.[a-z0-9_.-]+|persist\.sys\.[a-z0-9_.-]+)$"),
    "uuids": re.compile(r"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"),
    "calendar_uris_or_perms": re.compile(r"(?:content://[^\"'\s]*calendar[^\"'\s]*|permission\.[A-Z0-9_]*CALENDAR[A-Z0-9_]*)", re.IGNORECASE),
    "os_brand_literals": re.compile(r"^(?:xiaomi|redmi|poco|miui|hyperos|vela|coloros|flyme|originos|infinix|transsion|xos)$", re.IGNORECASE),
    "proto_or_sync_tags": re.compile(r"(?:calendar_sync|event_sync|schedule_sync|sync_calendar|sync_schedule|wear_calendar|calendar_event|schedule_info)", re.IGNORECASE),
}


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def read_uleb128(data: bytes, offset: int) -> tuple[int, int]:
    result = 0
    shift = 0
    while True:
        byte = data[offset]
        offset += 1
        result |= (byte & 0x7F) << shift
        if (byte & 0x80) == 0:
            break
        shift += 7
    return result, offset


def iter_dex_strings(dex_bytes: bytes):
    if not dex_bytes.startswith(DEX_MAGIC_PREFIX) or len(dex_bytes) < 0x70:
        return
    string_ids_size, string_ids_off = struct.unpack_from("<II", dex_bytes, 0x38)
    for i in range(string_ids_size):
        str_data_off = struct.unpack_from("<I", dex_bytes, string_ids_off + i * 4)[0]
        if str_data_off >= len(dex_bytes):
            continue
        try:
            _, pos = read_uleb128(dex_bytes, str_data_off)
            end = dex_bytes.index(b"\x00", pos)
            yield dex_bytes[pos:end].decode("utf-8", errors="ignore")
        except (ValueError, IndexError):
            continue


def scan_apk(apk_paths: list[Path]) -> dict:
    archives = []
    findings: dict[str, set[str]] = {k: set() for k in (*CLASS_PATTERNS.keys(), *STRING_PATTERNS.keys())}
    dex_total = 0
    native_libs: set[str] = set()

    for apk_path in apk_paths:
        archives.append({
            "fileName": apk_path.name,
            "sizeBytes": apk_path.stat().st_size,
            "sha256": sha256_file(apk_path),
        })
        with zipfile.ZipFile(apk_path, "r") as zf:
            for info in zf.infolist():
                name = info.filename
                if name.startswith("lib/") and name.endswith(".so"):
                    native_libs.add(Path(name).name)
                if name.endswith(".dex"):
                    dex_total += 1
                    dex_bytes = zf.read(name)
                    for s in iter_dex_strings(dex_bytes):
                        if not s or len(s) > 220:
                            continue
                        if s.startswith("L") and s.endswith(";"):
                            for key, regex in CLASS_PATTERNS.items():
                                if regex.match(s):
                                    findings[key].add(s)
                        else:
                            for key, regex in STRING_PATTERNS.items():
                                if regex.search(s):
                                    findings[key].add(s)

    return {
        "archives": archives,
        "dexCount": dex_total,
        "nativeLibrariesCount": len(native_libs),
        "nativeLibrariesSample": sorted(native_libs)[:40],
        "findings": {k: sorted(v) for k, v in findings.items()},
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Read-only APK metadata & interoperability symbol scanner for M3.")
    parser.add_argument("apks", nargs="+", help="Path(s) to local APK file(s) inside protocol-lab/apk-analysis/")
    parser.add_argument("--output", default=None, help="Optional JSON summary output path")
    args = parser.parse_args()

    paths = [Path(p) for p in args.apks]
    for p in paths:
        if not p.is_file():
            print(f"APK not found: {p}", file=sys.stderr)
            return 1

    report = scan_apk(paths)
    text = json.dumps(report, indent=2)
    if args.output:
        out = Path(args.output)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(text + "\n", encoding="utf-8")
        print(f"Wrote static metadata summary to {out}")
    else:
        print(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
