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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.royalreader.core.LocalAutoBrightness
import app.royalreader.core.LocalReducedAnimations
import app.royalreader.core.theme.ReaderPalette
import app.royalreader.core.theme.ReaderPalettes

@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    onOpenChapter: (novelID: Long, chapterID: Long) -> Unit,
    viewmodel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewmodel.uiState.collectAsState()

    // Reading time tracking lives and dies with the reader screen itself.
    DisposableEffect(Unit) {
        viewmodel.trackReadingTime()
        onDispose {
            viewmodel.stopTrackingReadingTime()
        }
    }

    ReaderLayout(state = state, viewmodel = viewmodel, onBackClick = onBackClick,
        onOpenChapter = onOpenChapter)
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
            Text(
                text = state.paragraphs[index],
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = state.settings.fontSizePx.sp,
                    lineHeight = (state.settings.fontSizePx * state.settings.lineHeight).sp,
                ),
                color = palette.text,
            )
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
