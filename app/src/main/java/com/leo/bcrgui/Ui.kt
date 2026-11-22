package com.leo.bcrgui

import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import androidx.media3.common.Tracks
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AppTheme(mode: String = "system", content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = when (mode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val scheme = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("bcr", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var tree by remember { mutableStateOf(prefs.getString("tree", null)) }
    var items by remember { mutableStateOf<List<Recording>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(0L) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var archiveSel by remember { mutableStateOf<Set<String>>(emptySet()) }
    var ui by remember { mutableStateOf(Store.load(ctx)) }
    var contactIndex by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val notifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val contactPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scope.launch {
            contactIndex = withContext(Dispatchers.IO) { Contacts.index(ctx) }
        }
    }
    LaunchedEffect(Unit) {
        val ok = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (ok) contactIndex = withContext(Dispatchers.IO) { Contacts.index(ctx) }
        else if (tree != null) contactPerm.launch(Manifest.permission.READ_CONTACTS)
    }
    val named = remember(items, contactIndex) {
        if (contactIndex.isEmpty()) items
        else items.map { it.copy(contactName = Contacts.nameFor(it.number, contactIndex)) }
    }
    val favList = remember(named, ui) { named.filter { it.id in ui.fav }.sortedByDescending { it.timeMillis } }

    var controller by remember { mutableStateOf<MediaController?>(null) }
    var current by remember { mutableStateOf<Recording?>(null) }
    var playing by remember { mutableStateOf(false) }
    var posMs by remember { mutableStateOf(0L) }
    var durMs by remember { mutableStateOf(0L) }
    var seekGuard by remember { mutableLongStateOf(0L) }
    var meta by remember { mutableStateOf("") }
    var playerCollapsed by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    var showSpeed by remember { mutableStateOf(false) }
    // 0 录音 / 1 归档 / 2 收藏 / 3 设置：底部栏（手机）和侧边栏（平板）共用同一个 page
    var page by remember { mutableStateOf(0) }
    var themeMode by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var transcript by remember { mutableStateOf<String?>(null) }
    var transcribing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val dlProgress = remember(status) {
        Regex("(\\d+)%").find(status ?: "")?.groupValues?.get(1)?.toIntOrNull()?.let { it / 100f }
    }
    var summary by remember { mutableStateOf<String?>(null) }
    var summarizing by remember { mutableStateOf(false) }
    var aiBase by remember { mutableStateOf(prefs.getString("ai_base", "") ?: "") }
    var aiKey by remember { mutableStateOf(prefs.getString("ai_key", "") ?: "") }
    var aiModel by remember { mutableStateOf(prefs.getString("ai_model", "") ?: "") }
    var transcripts by remember { mutableStateOf(Transcripts.load(ctx)) }
    var chatOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var chat by remember { mutableStateOf(ChatHistory.load(ctx)) }
    var draftMsg by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var autoTranscribe by remember { mutableStateOf(prefs.getBoolean("auto_transcribe", false)) }
    var durations by remember { mutableStateOf(Durations.load(ctx)) }
    var summaries by remember { mutableStateOf(Summaries.load(ctx)) }
    var icons by remember { mutableStateOf(AiIcons.load(ctx)) }
    var iconTried by remember { mutableStateOf<Set<String>>(emptySet()) }
    var iconRunning by remember { mutableStateOf(false) }
    var iconTotal by remember { mutableIntStateOf(0) }
    var iconDone by remember { mutableIntStateOf(0) }
    var iconFail by remember { mutableIntStateOf(0) }
    var iconErr by remember { mutableStateOf<String?>(null) }
    var syncTree by remember { mutableStateOf(prefs.getString("sync_tree", null)) }
    var syncing by remember { mutableStateOf(false) }
    var syncMsg by remember { mutableStateOf<String?>(null) }
    // 平板横屏：播放页收成右侧栏，列表窄一点但不被整块盖住
    val wide = LocalConfiguration.current.screenWidthDp >= 840
    val archiveOpen = page == 1
    val favOpen = page == 2
    val settingsOpen = page == 3
    // 底部栏高度用实测值，避免按 80dp 猜不准（不同导航方式 inset 不一样）
    var navBarPx by remember { mutableIntStateOf(0) }
    val navBarH = if (wide) 0.dp else with(LocalDensity.current) { navBarPx.toDp() }

    DisposableEffect(Unit) {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({ controller = runCatching { future.get() }.getOrNull() }, MoreExecutors.directExecutor())
        onDispose { controller?.release() }
    }

    DisposableEffect(controller) {
        val c = controller ?: return@DisposableEffect onDispose { }
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }

            override fun onTracksChanged(tracks: Tracks) {
                val f = tracks.groups
                    .firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
                    ?.let { g ->
                        (0 until g.mediaTrackGroup.length)
                            .map { g.mediaTrackGroup.getFormat(it) }
                            .firstOrNull()
                    }
                meta = f?.let(::fmtMeta).orEmpty()
            }
        }
        c.addListener(l)
        onDispose { c.removeListener(l) }
    }
    // 服务端校时：频率放低，避免频繁跨进程 IPC 造成卡顿
    LaunchedEffect(current?.id) {
        while (true) {
            if (System.currentTimeMillis() >= seekGuard) {
                val p = controller?.currentPosition
                if (p != null && p >= 0) posMs = p
                val d = controller?.duration ?: 0L
                if (d > 0) {
                    durMs = d
                    val id = current?.id
                    if (id != null && durations[id] != d) {
                        val merged = durations + (id to d)
                        durations = merged
                        Durations.save(ctx, merged)
                    }
                }
            }
            delay(1500)
        }
    }

    // 播放时按帧本地推进，进度条才顺
    LaunchedEffect(playing, current?.id) {
        if (!playing) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L && System.currentTimeMillis() >= seekGuard) {
                    posMs += (((now - last) / 1_000_000f) * speed).toLong()
                }
                last = now
            }
        }
    }

    LaunchedEffect(current?.id) {
        transcript = current?.let { transcripts[it.id] }
        summary = current?.let { summaries[it.id] }
        transcribing = false
        summarizing = false
    }

    LaunchedEffect(named) {
        val missing = named.filter { it.id !in durations }
        if (missing.isEmpty()) return@LaunchedEffect
        val added = HashMap<String, Long>()
        withContext(Dispatchers.IO) {
            for (r in missing) {
                val ms = runCatching {
                    val mmr = android.media.MediaMetadataRetriever()
                    try {
                        mmr.setDataSource(ctx, Uri.parse(r.id))
                        mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L
                    } finally {
                        runCatching { mmr.release() }
                    }
                }.getOrElse { 0L }
                if (ms > 0) added[r.id] = ms
            }
        }
        if (added.isNotEmpty()) {
            val merged = durations + added
            durations = merged
            Durations.save(ctx, merged)
        }
    }

    LaunchedEffect(autoTranscribe) {
        if (!autoTranscribe) return@LaunchedEffect
        for (r in named) {
            if (transcripts.containsKey(r.id)) continue
            val t = runCatching { Asr.transcribe(ctx, Uri.parse(r.id)) { } }.getOrNull() ?: continue
            transcripts = Transcripts.put(ctx, r.id, t)
        }
    }

    // 新录音自动匹配图标：一条一条来，模型只回一个图标名
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            if (iconRunning) continue
            if (aiBase.isBlank() || aiKey.isBlank() || aiModel.isBlank()) continue
            if (iconTried.size >= 40) continue
            val r = named.firstOrNull { rec ->
                rec.id !in icons && rec.id !in iconTried && transcripts[rec.id] != null
            } ?: continue
            iconTried = iconTried + r.id
            val body = transcripts[r.id]?.take(3000).orEmpty()
            val name = runCatching { Ai.pickIcon(aiBase, aiKey, aiModel, body) }.getOrNull().orEmpty()
            if (name.isNotBlank()) icons = AiIcons.put(ctx, r.id, name)
        }
    }

    fun sendChat() {
        val text = draftMsg.trim()
        if (text.isEmpty() || thinking) return
        chat = chat + ("user" to text)
        ChatHistory.save(ctx, chat)
        draftMsg = ""
        scope.launch {
            thinking = true
            val reply = runCatching {
                val corpus = transcripts.entries.joinToString("\n\n") { (id, t) ->
                    val rec = named.firstOrNull { it.id == id }
                    val name = rec?.title ?: id
                    val whenText = rec?.let { fmtTime(it.timeMillis) } ?: ""
                    "【$name · $whenText】\n$t"
                }
                Ai.chat(aiBase, aiKey, aiModel, chat, corpus)
            }.getOrElse { "出错了：" + (it.message ?: "未知错误") }
            chat = chat + ("assistant" to reply)
            ChatHistory.save(ctx, chat)
            thinking = false
        }
    }

    fun matchIcons(limit: Int = Int.MAX_VALUE) {
        if (iconRunning) return
        if (aiBase.isBlank() || aiKey.isBlank() || aiModel.isBlank()) {
            iconErr = "先在上面配好接口地址、API Key 和模型名"
            return
        }
        val pool = if (contactIndex.isEmpty()) items
        else items.map { it.copy(contactName = Contacts.nameFor(it.number, contactIndex)) }
        // 只有转写完成的录音才拿去匹配
        val targets = pool.filter { it.id !in icons && transcripts[it.id] != null }.take(limit)
        if (targets.isEmpty()) {
            iconErr = if (pool.none { it.id !in icons }) "所有录音都已经有图标了"
            else "没有可匹配的录音：需要先转写完成"
            return
        }
        iconRunning = true
        iconTotal = targets.size
        iconDone = 0
        iconFail = 0
        iconErr = null
        scope.launch {
            val acc = HashMap<String, String>()
            for (r in targets) {
                val text = transcripts[r.id]?.take(3000).orEmpty()
                val name = runCatching { Ai.pickIcon(aiBase, aiKey, aiModel, text) }
                    .onFailure { iconErr = it.message ?: "请求失败" }
                    .getOrNull().orEmpty()
                if (name.isNotBlank()) {
                    acc[r.id] = name
                    iconDone += 1
                    if (acc.size % 10 == 0) {
                        AiIcons.save(ctx, icons + acc)
                        icons = icons + acc
                    }
                } else {
                    iconFail += 1
                }
            }
            if (acc.isNotEmpty()) {
                AiIcons.save(ctx, icons + acc)
                icons = icons + acc
            }
            iconRunning = false
        }
    }

    fun clearIcons() {
        iconRunning = false
        AiIcons.save(ctx, emptyMap())
        icons = emptyMap()
        iconDone = 0
        iconFail = 0
        iconTotal = 0
        iconErr = null
        iconTried = emptySet()
    }

    fun syncNow() {
        val t = syncTree ?: return
        if (syncing) return
        scope.launch {
            syncing = true
            syncMsg = null
            val res = withContext(Dispatchers.IO) {
                runCatching { Mirror.sync(ctx, Uri.parse(t), ui, chat, transcripts, summaries) }
            }
            res.onSuccess { r ->
                ui = r.ui
                chat = r.chat
                transcripts = r.transcripts
                summaries = r.summaries
                val id = current?.id
                if (id != null) summary = r.summaries[id]
                syncMsg = "已同步：转写 ${r.transcripts.size} · 总结 ${r.summaries.size} · " +
                    "归档 ${r.ui.archived.size} · 收藏 ${r.ui.fav.size} · 聊天 ${r.chat.size} 条"
            }.onFailure {
                syncMsg = "同步失败：" + (it.message ?: "未知错误")
            }
            syncing = false
        }
    }

    fun play(r: Recording) {
        if (current?.id != r.id) {
            current = r
            posMs = 0
            durMs = 0
            controller?.setMediaItem(MediaItem.fromUri(Uri.parse(r.id)))
            controller?.prepare()
        }
        controller?.play()
    }

    BackHandler(enabled = current != null && !archiveOpen && !favOpen && !settingsOpen) {
        if (!wide && !playerCollapsed) {
            playerCollapsed = true
        } else {
            runCatching { controller?.stop() }
            current = null
            playing = false
            posMs = 0
            durMs = 0
        }
    }

    BackHandler(
        enabled = selected.isNotEmpty() || archiveSel.isNotEmpty() || favOpen || archiveOpen ||
            settingsOpen || chatOpen || searchOpen
    ) {
        when {
            searchOpen -> searchOpen = false
            chatOpen -> chatOpen = false
            archiveSel.isNotEmpty() -> archiveSel = emptySet()
            selected.isNotEmpty() -> selected = emptySet()
            page != 0 -> page = 0
        }
    }

    fun mut(f: (UiState) -> UiState) {
        ui = f(ui)
        Store.save(ctx, ui)
    }

    fun rescan(u: String, alsoIcons: Boolean = false) {
        scope.launch {
            scanning = true
            val t0 = System.currentTimeMillis()
            val raw = withContext(Dispatchers.IO) { Saf.scan(ctx, Uri.parse(u)) }
            items = raw.map { Names.parse(it) }.sortedByDescending { it.timeMillis }
            elapsed = System.currentTimeMillis() - t0
            scanning = false
            if (alsoIcons) matchIcons(60)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            prefs.edit().putString("tree", uri.toString()).apply()
            tree = uri.toString()
        }
    }

    val syncPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            prefs.edit().putString("sync_tree", uri.toString()).apply()
            syncTree = uri.toString()
            syncMsg = null
        }
    }

    LaunchedEffect(tree) { tree?.let { rescan(it) } }

    val shown = named
    val searchResults = remember(named, query, transcripts) {
        if (query.isBlank()) emptyList()
        else named.filter { r ->
            r.title.contains(query, true) ||
                r.number.contains(query) ||
                (transcripts[r.id]?.contains(query, true) == true)
        }
    }
    val active = remember(shown, ui) { shown.filter { it.id !in ui.archived && it.id !in ui.deleted } }
    val archivedList = remember(named, ui) {
        named.filter { it.id in ui.archived }.sortedByDescending { ui.archived[it.id] ?: 0L }
    }

    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    AppTheme(themeMode) {
        val cs = MaterialTheme.colorScheme
        val bg = if (dark) cs.surface else lerp(Color.White, cs.primary, 0.07f)
        val card = if (dark) cs.surfaceContainerHigh else Color.White

        Surface(Modifier.fillMaxSize(), color = bg) {
            Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.padding(start = if (wide) 80.dp else 0.dp),
                containerColor = Color.Transparent,
                bottomBar = {
                    if (!wide) {
                        AppNavBar(
                            page = page,
                            archivedCount = archivedList.size,
                            favCount = favList.size,
                            onSelect = { page = it },
                            onSize = { navBarPx = it },
                        )
                    }
                },
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        when {
                                            archiveOpen -> "归档"
                                            favOpen -> "收藏"
                                            settingsOpen -> "设置"
                                            selected.isNotEmpty() -> "已选 ${selected.size} 项"
                                            else -> "通话录音"
                                        }
                                    )
                                    if (!archiveOpen && !favOpen && !settingsOpen && selected.isEmpty()) {
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "${named.size} 条",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                if (settingsOpen) IconButton(onClick = { page = 0; archiveSel = emptySet() }) {
                                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "返回")
                                }
                            },
                            actions = {
                                if (archiveOpen || favOpen) {
                                    Text(
                                        "${if (archiveOpen) archivedList.size else favList.size} 条",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(end = 16.dp),
                                    )
                                } else if (selected.isNotEmpty()) {
                                    IconButton(onClick = { selected = emptySet() }) {
                                        Icon(Icons.Default.Close, contentDescription = "取消")
                                    }
                                } else {
                                    IconButton(onClick = { tree?.let { rescan(it, alsoIcons = true) } }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                                    }
                                }
                            },
                        )
                        if (!archiveOpen && !favOpen && !settingsOpen && selected.isEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = cs.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .height(52.dp)
                                    .clickable { searchOpen = true },
                            ) {
                                Row(
                                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "搜索号码、联系人、转写内容",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                    }
                },
            ) { pad ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(pad)
                        .padding(end = if (wide && current != null) 440.dp else 0.dp)
                ) {
                    when {
                        archiveOpen -> ArchivePane(
                            list = archivedList,
                            icons = icons,
                            archivedAt = { ui.archived[it] ?: 0L },
                            card = card,
                            durations = durations,
                            selected = archiveSel,
                            onToggle = { id ->
                                archiveSel = if (id in archiveSel) archiveSel - id else archiveSel + id
                            },
                            onLong = { id -> archiveSel = archiveSel + id },
                            onDelete = { ids ->
                                mut { st ->
                                    st.copy(deleted = st.deleted + ids, archived = st.archived - ids)
                                }
                                archiveSel = emptySet()
                            },
                        )

                        settingsOpen -> SettingsPane(
                            themeMode = themeMode,
                            onThemeMode = {
                                themeMode = it
                                prefs.edit().putString("theme", it).apply()
                            },
                            tree = tree,
                            onPickFolder = { picker.launch(null) },
                            count = items.size,
                            elapsed = elapsed,
                            card = card,
                            onRescan = { tree?.let { rescan(it, alsoIcons = true) } },
                            aiBase = aiBase,
                            aiKey = aiKey,
                            aiModel = aiModel,
                            onAiEdit = { field, value ->
                                when (field) {
                                    "base" -> { aiBase = value; prefs.edit().putString("ai_base", value).apply() }
                                    "key" -> { aiKey = value; prefs.edit().putString("ai_key", value).apply() }
                                    else -> { aiModel = value; prefs.edit().putString("ai_model", value).apply() }
                                }
                            },
                            onExit = { page = 0 },
                            autoTranscribe = autoTranscribe,
                            onAutoTranscribe = {
                                autoTranscribe = it
                                prefs.edit().putBoolean("auto_transcribe", it).apply()
                            },
                            doneCount = transcripts.size,
                            syncTree = syncTree,
                            onPickSyncFolder = { syncPicker.launch(null) },
                            onSyncNow = { syncNow() },
                            syncing = syncing,
                            syncMsg = syncMsg,
                            summaryCount = summaries.size,
                            iconCount = icons.size,
                            iconRunning = iconRunning,
                            iconTotal = iconTotal,
                            iconDone = iconDone,
                            iconFail = iconFail,
                            iconErr = iconErr,
                            onMatchIcons = { matchIcons() },
                            onClearIcons = { clearIcons() },
                        )

                        favOpen -> FavPane(
                            list = favList,
                            icons = icons,
                            card = card,
                            durations = durations,
                            onPlay = { play(it) },
                            onUnfav = { id -> mut { it.copy(fav = it.fav - id) } },
                        )

                        tree == null -> Center {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("还没选择录音目录", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { picker.launch(null) },
                                    shapes = ButtonDefaults.shapes(),
                                ) { Text("选择目录") }
                            }
                        }

                        else -> {
                            val prState = rememberPullToRefreshState()
                            val listState = rememberLazyListState()
                            val scaleFraction = {
                                if (scanning) 1f
                                else LinearOutSlowInEasing.transform(prState.distanceFraction)
                                    .coerceIn(0f, 1f)
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .pullToRefresh(
                                        state = prState,
                                        isRefreshing = scanning,
                                        onRefresh = { tree?.let { rescan(it, alsoIcons = true) } },
                                    )
                            ) {
                                if (active.isEmpty()) {
                                    Center {
                                        Text(if (query.isBlank()) "没有未归档的录音" else "没有匹配的录音")
                                    }
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        contentPadding = PaddingValues(
                                            start = 16.dp,
                                            end = 16.dp,
                                            top = 8.dp,
                                            bottom = when {
                                                selected.isNotEmpty() -> 140.dp
                                                current != null && playerCollapsed -> 100.dp
                                                else -> 24.dp
                                            },
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        itemsIndexed(
                                            items = active,
                                            key = { _, r -> r.id },
                                            contentType = { _, _ -> "rec" },
                                        ) { i, r ->
                                            SwipeActions(
                                                key = r.id,
                                                modifier = Modifier.animateItem(fadeInSpec = null),
                                                onArchive = {
                                                    mut {
                                                        it.copy(
                                                            archived = it.archived +
                                                                (r.id to System.currentTimeMillis())
                                                        )
                                                    }
                                                },
                                                onDelete = { mut { it.copy(deleted = it.deleted + r.id) } },
                                            ) {
                                                RecordingRow(
                                                    r = r,
                                                    card = card,
                                                    index = i,
                                                    count = active.size,
                                                    selected = r.id in selected,
                                                    durationMs = durations[r.id],
                                                    iconRes = icons[r.id]?.let { IconSet.res(ctx, it) } ?: 0,
                                                    onClick = {
                                                        if (selected.isNotEmpty()) {
                                                            selected = if (r.id in selected) selected - r.id
                                                            else selected + r.id
                                                        } else {
                                                            play(r)
                                                        }
                                                    },
                                                    onLongClick = { selected = selected + r.id },
                                                )
                                            }
                                        }
                                    }
                                }
                                FastScrollHandle(
                                    state = listState,
                                    count = active.size,
                                    bottomInset = if (current != null && playerCollapsed) 96.dp else 0.dp,
                                    labelAt = { i ->
                                        active.getOrNull(i)?.let { fmtDay(it.timeMillis) } ?: ""
                                    },
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .graphicsLayer {
                                            scaleX = scaleFraction()
                                            scaleY = scaleFraction()
                                        }
                                ) {
                                    PullToRefreshDefaults.LoadingIndicator(
                                        state = prState,
                                        isRefreshing = scanning,
                                    )
                                }
                            }
                        }
                    }

                    if (!wide && !archiveOpen && !favOpen && !settingsOpen && !chatOpen &&
                        selected.isEmpty() && archiveSel.isEmpty()
                    ) {
                        LargeFloatingActionButton(
                            onClick = { chatOpen = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    end = 16.dp,
                                    bottom = if (current != null && playerCollapsed) 100.dp else 16.dp,
                                ),
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_sparkle),
                                contentDescription = "AI 对话",
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = archiveOpen && archiveSel.isNotEmpty(),
                        enter = fadeIn() + scaleIn(initialScale = 0.92f),
                        exit = fadeOut() + scaleOut(targetScale = 0.92f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (current != null && playerCollapsed) 100.dp else 16.dp),
                    ) {
                        HorizontalFloatingToolbar(
                            expanded = true,
                            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                            floatingActionButton = {
                                FloatingToolbarDefaults.VibrantFloatingActionButton(
                                    onClick = {
                                        mut { st ->
                                            st.copy(
                                                deleted = st.deleted + archiveSel,
                                                archived = st.archived - archiveSel,
                                            )
                                        }
                                        archiveSel = emptySet()
                                    },
                                ) {
                                    Icon(painterResource(R.drawable.ic_delete), contentDescription = "删除")
                                }
                            },
                            floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.End,
                        ) {
                            IconButton(
                                onClick = {
                                    mut { it.copy(archived = it.archived - archiveSel) }
                                    archiveSel = emptySet()
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "恢复")
                            }
                            IconButton(onClick = { archiveSel = archivedList.map { it.id }.toSet() }) {
                                Icon(painterResource(R.drawable.ic_select_all), contentDescription = "全选")
                            }
                            IconButton(
                                onClick = {
                                    archiveSel = archivedList.map { it.id }.toSet() - archiveSel
                                }
                            ) {
                                Icon(painterResource(R.drawable.ic_deselect), contentDescription = "反选")
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = !archiveOpen && selected.isNotEmpty(),
                        enter = fadeIn() + scaleIn(initialScale = 0.92f),
                        exit = fadeOut() + scaleOut(targetScale = 0.92f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (current != null && playerCollapsed) 100.dp else 16.dp),
                    ) {
                        HorizontalFloatingToolbar(
                            expanded = true,
                            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                            floatingActionButton = {
                                FloatingToolbarDefaults.VibrantFloatingActionButton(
                                    onClick = {
                                        mut { it.copy(deleted = it.deleted + selected) }
                                        selected = emptySet()
                                    },
                                ) {
                                    Icon(painterResource(R.drawable.ic_delete), contentDescription = "删除")
                                }
                            },
                            floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.End,
                        ) {
                            IconButton(onClick = { selected = active.map { it.id }.toSet() }) {
                                Icon(painterResource(R.drawable.ic_select_all), contentDescription = "全选")
                            }
                            IconButton(onClick = { selected = active.map { it.id }.toSet() - selected }) {
                                Icon(painterResource(R.drawable.ic_deselect), contentDescription = "反选")
                            }
                            IconButton(onClick = { share(ctx, selected.toList()) }) {
                                Icon(Icons.Default.Share, contentDescription = "分享")
                            }
                            IconButton(
                                onClick = {
                                    val allFav = selected.all { it in ui.fav }
                                    mut { st ->
                                        st.copy(fav = if (allFav) st.fav - selected else st.fav + selected)
                                    }
                                }
                            ) {
                                Icon(
                                    if (selected.isNotEmpty() && selected.all { it in ui.fav })
                                        Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "收藏",
                                )
                            }
                            IconButton(
                                onClick = {
                                    mut {
                                        it.copy(archived = it.archived + selected.associateWith { System.currentTimeMillis() })
                                    }
                                    selected = emptySet()
                                }
                            ) {
                                Icon(painterResource(R.drawable.ic_archive), contentDescription = "归档")
                            }
                        }
                    }
                }
            }

            if (wide) {
                AppRail(
                    modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(80.dp),
                    page = page,
                    archivedCount = archivedList.size,
                    favCount = favList.size,
                    onSelect = { page = it },
                    onAi = { chatOpen = true },
                    card = card,
                )
            }

            val cur = current
            if (cur != null) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                PlayerPanel(
                    screenHeight = maxHeight - navBarH,
                    bottomInset = 0.dp,
                    collapsed = if (wide) false else playerCollapsed,
                    onCollapsedChange = { playerCollapsed = it },
                    panelModifier = if (wide) {
                        Modifier.width(440.dp).align(Alignment.CenterEnd)
                    } else {
                        Modifier.fillMaxWidth()
                    },
                    dragEnabled = !wide,
                    collapseEnabled = !wide,
                    onClose = {
                        runCatching { controller?.stop() }
                        current = null
                        playing = false
                        posMs = 0
                        durMs = 0
                    },
                    title = cur.title,
                    sub = "${dirLabel(cur.direction)} · ${fmtTime(cur.timeMillis)}",
                    meta = meta,
                    fav = cur.id in ui.fav,
                    onToggleFav = {
                        mut { st ->
                            st.copy(fav = if (cur.id in st.fav) st.fav - cur.id else st.fav + cur.id)
                        }
                    },
                    speed = speed,
                    onSpeed = { showSpeed = true },
                    onSave = { share(ctx, listOf(cur.id)) },
                    cardColor = card,
                    transcript = transcript,
                    transcribing = transcribing,
                    status = status,
                    transferProgress = if (transcribing) dlProgress else null,
                    onTranscribe = {
                        scope.launch {
                            transcribing = true
                            summary = summaries[cur.id]
                            status = "准备模型…"
                            transcript = runCatching {
                                Asr.transcribe(ctx, Uri.parse(cur.id)) { s -> status = s }
                            }.getOrElse { "转写失败：" + (it.message ?: "未知错误") }
                            transcripts = Transcripts.put(ctx, cur.id, transcript.orEmpty())
                            status = null
                            transcribing = false
                        }
                    },
                    summary = summary,
                    summarizing = summarizing,
                    onSummarize = {
                        scope.launch {
                            summarizing = true
                            val got = runCatching {
                                Ai.summarize(aiBase, aiKey, aiModel, transcript.orEmpty())
                            }.getOrElse { "总结失败：" + (it.message ?: "未知错误") }
                            summary = got
                            if (!got.startsWith("总结失败")) {
                                summaries = Summaries.put(ctx, cur.id, got)
                            }
                            summarizing = false
                        }
                    },
                    seed = cur.id,
                    positionMs = posMs,
                    durationMs = durMs,
                    playing = playing,
                    card = bg,
                    onToggle = { if (playing) controller?.pause() else controller?.play() },
                    onSkip = { sec ->
                        val c = controller
                        if (c != null) {
                            val dur = c.duration
                            val target = c.currentPosition + sec * 1000L
                            val clamped = if (dur > 0) target.coerceIn(0L, dur - 1) else target.coerceAtLeast(0L)
                            c.seekTo(clamped)
                            if (c.playbackState == Player.STATE_ENDED) c.play()
                        }
                    },
                    onSeekFraction = { f ->
                        if (durMs > 0) {
                            val target = (f * durMs).toLong()
                            posMs = target
                            seekGuard = System.currentTimeMillis() + 700
                            controller?.seekTo(target)
                        }
                    },
                )
                }
            }

            if (searchOpen) {
                SearchPane(
                    query = query,
                    onQuery = { query = it },
                    results = searchResults,
                    icons = icons,
                    transcripts = transcripts,
                    durations = durations,
                    onPick = { r ->
                        searchOpen = false
                        play(r)
                    },
                    onClose = { searchOpen = false },
                    bg = bg,
                    card = card,
                )
            }

            if (chatOpen) {
                AiChatPane(
                    messages = chat,
                    thinking = thinking,
                    draft = draftMsg,
                    onDraft = { draftMsg = it },
                    onSend = { sendChat() },
                    onClose = { chatOpen = false },
                    bg = bg,
                    card = card,
                )
            }

            if (showSpeed) {
                ModalBottomSheet(onDismissRequest = { showSpeed = false }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp),
                    ) {
                        Text("播放速度", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${"%.1f".format(speed)}×",
                            style = MaterialTheme.typography.displaySmall,
                        )
                        Slider(
                            value = speed,
                            onValueChange = {
                                speed = it
                                runCatching {
                                    controller?.playbackParameters = PlaybackParameters(it)
                                }
                            },
                            valueRange = 0.5f..3f,
                            steps = 24,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("0.5×", style = MaterialTheme.typography.labelMedium)
                            Text("3.0×", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchPane(
    query: String,
    onQuery: (String) -> Unit,
    results: List<Recording>,
    icons: Map<String, String>,
    transcripts: Map<String, String>,
    durations: Map<String, Long>,
    onPick: (Recording) -> Unit,
    onClose: () -> Unit,
    bg: Color,
    card: Color,
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    // 照官方 SearchBarSamples：一个 SearchBar，expanded 状态下把结果放进它的 content。
    var expanded by remember { mutableStateOf(true) }

    SearchBar(
        modifier = Modifier.fillMaxWidth(),
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQuery,
                onSearch = { },
                expanded = expanded,
                onExpandedChange = { },
                placeholder = { Text("搜索号码、联系人、转写内容") },
                leadingIcon = {
                    if (expanded) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    } else {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null)
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    } else {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                },
            )
        },
        expanded = expanded,
        onExpandedChange = { if (!it) onClose() else expanded = it },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when {
                query.isBlank() -> Text(
                    "在号码、联系人名和转写文本里查找，点结果直接播放",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )

                results.isEmpty() -> Text(
                    "没有找到「$query」",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )

                else -> {
                    Text(
                        "${results.size} 条结果",
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.onSurfaceVariant,
                    )
                    val shown = results.take(100)
                    shown.forEachIndexed { index, r ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 1.dp)) {
                            RecordingRow(
                                r = r,
                                card = card,
                                index = index,
                                count = shown.size,
                                selected = false,
                                durationMs = durations[r.id],
                                iconRes = icons[r.id]?.let { IconSet.res(ctx, it) } ?: 0,
                                highlight = query,
                                onClick = { onPick(r) },
                                onLongClick = { },
                            )
                        }
                    }
                    if (results.size > shown.size) {
                        Text(
                            "只显示前 ${shown.size} 条，再输入几个字缩小范围",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/** 把 query 在 text 里的每一处都加上高亮底色。 */
private fun hi(text: String, q: String, style: SpanStyle): AnnotatedString {
    if (q.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        var i = 0
        while (true) {
            val j = text.indexOf(q, i, ignoreCase = true)
            if (j < 0) break
            addStyle(style, j, j + q.length)
            i = j + q.length
        }
    }
}

private fun snippet(text: String?, q: String): String? {
    if (text.isNullOrBlank() || q.isBlank()) return null
    val i = text.indexOf(q, ignoreCase = true)
    if (i < 0) return null
    val start = (i - 24).coerceAtLeast(0)
    val end = (i + q.length + 40).coerceAtMost(text.length)
    val head = if (start > 0) "…" else ""
    val tail = if (end < text.length) "…" else ""
    return head + text.substring(start, end).replace('\n', ' ') + tail
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AiChatPane(
    messages: List<Pair<String, String>>,
    thinking: Boolean,
    draft: String,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
    bg: Color,
    card: Color,
) {
    val cs = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, thinking) {
        val target = messages.size + if (thinking) 1 else 0
        if (target > 0) listState.animateScrollToItem(target - 1)
    }

    Surface(Modifier.fillMaxSize(), color = bg) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "返回")
                }
                Text("AI 对话", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Icon(
                    painterResource(R.drawable.ic_sparkle),
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.padding(end = 14.dp).size(22.dp),
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            "可以问：这些录音里说过什么？谁答应了什么事？某天那通电话讲了什么？",
                            color = cs.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(messages) { msg ->
                    val fromUser = msg.first == "user"
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
                    ) {
                        Surface(
                            color = if (fromUser) cs.primaryContainer else card,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.widthIn(max = 320.dp),
                        ) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    msg.second,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
                if (thinking) {
                    item {
                        Row(Modifier.fillMaxWidth()) {
                            LoadingIndicator(Modifier.size(28.dp))
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraft,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("问点什么…") },
                    maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onSend, enabled = draft.isNotBlank() && !thinking) {
                    Icon(Icons.Default.Send, contentDescription = "发送")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsPane(
    themeMode: String,
    onThemeMode: (String) -> Unit,
    tree: String?,
    onPickFolder: () -> Unit,
    count: Int,
    elapsed: Long,
    card: Color,
    onRescan: () -> Unit,
    aiBase: String,
    aiKey: String,
    aiModel: String,
    onAiEdit: (String, String) -> Unit,
    onExit: () -> Unit,
    autoTranscribe: Boolean,
    onAutoTranscribe: (Boolean) -> Unit,
    doneCount: Int,
    syncTree: String?,
    onPickSyncFolder: () -> Unit,
    onSyncNow: () -> Unit,
    syncing: Boolean,
    syncMsg: String?,
    summaryCount: Int,
    iconCount: Int,
    iconRunning: Boolean,
    iconTotal: Int,
    iconDone: Int,
    iconFail: Int,
    iconErr: String?,
    onMatchIcons: () -> Unit,
    onClearIcons: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val modes = listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色")
    val cats = listOf(
        Triple("theme", "外观", "深色 / 浅色 / 跟随系统"),
        Triple("rec", "录音", "文件夹、刷新、条数"),
        Triple("ai", "AI 总结", "接口地址、API Key、模型名"),
        Triple("sync", "同步", "转写与 AI 总结跨设备同步"),
        Triple("about", "关于", "版本与说明"),
    )
    val catIcon = mapOf(
        "theme" to R.drawable.ic_palette,
        "rec" to R.drawable.ic_ms_folder,
        "ai" to R.drawable.ic_sparkle,
        "sync" to R.drawable.ic_archive,
        "about" to R.drawable.ic_info,
    )

    var page by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }

    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = true) { progress ->
        runCatching {
            progress.collect { backProgress = it.progress }
        }
        backProgress = 0f
        if (page != null) page = null else onExit()
    }

    if (editing != null) {
        val field = editing!!
        AlertDialog(
            onDismissRequest = { editing = null },
            title = {
                Text(when (field) { "base" -> "接口地址"; "key" -> "API Key"; else -> "模型名" })
            },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            when (field) {
                                "base" -> "https://api.openai.com/v1"
                                "key" -> "sk-..."
                                else -> "gpt-4o-mini"
                            }
                        )
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { onAiEdit(field, draft.trim()); editing = null }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("取消") } },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = backProgress.coerceIn(0f, 1f)
                scaleX = 1f - 0.08f * p
                scaleY = 1f - 0.08f * p
                alpha = 1f - 0.35f * p
            },
    ) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (page == null) {
            itemsIndexed(cats) { index, cat ->
                SegmentedListItem(
                    onClick = { page = cat.first },
                    shapes = ListItemDefaults.segmentedShapes(index, cats.size),
                    colors = ListItemDefaults.segmentedColors(containerColor = card),
                    supportingContent = {
                        Text(cat.third, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    leadingContent = {
                        Icon(
                            painterResource(catIcon.getValue(cat.first)),
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    trailingContent = {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    },
                ) {
                    Text(cat.second, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            item {
                SectionTitle(cats.first { it.first == page }.second)
            }
            when (page) {
                "theme" -> itemsIndexed(modes) { index, mode ->
                    SegmentedListItem(
                        onClick = { onThemeMode(mode.first) },
                        shapes = ListItemDefaults.segmentedShapes(index, modes.size),
                        colors = ListItemDefaults.segmentedColors(containerColor = card),
                        trailingContent = {
                            if (themeMode == mode.first) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = cs.primary)
                            }
                        },
                    ) {
                        Text(mode.second, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                "rec" -> {
                    item {
                        SegmentedListItem(
                            onClick = onPickFolder,
                            shapes = ListItemDefaults.segmentedShapes(0, 3),
                            colors = ListItemDefaults.segmentedColors(containerColor = card),
                            supportingContent = {
                                Text(tree ?: "还没选择文件夹", maxLines = 2, overflow = TextOverflow.Ellipsis)
                            },
                        ) {
                            Text("录音文件夹", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    item {
                        SegmentedListItem(
                            onClick = onRescan,
                            shapes = ListItemDefaults.segmentedShapes(1, 3),
                            colors = ListItemDefaults.segmentedColors(containerColor = card),
                            supportingContent = { Text("$count 条 · 最近扫描 ${elapsed}ms") },
                        ) {
                            Text("刷新列表", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    item {
                        SegmentedListItem(
                            onClick = { onAutoTranscribe(!autoTranscribe) },
                            shapes = ListItemDefaults.segmentedShapes(2, 3),
                            colors = ListItemDefaults.segmentedColors(containerColor = card),
                            supportingContent = {
                                Text("后台把录音转成文字（只识别，不调 AI）· 已转写 $doneCount 条")
                            },
                            trailingContent = {
                                Switch(checked = autoTranscribe, onCheckedChange = onAutoTranscribe)
                            },
                        ) {
                            Text("自动转写", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                "ai" -> {
                    item {
                        SegmentedListItem(
                            onClick = { editing = "base"; draft = aiBase },
                            shapes = ListItemDefaults.segmentedShapes(0, 5),
                            colors = ListItemDefaults.segmentedColors(containerColor = card),
                            supportingContent = {
                                Text(aiBase.ifBlank { "未设置" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        ) {
                            Text("接口地址", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    item {
                        SegmentedListItem(
                            onClick = { editing = "key"; draft = aiKey },
                            shapes = ListItemDefaults.segmentedShapes(1, 5),
                            colors = ListItemDefaults.segmentedColors(containerColor = card),
                            supportingContent = { Text(if (aiKey.isBlank()) "未设置" else "已设置") },
                        ) {
                            Text("API Key", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    item {
                        SegmentedListItem(
                            onClick = { editing = "model"; draft = aiModel },
                            shapes = ListItemDefaults.segmentedShapes(2, 5),
                            colors = ListItemDefaults.segmentedColors(containerColor = card),
                            supportingContent = {
                                Text(aiModel.ifBlank { "未设置" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        ) {
                            Text("模型名", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    item {
                        SegmentedListItem(
                            onClick = { if (!iconRunning) onMatchIcons() },
                            shapes = ListItemDefaults.segmentedShapes(3, 5),
                            colors = ListItemDefaults.segmentedColors(containerColor = card),
                            supportingContent = {
                                Text(
                                    when {
                                        iconRunning -> "正在匹配 $iconDone / $iconTotal" +
                                            (if (iconFail > 0) " · 失败 $iconFail" else "")
                                        iconErr != null -> iconErr
                                        else -> "点一下给所有还没图标的录音匹配 · 已有 $iconCount 条"
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingContent = {
                                if (iconRunning) {
                                    LoadingIndicator(modifier = Modifier.size(22.dp))
                                } else {
                                    Icon(painterResource(R.drawable.ic_sparkle), contentDescription = null)
                                }
                            },
                        ) {
                            Text("匹配图标", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    item {
                        SegmentedListItem(
                            onClick = { if (!iconRunning) onClearIcons() },
                            shapes = ListItemDefaults.segmentedShapes(4, 5),
                            colors = ListItemDefaults.segmentedColors(containerColor = card),
                            supportingContent = {
                                Text(
                                    "清空所有已匹配的图标（已有 $iconCount 条），可以重新匹配",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = cs.error,
                                )
                            },
                        ) {
                            Text("删除所有图标", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                "sync" -> {
                    item {
                        SegmentedListItem(
                            onClick = onPickSyncFolder,
                            shapes = ListItemDefaults.segmentedShapes(0, 2),
                            colors = ListItemDefaults.segmentedColors(containerColor = card),
                            supportingContent = {
                                Text(
                                    syncTree ?: "选一个云端/共享文件夹（如 NAS 同步目录）",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        ) {
                            Text("同步文件夹", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    item {
                        SegmentedListItem(
                            onClick = { if (!syncing && syncTree != null) onSyncNow() },
                            shapes = ListItemDefaults.segmentedShapes(1, 2),
                            colors = ListItemDefaults.segmentedColors(containerColor = card),
                            supportingContent = {
                                Text(
                                    syncMsg ?: "转写、AI 总结、归档、收藏、聊天记录 · 转写 $doneCount 条 · 总结 $summaryCount 条",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingContent = {
                                if (syncing) {
                                    LoadingIndicator(modifier = Modifier.size(22.dp))
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                }
                            },
                        ) {
                            Text("立即同步", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    item {
                        Text(
                            "两边都改过时按「谁的字多留谁」合并，不会被覆盖成空。",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }

                else -> item {
                    SegmentedListItem(
                        onClick = { },
                        shapes = ListItemDefaults.segmentedShapes(0, 1),
                        colors = ListItemDefaults.segmentedColors(containerColor = card),
                        supportingContent = { Text("Material 3 Expressive · 播放器照 Gramophone") },
                    ) {
                        Text("通话录音 0.1", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SegmentedListItem(
                    onClick = { page = null },
                    shapes = ListItemDefaults.segmentedShapes(0, 1),
                    colors = ListItemDefaults.segmentedColors(containerColor = card),
                    leadingContent = {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                    },
                ) {
                    Text("返回", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    }
}

/** 收起状态的迷你播放条：放在底部栏上面那一层，位置由 Scaffold 自己算，不会盖住导航栏。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiniPlayer(
    title: String,
    timeText: String,
    playing: Boolean,
    card: Color,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var dy by remember { mutableFloatStateOf(0f) }
    Surface(color = card, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(88.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dy = 0f },
                        onVerticalDrag = { change, drag ->
                            change.consume()
                            dy += drag
                        },
                        onDragEnd = { if (dy < -24f) onExpand() },
                    )
                }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExpand) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "展开播放页")
            }
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onExpand() }
                    .padding(start = 4.dp, end = 8.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(onClick = onToggle) {
                Icon(
                    painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (playing) "暂停" else "播放",
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

/** 手机底部栏：录音 / 归档 / 收藏 / 设置，归档和收藏用徽章显示条数。 */
@Composable
private fun AppNavBar(
    page: Int,
    archivedCount: Int,
    favCount: Int,
    onSelect: (Int) -> Unit,
    onSize: (Int) -> Unit = {},
) {
    NavigationBar(modifier = Modifier.onSizeChanged { onSize(it.height) }) {
        NavigationBarItem(
            selected = page == 0,
            onClick = { onSelect(0) },
            icon = { Icon(Icons.Default.Call, contentDescription = null) },
            label = { Text("录音") },
        )
        NavigationBarItem(
            selected = page == 1,
            onClick = { onSelect(1) },
            icon = {
                BadgedIcon(archivedCount) {
                    Icon(painterResource(R.drawable.ic_archive), contentDescription = null)
                }
            },
            label = { Text("归档") },
        )
        NavigationBarItem(
            selected = page == 2,
            onClick = { onSelect(2) },
            icon = {
                BadgedIcon(favCount) {
                    Icon(Icons.Default.Favorite, contentDescription = null)
                }
            },
            label = { Text("收藏") },
        )
        NavigationBarItem(
            selected = page == 3,
            onClick = { onSelect(3) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("设置") },
        )
    }
}

/** 平板侧边栏：顶部一个正常大小的 AI 键，下面是同样的四个入口。 */
@Composable
private fun AppRail(
    modifier: Modifier = Modifier,
    page: Int,
    archivedCount: Int,
    favCount: Int,
    onSelect: (Int) -> Unit,
    onAi: () -> Unit,
    card: Color,
) {
    val cs = MaterialTheme.colorScheme
    NavigationRail(
        modifier = modifier,
        containerColor = card,
        header = {
            FloatingActionButton(
                onClick = onAi,
                containerColor = cs.primaryContainer,
                contentColor = cs.onPrimaryContainer,
            ) {
                Icon(
                    painterResource(R.drawable.ic_sparkle),
                    contentDescription = "AI 对话",
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    ) {
        NavigationRailItem(
            selected = page == 0,
            onClick = { onSelect(0) },
            icon = { Icon(Icons.Default.Call, contentDescription = null) },
            label = { Text("录音") },
        )
        NavigationRailItem(
            selected = page == 1,
            onClick = { onSelect(1) },
            icon = {
                BadgedIcon(archivedCount) {
                    Icon(painterResource(R.drawable.ic_archive), contentDescription = null)
                }
            },
            label = { Text("归档") },
        )
        NavigationRailItem(
            selected = page == 2,
            onClick = { onSelect(2) },
            icon = {
                BadgedIcon(favCount) {
                    Icon(Icons.Default.Favorite, contentDescription = null)
                }
            },
            label = { Text("收藏") },
        )
        NavigationRailItem(
            selected = page == 3,
            onClick = { onSelect(3) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("设置") },
        )
    }
}

@Composable
private fun BadgedIcon(count: Int, icon: @Composable () -> Unit) {
    if (count > 0) {
        BadgedBox(badge = { Badge { Text(if (count > 99) "99+" else "$count") } }) { icon() }
    } else {
        icon()
    }
}

@Composable
private fun FavPane(
    list: List<Recording>,
    icons: Map<String, String>,
    card: Color,
    durations: Map<String, Long>,
    onPlay: (Recording) -> Unit,
    onUnfav: (String) -> Unit,
) {
    val ctx = LocalContext.current
    if (list.isEmpty()) {
        Center { Text("还没有收藏", style = MaterialTheme.typography.titleMedium) }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(list, key = { _, r -> r.id }) { i, r ->
            SegmentedListItem(
                onClick = { onPlay(r) },
                shapes = ListItemDefaults.segmentedShapes(index = i, count = list.size),
                colors = ListItemDefaults.segmentedColors(containerColor = card),
                leadingContent = {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            val ir = icons[r.id]?.let { IconSet.res(ctx, it) } ?: 0
                            if (ir != 0) {
                                Icon(painterResource(ir), contentDescription = null, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                },
                trailingContent = {
                    IconButton(onClick = { onUnfav(r.id) }) {
                        Icon(Icons.Default.Favorite, contentDescription = "取消收藏")
                    }
                },
                supportingContent = {
                    Text(
                        "${dirLabel(r.direction)} · ${fmtTime(r.timeMillis)} · " +
                            (durations[r.id]?.let { fmtDur(it) + " · " } ?: "") + fmtSize(r.size),
                        maxLines = 1,
                    )
                },
            ) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

/** 右侧拖动柄：拖动时左边浮出当前那一格的时间，气泡在整块宽度里自由向左展开。 */
@Composable
private fun FastScrollHandle(
    state: LazyListState,
    count: Int,
    labelAt: (Int) -> String,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    if (count <= 0) return
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    var dragging by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableIntStateOf(0) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val thumbH = 56.dp
        val track = (maxHeight - thumbH - 24.dp - bottomInset).coerceAtLeast(0.dp)

        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(32.dp)
                .fillMaxHeight()
                .pointerInput(count) {
                    detectVerticalDragGestures(
                        onDragStart = { off ->
                            dragging = true
                            val f = (off.y / size.height).coerceIn(0f, 1f)
                            dragIndex = (f * (count - 1)).toInt()
                            scope.launch { state.scrollToItem(dragIndex) }
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        change.consume()
                        val f = (change.position.y / size.height).coerceIn(0f, 1f)
                        dragIndex = (f * (count - 1)).toInt()
                        scope.launch { state.scrollToItem(dragIndex) }
                    }
                },
        )

        Row(
            Modifier
                .align(Alignment.TopEnd)
                // 滚动进度只在布局阶段读：composition 不跟着每帧重组，快速翻动才不卡
                .offset {
                    val idx = if (dragging) dragIndex else state.firstVisibleItemIndex
                    val f = (idx.toFloat() / count).coerceIn(0f, 1f)
                    IntOffset(0, (track.toPx() * f).roundToInt() + 12.dp.roundToPx())
                }
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(visible = dragging, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cs.inverseSurface,
                    contentColor = cs.inverseOnSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(end = 14.dp),
                ) {
                    Text(
                        labelAt(dragIndex),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
            Box(
                Modifier
                    .width(6.dp)
                    .height(thumbH)
                    .clip(RoundedCornerShape(50))
                    .background(cs.primary.copy(alpha = 0.85f)),
            )
        }
    }
}

@Composable
private fun ArchivePane(
    list: List<Recording>,
    icons: Map<String, String>,
    archivedAt: (String) -> Long,
    card: Color,
    durations: Map<String, Long>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onLong: (String) -> Unit,
    onDelete: (Set<String>) -> Unit,
) {
    val ctx = LocalContext.current
    if (list.isEmpty()) {
        Center { Text("归档是空的", style = MaterialTheme.typography.titleMedium) }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(list, key = { _, r -> r.id }) { i, r ->
            SwipeActions(
                key = r.id,
                onArchive = { onDelete(setOf(r.id)) },
                onDelete = { onDelete(setOf(r.id)) },
                leftIsDelete = true,
            ) {
                RecordingRow(
                    r = r,
                    card = card,
                    index = i,
                    count = list.size,
                    selected = r.id in selected,
                    durationMs = durations[r.id],
                    iconRes = icons[r.id]?.let { IconSet.res(ctx, it) } ?: 0,
                    onClick = {
                        if (selected.isNotEmpty()) onToggle(r.id) else Unit
                    },
                    onLongClick = { onLong(r.id) },
                    subtitle = "归档于 " + fmtTime(archivedAt(r.id)),
                )
            }
        }
    }
}

@Composable
private fun SwipeActions(
    key: Any,
    modifier: Modifier = Modifier,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    leftIsDelete: Boolean = false,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val swipeMax = with(density) { (maxWidth * 0.94f).toPx() }
        val trigger = with(density) { 88.dp.toPx() }

        val offset = remember(key) { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val archive = rememberUpdatedState(onArchive)
        val remove = rememberUpdatedState(onDelete)

        Box(Modifier.fillMaxWidth()) {
            if (offset.value != 0f) {
                val shown = abs(offset.value)
                val progress = (shown / trigger).coerceIn(0f, 1f)
                val toArchive = offset.value < 0
                val isDelete = !toArchive || leftIsDelete
                Box(Modifier.matchParentSize()) {
                    Box(
                        Modifier
                            .align(if (toArchive) Alignment.CenterEnd else Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(with(density) { shown.toDp() })
                            .clip(RoundedCornerShape(50))
                            .background(if (isDelete) cs.error else cs.tertiary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(if (isDelete) R.drawable.ic_delete else R.drawable.ic_archive),
                            contentDescription = if (isDelete) "删除" else "归档",
                            tint = (if (isDelete) cs.onError else cs.onTertiary).copy(alpha = progress),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Box(
                Modifier
                    .offset { IntOffset(offset.value.roundToInt(), 0) }
                    .pointerInput(key) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                scope.launch {
                                    offset.snapTo((offset.value + amount).coerceIn(-swipeMax, swipeMax))
                                }
                            },
                            onDragEnd = {
                                val gone = offset.value
                                scope.launch {
                                    if (abs(gone) >= trigger) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        offset.animateTo(
                                            if (gone < 0) -swipeMax else swipeMax,
                                            tween(durationMillis = 220),
                                        )
                                        if (gone < 0) archive.value() else remove.value()
                                    } else {
                                        offset.animateTo(0f, tween(180))
                                    }
                                }
                            },
                            onDragCancel = { scope.launch { offset.animateTo(0f, tween(180)) } },
                        )
                    },
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecordingRow(
    r: Recording,
    card: Color,
    index: Int,
    count: Int,
    selected: Boolean,
    durationMs: Long? = null,
    iconRes: Int = 0,
    highlight: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    subtitle: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    val hl = SpanStyle(background = cs.primaryContainer, color = cs.onPrimaryContainer)
    // 左边永远是蓝底圆：选中显示勾，匹配到图标显示图标，还没匹配就是空的
    val lead: @Composable () -> Unit = {
        Surface(
            shape = CircleShape,
            color = if (selected) cs.primary else cs.primaryContainer,
            contentColor = if (selected) cs.onPrimary else cs.onPrimaryContainer,
        ) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已选择",
                        modifier = Modifier.size(20.dp),
                    )
                } else if (iconRes != 0) {
                    Icon(
                        painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
    val sub = subtitle ?: "${dirLabel(r.direction)} · ${fmtTime(r.timeMillis)} · " +
        (durationMs?.let { fmtDur(it) + " · " } ?: "") + fmtSize(r.size)
    SegmentedListItem(
        onClick = onClick,
        onLongClick = onLongClick,
        shapes = if (selected) ListItemDefaults.segmentedShapes(index = 0, count = 1)
        else ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = if (selected) cs.secondaryContainer else card,
        ),
        leadingContent = lead,
        supportingContent = {
            Text(
                text = if (highlight.isNullOrBlank()) AnnotatedString(sub) else hi(sub, highlight, hl),
                maxLines = 1,
            )
        },
    ) {
        Text(
            text = if (highlight.isNullOrBlank()) AnnotatedString(r.title) else hi(r.title, highlight, hl),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private fun share(ctx: Context, ids: List<String>) {
    if (ids.isEmpty()) return
    val uris = ArrayList(ids.map { Uri.parse(it) })
    val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "audio/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { ctx.startActivity(Intent.createChooser(send, "分享录音")) }
}

@Composable
private fun Center(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private fun dirLabel(d: Direction) = when (d) {
    Direction.IN -> "呼入"
    Direction.OUT -> "呼出"
    Direction.CONFERENCE -> "会议"
    Direction.UNKNOWN -> "未知"
}

private val TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm")

private fun fmtTime(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(TIME_FMT)

private val DAY_FMT = DateTimeFormatter.ofPattern("MM-dd")

private fun fmtDay(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DAY_FMT)

private fun fmtDur(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return if (total >= 3600) "%d:%02d:%02d".format(total / 3600, total % 3600 / 60, total % 60)
    else "%d:%02d".format(total / 60, total % 60)
}

private fun fmtMeta(f: Format): String {
    val parts = ArrayList<String>(3)
    if (f.sampleRate != Format.NO_VALUE) parts += "%.1fkHz".format(f.sampleRate / 1000f)
    if (f.channelCount != Format.NO_VALUE) parts += "${f.channelCount}ch"
    if (f.bitrate != Format.NO_VALUE) parts += "${f.bitrate / 1000}kbps"
    return parts.joinToString(" / ")
}

private fun fmtSize(b: Long): String = when {
    b >= 1024 * 1024 -> String.format("%.1f MB", b / 1048576.0)
    b >= 1024 -> String.format("%.0f KB", b / 1024.0)
    else -> "$b B"
}
