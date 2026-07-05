package cx.aswin.boxcast.feature.explore

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cx.aswin.boxcast.core.designsystem.components.OptimizedImage
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import cx.aswin.boxcast.core.network.model.DailyCuriosityDto
import kotlin.math.roundToInt

@Composable
fun CuriosityCardStack(
    questions: List<DailyCuriosityDto>,
    isCurrentlyPlaying: (String) -> Boolean,
    onSwipeLeft: (DailyCuriosityDto) -> Unit,
    onSwipeRight: (DailyCuriosityDto) -> Unit,
    onPlayClick: (DailyCuriosityDto) -> Unit,
    modifier: Modifier = Modifier
) {
    if (questions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(340.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No more curiosities for today!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp),
        contentAlignment = Alignment.Center
    ) {
        // Render up to 3 cards in stack representation (reversed order so top is drawn last)
        val cardsToShow = questions.take(3).reversed()

        cardsToShow.forEach { daily ->
            val isTopCard = daily.episode.id.toString() == questions.first().episode.id.toString()

            if (isTopCard) {
                val swipeState = rememberSwipeableCardState(key = daily.episode.id) { direction ->
                    if (direction == SwipeDirection.Left) {
                        onSwipeLeft(daily)
                    } else {
                        onSwipeRight(daily)
                    }
                }

                var isFlipped by remember(daily.episode.id) { mutableStateOf(false) }

                val cardRotationY by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(durationMillis = 400),
                    label = "CardFlip"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                swipeState.offset.value.x.roundToInt(),
                                swipeState.offset.value.y.roundToInt()
                            )
                        }
                        .graphicsLayer {
                            rotationZ = (swipeState.offset.value.x / 40f)
                            this.rotationY = cardRotationY
                            cameraDistance = 12f * density
                        }
                        .pointerInput(daily.episode.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    val offsetX = swipeState.offset.value.x
                                    if (offsetX > 400f) {
                                        swipeState.swipe(SwipeDirection.Right)
                                    } else if (offsetX < -400f) {
                                        swipeState.swipe(SwipeDirection.Left)
                                    } else {
                                        swipeState.reset()
                                    }
                                },
                                onDragCancel = {
                                    swipeState.reset()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    swipeState.drag(dragAmount)
                                }
                            )
                        }
                ) {
                    CuriosityCardContent(
                        daily = daily,
                        isCurrentlyPlaying = isCurrentlyPlaying(daily.episode.id.toString()),
                        isFlipped = isFlipped,
                        rotationY = cardRotationY,
                        onFlipToggle = { isFlipped = !isFlipped },
                        onPlayClick = { onPlayClick(daily) }
                    )
                }
            } else {
                // Lower cards in stack
                val stackLevel = questions.indexOf(daily) // 1 or 2
                val scale = if (stackLevel == 1) 0.95f else 0.90f
                val verticalOffset = if (stackLevel == 1) 12.dp else 24.dp

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = verticalOffset)
                        .scale(scale)
                        .graphicsLayer {
                            alpha = if (stackLevel == 1) 0.85f else 0.6f
                        }
                ) {
                    CuriosityCardContent(
                        daily = daily,
                        isCurrentlyPlaying = false,
                        isFlipped = false,
                        rotationY = 0f,
                        onFlipToggle = {},
                        onPlayClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun CuriosityCardContent(
    daily: DailyCuriosityDto,
    isCurrentlyPlaying: Boolean,
    isFlipped: Boolean,
    rotationY: Float,
    onFlipToggle: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxSize()
            .expressiveClickable(onClick = onFlipToggle)
    ) {
        if (rotationY > 90f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.rotationY = 180f
                    }
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "EXPLANATION",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = daily.explanation ?: "No explanation available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val showArt = daily.episode.image ?: daily.episode.feedImage ?: ""
                        OptimizedImage(
                            url = showArt,
                            proxyWidth = 100,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = daily.episode.feedTitle ?: "Podcast Episode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val durationSec = daily.episode.duration
                            val durationMin = if (durationSec != null && durationSec > 0) {
                                "${durationSec / 60} min"
                            } else {
                                "Unknown duration"
                            }
                            Text(
                                text = durationMin,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalButton(
                            onClick = onPlayClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = if (isCurrentlyPlaying) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isCurrentlyPlaying) Icons.Filled.VolumeUp else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCurrentlyPlaying) "Playing" else "Listen",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "CURIOSITY OF THE DAY",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = daily.question,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 34.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap to flip details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Swipe to skip/queue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
