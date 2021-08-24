/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.desktop.examples.example1

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.desktop.LocalAppWindow
import androidx.compose.desktop.Window
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.plus
import androidx.compose.ui.input.key.shortcuts
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.res.vectorXmlResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.platform.FontLoader
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.awt.event.MouseEvent

private const val title = "Desktop Compose Elements"

val italicFont = try {
    FontFamily(
        Font("NotoSans-Italic.ttf").also {
            // Check that font is loadable.
            FontLoader().load(it)
        }
    )
} catch (e: Exception) {
    FontFamily.SansSerif
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Window(title, IntSize(1024, 850)) {
        App()
    }
}

@Composable
private fun App() {
    val uriHandler = LocalUriHandler.current
    DesktopMaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                svgResource("androidx/compose/desktop/example/star.svg"),
                                contentDescription = "Star"
                            )
                            Text(title)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Open URL") },
                    onClick = {
                        uriHandler.openUri("https://google.com")
                    }
                )
            },
            isFloatingActionButtonDocked = true,
            bottomBar = {
                BottomAppBar(cutoutShape = CircleShape) {
                    IconButton(
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Menu, "Menu", Modifier.size(ButtonDefaults.IconSize))
                    }
                }
            },
            content = {
                Row(Modifier.padding(bottom = 56.dp)) {
                    LeftColumn(Modifier.weight(1f))
                    RightColumn(Modifier.width(200.dp))
                }
            }
        )
    }
}

@Composable
private fun LeftColumn(modifier: Modifier) = Box(modifier.fillMaxSize()) {
    val state = rememberScrollState()
    ScrollableContent(state)

    VerticalScrollbar(
        rememberScrollbarAdapter(state),
        Modifier.align(Alignment.CenterEnd).fillMaxHeight()
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ScrollableContent(scrollState: ScrollState) {
    val amount = remember { mutableStateOf(0f) }
    val animation = remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
        val window = LocalAppWindow.current.window
        val info = "${window.renderApi} (${window.windowHandle})"
        Text(
            text = "Привет! 你好! Desktop Compose use $info: ${amount.value}",
            color = Color.Black,
            modifier = Modifier
                .background(Color.Blue)
                .height(56.dp)
                .wrapContentSize(Alignment.Center)
        )

        val inlineIndicatorId = "indicator"

        Text(
            text = buildAnnotatedString {
                append("The quick ")
                if (animation.value) {
                    appendInlineContent(inlineIndicatorId)
                }
                pushStyle(
                    SpanStyle(
                        color = Color(0xff964B00),
                        shadow = Shadow(Color.Green, offset = Offset(1f, 1f))
                    )
                )
                append("brown fox")
                pop()
                pushStyle(SpanStyle(background = Color.Yellow))
                append(" 🦊 ate a ")
                pop()
                pushStyle(SpanStyle(fontSize = 30.sp, textDecoration = Underline))
                append("zesty hamburgerfons")
                pop()
                append(" 🍔.\nThe 👩‍👩‍👧‍👧 laughed.")
                addStyle(SpanStyle(color = Color.Green), 25, 35)
            },
            color = Color.Black,
            inlineContent = mapOf(
                inlineIndicatorId to InlineTextContent(
                    Placeholder(
                        width = 1.em,
                        height = 1.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                    )
                ) {
                    CircularProgressIndicator(Modifier.padding(end = 3.dp))
                }
            )
        )

        val loremColors = listOf(
            Color.Black,
            Color.Yellow,
            Color.Green,
            Color.Blue
        )
        var loremColor by remember { mutableStateOf(0) }

        val loremDecorations = listOf(
            TextDecoration.None,
            TextDecoration.Underline,
            TextDecoration.LineThrough
        )
        val lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do" +
            " eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad" +
            " minim veniam, quis nostrud exercitation ullamco laboris nisi ut" +
            " aliquipex ea commodo consequat. Duis aute irure dolor in reprehenderit" +
            " in voluptate velit esse cillum dolore eu fugiat nulla pariatur." +
            " Excepteur" +
            " sint occaecat cupidatat non proident, sunt in culpa qui officia" +
            " deserunt mollit anim id est laborum."
        var loremDecoration by remember { mutableStateOf(0) }
        Text(
            text = lorem,
            color = loremColors[loremColor],
            textAlign = TextAlign.Center,
            textDecoration = loremDecorations[loremDecoration],
            modifier = Modifier.clickable {
                if (loremColor < loremColors.size - 1) {
                    loremColor += 1
                } else {
                    loremColor = 0
                }

                if (loremDecoration < loremDecorations.size - 1) {
                    loremDecoration += 1
                } else {
                    loremDecoration = 0
                }
            }
        )

        Text(
            text = lorem,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                "Default",
            )

            Text(
                "SansSerif",
                fontFamily = FontFamily.SansSerif
            )

            Text(
                "Serif",
                fontFamily = FontFamily.Serif
            )

            Text(
                "Monospace",
                fontFamily = FontFamily.Monospace
            )

            Text(
                "Cursive",
                fontFamily = FontFamily.Cursive
            )
        }

        var overText by remember { mutableStateOf("Move mouse over text:") }
        Text(overText, style = TextStyle(letterSpacing = 10.sp))

        SelectionContainer {
            Text(
                text = "fun <T : Comparable<T>> List<T>.quickSort(): List<T> = when {\n" +
                    "  size < 2 -> this\n" +
                    "  else -> {\n" +
                    "    val pivot = first()\n" +
                    "    val (smaller, greater) = drop(1).partition { it <= pivot }\n" +
                    "    smaller.quickSort() + pivot + greater.quickSort()\n" +
                    "   }\n" +
                    "}",
                fontFamily = italicFont,
                modifier = Modifier.padding(10.dp).pointerMoveFilter(
                    onMove = {
                        overText = "Move position: $it"
                        false
                    },
                    onEnter = {
                        overText = "Over enter"
                        false
                    },
                    onExit = {
                        overText = "Over exit"
                        false
                    }
                )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            var lastEvent by remember { mutableStateOf<MouseEvent?>(null) }
            Button(
                modifier = Modifier.padding(4.dp),
                onClick = {
                    amount.value++
                }
            ) {
                Text("Base")
            }

            Text(
                modifier = object : PointerInputModifier {
                    override val pointerInputFilter = object : PointerInputFilter() {
                        override fun onPointerEvent(
                            pointerEvent: PointerEvent,
                            pass: PointerEventPass,
                            bounds: IntSize
                        ) {
                            lastEvent = pointerEvent.mouseEvent
                        }

                        override fun onCancel() {
                        }
                    }
                },
                text = "Custom mouse event button: ${lastEvent?.button}"
            )
        }

        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Row(modifier = Modifier.padding(4.dp)) {
                    Checkbox(
                        animation.value,
                        onCheckedChange = {
                            animation.value = it
                        }
                    )
                    Text("Animation")
                }

                Button(
                    modifier = Modifier.padding(4.dp),
                    onClick = {
                        AppWindow(size = IntSize(400, 200)).also {
                            it.keyboard.setShortcut(Key.Escape) {
                                it.close()
                            }
                        }.show {
                            Animations(isCircularEnabled = animation.value)
                        }
                    }
                ) {
                    Text("Window")
                }
            }

            Animations(isCircularEnabled = animation.value)
        }

        Slider(
            value = amount.value / 100f,
            onValueChange = { amount.value = (it * 100) }
        )
        TextField(
            value = amount.value.toString(),
            onValueChange = { amount.value = it.toFloatOrNull() ?: 42f },
            label = { Text(text = "Input1") }
        )

        val (focusItem1, focusItem2) = FocusRequester.createRefs()
        val text = remember {
            mutableStateOf("Hello \uD83E\uDDD1\uD83C\uDFFF\u200D\uD83E\uDDB0")
        }
        TextField(
            value = text.value,
            onValueChange = { text.value = it },
            label = { Text(text = "Input2") },
            placeholder = {
                Text(text = "Important input")
            },
            maxLines = 1,
            modifier = Modifier.shortcuts {
                on(Key.MetaLeft + Key.ShiftLeft + Key.Enter) {
                    text.value = "Cleared with shift!"
                }
                on(Key.MetaLeft + Key.Enter) {
                    text.value = "Cleared!"
                }
            }.focusOrder(focusItem1) {
                next = focusItem2
            }
        )

        var text2 by remember {
            val initText = buildString {
                (1..1000).forEach {
                    append("$it\n")
                }
            }
            mutableStateOf(initText)
        }
        TextField(
            text2,
            modifier = Modifier.height(200.dp).focusOrder(focusItem2) {
                previous = focusItem1
            },
            onValueChange = { text2 = it }
        )

        Row {
            Image(
                imageResource("androidx/compose/desktop/example/circus.jpg"),
                "Localized description",
                Modifier.size(200.dp)
            )

            Icon(
                vectorXmlResource("androidx/compose/desktop/example/ic_baseline_deck_24.xml"),
                "Localized description",
                Modifier.size(100.dp).align(Alignment.CenterVertically),
                tint = Color.Blue.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun Animations(isCircularEnabled: Boolean) = Row {
    if (isCircularEnabled) {
        CircularProgressIndicator(Modifier.padding(10.dp))
    }

    val enabled = remember { mutableStateOf(true) }
    val color by animateColorAsState(
        if (enabled.value) Color.Green else Color.Red,
        animationSpec = TweenSpec(durationMillis = 2000)
    )

    MaterialTheme {
        Box(
            Modifier
                .size(70.dp)
                .clickable { enabled.value = !enabled.value }
                .background(color)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RightColumn(modifier: Modifier) = Box {
    val state = rememberLazyListState()
    val itemCount = 100000

    LazyColumn(modifier.graphicsLayer(alpha = 0.5f), state = state) {
        items((1..itemCount).toList()) { x ->
            val itemHeight = 20.dp + 20.dp * Math.random().toFloat()
            Text(x.toString(), Modifier.graphicsLayer(alpha = 0.5f).height(itemHeight))
        }
    }

    VerticalScrollbar(
        rememberScrollbarAdapter(state),
        Modifier.align(Alignment.CenterEnd)
    )
}
