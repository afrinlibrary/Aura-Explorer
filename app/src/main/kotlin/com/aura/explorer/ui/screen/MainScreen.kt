package com.aura.explorer.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.explorer.domain.model.*
import com.aura.explorer.ui.component.FileDetailsBottomSheet
import com.aura.explorer.ui.util.fileIconMeta
import com.aura.explorer.ui.viewmodel.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    viewModel              : FileViewModel = hiltViewModel(),
    onRequestAllFilesAccess: () -> Unit,
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }
    val scope         = rememberCoroutineScope()
    val haptic        = androidx.compose.ui.platform.LocalHapticFeedback.current

    var detailItem      by remember { mutableStateOf<FileItem?>(null) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showSortDialog   by remember { mutableStateOf(false) }
    var showNewFolder    by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is FileEvent.ShowSnackbar          -> snackbarHost.showSnackbar(event.message)
                is FileEvent.ShowDetails           -> { detailItem = event.item; detailSheetState.show() }
                is FileEvent.RequestAllFilesPermission -> onRequestAllFilesAccess()
                else                               -> Unit
            }
        }
    }

    BackHandler {
        when {
            uiState.isSearchActive             -> viewModel.setSearchActive(false)
            uiState.selectedItems.isNotEmpty() -> viewModel.clearSelection()
            else                               -> viewModel.navigateUp()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            AuraTopBar(
                uiState             = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSearchToggle      = viewModel::setSearchActive,
                onSortClick         = { showSortDialog = true },
                onViewToggle        = viewModel::toggleViewMode,
                onSelectAll         = viewModel::selectAll,
                onClearSelection    = viewModel::clearSelection,
                onDeleteSelected    = viewModel::deleteSelected,
                onCopySelected      = viewModel::copySelected,
                onCutSelected       = viewModel::cutSelected,
            )
        },
        bottomBar = {
            AuraBottomBar(
                uiState      = uiState,
                onHomeClick  = { viewModel.loadDirectory(android.os.Environment.getExternalStorageDirectory().absolutePath) },
                onPasteClick = viewModel::paste,
                onSearchClick= { viewModel.setSearchActive(true) },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.selectedItems.isEmpty(),
                enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)),
                exit    = scaleOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showNewFolder = true },
                    icon    = { Icon(Icons.Rounded.CreateNewFolder, null) },
                    text    = { Text("New folder") },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize()) {
            BreadcrumbRow(
                breadcrumbs  = uiState.breadcrumbs,
                onCrumbClick = viewModel::loadDirectory,
                modifier     = Modifier.padding(top = padding.calculateTopPadding()),
            )

            AnimatedContent(
                targetState = uiState.isLoading,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "loading",
            ) { loading ->
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    uiState.filteredItems.isEmpty() -> EmptyPane(uiState.isSearchActive)
                    else -> AnimatedContent(
                        targetState = uiState.viewMode,
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally { it / 8 }) togetherWith
                            (fadeOut() + slideOutHorizontally { -it / 8 })
                        },
                        label = "viewMode",
                    ) { mode ->
                        val bottomPad = padding.calculateBottomPadding() + 80.dp
                        when (mode) {
                            ViewMode.LIST -> FileListView(
                                items           = uiState.filteredItems,
                                selectedPaths   = uiState.selectedItems,
                                onItemClick     = viewModel::onItemClick,
                                onItemLongClick = { item ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onItemLongClick(item)
                                },
                                onMoreClick     = viewModel::showDetails,
                                contentPadding  = PaddingValues(bottom = bottomPad),
                            )
                            ViewMode.GRID -> FileGridView(
                                items           = uiState.filteredItems,
                                selectedPaths   = uiState.selectedItems,
                                onItemClick     = viewModel::onItemClick,
                                onItemLongClick = { item ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onItemLongClick(item)
                                },
                                contentPadding  = PaddingValues(bottom = bottomPad),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        SortDialog(
            current   = uiState.sortOrder,
            onSelect  = { viewModel.setSortOrder(it); showSortDialog = false },
            onDismiss = { showSortDialog = false },
        )
    }

    if (showNewFolder) {
        NewFolderDialog(
            onCreate  = { viewModel.createFolder(it); showNewFolder = false },
            onDismiss = { showNewFolder = false },
        )
    }

    detailItem?.let { item ->
        FileDetailsBottomSheet(
            item       = item,
            sheetState = detailSheetState,
            onDismiss  = { scope.launch { detailSheetState.hide(); detailItem = null } },
            onRename   = { viewModel.rename(item, it) },
            onDelete   = { viewModel.deleteSelected() },
            onCopy     = { viewModel.copySelected() },
            onMove     = { viewModel.cutSelected() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuraTopBar(
    uiState            : FileUiState,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle     : (Boolean) -> Unit,
    onSortClick        : () -> Unit,
    onViewToggle       : () -> Unit,
    onSelectAll        : () -> Unit,
    onClearSelection   : () -> Unit,
    onDeleteSelected   : () -> Unit,
    onCopySelected     : () -> Unit,
    onCutSelected      : () -> Unit,
) {
    AnimatedContent(
        targetState = uiState.selectedItems.isNotEmpty(),
        transitionSpec = {
            (fadeIn(tween(200)) + slideInVertically { -it / 4 }) togetherWith
            (fadeOut(tween(200)) + slideOutVertically { -it / 4 })
        },
        label = "topbar",
    ) { selecting ->
        if (selecting) {
            TopAppBar(
                title = { Text("${uiState.selectedItems.size} selected") },
                navigationIcon = {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Rounded.Close, "Clear selection")
                    }
                },
                actions = {
                    IconButton(onClick = onSelectAll)    { Icon(Icons.Rounded.SelectAll, "Select all") }
                    IconButton(onClick = onCopySelected) { Icon(Icons.Rounded.ContentCopy, "Copy") }
                    IconButton(onClick = onCutSelected)  { Icon(Icons.Rounded.ContentCut, "Cut") }
                    IconButton(onClick = onDeleteSelected){ Icon(Icons.Rounded.Delete, "Delete") }
                },
                colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            )
        } else {
            AnimatedContent(
                targetState = uiState.isSearchActive,
                label = "search",
                transitionSpec = {
                    (fadeIn() + expandHorizontally()) togetherWith (fadeOut() + shrinkHorizontally())
                },
            ) { searching ->
                if (searching) {
                    SearchBar(
                        query          = uiState.searchQuery,
                        onQueryChange  = onSearchQueryChange,
                        onSearch       = {},
                        active         = true,
                        onActiveChange = { onSearchToggle(it) },
                        placeholder    = { Text("Search files…") },
                        leadingIcon    = {
                            IconButton(onClick = { onSearchToggle(false) }) {
                                Icon(Icons.Rounded.ArrowBack, "Back")
                            }
                        },
                        trailingIcon   = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Rounded.Clear, "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {}
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text     = uiState.breadcrumbs.lastOrNull()?.name ?: "Aura Explorer",
                                style    = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        actions = {
                            IconButton(onClick = { onSearchToggle(true) }) { Icon(Icons.Rounded.Search, "Search") }
                            IconButton(onClick = onSortClick)              { Icon(Icons.Rounded.Sort, "Sort") }
                            IconButton(onClick = onViewToggle)             {
                                Icon(
                                    if (uiState.viewMode == ViewMode.LIST) Icons.Rounded.GridView
                                    else Icons.Rounded.ViewList,
                                    "Toggle view",
                                )
                            }
                        },
                        colors = topAppBarColors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
private fun AuraBottomBar(
    uiState      : FileUiState,
    onHomeClick  : () -> Unit,
    onPasteClick : () -> Unit,
    onSearchClick: () -> Unit,
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        IconButton(onClick = onHomeClick, modifier = Modifier.weight(1f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Home, "Home")
                Text("Home", style = MaterialTheme.typography.labelSmall)
            }
        }
        IconButton(onClick = onSearchClick, modifier = Modifier.weight(1f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Search, "Search")
                Text("Search", style = MaterialTheme.typography.labelSmall)
            }
        }
        AnimatedVisibility(visible = uiState.clipboardItems.isNotEmpty()) {
            IconButton(onClick = onPasteClick, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BadgedBox(badge = { Badge { Text("${uiState.clipboardItems.size}") } }) {
                        Icon(Icons.Rounded.ContentPaste, "Paste")
                    }
                    Text("Paste", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun BreadcrumbRow(
    breadcrumbs : List<BreadCrumb>,
    onCrumbClick: (String) -> Unit,
    modifier    : Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(breadcrumbs) {
        listState.animateScrollToItem(breadcrumbs.lastIndex.coerceAtLeast(0))
    }
    LazyRow(
        state             = listState,
        modifier          = modifier.fillMaxWidth().height(36.dp),
        contentPadding    = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(breadcrumbs) { index, crumb ->
            val isLast = index == breadcrumbs.lastIndex
            Text(
                text  = crumb.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isLast) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = !isLast) { onCrumbClick(crumb.path) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
            if (!isLast) {
                Icon(
                    Icons.Rounded.ChevronRight, null,
                    modifier = Modifier.size(16.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
fun FileListView(
    items          : List<FileItem>,
    selectedPaths  : Set<String>,
    onItemClick    : (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onMoreClick    : (FileItem) -> Unit,
    contentPadding : PaddingValues,
) {
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items, key = { _, it -> it.path }) { index, item ->
            FileListItem(
                item       = item,
                isSelected = item.path in selectedPaths,
                modifier   = Modifier.animateItem(
                    fadeInSpec    = tween(200),
                    fadeOutSpec   = tween(200),
                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                ),
                onClick     = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
                onMoreClick = { onMoreClick(item) },
            )
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 72.dp, end = 16.dp),
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun FileListItem(
    item       : FileItem,
    isSelected : Boolean,
    modifier   : Modifier,
    onClick    : () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val primary      = MaterialTheme.colorScheme.primary
    val iconMeta     = remember(item.type) { fileIconMeta(item.type, primary) }
    val bgAlpha by animateFloatAsState(if (isSelected) 1f else 0f, tween(200), label = "sel")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
            }
            .drawBehind { if (bgAlpha > 0f) drawRect(primary.copy(alpha = 0.12f * bgAlpha)) },
        color = Color.Transparent,
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = isSelected,
                    transitionSpec = { scaleIn(spring(Spring.DampingRatioMediumBouncy)) togetherWith scaleOut() },
                    label = "icon",
                ) { selected ->
                    if (selected) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Check, null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp))
                        }
                    } else {
                        Icon(iconMeta.icon, null, tint = iconMeta.tint, modifier = Modifier.size(28.dp))
                    }
                }
            }

            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = buildString {
                        if (item.isDirectory) append("Folder")
                        else { append(item.sizeFormatted); append("  ·  ") }
                        append(item.dateFormatted)
                    },
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onMoreClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.MoreVert, "More options",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FileGridView(
    items          : List<FileItem>,
    selectedPaths  : Set<String>,
    onItemClick    : (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    contentPadding : PaddingValues,
) {
    val primary = MaterialTheme.colorScheme.primary
    LazyVerticalGrid(
        columns               = GridCells.Adaptive(100.dp),
        contentPadding        = contentPadding + PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.path }) { item ->
            val isSelected = item.path in selectedPaths
            val iconMeta   = remember(item.type) { fileIconMeta(item.type, primary) }
            Surface(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .animateItem(fadeInSpec = tween(200), placementSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap       = { onItemClick(item) },
                            onLongPress = { onItemLongClick(item) },
                        )
                    },
                color          = if (isSelected) primary.copy(alpha = 0.15f)
                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 1.dp,
                shape          = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(iconMeta.icon, null, tint = iconMeta.tint, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(item.name, style = MaterialTheme.typography.labelSmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun EmptyPane(isSearch: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (isSearch) Icons.Rounded.SearchOff else Icons.Rounded.FolderOpen,
                null,
                modifier = Modifier.size(64.dp),
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Text(
                if (isSearch) "No results found" else "This folder is empty",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun SortDialog(current: SortOrder, onSelect: (SortOrder) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort by") },
        text = {
            Column {
                SortOrder.entries.forEach { order ->
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                            .clickable { onSelect(order) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = order == current, onClick = { onSelect(order) })
                        Spacer(Modifier.width(8.dp))
                        Text(order.label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NewFolderDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Folder name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private operator fun PaddingValues.plus(other: PaddingValues) = PaddingValues(
    start  = calculateLeftPadding(LayoutDirection.Ltr)  + other.calculateLeftPadding(LayoutDirection.Ltr),
    top    = calculateTopPadding()                       + other.calculateTopPadding(),
    end    = calculateRightPadding(LayoutDirection.Ltr) + other.calculateRightPadding(LayoutDirection.Ltr),
    bottom = calculateBottomPadding()                   + other.calculateBottomPadding(),
)
