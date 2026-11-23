package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.drop
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.Settings
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.runOnEnterKeyPressed
import tachiyomi.presentation.core.util.secondaryItemAlpha
import tachiyomi.presentation.core.util.showSoftKeyboard

@Composable
fun AppBar(
    title: String?,

    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    // Text
    subtitle: String? = null,
    // Up button
    navigateUp: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    // Menu
    actions: @Composable RowScope.() -> Unit = {},
    // Action mode
    actionModeCounter: Int = 0,
    onCancelActionMode: () -> Unit = {},
    actionModeActions: @Composable RowScope.() -> Unit = {},

    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val isActionMode by remember(actionModeCounter) {
        derivedStateOf { actionModeCounter > 0 }
    }

    AppBar(
        modifier = modifier,
        backgroundColor = backgroundColor,
        titleContent = {
            if (isActionMode) {
                AppBarTitle(actionModeCounter.toString())
            } else {
                AppBarTitle(title, subtitle = subtitle)
            }
        },
        navigateUp = navigateUp,
        navigationIcon = navigationIcon,
        actions = {
            if (isActionMode) {
                actionModeActions()
            } else {
                actions()
            }
        },
        isActionMode = isActionMode,
        onCancelActionMode = onCancelActionMode,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun AppBar(
    // Title
    titleContent: @Composable () -> Unit,

    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    // Up button
    navigateUp: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null, // RK
    // Menu
    actions: @Composable RowScope.() -> Unit = {},
    // Action mode
    isActionMode: Boolean = false,
    onCancelActionMode: () -> Unit = {},

    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            navigationIcon = {
                if (isActionMode) {
                    FilledTonalIconButton(
                        colors = noFillIconButtonColors(), // RK: no grey fill under the icon
                        onClick = onCancelActionMode,
                        modifier = Modifier.padding(start = 8.dp).size(46.dp), // 46 dp nav container + 16 dp edge
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Close,
                            contentDescription = stringResource(MR.strings.action_cancel),
                        )
                    }
                } else {
                    navigateUp?.let {
                        FilledTonalIconButton(
                            colors = noFillIconButtonColors(), // RK: no grey fill under the icon
                            onClick = it,
                            modifier = Modifier.padding(start = 8.dp).size(46.dp),
                        ) {
                            UpIcon(navigationIcon = navigationIcon, contentDescription = navigationContentDescription)
                        }
                    }
                }
            },
            title = titleContent,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor ?: MaterialTheme.colorScheme.surfaceColorAtElevation(
                    elevation = if (isActionMode) 3.dp else 0.dp,
                ),
            ),
            scrollBehavior = scrollBehavior,
        )
    }
}

@Composable
fun AppBarTitle(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier) {
        title?.let {
            Text(
                text = it,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(
                    repeatDelayMillis = 2_000,
                ),
            )
        }
    }
}

@Composable
fun AppBarActions(
    actions: List<AppBar.AppBarAction>,
) {
    var showMenu by remember { mutableStateOf(false) }

    // RK --> DropSauce connected action pill: 42dp cells 2dp apart, round outer ends, 8dp inner corners
    val plain = actions.filterIsInstance<AppBar.Action>()
    val overflowActions = actions.filterIsInstance<AppBar.OverflowAction>()
    val cellCount = plain.size + if (overflowActions.isNotEmpty()) 1 else 0
    val outer = 21.dp
    val inner = 8.dp
    fun cellShape(index: Int) = RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        bottomStart = if (index == 0) outer else inner,
        topEnd = if (index == cellCount - 1) outer else inner,
        bottomEnd = if (index == cellCount - 1) outer else inner,
    )
    val cellModifier = Modifier.size(width = 42.dp, height = 42.dp)
    Row(
        modifier = Modifier.padding(end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        plain.forEachIndexed { index, it ->
            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = {
                    PlainTooltip {
                        Text(it.title)
                    }
                },
                state = rememberTooltipState(),
                focusable = false,
            ) {
                FilledTonalIconButton(
                    colors = noFillIconButtonColors(), // RK: no grey fill under the icon
                    onClick = it.onClick,
                    enabled = it.enabled,
                    modifier = cellModifier,
                    shape = cellShape(index),
                ) {
                    Icon(
                        imageVector = it.icon,
                        tint = it.iconTint ?: LocalContentColor.current,
                        contentDescription = it.title,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        if (overflowActions.isNotEmpty()) {
            // RK --> the DropSauce expressive menu blooms out of this box (the overflow button)
            Box {
                TooltipBox(
                    positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(MR.strings.action_menu_overflow_description))
                        }
                    },
                    state = rememberTooltipState(),
                    focusable = false,
                ) {
                    FilledTonalIconButton(
                        colors = noFillIconButtonColors(), // RK: no grey fill under the icon
                        onClick = { showMenu = !showMenu },
                        modifier = cellModifier,
                        shape = cellShape(cellCount - 1),
                    ) {
                        Icon(
                            MaterialSymbols.Rounded.MoreVert,
                            contentDescription = stringResource(MR.strings.action_menu_overflow_description),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                ExpressiveMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    val anyIcon = overflowActions.any { it.icon != null }
                    overflowActions.forEach {
                        ExpressiveMenuItem(
                            text = it.title,
                            onClick = {
                                it.onClick()
                                showMenu = false
                            },
                            leadingIcon = it.icon,
                            checked = it.checked,
                            hasSubmenu = it.hasSubmenu,
                            reserveIconSpace = anyIcon,
                        )
                    }
                }
            }
            // RK <--
        }
    }
    // RK <--
}

/**
 * @param searchEnabled Set to false if you don't want to show search action.
 * @param searchQuery If null, use normal toolbar.
 * @param placeholderText If null, [MR.strings.action_search_hint] is used.
 */
@Composable
private fun noFillIconButtonColors() = IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
)

@Composable
fun SearchToolbar(
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    modifier: Modifier = Modifier,
    titleContent: @Composable () -> Unit = {},
    navigateUp: (() -> Unit)? = null,
    searchEnabled: Boolean = true,
    placeholderText: String? = null,
    // RK: when set, the closed bar shows a centered search pill with this hint instead of the title
    pillHint: String? = null,
    onSearch: (String) -> Unit = {},
    onClickCloseSearch: () -> Unit = { onChangeSearchQuery(null) },
    // RK: a gear in the up-button slot while search is closed
    onClickSettings: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focusRequester = remember { FocusRequester() }
    val searchFieldState = rememberTextFieldState(searchQuery.orEmpty())
    val currentOnChangeSearchQuery by rememberUpdatedState(onChangeSearchQuery)
    val searchOpen by rememberUpdatedState(searchQuery != null)
    // RK: the field is authoritative while it holds focus, see the adopt branch below.
    var searchFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(searchFieldState) {
        snapshotFlow { searchFieldState.text.toString() }
            .drop(1)
            .collect { if (searchOpen) currentOnChangeSearchQuery(it) }
    }
    LaunchedEffect(searchQuery) {
        when {
            searchQuery == null -> searchFieldState.clearText()
            // RK: adopting an incoming query while the user is typing loses input. The value
            //     arriving here is whatever the last report produced, so every keystroke landing
            //     between that report and this write is overwritten and gone.
            !searchFieldFocused && searchQuery != searchFieldState.text.toString() -> {
                searchFieldState.setTextAndPlaceCursorAtEnd(searchQuery)
            }
        }
    }

    AppBar(
        modifier = modifier,
        titleContent = {
            if (searchQuery == null) {
                return@AppBar if (pillHint != null && searchEnabled) {
                    HeaderSearchPill(hint = pillHint, onClick = { onChangeSearchQuery("") })
                } else {
                    titleContent()
                }
            }

            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current

            val searchAndClearFocus: () -> Unit = f@{
                val query = searchFieldState.text.toString()
                if (query.isBlank()) return@f
                onSearch(query)
                focusManager.clearFocus()
                keyboardController?.hide()
                focusManager.moveFocus(FocusDirection.Next)
            }

            BasicTextField(
                state = searchFieldState,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { searchFieldFocused = it.isFocused } // RK
                    .runOnEnterKeyPressed(action = searchAndClearFocus)
                    .showSoftKeyboard(remember { searchQuery.isEmpty() })
                    .clearFocusOnSoftKeyboardHide(),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = { searchAndClearFocus() },
                lineLimits = TextFieldLineLimits.SingleLine,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                interactionSource = interactionSource,
                decorator = TextFieldDefaults.decorator(
                    state = searchFieldState,
                    enabled = true,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    outputTransformation = null,
                    interactionSource = interactionSource,
                    placeholder = {
                        Text(
                            modifier = Modifier.secondaryItemAlpha(),
                            text = (placeholderText ?: stringResource(MR.strings.action_search_hint)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                        )
                    },
                    // RK --> DropSauce search pill: tonal container, hairline outline, full round
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    container = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                        )
                    },
                    // RK <--
                ),
            )
        },
        navigateUp = when {
            searchQuery != null -> onClickCloseSearch
            onClickSettings != null -> onClickSettings
            else -> navigateUp
        },
        // RK -->
        navigationIcon = if (searchQuery == null && onClickSettings != null) MaterialSymbols.Rounded.Settings else null,
        navigationContentDescription = if (searchQuery == null && onClickSettings != null) {
            stringResource(MR.strings.label_settings)
        } else {
            null
        },
        // RK <--
        actions = {
            key("search") {
                val onClick = { onChangeSearchQuery("") }

                if (!searchEnabled || (pillHint != null && searchQuery == null)) {
                    // Don't show search action (the pill is the way in)
                } else if (searchQuery == null) {
                    TooltipBox(
                        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(MR.strings.action_search))
                            }
                        },
                        state = rememberTooltipState(),
                        focusable = false,
                    ) {
                        IconButton(
                            onClick = onClick,
                        ) {
                            Icon(
                                MaterialSymbols.Rounded.Search,
                                contentDescription = stringResource(MR.strings.action_search),
                            )
                        }
                    }
                } else if (searchQuery.isNotEmpty()) {
                    TooltipBox(
                        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(MR.strings.action_reset))
                            }
                        },
                        state = rememberTooltipState(),
                        focusable = false,
                    ) {
                        IconButton(
                            onClick = {
                                searchFieldState.clearText()
                                onClick()
                                focusRequester.requestFocus()
                            },
                        ) {
                            Icon(
                                MaterialSymbols.Rounded.Close,
                                contentDescription = stringResource(MR.strings.action_reset),
                            )
                        }
                    }
                }
            }

            key("actions") { actions() }
        },
        isActionMode = false,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun UpIcon(
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    contentDescription: String? = null, // RK
) {
    val icon = navigationIcon
        ?: MaterialSymbols.AutoMirroredRounded.ArrowBack
    Icon(
        imageVector = icon,
        contentDescription = contentDescription ?: stringResource(MR.strings.action_bar_up_description),
        modifier = modifier,
    )
}

sealed interface AppBar {
    sealed interface AppBarAction

    data class Action(
        val title: String,
        val icon: ImageVector,
        val iconTint: Color? = null,
        val onClick: () -> Unit,
        val enabled: Boolean = true,
    ) : AppBarAction

    data class OverflowAction(
        val title: String,
        val onClick: () -> Unit,
        // RK --> optional DropSauce decorations; null / false keeps the plain text row
        val icon: ImageVector? = null,
        val checked: Boolean? = null,
        val hasSubmenu: Boolean = false,
        // RK <--
    ) : AppBarAction
}

// RK --> the same centered search pill Browse's header uses, shown on every tab's top bar
@Composable
fun HeaderSearchPill(
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            MaterialSymbols.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = hint,
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
// RK <--
