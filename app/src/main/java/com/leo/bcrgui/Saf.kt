package com.leo.bcrgui

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document as Doc

data class RawFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
)

/** SAF 批量扫描：只用 DocumentsContract 查询，避免 DocumentFile 逐条 getDisplayName 的慢路径。 */
object Saf {
    private val COLS = arrayOf(
        Doc.COLUMN_DOCUMENT_ID,
        Doc.COLUMN_DISPLAY_NAME,
        Doc.COLUMN_MIME_TYPE,
        Doc.COLUMN_SIZE,
        Doc.COLUMN_LAST_MODIFIED,
    )
    private val AUDIO_EXT =
        setOf("m4a", "mp3", "amr", "awb", "wav", "ogg", "opus", "flac", "aac", "mp4")

    fun scan(ctx: Context, tree: Uri): List<RawFile> {
        val out = ArrayList<RawFile>(512)
        val root = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: return out
        walk(ctx, tree, root, out, 0)
        return out
    }

    private fun walk(ctx: Context, tree: Uri, dirId: String, out: MutableList<RawFile>, depth: Int) {
        if (depth > 6) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dirId)
        val subDirs = ArrayList<String>(4)
        runCatching {
            ctx.contentResolver.query(children, COLS, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2) ?: ""
                    if (mime == Doc.MIME_TYPE_DIR) { subDirs.add(id); continue }
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (mime.startsWith("audio") || ext in AUDIO_EXT) {
                        out.add(
                            RawFile(
                                uri = DocumentsContract.buildDocumentUriUsingTree(tree, id),
                                name = name,
                                size = c.getLong(3),
                                lastModified = c.getLong(4),
                            )
                        )
                    }
                }
            }
        }
        subDirs.forEach { walk(ctx, tree, it, out, depth + 1) }
    }
}

/** 在选定目录里按文件名读写纯文本（同步用）。 */
private fun Saf.findChild(ctx: Context, tree: android.net.Uri, name: String): android.net.Uri? =
    runCatching {
        val root = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, root)
        ctx.contentResolver.query(
            children,
            arrayOf(Doc.COLUMN_DOCUMENT_ID, Doc.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == name) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                }
            }
        }
        null
    }.getOrNull()

fun Saf.readText(ctx: Context, tree: android.net.Uri, name: String): String? =
    runCatching {
        val uri = findChild(ctx, tree, name) ?: return null
        ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()

fun Saf.writeText(ctx: Context, tree: android.net.Uri, name: String, content: String) {
    runCatching {
        val existing = findChild(ctx, tree, name)
        val uri = existing ?: run {
            val root = DocumentsContract.getTreeDocumentId(tree)
            val parents = DocumentsContract.buildDocumentUriUsingTree(tree, root)
            DocumentsContract.createDocument(ctx.contentResolver, parents, "application/json", name)
        } ?: return
        ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray()) }
    }
}
