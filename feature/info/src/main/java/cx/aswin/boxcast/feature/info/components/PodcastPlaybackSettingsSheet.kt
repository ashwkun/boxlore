package cx.aswin.boxcast.feature.info.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastPlaybackSettingsSheet(
    podcastTitle: String,
    isSubscribed: Boolean,
    globalSkipBeginningMs: Long,
    globalSkipEndingMs: Long,
    skipBeginningOverrideMs: Long?,
    skipEndingOverrideMs: Long?,
    onUseAppDefaultsChange: (Boolean) -> Unit,
    onSkipBeginningOverrideChange: (Long) -> Unit,
    onSkipEndingOverrideChange: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val usesDefaults = skipBeginningOverrideMs == null && skipEndingOverrideMs == null
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Playback for this show",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = podcastTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use app defaults", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (isSubscribed) {
                            "Beginning ${durationLabel(globalSkipBeginningMs)} · Ending ${durationLabel(globalSkipEndingMs)}"
                        } else {
                            "Subscribe to customize playback for this show"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = usesDefaults,
                    enabled = isSubscribed,
                    onCheckedChange = onUseAppDefaultsChange,
                )
            }
            HorizontalDivider()
            PodcastDurationSlider(
                title = "Skip beginning",
                valueMs = skipBeginningOverrideMs ?: globalSkipBeginningMs,
                enabled = isSubscribed && !usesDefaults,
                onValueCommitted = onSkipBeginningOverrideChange,
            )
            PodcastDurationSlider(
                title = "Skip ending",
                valueMs = skipEndingOverrideMs ?: globalSkipEndingMs,
                enabled = isSubscribed && !usesDefaults,
                onValueCommitted = onSkipEndingOverrideChange,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Seek-button increments are app-wide and can be changed in Settings → Playback.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PodcastDurationSlider(
    title: String,
    valueMs: Long,
    enabled: Boolean,
    onValueCommitted: (Long) -> Unit,
) {
    val valueSeconds = (valueMs / 1_000L).toInt().coerceIn(0, 300)
    var pendingValue by remember(valueSeconds) { mutableFloatStateOf(valueSeconds.toFloat()) }
    val snappedSeconds = ((pendingValue / 5f).roundToInt() * 5).coerceIn(0, 300)
    val label = if (snappedSeconds == 0) "Off" else "$snappedSeconds seconds"

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = pendingValue,
            onValueChange = { pendingValue = it },
            onValueChangeFinished = { onValueCommitted(snappedSeconds * 1_000L) },
            enabled = enabled,
            valueRange = 0f..300f,
            steps = 0,
            modifier = Modifier.semantics {
                contentDescription = "$title, $label"
            },
        )
    }
}

private fun durationLabel(valueMs: Long): String =
    if (valueMs <= 0L) "Off" else "${valueMs / 1_000L}s"
