package com.aichat.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onClearHistory: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var openRouterKey by remember(uiState.openRouterKey) { mutableStateOf(uiState.openRouterKey) }
    var deepSeekKey by remember(uiState.deepSeekKey) { mutableStateOf(uiState.deepSeekKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "API ключи",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = openRouterKey,
                onValueChange = {
                    openRouterKey = it
                    viewModel.saveOpenRouterKey(it)
                },
                label = { Text("OpenRouter API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default
            )

            OutlinedTextField(
                value = deepSeekKey,
                onValueChange = {
                    deepSeekKey = it
                    viewModel.saveDeepSeekKey(it)
                },
                label = { Text("DeepSeek API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Данные",
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = {
                    viewModel.clearHistory()
                    onClearHistory()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Очистить историю")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "О приложении",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "AiChat v${uiState.appVersion}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}