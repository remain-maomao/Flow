package org.example.flow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.example.flow.classify.ModeClassifier
import org.example.flow.classify.ModeDetector
import org.example.flow.engine.ReminderEngine
import org.example.flow.model.ActiveWindow
import org.example.flow.model.BrowserMessage
import org.example.flow.model.ClassificationResult
import org.example.flow.model.Mode
import org.example.flow.monitor.WindowMonitor
import org.example.flow.notify.Notifier
import org.example.flow.server.TabServer
import java.io.File

@Composable
fun FlowApp(
    tabServer: TabServer,
    reminderEngine: ReminderEngine,
    notifier: Notifier,
    extensionDir: File,
    extensionConnected: Boolean,
) {
    var activeWindow by remember { mutableStateOf(ActiveWindow("(waiting...)", "(waiting...)", 0L)) }
    var browserMessage by remember { mutableStateOf<BrowserMessage?>(null) }
    // Latest browser tab data (used for classification when foreground window is a browser)
    var latestBrowserMessage: BrowserMessage? = null
    var currentMode by remember { mutableStateOf(Mode.WORK) }

    // Dev mode
    var developerMode by remember { mutableStateOf(false) }
    var devClickCount by remember { mutableStateOf(0) }
    var devLastClickTime by remember { mutableStateOf(0L) }

    // Tab selection
    var selectedTab by remember { mutableStateOf(0) }

    // Timer + timeScale
    var timeScale by remember { mutableStateOf(60L) }
    val elapsedVirtualMs by reminderEngine.elapsedVirtualMs.collectAsState()
    val nextReminderVirtualMs by reminderEngine.nextReminderVirtualMs.collectAsState()

    val modeDetector = remember { ModeDetector(debounceMs = 5_000L) }

    LaunchedEffect(Unit) {
        val cfg = org.example.flow.classify.ConfigManager.load()
        developerMode = cfg.developerMode
    }

    LaunchedEffect(Unit) {
        WindowMonitor.observeActiveWindow().collectLatest { activeWindow = it }
    }

    LaunchedEffect(Unit) {
        tabServer.messages.collectLatest { msg ->
            browserMessage = msg
            latestBrowserMessage = msg
        }
    }

    var showSetupGuide by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(3_000)
        if (!extensionConnected) showSetupGuide = true
    }

    // ── Classification flow (single entry point, no merge) ──
    val classificationFlow = remember {
        kotlinx.coroutines.flow.MutableSharedFlow<ClassificationResult>(extraBufferCapacity = 4)
    }

    LaunchedEffect(Unit) {
        val browserProcesses = setOf("chrome.exe", "msedge.exe", "firefox.exe", "brave.exe", "opera.exe")

        WindowMonitor.observeActiveWindow().collect { window ->
            val isBrowser = window.processName.lowercase() in browserProcesses

            val browserMsg = latestBrowserMessage
            val result = if (isBrowser && extensionConnected && browserMsg != null) {
                ModeClassifier.classifyBrowser(browserMsg)
            } else {
                ModeClassifier.classifyWindow(window)
            }
            classificationFlow.tryEmit(result)
        }
    }

    LaunchedEffect(Unit) {
        modeDetector.detect(classificationFlow).collectLatest { mode ->
            if (mode != currentMode) {
                currentMode = mode
                reminderEngine.onModeChanged(mode)
                notifier.updateIcon(mode)
            }
        }
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
            // ── Title + 7-click dev unlock ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Flow",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        val now = System.currentTimeMillis()
                        if (now - devLastClickTime > 600) devClickCount = 0
                        devClickCount++
                        devLastClickTime = now
                        if (devClickCount >= 7 && !developerMode) {
                            developerMode = true
                            val cfg = org.example.flow.classify.ConfigManager.load()
                            org.example.flow.classify.ConfigManager.save(cfg.copy(developerMode = true))
                            println("[App] Developer mode unlocked!")
                        }
                    },
                )
                if (developerMode) {
                    Text("DEV", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                } else if (devClickCount in 4..6) {
                    Text("${7 - devClickCount} more...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Tab Row ──
            val tabs = listOf("Dashboard", "Settings")
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                        },
                    )
                }
            }

            // ── Tab Content ──
            when (selectedTab) {
                0 -> DashboardPanel(
                    activeWindow = activeWindow,
                    browserMessage = browserMessage,
                    currentMode = currentMode,
                    elapsedVirtualMs = elapsedVirtualMs,
                    nextReminderVirtualMs = nextReminderVirtualMs,
                    timeScale = timeScale,
                    developerMode = developerMode,
                    showSetupGuide = showSetupGuide,
                    extensionDir = extensionDir,
                    reminderEngine = reminderEngine,
                    onTimeScaleChange = { newScale ->
                        if (newScale != timeScale) {
                            timeScale = newScale
                            reminderEngine.updateTimeScale(newScale)
                        }
                    },
                )
                1 -> SettingsPanel(onClose = { selectedTab = 0 })
            }
        }
    }
}
