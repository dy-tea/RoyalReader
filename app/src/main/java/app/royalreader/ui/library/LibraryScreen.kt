package app.royalreader.ui.library

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.royalreader.domain.model.LibraryEntry
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (novelID: Long) -> Unit,
    onDiscoverClick: () -> Unit,
    onReadChapter: (novelID: Long, chapterID: Long) -> Unit,
    viewmodel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewmodel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Library") }) },
    ) { padding ->
        if (state.books.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Your shelf is empty.",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Find stories worth following on Discover.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDiscoverClick) {
                    Text("Browse stories")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.books, key = { it.novel.id }) { entry ->
                    NovelCard(
                        entry = entry,
                        onReadClick = {
                            viewmodel.continueReading(
                                entry,
                                onReady = onReadChapter,
                                onFallback = { onBookClick(entry.novel.id) },
                            )
                        },
                        onDetailsClick = { onBookClick(entry.novel.id) },
                        onLongClick = { viewmodel.removeFromLibrary(entry.novel.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NovelCard(
    entry: LibraryEntry,
    onReadClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val novel = entry.novel
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onDetailsClick, onLongClick = onLongClick),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = novel.coverURL,
                contentDescription = novel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = novel.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = novel.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onReadClick,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = if (entry.resumeChapterID != null) "Continue" else "Start",
                        maxLines = 1,
                    )
                }
                TextButton(
                    onClick = onDetailsClick,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(text = "Details", maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Long-press to remove",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
