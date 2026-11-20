#!/usr/bin/env python3
"""
NothingWear M3 BTSnoop v1 Parser & Redacted Protocol Traffic Analyzer.

Parses standard Android Bluetooth HCI Snoop logs (`btsnoop_hci.log` or inside
an `adb bugreport` ZIP archive) using Python standard library only.

Capabilities:
  - Validates BTSnoop v1 magic header (`btsnoop\\0`) and datalink type.
  - Parses HCI ACL Data packets (H4 indicator 0x02) and L2CAP PDUs.
  - Summarizes ATT (CID 0x0004) Write Command / Write Request / Notification
    traffic by ATT handle and computes Shannon byte entropy to distinguish
    plaintext/structured payloads from encrypted streams.
  - Summarizes dynamic L2CAP channels (such as Classic RFCOMM/SPP 0x1101).
  - Searches reassembled payload bytes for controlled dummy event markers
    (default prefix: `NW_TEST_`).
  - Redacts any MAC-like sequences from text output.
"""

from __future__ import annotations

import argparse
import collections
import json
import math
import re
import struct
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path

BTSNOOP_MAGIC = b"btsnoop\x00"
DATALINK_HCI_H4 = 1002  # HCI UART (H4) used by Android btsnoop_hci.log

ATT_CID = 0x0004
ATT_OPCODES = {
    0x0B: "READ_RSP",
    0x12: "WRITE_REQ",
    0x1B: "HANDLE_VALUE_NTF",
    0x1D: "HANDLE_VALUE_IND",
    0x52: "WRITE_CMD",
}

MAC_REGEX = re.compile(
    r"\b([0-9A-Fa-f]{2})([:\-])([0-9A-Fa-f]{2})\2([0-9A-Fa-f]{2})\2([0-9A-Fa-f]{2})\2([0-9A-Fa-f]{2})\2([0-9A-Fa-f]{2})\b"
)


def redact_text(text: str) -> str:
    return MAC_REGEX.sub(lambda m: f"{m.group(1).upper()}:{m.group(3).upper()}:**:**:**:{m.group(7).upper()}", text)


def shannon_entropy(data: bytes) -> float:
    if not data:
        return 0.0
    counts = collections.Counter(data)
    length = len(data)
    return round(-sum((c / length) * math.log2(c / length) for c in counts.values()), 3)


@dataclass
class BtsnoopRecord:
    index: int
    orig_len: int
    incl_len: int
    flags: int
    timestamp_us: int
    payload: bytes

    @property
    def direction(self) -> str:
        # Bit 0: 0 = Host -> Controller (TX), 1 = Controller -> Host (RX)
        return "RX" if (self.flags & 0x01) else "TX"


def parse_btsnoop_bytes(raw: bytes) -> tuple[int, list[BtsnoopRecord]]:
    if len(raw) < 16 or raw[:8] != BTSNOOP_MAGIC:
        raise ValueError("Input is not a valid BTSnoop v1 file (missing magic header).")

    version, datalink = struct.unpack_from(">II", raw, 8)
    if version != 1:
        raise ValueError(f"Unsupported BTSnoop version: {version}")

    records: list[BtsnoopRecord] = []
    offset = 16
    index = 0
    while offset + 24 <= len(raw):
        orig_len, incl_len, flags, _drops, ts_us = struct.unpack_from(">IIIIQ", raw, offset)
        offset += 24
        if offset + incl_len > len(raw):
            break
        payload = raw[offset : offset + incl_len]
        offset += incl_len
        records.append(
            BtsnoopRecord(
                index=index,
                orig_len=orig_len,
                incl_len=incl_len,
                flags=flags,
                timestamp_us=ts_us,
                payload=payload,
            )
        )
        index += 1

    return datalink, records


def analyze_btsnoop(raw: bytes, marker: str = "NW_TEST_") -> dict:
    datalink, records = parse_btsnoop_bytes(raw)
    marker_bytes = marker.encode("utf-8")

    acl_count = 0
    att_stats: dict[str, dict] = {}
    dynamic_l2cap_stats: dict[str, dict] = {}
    marker_hits: list[dict] = []

    for rec in records:
        if not rec.payload:
            continue

        # Android H4 captures start with 1-byte HCI packet type (0x02 = ACL Data)
        if datalink == DATALINK_HCI_H4:
            pkt_type = rec.payload[0]
            if pkt_type != 0x02 or len(rec.payload) < 1 + 4 + 4:
                continue
            acl_body = rec.payload[1:]
        else:
            if len(rec.payload) < 8:
                continue
            acl_body = rec.payload

        acl_count += 1
        handle_flags, _acl_len = struct.unpack_from("<HH", acl_body, 0)
        pb_flag = (handle_flags >> 12) & 0x03
        # Skip continuation fragments (pb_flag == 0x01) for single-PDU L2CAP header parsing
        if pb_flag == 0x01 or len(acl_body) < 8:
            continue

        l2cap_len, l2cap_cid = struct.unpack_from("<HH", acl_body, 4)
        l2cap_payload = acl_body[8 : 8 + l2cap_len]

        if l2cap_cid == ATT_CID and len(l2cap_payload) >= 3:
            opcode = l2cap_payload[0]
            att_handle = struct.unpack_from("<H", l2cap_payload, 1)[0]
            att_value = l2cap_payload[3:]
            op_name = ATT_OPCODES.get(opcode, f"OP_0x{opcode:02X}")
            bucket_key = f"0x{att_handle:04X}:{op_name}:{rec.direction}"
            bucket = att_stats.setdefault(
                bucket_key,
                {
                    "attHandle": f"0x{att_handle:04X}",
                    "opcode": op_name,
                    "direction": rec.direction,
                    "packetCount": 0,
                    "totalValueBytes": 0,
                    "_sampleBytes": bytearray(),
                },
            )
            bucket["packetCount"] += 1
            bucket["totalValueBytes"] += len(att_value)
            if len(bucket["_sampleBytes"]) < 4096:
                bucket["_sampleBytes"].extend(att_value[: 4096 - len(bucket["_sampleBytes"])])

            if marker_bytes and marker_bytes in att_value:
                marker_hits.append({
                    "recordIndex": rec.index,
                    "channel": "ATT",
                    "attHandle": f"0x{att_handle:04X}",
                    "opcode": op_name,
                    "direction": rec.direction,
                    "valueLength": len(att_value),
                    "previewHex": att_value[:32].hex(),
                })
        elif l2cap_cid >= 0x0040:
            # Dynamic L2CAP CID (commonly Classic RFCOMM / SPP 0x1101 or CoC)
            bucket_key = f"CID_0x{l2cap_cid:04X}:{rec.direction}"
            bucket = dynamic_l2cap_stats.setdefault(
                bucket_key,
                {
                    "l2capCid": f"0x{l2cap_cid:04X}",
                    "direction": rec.direction,
                    "packetCount": 0,
                    "totalBytes": 0,
                    "_sampleBytes": bytearray(),
                },
            )
            bucket["packetCount"] += 1
            bucket["totalBytes"] += len(l2cap_payload)
            if len(bucket["_sampleBytes"]) < 4096:
                bucket["_sampleBytes"].extend(l2cap_payload[: 4096 - len(bucket["_sampleBytes"])])

            if marker_bytes and marker_bytes in l2cap_payload:
                marker_hits.append({
                    "recordIndex": rec.index,
                    "channel": f"L2CAP_0x{l2cap_cid:04X}",
                    "direction": rec.direction,
                    "valueLength": len(l2cap_payload),
                    "previewHex": l2cap_payload[:32].hex(),
                })

    att_summary = []
    for item in att_stats.values():
        sample = bytes(item.pop("_sampleBytes"))
        item["shannonEntropy"] = shannon_entropy(sample)
        item["likelyEncryptedOrCompressed"] = item["shannonEntropy"] >= 7.2 and len(sample) >= 64
        att_summary.append(item)

    dynamic_summary = []
    for item in dynamic_l2cap_stats.values():
        sample = bytes(item.pop("_sampleBytes"))
        item["shannonEntropy"] = shannon_entropy(sample)
        item["likelyEncryptedOrCompressed"] = item["shannonEntropy"] >= 7.2 and len(sample) >= 64
        dynamic_summary.append(item)

    return {
        "datalinkType": datalink,
        "totalRecords": len(records),
        "aclDataPackets": acl_count,
        "markerSearched": marker,
        "markerHits": marker_hits,
        "attChannels": sorted(att_summary, key=lambda x: (-x["packetCount"], x["attHandle"])),
        "dynamicL2capChannels": sorted(dynamic_summary, key=lambda x: (-x["packetCount"], x["l2capCid"])),
    }


def load_btsnoop_from_path(path: Path) -> bytes:
    if path.suffix.lower() == ".zip":
        with zipfile.ZipFile(path, "r") as zf:
            candidates = [
                n for n in zf.namelist()
                if "btsnoop_hci" in n.lower() or n.lower().endswith((".cfa", ".btsnoop"))
            ]
            if not candidates:
                raise FileNotFoundError("No btsnoop_hci log found inside bugreport ZIP.")
            return zf.read(candidates[0])
    return path.read_bytes()


def main() -> int:
    parser = argparse.ArgumentParser(description="Analyze Android BTSnoop HCI capture (redacted summary).")
    parser.add_argument("input_path", help="Path to btsnoop_hci.log / .cfa / adb bugreport .zip")
    parser.add_argument("--marker", default="NW_TEST_", help="Dummy calendar event marker to search for")
    parser.add_argument("--output", default=None, help="Optional redacted JSON output path")
    args = parser.parse_args()

    raw = load_btsnoop_from_path(Path(args.input_path))
    summary = analyze_btsnoop(raw, marker=args.marker)
    formatted = redact_text(json.dumps(summary, indent=2))
    if args.output:
        out = Path(args.output)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(formatted + "\n", encoding="utf-8")
        print(f"Wrote redacted BTSnoop summary to {out}")
    else:
        print(formatted)
    return 0


if __name__ == "__main__":
    sys.exit(main())
