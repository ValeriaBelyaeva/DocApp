package com.example.docapp.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.NoteAdd
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.docapp.core.DataValidator
import com.example.docapp.core.ErrorHandler
import com.example.docapp.core.ServiceLocator
import com.example.docapp.core.ThemeManager
import com.example.docapp.domain.Document
import com.example.docapp.domain.DocumentRepository
import com.example.docapp.domain.Folder
import com.example.docapp.domain.usecases.UseCases
import com.example.docapp.ui.theme.AppDimens
import com.example.docapp.ui.theme.AppLayout
import com.example.docapp.ui.theme.VSpace
import kotlinx.coroutines.launch
import com.example.docapp.ui.theme.GlassCard
import com.example.docapp.ui.theme.ThemeConfig
import com.example.docapp.ui.theme.SurfaceStyle
import com.example.docapp.ui.theme.SurfaceStyleTokens
import com.example.docapp.ui.theme.SurfaceTokens


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePager(
    openDoc: (String) -> Unit,
    createNew: (folderId: String?) -> Unit
) {
    val pager = rememberPagerState(initialPage = 1, pageCount = { 3 })
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> TreeScreen(openDoc = openDoc, createInFolder = createNew)
            1 -> ListScreen(openDoc = openDoc, onCreate = { createNew(null) })
            2 -> InfoScreen()
        }
    }
}

/* ===== Список (закреплённые/последние) с перестановкой + перемещением в папку ===== */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListScreen(openDoc: (String) -> Unit, onCreate: () -> Unit) {
    val uc = ServiceLocator.useCases
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val home by uc.observeHome().collectAsState(
        initial = DocumentRepository.HomeList(emptyList(), emptyList())
    )

    // Режим перестановки закреплённых
    var reorderMode by remember { mutableStateOf(false) }
    var firstSelected by remember { mutableStateOf<String?>(null) }

    // Диалог перемещения
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
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 24.dp + topInset.dp,
                    bottom = 160.dp + bottomInset.dp
                )
            ) {
                item {
                    HistorySectionCard(
                        title = "Закрепленные",
                        icon = Icons.Default.Star,
                        isCollapsed = pinnedCollapsed,
                        onToggleCollapse = { pinnedCollapsed = !pinnedCollapsed }
                    ) {
                        if (home.pinned.isEmpty()) {
                            HistoryEmptyPlaceholder("Пока нет закрепленных документов")
                        } else {
                            if (reorderMode) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(NeoShapes.row)
                                        .background(NeoPalette.controlBackground)
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = NeoPalette.neon
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Выбрали документ. Коснитесь другого закрепленного, чтобы поменять местами",
                                            color = NeoPalette.textSecondary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { reorderMode = false; firstSelected = null }) {
                                            Text("Готово")
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
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
                                                        uc.swapPinned(first, document.id)
                                                        toast("Поменяли местами")
                                                        firstSelected = document.id
                                                    } catch (error: Exception) {
                                                        toast("Не удалось переставить")
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onTogglePin = {
                                        scope.launch { uc.pinDoc(document.id, false) }
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
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    HistorySectionCard(
                        title = "Последние",
                        icon = Icons.Default.Description,
                        isCollapsed = recentCollapsed,
                        onToggleCollapse = { recentCollapsed = !recentCollapsed }
                    ) {
                        if (home.recent.isEmpty()) {
                            HistoryEmptyPlaceholder("Здесь появятся недавно открытые")
                        } else {
                            home.recent.forEachIndexed { index, document ->
                                var menuOpen by remember(document.id) { mutableStateOf(false) }
                                HistoryDocumentRow(
                                    document = document,
                                    isPinned = false,
                                    isSelected = false,
                                    onClick = { openDoc(document.id) },
                                    onTogglePin = {
                                        scope.launch { uc.pinDoc(document.id, true) }
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
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            HistoryBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp + bottomInset.dp),
                reorderMode = reorderMode,
                onToggleReorder = {
                    if (reorderMode) {
                        reorderMode = false
                        firstSelected = null
                    } else {
                        if (home.pinned.isEmpty()) {
                            toast("Нечего переставлять")
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
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(NeoShapes.dockButton)
                        .background(NeoPalette.controlBackground)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                        contentDescription = if (isCollapsed) "Развернуть" else "Свернуть",
                        tint = NeoPalette.neon
                    )
                }
                Spacer(Modifier.width(16.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeoPalette.neon,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(NeoShapes.dockButton)
                        .background(NeoPalette.iconBackground)
                        .padding(6.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    color = NeoPalette.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!isCollapsed) {
                Spacer(Modifier.height(18.dp))
                content()
            }
        }
    }
}

@Composable
private fun HistoryEmptyPlaceholder(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NeoShapes.row)
            .background(NeoPalette.item.copy(alpha = 0.6f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = NeoPalette.textSecondary,
            modifier = Modifier
                .size(32.dp)
                .clip(NeoShapes.dockButton)
                .background(NeoPalette.iconBackground)
                .padding(6.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            color = NeoPalette.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
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

    val rowBackground = if (isSelected) NeoPalette.item.copy(alpha = 0.9f) else NeoPalette.item
    val borderColor = if (isSelected) NeoPalette.neon.copy(alpha = 0.6f) else Color.Transparent

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
                    width = if (borderColor == Color.Transparent) 0.dp else 1.5.dp,
                    color = borderColor,
                    shape = NeoShapes.row
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier
                    .size(40.dp)
                    .clip(NeoShapes.dockButton)
                    .background(NeoPalette.controlBackground)
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (isPinned) "Открепить" else "Закрепить",
                    tint = NeoPalette.neon
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    color = NeoPalette.textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                if (document.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (descriptionMasked) "＊＊＊＊＊＊" else document.description,
                        color = NeoPalette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = { descriptionMasked = !descriptionMasked },
                modifier = Modifier
                    .size(40.dp)
                    .clip(NeoShapes.dockButton)
                    .background(NeoPalette.controlBackground)
            ) {
                Icon(
                    imageVector = if (descriptionMasked) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = if (descriptionMasked) "Показать" else "Скрыть",
                    tint = NeoPalette.neon
                )
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
            DropdownMenuItem(
                text = { Text("Переместить в папку") },
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
            .padding(horizontal = 32.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockButton(
            icon = Icons.Outlined.NoteAdd,
            description = "Новый документ",
            onClick = onCreateDocument
        )
        DockButton(
            icon = if (reorderMode) Icons.Default.Check else Icons.Default.SwapVert,
            description = if (reorderMode) "Завершить перестановку" else "Перестановка закрепленных",
            onClick = onToggleReorder
        )
    }
}

/* ===== Дерево (папки + “без папки”) — новый визуал ===== */

private object NeoPalette {
    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background
    val section: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    val item: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val iconBackground: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    val controlBackground: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val dockBackground: Color
        @Composable get() = MaterialTheme.colorScheme.surface
    val dockButtonBackground: Color
        @Composable get() = MaterialTheme.colorScheme.secondaryContainer
    val neon: Color
        @Composable get() = MaterialTheme.colorScheme.primary
    val textPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface
    val textSecondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
}

private object NeoShapes {
    private val tokens: SurfaceStyleTokens
        @Composable get() = SurfaceTokens.current(ThemeConfig.surfaceStyle)

    val section
        @Composable get() = tokens.shapes.largeCard
    val row
        @Composable get() = tokens.shapes.mediumCard
    val dock
        @Composable get() = tokens.shapes.largeCard
    val dockButton
        @Composable get() = tokens.shapes.icon
}

@Composable
private fun TreeScreen(
    openDoc: (String) -> Unit,
    createInFolder: (String?) -> Unit
) {
    val uc = ServiceLocator.useCases
    val scope = rememberCoroutineScope()
    val folders by uc.observeTree().collectAsState(initial = emptyList())
    val home by uc.observeHome().collectAsState(
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

    var collapsedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }

    val density = LocalDensity.current
    val bottomInset = WindowInsets.navigationBars.getBottom(density) / density.density
    val topInset = WindowInsets.statusBars.getTop(density) / density.density

    Surface(color = NeoPalette.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 24.dp + topInset.dp,
                    bottom = 160.dp + bottomInset.dp
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
                        },
                        onCreateInFolder = { createInFolder(folder.id) },
                        onDeleteFolder = { deleteFolderId = folder.id },
                        openDoc = openDoc,
                        onMoveDoc = { moveDocId = it }
                    )
                }

                if (docsNoFolder.isNotEmpty()) {
                    item {
                        FolderSectionCard(
                            title = "Without a folder",
                            documents = docsNoFolder,
                            isCollapsed = false,
                            onToggleCollapse = {},
                            onCreateInFolder = null,
                            onDeleteFolder = null,
                            openDoc = openDoc,
                            onMoveDoc = { moveDocId = it },
                            collapsible = false
                        )
                    }
                }
            }

            FloatingDock(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp + bottomInset.dp),
                onCreateDoc = { createInFolder(null) },
                onCreateFolder = { showNewFolderDialog = true }
            )
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Новая папка") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Название папки") },
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
                            ServiceLocator.repos.folders.addFolder(validation.getValue()!!, null)
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    } else {
                        ErrorHandler.showError(validation.getError()!!)
                    }
                }) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Отмена") }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (collapsible) {
                        IconButton(
                            onClick = onToggleCollapse,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(NeoShapes.dockButton)
                                .background(NeoPalette.controlBackground)
                        ) {
                            Icon(
                                imageVector = if (isCollapsed) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                                contentDescription = if (isCollapsed) "Развернуть" else "Свернуть",
                                tint = NeoPalette.neon
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    } else {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.NoteAdd,
                            contentDescription = null,
                            tint = NeoPalette.neon,
                            modifier = Modifier
                                .size(32.dp)
                                .background(NeoPalette.controlBackground, NeoShapes.dockButton)
                                .padding(6.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    Text(
                        text = title,
                        color = NeoPalette.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )

                    if (onCreateInFolder != null || onDeleteFolder != null) {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(NeoShapes.dockButton)
                                .background(NeoPalette.controlBackground)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Настройки папки",
                            tint = NeoPalette.neon
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (onCreateInFolder != null) {
                                DropdownMenuItem(
                                    text = { Text("Создать документ") },
                                    onClick = {
                                        menuOpen = false
                                        onCreateInFolder()
                                    }
                                )
                            }
                            if (onDeleteFolder != null) {
                                DropdownMenuItem(
                                    text = { Text("Удалить папку") },
                                    onClick = {
                                        menuOpen = false
                                        onDeleteFolder()
                                    }
                                )
                            }
                        }
                    }
                }

                if (!isCollapsed || !collapsible) {
                    Spacer(Modifier.height(16.dp))
                    documents.forEachIndexed { index, doc ->
                        DocumentRow(
                            document = doc,
                            openDoc = openDoc,
                            onMoveDoc = onMoveDoc
                        )
                        if (index != documents.lastIndex) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
            if (documents.isEmpty()) {
                HistoryEmptyPlaceholder("В этой папке пока пусто")
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = NeoPalette.neon,
            modifier = Modifier
                .size(36.dp)
                .clip(NeoShapes.dockButton)
                .background(NeoPalette.iconBackground)
                .padding(6.dp)
        )
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.name,
                color = NeoPalette.textPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            if (document.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isMasked) "＊＊＊＊＊＊" else document.description,
                    color = NeoPalette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        IconButton(
            onClick = { isMasked = !isMasked },
            modifier = Modifier
                .size(40.dp)
                .clip(NeoShapes.dockButton)
                .background(NeoPalette.controlBackground)
        ) {
            Icon(
                imageVector = if (isMasked) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = if (isMasked) "Показать описание" else "Скрыть описание",
                tint = NeoPalette.neon
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Переместить в папку") },
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
            .padding(horizontal = 32.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockButton(
            icon = Icons.Outlined.NoteAdd,
            description = "Новый документ",
            onClick = onCreateDoc
        )
        DockButton(
            icon = Icons.Outlined.CreateNewFolder,
            description = "Новая папка",
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
            .size(56.dp)
            .clip(NeoShapes.dockButton)
            .background(NeoPalette.dockButtonBackground)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = NeoPalette.neon,
            modifier = Modifier.size(28.dp)
        )
    }
}

/* ===== Диалог перемещения документа в папку ===== */
@Composable
private fun MoveToFolderDialogIfNeeded(moveDocId: String?, onClose: () -> Unit) {
    if (moveDocId == null) return
    val uc: UseCases = ServiceLocator.useCases
    val scope = rememberCoroutineScope()
    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        folders = uc.listFolders()
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Переместить в папку") },
        text = {
            Column {
                // Опция "Без папки"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == null, onClick = { selected = null })
                    Text("Без папки")
                }
                Spacer(Modifier.height(AppDimens.spaceSm))
                folders.forEach { f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected == f.id, onClick = { selected = f.id })
                        Text(f.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    uc.moveDocToFolder(moveDocId, selected)
                    onClose()
                }
            }) { Text("Переместить") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Отмена") }
        }
    )
}

/* ===== InfoScreen (как в предыдущей версии с валидацией PIN) ===== */

@Composable
private fun InfoScreen() {
    val context = LocalContext.current
    val useCases = ServiceLocator.useCases
    var changing by remember { mutableStateOf(false) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    fun isFourDigits(value: String) = value.matches(Regex("^\\d{4}$"))

    val density = LocalDensity.current
    val bottomInset = WindowInsets.navigationBars.getBottom(density) / density.density
    val topInset = WindowInsets.statusBars.getTop(density) / density.density

    Surface(color = NeoPalette.background, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 32.dp + topInset.dp,
                bottom = 160.dp + bottomInset.dp
            )
        ) {
            item {
                Text(
                    text = "INFO & SETTINGS",
                    color = NeoPalette.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            item {
                InformationSectionCard(title = "Тема приложения") {
                    Text(
                        text = "Оформи внешний вид, чтобы он подходил под настроение.",
                        color = NeoPalette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Палитра",
                        color = NeoPalette.textPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    ThemePaletteToggle(
                        isDark = ThemeManager.isDarkTheme,
                        onSelect = { dark -> ThemeManager.setTheme(context, dark) }
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Поверхность",
                        color = NeoPalette.textPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    SurfaceStyleToggle(
                        current = ThemeConfig.surfaceStyle,
                        onSelect = { style -> ThemeConfig.surfaceStyle = style }
                    )
                }
            }

            item {
                InformationSectionCard(title = "Контакты и ссылки") {
                    InformationLinkButton(
                        text = "Открыть GitHub",
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/ValeriaBelyaeva/DOC_APP")
                            )
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Telegram: @irisus_r",
                        color = NeoPalette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Версия: 1.0.0",
                        color = NeoPalette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                InformationSectionCard(title = "Пинкод") {
                    if (!changing) {
                        InformationPrimaryButton(
                            text = "Сменить пинкод",
                            onClick = { changing = true }
                        )
                    } else {
                        InformationTextField(
                            value = oldPin,
                            onValueChange = { oldPin = it },
                            label = "Старый пинкод"
                        )
                        Spacer(Modifier.height(12.dp))
                        InformationTextField(
                            value = newPin,
                            onValueChange = { newPin = it },
                            label = "Новый пинкод (4 цифры)"
                        )
                        Spacer(Modifier.height(12.dp))
                        InformationTextField(
                            value = confirmPin,
                            onValueChange = { confirmPin = it },
                            label = "Повтори пинкод"
                        )
                        Spacer(Modifier.height(16.dp))
                        InformationPrimaryButton(
                            text = "Применить",
                            onClick = {
                                scope.launch {
                                    if (!isFourDigits(newPin)) {
                                        toast("Пинкод должен состоять из четырех цифр")
                                        return@launch
                                    }
                                    if (newPin != confirmPin) {
                                        toast("Подтверждение пинкода не совпадает")
                                        return@launch
                                    }
                                    if (oldPin == newPin) {
                                        toast("Новый пинкод не должен совпадать со старым")
                                        return@launch
                                    }
                                    val ok = useCases.verifyPin(oldPin)
                                    if (!ok) {
                                        toast("Старый пинкод неверный")
                                        return@launch
                                    }
                                    useCases.setNewPin(newPin)
                                    toast("Пинкод обновлен")
                                    oldPin = ""
                                    newPin = ""
                                    confirmPin = ""
                                    changing = false
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        InformationGhostButton(
                            text = "Отмена",
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
                InformationSectionCard(title = "Подсказки для пользователя") {
                    InformationHintBlock(
                        title = "Навигация",
                        hints = listOf(
                            "Свайпай между экранами, чтобы быстро найти нужный раздел.",
                            "Влево — экран «Дерево» с папками и документами.",
                            "Середина — «Главная» со списком закрепленных и последних документов.",
                            "Вправо — экран «Info», где лежат подсказки и ссылки."
                        )
                    )
                    Spacer(Modifier.height(18.dp))
                    InformationHintBlock(
                        title = "Главная",
                        hints = listOf(
                            "Кнопка «плюс» сверху создает документ без папки.",
                            "Закрепленные документы отмечаются звездой.",
                            "Долгий тап включает режим перестановки закрепленных; потом тап по другой карточке меняет их местами.",
                            "Меню на карточке помогает переместить документ в папку.",
                            "Раздел «Последние» сортируется по времени последнего открытия."
                        )
                    )
                    Spacer(Modifier.height(18.dp))
                    InformationHintBlock(
                        title = "Папки",
                        hints = listOf(
                            "Внутри папки лежит список ее документов и кнопка создания нового документа прямо внутри.",
                            "Раздел «Без папки» собирает все документы без привязки; их тоже можно перенести в нужную папку."
                        )
                    )
                    Spacer(Modifier.height(18.dp))
                    InformationHintBlock(
                        title = "Документ",
                        hints = listOf(
                            "Тапни по карточке, чтобы открыть документ.",
                            "Описание скрывается звездочками; иконка глаза временно показывает текст.",
                            "Иконка копии переносит значение поля в буфер обмена.",
                            "Карандаш в шапке переводит в режим редактирования.",
                            "Плюс рядом с названием поля добавляет новое поле.",
                            "Документы поддерживают вложения: фото и PDF.",
                            "Корзины позволяют удалять документы и отдельные поля."
                        )
                    )
                    Spacer(Modifier.height(18.dp))
                    InformationHintBlock(
                        title = "Шаблоны",
                        hints = listOf(
                            "При создании документа можно выбрать готовый шаблон или начать с пустого листа.",
                            "Свои шаблоны создаются на экране настройки шаблонов."
                        )
                    )
                    Spacer(Modifier.height(18.dp))
                    InformationHintBlock(
                        title = "Закрепления",
                        hints = listOf(
                            "Звезда закрепляет документ и поднимает его в верх списка.",
                            "Порядок закрепленных меняется в режиме перестановки через долгий тап."
                        )
                    )
                    Spacer(Modifier.height(18.dp))
                    InformationHintBlock(
                        title = "Безопасность",
                        hints = listOf(
                            "Сейчас выключить пинкод нельзя.",
                            "Расширенные настройки и скрытие содержимого появятся в следующих версиях."
                        )
                    )
                }
            }
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
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                color = NeoPalette.textPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
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
        shape = NeoShapes.row
    ) {
        Text(text = text.uppercase())
    }
}

@Composable
private fun InformationGhostButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, NeoPalette.neon),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeoPalette.neon),
        shape = NeoShapes.row
    ) {
        Text(text = text.uppercase())
    }
}

@Composable
private fun InformationLinkButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, NeoPalette.neon.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeoPalette.neon),
        shape = NeoShapes.row
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
    Spacer(Modifier.height(10.dp))
    hints.forEachIndexed { index, hint ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "•",
                color = NeoPalette.neon,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = hint,
                color = NeoPalette.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
        if (index != hints.lastIndex) {
            Spacer(Modifier.height(8.dp))
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
            title = "Темная",
            isActive = isDark,
            onClick = { onSelect(true) },
            modifier = Modifier.weight(1f)
        )
        ThemePaletteOptionButton(
            title = "Светлая",
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
        containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
            title = "Стекло",
            target = SurfaceStyle.Glass,
            current = current,
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
        SurfaceStyleOptionButton(
            title = "Матовый",
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
        containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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

/* ===== Диалог удаления папки ===== */
@Composable
private fun DeleteFolderDialogIfNeeded(deleteFolderId: String?, onClose: () -> Unit) {
    if (deleteFolderId == null) return
    
    val uc: UseCases = ServiceLocator.useCases
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var documentsInFolder by remember { mutableStateOf<List<Document>>(emptyList()) }
    var deleteDocuments by remember { mutableStateOf(false) }
    
    LaunchedEffect(deleteFolderId) {
        documentsInFolder = uc.getDocumentsInFolder(deleteFolderId)
    }
    
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()
    
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Удалить папку") },
        text = {
            Column {
                Text("Папка содержит ${documentsInFolder.size} документов.")
                VSpace(AppDimens.spaceSm)
                Text("Что делать с документами?")
                VSpace(AppDimens.spaceSm)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !deleteDocuments,
                        onClick = { deleteDocuments = false }
                    )
                    Text("Переместить в \"Без папки\"", modifier = Modifier.padding(start = AppDimens.spaceSm))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = deleteDocuments,
                        onClick = { deleteDocuments = true }
                    )
                    Text("Удалить вместе с папкой", modifier = Modifier.padding(start = AppDimens.spaceSm))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        try {
                            uc.deleteFolder(deleteFolderId, deleteDocuments)
                            toast(if (deleteDocuments) "Папка и документы удалены" else "Папка удалена, документы перемещены")
                            onClose()
                        } catch (e: Exception) {
                            toast("Ошибка: ${e.message}")
                        }
                    }
                }
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Отмена") }
        }
    )
}
