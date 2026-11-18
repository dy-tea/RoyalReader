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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import app.royalreader.R
import app.royalreader.domain.model.Chapter
import app.royalreader.ui.library.RemoveFromLibraryDialog
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBackClick: () -> Unit,
    onReadChapter: (novelID: Long, chapterID: Long) -> Unit,
    onDiscoverClick: () -> Unit,
    viewmodel: BookDetailViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(
        checkNotNull<androidx.lifecycle.ViewModelStoreOwner>(LocalViewModelStoreOwner.current) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            },
        null
    ),
) {
    val uiState by viewmodel.uiState.collectAsState()
    val novel by viewmodel.novelState.collectAsState()
    val chapters by viewmodel.chapterState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var hasLoadedNovel by remember { mutableStateOf(false) }
    var hasNavigatedBack by remember { mutableStateOf(false) }
    val safeBack: () -> Unit = {
        if (!hasNavigatedBack) {
            hasNavigatedBack = true
            onBackClick()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(novel) {
        if (novel != null) {
            hasLoadedNovel = true
        } else if (hasLoadedNovel && !hasNavigatedBack) {
            hasNavigatedBack = true
            onBackClick()
        }
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
                    item { BookHeader(book = book, viewmodel = viewmodel, onBackClick = safeBack) }
                    if (chapters.isNotEmpty()) {
                        item {
                            Text(
                                text = "Chapters (${chapters.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(chapters, key = { it.id }) { chapter ->
                            ChapterRow(
                                chapter = chapter,
                                onClick = { onReadChapter(viewmodel.novelID, chapter.id) },
                                onDownload = { viewmodel.downloadSingleChapter(chapter.id) },
                                onDelete = { viewmodel.deleteChapter(chapter.id) },
                            )
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
private fun BookHeader(
    book: app.royalreader.domain.model.Novel,
    viewmodel: BookDetailViewModel,
    onBackClick: () -> Unit = {},
) {
    val showRemoveDialog = remember { mutableStateOf(false) }
    val uiState by viewmodel.uiState.collectAsState()
    val chapters by viewmodel.chapterState.collectAsState()
    val allDownloaded = chapters.isNotEmpty() && chapters.all { it.downloaded }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (book.inLibrary) {
                    FilledTonalButton(onClick = { showRemoveDialog.value = true }) {
                        Text("Remove from library")
                    }
                } else {
                    Button(onClick = viewmodel::toggleLibrary) {
                        Text("Add to library")
                    }
                }
                if (!allDownloaded) {
                    Spacer(modifier = Modifier.weight(1f))
                    if (uiState.downloading) {
                        CircularProgressIndicator(
                            progress = { uiState.downloadProgress },
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.downloadedCount}/${uiState.totalDownloads}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    } else {
                        IconButton(onClick = viewmodel::downloadAllChapters) {
                            Icon(
                                painter = painterResource(R.drawable.download_for_offline_24px),
                                "Download all chapters"
                            )
                        }
                    }
                }
            }
            if (showRemoveDialog.value) {
                RemoveFromLibraryDialog(
                    onDismissRequest = { showRemoveDialog.value = false },
                    onConfirmation = {
                        viewmodel.toggleLibrary()
                        showRemoveDialog.value = false
                        onBackClick()
                    },
                    dialogText = "Remove \"${book.title}\" from your library?",
                )
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
private fun ChapterRow(
    chapter: Chapter,
    onClick: () -> Unit,
    onDownload: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Row (modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(chapter.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
            Spacer(modifier = Modifier.weight(1f))
            if (chapter.downloaded) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete downloaded chapter")
                }
            } else {
                IconButton(onClick = onDownload) {
                    Icon(painter = painterResource(R.drawable.download_for_offline_24px), "Download single chapter")
                }
            }
        }
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
