package com.github.premtechworks.synqvia.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.premtechworks.synqvia.R
import com.github.premtechworks.synqvia.ui.LocalHazeState
import com.github.premtechworks.synqvia.ui.LocalIsBlurSupported
import com.github.premtechworks.synqvia.ui.components.CircleIconButton
import com.github.premtechworks.synqvia.ui.components.IconTile
import com.github.premtechworks.synqvia.ui.components.PrimaryButton
import com.github.premtechworks.synqvia.ui.components.ScreenScaffold
import com.github.premtechworks.synqvia.ui.components.ScrollableColumn
import com.github.premtechworks.synqvia.ui.components.SynqviaCard
import com.github.premtechworks.synqvia.ui.components.SynqviaMark
import com.github.premtechworks.synqvia.ui.components.SynqviaWordmark
import com.github.premtechworks.synqvia.ui.motion.LocalAppHaptics
import com.github.premtechworks.synqvia.ui.motion.SynqviaHaptics
import com.github.premtechworks.synqvia.ui.motion.entryStagger
import com.github.premtechworks.synqvia.ui.motion.pressable
import com.github.premtechworks.synqvia.ui.rememberCardGlassStyle
import com.github.premtechworks.synqvia.ui.rememberGlassCardBorderBrush
import com.github.premtechworks.synqvia.ui.support.ContactLinks
import com.github.premtechworks.synqvia.ui.support.FaqData
import com.github.premtechworks.synqvia.ui.support.FaqItem
import com.github.premtechworks.synqvia.ui.support.QuickLinkItem
import com.github.premtechworks.synqvia.ui.support.QuickLinkType
import com.github.premtechworks.synqvia.ui.theme.SynqviaTheme
import com.github.premtechworks.synqvia.ui.theme.SynqviaType
import com.github.premtechworks.synqvia.ui.util.AppVersionHelper
import com.github.premtechworks.synqvia.ui.util.InlineCodeParser
import dev.chrisbanes.haze.hazeEffect

@Composable
fun ContactSupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalAppHaptics.current
    val scrollState = rememberScrollState()
    val isScrolled by remember { derivedStateOf { scrollState.value > 8 } }
    val colors = SynqviaTheme.colors

    val visibleQuickLinks = remember { ContactLinks.getVisibleQuickLinks() }

    fun openUrl(url: String) {
        try {
            val formattedUrl = ContactLinks.normalizeUrl(url).ifEmpty { url }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            haptics.reject()
            onShowMessage(context.getString(R.string.error_no_app_for_link))
        } catch (_: Exception) {
            haptics.reject()
            onShowMessage(context.getString(R.string.error_no_app_for_link))
        }
    }

    ContactSupportContent(
        visibleQuickLinks = visibleQuickLinks,
        isScrolled = isScrolled,
        scrollState = scrollState,
        onBack = onBack,
        onOpenUrl = ::openUrl,
        modifier = modifier
    )
}

@Composable
fun ContactSupportContent(
    visibleQuickLinks: List<QuickLinkItem>,
    isScrolled: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    faqItems: List<FaqItem> = FaqData.ITEMS,
    selectedFaq: FaqItem? = null,
    onSelectFaq: ((FaqItem?) -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = SynqviaTheme.colors
    val isDark = SynqviaTheme.isDark
    val haptics = LocalAppHaptics.current

    var localSelectedFaq by remember { mutableStateOf<FaqItem?>(null) }
    val activeFaq = onSelectFaq?.let { selectedFaq } ?: (selectedFaq ?: localSelectedFaq)
    val updateSelectedFaq: (FaqItem?) -> Unit = { item ->
        if (onSelectFaq != null) {
            onSelectFaq(item)
        } else {
            localSelectedFaq = item
        }
    }

    ScreenScaffold(
        modifier = modifier.testTag("contact_support_screen"),
        isScrolled = isScrolled,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    size = 40.dp,
                    iconSize = 20.dp,
                    contentDescription = stringResource(R.string.navigate_back)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.contact_support_title),
                    style = SynqviaType.Title,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { contentPadding ->
        ScrollableColumn(
            contentPadding = contentPadding,
            scrollState = scrollState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1) Developer Card (20dp radius)
            DeveloperCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .entryStagger(index = 0)
            )

            // 2) Quick Links Card
            if (visibleQuickLinks.isNotEmpty()) {
                QuickLinksCard(
                    links = visibleQuickLinks,
                    isDark = isDark,
                    haptics = haptics,
                    onOpenUrl = onOpenUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .entryStagger(index = 1)
                )
            }

            // 3) COMMON QUESTIONS Card
            CommonQuestionsCard(
                faqItems = faqItems,
                isDark = isDark,
                haptics = haptics,
                onSelectFaq = { faq ->
                    updateSelectedFaq(faq)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .entryStagger(index = 2)
            )

            // 4) STILL NEED HELP? Card
            StillNeedHelpCard(
                onOpenUrl = onOpenUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .entryStagger(index = 3)
            )

            // 5) About Card
            AboutCard(
                context = context,
                isDark = isDark,
                haptics = haptics,
                onOpenRepository = { onOpenUrl(ContactLinks.GITHUB_URL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .entryStagger(index = 4)
            )

            // 6) Brand Footer
            BrandFooter(
                modifier = Modifier
                    .fillMaxWidth()
                    .entryStagger(index = 5)
            )
        }
    }

    // FAQ Answer Bottom Sheet
    if (activeFaq != null) {
        FaqAnswerBottomSheet(
            faqItem = activeFaq,
            onDismiss = { updateSelectedFaq(null) },
            onOpenUrl = onOpenUrl
        )
    }
}

@Composable
private fun DeveloperCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = SynqviaTheme.colors

    SynqviaCard(
        modifier = modifier.testTag("support_developer_card"),
        shape = RoundedCornerShape(20.dp),
        padding = 16.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar
            val avatarResId = remember(context) {
                context.resources.getIdentifier("dev_avatar", "drawable", context.packageName)
            }
            if (avatarResId != 0) {
                Image(
                    painter = painterResource(avatarResId),
                    contentDescription = stringResource(R.string.developer_avatar_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    colors.primary,
                                    colors.primary.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        style = SynqviaType.Title,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.developer_overline),
                    style = SynqviaType.Overline,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.developer_name),
                    style = SynqviaType.Headline,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.developer_role),
                    style = SynqviaType.Footnote,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.developer_bio),
            style = SynqviaType.Body,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun QuickLinksCard(
    links: List<QuickLinkItem>,
    isDark: Boolean,
    haptics: SynqviaHaptics,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SynqviaTheme.colors

    SynqviaCard(
        modifier = modifier.testTag("support_quick_links_card"),
        padding = 16.dp
    ) {
        Text(
            text = stringResource(R.string.quick_links_overline),
            style = SynqviaType.Overline,
            color = colors.textTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        links.forEachIndexed { index, item ->
            if (index > 0) {
                DividerLine()
            }

            val linkTitle = stringResource(item.titleRes)
            val opensBrowserCd = stringResource(R.string.accessibility_link_opens_browser, linkTitle)

            val tileContainer = when (item.type) {
                QuickLinkType.TELEGRAM -> Color.Transparent
                QuickLinkType.GITHUB -> if (isDark) colors.surfaceHigh else colors.surfaceInset
                QuickLinkType.KOFI -> if (isDark) item.darkContainerColor else item.lightContainerColor
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .semantics {
                        this.role = Role.Button
                        this.contentDescription = opensBrowserCd
                    }
                    .pressable(
                        targetScale = 0.97f,
                        showOverlay = true,
                        onClick = {
                            haptics.tick()
                            onOpenUrl(item.url)
                        }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 44dp IconTile
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .then(
                            if (item.type == QuickLinkType.TELEGRAM) {
                                Modifier
                            } else {
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tileContainer)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconRes = if (item.type == QuickLinkType.GITHUB && isDark) {
                        R.drawable.ic_brand_github_dark
                    } else {
                        item.iconRes
                    }
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = linkTitle,
                        style = SynqviaType.Headline,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(item.subtitleRes),
                        style = SynqviaType.Footnote,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutCard(
    context: Context,
    isDark: Boolean,
    haptics: SynqviaHaptics,
    onOpenRepository: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SynqviaTheme.colors

    SynqviaCard(
        modifier = modifier.testTag("support_about_card"),
        padding = 16.dp
    ) {
        Text(
            text = stringResource(R.string.about_overline),
            style = SynqviaType.Overline,
            color = colors.textTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Row 1: Version
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.about_version_label),
                style = SynqviaType.Callout,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = AppVersionHelper.getFormattedVersion(context),
                style = SynqviaType.Callout,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DividerLine()

        // Row 2: License
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.about_license_label),
                style = SynqviaType.Callout,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = AppVersionHelper.LICENSE,
                style = SynqviaType.Callout,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DividerLine()

        // Row 3: Repository (clickable)
        val repoCd = stringResource(R.string.accessibility_link_opens_browser, stringResource(R.string.about_repository_label))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .semantics {
                    this.role = Role.Button
                    this.contentDescription = repoCd
                }
                .pressable(
                    targetScale = 0.97f,
                    showOverlay = true,
                    onClick = {
                        haptics.tick()
                        onOpenRepository()
                    }
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.about_repository_label),
                style = SynqviaType.Callout,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppVersionHelper.REPOSITORY,
                    style = SynqviaType.Callout,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CommonQuestionsCard(
    faqItems: List<FaqItem>,
    isDark: Boolean,
    haptics: SynqviaHaptics,
    onSelectFaq: (FaqItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SynqviaTheme.colors

    SynqviaCard(
        modifier = modifier.testTag("support_faq_card"),
        padding = 16.dp
    ) {
        Text(
            text = stringResource(R.string.faq_overline),
            style = SynqviaType.Overline,
            color = colors.textTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        faqItems.forEachIndexed { index, item ->
            if (index > 0) {
                DividerLine()
            }

            val questionTitle = stringResource(item.titleRes)
            val containerColor = if (isDark) item.darkTileBg else item.lightTileBg
            val contentColor = if (isDark) item.darkTileFg else item.lightTileFg

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .semantics {
                        this.role = Role.Button
                    }
                    .pressable(
                        targetScale = 0.97f,
                        showOverlay = true,
                        onClick = {
                            haptics.tick()
                            onSelectFaq(item)
                        }
                    )
                    .padding(vertical = 8.dp)
                    .testTag("faq_row_${item.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(
                    icon = item.icon,
                    size = 36.dp,
                    iconSize = 18.dp,
                    cornerRadius = 10.dp,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = questionTitle,
                    style = SynqviaType.Callout,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StillNeedHelpCard(
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SynqviaTheme.colors

    SynqviaCard(
        modifier = modifier.testTag("support_still_need_help_card"),
        padding = 16.dp
    ) {
        Text(
            text = stringResource(R.string.still_need_help_overline),
            style = SynqviaType.Overline,
            color = colors.textTertiary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceInset)
                .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(
                    icon = Icons.Outlined.Mail,
                    size = 36.dp,
                    iconSize = 18.dp,
                    cornerRadius = 10.dp,
                    containerColor = colors.blueContainer,
                    contentColor = colors.primary,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.still_need_help_title),
                        style = SynqviaType.Callout.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val isTelegramClickable = ContactLinks.isValidUrl(ContactLinks.TELEGRAM_URL) &&
                            !ContactLinks.TELEGRAM_URL.contains("{{")

                    val annotatedText = buildAnnotatedString {
                        append(stringResource(R.string.still_need_help_prefix))
                        if (isTelegramClickable) {
                            pushLink(
                                LinkAnnotation.Clickable(
                                    tag = "telegram",
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = colors.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    ),
                                    linkInteractionListener = {
                                        onOpenUrl(ContactLinks.TELEGRAM_URL)
                                    }
                                )
                            )
                            append(stringResource(R.string.still_need_help_telegram))
                            pop()
                        } else {
                            append(stringResource(R.string.still_need_help_telegram))
                        }
                        append(stringResource(R.string.still_need_help_mid))
                        pushLink(
                            LinkAnnotation.Clickable(
                                tag = "github_issues",
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = colors.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ),
                                linkInteractionListener = {
                                    onOpenUrl(ContactLinks.GITHUB_ISSUES_URL)
                                }
                            )
                        )
                        append(stringResource(R.string.still_need_help_github))
                        pop()
                        append(stringResource(R.string.still_need_help_suffix))
                    }

                    Text(
                        text = annotatedText,
                        style = SynqviaType.Footnote,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandFooter(
    modifier: Modifier = Modifier
) {
    val colors = SynqviaTheme.colors
    val isDark = SynqviaTheme.isDark
    val waveColor = if (isDark) colors.primary.copy(alpha = 0.06f) else colors.primaryContainer.copy(alpha = 0.40f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .testTag("support_brand_footer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val w = size.width
            val h = size.height

            // Wave 1: swooping from bottom-left
            val path1 = Path().apply {
                moveTo(0f, h * 0.42f)
                cubicTo(
                    w * 0.20f, h * 0.60f,
                    w * 0.45f, h * 0.85f,
                    w * 0.70f, h
                )
                lineTo(0f, h)
                close()
            }
            drawPath(path = path1, color = waveColor)

            // Wave 2: swooping from bottom-right
            val path2 = Path().apply {
                moveTo(w, h * 0.32f)
                cubicTo(
                    w * 0.80f, h * 0.52f,
                    w * 0.40f, h * 0.76f,
                    w * 0.15f, h
                )
                lineTo(w, h)
                close()
            }
            drawPath(path = path2, color = waveColor)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            SynqviaMark(size = 48.dp, contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            SynqviaWordmark(height = 24.dp, contentDescription = "Synqvia")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.footer_tagline),
                style = SynqviaType.Footnote,
                color = colors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaqAnswerBottomSheet(
    faqItem: FaqItem,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val colors = SynqviaTheme.colors
    val isDark = SynqviaTheme.isDark
    val hazeState = LocalHazeState.current
    val isBlurSupported = LocalIsBlurSupported.current
    val cardGlassStyle = rememberCardGlassStyle()
    val glassBorderBrush = rememberGlassCardBorderBrush()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxHeight = screenHeight * 0.85f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.Transparent,
        contentColor = colors.textPrimary,
        scrimColor = colors.scrim,
        dragHandle = null,
        modifier = Modifier.testTag("faq_answer_sheet")
    ) {
        val sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .clip(sheetShape)
                .then(
                    if (isBlurSupported && hazeState != null) {
                        Modifier
                            .then(
                                if (isDark) {
                                    Modifier.hazeEffect(state = hazeState, style = cardGlassStyle)
                                } else {
                                    Modifier.background(colors.surface.copy(alpha = 0.94f))
                                }
                            )
                            .border(1.dp, if (isDark) glassBorderBrush else SolidColor(colors.outline), sheetShape)
                    } else {
                        Modifier
                            .background(if (isDark) colors.surfaceHigh else colors.surface)
                            .border(1.dp, colors.outline, sheetShape)
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Drag handle pill (36dp x 4dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(colors.outline)
                    )
                }

                // Question Title
                Text(
                    text = stringResource(faqItem.titleRes),
                    style = SynqviaType.Title,
                    color = colors.textPrimary,
                    modifier = Modifier
                        .semantics { heading() }
                        .testTag("faq_sheet_title")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Steps list
                val steps = remember(faqItem.stepsRes) {
                    context.resources.getStringArray(faqItem.stepsRes).toList()
                }

                val codeStyle = SpanStyle(
                    fontFamily = SynqviaType.Mono.fontFamily,
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    background = colors.surfaceInset
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    steps.forEachIndexed { index, stepText ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Step number badge: 22dp primaryContainer circle with primary SemiBold digits
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = SynqviaType.CaptionSemiBold.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = colors.primary
                                )
                            }

                            Text(
                                text = InlineCodeParser.parse(stepText, codeStyle),
                                style = SynqviaType.Body,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Optional action button
                if (faqItem.actionButtonTextRes != null && faqItem.actionUrl != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    PrimaryButton(
                        text = stringResource(faqItem.actionButtonTextRes),
                        onClick = {
                            onDismiss()
                            onOpenUrl(faqItem.actionUrl)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("faq_sheet_action_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(SynqviaTheme.colors.divider)
    )
}

@PreviewLightDark
@Composable
fun PreviewContactSupportScreen() {
    SynqviaTheme {
        ContactSupportContent(
            visibleQuickLinks = ContactLinks.getAllQuickLinks(
                telegramUrl = "https://t.me/synqvia",
                githubUrl = "https://github.com/premtechworks/Synqvia",
                kofiUrl = "https://ko-fi.com/synqvia"
            ),
            isScrolled = false,
            scrollState = rememberScrollState(),
            onBack = {},
            onOpenUrl = {}
        )
    }
}

/**
 * Verification Preview: Renders Telegram, GitHub, and Ko-fi brand icons at 24dp, 44dp, and 56dp
 * in light and dark themes to verify visual consistency with the mockup.
 */
@PreviewLightDark
@Composable
fun PreviewBrandIconsComparison() {
    val isDark = SynqviaTheme.isDark
    val colors = SynqviaTheme.colors
    SynqviaTheme {
        Column(
            modifier = Modifier
                .background(colors.bgTop)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(24.dp, 44.dp, 56.dp).forEach { size ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Telegram (badge is the tile)
                    Image(
                        painter = painterResource(R.drawable.ic_brand_telegram),
                        contentDescription = "Telegram",
                        modifier = Modifier.size(size)
                    )

                    // GitHub (octocat on neutral tile)
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(RoundedCornerShape(size * (12f / 44f)))
                            .background(if (isDark) colors.surfaceHigh else colors.surfaceInset),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(if (isDark) R.drawable.ic_brand_github_dark else R.drawable.ic_brand_github),
                            contentDescription = "GitHub",
                            modifier = Modifier.size(size)
                        )
                    }

                    // Ko-fi (cup on peach tile)
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(RoundedCornerShape(size * (12f / 44f)))
                            .background(if (isDark) Color(0xFFFF5E5B).copy(alpha = 0.14f) else Color(0xFFFDECE6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_brand_kofi),
                            contentDescription = "Ko-fi",
                            modifier = Modifier.size(size)
                        )
                    }
                }
            }
        }
    }
}
