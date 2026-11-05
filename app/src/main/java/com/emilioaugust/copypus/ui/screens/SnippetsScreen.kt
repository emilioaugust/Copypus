package com.emilioaugust.copypus.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emilioaugust.copypus.R
import com.emilioaugust.copypus.data.entity.SnippetEntity
import com.emilioaugust.copypus.data.repository.SnippetRepository
import com.emilioaugust.copypus.data.viewmodel.SnippetViewModel
import com.emilioaugust.copypus.utils.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsScreen(snippetViewModel: SnippetViewModel, onBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snippetsItems by snippetViewModel.items.collectAsState()
    var showAddSnippetViewer by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<SnippetEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.padding(0.dp)) {
                data -> CustomSnackBar(data)
        }},
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.snippets), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) },
                actions = {
                    ClipboardTopBarMenu(onClearAll = {
                            snippetViewModel.clearAll()
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddSnippetViewer = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.offset(y = (-16).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { paddingValues ->
        if (snippetsItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_history),
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
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = snippetsItems,
                        key = { it.id }
                    ) {item ->
                        SnippetItemCard(
                            item = item,
                            onDelete = {
                                snippetViewModel.deleteSnippet(item)
                            },
                            onLongClick = {
                                editingItem = item
                            }
                        )
                    }
                }
            }
        }

        if (showAddSnippetViewer) {
            val saveError by snippetViewModel.saveError.collectAsStateWithLifecycle()
            ModalBottomSheet(
                onDismissRequest = {
                    showAddSnippetViewer = false
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                AddSnippetSheet(
                    onDismiss = {
                        snippetViewModel.clearError()
                        showAddSnippetViewer = false
                    },
                    onSave = { key, value ->
                        snippetViewModel.createSnippet(key, value) {
                            showAddSnippetViewer = false
                        }
                    },
                    externalError = saveError
                )
            }
        }

        editingItem?.let { item ->
            ModalBottomSheet(
                onDismissRequest = { editingItem = null },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                val saveError by snippetViewModel.saveError.collectAsStateWithLifecycle()

                EditSnippetSheet(
                    initialKey = item.key,
                    initialValue = item.value,
                    onDismiss = {
                        snippetViewModel.clearError()
                        editingItem = null
                    },
                    onSave = { newKey, newValue ->
                        snippetViewModel.updateSnippet(
                            item = item,
                            newKey = newKey,
                            newValue = newValue,
                            onSuccess = { editingItem = null }
                        )
                    },
                    externalError = saveError
                )
            }
        }
    }
}

@Composable
fun SnippetItemCard(item: SnippetEntity, onDelete: () -> Unit, onLongClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when(value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when(direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFFFFD54F)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFE57373)
                    else -> MaterialTheme.colorScheme.surface
                },
                label = ""
            )

            val icon = when(direction) {
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> Icons.Default.Delete
            }
            val alignment = when(direction) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
                .clip(MaterialTheme.shapes.medium),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onLongClick
                        )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DataObject,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(24.dp)
                    )
                }


                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Column(modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = item.key,
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SelectionContainer {
                        item.value?.let {
                            Text(
                                text = it,
                                maxLines = if (expanded) Int.MAX_VALUE else 1,
                                overflow = if (expanded)
                                    TextOverflow.Visible
                                else
                                    TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatTime(item.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }

                Spacer(Modifier.width(14.dp))
                Icon(
                    imageVector =
                        if (expanded)
                            Icons.Default.ExpandLess
                        else
                            Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { expanded = !expanded }
                )
            }
        }
    }
}

@Composable
fun AddSnippetSheet(
    onDismiss: () -> Unit,
    onSave: (key: String, value: String) -> Unit,
    externalError: String? = null
) {
    var key by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val normalizedKey = remember(key) { key.trim().removePrefix(";;").lowercase() }
    val keyValid = normalizedKey.matches(SnippetRepository.KEY_REGEX)
    val canSave = keyValid && value.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.add_new_snippet),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = key,
            onValueChange = {
                key = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.key_without)) },
            placeholder = { Text(stringResource(R.string.addr)) },
            prefix = { Text(";;") },
            isError = key.isNotBlank() && !keyValid,
            supportingText = when {
                key.isBlank() -> null
                !keyValid -> {
                    { Text(stringResource(R.string.invalid_key)) }
                }
                else -> {
                    { Text(stringResource(R.string.write_in_text_field_normal)) }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.tertiary,
                unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
                cursorColor = MaterialTheme.colorScheme.tertiary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedPlaceholderColor = Color.LightGray
            ),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 12,
            label = { Text(stringResource(R.string.replacement_text)) }, // "Текст замены"
            placeholder = {
                Text(stringResource(R.string.paste_text_here_placeholder), color = Color.Gray)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.tertiary,
                unfocusedTextColor = MaterialTheme.colorScheme.tertiary,
                cursorColor = MaterialTheme.colorScheme.tertiary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedPlaceholderColor = Color.Gray
            ),
            shape = MaterialTheme.shapes.medium
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave,
            onClick = {
                onSave(normalizedKey, value.trim())
            }
        ) {
            Icon(Icons.Default.Save, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.save_btn), color = Color.White)
        }
    }
}

@Composable
fun EditSnippetSheet(
    initialKey: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (newKey: String, newValue: String) -> Unit,
    externalError: String? = null
) {
    var key by rememberSaveable(initialKey) { mutableStateOf(initialKey) }
    var value by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    // нормализация как в репозитории
    val normalizedKey = remember(key) { key.trim().removePrefix(";;").lowercase() }
    val keyValid = normalizedKey.matches(SnippetRepository.KEY_REGEX)
    val canSave = keyValid && value.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.edit_title),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(16.dp))

        // ---- КЛЮЧ ----
        OutlinedTextField(
            value = key,
            onValueChange = {
                key = it
                localError = null
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.key_without)) },
            prefix = { Text(";;") },
            isError = key.isNotBlank() && !keyValid,
            supportingText = when {
                key.isBlank() -> null
                !keyValid -> { { Text(stringResource(R.string.invalid_key)) } }
                else -> { { Text(stringResource(R.string.write_in_text_field_normal)) } }
            }
        )

        Spacer(Modifier.height(12.dp))

        // ---- ЗНАЧЕНИЕ ----
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                localError = null
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 12,
            label = { Text(stringResource(R.string.replacement_text)) },
            placeholder = {
                Text(stringResource(R.string.paste_text_here_placeholder), color = Color.Gray)
            }
        )

        val displayError = externalError ?: localError
        displayError?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave,
            onClick = {
                onSave(normalizedKey, value.trim())
            }
        ) {
            Icon(Icons.Default.Save, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.save_btn), color = Color.White)
        }
    }
}
