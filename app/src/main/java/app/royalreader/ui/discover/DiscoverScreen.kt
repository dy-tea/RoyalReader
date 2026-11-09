package app.royalreader.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.royalreader.domain.model.Novel
import app.royalreader.ui.discover.DiscoverUiState.Section
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onBookClick: (novelID: Long) -> Unit,
    viewmodel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewmodel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Discover") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewmodel::setQuery,
                placeholder = { Text("Search RoyalRoad") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            TabRow(selectedTabIndex = state.section.ordinal) {
                Tab(
                    selected = state.section == Section.POPULAR,
                    onClick = { viewmodel.setSection(Section.POPULAR) },
                    text = { Text("Popular") },
                )
                Tab(
                    selected = state.section == Section.LATEST,
                    onClick = { viewmodel.setSection(Section.LATEST) },
                    text = { Text("Latest") },
                )
            }
            when {
                state.loading && state.books.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 48.dp).align(Alignment.CenterHorizontally),
                    )
                }
                state.books.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Nothing here yet.", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = viewmodel::refresh) { Text("Retry") }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.books, key = { "${it.id}-${it.inLibrary}" }) { novel ->
                            NovelRow(
                                novel = novel,
                                onClick = { onBookClick(novel.id) },
                                onToggle = { viewmodel.toggleLibrary(novel) },
                                coverModel = novel.coverURL,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelRow(
    novel: Novel,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    coverModel: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = coverModel,
            contentDescription = novel.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 56.dp, height = 78.dp)
                .clip(MaterialTheme.shapes.small),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = novel.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row {
                Text(
                    text = novel.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                // Rating shown when available.
                novel.rating?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%.1f".format(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(onClick = onToggle) {
            Text(if (novel.inLibrary) "Remove" else "Add")
        }
    }
}
