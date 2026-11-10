package app.royalreader.ui.reader

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
                ChapterParagraphs(state, palette, viewmodel, onOpenChapter)
            }
        }

        if (state.chapter != null) {
            ReaderControls(
                state = state,
                palette = palette,
                viewmodel = viewmodel,
                onBackClick = onBackClick,
                onOpenChapter = onOpenChapter,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ChapterParagraphs(
    state: ReaderUiState,
    palette: ReaderPalette,
    viewmodel: ReaderViewModel,
    onOpenChapter: (novelID: Long, chapterID: Long) -> Unit,
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp, end = 24.dp, top = 48.dp, bottom = 96.dp,
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
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
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
