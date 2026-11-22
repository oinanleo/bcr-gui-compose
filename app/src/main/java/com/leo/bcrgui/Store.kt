package com.leo.bcrgui

import android.content.Context
import org.json.JSONObject

/** 归档与删除的本地状态：id -> 归档时间；删除只记 id，不动手机里的文件。 */
data class UiState(
    val archived: Map<String, Long> = emptyMap(),
    val deleted: Set<String> = emptySet(),
    val fav: Set<String> = emptySet(),
)

object Store {
    private const val FILE = "state.json"

    fun load(ctx: Context): UiState = runCatching {
        val f = ctx.filesDir.resolve(FILE)
        if (!f.exists()) return UiState()
        val o = JSONObject(f.readText())
        val a = o.optJSONObject("archived")
        val archived = buildMap {
            a?.keys()?.forEach { k -> put(k, a.optLong(k)) }
        }
        val favArr = o.optJSONArray("fav")
        val fav = buildSet {
            if (favArr != null) for (i in 0 until favArr.length()) add(favArr.getString(i))
        }
        val d = o.optJSONArray("deleted")
        val deleted = buildSet {
            if (d != null) for (i in 0 until d.length()) add(d.getString(i))
        }
        UiState(archived, deleted, fav)
    }.getOrElse { UiState() }

    fun save(ctx: Context, s: UiState) = runCatching {
        val a = JSONObject()
        s.archived.forEach { (k, v) -> a.put(k, v) }
        val o = JSONObject()
            .put("archived", a)
            .put("deleted", org.json.JSONArray(s.deleted.toList()))
            .put("fav", org.json.JSONArray(s.fav.toList()))
        ctx.filesDir.resolve(FILE).writeText(o.toString())
    }.let { }
}

/** 每段录音的转写文本，和录音 uri 一一对应。 */
object Transcripts {
    private fun file(ctx: android.content.Context) = ctx.filesDir.resolve("transcripts.json")

    fun load(ctx: android.content.Context): Map<String, String> = runCatching {
        val f = file(ctx)
        if (!f.exists()) return emptyMap()
        val o = JSONObject(f.readText())
        buildMap { o.keys().forEach { k -> put(k, o.optString(k)) } }
    }.getOrElse { emptyMap() }

    fun put(ctx: android.content.Context, id: String, text: String): Map<String, String> {
        val now = load(ctx).toMutableMap()
        now[id] = text
        save(ctx, now)
        return now
    }

    fun save(ctx: android.content.Context, map: Map<String, String>) {
        runCatching {
            val o = JSONObject()
            map.forEach { (k, v) -> o.put(k, v) }
            file(ctx).writeText(o.toString())
        }
    }
}

/** AI 对话记录（role to content），退出再进还在。 */
object ChatHistory {
    private fun file(ctx: android.content.Context) = ctx.filesDir.resolve("chat.json")

    fun load(ctx: android.content.Context): List<Pair<String, String>> = runCatching {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        val arr = org.json.JSONArray(f.readText())
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val role = o.optString("role")
            val content = o.optString("content")
            if (role.isBlank() || content.isBlank()) null else role to content
        }
    }.getOrElse { emptyList() }

    fun save(ctx: android.content.Context, msgs: List<Pair<String, String>>) {
        runCatching {
            val arr = org.json.JSONArray()
            msgs.forEach { (role, content) ->
                arr.put(JSONObject().put("role", role).put("content", content))
            }
            file(ctx).writeText(arr.toString())
        }
    }
}

/** 每条录音的时长（毫秒）缓存，列表要显示长度。 */
object Durations {
    private fun file(ctx: android.content.Context) = ctx.filesDir.resolve("durations.json")

    fun load(ctx: android.content.Context): Map<String, Long> = runCatching {
        val f = file(ctx)
        if (!f.exists()) return emptyMap()
        val o = JSONObject(f.readText())
        buildMap { o.keys().forEach { k -> put(k, o.optLong(k)) } }
    }.getOrElse { emptyMap() }

    fun save(ctx: android.content.Context, map: Map<String, Long>) {
        runCatching {
            val o = JSONObject()
            map.forEach { (k, v) -> o.put(k, v) }
            file(ctx).writeText(o.toString())
        }
    }
}

/** 每条录音的 AI 总结缓存。 */
object Summaries {
    private fun file(ctx: android.content.Context) = ctx.filesDir.resolve("summaries.json")

    fun load(ctx: android.content.Context): Map<String, String> = runCatching {
        val f = file(ctx)
        if (!f.exists()) return emptyMap()
        val o = JSONObject(f.readText())
        buildMap { o.keys().forEach { k -> put(k, o.optString(k)) } }
    }.getOrElse { emptyMap() }

    fun put(ctx: android.content.Context, id: String, text: String): Map<String, String> {
        val now = load(ctx).toMutableMap()
        now[id] = text
        save(ctx, now)
        return now
    }

    fun save(ctx: android.content.Context, map: Map<String, String>) {
        runCatching {
            val o = JSONObject()
            map.forEach { (k, v) -> o.put(k, v) }
            file(ctx).writeText(o.toString())
        }
    }
}

/** 每条录音匹配到的图标名（icon 集合里的名字）。 */
object AiIcons {
    private fun file(ctx: android.content.Context) = ctx.filesDir.resolve("icons.json")

    fun load(ctx: android.content.Context): Map<String, String> = runCatching {
        val f = file(ctx)
        if (!f.exists()) return emptyMap()
        val o = JSONObject(f.readText())
        buildMap { o.keys().forEach { k -> put(k, o.optString(k)) } }
    }.getOrElse { emptyMap() }

    fun put(ctx: android.content.Context, id: String, name: String): Map<String, String> {
        val now = load(ctx).toMutableMap()
        now[id] = name
        save(ctx, now)
        return now
    }

    fun save(ctx: android.content.Context, map: Map<String, String>) {
        runCatching {
            val o = JSONObject()
            map.forEach { (k, v) -> o.put(k, v) }
            file(ctx).writeText(o.toString())
        }
    }
}

/** 同步结果。 */
data class Synced(
    val ui: UiState,
    val chat: List<Pair<String, String>>,
    val transcripts: Map<String, String>,
    val summaries: Map<String, String>,
)

/** 文件夹镜像同步：转写、AI 总结、归档/收藏、AI 聊天记录。 */
object Mirror {
    private const val T = "transcripts.json"
    private const val S = "summaries.json"
    private const val U = "state.json"
    private const val C = "chat.json"

    fun sync(
        ctx: android.content.Context,
        tree: android.net.Uri,
        ui: UiState,
        chat: List<Pair<String, String>>,
        transcripts: Map<String, String>,
        summaries: Map<String, String>,
    ): Synced {
        val mergedT = merge(transcripts, parseMap(Saf.readText(ctx, tree, T)))
        val mergedS = merge(summaries, parseMap(Saf.readText(ctx, tree, S)))
        val mergedU = mergeState(ui, parseState(Saf.readText(ctx, tree, U)))
        val mergedC = mergeChat(chat, parseChat(Saf.readText(ctx, tree, C)))

        Saf.writeText(ctx, tree, T, dumpMap(mergedT))
        Saf.writeText(ctx, tree, S, dumpMap(mergedS))
        Saf.writeText(ctx, tree, U, dumpState(mergedU))
        Saf.writeText(ctx, tree, C, dumpChat(mergedC))

        Transcripts.save(ctx, mergedT)
        Summaries.save(ctx, mergedS)
        Store.save(ctx, mergedU)
        ChatHistory.save(ctx, mergedC)
        return Synced(mergedU, mergedC, mergedT, mergedS)
    }

    /** 同一个 id 两边都有时，留字多的那一份。 */
    private fun merge(a: Map<String, String>, b: Map<String, String>): Map<String, String> {
        val out = HashMap<String, String>(a)
        for ((k, v) in b) {
            val old = out[k]
            if (old == null || v.length > old.length) out[k] = v
        }
        return out
    }

    /** 归档取更早的时间，收藏和删除取并集。 */
    private fun mergeState(a: UiState, b: UiState): UiState {
        val archived = HashMap<String, Long>(b.archived)
        a.archived.forEach { (k, v) ->
            val old = archived[k]
            archived[k] = if (old == null) v else minOf(old, v)
        }
        return UiState(
            archived = archived,
            deleted = a.deleted + b.deleted,
            fav = a.fav + b.fav,
        )
    }

    /** 聊天记录是同一个会话，取更长的那个。 */
    private fun mergeChat(a: List<Pair<String, String>>, b: List<Pair<String, String>>): List<Pair<String, String>> =
        if (b.size > a.size) b else a

    private fun parseMap(text: String?): Map<String, String> = runCatching {
        if (text.isNullOrBlank()) return emptyMap()
        val o = JSONObject(text)
        buildMap { o.keys().forEach { k -> put(k, o.optString(k)) } }
    }.getOrElse { emptyMap() }

    private fun dumpMap(map: Map<String, String>): String {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        return o.toString()
    }

    private fun parseState(text: String?): UiState = runCatching {
        if (text.isNullOrBlank()) return UiState()
        val o = JSONObject(text)
        val a = o.optJSONObject("archived")
        val archived = buildMap { a?.keys()?.forEach { k -> put(k, a.optLong(k)) } }
        val favArr = o.optJSONArray("fav")
        val fav = buildSet { if (favArr != null) for (i in 0 until favArr.length()) add(favArr.getString(i)) }
        val delArr = o.optJSONArray("deleted")
        val deleted = buildSet { if (delArr != null) for (i in 0 until delArr.length()) add(delArr.getString(i)) }
        UiState(archived, deleted, fav)
    }.getOrElse { UiState() }

    private fun dumpState(s: UiState): String {
        val a = JSONObject()
        s.archived.forEach { (k, v) -> a.put(k, v) }
        return JSONObject()
            .put("archived", a)
            .put("deleted", org.json.JSONArray(s.deleted.toList()))
            .put("fav", org.json.JSONArray(s.fav.toList()))
            .toString()
    }

    private fun parseChat(text: String?): List<Pair<String, String>> = runCatching {
        if (text.isNullOrBlank()) return emptyList()
        val arr = org.json.JSONArray(text)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val role = o.optString("role")
            val content = o.optString("content")
            if (role.isBlank() || content.isBlank()) null else role to content
        }
    }.getOrElse { emptyList() }

    private fun dumpChat(msgs: List<Pair<String, String>>): String {
        val arr = org.json.JSONArray()
        msgs.forEach { (role, content) -> arr.put(JSONObject().put("role", role).put("content", content)) }
        return arr.toString()
    }
}
