package com.leo.bcrgui

import java.time.LocalDateTime
import java.time.ZoneOffset

enum class Direction { IN, OUT, CONFERENCE, UNKNOWN }

data class Recording(
    val id: String,
    val fileName: String,
    val size: Long,
    val timeMillis: Long,
    val direction: Direction,
    val number: String,
    val contactName: String = "",
) {
    /** 列表标题：有联系人名就用名字，否则号码，再不行用文件名 */
    val title: String get() = contactName.ifBlank { number.ifBlank { fileName } }
}

/**
 * ShizuCallRecorder 文件名模板：{date}_{direction}_{phone_number}
 * date = yyyyMMdd_HHmmss[.SSS][±HHmm]，direction = in|out|conference
 * 正则生成逻辑照 bcr-gui 的 getFilenameRegExpPattern 搬，只留这一套。
 */
object Names {
    private val RX = Regex(
        "^" +
            "(?<y>\\d{4})(?<mo>\\d{2})(?<d>\\d{2})_" +
            "(?<h>\\d{2})(?<mi>\\d{2})(?<s>\\d{2}(?:\\.\\d{1,3})?)" +
            "(?<tzh>[+\\-]?\\d{2})(?<tzm>\\d{2})" +
            "_(?<dir>in|out|conference)" +
            "_(?<num>[\\d+\\- ]+|unknown)"
    )

    fun parse(f: RawFile): Recording {
        val base = f.name.substringBeforeLast('.')
        val m = RX.find(base)
        return Recording(
            id = f.uri.toString(),
            fileName = f.name,
            size = f.size,
            timeMillis = m?.let { toEpoch(it) } ?: f.lastModified,
            direction = when (m?.groups?.get("dir")?.value) {
                "in" -> Direction.IN
                "out" -> Direction.OUT
                "conference" -> Direction.CONFERENCE
                else -> Direction.UNKNOWN
            },
            number = normalize(m?.groups?.get("num")?.value),
        )
    }

    private fun toEpoch(m: MatchResult): Long? = runCatching {
        val g = m.groups
        fun i(k: String) = g[k]!!.value.toInt()
        val sec = g["s"]!!.value.substringBefore('.').toInt()
        val ldt = LocalDateTime.of(i("y"), i("mo"), i("d"), i("h"), i("mi"), sec)
        val tzhRaw = g["tzh"]!!.value
        val sign = if (tzhRaw.startsWith("-")) -1 else 1
        val hh = tzhRaw.filter { it.isDigit() }.toInt()
        val mm = g["tzm"]!!.value.toInt()
        ldt.toInstant(ZoneOffset.ofTotalSeconds(sign * (hh * 3600 + mm * 60))).toEpochMilli()
    }.getOrNull()

    /** +86 138-0000 0000 -> +8613800000000 */
    fun normalize(raw: String?): String {
        val s = raw?.trim().orEmpty()
        if (s.isBlank() || s.equals("unknown", true)) return ""
        val digits = s.filter { it.isDigit() }
        return if (s.startsWith("+")) "+$digits" else digits
    }
}
