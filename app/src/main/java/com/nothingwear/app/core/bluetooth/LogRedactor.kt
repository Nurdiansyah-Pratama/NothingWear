package com.nothingwear.app.core.bluetooth

/**
 * Redacts personal identifiers and Bluetooth MAC addresses from diagnostic UI & logs
 * per Master Project Plan Section 9.
 */
object LogRedactor {

    private val macAddressRegex = Regex(
        pattern = """\b([0-9A-Fa-f]{2})([:\-])([0-9A-Fa-f]{2})\2([0-9A-Fa-f]{2})\2([0-9A-Fa-f]{2})\2([0-9A-Fa-f]{2})\2([0-9A-Fa-f]{2})\b""",
    )

    private val emailRegex = Regex(
        pattern = """\b([A-Za-z0-9._%+\-]{1,2})[A-Za-z0-9._%+\-]*@([A-Za-z0-9.\-]+\.[A-Za-z]{2,})\b""",
    )

    fun redactMacAddress(address: String?): String {
        if (address.isNullOrBlank()) return "**:**:**:**:**:**"
        val normalized = address.trim().uppercase()
        val match = macAddressRegex.matchEntire(normalized) ?: return "REDACTED-ADDR"
        val p1 = match.groupValues[1]
        val p2 = match.groupValues[3]
        val p6 = match.groupValues[7]
        return "$p1:$p2:**:**:**:$p6"
    }

    fun redactMessage(message: String): String {
        val withoutMacs = macAddressRegex.replace(message) { match ->
            val p1 = match.groupValues[1].uppercase()
            val p2 = match.groupValues[3].uppercase()
            val p6 = match.groupValues[7].uppercase()
            "$p1:$p2:**:**:**:$p6"
        }
        return emailRegex.replace(withoutMacs) { match ->
            "${match.groupValues[1]}***@${match.groupValues[2]}"
        }
    }
}
