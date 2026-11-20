import struct
import unittest

from btsnoop_analyzer import (
    BTSNOOP_MAGIC,
    DATALINK_HCI_H4,
    analyze_btsnoop,
    redact_text,
    shannon_entropy,
)


def build_btsnoop_file(packets: list[tuple[int, bytes]]) -> bytes:
    buf = bytearray(BTSNOOP_MAGIC)
    buf.extend(struct.pack(">II", 1, DATALINK_HCI_H4))
    for idx, (flags, payload) in enumerate(packets):
        buf.extend(struct.pack(">IIIIQ", len(payload), len(payload), flags, 0, 1_000_000 + idx))
        buf.extend(payload)
    return bytes(buf)


def build_h4_att_write_cmd(att_handle: int, value: bytes) -> bytes:
    att_pdu = struct.pack("<BH", 0x52, att_handle) + value
    l2cap = struct.pack("<HH", len(att_pdu), 0x0004) + att_pdu
    acl_hdr = struct.pack("<HH", 0x0040, len(l2cap))
    return b"\x02" + acl_hdr + l2cap


def build_h4_dynamic_l2cap(cid: int, payload: bytes) -> bytes:
    l2cap = struct.pack("<HH", len(payload), cid) + payload
    acl_hdr = struct.pack("<HH", 0x0040, len(l2cap))
    return b"\x02" + acl_hdr + l2cap


class BtsnoopAnalyzerTest(unittest.TestCase):
    def test_parses_att_write_and_finds_dummy_event_marker(self):
        pkt_att = build_h4_att_write_cmd(0x001A, b"\x01\x02NW_TEST_EVENT_01\x00")
        pkt_rfcomm = build_h4_dynamic_l2cap(0x0045, b"\x03\xef\x15NW_TEST_SPP")
        raw = build_btsnoop_file([(0, pkt_att), (0, pkt_rfcomm)])

        result = analyze_btsnoop(raw, marker="NW_TEST_")

        self.assertEqual(result["totalRecords"], 2)
        self.assertEqual(result["aclDataPackets"], 2)
        self.assertEqual(len(result["markerHits"]), 2)
        self.assertEqual(result["attChannels"][0]["attHandle"], "0x001A")
        self.assertEqual(result["attChannels"][0]["opcode"], "WRITE_CMD")
        self.assertEqual(result["dynamicL2capChannels"][0]["l2capCid"], "0x0045")

    def test_high_entropy_vs_plaintext_detection(self):
        plaintext = b"NW_TEST_EVENT_01" * 10
        pseudo_encrypted = bytes((i * 73 + 19) % 256 for i in range(256))

        self.assertLess(shannon_entropy(plaintext), 5.0)
        self.assertGreater(shannon_entropy(pseudo_encrypted), 7.5)

    def test_redacts_mac_addresses(self):
        self.assertEqual(
            redact_text("Peer C4:57:11:22:33:A3 connected"),
            "Peer C4:57:**:**:**:A3 connected",
        )


if __name__ == "__main__":
    unittest.main()
