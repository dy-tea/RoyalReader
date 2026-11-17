package app.royalreader.ui.reader

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.royalreader.core.LocalAutoBrightness
import app.royalreader.core.LocalReducedAnimations
import app.royalreader.core.theme.ReaderPalette
import app.royalreader.core.theme.ReaderPalettes
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    onOpenChapter: (novelID: Long, chapterID: Long) -> Unit,
    viewmodel: ReaderViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(
        checkNotNull<androidx.lifecycle.ViewModelStoreOwner>(LocalViewModelStoreOwner.current) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            },
        null
    ),
) {
    val state by viewmodel.uiState.collectAsState()

    // Reading time tracking lives and dies with the reader screen itself.
    DisposableEffect(Unit) {
        viewmodel.trackReadingTime()
        onDispose {
            viewmodel.stopTrackingReadingTime()
        }
    }

    ReaderLayout(
        state = state, viewmodel = viewmodel, onBackClick = onBackClick,
        onOpenChapter = onOpenChapter
    )
}

@Composable
private fun ReaderLayout(
    state: ReaderUiState,
    viewmodel: ReaderViewModel,
    onBackClick: () -> Unit,
    onOpenChapter: (novelID: Long, chapterID: Long) -> Unit,
) {
    val palette = ReaderPalettes.byID(state.settings.paletteID)
    val reducedAnimations = LocalReducedAnimations.current
    val dimAlpha = rememberSystemDimAlpha(enabled = LocalAutoBrightness.current)
    // Controls are toggled by tapping the reading surface.
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    val toggleControls = { controlsVisible = !controlsVisible }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        // It is fine to keep system bars default; the reader surface is colored.
        when {
            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.accent)
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.error, color = palette.text)
                    Spacer(modifier = Modifier.size(12.dp))
                    Button(onClick = viewmodel::retry) { Text("Retry") }
                    TextButton(onClick = onBackClick) { Text("Back") }
                }
            }

            else -> {
                ChapterParagraphs(
                    state = state,
                    palette = palette,
                    viewmodel = viewmodel,
                    onOpenChapter = onOpenChapter,
                    onToggleControls = toggleControls,
                )
            }
        }

        // Ambient dimming overlay follows the system (auto) brightness
        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = dimAlpha)),
            )
        }

        if (state.chapter != null) {
            val controls: @Composable () -> Unit = {
                ReaderControls(
                    state = state,
                    palette = palette,
                    viewmodel = viewmodel,
                    onBackClick = onBackClick,
                    onOpenChapter = onOpenChapter,
                )
            }
            if (reducedAnimations) {
                if (controlsVisible) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) { controls() }
                }
            } else {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    controls()
                }
            }
        }
    }
}

/**
 * Overlay alpha for a system brightness raw value (0..255): full brightness
 * means no overlay, a dark environment dims the surface to at most 70%.
 **/
object ReaderDimming {
    const val MIN_SURFACE = 0.3f

    fun overlayAlpha(rawBrightness: Int): Float {
        val fraction = (rawBrightness / 255f).coerceIn(0f, 1f)
        return (1f - MIN_SURFACE - fraction * (1f - MIN_SURFACE)).coerceIn(0f, 1f - MIN_SURFACE)
    }
}

/**
 * Reads the current system brightness.
 **/
@Composable
private fun rememberSystemDimAlpha(enabled: Boolean): Float {
    if (!enabled) return 0f
    val context = LocalContext.current
    var alpha by remember { mutableFloatStateOf(0f) }
    DisposableEffect(context, enabled) {
        val resolver = context.contentResolver
        val autoKey = "screen_auto_brightness"
        fun read() {
            val raw = runCatching { Settings.System.getInt(resolver, autoKey) }
                .recoverCatching { Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS) }
                .getOrDefault(255)
            alpha = ReaderDimming.overlayAlpha(raw)
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) = read()
        }
        val uris = listOf(
            Settings.System.getUriFor(autoKey),
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
        )
        uris.forEach { uri -> resolver.registerContentObserver(uri, false, observer) }
        read()
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return alpha
}

@Composable
private fun ChapterParagraphs(
    state: ReaderUiState,
    palette: ReaderPalette,
    viewmodel: ReaderViewModel,
    onOpenChapter: (novelID: Long, chapterID: Long) -> Unit,
    onToggleControls: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Restore position from saved progress once the content is loaded.
    LaunchedEffect(state.paragraphs) {
        if (state.restoredAnchor > 0 && state.paragraphs.isNotEmpty()) {
            listState.scrollToItem(index = state.restoredAnchor.coerceAtMost(state.paragraphs.size - 1))
        }
    }

    // Track pause/resume to store scroll anchor and reading time.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewmodel.saveProgress(
                    anchor = listState.firstVisibleItemIndex,
                    atEnd = listState.firstVisibleItemIndex >= state.paragraphs.size - 1,
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewmodel.saveProgress(
                anchor = listState.firstVisibleItemIndex,
                atEnd = listState.firstVisibleItemIndex >= state.paragraphs.size - 1,
            )
        }
    }

    LazyColumn(
        state = listState,
        // Tapping anywhere on the reading surface shows/hides the control bar.
        modifier = Modifier
            .fillMaxSize()
            .clickable { onToggleControls() },
        contentPadding = PaddingValues(
            start = 24.dp, end = 24.dp, top = 48.dp, bottom = 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Text(
                text = state.chapter?.title.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                color = palette.text,
            )
        }
        items(state.paragraphs.size) { index ->
            val paragraph = state.paragraphs[index]
            if (isTableParagraph(paragraph)) {
                val parsedTable = remember(paragraph) { parseTableBlock(paragraph) }
                if (parsedTable != null) {
                    TableBlock(
                        table = parsedTable,
                        palette = palette,
                        fontSizePx = state.settings.fontSizePx,
                        lineHeight = state.settings.lineHeight,
                    )
                } else {
                    Text(
                        text = AnnotatedString.fromHtml(paragraph),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = state.settings.fontSizePx.sp,
                            lineHeight = (state.settings.fontSizePx * state.settings.lineHeight).sp,
                        ),
                        color = palette.text,
                    )
                }
            } else {
                Text(
                    text = AnnotatedString.fromHtml(paragraph),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = state.settings.fontSizePx.sp,
                        lineHeight = (state.settings.fontSizePx * state.settings.lineHeight).sp,
                    ),
                    color = palette.text,
                )
            }
        }
        item(key = "end") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.size(8.dp))
                Row {
                    state.previousChapter?.let {
                        TextButton(onClick = { onOpenChapter(state.novelID, it.id) }) {
                            Text("Previous", color = palette.text)
                        }
                    }
                    state.nextChapter?.let {
                        TextButton(onClick = { onOpenChapter(state.novelID, it.id) }) {
                            Text("Next", color = palette.text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderControls(
    state: ReaderUiState,
    palette: ReaderPalette,
    viewmodel: ReaderViewModel,
    onBackClick: () -> Unit,
    onOpenChapter: (novelID: Long, chapterID: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        // Opaque bar in the reading palette.
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RectangleShape)
            .background(palette.background),
    ) {
        Divider(color = palette.accent.copy(alpha = 0.5f), thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            TextButton(onClick = onBackClick) { Text("Back", color = palette.text) }
            TextButton(onClick = { viewmodel.changeFontSize(-2) }) {
                Text("A-", color = palette.text)
            }
            TextButton(onClick = { viewmodel.changeFontSize(2) }) {
                Text("A+", color = palette.text)
            }
            TextButton(onClick = { viewmodel.changeLineHeight(-0.1f) }) {
                Text("↕ -", color = palette.text)
            }
            TextButton(onClick = { viewmodel.changeLineHeight(0.1f) }) {
                Text("↕ +", color = palette.text)
            }
            state.nextChapter?.let {
                TextButton(onClick = { onOpenChapter(state.novelID, it.id) }) {
                    Text("Next chapter", color = palette.text)
                }
            }
        }
        // Palette presets, one small swatch per theme.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            ReaderPalettes.presets.forEach { preset ->
                val selected = state.settings.paletteID == preset.id
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(32.dp)
                        .background(preset.background)
                        .selectableBorder(selected)
                        .clickable { viewmodel.changePalette(preset.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("A", color = preset.text, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Renders a parsed table block (HTML) using the widths, padding and colours
 * declared by the source markup where possible.
 **/
@Composable
private fun TableBlock(
    table: Element,
    palette: ReaderPalette,
    fontSizePx: Int,
    lineHeight: Float,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = if (maxWidth == Dp.Infinity) {
            LocalConfiguration.current.screenWidthDp.dp
        } else {
            maxWidth
        }
        TableLayout(
            table = table,
            palette = palette,
            fontSizePx = fontSizePx,
            lineHeight = lineHeight,
            availableWidth = availableWidth,
            inheritedTextColor = null,
            scrollable = true,
        )
    }
}

/**
 * Lays out one table. [availableWidth] is the width of the enclosing cell.
 **/
@Composable
private fun TableLayout(
    table: Element,
    palette: ReaderPalette,
    fontSizePx: Int,
    lineHeight: Float,
    availableWidth: Dp,
    inheritedTextColor: Color?,
    scrollable: Boolean,
) {
    val rows = tableRows(table)
    if (rows.isEmpty()) return
    val width = tableWidth(table, availableWidth, fontSizePx)
    val tableBackground = backgroundColor(table)
    val tableTextColor = explicitTextColor(table) ?: inheritedTextColor
    val defaultPadding = tableCellPadding(table)
    val scrollState = rememberScrollState()
    val containerModifier = if (scrollable) {
        Modifier.fillMaxWidth().horizontalScroll(scrollState)
    } else {
        Modifier.fillMaxWidth()
    }
    Box(
        modifier = containerModifier,
        contentAlignment = if (width <= availableWidth) Alignment.TopCenter else Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier
                .width(width)
                .background(tableBackground ?: Color.Transparent),
        ) {
            rows.forEach { row ->
                TableRow(
                    row = row,
                    tableWidth = width,
                    tableBackground = tableBackground,
                    palette = palette,
                    fontSizePx = fontSizePx,
                    lineHeight = lineHeight,
                    inheritedTextColor = tableTextColor,
                    defaultPadding = defaultPadding,
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    row: Element,
    tableWidth: Dp,
    tableBackground: Color?,
    palette: ReaderPalette,
    fontSizePx: Int,
    lineHeight: Float,
    inheritedTextColor: Color?,
    defaultPadding: Dp,
) {
    val cells = row.children().filter {
        it.tagName().equals("td", ignoreCase = true) ||
                it.tagName().equals("th", ignoreCase = true)
    }
    if (cells.isEmpty()) return
    val rowBackground = backgroundColor(row)
    val rowTextColor = explicitTextColor(row) ?: inheritedTextColor
    val widths = cellWidths(cells, tableWidth)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(rowBackground ?: Color.Transparent),
        verticalAlignment = Alignment.Top,
    ) {
        cells.forEachIndexed { index, cell ->
            val cellBackground = backgroundColor(cell)
            val padding = cellPadding(cell, defaultPadding, fontSizePx)
            val effectiveBackground = compositeBackground(
                compositeBackground(tableBackground, rowBackground),
                cellBackground,
            )
            val contentColor = explicitTextColor(cell)
                ?: rowTextColor
                ?: defaultTableTextColor(effectiveBackground, palette)
            val cellWidth = widths[index]
            val contentWidth = (cellWidth - padding.horizontal).coerceAtLeast(1.dp)
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxHeight()
                    .background(cellBackground ?: Color.Transparent)
                    .border(width = 1.dp, color = palette.accent.copy(alpha = 0.5f))
                    .padding(padding.values),
            ) {
                RenderHtmlContent(
                    html = cell.html(),
                    palette = palette,
                    fontSizePx = fontSizePx,
                    lineHeight = lineHeight,
                    availableWidth = contentWidth,
                    textColor = contentColor,
                )
            }
        }
    }
}

/**
 * Direct `tr` children.
 **/
private fun tableRows(table: Element): List<Element> {
    val rows = mutableListOf<Element>()
    table.children().forEach { child ->
        when (child.tagName().lowercase()) {
            "tr" -> rows.add(child)
            "thead", "tbody", "tfoot" ->
                rows.addAll(child.children().filter { it.tagName().equals("tr", ignoreCase = true) })
        }
    }
    return rows
}

/**
 * Royal Road's chapter stylesheet renders every table at 90% of its container
 * and overrides the `width` attribute.
 **/
private fun tableWidth(table: Element, available: Dp, fontSizePx: Int): Dp {
    cssValue(table.attr("style"), "width")?.let { value ->
        parseCssWidth(value, available, fontSizePx)?.let { return it }
    }
    return available * 0.9f
}

private fun parseCssWidth(raw: String, available: Dp, fontSizePx: Int): Dp? {
    val trimmed = raw.trim()
    if (trimmed.endsWith("%", ignoreCase = true)) {
        val percent = trimmed.dropLast(1).trim().toFloatOrNull() ?: return null
        return available * (percent.coerceIn(1f, 100f) / 100f)
    }
    return parseCssLength(trimmed, fontSizePx)
}

/**
 * Splits the available width between a row's cells using their `width`
 * attributes.
 **/
private fun cellWidths(cells: List<Element>, tableWidth: Dp): List<Dp> {
    val available = tableWidth.value
    val specified = cells.map { cellWidth(it.attr("width"), available) }
    val unknown = specified.count { it == null }
    if (unknown == 0) {
        val total = specified.filterNotNull().sumOf { it.value.toDouble() }.toFloat()
        val scale = if (total > 0f) available / total else 0f
        return specified.map { (it ?: 0.dp) * scale }
    }
    val known = specified.filterNotNull().sumOf { it.value.toDouble() }.toFloat()
    if (known >= available) {
        val scale = if (known > 0f) available / known else 0f
        return specified.map { (it ?: 0.dp) * scale }
    }
    val share = (available - known) / unknown
    return specified.map { it ?: share.dp }
}

private fun cellWidth(widthAttr: String, available: Float): Dp? {
    val trimmed = widthAttr.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.endsWith("%", ignoreCase = true)) {
        val percent = trimmed.dropLast(1).trim().toFloatOrNull() ?: return null
        if (percent <= 0f) return null
        return (available * (percent / 100f)).dp
    }
    val pixels = trimmed.toFloatOrNull() ?: return null
    return if (pixels > 0f) pixels.dp else null
}

private fun tableCellPadding(table: Element): Dp {
    val pixels = table.attr("cellpadding").trim().toFloatOrNull()
    return if (pixels != null && pixels >= 0f) pixels.dp else 4.dp
}

private class CellPadding(val values: PaddingValues, val horizontal: Dp)

private fun cellPadding(cell: Element, default: Dp, fontSizePx: Int): CellPadding {
    val style = cell.attr("style")
    val sides = listOf("padding-top", "padding-right", "padding-bottom", "padding-left")
        .map { cssValue(style, it)?.let { value -> parseCssLength(value, fontSizePx) } }
    val shorthand = cssValue(style, "padding")
        ?.split(Regex("\\s+"))
        ?.mapNotNull { parseCssLength(it, fontSizePx) }
        .orEmpty()
    val expanded = expandPadding(shorthand, default)
    val top = sides[0] ?: expanded[0]
    val right = sides[1] ?: expanded[1]
    val bottom = sides[2] ?: expanded[2]
    val left = sides[3] ?: expanded[3]
    return CellPadding(
        values = PaddingValues(start = left, top = top, end = right, bottom = bottom),
        horizontal = left + right,
    )
}

private fun expandPadding(parts: List<Dp>, default: Dp): List<Dp> = when (parts.size) {
    1 -> List(4) { parts[0] }
    2 -> listOf(parts[0], parts[1], parts[0], parts[1])
    3 -> listOf(parts[0], parts[1], parts[2], parts[1])
    4 -> parts
    else -> List(4) { default }
}

private val cssLengthRegex =
    Regex("""^([0-9]*\.?[0-9]+)\s*(px|pt|pc|in|cm|mm|em|rem|%)?$""", RegexOption.IGNORE_CASE)

private fun parseCssLength(raw: String, fontSizePx: Int): Dp? {
    val match = cssLengthRegex.find(raw.trim()) ?: return null
    val value = match.groupValues[1].toFloatOrNull() ?: return null
    return when (match.groupValues[2].lowercase()) {
        "", "px" -> value.dp
        "pt" -> (value * 4f / 3f).dp
        "pc" -> (value * 16f).dp
        "in" -> (value * 96f).dp
        "cm" -> (value * 96f / 2.54f).dp
        "mm" -> (value * 96f / 25.4f).dp
        "em", "rem" -> (value * fontSizePx).dp
        else -> null
    }
}

private fun cssValue(style: String, property: String): String? {
    val match = Regex(
        "(?:^|;)\\s*${Regex.escape(property)}\\s*:\\s*([^;]+)",
        RegexOption.IGNORE_CASE,
    ).find(style) ?: return null
    return match.groupValues[1].trim()
}

private fun backgroundColor(element: Element): Color? =
    listOfNotNull(
        cssValue(element.attr("style"), "background-color"),
        cssValue(element.attr("style"), "background"),
        element.attr("bgcolor").takeIf { it.isNotBlank() },
    ).firstNotNullOfOrNull { parseColor(it) }

private fun explicitTextColor(element: Element): Color? =
    listOfNotNull(
        cssValue(element.attr("style"), "color"),
        element.attr("color").takeIf { it.isNotBlank() },
    ).firstNotNullOfOrNull { parseColor(it) }

// Parse rgb()/rgba() from html.
private fun parseColor(raw: String): Color? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    runCatching { Color(value.toColorInt()) }.getOrNull()?.let { return it }
    val match = Regex(
        """rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})(?:\s*,\s*([0-9.]+))?\s*\)""",
        RegexOption.IGNORE_CASE,
    ).find(value) ?: return null
    val r = match.groupValues[1].toInt().coerceIn(0, 255)
    val g = match.groupValues[2].toInt().coerceIn(0, 255)
    val b = match.groupValues[3].toInt().coerceIn(0, 255)
    val a = match.groupValues[4].toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
    return Color(r / 255f, g / 255f, b / 255f, a)
}

/**
 *  Composites [top] over [bottom] for contrast calculations.
 **/
private fun compositeBackground(bottom: Color?, top: Color?): Color? {
    if (top == null || top.alpha <= 0f) return bottom
    if (bottom == null || bottom.alpha <= 0f) return top
    val alpha = top.alpha + bottom.alpha * (1f - top.alpha)
    if (alpha <= 0f) return null
    return Color(
        red = (top.red * top.alpha + bottom.red * bottom.alpha * (1f - top.alpha)) / alpha,
        green = (top.green * top.alpha + bottom.green * bottom.alpha * (1f - top.alpha)) / alpha,
        blue = (top.blue * top.alpha + bottom.blue * bottom.alpha * (1f - top.alpha)) / alpha,
        alpha = alpha,
    )
}

/**
 * Table cells default color.
 **/
private fun defaultTableTextColor(background: Color?, palette: ReaderPalette): Color {
    if (background == null || background.alpha < 0.05f) return palette.text
    return if (background.luminance() < 0.5f) Color(0xFFCCCCCC) else Color(0xFF222222)
}

private fun elementTextAlign(element: Element): TextAlign? {
    val value = cssValue(element.attr("style"), "text-align")
        ?: element.attr("align").takeIf { it.isNotBlank() }
        ?: return null
    return when (value.trim().lowercase()) {
        "center" -> TextAlign.Center
        "right", "end" -> TextAlign.End
        "left", "start" -> TextAlign.Start
        "justify" -> TextAlign.Justify
        else -> null
    }
}

@Composable
private fun RenderHtmlContent(
    html: String,
    palette: ReaderPalette,
    fontSizePx: Int,
    lineHeight: Float,
    availableWidth: Dp,
    textColor: Color,
    textAlign: TextAlign? = null,
) {
    val fragment = remember(html) { Jsoup.parseBodyFragment(html) }
    val body = fragment.body()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (body.children().isEmpty()) {
            val text = body.text()
            if (text.isNotBlank()) {
                Text(
                    text = AnnotatedString.fromHtml(html),
                    color = textColor,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSizePx.sp,
                        lineHeight = (fontSizePx * lineHeight).sp,
                    ),
                )
            }
            return@Column
        }
        body.children().forEach { node ->
            when (node.tagName().lowercase()) {
                "table" -> TableLayout(
                    table = node,
                    palette = palette,
                    fontSizePx = fontSizePx,
                    lineHeight = lineHeight,
                    availableWidth = availableWidth,
                    inheritedTextColor = textColor,
                    scrollable = false,
                )

                "p" -> if (node.text().isNotBlank()) {
                    Text(
                        text = AnnotatedString.fromHtml(node.html()),
                        color = explicitTextColor(node) ?: textColor,
                        textAlign = elementTextAlign(node) ?: textAlign,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSizePx.sp,
                            lineHeight = (fontSizePx * lineHeight).sp,
                        ),
                    )
                }

                "center" -> RenderHtmlContent(
                    html = node.html(),
                    palette = palette,
                    fontSizePx = fontSizePx,
                    lineHeight = lineHeight,
                    availableWidth = availableWidth,
                    textColor = explicitTextColor(node) ?: textColor,
                    textAlign = TextAlign.Center,
                )

                "div", "span" -> RenderHtmlContent(
                    html = node.html(),
                    palette = palette,
                    fontSizePx = fontSizePx,
                    lineHeight = lineHeight,
                    availableWidth = availableWidth,
                    textColor = explicitTextColor(node) ?: textColor,
                    textAlign = elementTextAlign(node) ?: textAlign,
                )

                else -> {
                    if (node.text().isNotBlank()) {
                        if (node.select("table").isNotEmpty()) {
                            RenderHtmlContent(
                                html = node.html(),
                                palette = palette,
                                fontSizePx = fontSizePx,
                                lineHeight = lineHeight,
                                availableWidth = availableWidth,
                                textColor = explicitTextColor(node) ?: textColor,
                                textAlign = elementTextAlign(node) ?: textAlign,
                            )
                        } else {
                            Text(
                                text = AnnotatedString.fromHtml(node.outerHtml()),
                                color = explicitTextColor(node) ?: textColor,
                                textAlign = elementTextAlign(node) ?: textAlign,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = fontSizePx.sp,
                                    lineHeight = (fontSizePx * lineHeight).sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parses a table block.
 **/
private fun parseTableBlock(html: String): Element? {
    val trimmed = html.trimStart()
    return if (trimmed.startsWith("<table", ignoreCase = true)) {
        Jsoup.parseBodyFragment(trimmed).selectFirst("table")
    } else {
        Jsoup.parse("<table>$html</table>").selectFirst("table")
    }
}

private fun isTableParagraph(html: String): Boolean =
    html.contains("<tr", ignoreCase = true) || html.contains("<table", ignoreCase = true) || html.contains(
        "<td",
        ignoreCase = true
    )

/**
 * Visible outline for the currently selected palette swatch.
 **/
@Composable
private fun Modifier.selectableBorder(selected: Boolean): Modifier =
    if (selected) {
        this.then(
            Modifier.border(width = 2.dp, color = Color.White, shape = MaterialTheme.shapes.small)
        )
    } else {
        this
    }
