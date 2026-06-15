package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.CodeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E1E)
                ) {
                    VSCodeAppContainer()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VSCodeAppContainer(viewModel: CodeViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Database states
    val files by viewModel.allFiles.collectAsState()
    val extensions by viewModel.extensions.collectAsState()
    val gitRepos by viewModel.gitRepos.collectAsState()

    // View state mappings
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabPath by viewModel.activeTabPath.collectAsState()
    val activeContent by viewModel.activeFileContent.collectAsState()
    val highlightedContent by viewModel.highlightedContent.collectAsState()
    val isFileModified by viewModel.isFileModified.collectAsState()

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val isFindPanelOpen by viewModel.isFindPanelOpen.collectAsState()
    val findQuery by viewModel.findQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val searchMatches by viewModel.searchMatches.collectAsState()
    val activeMatchIndex by viewModel.activeMatchIndex.collectAsState()
    val matchCase by viewModel.matchCase.collectAsState()
    val wholeWord by viewModel.wholeWord.collectAsState()
    val autoSaveStatus by viewModel.autoSaveStatus.collectAsState()

    // Layout configuration
    val isSidebarOpen by viewModel.isSidebarOpen.collectAsState()
    val activeSidebar by viewModel.activeSidebar.collectAsState()
    val isTerminalOpen by viewModel.isTerminalOpen.collectAsState()
    val activeTheme by viewModel.activeTheme.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val gitBranch by viewModel.repoBranchInput.collectAsState()
    val isSetupComplete by viewModel.isSetupComplete.collectAsState()

    // Dialog state controllers
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var createTargetFolderName by remember { mutableStateOf("") }
    var newItemNameInput by remember { mutableStateOf("") }

    var showAddSnippetDialog by remember { mutableStateOf(false) }
    var isSnippetLanguageFilter by remember { mutableStateOf("javascript") }
    var newSnippetLabel by remember { mutableStateOf("") }
    var newSnippetPrefix by remember { mutableStateOf("") }
    var newSnippetBody by remember { mutableStateOf("") }
    var newSnippetDesc by remember { mutableStateOf("") }

    var showGitInitDialog by remember { mutableStateOf(false) }

    // Floating action alert triggers
    val message by viewModel.statusMessage.collectAsState()
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    if (!isSetupComplete) {
        VSCodeEnvironmentSetupScreen(viewModel = viewModel)
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = activeTheme.editorBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. TOP TITLE AND CONTROL ACCENTS BAR
            VSCodeTopMenuBar(
                activeTabPath = activeTabPath,
                isFileModified = isFileModified,
                theme = activeTheme,
                canUndo = canUndo,
                canRedo = canRedo,
                onSaveClick = { viewModel.saveActiveFile() },
                onFormatClick = { viewModel.autoFormatCode() },
                onRunClick = {
                    activeTabPath?.let {
                        viewModel.executeTerminalCommand("run $it")
                    } ?: viewModel.showMessage("Buka file dahulu untuk meluncurkan program!")
                },
                onAICopilotClick = { viewModel.askAICopilot("AUTOCOMPLETE") },
                onAICopilotExplain = { viewModel.askAICopilot("EXPLAIN") },
                onToggleSidebar = { viewModel.toggleSidebar() },
                onToggleTerminal = { viewModel.toggleTerminal() },
                onUndoClick = { viewModel.undoEdit() },
                onRedoClick = { viewModel.redoEdit() },
                onSearchClick = { viewModel.openFindPanel() }
            )

            // 2. MAIN LAYOUT AREA (SIDEBAR + EDITOR + TERMINAL)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Left Ribbon Navigation & Expanded Sidebar View
                AnimatedVisibility(
                    visible = isSidebarOpen,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(220.dp)
                            .background(activeTheme.sidebarBg)
                    ) {
                        // Ribbon column (left actions bar)
                        VSCodeActivityBarRibbon(
                            activeSidebar = activeSidebar,
                            theme = activeTheme,
                            onSidebarSelect = { viewModel.chooseSidebar(it) }
                        )

                        VerticalDivider(color = Color(0xFF30363D), thickness = 1.dp)

                        // Target sidebar panel details
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = activeSidebar.name,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.2.sp
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            when (activeSidebar) {
                                CodeViewModel.SidebarCategory.EXPLORER -> {
                                    VSCodeExplorerPanel(
                                        files = files,
                                        activePath = activeTabPath,
                                        theme = activeTheme,
                                        onFileSelect = {
                                            viewModel.openFile(it)
                                            viewModel.setSidebarOpen(false)
                                        },
                                        onCreateFileClick = {
                                            createTargetFolderName = ""
                                            showCreateFileDialog = true
                                        },
                                        onCreateFolderClick = {
                                            createTargetFolderName = ""
                                            showCreateFolderDialog = true
                                        },
                                        onSubFolderCreateFile = { folder ->
                                            createTargetFolderName = folder
                                            showCreateFileDialog = true
                                        },
                                        onDeletePath = { viewModel.deleteFileFromUI(it) }
                                    )
                                }
                                CodeViewModel.SidebarCategory.SEARCH -> {
                                    VSCodeSearchPanel(
                                        searchQuery = searchQuery,
                                        searchResults = searchResults,
                                        theme = activeTheme,
                                        onQueryChange = { viewModel.searchFiles(it) },
                                        onFileClick = {
                                            viewModel.openFile(it)
                                            viewModel.setSidebarOpen(false)
                                        }
                                    )
                                }
                                CodeViewModel.SidebarCategory.GIT -> {
                                    VSCodeGitPanel(
                                        gitRepos = gitRepos,
                                        theme = activeTheme,
                                        viewModel = viewModel,
                                        onInitGitRepo = { showGitInitDialog = true }
                                    )
                                }
                                CodeViewModel.SidebarCategory.SNIPPETS -> {
                                    VSCodeSnippetsPanel(
                                        snippets = viewModel.activeSnippets.collectAsState().value,
                                        theme = activeTheme,
                                        onAddSnippet = { showAddSnippetDialog = true },
                                        onInsertSnippet = { viewModel.insertSnippetIntoEditor(it) },
                                        onDeleteSnippet = { viewModel.deleteSnippet(it) }
                                    )
                                }
                                CodeViewModel.SidebarCategory.EXTENSIONS -> {
                                    VSCodeExtensionsPanel(
                                        extensions = extensions,
                                        theme = activeTheme,
                                        onToggleState = { id, state -> viewModel.toggleExtension(id, state) }
                                    )
                                }
                                CodeViewModel.SidebarCategory.SETTINGS -> {
                                    VSCodeSettingsPanel(
                                        activeTheme = activeTheme,
                                        theme = activeTheme,
                                        onThemeChange = { viewModel.changeTheme(it) }
                                    )
                                }
                            }
                        }
                    }
                }

                // If sidebar closed, still show miniature Activity Ribbon, for convenient split navigations!
                if (!isSidebarOpen) {
                    VSCodeActivityBarRibbon(
                        activeSidebar = activeSidebar,
                        theme = activeTheme,
                        onSidebarSelect = {
                            viewModel.chooseSidebar(it)
                        }
                    )
                }

                VerticalDivider(color = Color(0xFF30363D), thickness = 1.dp)

                // MIDDLE AND BOTTOM EDITOR AND TERMINAL SECTION
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(activeTheme.editorBg)
                ) {
                    // Open Tabs row
                    VSCodeFileTabsRow(
                        openTabs = openTabs,
                        activeTabPath = activeTabPath,
                        theme = activeTheme,
                        onTabSelect = { viewModel.openFile(it) },
                        onTabClose = { viewModel.closeTab(it) }
                    )

                    // Code Editor Block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (activeTabPath != null) {
                            VSCodeCodeEditorArea(
                                content = activeContent,
                                language = activeTabPath!!.substringAfterLast(".", "text"),
                                theme = activeTheme,
                                highlightedContent = highlightedContent,
                                onContentChange = { viewModel.modifyActiveContent(it) }
                            )

                            if (isFindPanelOpen) {
                                VSCodeFindReplacePanel(
                                    findQuery = findQuery,
                                    replaceQuery = replaceQuery,
                                    searchMatches = searchMatches,
                                    activeMatchIndex = activeMatchIndex,
                                    matchCase = matchCase,
                                    wholeWord = wholeWord,
                                    theme = activeTheme,
                                    onFindQueryChange = { viewModel.updateFindQuery(it) },
                                    onReplaceQueryChange = { viewModel.updateReplaceQuery(it) },
                                    onFindNext = { viewModel.findNext() },
                                    onFindPrevious = { viewModel.findPrevious() },
                                    onReplace = { viewModel.replaceCurrent() },
                                    onReplaceAll = { viewModel.replaceAll() },
                                    onToggleMatchCase = { viewModel.toggleMatchCase() },
                                    onToggleWholeWord = { viewModel.toggleWholeWord() },
                                    onClose = { viewModel.closeFindPanel() },
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        } else {
                            // High Quality Space Blueprint Empty State
                            VSCodeEmptyWorkspaceState(theme = activeTheme)
                        }
                    }

                    // Sourcat shortcut paste helper bar always visible above terminal
                    VSCodeShortcutHelperBar(
                        theme = activeTheme,
                        activeTabPath = activeTabPath,
                        onInsertText = { viewModel.insertSnippetIntoEditor(it) },
                        onRun = {
                            activeTabPath?.let {
                                viewModel.executeTerminalCommand("run $it")
                            } ?: viewModel.showMessage("Buka berkas untuk eksekusi.")
                        },
                        onFormat = { viewModel.autoFormatCode() },
                        onSendAI = { viewModel.askAICopilot("AUTOCOMPLETE") },
                        onUndo = { viewModel.undoEdit() },
                        onRedo = { viewModel.redoEdit() }
                    )

                    // Splitted Bottom Terminal Panel
                    AnimatedVisibility(
                        visible = isTerminalOpen,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color.Black)
                        ) {
                            VSCodeTermuxTerminal(
                                terminalLines = viewModel.terminalLines.collectAsState().value,
                                terminalInput = viewModel.terminalInput.collectAsState().value,
                                onValueChange = { viewModel.updateTerminalInput(it) },
                                theme = activeTheme,
                                onEnterCommand = { viewModel.executeTerminalCommand(it) }
                            )
                        }
                    }
                }
            }
            VSCodeStatusBar(
                branch = gitBranch,
                theme = activeTheme,
                onSyncClick = { viewModel.executeTerminalCommand("git pull") },
                autoSaveStatus = autoSaveStatus
            )
        }
    }

    // dialog boxes definitions for custom entries
    if (showCreateFileDialog) {
        VSCodeCreateItemDialog(
            title = "Buat Berkas Baru",
            isFolder = false,
            parentFolder = createTargetFolderName,
            onDismiss = { showCreateFileDialog = false },
            onConfirm = { name ->
                viewModel.createNewFile(name, createTargetFolderName)
                showCreateFileDialog = false
            }
        )
    }

    if (showCreateFolderDialog) {
        VSCodeCreateItemDialog(
            title = "Buat Folder Baru",
            isFolder = true,
            parentFolder = createTargetFolderName,
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                viewModel.createNewFolder(name, createTargetFolderName)
                showCreateFolderDialog = false
            }
        )
    }

    if (showAddSnippetDialog) {
        AlertDialog(
            onDismissRequest = { showAddSnippetDialog = false },
            containerColor = activeTheme.sidebarBg,
            title = { Text("Pintasan Kode Baru (Sourcat)", color = activeTheme.textColor, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = newSnippetLabel,
                        onValueChange = { newSnippetLabel = it },
                        label = { Text("Label") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeTheme.accentColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = newSnippetPrefix,
                        onValueChange = { newSnippetPrefix = it },
                        label = { Text("Prefix / Trigger Kata Sandi") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeTheme.accentColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = newSnippetDesc,
                        onValueChange = { newSnippetDesc = it },
                        label = { Text("Deskripsi") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeTheme.accentColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    Text("Pilih Bahasa Pemrograman:", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("javascript", "python", "kotlin", "html", "all").forEach { lang ->
                            val isSelected = isSnippetLanguageFilter == lang
                            Button(
                                onClick = { isSnippetLanguageFilter = lang },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) activeTheme.accentColor else Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(lang, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newSnippetBody,
                        onValueChange = { newSnippetBody = it },
                        label = { Text("Body Kode Pintasan") },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeTheme.accentColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSnippetLabel.isNotEmpty() && newSnippetBody.isNotEmpty()) {
                            viewModel.addCustomSnippet(
                                newSnippetLabel,
                                newSnippetPrefix,
                                newSnippetBody,
                                newSnippetDesc,
                                isSnippetLanguageFilter
                            )
                            showAddSnippetDialog = false
                            newSnippetLabel = ""
                            newSnippetPrefix = ""
                            newSnippetBody = ""
                            newSnippetDesc = ""
                        }
                    }
                ) {
                    Text("Simpan", color = activeTheme.accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSnippetDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    if (showGitInitDialog) {
        AlertDialog(
            onDismissRequest = { showGitInitDialog = false },
            containerColor = activeTheme.sidebarBg,
            title = { Text("Hubungkan Repositori Git Cloud", color = activeTheme.textColor, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = viewModel.repoNameInput.collectAsState().value,
                        onValueChange = { viewModel.updateRepoName(it) },
                        label = { Text("Nama Repositori Lokal") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeTheme.accentColor, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = viewModel.repoUrlInput.collectAsState().value,
                        onValueChange = { viewModel.updateRepoUrl(it) },
                        label = { Text("URL Remote Git (HTTPS)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeTheme.accentColor, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = viewModel.repoBranchInput.collectAsState().value,
                        onValueChange = { viewModel.updateRepoBranch(it) },
                        label = { Text("Branch Default") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeTheme.accentColor, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = viewModel.gitUsernameInput.collectAsState().value,
                        onValueChange = { viewModel.updateGitUsername(it) },
                        label = { Text("Username GitHub (Opsional)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeTheme.accentColor, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = viewModel.gitTokenInput.collectAsState().value,
                        onValueChange = { viewModel.updateGitToken(it) },
                        label = { Text("Token Akses Pribadi / Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = activeTheme.accentColor, focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nameState = viewModel.repoNameInput.value
                        val urlState = viewModel.repoUrlInput.value
                        if (nameState.isNotEmpty() && urlState.isNotEmpty()) {
                            viewModel.insertGitRepoConfig(
                                nameState,
                                urlState,
                                viewModel.repoBranchInput.value,
                                viewModel.gitUsernameInput.value,
                                viewModel.gitTokenInput.value
                            )
                            showGitInitDialog = false
                        }
                    }
                ) {
                    Text("Sinkronkan Baru", color = activeTheme.accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGitInitDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}

// --- 1. TOP MENU CONTROL BAR ---
@Composable
fun VSCodeTopMenuBar(
    activeTabPath: String?,
    isFileModified: Boolean,
    theme: EditorTheme,
    canUndo: Boolean,
    canRedo: Boolean,
    onSaveClick: () -> Unit,
    onFormatClick: () -> Unit,
    onRunClick: () -> Unit,
    onAICopilotClick: () -> Unit,
    onAICopilotExplain: () -> Unit,
    onToggleSidebar: () -> Unit,
    onToggleTerminal: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(theme.sidebarBg)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleSidebar) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Toggle Panel Kiri",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF2188FF), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "VS Code Logo Accent",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Document Title Path status indicator
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (activeTabPath.isNullOrEmpty()) "Tidak ada file terbuka" else activeTabPath,
                    fontSize = 13.sp,
                    color = if (isFileModified) theme.accentColor else Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    fontFamily = FontFamily.Monospace
                )
                if (isFileModified) {
                    Text(
                        text = "Diubah - Klik Simpan",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            // Action controls
            if (activeTabPath != null) {
                IconButton(onClick = onUndoClick, enabled = canUndo) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) theme.accentColor else Color.Gray
                    )
                }
                IconButton(onClick = onRedoClick, enabled = canRedo) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) theme.accentColor else Color.Gray
                    )
                }
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari Teks",
                        tint = Color.LightGray
                    )
                }
                IconButton(onClick = onSaveClick) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Simpan File",
                        tint = if (isFileModified) theme.accentColor else Color.LightGray
                    )
                }
                IconButton(onClick = onFormatClick) {
                    Icon(
                        imageVector = Icons.Default.FormatAlignLeft,
                        contentDescription = "Auto Format Kode",
                        tint = Color.LightGray
                    )
                }
                IconButton(onClick = onAICopilotClick) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Copilot Auto-Complete",
                        tint = theme.accentColor
                    )
                }
                IconButton(onClick = onAICopilotExplain) {
                    Icon(
                        imageVector = Icons.Default.HelpCenter,
                        contentDescription = "AI Explain Code",
                        tint = Color(0xFF00FFCC)
                    )
                }
                IconButton(onClick = onRunClick) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Jalankan Program",
                        tint = Color(0xFF79C0FF)
                    )
                }
            }

            IconButton(onClick = onToggleTerminal) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Toggle Terminal",
                    tint = Color.White
                )
            }
        }
        HorizontalDivider(color = Color(0xFF30363D), thickness = 1.dp)
    }
}

// --- 2. ACTIVITY BAR (LEFT PANEL RIBBON SELECTION) ---
@Composable
fun VSCodeActivityBarRibbon(
    activeSidebar: CodeViewModel.SidebarCategory,
    theme: EditorTheme,
    onSidebarSelect: (CodeViewModel.SidebarCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(50.dp)
            .background(theme.sidebarBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val navItems = listOf(
            CodeViewModel.SidebarCategory.EXPLORER to Icons.Default.Folder,
            CodeViewModel.SidebarCategory.SEARCH to Icons.Default.Search,
            CodeViewModel.SidebarCategory.GIT to Icons.Default.Layers,
            CodeViewModel.SidebarCategory.SNIPPETS to Icons.Default.ShortText,
            CodeViewModel.SidebarCategory.EXTENSIONS to Icons.Outlined.Extension,
            CodeViewModel.SidebarCategory.SETTINGS to Icons.Default.Settings
        )

        navItems.forEach { (cat, icon) ->
            val isSelected = activeSidebar == cat
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { onSidebarSelect(cat) }
                    .testTag("ribbon_${cat.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(2.5.dp)
                            .fillMaxHeight(0.5f)
                            .background(theme.accentColor)
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = cat.name,
                    tint = if (isSelected) theme.accentColor else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// --- 3. EXPLORER SIDE PANEL TREE ---
@Composable
fun VSCodeExplorerPanel(
    files: List<CodeFile>,
    activePath: String?,
    theme: EditorTheme,
    onFileSelect: (String) -> Unit,
    onCreateFileClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onSubFolderCreateFile: (String) -> Unit,
    onDeletePath: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("WORKPLACE FILES", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
            Row {
                IconButton(onClick = onCreateFileClick, modifier = Modifier.size(24.dp).testTag("create_file_btn")) {
                    Icon(Icons.Default.NoteAdd, "File Baru", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onCreateFolderClick, modifier = Modifier.size(24.dp).testTag("create_folder_btn")) {
                    Icon(Icons.Default.CreateNewFolder, "Folder Baru", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }

        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 4.dp)
        ) {
            // Group Folders first, then Files
            val nestedFolders = files.filter { it.isFolder }.sortedBy { it.path }
            val rootFiles = files.filter { !it.isFolder }.sortedBy { it.path }

            items(nestedFolders) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onFileSelect(folder.path) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = "Folder", tint = Color(0xFFFFCA28), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(folder.name, color = Color.White, fontSize = 13.sp)
                    }
                    Row {
                        IconButton(onClick = { onSubFolderCreateFile(folder.path) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.NoteAdd, "Dalam Folder File", tint = theme.accentColor, modifier = Modifier.size(12.dp))
                        }
                        IconButton(onClick = { onDeletePath(folder.path) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Delete, "Hapus Folder", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            items(rootFiles) { file ->
                val isSelected = activePath == file.path
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) theme.accentColor.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { onFileSelect(file.path) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val fileIcon = getFileIcon(file.language)
                        Icon(
                            imageVector = fileIcon,
                            contentDescription = "File Type",
                            tint = getFileIconColor(file.language),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = file.name,
                            color = if (isSelected) theme.accentColor else Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    IconButton(onClick = { onDeletePath(file.path) }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Delete, "Hapus File", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

// --- 4. SEARCH SIDE PANEL ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VSCodeSearchPanel(
    searchQuery: String,
    searchResults: List<CodeFile>,
    theme: EditorTheme,
    onQueryChange: (String) -> Unit,
    onFileClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Cari kata kunci...", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.accentColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
            textStyle = TextStyle(fontSize = 13.sp),
            singleLine = true
        )

        Text("HASIL PENCARIAN (${searchResults.size})", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
            items(searchResults) { file ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onFileClick(file.path) }
                        .padding(8.dp)
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                ) {
                    Text(file.path, fontWeight = FontWeight.Bold, color = theme.accentColor, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(file.content.lineSequence().find { it.contains(searchQuery, ignoreCase = true) }?.trim() ?: "Matched line", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// --- 5. VISUAL INTEGRATED GIT SOURCE CONTROL ---
@Composable
fun VSCodeGitPanel(
    gitRepos: List<GitRepo>,
    theme: EditorTheme,
    viewModel: CodeViewModel,
    onInitGitRepo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val currRepo = gitRepos.firstOrNull()

        if (currRepo == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray.copy(alpha = 0.35f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudQueue, "Cloud Git", tint = theme.accentColor, modifier = Modifier.size(40.dp))
                    Text("Repositori Git belum terhubung", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onInitGitRepo,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                    ) {
                        Text("Hubungkan Git", color = Color.White)
                    }
                }
            }
        } else {
            // Live details
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ACTIVE COUPLING", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Text("Repository: ${currRepo.repoName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Text("Remote URL: ${currRepo.remoteUrl}", fontSize = 12.sp, color = Color.LightGray)
                    Text("Branch: ${currRepo.branchName}", fontSize = 12.sp, color = theme.accentColor)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.deleteGitRepoConfig(currRepo) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Putuskan Sinkronisasi", color = Color.White)
                    }
                }
            }

            Text("TINDAKAN GIT CLOUD SINKRONISASI", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.executeGitAction("CLONE") },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pull & Checkout (Clone)", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { viewModel.executeGitAction("COMMIT") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add & Commit", fontSize = 11.sp, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.executeGitAction("PUSH") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Push Origin", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { viewModel.executeGitAction("PULL") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pull Origin", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// --- 6. SNIPPETS CONTAINER PANEL ---
@Composable
fun VSCodeSnippetsPanel(
    snippets: List<Snippet>,
    theme: EditorTheme,
    onAddSnippet: () -> Unit,
    onInsertSnippet: (String) -> Unit,
    onDeleteSnippet: (Snippet) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PINTASAN SOURCAT (" + snippets.size + ")", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
            IconButton(onClick = onAddSnippet, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, "Tambah Pintasan", tint = theme.accentColor, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(snippets) { snippet ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(snippet.label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text("Prefix: " + snippet.prefix, fontSize = 11.sp, color = theme.accentColor)
                            }
                            IconButton(onClick = { onDeleteSnippet(snippet) }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Delete, "Hapus Pintasan", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                            }
                        }
                        Text(snippet.description, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                        Button(
                            onClick = { onInsertSnippet(snippet.body) },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Tempel Kode", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- 7. EXTENSIONS CONFIGURATOR MARKET ---
@Composable
fun VSCodeExtensionsPanel(
    extensions: List<ExtensionItem>,
    theme: EditorTheme,
    onToggleState: (String, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("INSTALLED EXTENSIONS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
        Text("Kelola fungsionalitas cerdas dan visual editor", fontSize = 10.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(extensions) { ext ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ext.isEnabled) Color.DarkGray.copy(alpha = 0.25f) else Color.DarkGray.copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val extIcon = when (ext.iconName) {
                                    "ai_stars" -> Icons.Default.AutoAwesome
                                    "format_align_left" -> Icons.Default.FormatAlignLeft
                                    "terminal" -> Icons.Default.Terminal
                                    "sync" -> Icons.Default.Sync
                                    else -> Icons.Default.Palette
                                }
                                Icon(
                                    imageVector = extIcon,
                                    contentDescription = ext.name,
                                    tint = if (ext.isEnabled) theme.accentColor else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = ext.name,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ext.isEnabled) Color.White else Color.Gray,
                                        fontSize = 13.sp
                                    )
                                    Text("v" + ext.version + " - oleh " + ext.publisher, fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            Switch(
                                checked = ext.isEnabled,
                                onCheckedChange = { onToggleState(ext.id, ext.isEnabled) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.accentColor,
                                    checkedTrackColor = theme.accentColor.copy(alpha = 0.44f)
                                )
                            )
                        }
                        Text(
                            text = ext.description,
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- 8. SETTINGS & CUSTOM THEMES PANEL ---
@Composable
fun VSCodeSettingsPanel(
    activeTheme: EditorTheme,
    theme: EditorTheme,
    onThemeChange: (EditorTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("PREFERENCES WORKSPACE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Text("PILIH TEMA VISUAL:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        EditorTheme.ALL_THEMES.forEach { tm ->
            val isSelected = activeTheme.name == tm.name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) theme.accentColor.copy(alpha = 0.25f) else Color.DarkGray.copy(alpha = 0.15f))
                    .clickable { onThemeChange(tm) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(tm.editorBg)
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(tm.name, color = Color.White, fontSize = 13.sp)
                }
                if (isSelected) {
                    Icon(Icons.Default.Check, "Selected", tint = theme.accentColor, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("DOKUMENTASI LINGKUNGAN", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 11.sp)
        Text(
            text = "Droid VS Code Android Edition menggunakan sandboxed SQLite penyimpanan untuk mengelola file sistem. Anda dapat memicu fungsionalitas Git push dan clone yang tersinkronisasi server di Cloud.",
            color = Color.LightGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// --- 9. OPEN FILES TABS RIBBON BAR ---
@Composable
fun VSCodeFileTabsRow(
    openTabs: List<String>,
    activeTabPath: String?,
    theme: EditorTheme,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(theme.sidebarBg)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        openTabs.forEach { tabPath ->
            val isActive = activeTabPath == tabPath
            val ext = tabPath.substringAfterLast(".", "")

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(if (isActive) theme.editorBg else theme.sidebarBg.copy(alpha = 0.5f))
                    .clickable { onTabSelect(tabPath) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Active top-accent border line
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color(0xFFF78166))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getFileIcon(ext),
                        contentDescription = "Tab Type",
                        tint = getFileIconColor(ext).copy(alpha = if (isActive) 1f else 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tabPath.substringAfterLast("/", tabPath),
                        fontSize = 11.sp,
                        color = if (isActive) theme.textColor else theme.textColor.copy(alpha = 0.5f),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onTabClose(tabPath) },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tab",
                            tint = if (isActive) theme.textColor else theme.textColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            VerticalDivider(color = Color(0xFF30363D), thickness = 1.dp)
        }
    }
}

// --- 10. CODE EDITOR VIEW (LIVELINE NUMBERS & SYNTAX HIGHLIGHTING) ---
@Composable
fun VSCodeCodeEditorArea(
    content: String,
    language: String,
    theme: EditorTheme,
    highlightedContent: AnnotatedString?,
    onContentChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val linesCount = content.split("\n").size
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scrollY = scrollState.value

    val visibleLineRange by remember(scrollY, linesCount) {
        androidx.compose.runtime.derivedStateOf {
            val lineHeightPx = with(density) { 18.dp.toPx() }
            val firstVisible = (scrollY / lineHeightPx).toInt()
            
            val startLine = maxOf(0, firstVisible - 50)
            val endLine = minOf(linesCount - 1, firstVisible + 40 + 50)
            startLine..endLine
        }
    }

    val transformedAnnotatedString by remember(content, visibleLineRange, highlightedContent, theme, language) {
        androidx.compose.runtime.derivedStateOf {
            if (highlightedContent != null && highlightedContent.text == content) {
                highlightedContent
            } else {
                syntaxHighlight(content, language, theme, visibleLineRange)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .verticalScroll(scrollState)
    ) {
        // Line Numbers list Column
        Column(
            modifier = Modifier
                .background(theme.editorBg.copy(alpha = 0.95f))
                .padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.End
        ) {
            for (i in 1..linesCount) {
                Text(
                    text = "$i",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = theme.lineNumbersColor,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.height(18.dp)
                )
            }
        }

        VerticalDivider(color = theme.lineNumbersColor.copy(alpha = 0.25f), thickness = 1.dp)

        // Custom Highlighted Code Text Field block
        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("code_input_editor"),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = theme.textColor
            ),
            cursorBrush = SolidColor(theme.accentColor),
            visualTransformation = { text ->
                TransformedText(
                    transformedAnnotatedString,
                    OffsetMapping.Identity
                )
            }
        )
    }
}

// --- 11. SOURCAT SHORTCUT PINTASAN BAR CONTROLS ---
@Composable
fun VSCodeShortcutHelperBar(
    theme: EditorTheme,
    activeTabPath: String?,
    onInsertText: (String) -> Unit,
    onRun: () -> Unit,
    onFormat: () -> Unit,
    onSendAI: () -> Unit,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(theme.sidebarBg)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shortcuts = listOf(
                "{" to " { ", "}" to " }", "[" to "[", "]" to "]", "(" to "(", ")" to ")",
                ";" to ";", "=" to " = ", "\t" to "    ", "const" to "const ", "let" to "let ",
                "function" to "function () {\n    \n}", "print" to "print()", "kotlin" to "fun "
            )

            shortcuts.forEach { (label, code) ->
                Button(
                    onClick = { onInsertText(code) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .padding(horizontal = 3.dp)
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        VerticalDivider(color = Color.DarkGray)

        if (activeTabPath != null) {
            Row(
                modifier = Modifier.padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onUndo != null) {
                    IconButton(onClick = onUndo, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Undo, "Undo", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                if (onRedo != null) {
                    IconButton(onClick = onRedo, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Redo, "Redo", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                // Formatting Text Helper
                IconButton(onClick = onFormat, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.FormatAlignLeft, "Format", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onSendAI, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.AutoAwesome, "AI", tint = theme.accentColor, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onRun, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.PlayArrow, "Run", tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// --- 12. TERMUX TERMINAL EMULATOR SHELL ---
@Composable
fun VSCodeTermuxTerminal(
    terminalLines: List<CodeViewModel.TerminalLine>,
    terminalInput: String,
    onValueChange: (String) -> Unit,
    theme: EditorTheme,
    onEnterCommand: (String) -> Unit
) {
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentTypedText = terminalInput

    // Auto scroll bottom when line counts changes
    LaunchedEffect(terminalLines.size) {
        if (terminalLines.isNotEmpty()) {
            scrollState.animateScrollToItem(terminalLines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        // Header title control bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, "Termux Core", tint = Color(0xFF00FF00), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("TERMINAL SHELL - droid@termux:~", color = Color(0xFF33FF33), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Row {
                TextButton(onClick = { onEnterCommand("clear") }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(20.dp)) {
                    Text("CLEAR", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onEnterCommand("help") }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(20.dp)) {
                    Text("HELP", color = theme.accentColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.45f))

        // History content area
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            items(terminalLines) { line ->
                val color = when (line.type) {
                    CodeViewModel.LineType.INPUT_PROMPT -> Color(0xFF33FF33)
                    CodeViewModel.LineType.SYSTEM -> Color(0xFFFFCC00)
                    CodeViewModel.LineType.ERROR -> Color(0xFFEF4444)
                    CodeViewModel.LineType.SUCCESS -> Color(0xFF10B981)
                    CodeViewModel.LineType.INFO -> Color(0xFF3B82F6)
                    CodeViewModel.LineType.OUTPUT -> Color(0xFFDDDDDD)
                }
                Text(
                    text = line.text,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = color,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        // Action prompt Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFF0A0A0A))
                .border(0.5.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "droid-vscode@android:~$",
                color = Color(0xFF33FF33),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Console entry BasicTextField with custom hint
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (currentTypedText.isEmpty()) {
                    Text("Ketik perintah UNIX (misal: 'ls' atau 'run README.md')", color = Color.Gray.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                BasicTextField(
                    value = currentTypedText,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("terminal_shell_input"),
                    cursorBrush = SolidColor(Color(0xFF00FF00)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (currentTypedText.isNotEmpty()) {
                            onEnterCommand(currentTypedText)
                        }
                    })
                )
            }
        }
    }
}

// --- 13. BLUEPRINT SPACE EMPTY WORKSPACE STATE ---
@Composable
fun VSCodeEmptyWorkspaceState(theme: EditorTheme) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "VS Code Art Accent",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, theme.accentColor)
                    .padding(8.dp),
                tint = theme.accentColor
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Droid VS Code v1.0.0",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Buka menu berkas di Sidebar, luncurkan 'README.md', atau ketik perintah 'help' di terminal termux-like di bawah untuk mulai mengompilasi kode.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 11.sp,
                color = Color.LightGray.copy(alpha = 0.8f)
            )
        }
    }
}

// --- 14. HELPER CREATION DIALOG COMPOSABLES ---
@Composable
fun VSCodeCreateItemDialog(
    title: String,
    isFolder: Boolean,
    parentFolder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (parentFolder.isNotEmpty()) {
                    Text("Membuat item di folder: $parentFolder", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                }
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text(if (isFolder) "misal: src" else "misal: main.js") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_item_dialog_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (nameInput.isNotEmpty()) onConfirm(nameInput) },
                enabled = nameInput.isNotEmpty()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

private val COMMENT_REGEX_DEFAULT = "(//.*|/\\*[\\s\\S]*?\\*/)".toRegex()
private val COMMENT_REGEX_PYTHON = "(#.*)".toRegex()
private val STRING_DOUBLE_REGEX = "(\"[^\"]*\")".toRegex()
private val STRING_SINGLE_REGEX = "('[^']*')".toRegex()
private val NUMBER_REGEX = "\\b(\\d+)\\b".toRegex()

private val KEYWORDS_JS = listOf("const", "let", "var", "function", "return", "if", "else", "for", "while", "class", "import", "export", "from", "default", "true", "false", "this")
private val KEYWORDS_PY = listOf("def", "return", "if", "else", "elif", "for", "while", "import", "from", "in", "and", "or", "not", "True", "False", "None", "print")
private val KEYWORDS_KT = listOf("package", "import", "fun", "val", "var", "if", "else", "for", "while", "return", "class", "object", "interface", "null", "true", "false", "when", "println")
private val KEYWORDS_HTML = listOf("doctype", "html", "head", "body", "title", "style", "meta", "script", "div", "h1", "p", "button", "onclick")

private val KEYWORD_REGEX_JS = "\\b(${KEYWORDS_JS.joinToString("|")})\\b".toRegex(RegexOption.IGNORE_CASE)
private val KEYWORD_REGEX_PY = "\\b(${KEYWORDS_PY.joinToString("|")})\\b".toRegex(RegexOption.IGNORE_CASE)
private val KEYWORD_REGEX_KT = "\\b(${KEYWORDS_KT.joinToString("|")})\\b".toRegex(RegexOption.IGNORE_CASE)
private val KEYWORD_REGEX_HTML = "\\b(${KEYWORDS_HTML.joinToString("|")})\\b".toRegex(RegexOption.IGNORE_CASE)

fun syntaxHighlight(
    code: String,
    language: String,
    theme: EditorTheme,
    visibleLineRange: IntRange? = null,
    searchMatches: List<com.example.ui.CodeViewModel.SearchMatch> = emptyList(),
    activeMatchIndex: Int = 0
): AnnotatedString {
    val builder = AnnotatedString.Builder(code)
    builder.addStyle(SpanStyle(color = theme.textColor), 0, code.length)
    if (code.isEmpty()) return builder.toAnnotatedString()

    try {
        var startCharIndex = 0
        var endCharIndex = code.length

        if (visibleLineRange != null) {
            var currentLine = 0
            var charPos = 0
            val targetStartLine = visibleLineRange.first
            val targetEndLine = visibleLineRange.last
            var startSet = false

            while (charPos < code.length) {
                if (currentLine >= targetStartLine && !startSet) {
                    startCharIndex = charPos
                    startSet = true
                }
                if (currentLine == targetEndLine + 1) {
                    endCharIndex = charPos
                    break
                }
                if (code[charPos] == '\n') {
                    currentLine++
                }
                charPos++
            }
        }

        // Safety clamp indices
        startCharIndex = startCharIndex.coerceIn(0, code.length)
        endCharIndex = endCharIndex.coerceIn(startCharIndex, code.length)

        if (startCharIndex == endCharIndex) {
            return builder.toAnnotatedString()
        }

        // Get the substring to run regex matching on
        val subCode = code.substring(startCharIndex, endCharIndex)

        // Helper to add SpanStyle relative to original text
        fun addStyleToRange(style: androidx.compose.ui.text.SpanStyle, start: Int, end: Int) {
            val absStart = (start + startCharIndex).coerceIn(0, code.length)
            val absEnd = (end + startCharIndex).coerceIn(absStart, code.length)
            if (absStart < absEnd) {
                builder.addStyle(style, absStart, absEnd)
            }
        }

        // Match Comments
        val commentRegex = if (language == "py") COMMENT_REGEX_PYTHON else COMMENT_REGEX_DEFAULT
        commentRegex.findAll(subCode).forEach { match ->
            addStyleToRange(SpanStyle(color = theme.commentColor), match.range.first, match.range.last + 1)
        }

        // Match Strings (Double quotes)
        STRING_DOUBLE_REGEX.findAll(subCode).forEach { match ->
            addStyleToRange(SpanStyle(color = theme.stringColor), match.range.first, match.range.last + 1)
        }

        // Match Strings (Single quotes)
        STRING_SINGLE_REGEX.findAll(subCode).forEach { match ->
            addStyleToRange(SpanStyle(color = theme.stringColor), match.range.first, match.range.last + 1)
        }

        // Match Numbers
        NUMBER_REGEX.findAll(subCode).forEach { match ->
            addStyleToRange(SpanStyle(color = theme.numberColor), match.range.first, match.range.last + 1)
        }

        // Match Keywords
        val keywordRegex = when (language) {
            "js", "json" -> KEYWORD_REGEX_JS
            "py" -> KEYWORD_REGEX_PY
            "kt" -> KEYWORD_REGEX_KT
            "html" -> KEYWORD_REGEX_HTML
            else -> null
        }

        if (keywordRegex != null) {
            keywordRegex.findAll(subCode).forEach { match ->
                addStyleToRange(
                    SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        // Match Search highlights overlay
        if (searchMatches.isNotEmpty()) {
            searchMatches.forEachIndexed { index, match ->
                val style = if (index == activeMatchIndex) {
                    SpanStyle(background = androidx.compose.ui.graphics.Color(0xFFFF8C00).copy(alpha = 0.6f))
                } else {
                    SpanStyle(background = androidx.compose.ui.graphics.Color.Yellow.copy(alpha = 0.4f))
                }
                val absStart = match.start.coerceIn(0, code.length)
                val absEnd = match.end.coerceIn(absStart, code.length)
                if (absStart < absEnd) {
                    builder.addStyle(style, absStart, absEnd)
                }
            }
        }
    } catch (_: Exception) {}

    return builder.toAnnotatedString()
}

// --- UTILITY MATGERS ---
fun getFileIcon(extension: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (extension.lowercase()) {
        "js", "ts", "json" -> Icons.Default.IntegrationInstructions
        "py" -> Icons.Default.DataObject
        "kt" -> Icons.Default.DeveloperMode
        "html", "xml" -> Icons.Default.Language
        "markdown", "md" -> Icons.Default.Description
        "folder" -> Icons.Default.Folder
        else -> Icons.Default.InsertDriveFile
    }
}

fun getFileIconColor(extension: String): Color {
    return when (extension.lowercase()) {
        "js" -> Color(0xFFF7DF1E)
        "py" -> Color(0xFF3776AB)
        "kt" -> Color(0xFFFF8F00)
        "html" -> Color(0xFFE34F26)
        "markdown", "md" -> Color(0xFF007ACC)
        "folder" -> Color(0xFFFFCA28)
        else -> Color.LightGray
    }
}

// --- 15. VS CODE STATUS BAR FOOTER ---
@Composable
fun VSCodeStatusBar(
    branch: String,
    theme: EditorTheme,
    onSyncClick: () -> Unit,
    autoSaveStatus: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(Color(0xFF0969DA))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Branch details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onSyncClick() }
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Git Branch",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "$branch*",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Synced icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onSyncClick() }
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Synced",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Error statistics
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Errors",
                    tint = Color(0xFFFFCCCC),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "0",
                    color = Color(0xFFFFCCCC),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Auto-Save Status Indicator
            if (autoSaveStatus != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = autoSaveStatus,
                        color = if (autoSaveStatus.startsWith("✓")) Color(0xFF22C55E) else Color.Yellow,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Spaces: 2",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "UTF-8",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

// --- 16. VS CODE ENVIRONMENT SETUP SCREEN ---
@Composable
fun VSCodeEnvironmentSetupScreen(viewModel: CodeViewModel) {
    val isSettingUp by viewModel.isSettingUp.collectAsState()
    val setupStage by viewModel.setupStage.collectAsState()
    val setupProgressPercent by viewModel.setupProgressPercent.collectAsState()
    val setupLogsList by viewModel.setupLogsList.collectAsState()
    val setupPausedDueToInternet by viewModel.setupPausedDueToInternet.collectAsState()
    
    val listState = rememberLazyListState()
    
    LaunchedEffect(setupLogsList.size) {
        if (setupLogsList.isNotEmpty()) {
            listState.animateScrollToItem(setupLogsList.size - 1)
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.startEnvironmentSetup()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1419))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App logo container
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF007ACC), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "VS Code Logo",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Droid VS Code",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Text(
            text = "Menyiapkan Lingkungan Sandbox Ubuntu & Linux Proot...",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151B23)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (setupStage) {
                            0 -> "Tahap 1/5: Inisialisasi Sandbox..."
                            1 -> "Tahap 2/5: Pemasangan proot-distro..."
                            2 -> "Tahap 3/5: Mengunduh Image Ubuntu OS..."
                            3 -> "Tahap 4/5: Ekstraksi RootFS System..."
                            4 -> "Tahap 5/5: Konfigurasi Apt & Dependensi Pengembang..."
                            else -> "Tahap Selesai: Menyiapkan Workspace..."
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (setupPausedDueToInternet) Color(0xFFF78166) else Color(0xFF58A6FF)
                    )
                    
                    Text(
                        text = "${(setupProgressPercent * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                LinearProgressIndicator(
                    progress = { setupProgressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (setupPausedDueToInternet) Color(0xFFF78166) else Color(0xFF58A6FF),
                    trackColor = Color(0xFF30363D)
                )
                
                if (setupPausedDueToInternet) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(Color(0x33F78166), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Offline Interrupted",
                            tint = Color(0xFFF78166),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Koneksi terputus! Progress disimpan secara otomatis. Klik tombol di bawah untuk mencoba kembali.",
                            fontSize = 11.sp,
                            color = Color(0xFFF78166),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(setupLogsList) { log ->
                    val color = when {
                        log.startsWith("[SUKSES]") -> Color(0xFF3FB950)
                        log.startsWith("[EROR") || log.startsWith("⚠️") -> Color(0xFFF85149)
                        log.startsWith("[SISTEM]") -> Color(0xFF58A6FF)
                        log.startsWith("[OPTIMISASI") -> Color(0xFFA5D6FF)
                        log.contains("$ ") -> Color(0xFFFFD600)
                        else -> Color.LightGray
                    }
                    Text(
                        text = log,
                        fontSize = 11.sp,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.6.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0x3330363D))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "RAM Optimasi",
                    tint = Color(0xFF3FB950),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Optimalisasi Penggunaan RAM & Memori Internal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Pembersihan otomatis cache APT dijalankan untuk menghemat 285MB internal. Frekuensi Garbage Collection diatur rendah untuk mengoptimalkan RAM low-end device.",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSettingUp) {
                Button(
                    onClick = { viewModel.forceSimulateDisconnect() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30363D)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Simulate Offline",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulasi Terputus", fontSize = 11.sp, color = Color.White)
                }
            }
            
            if (setupPausedDueToInternet || !isSettingUp) {
                Button(
                    onClick = { viewModel.startEnvironmentSetup() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F6FEB)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Resume",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (setupPausedDueToInternet) "Lanjutkan Setup" else "Mulai Ulang / Hubungkan",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Composable
fun VSCodeFindReplacePanel(
    findQuery: String,
    replaceQuery: String,
    searchMatches: List<com.example.ui.CodeViewModel.SearchMatch>,
    activeMatchIndex: Int,
    matchCase: Boolean,
    wholeWord: Boolean,
    theme: EditorTheme,
    onFindQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onToggleMatchCase: () -> Unit,
    onToggleWholeWord: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matchCount = searchMatches.size
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F1F1F).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF3C3C3C))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Find Input + Counter + Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = findQuery,
                    onValueChange = onFindQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    placeholder = { Text("Cari...", color = Color.Gray, fontSize = 13.sp) },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = Color(0xFF3C3C3C),
                        focusedContainerColor = Color(0xFF2D2D2D),
                        unfocusedContainerColor = Color(0xFF2D2D2D)
                    ),
                    shape = RoundedCornerShape(4.dp)
                )

                val matchNum = if (matchCount > 0) activeMatchIndex + 1 else 0
                Text(
                    text = "$matchNum dari $matchCount",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(onClick = onFindPrevious, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Sebelumnya",
                        tint = if (matchCount > 0) Color.White else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onFindNext, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Berikutnya",
                        tint = if (matchCount > 0) Color.White else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Row 2: Replace Input + Action Buttons + Option Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    placeholder = { Text("Ganti dengan...", color = Color.Gray, fontSize = 13.sp) },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = Color(0xFF3C3C3C),
                        focusedContainerColor = Color(0xFF2D2D2D),
                        unfocusedContainerColor = Color(0xFF2D2D2D)
                    ),
                    shape = RoundedCornerShape(4.dp)
                )

                IconButton(
                    onClick = onToggleMatchCase,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (matchCase) theme.accentColor.copy(alpha = 0.3f) else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    Text(
                        "Aa",
                        color = if (matchCase) theme.accentColor else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onToggleWholeWord,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (wholeWord) theme.accentColor.copy(alpha = 0.3f) else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    Text(
                        "W",
                        color = if (wholeWord) theme.accentColor else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onReplace,
                    enabled = matchCount > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Ganti Current",
                        tint = if (matchCount > 0) theme.accentColor else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onReplaceAll,
                    enabled = matchCount > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Ganti Semua",
                        tint = if (matchCount > 0) theme.accentColor else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

