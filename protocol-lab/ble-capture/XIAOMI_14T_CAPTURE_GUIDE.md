# Controlled Comparative Capture Guide — Reference A (Xiaomi 14T)

This guide collects reproducible, redacted evidence from **Reference A (Xiaomi 14T)** to complete the comparative dataset against **Target B (Infinix X6880 / XOS 15)**.

> **Safety Rules:**
> - Keep **Xiaomi Watch S4 firmware** and **Mi Fitness (`com.xiaomi.wearable 3.60.0i`)** constant between both phones.
> - Use **dummy calendar events only** (prefix `NW_TEST_`, e.g., `NW_TEST_EVENT_01`).
> - Never commit raw `.btsnoop`, `.cfa`, `.pcap`, or bugreport `.zip` files to Git (`.gitignore` blocks them automatically).

---

## Step 1 — Collect Redacted Device Profile (Xiaomi 14T)
Connect **Xiaomi 14T** via USB (USB Debugging enabled) and run from the repository root:

```powershell
python protocol-lab/ble-capture/collect_device_profile.py `
  --role Reference_A_Xiaomi_14T `
  --output protocol-lab/protocol-docs/reference_a_xiaomi_14t_profile.json
```

Verify that `com.xiaomi.wearable` matches `versionName = 3.60.0i` (`versionCode = 360000`) and compare `osProperties` against `target_b_infinix_xos15_profile.json`.

---

## Step 2 — Capture Bluetooth HCI Snoop Log During Dummy Event Sync
1. On **Xiaomi 14T**, open **Settings → Developer Options → Enable Bluetooth HCI snoop log** (`Enabled`).
2. Toggle Bluetooth **Off → On** so Android starts a fresh HCI log session, and confirm **Xiaomi Watch S4** reconnects in Mi Fitness.
3. In Google Calendar / Xiaomi Calendar on Xiaomi 14T, create a single dummy event:
   - **Title:** `NW_TEST_EVENT_01`
   - **Time:** 1 hour from now
   - **Location:** `NW_TEST_LOC`
4. Open **Mi Fitness → Device → Events** and trigger synchronization; confirm `NW_TEST_EVENT_01` appears in the native **Events** app on Xiaomi Watch S4.
5. Wait 5 seconds, then pull the bugreport into git-ignored `protocol-lab/ble-capture/`:

```powershell
C:\Users\asus\AppData\Local\Android\Sdk\platform-tools\adb.exe bugreport protocol-lab/ble-capture/xiaomi14t_event_sync_bugreport.zip
```

6. Disable **Bluetooth HCI snoop log** on Xiaomi 14T after capture so logs don't grow indefinitely.

---

## Step 3 — Run Redacted BTSnoop Traffic Analysis
Run our pure-Python analyzer (automatically extracts `btsnoop_hci.log` from the bugreport ZIP and redacts MAC addresses):

```powershell
python protocol-lab/packet-parser/btsnoop_analyzer.py `
  protocol-lab/ble-capture/xiaomi14t_event_sync_bugreport.zip `
  --marker NW_TEST_ `
  --output protocol-lab/protocol-docs/xiaomi14t_btsnoop_summary.json
```

### What the Output Answers for M4 Feasibility
- **Active Transport:** Whether traffic spikes on ATT/GATT (`0xFDAB` handles `0x0002`/`0x0003`) or dynamic L2CAP/RFCOMM (`SPP 0x1101`).
- **Encryption Confirmation:**
  - If `markerHits` is empty AND `shannonEntropy >= 7.2` (`likelyEncryptedOrCompressed: true`), wire payloads are encrypted under `WearAuthV1`/`WearAuthV2`.
  - If `markerHits` is non-empty AND entropy is low, payload framing/protobuf fields are observable in cleartext.
