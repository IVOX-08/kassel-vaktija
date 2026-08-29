package de.igbdsandzakkassel.vaktija.ui.news

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.material3.Switch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ThumbDown
import de.igbdsandzakkassel.vaktija.data.model.Reaction
import de.igbdsandzakkassel.vaktija.ui.theme.BrandGreen
import de.igbdsandzakkassel.vaktija.R
import de.igbdsandzakkassel.vaktija.core.locale.LocaleController
import de.igbdsandzakkassel.vaktija.data.model.Community
import de.igbdsandzakkassel.vaktija.data.model.NewsItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Community announcements feed. Admin (when signed in) can post and delete; everyone else reads. */
@Composable
fun NewsScreen(
    modifier: Modifier = Modifier,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val news by viewModel.news.collectAsStateWithLifecycle()
    val community by viewModel.community.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val canBroadcast by viewModel.canBroadcast.collectAsStateWithLifecycle()
    val broadcastOnly by viewModel.broadcastOnly.collectAsStateWithLifecycle()
    val deleteCtx by viewModel.deleteContext.collectAsStateWithLifecycle()
    val allCommunities by viewModel.allCommunities.collectAsStateWithLifecycle()
    // The user's selected app language — each announcement is shown in this language.
    val locales = LocalConfiguration.current.locales
    val lang = if (locales.isEmpty) LocaleController.current().tag else locales[0].language

    val context = LocalContext.current
    val partialMsg = stringResource(R.string.news_translate_partial)
    val failMsg = stringResource(R.string.news_post_failed)
    val imageFailedMsg = stringResource(R.string.news_image_failed)

    var showCompose by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<NewsItem?>(null) }
    // When non-null, the attached flyer is shown full-screen (pinch-to-zoom).
    var viewerImage by remember { mutableStateOf<ByteArray?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.nav_news),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            // Where the community posts. Beside the heading rather than at the foot of the list:
            // someone who has just read an announcement is exactly the person who wants more, and
            // at the foot they would be below however many announcements there happen to be.
            SocialLinks(community)
        }

        if (isAdmin) {
            Button(
                onClick = { showCompose = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.news_add))
            }
            Spacer(Modifier.height(12.dp))
        }

        val list = news
        when {
            list == null -> CenteredBox { CircularProgressIndicator() }
            list.isEmpty() -> CenteredBox {
                // Offline with an empty local cache is indistinguishable from "the mosque never
                // posted" from the data alone — check connectivity so we don't confidently claim
                // "no announcements" when the truth is "couldn't load them".
                val ctx = LocalContext.current
                val offline = remember {
                    val cm = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                        as? android.net.ConnectivityManager
                    cm?.activeNetwork == null
                }
                Text(
                    text = stringResource(if (offline) R.string.news_offline else R.string.news_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { item ->
                    NewsCard(
                        item = item,
                        lang = lang,
                        canDelete = viewModel.canDelete(item, deleteCtx.first, deleteCtx.second),
                        onDelete = { pendingDelete = item },
                        loadImage = viewModel::loadImage,
                        onImageClick = { viewerImage = it },
                        senderName = allCommunities.firstOrNull { it.id == item.communityId }?.name,
                        senderLogoUrl = allCommunities.firstOrNull { it.id == item.communityId }?.logoUrl,
                        observeMyReaction = viewModel::observeMyReaction,
                        onReact = viewModel::react,
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    if (showCompose) {
        ComposeNewsDialog(
            onDismiss = { showCompose = false },
            canBroadcast = canBroadcast,
            broadcastOnly = broadcastOnly,
            communities = allCommunities,
            onPost = { title, body, imageUri, broadcast, audience, cb ->
                viewModel.postNews(title, body, imageUri, broadcast, audience) { outcome ->
                    when {
                        !outcome.ok ->
                            Toast.makeText(context, failMsg, Toast.LENGTH_LONG).show()
                        outcome.imageDropped ->
                            Toast.makeText(context, imageFailedMsg, Toast.LENGTH_LONG).show()
                        outcome.failedLangs.isNotEmpty() ->
                            Toast.makeText(context, partialMsg, Toast.LENGTH_LONG).show()
                    }
                    cb(outcome.ok)
                    if (outcome.ok) showCompose = false
                }
            },
        )
    }

    viewerImage?.let { bytes ->
        FullScreenImageViewer(
            bytes = bytes,
            contentDescription = stringResource(R.string.cd_news_image),
            onDismiss = { viewerImage = null },
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.news_delete_confirm)) },
            text = { Text(target.title(lang)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNews(target)
                    pendingDelete = null
                }) { Text(stringResource(R.string.news_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

/**
 * Like / dislike, the show of hands a community asks for ("tap the heart if you are coming").
 *
 * The chosen side fills in and the count moves immediately — Firestore applies the write locally
 * before it reaches the server, so this stays responsive with no connection. Tapping the same side
 * again takes the answer back, because someone whose plans change has to be able to say so.
 */
@Composable
private fun ReactionRow(
    item: NewsItem,
    mine: Reaction?,
    onReact: (Reaction) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 10.dp),
    ) {
        ReactionButton(
            selected = mine == Reaction.LIKE,
            count = item.likeCount,
            selectedIcon = Icons.Filled.Favorite,
            plainIcon = Icons.Outlined.FavoriteBorder,
            tint = BrandGreen,
            contentDescription = stringResource(R.string.news_like),
            onClick = { onReact(Reaction.LIKE) },
        )
        ReactionButton(
            selected = mine == Reaction.DISLIKE,
            count = item.dislikeCount,
            selectedIcon = Icons.Filled.ThumbDown,
            plainIcon = Icons.Outlined.ThumbDown,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = stringResource(R.string.news_dislike),
            onClick = { onReact(Reaction.DISLIKE) },
        )
    }
}

@Composable
private fun ReactionButton(
    selected: Boolean,
    count: Int,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    plainIcon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val active = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) tint.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else plainIcon,
            contentDescription = contentDescription,
            tint = active,
            modifier = Modifier.size(18.dp),
        )
        if (count > 0) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = active,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/** Circular logo plus "Gesendet von …" — the community's own, or the federation's for a broadcast. */
@Composable
private fun SenderLine(item: NewsItem, senderName: String?, senderLogoUrl: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            when {
                // A federation notice is signed with the app's own emblem, which is the IGBD one.
                item.isBroadcast || senderLogoUrl == null -> Image(
                    painter = painterResource(R.drawable.logo_emblem),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp).clip(CircleShape),
                )
                else -> AsyncImage(
                    model = senderLogoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp).clip(CircleShape),
                )
            }
        }
        Text(
            text = stringResource(
                R.string.news_sent_by,
                senderName ?: stringResource(R.string.news_sender_federation),
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun NewsCard(
    item: NewsItem,
    lang: String,
    canDelete: Boolean,
    onDelete: () -> Unit,
    loadImage: suspend (NewsItem) -> ByteArray?,
    onImageClick: (ByteArray) -> Unit,
    senderName: String? = null,
    senderLogoUrl: String? = null,
    observeMyReaction: (NewsItem) -> kotlinx.coroutines.flow.Flow<Reaction?>,
    onReact: (NewsItem, Reaction) -> Unit,
) {
    val body = item.body(lang)
    val myReaction by observeMyReaction(item).collectAsStateWithLifecycle(initialValue = null)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Who this is from, above the headline. With one community it was obvious; now a
            // federation notice and the mosque down the road sit in the same list.
            SenderLine(item = item, senderName = senderName, senderLogoUrl = senderLogoUrl)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.news_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (item.createdAt > 0L) {
                Text(
                    text = formatTimestamp(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (body.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (item.hasImage) {
                // Fetched lazily (and cache-first) only now that this card is composed. A spinner
                // holds a 16:9 slot until the flyer is ready; if it can't be loaded (e.g. offline,
                // or the rule isn't deployed yet) the slot disappears instead of spinning forever.
                val flyer by produceState<FlyerState>(FlyerState.Loading, item.id) {
                    value = loadImage(item)?.let { FlyerState.Ready(it) } ?: FlyerState.Unavailable
                }
                when (val state = flyer) {
                    FlyerState.Loading -> {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                    is FlyerState.Ready -> {
                        Spacer(Modifier.height(12.dp))
                        AsyncImage(
                            model = state.bytes,
                            contentDescription = stringResource(R.string.cd_news_image),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(state.bytes) },
                        )
                    }
                    FlyerState.Unavailable -> Unit
                }
                ReactionRow(item = item, mine = myReaction, onReact = { onReact(item, it) })
            }
        }
    }
}

/** Load state of a card's attached flyer. [Unavailable] keeps a failed load from spinning forever. */
private sealed interface FlyerState {
    data object Loading : FlyerState
    data object Unavailable : FlyerState
    class Ready(val bytes: ByteArray) : FlyerState
}

/** Full-screen flyer viewer: pinch-to-zoom, drag-to-pan, double-tap to toggle zoom, tap to close. */
@Composable
private fun FullScreenImageViewer(
    bytes: ByteArray,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .onSizeChanged { containerSize = it },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = bytes,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            // Clamp the pan so a zoomed flyer can't be dragged off into empty space
                            // (and snaps back to centre as it returns to 1×).
                            val maxX = containerSize.width * (scale - 1f) / 2f
                            val maxY = containerSize.height * (scale - 1f) / 2f
                            offset = if (scale > 1f) {
                                Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY),
                                )
                            } else {
                                Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (scale <= 1f) onDismiss() },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            },
                        )
                    },
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(android.R.string.cancel),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ComposeNewsDialog(
    onDismiss: () -> Unit,
    onPost: (String, String, Uri?, Boolean, List<String>, (Boolean) -> Unit) -> Unit,
    communities: List<Community> = emptyList(),
    canBroadcast: Boolean = false,
    broadcastOnly: Boolean = false,
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    var broadcast by remember { mutableStateOf(broadcastOnly) }
    var audience by remember { mutableStateOf(emptyList<String>()) }
    var audienceOpen by remember { mutableStateOf(false) }
    // System photo picker (no storage permission needed; available on every supported API level).
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) imageUri = uri }
    AlertDialog(
        // Block dismissal (back press / outside tap) while translating + posting is in flight.
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(stringResource(R.string.news_add)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= TITLE_MAX_CHARS) title = it },
                    label = { Text(stringResource(R.string.news_title_label)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= BODY_MAX_CHARS) body = it },
                    label = { Text(stringResource(R.string.news_body_label)) },
                    minLines = 3,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                val picked = imageUri
                if (picked != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = picked,
                            contentDescription = stringResource(R.string.cd_news_image),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        IconButton(
                            onClick = { imageUri = null },
                            enabled = !loading,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50)),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.news_remove_image),
                                tint = Color.White,
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.news_add_image))
                    }
                }
                // A head admin who cannot post for a community has nothing to choose — he is told
                // where it goes. Everyone else who CAN broadcast gets the switch, off by default:
                // reaching every community should be a decision, not a forgotten setting.
                if (broadcastOnly) {
                    Text(
                        text = stringResource(R.string.news_broadcast_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (canBroadcast) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.news_broadcast_all),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(R.string.news_broadcast_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = broadcast,
                            onCheckedChange = { broadcast = it },
                            enabled = !loading,
                        )
                    }
                }
                // Recipient picker: only meaningful once the announcement leaves one community.
                if (broadcast) {
                    OutlinedButton(
                        onClick = { audienceOpen = true },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (audience.isEmpty()) stringResource(R.string.news_audience_confirm_all)
                            else stringResource(R.string.news_audience_confirm, audience.size),
                        )
                    }
                }
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.news_translating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading && title.isNotBlank(),
                onClick = {
                    loading = true
                    onPost(title, body, imageUri, broadcast, audience) { loading = false }
                },
            ) { Text(stringResource(R.string.news_post)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )

    if (audienceOpen) {
        AudiencePickerDialog(
            communities = communities,
            selected = audience,
            onDismiss = { audienceOpen = false },
            onConfirm = { audience = it; audienceOpen = false },
        )
    }
}

// Keep announcements well within ML Kit's translate limits and Firestore's 1 MB document size
// (8 language copies × title+body).
private const val TITLE_MAX_CHARS = 200
private const val BODY_MAX_CHARS = 4000

/** Instagram, Facebook and YouTube — only the ones the community actually has. */
@Composable
private fun SocialLinks(community: de.igbdsandzakkassel.vaktija.data.model.Community?) {
    val uriHandler = LocalUriHandler.current
    val links = listOfNotNull(
        community?.instagramUrl?.takeIf { it.isNotBlank() }
            ?.let { R.drawable.ic_instagram to it },
        community?.facebookUrl?.takeIf { it.isNotBlank() }
            ?.let { R.drawable.ic_facebook to it },
        community?.youtubeUrl?.takeIf { it.isNotBlank() }
            ?.let { R.drawable.ic_youtube to it },
    )
    if (links.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        links.forEach { (icon, url) ->
            IconButton(onClick = { uriHandler.openUri(url) }) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = url,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

private fun formatTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(
            // Locale-aware pattern: the fixed "d. MMMM" dot style is Bosnian/German convention only
            // and looked wrong in en/ru/tr ("2. July 2026"). Let each locale pick its own format.
            DateTimeFormatter.ofPattern(
                android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyHHmm"),
                Locale.getDefault(),
            ),
        )
