package com.example.docapp.ui.home
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.docapp.core.DataValidator
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.FolderStateStore
import com.example.docapp.core.ThemeManager
import com.example.docapp.domain.Document
import com.example.docapp.domain.DocumentRepository
import com.example.docapp.domain.Folder
import com.example.docapp.ui.theme.AppAlphas
import com.example.docapp.ui.theme.AppBorderWidths
import com.example.docapp.ui.theme.AppColors
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.AppShapes
import com.example.docapp.ui.theme.AppLayout
import com.example.docapp.ui.theme.VSpace
import kotlinx.coroutines.launch
import com.example.docapp.ui.theme.GlassCard
import com.example.docapp.ui.theme.ThemeConfig
import com.example.docapp.ui.theme.SurfaceStyle
import com.example.docapp.ui.theme.SurfaceStyleTokens
import com.example.docapp.ui.theme.SurfaceTokens
import androidx.compose.foundation.shape.CircleShape
private const val NO_FOLDER_SECTION_ID = "__NO_FOLDER_SECTION__"
/**
 * Main home screen composable that displays a horizontal pager with three tabs: tree view, list view, and info screen.
 * Provides navigation to documents and document creation functionality.
 * 
 * Works by using HorizontalPager to display three different screens that can be swiped between.
 * Blocks back navigation to prevent returning to PIN screen.
 * 
 * arguments:
 *     openDoc - (String) -> Unit: Callback function invoked when a document is selected, receives the document ID
 *     createNew - (folderId: String?) -> Unit: Callback function invoked when creating a new document, receives optional folder ID
 *     navigator - AppNavigator: Navigation helper for safe back navigation
 * 
 * return:
 *     Unit - No return value
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePager(
    openDoc: (String) -> Unit,
    createNew: (folderId: String?) -> Unit,
    navigator: com.example.docapp.ui.navigation.AppNavigator
) {
    BackHandler(enabled = true) {
        navigator.safePopBack()
    }
    val pager = rememberPagerState(initialPage = 1, pageCount = { 3 })
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> TreeScreen(openDoc = openDoc, createInFolder = createNew)
            1 -> ListScreen(openDoc = openDoc, onCreate = { createNew(null) })
            2 -> InfoScreen()
        }
    }
}
/**
 * List screen composable that displays pinned and recently opened documents in a scrollable list.
 * Shows documents sorted by pinned status and last opened timestamp.
 * 
 * Works by observing the home document list from the repository, displaying pinned documents first,
 * then recently opened documents, with options to open documents or create new ones.
 * 
 * arguments:
 *     openDoc - (String) -> Unit: Callback function invoked when a document is selected, receives the document ID
 *     onCreate - () -> Unit: Callback function invoked when creating a new document
 * 
 * return:
 *     Unit - No return value
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListScreen(openDoc: (String) -> Unit, onCreate: () -> Unit) {
    val domainInteractors = ServiceLocator.domain
    val documentInteractors = domainInteractors.documents
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val home by documentInteractors.observeHome().collectAsState(
        initial = DocumentRepository.HomeList(emptyList(), emptyList())
    )
    var reorderMode by remember { mutableStateOf(false) }
    var firstSelected by remember { mutableStateOf<String?>(null) }
    var moveDocId by remember { mutableStateOf<String?>(null) }
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()
    var pinnedCollapsed by remember { mutableStateOf(false) }
    var recentCollapsed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val bottomInset = WindowInsets.navigationBars.getBottom(density) / density.density
    val topInset = WindowInsets.statusBars.getTop(density) / density.density
    Surface(color = NeoPalette.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.sectionSpacing),
                contentPadding = PaddingValues(
                    start = AppDimens.screenPadding,
                    end = AppDimens.screenPadding,
                    top = AppDimens.sectionSpacing + topInset.dp,
                    bottom = AppDimens.bottomButtonsSpacer + bottomInset.dp
                )
            ) {
                item {
                    HistorySectionCard(
                        title = "Pinned",
                        icon = Icons.Default.Star,
                        isCollapsed = pinnedCollapsed,
                        onToggleCollapse = { pinnedCollapsed = !pinnedCollapsed }
                    ) {
                        if (home.pinned.isNotEmpty()) {
                            if (reorderMode) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(NeoShapes.row)
                                        .background(NeoPalette.controlBackground)
                                        .padding(
                                            horizontal = AppDimens.panelPaddingHorizontal,
                                            vertical = AppDimens.panelPaddingVertical
                                        )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = NeoPalette.neon
                                        )
                                        Spacer(Modifier.width(AppDimens.spaceMd))
                                        Text(
                                            text = "Document selected. Tap another pinned document to swap",
                                            color = NeoPalette.textSecondary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { reorderMode = false; firstSelected = null }) {
                                            Text("Done")
                                        }
                                    }
                                }
                                Spacer(Modifier.height(AppDimens.sectionSpacing))
                            }
                            home.pinned.forEachIndexed { index, document ->
                                var menuOpen by remember(document.id) { mutableStateOf(false) }
                                val isSelected = reorderMode && firstSelected == document.id
                                HistoryDocumentRow(
                                    document = document,
                                    isPinned = true,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (!reorderMode) {
                                            openDoc(document.id)
                                        } else {
                                            val first = firstSelected
                                            if (first == null) {
                                                firstSelected = document.id
                                            } else if (first != document.id) {
                                                scope.launch {
                                                    try {
                                                        documentInteractors.swapPinned(first, document.id)
                                                        toast("Documents swapped")
                                                        firstSelected = document.id
                                                    } catch (error: Exception) {
                                                        toast("Swap failed")
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onTogglePin = {
                                        scope.launch { documentInteractors.pin(document.id, false) }
                                    },
                                    menuExpanded = menuOpen,
                                    onMenuOpen = {
                                        if (!reorderMode) menuOpen = true
                                    },
                                    onMenuDismiss = { menuOpen = false },
                                    onMoveDocument = {
                                        menuOpen = false
                                        moveDocId = document.id
                                    },
                                    menuEnabled = !reorderMode
                                )
                                if (index != home.pinned.lastIndex) {
                                    Spacer(Modifier.height(AppDimens.listSpacing))
                                }
                            }
                        }
                    }
                }
                item {
                    HistorySectionCard(
                        title = "Recent",
                        icon = Icons.Default.Description,
                        isCollapsed = recentCollapsed,
                        onToggleCollapse = { recentCollapsed = !recentCollapsed }
                    ) {
                        home.recent.forEachIndexed { index, document ->
                            var menuOpen by remember(document.id) { mutableStateOf(false) }
                            HistoryDocumentRow(
                                document = document,
                                isPinned = false,
                                isSelected = false,
                                onClick = { openDoc(document.id) },
                                onTogglePin = {
                                    scope.launch { documentInteractors.pin(document.id, true) }
                                },
                                menuExpanded = menuOpen,
                                onMenuOpen = { menuOpen = true },
                                onMenuDismiss = { menuOpen = false },
                                onMoveDocument = {
                                    menuOpen = false
                                    moveDocId = document.id
                                },
                                menuEnabled = true
                            )
                            if (index != home.recent.lastIndex) {
                                Spacer(Modifier.height(AppDimens.listSpacing))
                            }
                        }
                    }
                }
            }
            HistoryBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppDimens.spaceXl + bottomInset.dp),
                reorderMode = reorderMode,
                onToggleReorder = {
                    if (reorderMode) {
                        reorderMode = false
                        firstSelected = null
                    } else {
                        if (home.pinned.isEmpty()) {
                             toast("Nothing to rearrange")
                        } else {
                            reorderMode = true
                            firstSelected = null
                        }
                    }
                },
                onCreateDocument = onCreate
            )
        }
    }
    MoveToFolderDialogIfNeeded(moveDocId = moveDocId, onClose = { moveDocId = null })
}
/**
 * Composable for a collapsible section card in the history list screen.
 * Displays a title with icon and collapsible content area for pinned or recent documents.
 * 
 * Works by rendering a glass card with a header row containing icon and title, and a collapsible
 * content area that shows or hides based on the isCollapsed state.
 * 
 * arguments:
 *     title - String: The section title text to display in the header
 *     icon - ImageVector: The icon to display next to the title
 *     isCollapsed - Boolean: Whether the content area is currently collapsed (hidden) or expanded (visible)
 *     onToggleCollapse - () -> Unit: Callback function invoked when the collapse/expand button is clicked
 *     content - @Composable ColumnScope.() -> Unit: The composable content to display in the collapsible area
 * 
 * return:
 *     Unit - No return value
 */
@Composable
private fun HistorySectionCard(
    title: String,
    icon: ImageVector,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = NeoShapes.section) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.panelPaddingVertical
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier
                        .size(AppDimens.Home.folderToggleIconButton)
                        .clip(AppShapes.iconButton())
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                        contentDescription = if (isCollapsed) "Expand" else "Collapse",
                        tint = NeoPalette.neon,
                        modifier = Modifier.size(AppDimens.Home.folderToggleIcon)
                    )
                }
                Spacer(Modifier.width(AppDimens.iconRowSpacing))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeoPalette.neon,
                    modifier = Modifier
                        .size(AppDimens.Home.folderGlyphIcon)
                )
                Spacer(Modifier.width(AppDimens.iconRowSpacing))
                Text(
                    text = title,
                    color = NeoPalette.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            if (!isCollapsed) {
                Spacer(Modifier.height(AppDimens.listSpacing))
                content()
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryDocumentRow(
    document: Document,
    isPinned: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    menuExpanded: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onMoveDocument: () -> Unit,
    menuEnabled: Boolean
) {
    var descriptionMasked by remember(document.id) { mutableStateOf(true) }
    val rowBackground = if (isSelected) NeoPalette.item.copy(alpha = AppAlphas.Home.selectedRowBackground) else NeoPalette.item
    val borderColor = if (isSelected) NeoPalette.neon.copy(alpha = AppAlphas.Home.selectedBorderAccent) else Color.Transparent
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(NeoShapes.row)
                .background(rowBackground)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        if (menuEnabled) onMenuOpen()
                    }
                )
                .border(
                    width = if (borderColor == Color.Transparent) 0.dp else AppDimens.Home.selectionBorderWidth,
                    color = borderColor,
                    shape = NeoShapes.row
                )
                .padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.panelPaddingVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
        IconButton(
            onClick = onTogglePin,
            modifier = Modifier
                .size(AppDimens.Home.documentActionIconButton)
                .clip(AppShapes.iconButton())
        ) {
            Icon(
                imageVector = if (isPinned) Icons.Default.Star else Icons.Outlined.StarOutline,
                contentDescription = if (isPinned) "Unpin" else "Pin",
                tint = NeoPalette.neon
            )
        }
            Spacer(Modifier.width(AppDimens.iconRowSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    color = NeoPalette.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                if (document.description.isNotBlank()) {
                    Spacer(Modifier.height(AppDimens.labelSpacing))
                    Text(
                        text = if (descriptionMasked) "******" else document.description,
                        color = NeoPalette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.width(AppDimens.iconRowSpacing))
        IconButton(
            onClick = { descriptionMasked = !descriptionMasked },
            modifier = Modifier
                .size(AppDimens.Home.documentActionIconButton)
                .clip(AppShapes.iconButton())
        ) {
                Icon(
                    imageVector = if (descriptionMasked) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = if (descriptionMasked) "Show" else "Hide",
                    tint = NeoPalette.neon
                )
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
            DropdownMenuItem(
                text = { Text("Move to folder") },
                onClick = onMoveDocument
            )
        }
    }
}
@Composable
private fun HistoryBottomBar(
    modifier: Modifier = Modifier,
    reorderMode: Boolean,
    onToggleReorder: () -> Unit,
    onCreateDocument: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(NeoShapes.dock)
            .background(NeoPalette.dockBackground)
            .padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            ),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.dockSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockButton(
            icon = Icons.AutoMirrored.Outlined.NoteAdd,
            description = "New document",
            onClick = onCreateDocument
        )
        DockButton(
            icon = if (reorderMode) Icons.Default.Check else Icons.Default.SwapVert,
            description = if (reorderMode) "Finish reordering" else "Reorder pinned",
            onClick = onToggleReorder
        )
    }
}
private object NeoPalette {
    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background
    val section: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    val item: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    val iconBackground: Color
        @Composable get() = AppColors.iconAccentBackground()
    val controlBackground: Color
        @Composable get() = AppColors.level2Background()
    val dockBackground: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    val dockButtonBackground: Color
        @Composable get() = AppColors.level2Background()
    val neon: Color
        @Composable get() = AppColors.iconAccent()
    val textPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
    val textSecondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
}
private object NeoShapes {
    val section
        @Composable get() = AppShapes.panelLarge()
    val row
        @Composable get() = AppShapes.listItem()
    val dock
        @Composable get() = AppShapes.panelLarge()
    val dockButton
        @Composable get() = AppShapes.iconButton()
}
@Composable
private fun TreeScreen(
    openDoc: (String) -> Unit,
    createInFolder: (String?) -> Unit
) {
    val domainInteractors = ServiceLocator.domain
    val documentInteractors = domainInteractors.documents
    val folderInteractors = domainInteractors.folders
    val scope = rememberCoroutineScope()
    val folders by folderInteractors.observeTree().collectAsState(initial = emptyList())
    val home by documentInteractors.observeHome().collectAsState(
        initial = DocumentRepository.HomeList(emptyList(), emptyList())
    )
    val allDocs = remember(home) { (home.pinned + home.recent).distinctBy { it.id } }
    val docsByFolderId = remember(folders, allDocs) {
        folders.associate { folder -> folder.id to allDocs.filter { it.folderId == folder.id } }
    }
    val docsNoFolder = remember(allDocs) { allDocs.filter { it.folderId == null } }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var moveDocId by remember { mutableStateOf<String?>(null) }
    var deleteFolderId by remember { mutableStateOf<String?>(null) }
    var collapsedFolders by remember { mutableStateOf(FolderStateStore.getCollapsedFolders()) }
    LaunchedEffect(folders, docsNoFolder.isNotEmpty()) {
        val validIds = folders.map { it.id }.toMutableSet()
        if (docsNoFolder.isNotEmpty()) {
            validIds += NO_FOLDER_SECTION_ID
        }
        val filtered = collapsedFolders.intersect(validIds)
        if (filtered.size != collapsedFolders.size) {
            collapsedFolders = filtered
            FolderStateStore.saveCollapsedFolders(filtered)
        }
    }
    val density = LocalDensity.current
    val bottomInset = WindowInsets.navigationBars.getBottom(density) / density.density
    val topInset = WindowInsets.statusBars.getTop(density) / density.density
    Surface(color = NeoPalette.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.sectionSpacing),
                contentPadding = PaddingValues(
                    start = AppDimens.screenPadding,
                    end = AppDimens.screenPadding,
                    top = AppDimens.sectionSpacing + topInset.dp,
                    bottom = AppDimens.bottomButtonsSpacer + bottomInset.dp
                )
            ) {
                items(folders) { folder ->
                    val docs = docsByFolderId[folder.id].orEmpty()
                    val isCollapsed = collapsedFolders.contains(folder.id)
                    FolderSectionCard(
                        title = folder.name,
                        documents = docs,
                        isCollapsed = isCollapsed,
                        onToggleCollapse = {
                            collapsedFolders = if (isCollapsed) {
                                collapsedFolders - folder.id
                            } else {
                                collapsedFolders + folder.id
                            }
                            FolderStateStore.saveCollapsedFolders(collapsedFolders)
                        },
                        onCreateInFolder = { createInFolder(folder.id) },
                        onDeleteFolder = { deleteFolderId = folder.id },
                        openDoc = openDoc,
                        onMoveDoc = { moveDocId = it }
                    )
                }
                if (docsNoFolder.isNotEmpty()) {
                    val noFolderCollapsed = collapsedFolders.contains(NO_FOLDER_SECTION_ID)
                    item {
                        FolderSectionCard(
                            title = "Without a folder",
                            documents = docsNoFolder,
                            isCollapsed = noFolderCollapsed,
                            onToggleCollapse = {
                                collapsedFolders = if (noFolderCollapsed) {
                                    collapsedFolders - NO_FOLDER_SECTION_ID
                                } else {
                                    collapsedFolders + NO_FOLDER_SECTION_ID
                                }
                                FolderStateStore.saveCollapsedFolders(collapsedFolders)
                            },
                            onCreateInFolder = null,
                            onDeleteFolder = null,
                            openDoc = openDoc,
                            onMoveDoc = { moveDocId = it },
                            collapsible = true
                        )
                    }
                }
            }
            FloatingDock(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppDimens.dockBottomPadding + bottomInset.dp),
                onCreateDoc = { createInFolder(null) },
                onCreateFolder = { showNewFolderDialog = true }
            )
        }
    }
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val validation = DataValidator.validateFolderName(newFolderName)
                    if (validation.isSuccess) {
                        scope.launch {
                            folderInteractors.add(validation.getValue()!!, null)
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    } else {
                        ErrorHandler.showError(validation.getError()!!)
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }
    MoveToFolderDialogIfNeeded(moveDocId = moveDocId, onClose = { moveDocId = null })
    DeleteFolderDialogIfNeeded(deleteFolderId = deleteFolderId, onClose = { deleteFolderId = null })
}
@Composable
private fun FolderSectionCard(
    title: String,
    documents: List<Document>,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onCreateInFolder: (() -> Unit)?,
    onDeleteFolder: (() -> Unit)?,
    openDoc: (String) -> Unit,
    onMoveDoc: (String) -> Unit,
    collapsible: Boolean = true
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = NeoShapes.section) {
            val iconBackground = if (SurfaceTokens.current(ThemeConfig.surfaceStyle).useGradient) AppColors.level3Background() else Color.Transparent
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppDimens.panelPaddingHorizontal,
                        vertical = AppDimens.panelPaddingVertical
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (collapsible) {
                        IconButton(
                            onClick = onToggleCollapse,
                            modifier = Modifier
                                .size(AppDimens.Home.folderToggleIconButton)
                                .clip(AppShapes.iconButton())
                                .background(iconBackground)
                        ) {
                            Icon(
                                imageVector = if (isCollapsed) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                                tint = NeoPalette.neon,
                                modifier = Modifier.size(AppDimens.Home.folderToggleIcon)
                            )
                        }
                        Spacer(Modifier.width(AppDimens.spaceMd))
                    } else {
                        Spacer(Modifier.width(AppDimens.spaceXxs))
                    }
                    Text(
                        text = title,
                        color = NeoPalette.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (onCreateInFolder != null || onDeleteFolder != null) {
                        Box {
                            IconButton(
                                onClick = { menuOpen = true },
                                modifier = Modifier
                                    .size(AppDimens.Home.documentActionIconButton)
                                    .clip(AppShapes.iconButton())
                                    .background(iconBackground)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = "Folder options",
                                    tint = NeoPalette.neon
                                )
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (onCreateInFolder != null) {
                                    DropdownMenuItem(
                                        text = { Text("Create document") },
                                        onClick = {
                                            menuOpen = false
                                            onCreateInFolder()
                                        }
                                    )
                                }
                                if (onDeleteFolder != null) {
                                    DropdownMenuItem(
                                        text = { Text("Delete folder") },
                                        onClick = {
                                            menuOpen = false
                                            onDeleteFolder()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                if (!isCollapsed || !collapsible) {
                    if (documents.isNotEmpty()) {
                        Spacer(Modifier.height(AppDimens.listSpacing))
                        documents.forEachIndexed { index, doc ->
                            DocumentRow(
                                document = doc,
                                openDoc = openDoc,
                                onMoveDoc = onMoveDoc
                            )
                            if (index != documents.lastIndex) {
                                Spacer(Modifier.height(AppDimens.listSpacing))
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentRow(
    document: Document,
    openDoc: (String) -> Unit,
    onMoveDoc: (String) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var isMasked by remember(document.id) { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NeoShapes.row)
            .background(NeoPalette.item)
            .combinedClickable(
                onClick = { openDoc(document.id) },
                onLongClick = { menuOpen = true }
            )
            .padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = NeoPalette.neon,
            modifier = Modifier
                .size(AppDimens.Home.documentLeadingIcon)
        )
        Spacer(Modifier.width(AppDimens.spaceLg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.name,
                color = NeoPalette.textPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            if (document.description.isNotBlank()) {
                Spacer(Modifier.height(AppDimens.labelSpacing))
                Text(
                    text = if (isMasked) "******" else document.description,
                    color = NeoPalette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(Modifier.width(AppDimens.iconRowSpacing))
        IconButton(
            onClick = { isMasked = !isMasked },
            modifier = Modifier
                .size(AppDimens.Home.documentActionIconButton)
                .clip(AppShapes.iconButton())
        ) {
            Icon(
                imageVector = if (isMasked) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = if (isMasked) "Show description" else "Hide description",
                tint = NeoPalette.neon
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Move to folder") },
                onClick = {
                    menuOpen = false
                    onMoveDoc(document.id)
                }
            )
        }
    }
}
@Composable
private fun FloatingDock(
    modifier: Modifier = Modifier,
    onCreateDoc: () -> Unit,
    onCreateFolder: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(NeoShapes.dock)
            .background(NeoPalette.dockBackground)
            .padding(
                horizontal = AppDimens.panelPaddingHorizontal,
                vertical = AppDimens.panelPaddingVertical
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.dockSpacing)
    ) {
        DockButton(
            icon = Icons.AutoMirrored.Outlined.NoteAdd,
            description = "New document",
            onClick = onCreateDoc
        )
        DockButton(
            icon = Icons.Outlined.CreateNewFolder,
            description = "New folder",
            onClick = onCreateFolder
        )
    }
}
@Composable
private fun DockButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(AppDimens.Home.dockButton)
            .clip(NeoShapes.dockButton)
            .background(NeoPalette.dockButtonBackground)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = NeoPalette.neon,
            modifier = Modifier.size(AppDimens.Home.dockButtonIcon)
        )
    }
}
@Composable
private fun MoveToFolderDialogIfNeeded(moveDocId: String?, onClose: () -> Unit) {
    if (moveDocId == null) return
    val domainInteractors = ServiceLocator.domain
    val folderInteractors = domainInteractors.folders
    val documentInteractors = domainInteractors.documents
    val scope = rememberCoroutineScope()
    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        folders = folderInteractors.listAll()
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Move to folder") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedFolderId == null, onClick = { selectedFolderId = null })
                    Text("No folder")
                }
                Spacer(Modifier.height(AppDimens.spaceSm))
                folders.forEach { folder ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedFolderId == folder.id, onClick = { selectedFolderId = folder.id })
                        Text(folder.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    documentInteractors.moveToFolder(moveDocId, selectedFolderId)
                    onClose()
                }
            }) { Text("Move") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel") }
        }
    )
}
@Composable
private fun InfoScreen() {
    val context = LocalContext.current
    val domainInteractors = ServiceLocator.domain
    val settingsInteractors = domainInteractors.settings
    val dataTransfer = ServiceLocator.dataTransfer
    var changing by remember { mutableStateOf(false) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var transferMessage by remember { mutableStateOf<String?>(null) }
    var transferInProgress by remember { mutableStateOf(false) }
    var lastBackupFile by remember { mutableStateOf<java.io.File?>(null) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var importPassword by remember { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportPasswordDialog = true
        }
    }
    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    fun shareBackup(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "DocApp Backup")
                putExtra(Intent.EXTRA_TEXT, "DocApp backup file: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share backup"))
        } catch (e: Exception) {
            toast("Failed to share backup: ${e.message}")
        }
    }
    fun isFourDigits(value: String) = value.matches(Regex("^\\d{4}$"))
    val density = LocalDensity.current
    val bottomInset = WindowInsets.navigationBars.getBottom(density) / density.density
    val topInset = WindowInsets.statusBars.getTop(density) / density.density
    Surface(color = NeoPalette.background, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.sectionSpacing),
            contentPadding = PaddingValues(
                start = AppDimens.screenPadding,
                end = AppDimens.screenPadding,
                top = AppDimens.sectionSpacing + topInset.dp,
                bottom = AppDimens.bottomButtonsSpacer + bottomInset.dp
            )
        ) {
            item {
                Text(
                    text = "INFO & SETTINGS",
                    color = NeoPalette.textPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            item {
                InformationSectionCard(title = "App theme") {
                    Text(
                        text = "Tune the visuals to match your mood.",
                        color = NeoPalette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(AppDimens.spaceLg))
                    Text(
                        text = "Palette",
                        color = NeoPalette.textPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(AppDimens.spaceSm))
                    ThemePaletteToggle(
                        isDark = ThemeManager.isDarkTheme,
                        onSelect = { dark -> ThemeManager.setTheme(context, dark) }
                    )
                    Spacer(Modifier.height(AppDimens.spaceLg))
                    Text(
                        text = "Surface style",
                        color = NeoPalette.textPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(AppDimens.spaceSm))
                    SurfaceStyleToggle(
                        current = ThemeConfig.surfaceStyle,
                        onSelect = { style -> ThemeConfig.surfaceStyle = style }
                    )
                }
            }
            item {
                InformationSectionCard(title = "Contacts & links") {
                    InformationLinkButton(
                        text = "Open GitHub",
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com")
                            )
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(AppDimens.spaceMd))
                    Text(
                        text = "Telegram: @irisus_r",
                        color = NeoPalette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Version: 1.0.0",
                        color = NeoPalette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            item {
                InformationSectionCard(title = "PIN") {
                    if (!changing) {
                        InformationPrimaryButton(
                            text = "Change PIN",
                            onClick = { changing = true }
                        )
                    } else {
                        InformationTextField(
                            value = oldPin,
                            onValueChange = { oldPin = it },
                            label = "Current PIN"
                        )
                        Spacer(Modifier.height(AppDimens.spaceMd))
                        InformationTextField(
                            value = newPin,
                            onValueChange = { newPin = it },
                            label = "New PIN (4 digits)"
                        )
                        Spacer(Modifier.height(AppDimens.spaceMd))
                        InformationTextField(
                            value = confirmPin,
                            onValueChange = { confirmPin = it },
                            label = "Confirm PIN"
                        )
                        Spacer(Modifier.height(AppDimens.spaceLg))
                        InformationPrimaryButton(
                            text = "Apply",
                            onClick = {
                                scope.launch {
                                    if (!isFourDigits(newPin)) {
                                        toast("PIN must contain four digits")
                                        return@launch
                                    }
                                    if (newPin != confirmPin) {
                                        toast("PIN confirmation does not match")
                                        return@launch
                                    }
                                    if (oldPin == newPin) {
                                        toast("New PIN must differ from the current one")
                                        return@launch
                                    }
                                    val ok = settingsInteractors.verifyPin(oldPin)
                                    if (!ok) {
                                        toast("Current PIN is incorrect")
                                        return@launch
                                    }
                                    settingsInteractors.setNewPin(newPin)
                                    toast("PIN updated")
                                    oldPin = ""
                                    newPin = ""
                                    confirmPin = ""
                                    changing = false
                                }
                            }
                        )
                        Spacer(Modifier.height(AppDimens.spaceMd))
                        InformationGhostButton(
                            text = "Cancel",
                            onClick = {
                                oldPin = ""
                                newPin = ""
                                confirmPin = ""
                                changing = false
                            }
                        )
                    }
                }
            }
            item {
                InformationSectionCard(title = "Data transfer") {
                    Text(
                        text = "Export all documents with media into a zip file and restore it on another phone.",
                        color = NeoPalette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(AppDimens.spaceLg))
                    InformationPrimaryButton(
                        text = if (transferInProgress) "Working..." else "Export backup"
                    ) {
                        if (!transferInProgress) {
                            exportPassword = ""
                            showExportPasswordDialog = true
                        }
                    }
                    Spacer(Modifier.height(AppDimens.spaceSm))
                    if (lastBackupFile != null) {
                        InformationGhostButton(text = "Share backup") {
                            if (!transferInProgress && lastBackupFile != null) {
                                shareBackup(lastBackupFile!!)
                            }
                        }
                        Spacer(Modifier.height(AppDimens.spaceSm))
                    }
                    InformationGhostButton(text = "Import backup") {
                        if (!transferInProgress) {
                            importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                        }
                    }
                    transferMessage?.let { message ->
                        Spacer(Modifier.height(AppDimens.spaceSm))
                        Text(
                            text = message,
                            color = NeoPalette.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        if (showExportPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showExportPasswordDialog = false },
                title = { Text("Export backup") },
                text = {
                    Column {
                        Text(
                            text = "Enter password to protect backup (leave empty for no password):",
                            color = NeoPalette.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(AppDimens.spaceMd))
                        InformationTextField(
                            value = exportPassword,
                            onValueChange = { exportPassword = it },
                            label = "Password (optional)"
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExportPasswordDialog = false
                        scope.launch {
                            transferInProgress = true
                            transferMessage = "Exporting backup..."
                            try {
                                val password = exportPassword.takeIf { it.isNotEmpty() }
                                val result = dataTransfer.exportBackup(password)
                                lastBackupFile = result.file
                                transferMessage = "Backup saved: ${result.file.name} (${result.documents} docs)"
                                exportPassword = ""
                            } catch (e: Exception) {
                                transferMessage = "Export failed: ${e.message}"
                            } finally {
                                transferInProgress = false
                            }
                        }
                    }) { Text("Export") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showExportPasswordDialog = false
                        exportPassword = ""
                    }) { Text("Cancel") }
                }
            )
        }
        if (showImportPasswordDialog && pendingImportUri != null) {
            AlertDialog(
                onDismissRequest = {
                    showImportPasswordDialog = false
                    pendingImportUri = null
                    importPassword = ""
                },
                title = { Text("Import backup") },
                text = {
                    Column {
                        Text(
                            text = "Enter password for backup file (leave empty if not protected):",
                            color = NeoPalette.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(AppDimens.spaceMd))
                        InformationTextField(
                            value = importPassword,
                            onValueChange = { importPassword = it },
                            label = "Password (optional)"
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val uri = pendingImportUri
                        showImportPasswordDialog = false
                        pendingImportUri = null
                        if (uri != null) {
                            scope.launch {
                                transferInProgress = true
                                transferMessage = "Importing backup..."
                                try {
                                    val password = importPassword.takeIf { it.isNotEmpty() }
                                    val result = dataTransfer.importBackup(uri, password)
                                    transferMessage = "Imported ${result.documents} docs / ${result.attachments} files"
                                    importPassword = ""
                                } catch (e: Exception) {
                                    transferMessage = "Import failed: ${e.message}"
                                } finally {
                                    transferInProgress = false
                                }
                            }
                        }
                    }) { Text("Import") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showImportPasswordDialog = false
                        pendingImportUri = null
                        importPassword = ""
                    }) { Text("Cancel") }
                }
            )
        }
    }
}
@Composable
private fun InformationSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val surfaceTokens = SurfaceTokens.current(ThemeConfig.surfaceStyle)
    val shape = NeoShapes.section
    val border = if (surfaceTokens.borderWidth > 0.dp) BorderStroke(surfaceTokens.borderWidth, MaterialTheme.colorScheme.outline) else null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (surfaceTokens.useGradient) Color.Transparent else NeoPalette.section
        ),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (surfaceTokens.useGradient)
                        Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            ),
                            shape
                        )
                    else Modifier.background(NeoPalette.section, shape)
                )
                .padding(
                    horizontal = AppDimens.panelPaddingHorizontal,
                    vertical = AppDimens.panelPaddingVertical
                )
        ) {
            Text(
                text = title,
                color = NeoPalette.textPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(AppDimens.listSpacing))
            content()
        }
    }
}
@Composable
private fun InformationPrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = NeoPalette.neon,
            contentColor = NeoPalette.background
        ),
        shape = AppShapes.primaryButton()
    ) {
        Text(text = text.uppercase())
    }
}
@Composable
private fun InformationGhostButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(AppBorderWidths.thin, NeoPalette.neon),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeoPalette.neon),
        shape = AppShapes.secondaryButton()
    ) {
        Text(text = text.uppercase())
    }
}
@Composable
private fun InformationLinkButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(AppBorderWidths.thin, NeoPalette.neon.copy(alpha = AppAlphas.Home.selectedBorderAccent)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeoPalette.neon),
        shape = AppShapes.secondaryButton()
    ) {
        Text(text = text.uppercase())
    }
}
@Composable
private fun InformationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = NeoPalette.textSecondary) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = NeoPalette.textPrimary,
            unfocusedTextColor = NeoPalette.textPrimary,
            cursorColor = NeoPalette.neon,
            focusedBorderColor = NeoPalette.neon,
            unfocusedBorderColor = NeoPalette.controlBackground,
            focusedLabelColor = NeoPalette.neon,
            unfocusedLabelColor = NeoPalette.textSecondary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}
@Composable
private fun InformationHintBlock(title: String, hints: List<String>) {
    Text(
        text = title,
        color = NeoPalette.textPrimary,
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(Modifier.height(AppDimens.spaceSm))
    hints.forEachIndexed { index, hint ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "�",
                color = NeoPalette.neon,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = AppDimens.listSpacing)
            )
            Text(
                text = hint,
                color = NeoPalette.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
        if (index != hints.lastIndex) {
            Spacer(Modifier.height(AppDimens.spaceSm))
        }
    }
}
@Composable
private fun ThemePaletteToggle(isDark: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
    ) {
        ThemePaletteOptionButton(
            title = "Dark",
            isActive = isDark,
            onClick = { onSelect(true) },
            modifier = Modifier.weight(1f)
        )
        ThemePaletteOptionButton(
            title = "Light",
            isActive = !isDark,
            onClick = { onSelect(false) },
            modifier = Modifier.weight(1f)
        )
    }
}
@Composable
private fun ThemePaletteOptionButton(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ButtonDefaults.buttonColors(
        containerColor = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        shape = AppLayout.smallButtonShape()
    ) {
        Text(title.uppercase())
    }
}
@Composable
private fun SurfaceStyleToggle(current: SurfaceStyle, onSelect: (SurfaceStyle) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
    ) {
        SurfaceStyleOptionButton(
            title = "Glass",
            target = SurfaceStyle.Glass,
            current = current,
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
        SurfaceStyleOptionButton(
            title = "Matte",
            target = SurfaceStyle.Matte,
            current = current,
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
    }
}
@Composable
private fun SurfaceStyleOptionButton(
    title: String,
    target: SurfaceStyle,
    current: SurfaceStyle,
    onSelect: (SurfaceStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = target == current
    val colors = ButtonDefaults.buttonColors(
        containerColor = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = { onSelect(target) },
        modifier = modifier,
        colors = colors,
        shape = AppLayout.smallButtonShape()
    ) {
        Text(title.uppercase())
    }
}
@Composable
private fun DeleteFolderDialogIfNeeded(deleteFolderId: String?, onClose: () -> Unit) {
    if (deleteFolderId == null) return
    val domainInteractors = ServiceLocator.domain
    val documentInteractors = domainInteractors.documents
    val folderInteractors = domainInteractors.folders
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var documentsInFolder by remember { mutableStateOf<List<Document>>(emptyList()) }
    var deleteDocuments by remember { mutableStateOf(false) }
    LaunchedEffect(deleteFolderId) {
        documentsInFolder = documentInteractors.getDocumentsInFolder(deleteFolderId)
    }
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Delete folder") },
        text = {
            Column {
                Text("Folder contains ${documentsInFolder.size} ${if (documentsInFolder.size == 1) "document" else "documents"}.")
                VSpace(AppDimens.spaceSm)
                Text("What should happen to them?")
                VSpace(AppDimens.spaceSm)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !deleteDocuments,
                        onClick = { deleteDocuments = false }
                    )
                    Text("Move to \"No folder\"", modifier = Modifier.padding(start = AppDimens.spaceSm))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = deleteDocuments,
                        onClick = { deleteDocuments = true }
                    )
                    Text("Delete with the folder", modifier = Modifier.padding(start = AppDimens.spaceSm))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            if (deleteDocuments) {
                                documentsInFolder.forEach { document: Document ->
                                    documentInteractors.delete(document.id)
                                }
                            } else {
                                documentsInFolder.forEach { document: Document ->
                                    documentInteractors.moveToFolder(document.id, null)
                                }
                            }
                            folderInteractors.delete(deleteFolderId)
                            toast(if (deleteDocuments) "Folder and documents removed" else "Folder deleted, documents moved")
                            onClose()
                        } catch (e: Exception) {
                            toast("Error: ${e.message}")
                        }
                    }
                }
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel") }
        }
    )
}
