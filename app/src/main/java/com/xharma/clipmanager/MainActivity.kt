package com.xharma.clipmanager

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xharma.clipmanager.data.ClipDatabase
import com.xharma.clipmanager.data.ClipEntry
import com.xharma.clipmanager.data.PreferenceManager
import com.xharma.clipmanager.service.ClipboardMonitorService
import com.xharma.clipmanager.ui.theme.ClipManagerTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClipManagerTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefManager = remember { PreferenceManager(context) }
    val db = remember { ClipDatabase.getDatabase(context) }
    
    val isRunning by ClipboardMonitorService.isRunning.collectAsStateWithLifecycle()
    val isServicePrefEnabled by prefManager.isServiceEnabled.collectAsState(initial = false)
    val isAutoSavePrefEnabled by prefManager.isAutoSaveEnabled.collectAsState(initial = false)

    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            ClipboardMonitorService.start(context)
            scope.launch { prefManager.setServiceEnabled(true) }
        }
    }

    LaunchedEffect(isServicePrefEnabled) {
        if (isServicePrefEnabled && !isRunning) {
            ClipboardMonitorService.start(context)
        }
    }

    LaunchedEffect(isAutoSavePrefEnabled) {
        ClipboardMonitorService.setAutoSaveEnabled(isAutoSavePrefEnabled)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isRunning) ClipboardMonitorService.refresh(context)
    }

    if (showHistory) {
        BackHandler { showHistory = false }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to delete all captured clips? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            db.clipDao().deleteAll()
                            showClearDialog = false
                            Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.semantics { contentDescription = "dialog_clear_all_button" }
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    modifier = Modifier.semantics { contentDescription = "dialog_cancel_button" }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.ContentPaste, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (showHistory) "Clip History" else "ClipManager", 
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    if (showHistory) {
                        IconButton(onClick = { showHistory = false }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "back_button")
                        }
                    }
                },
                actions = {
                    if (showHistory) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "settings_button")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.width(200.dp).background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Clear All History", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        showClearDialog = true
                                    },
                                    modifier = Modifier.semantics { contentDescription = "clear_all_history_menu_item" }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (showHistory) {
                HistoryScreen()
            } else {
                LatestClipScreen(
                    isRunning = isRunning,
                    isServiceEnabled = isServicePrefEnabled,
                    isAutoSaveEnabled = isAutoSavePrefEnabled,
                    onServiceToggle = { enabled ->
                        scope.launch {
                            prefManager.setServiceEnabled(enabled)
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else ClipboardMonitorService.start(context)
                            } else {
                                ClipboardMonitorService.stop(context)
                            }
                        }
                    },
                    onAutoSaveToggle = { enabled ->
                        scope.launch {
                            prefManager.setAutoSaveEnabled(enabled)
                            ClipboardMonitorService.setAutoSaveEnabled(enabled)
                        }
                    },
                    onViewHistory = { showHistory = true }
                )
            }
        }
    }
}

@Composable
fun LatestClipScreen(
    isRunning: Boolean,
    isServiceEnabled: Boolean,
    isAutoSaveEnabled: Boolean,
    onServiceToggle: (Boolean) -> Unit,
    onAutoSaveToggle: (Boolean) -> Unit,
    onViewHistory: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { ClipDatabase.getDatabase(context) }
    val latestClip by db.clipDao().getLatestClip().collectAsState(initial = null)

    var latestVideo by remember { mutableStateOf<File?>(null) }
    var latestDownload by remember { mutableStateOf<File?>(null) }

    val filePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            latestVideo = getLatestFileFromDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
            latestDownload = getLatestFileFromDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        }
    }

    fun refreshFiles() {
        latestVideo = getLatestFileFromDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
        latestDownload = getLatestFileFromDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        filePermissionLauncher.launch(permissions)
        refreshFiles()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshFiles()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Quick Stats / Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                title = "Service",
                status = if (isRunning) "Active" else "Inactive",
                icon = if (isRunning) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                isActive = isRunning,
                modifier = Modifier.weight(1f).semantics { contentDescription = "service_status_card" },
                onClick = { onServiceToggle(!isServiceEnabled) }
            )
            StatusCard(
                title = "Auto-save",
                status = if (isAutoSaveEnabled) "Enabled" else "Disabled",
                icon = Icons.Rounded.Save,
                isActive = isAutoSaveEnabled,
                modifier = Modifier.weight(1f).semantics { contentDescription = "autosave_status_card" },
                onClick = { onAutoSaveToggle(!isAutoSaveEnabled) }
            )
        }

        // Section 1: Clipboard
        HomeSection(title = "CLIPBOARD", icon = Icons.Rounded.ContentPaste) {
            if (latestClip != null) {
                HistoryItem(
                    clip = latestClip!!,
                    onSaveToFile = {
                        saveClipToFile(latestClip!!.text, context)
                    }
                )
            } else {
                PlaceholderCard("No clips captured yet")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "view_history_button" },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.History, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Full History")
            }
        }

        // Section 2: Latest Movie
        HomeSection(title = "LATEST MOVIE", icon = Icons.Rounded.Movie) {
            if (latestVideo != null) {
                FileItem(latestVideo!!, "move_movie_button") {
                    moveFileToTmp(latestVideo!!, context)
                }
            } else {
                PlaceholderCard("No videos found in Movies")
            }
        }

        // Section 3: Latest Download
        HomeSection(title = "LATEST DOWNLOAD", icon = Icons.Rounded.Download) {
            if (latestDownload != null) {
                FileItem(latestDownload!!, "move_download_button") {
                    moveFileToTmp(latestDownload!!, context)
                }
            } else {
                PlaceholderCard("No files found in Downloads")
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun HomeSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.1.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun PlaceholderCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun FileItem(file: File, moveDescription: String, onMove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${file.length() / 1024} KB • ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onMove,
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(Icons.Rounded.DriveFileMove, moveDescription, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun getLatestFileFromDir(dir: File): File? {
    return runCatching {
        dir.listFiles()?.filter { it.isFile }?.maxByOrNull { it.lastModified() }
    }.getOrNull()
}

private fun moveFileToTmp(file: File, context: Context) {
    val dest = File("/data/local/tmp", "y_clipmanager.mp4")
    try {
        file.inputStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Toast.makeText(context, "Moved to /data/local/tmp/y_clipmanager.mp4", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("FileMove", "Error moving file", e)
        Toast.makeText(context, "Permission Denied: Run ADB command to fix", Toast.LENGTH_LONG).show()
    }
}

private fun saveClipToFile(text: String, context: Context) {
    val dest = File("/data/local/tmp", "x_clipmanager.txt")
    try {
        dest.writeText(text)
        Toast.makeText(context, "Saved to /data/local/tmp/x_clipmanager.txt", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("FileSave", "Error saving clip", e)
        Toast.makeText(context, "Permission Denied: Run ADB command to fix", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusCard(
    title: String,
    status: String,
    icon: ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (!isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null

    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = border
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(status, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsToggle(
    title: String, 
    subtitle: String,
    isChecked: Boolean, 
    icon: ImageVector, 
    onToggle: (Boolean) -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isChecked, 
                    onCheckedChange = null,
                    modifier = Modifier.scale(0.7f)
                )
            }
        },
        onClick = { onToggle(!isChecked) }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { ClipDatabase.getDatabase(context) }
    val clips by db.clipDao().getAllClips().collectAsState(initial = emptyList())
    
    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    LaunchedEffect(clips.size) {
        if (clips.isNotEmpty() && listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }

    AnimatedContent(
        targetState = clips.isEmpty(),
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
        },
        label = "content_transition"
    ) { isEmpty ->
        if (isEmpty) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Inbox, 
                        null, 
                        modifier = Modifier.size(72.dp), 
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No captured clips yet", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = clips,
                        key = { it.id }
                    ) { clip ->
                        HistoryItem(
                            clip = clip,
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Scroll to top")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItem(
    clip: ClipEntry, 
    modifier: Modifier = Modifier,
    onSaveToFile: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    
    val transition = updateTransition(expanded, label = "expansion")
    val cardElevation by transition.animateDp(label = "elevation") { if (it) 8.dp else 0.dp }
    val cardColor by transition.animateColor(label = "color") { 
        if (it) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clip.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(clip.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onSaveToFile != null) {
                        IconButton(
                            onClick = onSaveToFile,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Save, 
                                contentDescription = "save_clip_button", 
                                tint = MaterialTheme.colorScheme.secondary, 
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Copied Clip", clip.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy, 
                            contentDescription = "copy_clip_button",
                            tint = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
