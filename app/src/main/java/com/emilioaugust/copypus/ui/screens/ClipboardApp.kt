package com.emilioaugust.copypus.ui.screens

import com.emilioaugust.copypus.R
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.rememberAsyncImagePainter
import com.emilioaugust.copypus.ClipboardData
import com.emilioaugust.copypus.data.viewmodel.MainViewModel
import com.emilioaugust.copypus.data.entity.ClipboardItem
import com.emilioaugust.copypus.ClipboardManagerHelper
import com.emilioaugust.copypus.ui.EditSheet
import com.emilioaugust.copypus.ui.ManualEntrySheet
import com.emilioaugust.copypus.ui.MultiPasteSheet
import com.emilioaugust.copypus.utils.ClipboardType
import com.emilioaugust.copypus.utils.detectClipboardType
import com.emilioaugust.copypus.utils.formatSectionTitle
import com.emilioaugust.copypus.utils.formatTime
import com.emilioaugust.copypus.utils.shareImage
import com.emilioaugust.copypus.utils.shareText
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val clipboardItems by viewModel.items.collectAsState()
    val filteredItems = clipboardItems.filter { item ->
        item.type == "IMAGE" ||
                item.text?.contains(
                    searchQuery,
                    ignoreCase = true
                ) == true
    }
    val clipboardHelper = remember { ClipboardManagerHelper(context.applicationContext) }
    val groupedItems = filteredItems.groupBy { formatSectionTitle(it.timestamp, context) }
    var editingItem by remember { mutableStateOf<ClipboardItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var currentSheet by remember { mutableStateOf(AddSheet.NONE) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        clipboardHelper.startListening { data ->
            when (data) {
                is ClipboardData.Text -> {
                    viewModel.saveText(data.text)
                }

                is ClipboardData.Image -> {}
            }
        }

        onDispose {
            clipboardHelper.stopListening()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.padding(0.dp)) {
            data -> CustomSnackBar(data)
        }},
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.topbar_copypus), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold) },
                actions = {
                    ClipboardTopBarMenu(onClearAll = {
                        viewModel.clearAll()
                        Toast.makeText(
                            context,
                            context.getString(R.string.it_s_empty_text),
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                },
                navigationIcon = {
                    ClipboardAddMenu(
                        onManualEntry = { currentSheet = AddSheet.MANUAL },
                        onMultiPaste = { currentSheet = AddSheet.MULTI }
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (clipboardItems.isEmpty()) {
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp
                        ),
                    placeholder = {
                        Text(stringResource(R.string.search_title))
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
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
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
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
                                            context.getString(R.string.added_to_favorites_text),
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
                                    onLongClick = {
                                        if (item.type == "TEXT") {
                                            editingItem = item
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { item ->

        if (item.type == "TEXT" && item.text != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    editingItem = null
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                EditSheet(
                    initialText = item.text,
                    onDismiss = {
                        editingItem = null
                    },
                    onSave = { newText ->
                        viewModel.updateItem(
                            item.copy(text = newText)
                        )
                        editingItem = null
                    }
                )
            }
        }
    }

    when(currentSheet) {
        AddSheet.MANUAL -> {
            ModalBottomSheet(
                onDismissRequest = {
                    currentSheet = AddSheet.NONE
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                ManualEntrySheet(
                    onDismiss = { currentSheet = AddSheet.NONE },
                    onSave = { text ->
                        viewModel.saveText(text)
                        currentSheet = AddSheet.NONE
                    }
                )
            }
        }

        AddSheet.MULTI -> {
            ModalBottomSheet(
                onDismissRequest = {
                    currentSheet = AddSheet.NONE
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                MultiPasteSheet(
                    onDismiss = { currentSheet = AddSheet.NONE },
                    onSaveAll = { texts ->
                        texts.forEach {
                            viewModel.saveText(it)
                        }
                    }
                )
            }
        }

        AddSheet.NONE -> Unit
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardItemCard(item: ClipboardItem, onCopy: () -> Unit, onFavorite: () -> Unit,
                      onDelete: () -> Unit, onLongClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when(value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onFavorite()
                    false
                }
                else -> false
            }
        }
    )
    val type = if (item.type == "IMAGE") {
        ClipboardType.IMAGE
    } else {
        detectClipboardType(item.text ?: "")
    }

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
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Star
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> Icons.Default.Delete
            }
            val alignment = when(direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
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
                if (item.type == "IMAGE" && item.imagePath != null) {
                    Image(
                        painter = rememberAsyncImagePainter(item.imagePath),
                        contentDescription = item.text,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                showImageViewer = true
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
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
                            imageVector = when (type) {
                                ClipboardType.LINK ->
                                    Icons.Default.Link

                                ClipboardType.CODE ->
                                    Icons.Default.Code

                                ClipboardType.TEXT ->
                                    Icons.Default.TextFields

                                ClipboardType.IMAGE ->
                                    Icons.Default.Image
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(24.dp)
                        )
                    }
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
                    if (item.type == "TEXT") {
                        SelectionContainer {
                            item.text?.let {
                                Text(
                                    text = it,
                                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                                    overflow = if (expanded)
                                        TextOverflow.Visible
                                    else
                                        TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    } else if (item.type == "IMAGE") {
                        item.text?.let {
                            Text(
                                text = it,
                                maxLines = if (expanded) Int.MAX_VALUE else 2,
                                overflow = if (expanded)
                                    TextOverflow.Visible
                                else
                                    TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = onLongClick
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatTime(item.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
                Spacer(Modifier.width(10.dp))

                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(24.dp)
                        .clickable { onCopy() }
                )
                Spacer(Modifier.width(14.dp))
                Icon(
                    imageVector =
                        if (expanded)
                            Icons.Default.ExpandLess
                        else
                            Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 12.dp)
                        .clickable { expanded = !expanded }
                )
            }
        }
    }

    if (showImageViewer && item.imagePath != null) {
        ImageViewerDialog(
            imagePath = item.imagePath,
            onDismiss = {
                showImageViewer = false
            }
        )
    }
}

@Composable
fun ImageViewerDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var swipeOffsetY by remember { mutableFloatStateOf(0f) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val minScale = 1f
    val maxScale = 5f
    val doubleTapScale = 2.5f
    val panSpeedFactor = 1.8f

    val animatedScale = remember { Animatable(1f) }
    val animatedOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    LaunchedEffect(animatedScale.value, animatedOffset.value) {
        scale = animatedScale.value
        offset = animatedOffset.value
    }

    fun getMaxOffset(currentScale: Float): Offset {
        if (containerSize == IntSize.Zero || imageSize == IntSize.Zero) {
            return Offset.Zero
        }

        val fitScale = minOf(
            containerSize.width.toFloat() / imageSize.width,
            containerSize.height.toFloat() / imageSize.height
        )

        val displayedWidth = imageSize.width * fitScale * currentScale
        val displayedHeight = imageSize.height * fitScale * currentScale

        val maxX = ((displayedWidth - containerSize.width) / 2f).coerceAtLeast(0f)
        val maxY = ((displayedHeight - containerSize.height) / 2f).coerceAtLeast(0f)

        return Offset(maxX, maxY)
    }

    fun clampOffset(candidate: Offset, currentScale: Float): Offset {
        val max = getMaxOffset(currentScale)
        return Offset(
            x = candidate.x.coerceIn(-max.x, max.x),
            y = candidate.y.coerceIn(-max.y, max.y)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { containerSize = it }
                // ЗУМ + ПАНОРАМИРОВАНИЕ (мультитач)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                        val speed = 1f + (scale - minScale) *
                                (panSpeedFactor - 1f) / (maxScale - minScale)
                        val acceleratedPan = pan * speed

                        val candidate = Offset(
                            x = offset.x + acceleratedPan.x / newScale +
                                    (newScale - scale) *
                                    (centroid.x - size.width / 2f - offset.x) / newScale,
                            y = offset.y + acceleratedPan.y / newScale +
                                    (newScale - scale) *
                                    (centroid.y - size.height / 2f - offset.y) / newScale
                        )

                        scale = newScale
                        offset = if (newScale <= minScale + 0.01f) {
                            Offset.Zero
                        } else {
                            clampOffset(candidate, newScale)
                        }
                    }
                }

                .pointerInput(containerSize, imageSize) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            scope.launch {
                                if (scale > minScale + 0.01f) {
                                    launch { animatedScale.animateTo(minScale, tween(250)) }
                                    launch { animatedOffset.animateTo(Offset.Zero, tween(250)) }
                                } else {
                                    val targetScale = doubleTapScale
                                    val rawOffset = Offset(
                                        x = (size.width / 2f - tapOffset.x) *
                                                (targetScale / minScale - 1f),
                                        y = (size.height / 2f - tapOffset.y) *
                                                (targetScale / minScale - 1f)
                                    )
                                    // Ограничиваем и целевое смещение
                                    val targetOffset = clampOffset(rawOffset, targetScale)
                                    launch { animatedScale.animateTo(targetScale, tween(250)) }
                                    launch { animatedOffset.animateTo(targetOffset, tween(250)) }
                                }
                            }
                        }
                    )
                }
                // СВАЙП ВНИЗ ДЛЯ ЗАКРЫТИЯ (только при 1x)
                .pointerInput(scale) {
                    if (scale <= minScale + 0.01f) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                swipeOffsetY = (swipeOffsetY + dragAmount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (swipeOffsetY > 200f) {
                                    onDismiss()
                                } else {
                                    swipeOffsetY = 0f
                                }
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = imagePath,
                    onSuccess = { state ->
                        // Запоминаем реальный размер исходного изображения
                        val size = state.result.image.width to state.result.image.height
                        imageSize = IntSize(size.first, size.second)
                    }
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y + swipeOffsetY
                        alpha = (1f - (swipeOffsetY / 600f)).coerceIn(0.3f, 1f)
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun ClipboardTopBarMenu(onClearAll: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreHoriz, contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.small,
            shadowElevation = 8.dp,
            tonalElevation = 8.dp) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.clear_all_btn)) },
                onClick = {
                    onClearAll()
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
            )
        }
    }
}

@Composable
fun ClipboardAddMenu(onManualEntry: () -> Unit, onMultiPaste: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.Add,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.small,
            shadowElevation = 8.dp,
            tonalElevation = 8.dp
        ) {

            DropdownMenuItem(
                text = { Text(stringResource(R.string.manual_entry_title)) },
                leadingIcon = { Icon(Icons.Default.Edit, null,
                    tint = MaterialTheme.colorScheme.onTertiary) },
                onClick = {
                    expanded = false
                    onManualEntry()
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.multi_paste_title)) },
                leadingIcon = { Icon(Icons.Default.ContentPaste, null,
                    tint = MaterialTheme.colorScheme.onTertiary) },
                onClick = {
                    expanded = false
                    onMultiPaste()
                }
            )

        }
    }
}

@Composable
fun CustomSnackBar(data: SnackbarData) {
    Snackbar(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = data.visuals.message, modifier = Modifier.weight(1f))
            TextButton(onClick = { data.performAction() }) {
                Text(stringResource(R.string.undo_text))
            }
        }
    }
}
