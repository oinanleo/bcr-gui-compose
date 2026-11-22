package com.leo.bcrgui

import android.content.Context
import android.provider.ContactsContract

/** 读通讯录，把号码映射成联系人名（照 BCR-GUI 的 getContactsIndexedByE164 思路，只保留号码->名字）。 */
object Contacts {
    fun index(ctx: Context): Map<String, String> = runCatching {
        val map = HashMap<String, String>(512)
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val number = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val digits = digits(number)
                if (digits.isEmpty()) continue
                map[digits] = name
                if (digits.length > 9) map[digits.takeLast(9)] = name
            }
        }
        map
    }.getOrElse { emptyMap() }

    fun digits(s: String?): String = s.orEmpty().filter { it.isDigit() }

    fun nameFor(number: String, index: Map<String, String>): String {
        if (index.isEmpty()) return ""
        val d = digits(number)
        if (d.isEmpty()) return ""
        index[d]?.let { return it }
        if (d.length > 9) index[d.takeLast(9)]?.let { return it }
        return ""
    }
}
