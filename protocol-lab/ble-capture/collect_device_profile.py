#!/usr/bin/env python3
"""
NothingWear M3 Comparative Device Environment Collector (Read-Only, Redacted).

Collects non-sensitive OS/manufacturer properties, Mi Fitness package version,
runtime permissions, and installed calendar package names via ADB for
Xiaomi 14T (Reference A) vs Infinix XOS 15 (Target B) comparison.

Never reads personal calendar entries, contacts, accounts, or raw MAC addresses.
"""

from __future__ import annotations

import argparse
import datetime
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

DEFAULT_ADB_CANDIDATES = [
    r"C:\Users\asus\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "adb",
]

OS_PROPERTIES = [
    "ro.product.manufacturer",
    "ro.product.brand",
    "ro.product.model",
    "ro.product.device",
    "ro.build.version.release",
    "ro.build.version.sdk",
    "ro.build.display.id",
    "ro.miui.ui.version.name",
    "ro.miui.ui.version.code",
    "ro.mi.os.version.name",
    "ro.mi.os.version.incremental",
    "ro.tranos.version",
    "ro.os.version",
]

CALENDAR_PACKAGES_TO_CHECK = [
    "com.google.android.calendar",
    "com.android.calendar",
    "com.miui.calendar",
    "com.xiaomi.calendar",
    "com.coloros.calendar",
    "com.transsion.calendar",
    "com.sh.smart.caller",
]

MI_FITNESS_CANDIDATES = [
    "com.xiaomi.wearable",
    "com.mi.health",
]


def find_adb() -> str:
    for candidate in DEFAULT_ADB_CANDIDATES:
        if os.path.isfile(candidate):
            return candidate
        resolved = shutil.which(candidate)
        if resolved:
            return resolved
    raise FileNotFoundError("adb executable not found. Add Android SDK platform-tools to PATH.")


def run_adb(adb_path: str, args: list[str], serial: str | None = None) -> str:
    cmd = [adb_path]
    if serial:
        cmd.extend(["-s", serial])
    cmd.extend(args)
    proc = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", check=False)
    return proc.stdout.strip()


def parse_mi_fitness_dumpsys(dumpsys_text: str) -> dict:
    version_name = None
    version_code = None
    min_sdk = None
    target_sdk = None
    runtime_perms: dict[str, str] = {}

    m_name = re.search(r"versionName=([^\s]+)", dumpsys_text)
    if m_name:
        version_name = m_name.group(1)

    m_code = re.search(r"versionCode=(\d+)\s+minSdk=(\d+)\s+targetSdk=(\d+)", dumpsys_text)
    if m_code:
        version_code = int(m_code.group(1))
        min_sdk = int(m_code.group(2))
        target_sdk = int(m_code.group(3))

    in_runtime = False
    for line in dumpsys_text.splitlines():
        if "runtime permissions:" in line:
            in_runtime = True
            continue
        if in_runtime:
            if line.strip() and not line.startswith("        ") and "granted=" not in line:
                in_runtime = False
                continue
            m_perm = re.search(r"([A-Za-z0-9_.]+\.permission\.[A-Z0-9_]+):\s+granted=(true|false)", line)
            if m_perm:
                perm_name = m_perm.group(1)
                if any(k in perm_name for k in ("CALENDAR", "BLUETOOTH", "NOTIFICATION", "LOCATION")):
                    runtime_perms[perm_name] = m_perm.group(2)

    return {
        "versionName": version_name,
        "versionCode": version_code,
        "minSdk": min_sdk,
        "targetSdk": target_sdk,
        "relevantRuntimePermissions": runtime_perms,
    }


def collect_profile(role: str, serial: str | None = None) -> dict:
    adb = find_adb()
    props = {}
    for prop in OS_PROPERTIES:
        val = run_adb(adb, ["shell", "getprop", prop], serial=serial)
        props[prop] = val if val else None

    all_packages_raw = run_adb(adb, ["shell", "pm", "list", "packages"], serial=serial)
    installed_packages = {
        line.replace("package:", "").strip()
        for line in all_packages_raw.splitlines()
        if line.startswith("package:")
    }

    mi_fitness_info = {}
    for pkg in MI_FITNESS_CANDIDATES:
        if pkg in installed_packages:
            dump = run_adb(adb, ["shell", "dumpsys", "package", pkg], serial=serial)
            mi_fitness_info[pkg] = parse_mi_fitness_dumpsys(dump)

    calendar_pkgs = sorted(
        pkg for pkg in installed_packages
        if pkg in CALENDAR_PACKAGES_TO_CHECK or "calendar" in pkg.lower()
    )

    return {
        "collectedAtUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "role": role,
        "serialRedacted": "REDACTED",
        "osProperties": props,
        "miFitnessPackages": mi_fitness_info,
        "installedCalendarPackages": calendar_pkgs,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Collect redacted Android device profile for M3 comparative analysis.")
    parser.add_argument("--role", required=True, help="Device label, e.g. 'Target_B_Infinix_XOS15' or 'Reference_A_Xiaomi_14T'")
    parser.add_argument("--serial", default=None, help="Optional ADB serial if multiple devices are attached")
    parser.add_argument("--output", default=None, help="Optional JSON output path")
    args = parser.parse_args()

    profile = collect_profile(role=args.role, serial=args.serial)
    formatted = json.dumps(profile, indent=2)
    if args.output:
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(formatted + "\n", encoding="utf-8")
        print(f"Wrote redacted device profile to {out_path}")
    else:
        print(formatted)
    return 0


if __name__ == "__main__":
    sys.exit(main())
