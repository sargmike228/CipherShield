package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Base64
import androidx.compose.ui.draw.scale
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.text.selection.SelectionContainer
import com.example.audio.RetroSoundGenerator
import com.example.crypto.MultiLayerCrypto
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CryptoStepStatus
import com.example.ui.viewmodel.CryptoViewModel
import kotlinx.coroutines.launch

// Beautiful Dark Futuristic Theme Colors
val CyberDarkBg = Color(0xFF0F172A)      // Deep Slate
val CyberCardBg = Color(0xFF1E293B)      // Slate Card
val CyberTerminalBg = Color(0xFF020617)  // Obsidian Black
val CyberPrimary = Color(0xFF10B981)     // Neon Emerald
val CyberSecondary = Color(0xFF06B6D4)   // Electric Cyan
val CyberAccent = Color(0xFFEC4899)      // Hot Pink
val CyberTextBright = Color(0xFFF8FAFC)  // Bright White
val CyberTextMuted = Color(0xFF94A3B8)   // Slate Muted
val CyberError = Color(0xFFEF4444)       // Neon Red
val CyberYellow = Color(0xFFF59E0B)      // Tech Gold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainCryptoScreen()
            }
        }
    }
}

@Composable
fun MainCryptoScreen(viewModel: CryptoViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()
    val pipelineStates by viewModel.pipelineStates.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    
    val secureFiles by viewModel.secureFiles.collectAsStateWithLifecycle()
    val keyProfiles by viewModel.keyProfiles.collectAsStateWithLifecycle()
    val cryptoHistory by viewModel.cryptoHistory.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("ТЕКСТ", "СЕЙФ", "PGP RSA", "ХЭШИРОВАНИЕ", "КЛЮЧИ", "СТАТУС & ЛОГИ")

    // State for Dialogs
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showImportFileDialog by remember { mutableStateOf(false) }
    var showSaveProfileDialog by remember { mutableStateOf(false) }
    var showViewContentDialog by remember { mutableStateOf<SecureFileItem?>(null) }
    var fileContentText by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold")
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = CyberDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberDarkBg)
        ) {
            // Elegant Cyber Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isProcessing) CyberAccent else CyberPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CIPHERSHIELD",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = CyberTextBright,
                            letterSpacing = 2.sp
                        )
                    }
                    Text(
                        text = "7-уровневая криптографическая станция",
                        fontSize = 11.sp,
                        color = CyberTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isProcessing) "RUNNING" else "SECURED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProcessing) CyberAccent else CyberPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Tabs controller
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = CyberDarkBg,
                contentColor = CyberPrimary,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = CyberCardBg) },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = CyberPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = {
                            activeTab = index
                            scope.launch { RetroSoundGenerator.playDiceRoll() }
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (activeTab == index) CyberPrimary else CyberTextMuted
                            )
                        },
                        modifier = Modifier.testTag("tab_$index")
                    )
                }
            }

            // Real-time visual pipeline stepper (always visible as a sleek compact header at the bottom of headers)
            PipelineProgressStepper(pipelineStates = pipelineStates, isProcessing = isProcessing)

            // Main Content Area with Animated transitions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                when (activeTab) {
                    0 -> TextCryptoTab(viewModel, context, scope)
                    1 -> WorkspaceSafeTab(
                        viewModel = viewModel,
                        secureFiles = secureFiles,
                        onAddClick = { showCreateFileDialog = true },
                        onImportClick = { showImportFileDialog = true },
                        onViewContent = { file ->
                            scope.launch {
                                val text = viewModel.readWorkspaceFileText(file)
                                fileContentText = text
                                showViewContentDialog = file
                            }
                        }
                    )
                    2 -> PgpCryptoTab(viewModel, secureFiles, context, scope)
                    3 -> HashingTab(viewModel, secureFiles, context, scope)
                    4 -> KeyVaultTab(viewModel, keyProfiles, onSavePresetClick = { showSaveProfileDialog = true })
                    5 -> StatusLogsTab(viewModel, consoleLogs, cryptoHistory)
                }
            }
        }
    }

    // DIALOGS
    if (showCreateFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        var newFileContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            containerColor = CyberCardBg,
            title = {
                Text(
                    "СОЗДАТЬ СЕКРЕТНЫЙ ФАЙЛ",
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Имя файла (например, memo.txt)") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("new_file_name_input")
                    )
                    OutlinedTextField(
                        value = newFileContent,
                        onValueChange = { newFileContent = it },
                        label = { Text("Секретное содержимое") },
                        minLines = 4,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("new_file_content_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.trim().isNotEmpty()) {
                            viewModel.createTextFileInWorkspace(newFileName, newFileContent)
                            showCreateFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier.testTag("confirm_create_file")
                ) {
                    Text("СОЗДАТЬ", color = CyberDarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("ОТМЕНА", color = CyberTextMuted)
                }
            }
        )
    }

    if (showImportFileDialog) {
        var importName by remember { mutableStateOf("") }
        var importRawText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportFileDialog = false },
            containerColor = CyberCardBg,
            title = {
                Text(
                    "ИМПОРТИРОВАТЬ ТЕКСТ / БАЙТЫ",
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Создайте симуляцию импорта внешнего файла. Вы можете вставить зашифрованные Base64-строки для расшифрования из файла.",
                        fontSize = 12.sp,
                        color = CyberTextMuted
                    )
                    OutlinedTextField(
                        value = importName,
                        onValueChange = { importName = it },
                        label = { Text("Имя виртуального файла (например, import.txt)") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("import_file_name_input")
                    )
                    OutlinedTextField(
                        value = importRawText,
                        onValueChange = { importRawText = it },
                        label = { Text("Текст или зашифрованный Base64 контейнер") },
                        minLines = 3,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("import_file_content_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importName.trim().isNotEmpty()) {
                            val bytes = try {
                                Base64.decode(importRawText, Base64.DEFAULT)
                            } catch (e: Exception) {
                                importRawText.toByteArray(Charsets.UTF_8)
                            }
                            viewModel.importBytesToWorkspace(importName, bytes)
                            showImportFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier.testTag("confirm_import_file")
                ) {
                    Text("ИМПОРТ", color = CyberDarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportFileDialog = false }) {
                    Text("ОТМЕНА", color = CyberTextMuted)
                }
            }
        )
    }

    if (showSaveProfileDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveProfileDialog = false },
            containerColor = CyberCardBg,
            title = {
                Text(
                    "СОХРАНИТЬ КЛЮЧЕВОЙ ПРЕСЕТ",
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Имя пресета ключей (например, Vault-Key-2026)") },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("preset_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.trim().isNotEmpty()) {
                            viewModel.saveCurrentKeyProfile(presetName.trim())
                            showSaveProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier.testTag("confirm_save_preset")
                ) {
                    Text("СОХРАНИТЬ", color = CyberDarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveProfileDialog = false }) {
                    Text("ОТМЕНА", color = CyberTextMuted)
                }
            }
        )
    }

    showViewContentDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showViewContentDialog = null },
            containerColor = CyberCardBg,
            title = {
                Text(
                    "СОДЕРЖИМОЕ: ${file.originalName}",
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .background(CyberTerminalBg)
                        .border(1.dp, CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = fileContentText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (file.isEncrypted) CyberAccent else CyberPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Copied secure content", fileContentText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Скопировано в буфер!", Toast.LENGTH_SHORT).show()
                        scope.launch { RetroSoundGenerator.playHeal() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    modifier = Modifier.testTag("copy_file_content")
                ) {
                    Text("КОПИРОВАТЬ", color = CyberDarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showViewContentDialog = null }) {
                    Text("ЗАКРЫТЬ", color = CyberTextMuted)
                }
            }
        )
    }
}

/**
 * Visual Top Stepper to track all 7 security layers
 */
@Composable
fun PipelineProgressStepper(pipelineStates: List<CryptoStepStatus>, isProcessing: Boolean) {
    val stepShortNames = listOf("AES", "BLOW", "3DES", "LFSR", "LCG", "HM512", "HM256")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, CyberCardBg.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stepShortNames.forEachIndexed { index, name ->
                val status = pipelineStates[index]
                val color = when (status) {
                    is CryptoStepStatus.NotStarted -> CyberTextMuted.copy(alpha = 0.3f)
                    is CryptoStepStatus.Processing -> CyberYellow
                    is CryptoStepStatus.Success -> CyberPrimary
                    is CryptoStepStatus.Failed -> CyberError
                }

                // Dynamic pulsating weight/offset for processing step
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, easing = EaseInOutQuad),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.15f))
                            .border(
                                width = if (status is CryptoStepStatus.Processing) 2.dp else 1.dp,
                                color = color,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .then(
                                if (status is CryptoStepStatus.Processing) Modifier.scale(scale) else Modifier
                            )
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = color
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (status is CryptoStepStatus.NotStarted) CyberTextMuted.copy(alpha = 0.4f) else color
                    )
                }
            }
        }
    }
}

/**
 * Tab 1: TEXT CRYPTO HANDLER
 */
@Composable
fun TextCryptoTab(viewModel: CryptoViewModel, context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val outputText by viewModel.outputText.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()

    val currentEntropy = getPasswordEntropy(inputText)
    val entropyRating = when {
        currentEntropy == 0.0 -> "Пусто"
        currentEntropy < 32.0 -> "Слабый (небезопасно)"
        currentEntropy < 64.0 -> "Средний"
        currentEntropy < 128.0 -> "Высокий"
        else -> "Сверхвысокий (квантово-защищенный)"
    }
    val entropyColor = when {
        currentEntropy == 0.0 -> CyberTextMuted
        currentEntropy < 32.0 -> CyberError
        currentEntropy < 64.0 -> CyberYellow
        else -> CyberPrimary
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Source Plaintext
        OutlinedTextField(
            value = inputText,
            onValueChange = { viewModel.inputText.value = it },
            label = { Text("ИСХОДНЫЙ ТЕКСТ ДЛЯ ШИФРОВАНИЯ / РАСШИФРОВАНИЯ", fontFamily = FontFamily.Monospace) },
            minLines = 4,
            maxLines = 8,
            colors = outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_input_field")
        )

        // Entropy Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Информационная энтропия: ${String.format("%.1f", currentEntropy)} бит",
                fontSize = 11.sp,
                color = CyberTextMuted,
                fontFamily = FontFamily.Monospace
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = entropyColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, entropyColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = entropyRating,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = entropyColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.encryptText() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("encrypt_text_button"),
                enabled = !isProcessing
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Encrypt", tint = CyberDarkBg)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ШИФРОВАТЬ", fontWeight = FontWeight.Black, color = CyberDarkBg, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.decryptText() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("decrypt_text_button"),
                enabled = !isProcessing
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Decrypt", tint = CyberDarkBg)
                Spacer(modifier = Modifier.width(6.dp))
                Text("РАСШИФРОВАТЬ", fontWeight = FontWeight.Black, color = CyberDarkBg, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        // Decrypted / Encrypted Output Block
        OutlinedTextField(
            value = outputText,
            onValueChange = { viewModel.outputText.value = it },
            label = { Text("РЕЗУЛЬТАТ ОБРАБОТКИ (ВЫХОДНОЙ КАНАЛ)", fontFamily = FontFamily.Monospace) },
            minLines = 4,
            maxLines = 8,
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CyberTerminalBg,
                unfocusedContainerColor = CyberTerminalBg,
                focusedTextColor = CyberPrimary,
                unfocusedTextColor = CyberPrimary,
                focusedBorderColor = CyberPrimary.copy(alpha = 0.5f),
                unfocusedBorderColor = CyberCardBg,
                focusedLabelColor = CyberPrimary,
                unfocusedLabelColor = CyberTextMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_output_field")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("CipherShield Output", outputText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Результат скопирован!", Toast.LENGTH_SHORT).show()
                    scope.launch { RetroSoundGenerator.playHeal() }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCardBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("copy_output_button"),
                enabled = outputText.isNotEmpty()
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Copy", tint = CyberPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("КОПИРОВАТЬ", color = CyberPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.inputText.value = ""
                    viewModel.outputText.value = ""
                    scope.launch { RetroSoundGenerator.playDamage() }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCardBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("clear_fields_button")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = CyberAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("СБРОСИТЬ", color = CyberAccent, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Tab 2: WORKSPACE FILE SAFE TAB
 */
@Composable
fun WorkspaceSafeTab(
    viewModel: CryptoViewModel,
    secureFiles: List<SecureFileItem>,
    onAddClick: () -> Unit,
    onImportClick: () -> Unit,
    onViewContent: (SecureFileItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ЗАЩИЩЕННЫЙ ЛОКАЛЬНЫЙ СЕЙФ",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTextBright
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp).testTag("create_file_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = CyberDarkBg, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("СОЗДАТЬ", color = CyberDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = onImportClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp).testTag("import_file_btn")
                ) {
                    Icon(imageVector = Icons.Default.List, contentDescription = "Import", tint = CyberDarkBg, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ИМПОРТ", color = CyberDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        if (secureFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, CyberCardBg, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Empty safe", tint = CyberTextMuted, modifier = Modifier.size(48.dp))
                    Text("Ваш сейф пуст. Создайте текстовый файл или импортируйте зашифрованную строку.", color = CyberTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(secureFiles) { file ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (file.isEncrypted) CyberAccent.copy(alpha = 0.3f) else CyberPrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("file_item_${file.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (file.isEncrypted) Icons.Default.Lock else Icons.Default.Menu,
                                        contentDescription = "File Type",
                                        tint = if (file.isEncrypted) CyberAccent else CyberPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = file.originalName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = CyberTextBright
                                        )
                                        Text(
                                            text = "${file.sizeBytes} байт | ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(file.timestamp)}",
                                            fontSize = 11.sp,
                                            color = CyberTextMuted
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // View button
                                    IconButton(
                                        onClick = { onViewContent(file) },
                                        modifier = Modifier.size(32.dp).testTag("view_file_${file.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Info, contentDescription = "View", tint = CyberPrimary, modifier = Modifier.size(18.dp))
                                    }

                                    // Export button
                                    IconButton(
                                        onClick = { viewModel.exportFileToDownloads(file) },
                                        modifier = Modifier.size(32.dp).testTag("export_file_${file.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export to Downloads", tint = CyberSecondary, modifier = Modifier.size(18.dp))
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = { viewModel.deleteWorkspaceFile(file) },
                                        modifier = Modifier.size(32.dp).testTag("delete_file_${file.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CyberError, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = CyberDarkBg, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Encryption/Decryption/Signature controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (file.isEncrypted) {
                                    if (file.originalName.endsWith(".pgp.enc")) {
                                        Button(
                                            onClick = { viewModel.decryptFilePgp(file) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp).testTag("decrypt_pgp_${file.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Decrypt PGP", tint = CyberDarkBg, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("РАСШИФРОВАТЬ PGP", color = CyberDarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.decryptFile(file) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp).testTag("decrypt_file_action_${file.id}")
                                        ) {
                                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Decrypt file", tint = CyberDarkBg, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("7-L РАСШИФРОВАТЬ", color = CyberDarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                } else {
                                    // 7-Layer Encribute
                                    Button(
                                        onClick = { viewModel.encryptFile(file) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp).testTag("encrypt_file_action_${file.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Encrypt 7-L", tint = CyberDarkBg, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("7-L ШИФРОВАТЬ", color = CyberDarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }

                                    // PGP Encrypt
                                    Button(
                                        onClick = { viewModel.encryptFilePgp(file) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp).testTag("encrypt_pgp_${file.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Encrypt PGP", tint = CyberDarkBg, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PGP ШИФРОВАТЬ", color = CyberDarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }

                                    // PGP Sign
                                    Button(
                                        onClick = { viewModel.signFilePgp(file) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberYellow),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp).testTag("sign_pgp_${file.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Sign PGP", tint = CyberDarkBg, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ПОДПИСАТЬ", color = CyberDarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }

                                    // PGP Verify
                                    Button(
                                        onClick = { viewModel.verifyFilePgp(file) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp).testTag("verify_pgp_${file.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Verify Signature", tint = CyberDarkBg, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ПРОВЕРИТЬ", color = CyberDarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: KEY VAULT TAB
 */
@Composable
fun KeyVaultTab(
    viewModel: CryptoViewModel,
    keyProfiles: List<SavedKeyProfile>,
    onSavePresetClick: () -> Unit
) {
    val isCustom by viewModel.isCustomKeys.collectAsStateWithLifecycle()
    val masterPass by viewModel.masterPassword.collectAsStateWithLifecycle()
    val customK by viewModel.customKeys.collectAsStateWithLifecycle()

    var masterPassVisible by remember { mutableStateOf(false) }
    val keysVisible = remember { mutableStateListOf(*Array(7) { false }) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Toggle expert custom keys vs master password
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCardBg, RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Режим Эксперта (7 ключей)",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCustom) CyberPrimary else CyberTextMuted,
                modifier = Modifier.padding(start = 10.dp)
            )
            Switch(
                checked = isCustom,
                onCheckedChange = {
                    viewModel.isCustomKeys.value = it
                    scope.launch { RetroSoundGenerator.playDiceRoll() }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyberDarkBg,
                    checkedTrackColor = CyberPrimary,
                    uncheckedThumbColor = CyberTextMuted,
                    uncheckedTrackColor = CyberTerminalBg
                )
            )
        }

        // Vault Keys Config Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isCustom) {
                    // Single Master Password Mode
                    Text(
                        "КЛЮЧЕВАЯ КОНФИГУРАЦИЯ: МАСТЕР-ПАРОЛЬ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPrimary
                    )
                    Text(
                        "Из единого мастер-пароля система сгенерирует 7 уникальных высокоэнтропийных ключей для каждого уровня защиты.",
                        fontSize = 11.sp,
                        color = CyberTextMuted
                    )

                    OutlinedTextField(
                        value = masterPass,
                        onValueChange = { viewModel.masterPassword.value = it },
                        label = { Text("Введите мастер-пароль") },
                        visualTransformation = if (masterPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = outlinedTextFieldColors(),
                        trailingIcon = {
                            IconButton(onClick = { masterPassVisible = !masterPassVisible }) {
                                Icon(
                                    imageVector = if (masterPassVisible) Icons.Default.Info else Icons.Default.Lock,
                                    contentDescription = "Show/Hide password",
                                    tint = CyberPrimary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("master_password_input")
                    )

                    // Generation tools
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.masterPassword.value = generatePassphrase()
                                scope.launch { RetroSoundGenerator.playHeal() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTerminalBg),
                            border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("generate_master_pass")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Gen", tint = CyberPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ГЕНЕРАТОР", color = CyberPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = onSavePresetClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_master_pass_preset")
                        ) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = "Save", tint = CyberDarkBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("СОХРАНИТЬ", color = CyberDarkBg, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    // Expert Custom Keys Mode
                    Text(
                        "КОНФИГУРАЦИЯ ЭКСПЕРТА: 7 КЛЮЧЕЙ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberAccent
                    )
                    Text(
                        "Установите 7 полностью независимых текстовых паролей для криптографического конвейера вручную.",
                        fontSize = 11.sp,
                        color = CyberTextMuted
                    )

                    val algorithmLabels = listOf(
                        "1. AES-GCM Key A", "2. Blowfish Key B", "3. 3DES Key C",
                        "4. LFSR XOR Pass D", "5. LCG XOR Pass E", "6. HMAC-512 Pass F", "7. HMAC-256 Pass G"
                    )

                    for (i in 0..6) {
                        OutlinedTextField(
                            value = customK[i],
                            onValueChange = { newText ->
                                viewModel.customKeys.value = customK.toMutableList().apply { this[i] = newText }
                            },
                            label = { Text(algorithmLabels[i], fontSize = 11.sp) },
                            visualTransformation = if (keysVisible[i]) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = outlinedTextFieldColors(),
                            trailingIcon = {
                                Row {
                                    IconButton(
                                        onClick = {
                                            viewModel.customKeys.value = customK.toMutableList().apply { this[i] = generatePassphrase() }
                                            scope.launch { RetroSoundGenerator.playHeal() }
                                        },
                                        modifier = Modifier.size(36.dp).testTag("gen_expert_key_$i")
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Gen key", tint = CyberPrimary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { keysVisible[i] = !keysVisible[i] },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (keysVisible[i]) Icons.Default.Info else Icons.Default.Lock,
                                            contentDescription = "Show/Hide key",
                                            tint = CyberTextMuted
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expert_key_input_$i")
                        )
                    }

                    Button(
                        onClick = onSavePresetClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_expert_preset")
                    ) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "Save", tint = CyberDarkBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("СОХРАНИТЬ ВСЕ КЛЮЧИ КАК ПРЕСЕТ", color = CyberDarkBg, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Key presets manager
        Text(
            "СОХРАНЕННЫЕ ПРЕСЕТЫ СЕКРЕТОВ (${keyProfiles.size})",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTextBright
        )

        if (keyProfiles.isEmpty()) {
            Text("У вас нет сохраненных профилей ключей.", fontSize = 11.sp, color = CyberTextMuted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keyProfiles.forEach { profile ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                        border = BorderStroke(1.dp, CyberCardBg.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("preset_card_${profile.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (profile.isCustom) Icons.Default.Star else Icons.Default.Lock,
                                    contentDescription = "Preset icon",
                                    tint = if (profile.isCustom) CyberAccent else CyberPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = profile.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = CyberTextBright
                                    )
                                    Text(
                                        text = if (profile.isCustom) "Режим: Эксперт (7 ключей)" else "Режим: Мастер-пароль",
                                        fontSize = 11.sp,
                                        color = CyberTextMuted
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.loadKeyProfile(profile) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp).testTag("load_preset_${profile.id}")
                                ) {
                                    Text("ВЫБРАТЬ", color = CyberDarkBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteKeyProfile(profile) },
                                    modifier = Modifier.size(28.dp).testTag("delete_preset_${profile.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete preset", tint = CyberError, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 4: STATUS TERMINAL & SQL OPERATIONS HISTORY
 */
@Composable
fun StatusLogsTab(
    viewModel: CryptoViewModel,
    consoleLogs: List<String>,
    history: List<CryptoHistory>
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Virtual Hacking Terminal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ВИРТУАЛЬНЫЙ КРИПТО-ТЕРМИНАЛ LOGS",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberPrimary
            )
            IconButton(
                onClick = { viewModel.clearConsole() },
                modifier = Modifier.size(28.dp).testTag("clear_terminal_btn")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear console", tint = CyberPrimary, modifier = Modifier.size(16.dp))
            }
        }

        val listState = rememberScrollState()
        LaunchedEffect(consoleLogs.size) {
            // Auto scroll console to bottom on new log!
            listState.animateScrollTo(listState.maxValue)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(CyberTerminalBg, RoundedCornerShape(12.dp))
                .border(1.dp, CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp)
                .verticalScroll(listState)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                consoleLogs.forEach { log ->
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = when {
                            log.startsWith("❌") -> CyberError
                            log.startsWith("⚠️") -> CyberYellow
                            log.startsWith("✅") -> CyberPrimary
                            log.startsWith("▶️") -> CyberAccent
                            else -> CyberPrimary.copy(alpha = 0.8f)
                        },
                        lineHeight = 15.sp
                    )
                }
                // Blinking terminal cursor
                val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "C:\\Shield_Host\\root> ", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberPrimary)
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(11.dp)
                            .background(CyberPrimary.copy(alpha = alpha))
                    )
                }
            }
        }

        // DB Operations History Logs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "БАЗА ДАННЫХ SQLite: ИСТОРИЯ ОПЕРАЦИЙ",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberTextBright
            )
            TextButton(
                onClick = { viewModel.clearAllHistory() },
                modifier = Modifier.height(28.dp).testTag("clear_history_btn")
            ) {
                Text("ОЧИСТИТЬ БАЗУ", color = CyberError, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, CyberCardBg, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("В базе данных нет зарегистрированных крипто-операций.", color = CyberTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(history) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (entry.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = "Status",
                                        tint = if (entry.success) CyberPrimary else CyberError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${entry.operation}: ${entry.sourceName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberTextBright,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = if (entry.success) "SUCCESS" else "FAIL",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.success) CyberPrimary else CyberError
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Детали: ${entry.details}",
                                fontSize = 11.sp,
                                color = CyberTextMuted,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Размер: ${entry.sizeBytes} байт | Дата: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(entry.timestamp)}",
                                fontSize = 9.sp,
                                color = CyberTextMuted.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom reusable style helpers for textfields
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CyberTerminalBg,
    unfocusedContainerColor = CyberTerminalBg,
    focusedTextColor = CyberTextBright,
    unfocusedTextColor = CyberTextBright,
    focusedBorderColor = CyberPrimary,
    unfocusedBorderColor = CyberCardBg,
    focusedLabelColor = CyberPrimary,
    unfocusedLabelColor = CyberTextMuted
)

/**
 * Generate highly secure cyberpunk passphrase (e.g., matrix-vault-subnet-582)
 */
fun generatePassphrase(): String {
    val words = listOf(
        "cyber", "shield", "crypto", "layer", "matrix", "secure", "vault", "binary", "cipher", "entropy",
        "quantum", "vector", "signal", "node", "nexus", "orbit", "omega", "alpha", "cosmic", "slate",
        "plasma", "vortex", "hologram", "proxy", "token", "packet", "subnet", "kernel", "daemon", "vertex"
    )
    return List(3) { words.random() }.joinToString("-") + "-" + (100..999).random()
}

/**
 * Calculations for Password Entropy
 */
fun getPasswordEntropy(password: String): Double {
    if (password.isEmpty()) return 0.0
    val uniqueChars = password.toSet().size
    var pool = 0
    if (password.any { it.isLowerCase() }) pool += 26
    if (password.any { it.isUpperCase() }) pool += 26
    if (password.any { it.isDigit() }) pool += 10
    if (password.any { !it.isLetterOrDigit() }) pool += 20
    if (pool == 0) pool = 1
    return password.length * (Math.log(pool.toDouble()) / Math.log(2.0))
}

@Composable
fun PgpCryptoTab(
    viewModel: CryptoViewModel,
    secureFiles: List<SecureFileItem>,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val pubKeyPem by viewModel.pgpPublicKeyPem.collectAsStateWithLifecycle()
    val privKeyPem by viewModel.pgpPrivateKeyPem.collectAsStateWithLifecycle()
    val signatureResult by viewModel.pgpSignatureResult.collectAsStateWithLifecycle()
    val signatureVerifyInput by viewModel.pgpSignatureVerifyInput.collectAsStateWithLifecycle()
    val verificationStatus by viewModel.pgpVerificationStatus.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val outputText by viewModel.outputText.collectAsStateWithLifecycle()

    var ownerName by remember { mutableStateOf("Alice") }
    var selectedKeySize by remember { mutableStateOf(2048) }
    var showPrivateKey by remember { mutableStateOf(false) }

    val pemFiles = secureFiles.filter { it.originalName.endsWith(".pem") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1: KEY GENERATION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "1. ГЕНЕРАЦИЯ PGP RSA КЛЮЧЕВОЙ ПАРЫ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberPrimary
                )
                
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Идентификатор пользователя (Owner ID)") },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("pgp_owner_id")
                )

                Text(
                    "Размер ключа RSA:",
                    fontSize = 11.sp,
                    color = CyberTextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(2048, 3072, 4096).forEach { size ->
                        val isSelected = selectedKeySize == size
                        Card(
                            onClick = { selectedKeySize = size },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CyberPrimary.copy(alpha = 0.15f) else CyberDarkBg
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyberPrimary else CyberCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$size бит",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyberPrimary else CyberTextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { viewModel.generatePgpKeys(ownerName, selectedKeySize) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("generate_pgp_keys_btn")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Generate", tint = CyberDarkBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ГЕНЕРИРОВАТЬ КЛЮЧИ RSA", color = CyberDarkBg, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // --- SECTION 2: KEYRING / ACTIVE KEYS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "2. АКТИВНЫЙ PGP КЛЮЧЕВОЙ СТЕНД",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberSecondary
                )

                // Quick Autofill from Safe files
                if (pemFiles.isNotEmpty()) {
                    Text(
                        "Быстрая загрузка из Сейфа:",
                        fontSize = 11.sp,
                        color = CyberTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pemFiles.forEach { file ->
                            Card(
                                onClick = {
                                    scope.launch {
                                        val content = viewModel.readWorkspaceFileText(file)
                                        if (content.contains("PUBLIC KEY")) {
                                            viewModel.pgpPublicKeyPem.value = content
                                            Toast.makeText(context, "Публичный ключ загружен!", Toast.LENGTH_SHORT).show()
                                        } else if (content.contains("PRIVATE KEY")) {
                                            viewModel.pgpPrivateKeyPem.value = content
                                            Toast.makeText(context, "Приватный ключ загружен!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Неизвестный PEM формат", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = CyberDarkBg),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = file.originalName,
                                    fontSize = 10.sp,
                                    color = CyberTextBright,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Public Key Field
                OutlinedTextField(
                    value = pubKeyPem,
                    onValueChange = { viewModel.pgpPublicKeyPem.value = it },
                    label = { Text("ОТКРЫТЫЙ КЛЮЧ (PUBLIC KEY PEM)") },
                    minLines = 3,
                    maxLines = 5,
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("pgp_pub_field")
                )

                // Private Key Field
                OutlinedTextField(
                    value = privKeyPem,
                    onValueChange = { viewModel.pgpPrivateKeyPem.value = it },
                    label = { Text("ЗАКРЫТЫЙ КЛЮЧ (PRIVATE KEY PEM)") },
                    minLines = 3,
                    maxLines = 5,
                    visualTransformation = if (showPrivateKey) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("pgp_priv_field")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showPrivateKey = !showPrivateKey },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberDarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (showPrivateKey) "СКРЫТЬ ПРИВАТНЫЙ" else "ПОКАЗАТЬ ПРИВАТНЫЙ",
                            fontSize = 10.sp,
                            color = CyberSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.pgpPublicKeyPem.value = ""
                                viewModel.pgpPrivateKeyPem.value = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberDarkBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("ОЧИСТИТЬ", fontSize = 10.sp, color = CyberError, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // --- SECTION 3: ASYMMETRIC TEXT OPERATIONS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberAccent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "3. АСИММЕТРИЧНЫЕ ОПЕРАЦИИ С ТЕКСТОМ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberAccent
                )

                Text(
                    "Использует общие текстовые поля ввода/вывода на главной вкладке 'ТЕКСТ'.",
                    fontSize = 11.sp,
                    color = CyberTextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.encryptTextPgp() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("pgp_encrypt_text_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Encrypt", tint = CyberDarkBg, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ШИФРОВАТЬ", color = CyberDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { viewModel.decryptTextPgp() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("pgp_decrypt_text_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Decrypt", tint = CyberDarkBg, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("РАСШИФРОВАТЬ", color = CyberDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.signTextPgp() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberYellow),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("pgp_sign_text_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Sign", tint = CyberDarkBg, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ПОДПИСАТЬ", color = CyberDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { viewModel.verifyTextPgp() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("pgp_verify_text_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Verify", tint = CyberDarkBg, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ВЕРИФИЦИРОВАТЬ", color = CyberDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // --- SECTION 4: SIGNATURE VERIFICATION PANEL ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "4. ВЕРИФИКАЦИОННЫЙ СТЕНД ПОДПИСЕЙ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberYellow
                )

                // Signature Result Box (Output)
                if (signatureResult.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Сгенерированная подпись (Base64):", fontSize = 10.sp, color = CyberTextMuted, fontFamily = FontFamily.Monospace)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberTerminalBg, RoundedCornerShape(8.dp))
                                .border(1.dp, CyberCardBg, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = signatureResult,
                                    fontSize = 10.sp,
                                    color = CyberPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PGP Signature", signatureResult)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Подпись скопирована!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberDarkBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp).align(Alignment.End)
                        ) {
                            Text("КОПИРОВАТЬ", fontSize = 9.sp, color = CyberPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Input for verification
                OutlinedTextField(
                    value = signatureVerifyInput,
                    onValueChange = { viewModel.pgpSignatureVerifyInput.value = it },
                    label = { Text("ПОДПИСЬ ДЛЯ ПРОВЕРКИ (Base64)", fontFamily = FontFamily.Monospace) },
                    minLines = 2,
                    maxLines = 4,
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("pgp_verify_sig_input")
                )

                // Verification Status Display
                if (verificationStatus.isNotEmpty()) {
                    val statusColor = when (verificationStatus) {
                        "SUCCESS" -> CyberPrimary
                        "FAILED" -> CyberError
                        else -> CyberYellow
                    }
                    val statusText = when (verificationStatus) {
                        "SUCCESS" -> "ПОДПИСЬ ПОДЛИННАЯ (ЦЕЛОСТНОСТЬ ДАННЫХ ПОДТВЕРЖДЕНА)"
                        "FAILED" -> "ВЕРИФИКАЦИЯ ОТКЛОНЕНА (ДАННЫЕ ИЛИ ПОДПИСЬ ИЗМЕНЕНЫ)"
                        else -> "ОШИБКА ОБРАБОТКИ"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HashingTab(
    viewModel: CryptoViewModel,
    secureFiles: List<SecureFileItem>,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val hashingAlgo by viewModel.hashingAlgorithm.collectAsStateWithLifecycle()
    val hashingKey by viewModel.hashingKey.collectAsStateWithLifecycle()
    val hashingInput by viewModel.hashingInput.collectAsStateWithLifecycle()
    val hashingOutput by viewModel.hashingOutput.collectAsStateWithLifecycle()
    val isHmacEnabled by viewModel.isHmacEnabled.collectAsStateWithLifecycle()

    var isTextMode by remember { mutableStateOf(true) }
    var selectedFileIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- INPUT MODE ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "1. ВЫБОР ИСТОЧНИКА ДАННЫХ ДЛЯ ХЭШИРОВАНИЯ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        onClick = { isTextMode = true },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTextMode) CyberPrimary.copy(alpha = 0.15f) else CyberDarkBg
                        ),
                        border = BorderStroke(1.dp, if (isTextMode) CyberPrimary else CyberDarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("ТЕКСТ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isTextMode) CyberPrimary else CyberTextMuted, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Card(
                        onClick = { isTextMode = false },
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isTextMode) CyberPrimary.copy(alpha = 0.15f) else CyberDarkBg
                        ),
                        border = BorderStroke(1.dp, if (!isTextMode) CyberPrimary else CyberDarkBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("ФАЙЛ СЕЙФА", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!isTextMode) CyberPrimary else CyberTextMuted, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                if (isTextMode) {
                    OutlinedTextField(
                        value = hashingInput,
                        onValueChange = { viewModel.hashingInput.value = it },
                        label = { Text("Текст для хэширования", fontFamily = FontFamily.Monospace) },
                        minLines = 3,
                        maxLines = 6,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("hashing_text_input")
                    )
                } else {
                    if (secureFiles.isEmpty()) {
                        Text(
                            text = "Сейф пуст. Пожалуйста, импортируйте или создайте файлы на вкладке 'СЕЙФ'!",
                            color = CyberError,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text("Выберите файл из Сейфа:", fontSize = 11.sp, color = CyberTextMuted, fontFamily = FontFamily.Monospace)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            secureFiles.forEachIndexed { idx, file ->
                                val isSelected = selectedFileIndex == idx
                                Card(
                                    onClick = { selectedFileIndex = idx },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) CyberSecondary.copy(alpha = 0.15f) else CyberDarkBg
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) CyberSecondary else CyberCardBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (file.isEncrypted) Icons.Default.Lock else Icons.Default.Menu,
                                            contentDescription = "File",
                                            tint = if (isSelected) CyberSecondary else CyberTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = file.originalName,
                                            fontSize = 12.sp,
                                            color = if (isSelected) CyberSecondary else CyberTextBright,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- CRYPTO ALGORITHMS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "2. КРИПТОГРАФИЧЕСКИЙ АЛГОРИТМ И КЛЮЧ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SHA-256", "SHA-512", "MD5", "SHA-1").forEach { algo ->
                        val isSelected = hashingAlgo == algo
                        Card(
                            onClick = { viewModel.hashingAlgorithm.value = algo },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CyberSecondary.copy(alpha = 0.15f) else CyberDarkBg
                            ),
                            border = BorderStroke(1.dp, if (isSelected) CyberSecondary else CyberDarkBg),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(algo, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) CyberSecondary else CyberTextMuted, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isHmacEnabled,
                            onCheckedChange = { viewModel.isHmacEnabled.value = it },
                            colors = CheckboxDefaults.colors(checkedColor = CyberSecondary)
                        )
                        Text("Включить режим HMAC", color = CyberTextBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                if (isHmacEnabled) {
                    OutlinedTextField(
                        value = hashingKey,
                        onValueChange = { viewModel.hashingKey.value = it },
                        label = { Text("Секретный ключ HMAC (Secret Key)", fontFamily = FontFamily.Monospace) },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("hashing_key_field")
                    )
                }
            }
        }

        // --- ACTIONS & OUTPUT ---
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberAccent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val file = if (isTextMode) null else secureFiles.getOrNull(selectedFileIndex)
                        viewModel.calculateHashing(isTextMode, file)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("calculate_hash_btn")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Compute", tint = CyberDarkBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ВЫЧИСЛИТЬ ХЭШ-СУММУ", color = CyberDarkBg, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                if (hashingOutput.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Результат вычисления (HEX):", fontSize = 11.sp, color = CyberTextMuted, fontFamily = FontFamily.Monospace)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberTerminalBg, RoundedCornerShape(8.dp))
                                .border(1.dp, CyberCardBg, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = hashingOutput,
                                    fontSize = 11.sp,
                                    color = CyberPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Hash Output", hashingOutput)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Хэш скопирован!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberDarkBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp).align(Alignment.End)
                        ) {
                            Text("КОПИРОВАТЬ РЕЗУЛЬТАТ", fontSize = 10.sp, color = CyberPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
