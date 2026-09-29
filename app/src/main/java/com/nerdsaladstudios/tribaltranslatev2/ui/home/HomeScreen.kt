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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nerdsaladstudios.tribaltranslatev2.domain.AudioAvailabilityState
import com.nerdsaladstudios.tribaltranslatev2.domain.TranslationLanguage
import com.nerdsaladstudios.tribaltranslatev2.ui.theme.TribalTranslateV2Theme
import com.nerdsaladstudios.tribaltranslatev2.ui.worksheet.WorksheetGeneratorScreen

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
            viewModel.onMicClicked(context, hasRecordPermission = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = "Vani Setu Logo",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Vani Setu",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Offline Voice Translator • Hindi ↔ Santali",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )

                PrimaryTabRow(
                    selectedTabIndex = uiState.selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Tab(
                        selected = uiState.selectedTabIndex == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = {
                            Text(
                                "Voice Translation",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Voice Translation Module"
                            )
                        }
                    )
                    Tab(
                        selected = uiState.selectedTabIndex == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = {
                            Text(
                                "Worksheets",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Worksheet Generator Module"
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (uiState.selectedTabIndex == 0) {
            HomeScreenContent(
                uiState = uiState,
                onSwapLanguages = viewModel::swapLanguages,
                onSourceTextChange = viewModel::updateSourceText,
                onTranslatedTextChange = viewModel::updateTranslatedText,
                onClearClick = viewModel::clearTexts,
                onMicClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        viewModel.onMicClicked(context, hasRecordPermission = true)
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            WorksheetGeneratorScreen(
                uiState = uiState,
                onClassSelect = viewModel::selectClass,
                onSubjectSelect = viewModel::selectSubject,
                onGenerateClick = viewModel::generateWorksheet,
                onResetClick = viewModel::resetWorksheetGenerator,
                modifier = Modifier.padding(innerPadding)
            )
        }
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
    onTranslatedTextChange: (String) -> Unit,
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
        LanguageSelectorCard(
            sourceLanguage = uiState.sourceLanguage,
            targetLanguage = uiState.targetLanguage,
            onSwapClick = onSwapLanguages
        )

        RecordingStatusIndicator(
            isRecording = uiState.isRecording,
            isPlayingAudioAnimation = uiState.isPlayingAudioAnimation,
            statusMessage = uiState.statusMessage,
            sourceLanguage = uiState.sourceLanguage
        )

        SourceTextCard(
            language = uiState.sourceLanguage,
            text = uiState.sourceText,
            onTextChange = onSourceTextChange,
            onClearClick = onClearClick,
            isRecording = uiState.isRecording
        )

        TranslatedTextCard(
            language = uiState.targetLanguage,
            translatedText = uiState.translatedText,
            onTranslatedTextChange = onTranslatedTextChange,
            audioState = uiState.audioState
        )

        Spacer(modifier = Modifier.weight(1f))

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
                    contentDescription = "Trigger Audio Output Animation",
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
    isPlayingAudioAnimation: Boolean,
    statusMessage: String?,
    sourceLanguage: TranslationLanguage,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isRecording -> MaterialTheme.colorScheme.errorContainer
            isPlayingAudioAnimation -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        label = "statusBgColor"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isRecording -> MaterialTheme.colorScheme.onErrorContainer
            isPlayingAudioAnimation -> MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onSecondaryContainer
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
            when {
                isRecording -> {
                    PulsingMicIcon(color = textColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Listening to ${sourceLanguage.displayName} (${sourceLanguage.nativeName})...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = textColor
                    )
                }
                isPlayingAudioAnimation -> {
                    PulsingAudioWaveIcon(color = textColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Playing Santali audio output...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = textColor
                    )
                }
                else -> {
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
fun PulsingAudioWaveIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWavePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "audioWaveScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .background(color.copy(alpha = 0.35f), CircleShape)
        )
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "Audio Output Waveform",
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
    onTranslatedTextChange: (String) -> Unit,
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

            OutlinedTextField(
                value = translatedText,
                onValueChange = onTranslatedTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Type or edit Santali text in Ol Chiki script...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            AudioStatusBanner(
                audioState = audioState
            )
        }
    }
}

@Composable
fun AudioStatusBanner(
    audioState: AudioAvailabilityState,
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
                    imageVector = Icons.Default.VolumeUp,
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
                        AudioAvailabilityState.PLAYING -> "Playing Santali audio output..."
                        else -> "Santali audio available"
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
                contentDescription = if (isRecording) "Stop Listening" else "Tap to speak",
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
                audioState = AudioAvailabilityState.AVAILABLE,
                statusMessage = "Tap microphone to speak"
            ),
            onSwapLanguages = {},
            onSourceTextChange = {},
            onTranslatedTextChange = {},
            onClearClick = {},
            onMicClick = {}
        )
    }
}
