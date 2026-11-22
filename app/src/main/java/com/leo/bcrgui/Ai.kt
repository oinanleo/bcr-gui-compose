package com.leo.bcrgui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** OpenAI 兼容接口的极简客户端：总结 + 对话。 */
object Ai {
    private const val SYS_SUMMARY =
        "你是一个通话录音整理助手。请用简体中文，把这段通话转写文本总结成清晰的要点，" +
            "先一句话说这通电话是关于什么的，再用 3-5 条要点列出双方说了什么、有没有待办事项。" +
            "不要臆造内容，原文没有的信息不要写。总长不超过 200 字。"

    private const val SYS_CHAT =
        "你是通话录音助手。用户会把一些通话的转写文本交给你，你可以据此回答、检索、比较和归纳。" +
            "回答用简体中文，简洁直接；资料里没有的内容就直说没有，不要编。"

    private suspend fun post(base: String, key: String, model: String, messages: JSONArray): String =
        withContext(Dispatchers.IO) {
            require(base.isNotBlank()) { "没配置接口地址" }
            require(model.isNotBlank()) { "没配置模型" }
            val url = URL(base.trimEnd('/') + "/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 20000
                conn.readTimeout = 120000
                conn.setRequestProperty("Content-Type", "application/json")
                if (key.isNotBlank()) conn.setRequestProperty("Authorization", "Bearer $key")

                val body = JSONObject()
                    .put("model", model)
                    .put("temperature", 0.3)
                    .put("messages", messages)
                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val resp = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("HTTP $code：${resp.take(200)}")

                JSONObject(resp)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            } finally {
                conn.disconnect()
            }
        }

    private const val SYS_ICON =
        "你是图标匹配器。根据通话内容，从给定的图标列表里挑一个最贴切的给我。规则：" +
            "必须挑一个；只输出图标名本身，全小写英文，不要解释、不要标点、不要换行、不要任何多余文字。"

    /** 返回 IconSet 里的图标名，模型没给出合法名字时返回空串。 */
    suspend fun pickIcon(base: String, key: String, model: String, text: String): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", SYS_ICON))
            .put(
                JSONObject().put("role", "user").put(
                    "content",
                    "可选图标（按场景分组，名字后面是中文含义）：\n" + IconSet.prompt +
                        "\n\n通话内容：\n" + text.take(4000),
                )
            )
        return IconSet.match(post(base, key, model, messages))
    }

    suspend fun summarize(base: String, key: String, model: String, text: String): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", SYS_SUMMARY))
            .put(JSONObject().put("role", "user").put("content", text.take(12000)))
        return post(base, key, model, messages)
    }

    /** history: (role, content)，role 只会是 user / assistant。 */
    suspend fun chat(
        base: String,
        key: String,
        model: String,
        history: List<Pair<String, String>>,
        transcripts: String,
    ): String {
        val messages = JSONArray()
        messages.put(
            JSONObject().put("role", "system").put(
                "content",
                SYS_CHAT + "\n\n【可检索的转写资料】\n" + transcripts.take(12000),
            )
        )
        history.takeLast(20).forEach { (role, content) ->
            messages.put(JSONObject().put("role", role).put("content", content.take(6000)))
        }
        return post(base, key, model, messages)
    }
}
