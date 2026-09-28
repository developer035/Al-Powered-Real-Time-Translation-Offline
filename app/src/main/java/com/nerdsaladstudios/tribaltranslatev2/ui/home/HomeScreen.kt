package com.nerdsaladstudios.tribaltranslatev2.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nerdsaladstudios.tribaltranslatev2.domain.AudioAvailabilityState
import com.nerdsaladstudios.tribaltranslatev2.domain.TranslationLanguage
import com.nerdsaladstudios.tribaltranslatev2.ui.theme.TribalTranslateV2Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
        if (isGranted) {
            viewModel.onMicClicked(hasRecordPermission = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tribal Translate",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Offline Voice Translator • Classroom MVP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        HomeScreenContent(
            uiState = uiState,
            onSwapLanguages = viewModel::swapLanguages,
            onSourceTextChange = viewModel::updateSourceText,
            onClearClick = viewModel::clearTexts,
            onMicClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    viewModel.onMicClicked(hasRecordPermission = true)
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (uiState.showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPermissionDialog,
            title = { Text("Microphone Permission Required") },
            text = {
                Text("This app needs microphone access to listen to speech for offline Hindi ↔ Santali voice translation.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissPermissionDialog()
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPermissionDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onSwapLanguages: () -> Unit,
    onSourceTextChange: (String) -> Unit,
    onClearClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Direction selector card
        LanguageSelectorCard(
            sourceLanguage = uiState.sourceLanguage,
            targetLanguage = uiState.targetLanguage,
            onSwapClick = onSwapLanguages
        )

        // Status or recording alert indicator
        RecordingStatusIndicator(
            isRecording = uiState.isRecording,
            statusMessage = uiState.statusMessage,
            sourceLanguage = uiState.sourceLanguage
        )

        // Source Text Card
        SourceTextCard(
            language = uiState.sourceLanguage,
            text = uiState.sourceText,
            onTextChange = onSourceTextChange,
            onClearClick = onClearClick,
            isRecording = uiState.isRecording
        )

        // Translated Text Card
        TranslatedTextCard(
            language = uiState.targetLanguage,
            translatedText = uiState.translatedText,
            audioState = uiState.audioState
        )

        Spacer(modifier = Modifier.weight(1f))

        // Mic FAB area
        MicActionButton(
            isRecording = uiState.isRecording,
            onClick = onMicClick
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun LanguageSelectorCard(
    sourceLanguage: TranslationLanguage,
    targetLanguage: TranslationLanguage,
    onSwapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LanguageBadge(
                language = sourceLanguage,
                isSource = true,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onSwapClick,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap Languages",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            LanguageBadge(
                language = targetLanguage,
                isSource = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LanguageBadge(
    language: TranslationLanguage,
    isSource: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (isSource) Alignment.Start else Alignment.End
    ) {
        Text(
            text = if (isSource) "FROM" else "TO",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${language.displayName} (${language.nativeName})",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = language.scriptName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun RecordingStatusIndicator(
    isRecording: Boolean,
    statusMessage: String?,
    sourceLanguage: TranslationLanguage,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isRecording) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        label = "statusBgColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isRecording) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        label = "statusTextColor"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRecording) {
                PulsingMicIcon(color = textColor)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Listening to ${sourceLanguage.displayName} (${sourceLanguage.nativeName})...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = textColor
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusMessage ?: "Tap the microphone button to start speaking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun PulsingMicIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .background(color.copy(alpha = 0.3f), CircleShape)
        )
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Recording",
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SourceTextCard(
    language: TranslationLanguage,
    text: String,
    onTextChange: (String) -> Unit,
    onClearClick: () -> Unit,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Source Speech / Text (${language.displayName})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (text.isNotEmpty()) {
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear text",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (isRecording) {
                            "Listening... speak now in ${language.displayName}"
                        } else {
                            "Speak or type text in ${language.displayName} (${language.nativeName})..."
                        }
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun TranslatedTextCard(
    language: TranslationLanguage,
    translatedText: String,
    audioState: AudioAvailabilityState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Translation (${language.displayName})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = language.scriptName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                if (translatedText.isEmpty()) {
                    Text(
                        text = "Translated text will appear here...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = translatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio availability badge / indicator
            AudioStatusBanner(
                audioState = audioState,
                targetLanguage = language
            )
        }
    }
}

@Composable
fun AudioStatusBanner(
    audioState: AudioAvailabilityState,
    targetLanguage: TranslationLanguage,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = audioState != AudioAvailabilityState.NONE,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = when (audioState) {
                AudioAvailabilityState.NO_MATCH -> MaterialTheme.colorScheme.surfaceContainerHighest
                AudioAvailabilityState.AVAILABLE -> MaterialTheme.colorScheme.primaryContainer
                AudioAvailabilityState.PLAYING -> MaterialTheme.colorScheme.tertiaryContainer
                AudioAvailabilityState.NONE -> MaterialTheme.colorScheme.surface
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (audioState) {
                        AudioAvailabilityState.NO_MATCH -> Icons.Default.VolumeOff
                        else -> Icons.Default.VolumeUp
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = when (audioState) {
                        AudioAvailabilityState.NO_MATCH -> MaterialTheme.colorScheme.onSurfaceVariant
                        AudioAvailabilityState.AVAILABLE -> MaterialTheme.colorScheme.onPrimaryContainer
                        AudioAvailabilityState.PLAYING -> MaterialTheme.colorScheme.onTertiaryContainer
                        AudioAvailabilityState.NONE -> MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = when (audioState) {
                        AudioAvailabilityState.NO_MATCH -> {
                            if (targetLanguage == TranslationLanguage.SANTALI) {
                                "No Santali audio available for this phrase"
                            } else {
                                "Hindi audio ready (Android System TTS)"
                            }
                        }
                        AudioAvailabilityState.AVAILABLE -> "Santali audio clip available (Phrase bank)"
                        AudioAvailabilityState.PLAYING -> "Playing Santali audio..."
                        AudioAvailabilityState.NONE -> ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when (audioState) {
                        AudioAvailabilityState.NO_MATCH -> MaterialTheme.colorScheme.onSurfaceVariant
                        AudioAvailabilityState.AVAILABLE -> MaterialTheme.colorScheme.onPrimaryContainer
                        AudioAvailabilityState.PLAYING -> MaterialTheme.colorScheme.onTertiaryContainer
                        AudioAvailabilityState.NONE -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
fun MicActionButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isRecording) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "micButtonColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isRecording) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        label = "micContentColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            containerColor = buttonColor,
            contentColor = contentColor
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop Listening" else "Start Listening",
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isRecording) "Tap to stop" else "Tap to speak",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    TribalTranslateV2Theme {
        HomeScreenContent(
            uiState = HomeUiState(
                sourceLanguage = TranslationLanguage.HINDI,
                targetLanguage = TranslationLanguage.SANTALI,
                isRecording = false,
                sourceText = "अपनी किताबें खोलो",
                translatedText = "ᱟᱢᱟᱜ ᱯᱩᱛᱷᱤ ᱠᱷᱩᱞᱟᱹᱭ",
                audioState = AudioAvailabilityState.NO_MATCH,
                statusMessage = "Tap microphone to speak"
            ),
            onSwapLanguages = {},
            onSourceTextChange = {},
            onClearClick = {},
            onMicClick = {}
        )
    }
}
