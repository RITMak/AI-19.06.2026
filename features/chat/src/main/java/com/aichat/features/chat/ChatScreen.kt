package com.aichat.features.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aichat.core.domain.model.ChatStage
import com.aichat.core.domain.model.MessageDomain
import com.aichat.core.model.MessageType
import com.aichat.core.model.Role
import com.aichat.core.ui.theme.AssistantBubble
import com.aichat.core.ui.theme.UserBubble

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showContextDialog by remember { mutableStateOf(false) }
    var showMemoryInfoDialog by remember { mutableStateOf(false) }

    // Long-press menu state
    var menuMessage by remember { mutableStateOf<MessageDomain?>(null) }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
    }

    // Auto-scroll — check if guard (last item) is visible
    val isGuardVisible by remember {
        derivedStateOf {
            val vi = listState.layoutInfo.visibleItemsInfo
            vi.any { it.key == "guard" }
        }
    }
    LaunchedEffect(uiState.streamingText, uiState.messages.size, uiState.hasStageCompleteMarker) {
        val shouldScroll = uiState.streamingText != null || uiState.isLoading || uiState.hasStageCompleteMarker
        if (shouldScroll && !isGuardVisible) {
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex >= 0) {
                listState.animateScrollToItem(lastIndex)
            }
        }
    }

    // Memory info dialog
    if (showMemoryInfoDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryInfoDialog = false },
            title = { Text("🧠 Контекст памяти") },
            text = {
                Column {
                    if (uiState.taskName != null) {
                        Text("📋 Задача: ${uiState.taskName}")
                        Spacer(Modifier.height(8.dp))
                    }
                    if (uiState.userInfo.isNotEmpty()) {
                        Text("👤 Информация о пользователе:", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        uiState.userInfo.forEach { (key, value) ->
                            Text("• $key: $value")
                        }
                    } else {
                        Text("Нет данных о пользователе")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemoryInfoDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Context settings dialog
    if (showContextDialog) {
        ContextSettingsDialog(
            currentSummaryEvery = uiState.summaryEvery,
            onDismiss = { showContextDialog = false },
            onApply = { summaryEvery ->
                viewModel.updateSummaryEvery(summaryEvery)
            }
        )
    }

    // Overflow menu state
    var showOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.modelName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildStageText(uiState),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Text("⋮", style = MaterialTheme.typography.titleMedium)
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🧠 Память") },
                                onClick = {
                                    showOverflowMenu = false
                                    showMemoryInfoDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("⚙️ Summary settings") },
                                onClick = {
                                    showOverflowMenu = false
                                    showContextDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column {
                    val ctxLimit = uiState.contextLimit
                    Text(
                        text = if (ctxLimit > 0) {
                            "Context: ${ctxLimit / 1000}K | Used: ${uiState.totalTokens}"
                        } else {
                            "Used: ${uiState.totalTokens}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "Chat total: $${"%.6f".format(uiState.totalCost)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    val error = uiState.error
                    if (error != null) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                viewModel.updateMessageText(it)
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Сообщение...") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            shape = RoundedCornerShape(24.dp),
                            enabled = !uiState.isLoading,
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() && !uiState.isLoading) {
                                        viewModel.sendMessage()
                                        inputText = ""
                                    }
                                }
                            ),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage()
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !uiState.isLoading,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send"
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Render messages
                items(uiState.messages) { message ->
                    when (message.messageType) {
                        MessageType.SUMMARY -> SummaryBubble(
                            message = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                        MessageType.STAGE_CHANGE -> StageChangeBubble(
                            message = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                        MessageType.FSM_REJECT -> FsmRejectBubble(
                            message = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                        else -> MessageBubble(
                            message = message,
                            onLongPress = { menuMessage = message },
                            showStageButtons = false,
                            onConfirmStage = { viewModel.confirmStageTransition() },
                            onRejectStage = { viewModel.rejectStageTransition() }
                        )
                    }
                }

                // Stage confirm buttons (shown after assistant message with [STAGE_COMPLETE])
                if (uiState.hasStageCompleteMarker && !uiState.isLoading && uiState.streamingText == null) {
                    item(key = "stage_confirm_buttons") {
                        if (!uiState.planConfirmed) {
                            // Plan not yet confirmed — show "Принять план" / "Продолжить уточнение"
                            PlanConfirmButtons(
                                onConfirm = { viewModel.confirmPlan() },
                                onReject = { viewModel.rejectStageTransition() }
                            )
                        } else {
                            // Plan confirmed — show "Принять" / "Повторить этап"
                            StageConfirmButtons(
                                confirmInfo = uiState.showStageConfirm,
                                onConfirm = { viewModel.confirmStageTransition() },
                                onReject = { viewModel.repeatStage() }
                            )
                        }
                    }
                }
                if (uiState.streamingText != null) {
                    item(key = "streaming") {
                        StreamingBubble(
                            text = uiState.streamingText!!,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }

                if (uiState.isLoading) {
                    item(key = "spinner") {
                        SpinnerBubble(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }

                item(key = "guard") {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }

            // Long-press menu overlay
            if (menuMessage != null) {
                MessageBubbleMenu(
                    message = menuMessage!!,
                    onDismiss = { menuMessage = null },
                    onEditAndResend = {
                        inputText = menuMessage!!.text
                        menuMessage = null
                    },
                    onCopyText = {
                        clipboardManager.setText(AnnotatedString(menuMessage!!.text))
                        menuMessage = null
                    }
                )
            }
        }
    }
}

private fun buildStageText(uiState: ChatUiState): String {
    return uiState.currentStage.label
}

@Composable
fun StageConfirmButtons(
    confirmInfo: StageConfirmInfo?,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    if (confirmInfo == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            ) {
                Text(confirmInfo.confirmLabel, fontSize = 13.sp)
            }
            TextButton(
                onClick = onReject,
                modifier = Modifier.weight(1f)
            ) {
                Text(confirmInfo.rejectLabel, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SummaryBubble(
    message: MessageDomain,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.wrapContentWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "📝",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun FsmRejectBubble(
    message: MessageDomain,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFF3E0),
            modifier = Modifier.wrapContentWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}

@Composable
fun SpinnerBubble(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            color = AssistantBubble,
            modifier = Modifier.widthIn(max = 120.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF444444)
                )
            }
        }
    }
}

@Composable
fun StreamingBubble(
    text: String,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            color = AssistantBubble,
            modifier = Modifier.widthIn(max = screenWidth * 0.8f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun StageChangeBubble(
    message: MessageDomain,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageDomain,
    onLongPress: () -> Unit,
    showStageButtons: Boolean = false,
    onConfirmStage: () -> Unit = {},
    onRejectStage: () -> Unit = {}
) {
    val isUser = message.role == Role.USER
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* no action on tap */ },
                onLongClick = onLongPress
            ),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) UserBubble else AssistantBubble,
            modifier = Modifier.widthIn(max = screenWidth * 0.8f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) Color.White else Color.Black
                )
                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "↑${message.totalTokens}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "↑$${"%.6f".format(message.promptCost)} ↓$${"%.6f".format(message.completionCost)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun PlanConfirmButtons(
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            ) {
                Text("📋 Принять план", fontSize = 13.sp)
            }
            TextButton(
                onClick = onReject,
                modifier = Modifier.weight(1f)
            ) {
                Text("🔄 Продолжить уточнение", fontSize = 13.sp)
            }
        }
    }
}
