package com.example.docapp.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.docapp.core.ServiceLocator
import com.example.docapp.domain.Document
import com.example.docapp.domain.DocumentRepository
import com.example.docapp.domain.Folder
import com.example.docapp.domain.usecases.UseCases
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll



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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
    ) {
        IconButton(onClick = onCreate, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.Add, contentDescription = "Создать")
        }
        Spacer(Modifier.height(12.dp))

        Text("ЗАКРЕПЛЕННЫЕ", style = MaterialTheme.typography.titleSmall)
        if (reorderMode) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Режим перестановки: тапни по другому закреплённому для обмена местами",
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { reorderMode = false; firstSelected = null }) { Text("Готово") }
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, true)) {
            // --- Pinned ---
            items(home.pinned) { doc ->
                var menuOpen by remember { mutableStateOf(false) }
                val isSelected = reorderMode && firstSelected == doc.id

                ElevatedCard(
                    onClick = {
                        if (!reorderMode) openDoc(doc.id)
                        else {
                            // Вторая точка обмена
                            val a = firstSelected
                            if (a == null) firstSelected = doc.id
                            else if (a != doc.id) {
                                scope.launch {
                                    try {
                                        uc.swapPinned(a, doc.id)
                                        toast("Поменяли местами")
                                        firstSelected = doc.id // остаёмся в режиме перестановки
                                    } catch (e: Exception) {
                                        toast("Не удалось переставить")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {}, // используем onClick в ElevatedCard
                            onLongClick = {
                                // Вход в режим перестановки
                                reorderMode = true
                                firstSelected = doc.id
                            }
                        )
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isSelected) "▶ ${doc.name}" else doc.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { scope.launch { uc.pinDoc(doc.id, false) } }) {
                            Icon(Icons.Default.Star, contentDescription = "Unpin")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Переместить в папку") },
                                    onClick = { menuOpen = false; moveDocId = doc.id }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item { Text("ПОСЛЕДНИЕ", style = MaterialTheme.typography.titleSmall) }

            // --- Recent ---
            items(home.recent) { doc ->
                var menuOpen by remember { mutableStateOf(false) }
                ElevatedCard(onClick = { openDoc(doc.id) }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(doc.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { scope.launch { uc.pinDoc(doc.id, true) } }) {
                            Icon(Icons.Outlined.StarOutline, contentDescription = "Pin")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Переместить в папку") },
                                    onClick = { menuOpen = false; moveDocId = doc.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    MoveToFolderDialogIfNeeded(moveDocId = moveDocId, onClose = { moveDocId = null })
}

/* ===== Дерево (папки + “без папки” в ОДНОЙ прокрутке) + перемещение в папку ===== */

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

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            items(folders) { folder ->
                val docs = docsByFolderId[folder.id].orEmpty()
                FolderBlock(folder = folder, docs = docs, openDoc = openDoc, createInFolder = createInFolder)
            }

            if (docsNoFolder.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)) }
                item { Text("БЕЗ ПАПКИ", style = MaterialTheme.typography.titleSmall) }
                items(docsNoFolder) { doc ->
                    var menuOpen by remember { mutableStateOf(false) }
                    ElevatedCard(onClick = { openDoc(doc.id) }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(doc.name, Modifier.weight(1f))
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Переместить в папку") },
                                        onClick = { menuOpen = false; moveDocId = doc.id }
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(56.dp)) } // под FAB
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showNewFolderDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Создать папку") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        if (showNewFolderDialog) {
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                title = { Text("Новая папка") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Название папки") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = newFolderName.trim()
                        if (name.isNotEmpty()) {
                            scope.launch {
                                ServiceLocator.repos.folders.addFolder(name, null)
                                newFolderName = ""
                                showNewFolderDialog = false
                            }
                        }
                    }) { Text("Создать") }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) { Text("Отмена") }
                }
            )
        }
    }

    MoveToFolderDialogIfNeeded(moveDocId = moveDocId, onClose = { moveDocId = null })
}

@Composable
private fun FolderBlock(
    folder: Folder,
    docs: List<Document>,
    openDoc: (String) -> Unit,
    createInFolder: (String?) -> Unit
) {
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { createInFolder(folder.id) }) { Text("+ Документ") }
            }
            docs.forEach { doc ->
                TextButton(onClick = { openDoc(doc.id) }, modifier = Modifier.padding(start = 8.dp)) {
                    Text("• ${doc.name}")
                }
            }
        }
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
                Spacer(Modifier.height(8.dp))
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
    val scroll = rememberScrollState()   // ← создаём состояние скролла здесь
    val ctx = LocalContext.current
    val uc = ServiceLocator.useCases
    var changing by remember { mutableStateOf(false) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var newPin2 by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    fun isFourDigits(s: String) = s.matches(Regex("^\\d{4}$"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("INFO & SETTINGS", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ValeriaBelyaeva/DOC_APP"))
            ctx.startActivity(intent)
        }) { Text("ОТКРЫТЬ GITHUB") }
        Text("TG: @irisus_r")
        Text("version: 1.0.0")
        Spacer(Modifier.height(16.dp))

        if (!changing) {
            Button(onClick = { changing = true }) { Text("СМЕНИТЬ ПИНКОД") }
        } else {
            OutlinedTextField(value = oldPin, onValueChange = { oldPin = it }, label = { Text("Старый PIN") })
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = newPin, onValueChange = { newPin = it }, label = { Text("Новый PIN (4 цифры)") })
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = newPin2, onValueChange = { newPin2 = it }, label = { Text("Повторите PIN") })
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    if (!isFourDigits(newPin)) { toast("PIN должен быть ровно 4 цифры"); return@launch }
                    if (newPin != newPin2)     { toast("Подтверждение PIN не совпало"); return@launch }
                    if (oldPin == newPin)      { toast("Новый PIN не должен совпадать со старым"); return@launch }
                    val ok = uc.verifyPin(oldPin)
                    if (!ok) { toast("Старый PIN неверен"); return@launch }
                    uc.setNewPin(newPin)
                    toast("PIN обновлён")
                    oldPin = ""; newPin = ""; newPin2 = ""; changing = false
                }
            }) { Text("ПРИМЕНИТЬ") }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { oldPin = ""; newPin = ""; newPin2 = ""; changing = false }) { Text("Отмена") }
        }

        Spacer(Modifier.height(24.dp))


        Spacer(Modifier.height(16.dp))
        Text("Подсказки для пользователя", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(scroll)
        ){
        // Навигация
        Text("• Перелистывай экраны свайпом по горизонтали:")
        Text("    — ВЛЕВО: «Дерево» — папки + документы;")
        Text("    — СЕРЕДИНА: «Главная» — закреплённые и последние документы;")
        Text("    — ВПРАВО: «Info» — эта справка, ссылки, версия.")
        Spacer(Modifier.height(8.dp))

        // Главная
        Text("Главная")
        Text("• Кнопка «плюс» сверху по центру — создать документ без папки.")
        Text("• Блок «ЗАКРЕПЛЕННЫЕ»: документы, закреплённые звездой.")
        Text("• Долгий тап по закреплённому — режим перестановки; тап по другому — обмен местами.")
        Text("• Три точки на карточке — «Переместить в папку».")
        Text("• Блок «ПОСЛЕДНИЕ»: сортируются по времени последнего открытия (новые сверху).")
        Spacer(Modifier.height(8.dp))

        // Дерево/папки
        Text("Папки")
        Text("• Внутри каждой папки: список её документов и кнопка «+ Документ» для создания прямо в папке.")
        Text("• Раздел «БЕЗ ПАПКИ» внизу — документы без папки, их тоже можно переместить в папку через меню.")
        Spacer(Modifier.height(8.dp))

        // Документ
        Text("Документ")
        Text("• Открыть документ — тап по карточке/строке.")
        Text("• В режиме просмотра видно название поля и первые символы данных серым.")
        Text("• Иконка «глаз» у поля — временно показать полные данные (название скрывается).")
        Text("• Иконка «копия» — копировать ЗНАЧЕНИЕ поля в буфер.")
        Text("• Иконка «карандаш» в шапке — перейти в редактирование: менять имя документа, добавлять/удалять поля, вложения.")
        Text("• «Плюс» возле ввода названия поля — добавить поле; позиционирован по центру по вертикали.")
        Text("• Поддерживаются вложения: несколько фото из галереи + несколько PDF.")
        Text("• Есть удаление: корзина у документа (с подтверждением) и у каждого поля (с подтверждением).")
        Spacer(Modifier.height(8.dp))

        // Шаблоны
        Text("Шаблоны")
        Text("• При создании документа можно выбрать шаблон (предзаданные имена полей) или начать с нуля.")
        Text("• Свои шаблоны — через экран создания шаблона ")
        Spacer(Modifier.height(8.dp))

        // Закрепления
        Text("Закрепления")
        Text("• Звезда — закрепить/открепить. Закреплённые всегда наверху на «Главной».")
        Text("• Порядок закреплённых регулируется долгим тапом и обменом мест.")
        Spacer(Modifier.height(8.dp))

        // Безопасность (кнопка скрыта)
        Text("Безопасность (PIN)")
        Text("• На текущей версии управление PIN временно скрыто. Вход без ввода кода пока не возможен.")
        Text("• Функция выключения PIN будет в следующих версиях, как и формат скрытого контента для паролей.")
        Spacer(Modifier.height(12.dp))
}
    }
}
