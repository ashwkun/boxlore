package cx.aswin.boxcast.feature.onboarding

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import android.os.Build
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cx.aswin.boxcast.core.designsystem.theme.ExpressiveShapes
import cx.aswin.boxcast.core.designsystem.components.OptimizedImage
import cx.aswin.boxcast.core.designsystem.theme.expressiveClickable
import cx.aswin.boxcast.core.designsystem.components.BoxCastLoader
import cx.aswin.boxcast.core.designsystem.components.LogRecomposition
import cx.aswin.boxcast.core.model.Podcast
import cx.aswin.boxcast.core.designsystem.R
import cx.aswin.boxcast.core.network.model.OnboardingCurriculumRowDto
import cx.aswin.boxcast.core.network.model.toPodcast
import cx.aswin.boxcast.core.network.model.toEpisode
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked

// Genre data matching GenreSelector.kt
data class GenreItem(val label: String, val value: String, val icon: ImageVector)

val ONBOARDING_GENRES = listOf(
    GenreItem("News", "News", Icons.Rounded.Newspaper),
    GenreItem("Tech", "Technology", Icons.Rounded.Computer),
    GenreItem("Business", "Business", Icons.Rounded.Work),
    GenreItem("Comedy", "Comedy", Icons.Rounded.SentimentVerySatisfied),
    GenreItem("True Crime", "True Crime", Icons.Rounded.Fingerprint),
    GenreItem("Sports", "Sports", Icons.Rounded.EmojiEvents),
    GenreItem("Health", "Health", Icons.Rounded.MonitorHeart),
    GenreItem("History", "History", Icons.Rounded.AccountBalance),
    GenreItem("Arts", "Arts", Icons.Rounded.Palette),
    GenreItem("Society", "Society & Culture", Icons.Rounded.Groups),
    GenreItem("Education", "Education", Icons.Rounded.School),
    GenreItem("Science", "Science", Icons.Rounded.Science),
    GenreItem("TV & Film", "TV & Film", Icons.Rounded.Movie),
    GenreItem("Fiction", "Fiction", Icons.Rounded.AutoStories),
    GenreItem("Music", "Music", Icons.Rounded.MusicNote),
    GenreItem("Religion", "Religion & Spirituality", Icons.Rounded.SelfImprovement),
    GenreItem("Family", "Kids & Family", Icons.Rounded.ChildCare),
    GenreItem("Leisure", "Leisure", Icons.Rounded.Weekend),
    GenreItem("Govt", "Government", Icons.Rounded.Gavel)
)

// ============================================================
// GENRE PICKER
// ============================================================

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
    onImportJson: (android.net.Uri) -> Unit = {},
    onImportOpml: (android.net.Uri) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val handleComplete = {
        viewModel.completeOnboarding(onComplete)
    }

    BackHandler(
        enabled = uiState.currentStep != OnboardingStep.WELCOME &&
                uiState.currentStep != OnboardingStep.AI_ONBOARDING
    ) {
        when (uiState.currentStep) {
            OnboardingStep.GENRES -> {
                viewModel.navigateBackToWelcome()
            }
            OnboardingStep.PODCASTS -> {
                viewModel.navigateBackFromPodcasts()
            }
            OnboardingStep.SEARCH -> {
                viewModel.navigateBackFromSearch()
            }
            else -> {}
        }
    }

    // Main content with animated transitions
    AnimatedContent(
        targetState = uiState.currentStep,
        transitionSpec = {
            (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
        },
        label = "onboarding_step"
    ) { step ->
        when (step) {
            OnboardingStep.WELCOME -> {
                LaunchedEffect(Unit) {
                    viewModel.onWelcomeScreenViewed()
                }
                WelcomeScreen(
                    onHelpMeFind = viewModel::startOnboarding,
                    onSearch = viewModel::navigateToSearch,
                    onSkip = { viewModel.skipOnboarding(onComplete) },
                    onImportJson = onImportJson,
                    onImportOpml = onImportOpml
                )
            }
            OnboardingStep.GENRES -> {
                // Analytics: Fire onboarding_started when genre screen loads
                LaunchedEffect(Unit) { viewModel.onGenreScreenViewed() }
                GenrePickerScreen(
                    selectedGenres = uiState.selectedGenres,
                    onToggleGenre = viewModel::toggleGenre,
                    onContinue = viewModel::continueToRecommendations,
                    onSearch = viewModel::navigateToSearch,
                    onSkip = { viewModel.skipOnboarding(onComplete) },
                    onImportJson = onImportJson,
                    onImportOpml = onImportOpml,
                    onImportSheetOpened = { cx.aswin.boxcast.core.data.analytics.AnalyticsHelper.trackImportSheetOpened(viewModel.getGenreScreenTimeSpent()) }
                )
            }
            OnboardingStep.PODCASTS -> {
                LaunchedEffect(Unit) {
                    viewModel.onPodcastScreenViewed()
                }
                PodcastPicksScreen(
                    podcasts = uiState.recommendedPodcasts,
                    subscribedIds = uiState.subscribedPodcastIds,
                    currentRegion = uiState.currentRegion,
                    isLoading = uiState.isLoadingPodcasts,
                    onToggleSubscription = viewModel::togglePodcastSubscription,
                    onRegionChange = viewModel::setRegion,
                    onSearch = viewModel::navigateToSearch,
                    onBack = viewModel::navigateBackFromPodcasts,
                    onDone = handleComplete,
                    onSkip = { viewModel.skipOnboarding(onComplete) },
                    onDidScroll = viewModel::onPodcastScreenScrolled
                )
            }
            OnboardingStep.SEARCH -> {
                OnboardingSearchScreen(
                    query = uiState.searchQuery,
                    results = uiState.searchResults,
                    isSearching = uiState.isSearching,
                    subscribedIds = uiState.subscribedPodcastIds,
                    onQueryChange = viewModel::updateSearchQuery,
                    onSubscribe = viewModel::subscribeFromSearch,
                    onBack = viewModel::navigateBackFromSearch,
                    onDone = handleComplete
                )
            }
            OnboardingStep.AI_ONBOARDING -> {
                AiOnboardingScreen(
                    uiState = uiState,
                    onBack = viewModel::navigateBackInAiOnboarding,
                    onOptionToggle = viewModel::toggleAiOption,
                    onCustomInputChange = viewModel::updateAiCustomInput,
                    onContinue = {
                        if (uiState.aiCustomInputText.isNotBlank()) {
                            // Text route: skip multi-turn, go straight to synthesize
                            viewModel.synthesizeAndBuildCurriculum()
                        } else if (uiState.aiCurrentTurn < 3) {
                            viewModel.sendAiTurnInput()
                        } else {
                            viewModel.synthesizeAndBuildCurriculum()
                        }
                    },
                    onFinish = {
                        viewModel.finishAiOnboarding(onComplete)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiOnboardingScreen(
    uiState: OnboardingUiState,
    onBack: () -> Unit,
    onOptionToggle: (String) -> Unit,
    onCustomInputChange: (String) -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit
) {
    if (uiState.isAiLoading) {
        AiOnboardingLoadingScreen()
    } else if (uiState.aiCurrentTurn == 4) {
        AiCurriculumPreviewScreen(
            curriculumRows = uiState.aiCurriculumRows,
            isCompleting = uiState.isCompleting,
            onBack = onBack,
            onFinish = onFinish
        )
    } else {
        AiConversationTurnScreen(
            uiState = uiState,
            onBack = onBack,
            onOptionToggle = onOptionToggle,
            onCustomInputChange = onCustomInputChange,
            onContinue = onContinue
        )
    }
}

@Composable
private fun AiOnboardingLoadingScreen() {
    val messages = listOf(
        "Tuning in to your audio frequency...",
        "Scouting the absolute best shows...",
        "Curating customized curriculum rows...",
        "Organizing your personalized playlist...",
        "Polishing the final recommendations..."
    )
    var currentMessageIdx by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            currentMessageIdx = (currentMessageIdx + 1) % messages.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BoxCastLoader.Expressive(size = 120.dp)
            Spacer(modifier = Modifier.height(32.dp))
            AnimatedContent(
                targetState = messages[currentMessageIdx],
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "loading_msg"
            ) { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

private fun getOptionIcons(option: String): Pair<ImageVector, ImageVector> {
    val lower = option.lowercase()
    return when {
        // Story / Narrative / True Crime
        lower.contains("story") || lower.contains("mystery") || lower.contains("fiction") || 
        lower.contains("crime") || lower.contains("detective") || lower.contains("thriller") || 
        lower.contains("narrative") || lower.contains("case") || lower.contains("murder") || 
        lower.contains("novel") || lower.contains("book") -> {
            Pair(Icons.Outlined.AutoStories, Icons.Rounded.AutoStories)
        }
        
        // Learn / Info / Science / Deep Dive / History / Tech / Business
        lower.contains("learn") || lower.contains("deep") || lower.contains("science") || 
        lower.contains("tech") || lower.contains("mind") || lower.contains("knowledge") || 
        lower.contains("teach") || lower.contains("educat") || lower.contains("history") || 
        lower.contains("documentary") || lower.contains("space") || lower.contains("fact") ||
        lower.contains("business") || lower.contains("career") || lower.contains("finance") ||
        lower.contains("explain") || lower.contains("intellect") || lower.contains("discover") ||
        lower.contains("explore") || lower.contains("curious") -> {
            Pair(Icons.Outlined.Lightbulb, Icons.Rounded.Lightbulb)
        }
        
        // Conversation / Talk / Comedy
        lower.contains("comedy") || lower.contains("conversation") || lower.contains("chat") || 
        lower.contains("talk") || lower.contains("host") || lower.contains("interview") || 
        lower.contains("forum") || lower.contains("banter") || lower.contains("laugh") || 
        lower.contains("humor") || lower.contains("discuss") || lower.contains("society") ||
        lower.contains("culture") -> {
            Pair(Icons.Outlined.Forum, Icons.Rounded.Forum)
        }
        
        // Relax / Calm / Sleep / Spa
        lower.contains("relax") || lower.contains("wind") || lower.contains("sooth") || 
        lower.contains("sleep") || lower.contains("spa") || lower.contains("calm") || 
        lower.contains("quiet") || lower.contains("meditat") || lower.contains("mindful") ||
        lower.contains("peace") || lower.contains("ambient") || lower.contains("nature") -> {
            Pair(Icons.Outlined.Spa, Icons.Rounded.Spa)
        }
        
        // News / Politics
        lower.contains("news") || lower.contains("daily") || lower.contains("current") || 
        lower.contains("today") || lower.contains("politic") || lower.contains("world") ||
        lower.contains("report") || lower.contains("journalism") -> {
            Pair(Icons.Outlined.Newspaper, Icons.Rounded.Newspaper)
        }
        
        // Music
        lower.contains("music") || lower.contains("song") || lower.contains("audio") || 
        lower.contains("sound") || lower.contains("melody") || lower.contains("beat") -> {
            Pair(Icons.Outlined.MusicNote, Icons.Rounded.MusicNote)
        }
        
        // Sports
        lower.contains("sport") || lower.contains("game") || lower.contains("play") || 
        lower.contains("football") || lower.contains("f1") || lower.contains("race") ||
        lower.contains("athlete") || lower.contains("match") || lower.contains("league") -> {
            Pair(Icons.Outlined.EmojiEvents, Icons.Rounded.EmojiEvents)
        }
        
        // Health / Fitness
        lower.contains("health") || lower.contains("fit") || lower.contains("body") || 
        lower.contains("well") || lower.contains("exercise") || lower.contains("medicine") ||
        lower.contains("mental") || lower.contains("doctor") -> {
            Pair(Icons.Outlined.MonitorHeart, Icons.Rounded.MonitorHeart)
        }
        
        // Default
        else -> {
            Pair(Icons.Outlined.Mic, Icons.Rounded.Mic)
        }
    }
}

@Composable
private fun GridChoiceCard(
    title: String,
    description: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    val topStartCorner by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "topStart"
    )
    val topEndCorner by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "topEnd"
    )
    val bottomEndCorner by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "bottomEnd"
    )
    val bottomStartCorner by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "bottomStart"
    )

    val cardShape = RoundedCornerShape(
        topStart = topStartCorner,
        topEnd = topEndCorner,
        bottomEnd = bottomEndCorner,
        bottomStart = bottomStartCorner
    )

    val iconRotation by animateFloatAsState(
        targetValue = if (isSelected) 12f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "iconRotation"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "iconScale"
    )

    val themeColor = when (title) {
        "Storyseeker" -> Color(0xFFE07A5F)
        "Deep Diver" -> Color(0xFF3D5A80)
        "Conversationalist" -> Color(0xFF81B29A)
        "Zen Listener" -> Color(0xFF9C89B8)
        else -> MaterialTheme.colorScheme.primary
    }

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected)
                themeColor.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        shape = cardShape,
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = if (isSelected) 6.dp else 0.dp
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) themeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .expressiveClickable { onClick() }
    ) {
        GridChoiceCardContent(
            title = title,
            description = description,
            icon = if (isSelected) selectedIcon else icon,
            isSelected = isSelected,
            themeColor = themeColor,
            iconScale = iconScale,
            iconRotation = iconRotation
        )
    }
}

@Composable
private fun GridChoiceCardContent(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    themeColor: Color,
    iconScale: Float,
    iconRotation: Float
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Large Watermark Background Icon at top-left
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = themeColor.copy(alpha = if (isSelected) 0.60f else 0.16f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-12).dp, y = (-12).dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    rotationZ = iconRotation
                }
                .size(110.dp)
        )

        // Foreground Text content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun SelectableChoiceRow(
    option: String,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    val topStartCorner by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "topStart"
    )
    val topEndCorner by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "topEnd"
    )
    val bottomEndCorner by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "bottomEnd"
    )
    val bottomStartCorner by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "bottomStart"
    )

    val cardShape = RoundedCornerShape(
        topStart = topStartCorner,
        topEnd = topEndCorner,
        bottomEnd = bottomEndCorner,
        bottomStart = bottomStartCorner
    )

    val iconRotation by animateFloatAsState(
        targetValue = if (isSelected) 10f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "iconRotation"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "iconScale"
    )

    val themeColor = when (index % 4) {
        0 -> Color(0xFF3D5A80) // Deep Diver Blue
        1 -> Color(0xFF81B29A) // Sage Green
        2 -> Color(0xFF9C89B8) // Lavender/Purple
        3 -> Color(0xFFE07A5F) // Coral/Rose
        else -> MaterialTheme.colorScheme.primary
    }

    val icons = getOptionIcons(option)
    val icon = if (isSelected) icons.second else icons.first

    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected)
                themeColor.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        shape = cardShape,
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) themeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .expressiveClickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(80.dp)
        ) {
            // Large Watermark Background Icon at top-left
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = themeColor.copy(alpha = if (isSelected) 0.35f else 0.10f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-12).dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        rotationZ = iconRotation
                    }
                    .size(85.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 28.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiConversationTurnScreen(
    uiState: OnboardingUiState,
    onBack: () -> Unit,
    onOptionToggle: (String) -> Unit,
    onCustomInputChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient background glow top-right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.08f),
                            secondaryColor.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(x = 1000f, y = -100f),
                        radius = 1200f
                    )
                )
        )
        // Ambient background glow bottom-left
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = 0.05f),
                            primaryColor.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(x = -200f, y = 1800f),
                        radius = 1200f
                    )
                )
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "Personalizing Your Feed",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    LinearProgressIndicator(
                        progress = { uiState.aiCurrentTurn / 3.0f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                }
            },
            bottomBar = {
                val canContinue = uiState.aiSelectedOptions.isNotEmpty() || uiState.aiCustomInputText.isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onContinue,
                        enabled = canContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (uiState.aiCustomInputText.isNotBlank()) "Build My Feed" 
                                   else if (uiState.aiCurrentTurn == 3) "Build My Feed" 
                                   else "Continue", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) { innerPadding ->
            val focusManager = LocalFocusManager.current
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            var isTextFieldFocused by remember { mutableStateOf(false) }

            BackHandler(enabled = true) {
                if (isTextFieldFocused) {
                    focusManager.clearFocus()
                } else {
                    onBack()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Glassmorphic Assistant Prompt Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "BOXCAST AI",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Text(
                            text = uiState.aiAssistantMessage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                val hasCustomText = uiState.aiCustomInputText.isNotBlank()
                val cardsAlpha by animateFloatAsState(
                    targetValue = if (hasCustomText) 0.35f else 1f,
                    animationSpec = tween(300),
                    label = "cardsAlpha"
                )

                if (uiState.aiCurrentTurn == 1) {
                    Text(
                        text = "Tap all that resonate with you",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.graphicsLayer { alpha = cardsAlpha }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = cardsAlpha }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                val opt = uiState.aiOptions.getOrNull(0) ?: ""
                                GridChoiceCard(title = "Storyseeker", description = "Deep narratives & mysteries", icon = Icons.Outlined.AutoStories, selectedIcon = Icons.Rounded.AutoStories, isSelected = opt.isNotEmpty() && opt in uiState.aiSelectedOptions, onClick = { if (opt.isNotEmpty()) onOptionToggle(opt) })
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                val opt = uiState.aiOptions.getOrNull(1) ?: ""
                                GridChoiceCard(title = "Deep Diver", description = "Tech, science & deep dives", icon = Icons.Outlined.Lightbulb, selectedIcon = Icons.Rounded.Lightbulb, isSelected = opt.isNotEmpty() && opt in uiState.aiSelectedOptions, onClick = { if (opt.isNotEmpty()) onOptionToggle(opt) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                val opt = uiState.aiOptions.getOrNull(2) ?: ""
                                GridChoiceCard(title = "Conversationalist", description = "Comedy, talk & banter", icon = Icons.Outlined.Forum, selectedIcon = Icons.Rounded.Forum, isSelected = opt.isNotEmpty() && opt in uiState.aiSelectedOptions, onClick = { if (opt.isNotEmpty()) onOptionToggle(opt) })
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                val opt = uiState.aiOptions.getOrNull(3) ?: ""
                                GridChoiceCard(title = "Zen Listener", description = "Calming stories & wind down", icon = Icons.Outlined.Spa, selectedIcon = Icons.Rounded.Spa, isSelected = opt.isNotEmpty() && opt in uiState.aiSelectedOptions, onClick = { if (opt.isNotEmpty()) onOptionToggle(opt) })
                            }
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.graphicsLayer { alpha = cardsAlpha }
                    ) {
                        uiState.aiOptions.forEachIndexed { index, option ->
                            SelectableChoiceRow(
                                option = option,
                                index = index,
                                isSelected = option in uiState.aiSelectedOptions,
                                onClick = { onOptionToggle(option) }
                            )
                        }
                    }
                }

                if (uiState.aiCurrentTurn == 1) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // OR divider
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Text(text = "OR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 1.5.sp)
                        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Text input
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Just describe what you like and don't like", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = uiState.aiCustomInputText,
                            onValueChange = onCustomInputChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    isTextFieldFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        coroutineScope.launch {
                                            delay(300)
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    }
                                },
                            placeholder = {
                                Text("E.g., I love true crime and sleep stories, hate sports pods...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiCurriculumPreviewScreen(
    curriculumRows: List<OnboardingCurriculumRowDto>,
    isCompleting: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient background glow top-right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.08f),
                            secondaryColor.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(x = 1000f, y = -100f),
                        radius = 1200f
                    )
                )
        )
        // Ambient background glow bottom-left
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = 0.05f),
                            primaryColor.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(x = -200f, y = 1800f),
                        radius = 1200f
                    )
                )
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = "Your Personalized Feed",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onFinish,
                        enabled = !isCompleting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isCompleting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "Finish & Listen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (curriculumRows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items generated. Click Finish to complete onboarding.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(curriculumRows) { row ->
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = row.rowTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                            
                            if (row.podcasts.isNotEmpty()) {
                                Text(
                                    text = "Podcasts",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(row.podcasts) { podcastDto ->
                                        val podcast = podcastDto.toPodcast()
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.width(130.dp)
                                        ) {
                                            Column {
                                                OptimizedImage(
                                                    url = podcast.imageUrl,
                                                    proxyWidth = 200,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .padding(6.dp)
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(14.dp))
                                                )
                                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                    Text(
                                                        text = podcast.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = podcast.artist,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (row.episodes.isNotEmpty()) {
                                Text(
                                    text = "Episodes",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(row.episodes) { episodeDto ->
                                        val episode = episodeDto.toEpisode()
                                        val minutes = if (episode.duration > 0) "${episode.duration / 60}m" else "Audio"
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.width(270.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OptimizedImage(
                                                    url = episode.imageUrl ?: "",
                                                    proxyWidth = 120,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = episode.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.PlayArrow,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = minutes,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
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
            }
        }
    }
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val CondensedGoogleSans = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    FontFamily(
        Font(
            cx.aswin.boxcast.core.designsystem.R.font.google_sans_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(700),
                FontVariation.Setting("wdth", 75f)
            )
        )
    )
} else {
    FontFamily.Default
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeScreen(
    onHelpMeFind: () -> Unit,
    onSearch: () -> Unit,
    onSkip: () -> Unit,
    onImportJson: (android.net.Uri) -> Unit,
    onImportOpml: (android.net.Uri) -> Unit
) {
    var showImportBottomSheet by remember { mutableStateOf(false) }

    val importJsonLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { onImportJson(it) } }
    )
    val importOpmlLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { onImportOpml(it) } }
    )

    val entranceProgress = remember { Animatable(0f) }
    val driftProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Run entrance and drift concurrently for a seamless transition
        launch {
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 4800, // Gentle 4.8 seconds
                    easing = LinearEasing
                )
            )
        }
        launch {
            driftProgress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 25000, // Matched with 732dp loop distance for a smooth 29dp/s speed
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }



    if (showImportBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Text(
                    text = "Import Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    onClick = {
                        showImportBottomSheet = false
                        importJsonLauncher.launch(arrayOf("application/json"))
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("boxcast Backup (.json)") },
                        supportingContent = { Text("Restore a perfect backup of subscriptions and liked episodes") },
                        leadingContent = { Icon(Icons.Rounded.SettingsBackupRestore, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    onClick = {
                        showImportBottomSheet = false
                        importOpmlLauncher.launch(arrayOf("*/*"))
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Other App Backup (.opml)") },
                        supportingContent = { Text("Migrate subscriptions from Apple Podcasts, Spotify, etc.") },
                        leadingContent = { Icon(Icons.Rounded.ImportExport, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 0. Shared animation progress (linear timeline mapped to FastOutSlowInEasing)
            val logoProgress = ((entranceProgress.value - 0.20f) / 0.55f).coerceIn(0f, 1f)
            val logoEase = FastOutSlowInEasing.transform(logoProgress)

            // 1. Podcast Cover Grid — occupies entire background, always visible
            CinematicBackgroundGrid(
                entranceProgressProvider = { entranceProgress.value },
                driftProgressProvider = { driftProgress.value }
            )

            // 2. Bottom-heavy gradient scrim — grows dynamically as logo/buttons shift up to cover the final logo position
            val scrimColor = MaterialTheme.colorScheme.surface
            val scrimEdge = 0.68f - (logoEase * 0.23f) // 0.68f → 0.45f
            val scrimMid = 0.76f - (logoEase * 0.24f)  // 0.76f → 0.52f
            val scrimFull = 0.81f - (logoEase * 0.24f) // 0.81f → 0.57f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to scrimColor.copy(alpha = 0.0f),
                                (scrimEdge - 0.15f).coerceAtLeast(0f) to scrimColor.copy(alpha = 0.0f),
                                scrimEdge to scrimColor.copy(alpha = 0.5f),
                                scrimMid to scrimColor.copy(alpha = 0.9f),
                                scrimFull to scrimColor,
                                1.0f to scrimColor
                            )
                        )
                    )
            )

            // 3. Content — logo always visible, slides up to reveal buttons
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Push everything to the bottom half
                Spacer(modifier = Modifier.weight(1f))

                // Logo block — always visible, starts bigger and centered in white space,
                // then scales down and nudges up to make room for buttons.
                val logoScale = 1.3f - (logoEase * 0.3f) // 1.3 → 1.0
                val logoOffsetY = (1f - logoEase) * 150f // starts 150dp lower, ends at 0
                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = logoScale
                            scaleY = logoScale
                            translationY = logoOffsetY * density
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome to",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CondensedGoogleSans,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    cx.aswin.boxcast.core.designsystem.components.BoxCastLogo(
                        textColor = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Staggered button reveal — starts after the logo has mostly moved up
                val btn1RawProgress = ((entranceProgress.value - 0.45f) / 0.30f).coerceIn(0f, 1f)
                val btn2RawProgress = ((entranceProgress.value - 0.53f) / 0.30f).coerceIn(0f, 1f)
                val btn3RawProgress = ((entranceProgress.value - 0.61f) / 0.30f).coerceIn(0f, 1f)

                val btn1Alpha = FastOutSlowInEasing.transform(btn1RawProgress)
                val btn2Alpha = FastOutSlowInEasing.transform(btn2RawProgress)
                val btn3Alpha = FastOutSlowInEasing.transform(btn3RawProgress)

                // Primary CTA
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onHelpMeFind,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .graphicsLayer {
                                alpha = btn1Alpha
                                translationY = (1f - btn1Alpha) * 20.dp.toPx()
                            },
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Build my personalized feed.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "We'll find you perfect shows based on what you love",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Floating AI Badge sitting on the button border
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-24).dp, y = (-6).dp)
                            .graphicsLayer {
                                alpha = btn1Alpha
                                translationY = (1f - btn1Alpha) * 20.dp.toPx()
                            }
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(percent = 50)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(percent = 50)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = btn2Alpha
                            translationY = (1f - btn2Alpha) * 20.dp.toPx()
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSearch,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(percent = 50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "I know my shows",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showImportBottomSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(percent = 50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Import library",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skip
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = btn3Alpha
                        }
                ) {
                    Text(
                        text = "Skip Setup",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color(0xFF888888)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFF888888),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun CinematicBackgroundGrid(
    entranceProgressProvider: () -> Float,
    driftProgressProvider: () -> Float
) {
    val context = LocalContext.current
    val allCovers = remember {
        (0..99).map { index ->
            context.resources.getIdentifier("pod_cover_$index", "drawable", context.packageName)
        }.filter { it != 0 }.shuffled()
    }

    if (allCovers.isEmpty()) return

    // 4 rows of covers — top half of the screen
    val row1Covers = remember(allCovers) { allCovers.filterIndexed { idx, _ -> idx % 4 == 0 } }
    val row2Covers = remember(allCovers) { allCovers.filterIndexed { idx, _ -> idx % 4 == 1 } }
    val row3Covers = remember(allCovers) { allCovers.filterIndexed { idx, _ -> idx % 4 == 2 } }
    val row4Covers = remember(allCovers) { allCovers.filterIndexed { idx, _ -> idx % 4 == 3 } }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        ScrollingRow(
            covers = row1Covers,
            translationX = {
                val ent = entranceProgressProvider()
                val scrollProgress = (ent / 0.75f).coerceIn(0f, 1f)
                val scrollEase = FastOutSlowInEasing.transform(scrollProgress)
                val drft = driftProgressProvider()
                -2200f + (1400f * scrollEase) + (732f * drft)
            }
        )
        ScrollingRow(
            covers = row2Covers,
            translationX = {
                val ent = entranceProgressProvider()
                val scrollProgress = (ent / 0.75f).coerceIn(0f, 1f)
                val scrollEase = FastOutSlowInEasing.transform(scrollProgress)
                val drft = driftProgressProvider()
                -100f - (1400f * scrollEase) - (732f * drft)
            }
        )
        ScrollingRow(
            covers = row3Covers,
            translationX = {
                val ent = entranceProgressProvider()
                val scrollProgress = (ent / 0.75f).coerceIn(0f, 1f)
                val scrollEase = FastOutSlowInEasing.transform(scrollProgress)
                val drft = driftProgressProvider()
                -2400f + (1400f * scrollEase) + (732f * drft)
            }
        )
        ScrollingRow(
            covers = row4Covers,
            translationX = {
                val ent = entranceProgressProvider()
                val scrollProgress = (ent / 0.75f).coerceIn(0f, 1f)
                val scrollEase = FastOutSlowInEasing.transform(scrollProgress)
                val drft = driftProgressProvider()
                -300f - (1400f * scrollEase) - (732f * drft)
            }
        )
    }
}

@Composable
private fun ScrollingRow(
    covers: List<Int>,
    translationX: () -> Float
) {
    // Take exactly 6 covers for a precise 732dp (6 * 122dp) seamless restart loop
    val loopCovers = remember(covers) { covers.take(6) }
    // Repeat covers list 20 times to simulate an infinite list within the bounds of scroll + drift
    val infiniteCovers = remember(loopCovers) { List(20) { loopCovers }.flatten() }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(unbounded = true, align = Alignment.Start)
            .graphicsLayer { this.translationX = translationX().dp.toPx() },
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        infiniteCovers.forEach { drawableResId ->
            val cardShape = RoundedCornerShape(16.dp)
            Card(
                modifier = Modifier
                    .size(110.dp),
                shape = cardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Image(
                    painter = painterResource(id = drawableResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GenrePickerScreen(
    selectedGenres: Set<String>,
    onToggleGenre: (String) -> Unit,
    onContinue: () -> Unit,
    onSearch: () -> Unit,
    onSkip: () -> Unit,
    onImportJson: (android.net.Uri) -> Unit = {},
    onImportOpml: (android.net.Uri) -> Unit = {},
    onImportSheetOpened: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val importJsonLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { onImportJson(it) } }
    )
    val importOpmlLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { onImportOpml(it) } }
    )
    
    var showImportBottomSheet by remember { mutableStateOf(false) }

    // Analytics: Track when import bottom sheet is opened
    LaunchedEffect(showImportBottomSheet) {
        if (showImportBottomSheet) onImportSheetOpened()
    }

    if (showImportBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Text(
                    text = "Import Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    onClick = {
                        showImportBottomSheet = false
                        importJsonLauncher.launch(arrayOf("application/json"))
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("boxcast Backup (.json)") },
                        supportingContent = { Text("Restore a perfect backup of subscriptions and liked episodes") },
                        leadingContent = { Icon(Icons.Rounded.SettingsBackupRestore, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    onClick = {
                        showImportBottomSheet = false
                        importOpmlLauncher.launch(arrayOf("*/*"))
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Other App Backup (.opml)") },
                        supportingContent = { Text("Migrate subscriptions from Apple Podcasts, Spotify, etc.") },
                        leadingContent = { Icon(Icons.Rounded.ImportExport, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Can we get to know you better?",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onContinue,
                        enabled = selectedGenres.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            if (selectedGenres.isEmpty()) "Pick at least 1"
                            else "Continue (${selectedGenres.size} selected)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
    
                    TextButton(
                        onClick = onSearch,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Search for your favorite podcasts instead",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    TextButton(onClick = onSkip) {
                        Text(
                            "Skip",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Pick the topics you enjoy",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Import Library Option
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .expressiveClickable { showImportBottomSheet = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Already have a library?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Import OPML or Backup",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ONBOARDING_GENRES.forEach { genre ->
                    val isSelected = genre.value in selectedGenres
                    GenreChip(
                        genre = genre,
                        isSelected = isSelected,
                        onClick = { onToggleGenre(genre.value) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastPicksScreen(
    podcasts: List<Podcast>,
    subscribedIds: Set<String>,
    currentRegion: String,
    isLoading: Boolean,
    onToggleSubscription: (String) -> Unit,
    onRegionChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    onDidScroll: () -> Unit
) {
    LogRecomposition(name = "PodcastPicksScreen")
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) {
            onDidScroll()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Here are some picks for you",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onDone,
                        enabled = subscribedIds.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            if (subscribedIds.isEmpty()) "Pick at least 1" else "Subscribe & Start (${subscribedIds.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextButton(
                        onClick = onSearch,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Search for more",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    TextButton(onClick = onSkip) {
                        Text(
                            "Skip",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                BoxCastLoader.Expressive()
            }
        } else {
            val distinctPodcasts = remember(podcasts) { podcasts.distinctBy { it.id } }
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(
                            text = "Tap to subscribe — you can always change later",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Premium Region Segmented Control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val regions = listOf(
                                "us" to "USA",
                                "in" to "India",
                                "gb" to "UK",
                                "fr" to "France"
                            )
                            regions.forEach { (code, label) ->
                                val isSelected = currentRegion == code
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else androidx.compose.ui.graphics.Color.Transparent
                                        )
                                        .expressiveClickable { onRegionChange(code) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                items(distinctPodcasts, key = { it.id }) { podcast ->
                    PodcastPickCard(
                        podcast = podcast,
                        isSubscribed = podcast.id in subscribedIds,
                        onToggle = { onToggleSubscription(podcast.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastPickCard(
    podcast: Podcast,
    isSubscribed: Boolean,
    onToggle: () -> Unit
) {
    LogRecomposition(name = "PodcastPickCard")
    val containerColor = if (isSubscribed)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.surfaceContainerHigh
    
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.extraLarge, // Back to standard rounded shape
        modifier = Modifier
            .fillMaxWidth()
            .expressiveClickable(onClick = onToggle)
    ) {
        Column {
            Box {
                OptimizedImage(
                    url = podcast.imageUrl,
                    proxyWidth = 400,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                )
                
                // Subscribe badge
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSubscribed,
                    enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = androidx.compose.foundation.shape.CircleShape // Standard circle
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Subscribed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    podcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    podcast.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GenreChip(
    genre: GenreItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHigh
    
    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface
    
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraLarge,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .height(64.dp)
            .expressiveClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp), // Reduced padding (from 24dp)
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing (from 12dp)
        ) {
            Icon(
                genre.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp), // Reduced icon size (from 28dp)
                tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor
            )
            Text(
                genre.label,
                style = MaterialTheme.typography.titleMedium, // Reduced text size (from TitleLarge)
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================
// SEARCH
// ============================================================

@Composable
private fun OnboardingSearchScreen(
    query: String,
    results: List<Podcast>,
    isSearching: Boolean,
    subscribedIds: Set<String>,
    onQueryChange: (String) -> Unit,
    onSubscribe: (Podcast) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
            }
            
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search podcasts...") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                leadingIcon = { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, "Clear", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
        }
        
        when {
            isSearching -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    BoxCastLoader.Expressive(size = 100.dp)
                }
            }
            query.length >= 2 && results.isEmpty() && !isSearching -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Podcasts Found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please try searching for the exact word or name of the podcast you are looking for, and double-check spacing and spelling.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            results.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Search for your favorite podcasts\nand subscribe right here",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                val distinctResults = remember(results) { results.distinctBy { it.id } }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(distinctResults, key = { it.id }) { podcast ->
                        SearchResultRow(
                            podcast = podcast,
                            isSubscribed = podcast.id in subscribedIds,
                            onSubscribe = { onSubscribe(podcast) }
                        )
                    }
                }
            }
        }
        
        // Done button at bottom
        if (subscribedIds.isNotEmpty()) {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    "Done (${subscribedIds.size} subscribed)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    podcast: Podcast,
    isSubscribed: Boolean,
    onSubscribe: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .expressiveClickable(onClick = onSubscribe)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OptimizedImage(
            url = podcast.imageUrl,
            proxyWidth = 400,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                podcast.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                podcast.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        if (isSubscribed) {
            FilledIconButton(
                onClick = {},
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Subscribed",
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            OutlinedIconButton(
                onClick = onSubscribe,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Subscribe",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
