package com.leo.bcrgui

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

private val MiniHeight = 88.dp

/**
 * Gramophone 那套全屏播放页：顶部收起箭头，方形封面，标题/副标题，波浪进度，
 * 时间与音频参数，播放控制，底部一排（歌词 / 循环 / 随机 / 收藏）。
 * 上滑下滑都是跟手的，松手吸附到「展开」或「收起」。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerPanel(
    screenHeight: Dp,
    collapsed: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
    title: String,
    sub: String,
    meta: String,
    fav: Boolean,
    onToggleFav: () -> Unit,
    speed: Float,
    onSpeed: () -> Unit,
    onSave: () -> Unit,
    cardColor: Color,
    transcript: String?,
    transcribing: Boolean,
    status: String?,
    transferProgress: Float?,
    onTranscribe: () -> Unit,
    summary: String?,
    summarizing: Boolean,
    onSummarize: () -> Unit,
    seed: String,
    positionMs: Long,
    durationMs: Long,
    playing: Boolean,
    card: Color,
    onToggle: () -> Unit,
    onSkip: (Int) -> Unit,
    onSeekFraction: (Float) -> Unit,
    bottomInset: Dp = 0.dp,
    panelModifier: Modifier = Modifier,
    dragEnabled: Boolean = true,
    collapseEnabled: Boolean = true,
    onClose: () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val maxOffset = with(density) { (screenHeight - MiniHeight - bottomInset).toPx() }
    val offset = remember { Animatable(if (collapsed) maxOffset else 0f) }
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    LaunchedEffect(collapsed) {
        offset.animateTo(if (collapsed) maxOffset else 0f, tween(280))
    }

    val toggleRef = rememberUpdatedState(onToggle)
    val playButton = remember {
        movableContentOf<Boolean, Dp, Color, Color> { isPlaying, sz, bg, fg ->
            GramophonePlayButton(
                playing = isPlaying,
                size = sz,
                color = bg,
                iconColor = fg,
                onClick = { toggleRef.value() },
            )
        }
    }

    val dragModifier = if (dragEnabled) {
        Modifier.pointerInput(collapsed) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragStart = { dy = 0f },
                    onVerticalDrag = { change, drag ->
                        change.consume()
                        dy += drag
                        scope.launch {
                            offset.snapTo((offset.value + drag).coerceIn(0f, maxOffset))
                        }
                    },
                    onDragEnd = {
                        val threshold = with(density) { 72.dp.toPx() }
                        val collapseNow = when {
                            dy > threshold -> true
                            dy < -threshold -> false
                            else -> offset.value > maxOffset / 2f
                        }
                        scope.launch {
                            offset.animateTo(if (collapseNow) maxOffset else 0f, tween(220))
                            onCollapsedChange(collapseNow)
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offset.animateTo(
                                if (collapsed) maxOffset else 0f,
                                tween(180),
                            )
                        }
                    },
                )
        }
    } else {
        Modifier
    }

    // 收起的整块只露到导航栏上沿（bottomInset），往上拉的越多，底部就越往下长，拉满时正好占满全屏
    Box(
        Modifier
            .fillMaxWidth()
            .layout { measurable, cons ->
                val frac = if (maxOffset > 0f) (offset.value / maxOffset).coerceIn(0f, 1f) else 1f
                val insetPx = with(density) { bottomInset.toPx() }
                val clipH = (cons.maxHeight - insetPx * frac).roundToInt()
                val placeable = measurable.measure(cons.copy(minHeight = 0))
                layout(placeable.width, clipH.coerceAtMost(placeable.height)) { placeable.place(0, 0) }
            }
            .clipToBounds()
    ) {
    Surface(
        modifier = panelModifier
            .fillMaxWidth()
            .height(screenHeight)
            .offset { IntOffset(0, offset.value.roundToInt()) }
            .then(dragModifier),
        shape = RectangleShape,
        color = card,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.fillMaxSize()) {

            // 顶部：收起箭头（收起时同一行变成迷你信息 + 播放键）
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (collapsed) Modifier else Modifier.statusBarsPadding())
                    .height(MiniHeight)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (collapseEnabled) {
                    IconButton(onClick = {
                        if (collapsed) {
                            onCollapsedChange(false)
                        } else {
                            scope.launch {
                                offset.animateTo(maxOffset, tween(220))
                                onCollapsedChange(true)
                            }
                        }
                    }) {
                        Icon(
                            if (collapsed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (collapsed) "展开" else "收起",
                        )
                    }
                } else {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                if (collapsed) {
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(
                            "${clock(positionMs)} / ${clock(durationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    playButton(playing, 52.dp, cs.primaryContainer, cs.onPrimaryContainer)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }

            if (!collapsed) {
                Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 2.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = cs.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                    )
                }


                Spacer(Modifier.height(12.dp))

                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // 转写卡片
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(cardColor),
                    ) {
                        when {
                            transcribing -> Column(
                                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (transferProgress != null) {
                                    LinearWavyProgressIndicator(
                                        progress = { transferProgress },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    ContainedLoadingIndicator(modifier = Modifier.size(96.dp))
                                }
                                if (!status.isNullOrBlank()) {
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        status,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                            }

                            transcript.isNullOrBlank() -> Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Button(
                                    onClick = onTranscribe,
                                    shapes = ButtonDefaults.shapes(),
                                    modifier = Modifier.fillMaxWidth(0.62f).height(80.dp),
                                ) {
                                    Text("转写", style = MaterialTheme.typography.headlineSmall)
                                }
                            }

                            else -> Column(
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                            ) {
                                Text(
                                    "转写",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cs.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(transcript, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }

                    // AI 总结卡片（右上角星标）
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(cardColor),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sparkle),
                            contentDescription = "AI",
                            tint = cs.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp)
                                .size(20.dp),
                        )
                        when {
                            summarizing -> Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingIndicator(modifier = Modifier.size(56.dp))
                            }

                            summary.isNullOrBlank() -> Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Button(
                                    onClick = onSummarize,
                                    enabled = !transcript.isNullOrBlank(),
                                    shapes = ButtonDefaults.shapes(),
                                    modifier = Modifier.fillMaxWidth(0.7f).height(64.dp),
                                ) {
                                    Text(
                                        if (transcript.isNullOrBlank()) "先转写，再让 AI 总结" else "AI 总结",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }

                            else -> Column(
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                            ) {
                                Spacer(Modifier.height(12.dp))
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(summary, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }


                SquigglyProgress(
                    progress = progress,
                    animate = playing,
                    color = cs.primary,
                    onSeek = onSeekFraction,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(30.dp),
                )

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(clock(positionMs), style = MaterialTheme.typography.labelLarge)
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text(clock(durationMs), style = MaterialTheme.typography.labelLarge)
                }

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onSkip(-10) }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_skip_previous_filled),
                            contentDescription = "后退 10 秒",
                            tint = cs.onSurface,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    playButton(playing, 84.dp, cs.primaryContainer, cs.onPrimaryContainer)
                    IconButton(onClick = { onSkip(10) }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_skip_next_filled),
                            contentDescription = "前进 10 秒",
                            tint = cs.onSurface,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }

                // 底部一排：保存 / 收藏 / 调速
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onSave) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "保存",
                            tint = cs.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onToggleFav) {
                        Icon(
                            if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (fav) "取消收藏" else "收藏",
                            tint = if (fav) cs.primary else cs.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onSpeed) {
                        Text("${"%.1f".format(speed)}×", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    }
}

/** Gramophone 的默认封面：同心圆唱片。 */
@Composable
private fun CoverDisc(color: Color) {
    Canvas(Modifier.fillMaxSize(0.62f)) {
        val r = size.minDimension / 2f
        drawCircle(color, radius = r, center = center, style = Stroke(width = r * 0.16f))
        drawCircle(color, radius = r * 0.52f, center = center)
        drawCircle(Color.White, radius = r * 0.12f, center = center)
    }
}

/**
 * Gramophone 的 SquigglyProgress：已播放部分是正弦波，未播放用同一路径画淡线，
 * 振幅在播放头前 1.5 个波长内线性降为 0，所以右边看起来是直线。
 */
@Composable
private fun SquigglyProgress(
    progress: Float,
    animate: Boolean,
    color: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val waveLength = with(density) { 48.dp.toPx() }
    val amplitude = with(density) { 4f.dp.toPx() }
    val stroke = with(density) { 4.5.dp.toPx() }
    val transitionPeriods = 1.5f
    val phaseSpeed = waveLength * 0.5f

    var phase by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var dragP by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var dragging by remember { androidx.compose.runtime.mutableStateOf(false) }
    val shown = if (dragging) dragP else progress
    val heightFraction by animateFloatAsState(
        targetValue = if (animate && !dragging) 1f else if (animate) 1f else 0f,
        animationSpec = tween(if (animate) 800 else 550),
        label = "amp",
    )

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) phase = (phase + (now - last) / 1e9f * phaseSpeed) % waveLength
                last = now
            }
        }
    }

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { off -> onSeek((off.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { off ->
                        dragging = true
                        dragP = (off.x / size.width).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragP = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragging = false
                        onSeek(dragP)
                    },
                    onDragCancel = { dragging = false },
                )
            },
    ) {
        val w = size.width
        val cy = size.height / 2f
        val p = shown.coerceIn(0f, 1f)
        val totalProgressPx = w * p
        val clipTop = amplitude + stroke

        val path = Path()
        val waveStart = -phase - waveLength / 2f
        val half = waveLength / 2f
        val len = transitionPeriods * waveLength

        fun ampAt(x: Float, sign: Float): Float {
            val coeff = ((totalProgressPx + len / 2f - x) / len).coerceIn(0f, 1f)
            return sign * heightFraction * amplitude * coeff
        }

        path.moveTo(waveStart, 0f)
        var x = waveStart
        var sign = 1f
        var amp = ampAt(x, sign)
        while (x < w) {
            sign = -sign
            val nextX = x + half
            val mid = x + half / 2f
            val nextAmp = ampAt(nextX, sign)
            path.cubicTo(mid, amp, mid, nextAmp, nextX, nextAmp)
            amp = nextAmp
            x = nextX
        }

        val style = Stroke(width = stroke, cap = StrokeCap.Round)
        translate(0f, cy) {
            clipRect(0f, -clipTop, totalProgressPx, clipTop) {
                drawPath(path, color, style = style)
            }
            clipRect(totalProgressPx, -clipTop, w, clipTop) {
                drawPath(path, color.copy(alpha = 77f / 255f), style = style)
            }
            val startAmp = cos(abs(waveStart) / waveLength * (2f * PI.toFloat()))
            drawCircle(
                color = color,
                radius = stroke / 2f,
                center = Offset(0f, startAmp * amplitude * heightFraction),
            )
        }

        drawRoundRect(
            color = color,
            topLeft = Offset(totalProgressPx - 2.dp.toPx(), 0f),
            size = Size(4.dp.toPx(), size.height),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}

/** Gramophone 的播放键：bg_play_anim / bg_pause_anim（圆 ⇄ 花瓣）+ ic_media_*_anim（三角 ⇄ 竖条）。 */
@Composable
private fun GramophonePlayButton(
    playing: Boolean,
    size: Dp,
    color: Color,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVector(
            res = if (playing) R.drawable.bg_play_anim else R.drawable.bg_pause_anim,
            tint = color,
            modifier = Modifier.matchParentSize(),
        )
        AnimatedVector(
            res = if (playing) R.drawable.ic_media_play_anim else R.drawable.ic_media_pause_anim,
            tint = iconColor,
            modifier = Modifier.fillMaxSize(0.42f),
        )
    }
}

@Composable
private fun AnimatedVector(res: Int, tint: Color, modifier: Modifier = Modifier) {
    val tintList = remember(tint) { ColorStateList.valueOf(tint.toArgb()) }
    AndroidView(
        modifier = modifier,
        factory = { c ->
            ImageView(c).apply {
                setImageResource(res)
                imageTintList = tintList
            }
        },
        update = { iv ->
            if (iv.tag != res) {
                iv.tag = res
                iv.setImageResource(res)
                iv.imageTintList = tintList
                (iv.drawable as? android.graphics.drawable.Animatable)?.start()
            } else {
                iv.imageTintList = tintList
            }
        },
    )
}

private fun clock(ms: Long): String {
    val s = (ms.coerceAtLeast(0) / 1000)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(),
)
