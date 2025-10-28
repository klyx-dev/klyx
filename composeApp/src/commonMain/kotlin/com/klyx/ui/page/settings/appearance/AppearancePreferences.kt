package com.klyx.ui.page.settings.appearance

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.klyx.AppRoute
import com.klyx.core.LocalPaletteStyleIndex
import com.klyx.core.LocalSeedColor
import com.klyx.core.settings.LocalAppSettings
import com.klyx.core.settings.STYLE_MONOCHROME
import com.klyx.core.settings.STYLE_TONAL_SPOT
import com.klyx.core.settings.paletteStyles
import com.klyx.core.settings.update
import com.klyx.core.theme.Appearance
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.PreferenceSwitch
import com.klyx.core.ui.component.PreferenceSwitchWithDivider
import com.klyx.res.Res
import com.klyx.res.dark_theme
import com.klyx.res.dynamic_color
import com.klyx.res.dynamic_color_desc
import com.klyx.res.follow_system
import com.klyx.res.look_and_feel
import com.klyx.res.off
import com.klyx.res.on
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import com.kyant.monet.a1
import com.kyant.monet.a2
import com.kyant.monet.a3
import io.material.hct.Hct
import org.jetbrains.compose.resources.stringResource

private val ColorList = ((4..10) + (1..3)).map { it * 35.0 }.map {
    Color(Hct.from(it, 40.0, 40.0).toInt())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePreferences(onNavigateBack: () -> Unit, onNavigateTo: (Any) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appSettings = LocalAppSettings.current
    val isDynamicColor = appSettings.dynamicColor

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.look_and_feel)) },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(padding)
        ) {
            val pageCount = ColorList.size + 1

            val pagerState = rememberPagerState(
                initialPage = if (LocalPaletteStyleIndex.current == STYLE_MONOCHROME) {
                    pageCount
                } else {
                    ColorList.indexOf(Color(LocalSeedColor.current)).let { if (it == -1) 0 else it }
                }
            ) { pageCount }

            HorizontalPager(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                contentPadding = PaddingValues(horizontal = 12.dp),
                state = pagerState
            ) { page ->
                if (page < pageCount - 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        ColorButtons(ColorList[page])
                    }
                } else {
                    // ColorButton for Monochrome theme
                    val isSelected = LocalPaletteStyleIndex.current == STYLE_MONOCHROME && !isDynamicColor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        ColorButtonImpl(
                            modifier = Modifier,
                            isSelected = { isSelected },
                            tonalPalettes = Color.Black.toTonalPalettes(PaletteStyle.Monochrome),
                            onClick = {
                                appSettings.update {
                                    it.copy(
                                        dynamicColor = false,
                                        seedColor = Color.Black.toArgb(),
                                        paletteStyleIndex = STYLE_MONOCHROME
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }

                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(6.dp)
                    )
                }
            }

            PreferenceSwitch(
                title = stringResource(Res.string.dynamic_color),
                description = stringResource(Res.string.dynamic_color_desc),
                icon = Icons.Outlined.Colorize,
                isChecked = appSettings.dynamicColor,
                onClick = { dynamicColor ->
                    appSettings.update { it.copy(dynamicColor = dynamicColor) }
                }
            )

            val isDarkTheme = LocalIsDarkMode.current

            PreferenceSwitchWithDivider(
                title = stringResource(Res.string.dark_theme),
                icon = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                isChecked = isDarkTheme,
                description = appSettings.appearance.desc(),
                onChecked = { checked ->
                    appSettings.update {
                        it.copy(appearance = if (checked) Appearance.Dark else Appearance.Light)
                    }
                },
                onClick = { onNavigateTo(AppRoute.Settings.DarkTheme) },
            )
        }
    }
}

@Composable
private fun Appearance.desc() = when (this) {
    Appearance.Light -> stringResource(Res.string.off)
    Appearance.Dark -> stringResource(Res.string.on)
    Appearance.System -> stringResource(Res.string.follow_system)
}

@Composable
fun RowScope.ColorButtons(color: Color) {
    paletteStyles.subList(STYLE_TONAL_SPOT, STYLE_MONOCHROME).forEachIndexed { index, style ->
        ColorButton(color = color, index = index, tonalStyle = style)
    }
}

@Composable
fun RowScope.ColorButton(
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    index: Int = 0,
    tonalStyle: PaletteStyle = PaletteStyle.TonalSpot,
) {
    val appSettings = LocalAppSettings.current
    val isDynamicColor = appSettings.dynamicColor

    val tonalPalettes by remember { mutableStateOf(color.toTonalPalettes(tonalStyle)) }
    val isSelect =
        !isDynamicColor && LocalSeedColor.current == color.toArgb() && LocalPaletteStyleIndex.current == index

    ColorButtonImpl(modifier = modifier, tonalPalettes = tonalPalettes, isSelected = { isSelect }) {
        appSettings.update {
            it.copy(
                dynamicColor = false,
                seedColor = color.toArgb(),
                paletteStyleIndex = index
            )
        }
    }
}

@Composable
fun RowScope.ColorButtonImpl(
    modifier: Modifier = Modifier,
    isSelected: () -> Boolean = { false },
    tonalPalettes: TonalPalettes,
    cardColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit = {},
) {

    val containerSize by animateDpAsState(targetValue = if (isSelected.invoke()) 28.dp else 0.dp)
    val iconSize by animateDpAsState(targetValue = if (isSelected.invoke()) 16.dp else 0.dp)

    Surface(
        modifier =
            modifier
                .padding(4.dp)
                .sizeIn(maxHeight = 80.dp, maxWidth = 80.dp, minHeight = 64.dp, minWidth = 64.dp)
                .weight(1f, false)
                .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        onClick = onClick,
    ) {
        CompositionLocalProvider(LocalTonalPalettes provides tonalPalettes) {
            val color1 = 80.a1
            val color2 = 90.a2
            val color3 = 60.a3
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .drawBehind { drawCircle(color1) }
                            .align(Alignment.Center)
                ) {
                    Surface(
                        color = color2,
                        modifier = Modifier.align(Alignment.BottomStart).size(24.dp),
                    ) {}
                    Surface(
                        color = color3,
                        modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
                    ) {}
                    Box(
                        modifier =
                            Modifier.align(Alignment.Center)
                                .clip(CircleShape)
                                .size(containerSize)
                                .drawBehind { drawCircle(containerColor) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
