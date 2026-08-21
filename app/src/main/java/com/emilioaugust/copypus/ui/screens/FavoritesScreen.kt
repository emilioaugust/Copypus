package com.emilioaugust.copypus.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.emilioaugust.copypus.R
import com.emilioaugust.copypus.ClipboardManagerHelper
import com.emilioaugust.copypus.data.MainViewModel
import com.emilioaugust.copypus.utils.formatSectionTitle
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val favoriteItems by viewModel.favoriteItems.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredItems = favoriteItems.filter { item ->
        val textMatch = item.text.contains(searchQuery, ignoreCase = true)
        textMatch
    }
    val groupedItems = filteredItems.groupBy { formatSectionTitle(it.timestamp, context) }
    val clipboardHelper = remember { ClipboardManagerHelper(context.applicationContext) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.padding(0.dp)) {
                data -> CustomSnackBar(data)
        }},
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.topbar_favorites),
                    color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (favoriteItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_favorites),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                        end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = 0.dp
                    )
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                        ),
                    placeholder = {
                        Text(stringResource(R.string.search_title))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
                        cursorColor = MaterialTheme.colorScheme.tertiary,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedPlaceholderColor = Color.Gray
                    ),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    },
                    shape = MaterialTheme.shapes.medium
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedItems.forEach { (sectionTitle, sectionItems) ->
                        item {
                            Text(
                                text = sectionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(
                                    vertical = 8.dp
                                )
                            )
                        }

                        items(
                            items = sectionItems,
                            key = { it.id }
                        ) { item ->
                            ClipboardItemCard(
                                item = item,
                                onCopy = {
                                    clipboardHelper.copyTextToClipboard(
                                        item.text
                                    )
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.text_copied_text),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFavorite = {
                                    viewModel.toggleFavorite(item)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.deleted_from_favorites_text),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                             },
                                onDelete = {
                                    viewModel.deleteItem(item)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.clipboard_deleted_text),
                                            actionLabel = context.getString(R.string.undo_text),
                                            duration = SnackbarDuration.Short
                                        )

                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreItem(item)
                                        }
                                    }
                                },
                                onLongClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}