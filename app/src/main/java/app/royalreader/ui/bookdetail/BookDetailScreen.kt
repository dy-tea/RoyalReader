package app.royalreader.ui.bookdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.royalreader.domain.model.Chapter
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBackClick: () -> Unit,
    onReadChapter: (novelID: Long, chapterID: Long) -> Unit,
    onDiscoverClick: () -> Unit,
    viewmodel: BookDetailViewModel = hiltViewModel(),
) {
    val uiState by viewmodel.uiState.collectAsState()
    val novel by viewmodel.novelState.collectAsState()
    val chapters by viewmodel.chapterState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(novel?.title ?: "Book", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.loading -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            novel == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("This book is not cached.", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onDiscoverClick) { Text("Browse stories") }
                }
            }
            else -> {
                val book = novel!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { BookHeader(book = book, viewmodel = viewmodel) }
                    if (chapters.isNotEmpty()) {
                        item {
                            Text(
                                text = "Chapters (${chapters.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(chapters, key = { it.id }) { chapter ->
                            ChapterRow(chapter, onClick = {
                                onReadChapter(viewmodel.novelID, chapter.id)
                            })
                        }
                    } else {
                        item {
                            Text(
                                text = "No chapters loaded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookHeader(book: app.royalreader.domain.model.Novel, viewmodel: BookDetailViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = book.coverURL,
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 96.dp, height = 140.dp)
                .clip(MaterialTheme.shapes.small),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = book.author.ifBlank { "Unknown author" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            book.rating?.let {
                Text(
                    text = "%.2f  by readers".format(it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (book.inLibrary) {
                FilledTonalButton(onClick = viewmodel::toggleLibrary) {
                    Text("Remove from library")
                }
            } else {
                Button(onClick = viewmodel::toggleLibrary) {
                    Text("Add to library")
                }
            }
        }
    }
    if (book.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(book.description, style = MaterialTheme.typography.bodyMedium)
    }
    if (book.tags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = book.tags.joinToString(" • "), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(chapter.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
        if (chapter.read) {
            Text(
                text = "read",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (chapter.downloaded) {
            Text(
                text = "downloaded",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
