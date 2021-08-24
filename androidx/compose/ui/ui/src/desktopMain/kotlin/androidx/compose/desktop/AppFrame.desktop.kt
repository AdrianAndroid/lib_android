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
package androidx.compose.desktop

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.v1.MenuBar
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import javax.swing.JFrame

/**
 * AppFrame is an abstract class that represents a window.
 * <p>
 * Known subclasses: AppWindow
 */
abstract class AppFrame {

    /**
     * Gets ComposeWindow object.
     */
    abstract val window: ComposeWindow

    internal var menuBar: MenuBar? = null

    /**
     * Gets the parent window. If the value is not null, the current window is a dialog.
     */
    var invoker: AppFrame? = null
        protected set

    /**
     * Gets the title of the window. The title is displayed in the windows's native border.
     */
    val title: String
        get() = window.title

    /**
     * Gets the width of the window.
     */
    val width: Int
        get() = window.width

    /**
     * Gets the height of the window.
     */
    val height: Int
        get() = window.height

    /**
     * Gets the current x coordinate of the window.
     */
    val x: Int
        get() = window.x

    /**
     * Gets the current y coordinate of the window.
     */
    val y: Int
        get() = window.y

    /**
     * Returns true if the winodw is closed, false otherwise.
     */
    var isClosed: Boolean = false
        internal set

    /**
     * Returns the icon image of the window. If the icon is not set, null is returned.
     */
    var icon: BufferedImage? = null
        internal set

    /**
     * Returns events of the window. Each event is described as a lambda that is invoked when
     * needed.
     */
    var events: WindowEvents = WindowEvents()
        internal set

    internal var onDispose: (() -> Unit)? = null

    internal var onDismiss: (() -> Unit)? = null

    /**
     * Sets the title of the window.
     *
     * @param title Window title text.
     */
    abstract fun setTitle(title: String)

    /**
     * Sets the image icon of the window.
     *
     * @param image Image of the icon.
     */
    abstract fun setIcon(image: BufferedImage?)

    /**
     * Sets the menu bar of the window. The menu bar can be displayed inside a window (Windows,
     * Linux) or at the top of the screen (Mac OS).
     *
     * @param menuBar Window menu bar.
     */
    abstract fun setMenuBar(menuBar: MenuBar)

    /**
     * Removes the menu bar of the window.
     */
    abstract fun removeMenuBar()

    /**
     * Switches the window to fullscreen mode if the window is resizable. If the window is in
     * fullscreen mode [minimize] and [maximize] methods are ignored.
     */
    abstract fun makeFullscreen()

    /**
     * Returns true if the window is in fullscreen state, false otherwise.
     */
    abstract val isFullscreen: Boolean
        get

    /**
     * Minimizes the window to the taskbar.
     */
    abstract fun minimize()

    /**
     * Returns true if the window is minimized, false otherwise.
     */
    val isMinimized: Boolean
        get() = window.extendedState == JFrame.ICONIFIED

    /**
     * Maximizes the window to fill all available screen space.
     */
    abstract fun maximize()

    /**
     * Returns true if the window is maximized, false otherwise.
     */
    val isMaximized: Boolean
        get() = window.extendedState == JFrame.MAXIMIZED_BOTH

    /**
     * Restores the previous state and size of the window after
     * maximizing/minimizing/fullscreen mode.
     */
    abstract fun restore()

    /**
     * Sets the ability to resize the window. True - the window can be resized,
     * false - the window cannot be resized. If the window is in fullscreen mode
     * setter of this property is ignored. If this property is true the [makeFullscreen()]
     * method is ignored.
     */
    abstract var resizable: Boolean

    /**
     * Sets the new position of the window on the screen.
     *
     * @param x the new x-coordinate of the window.
     * @param y the new y-coordinate of the window.
     */
    abstract fun setLocation(x: Int, y: Int)

    /**
     * Sets the window to the center of the screen.
     */
    abstract fun setWindowCentered()

    /**
     * Sets the new size of the window.
     *
     * @param width the new width of the window.
     * @param height the new height of the window.
     */
    abstract fun setSize(width: Int, height: Int)

    /**
     * Adds the specified mouse listener to receive mouse events from layer component.
     * @param listener the mouse listener.
     */
    abstract fun addMouseListener(listener: MouseListener)

    /**
     * Removes the specified mouse listener so that it no longer receives mouse events
     * from layer component.
     * @param listener the mouse listener.
     */
    abstract fun removeMouseListener(listener: MouseListener)

    /**
     * Adds the specified mouse motion listener to receive mouse events from layer component.
     * @param listener the mouse listener.
     */
    abstract fun addMouseMotionListener(listener: MouseMotionListener)

    /**
     * Removes the specified mouse motion listener so that it no longer receives mouse events
     * from layer component.
     * @param listener the mouse listener.
     */
    abstract fun removeMouseMotionListener(listener: MouseMotionListener)

    // TODO(demin): uncomment this after b/175234629 will be fixed
//    /**
//     * Shows a window with the given Compose content.
//     *
//     * @param parentComposition The parent composition reference to coordinate
//     *        scheduling of composition updates.
//     *        If null then default root composition will be used.
//     * @param content Composable content of the window.
//     */
//    abstract fun show(
//        parentComposition: CompositionContext? = null,
//        content: @Composable () -> Unit
//    )

    /**
     * Closes the window.
     */
    abstract fun close()

    internal abstract fun dispose()

    internal abstract fun connectPair(window: AppFrame)

    internal abstract fun disconnectPair()

    internal abstract fun lockWindow()

    internal abstract fun unlockWindow()
}

/**
 * WindowEvents is the class that contains all the events supported by window.
 *
 * @param onOpen The event that is invoked after the window appears.
 * @param onClose The event that is invoked after the window is closed.
 * @param onMinimize The event that is invoked after the window is minimized.
 * @param onMaximize The event that is invoked after the window is maximized.
 * @param onRestore The event that is invoked when the window is restored after the window has been
 * maximized or minimized.
 * @param onFocusGet The event that is invoked when the window gets focus.
 * @param onFocusLost The event that is invoked when the window lost focus.
 * @param onResize The event that is invoked when the window is resized.
 * @param onRelocate The event that is invoked when the window is relocated.
 */
class WindowEvents(
    var onOpen: (() -> Unit)? = null,
    var onClose: (() -> Unit)? = null,
    var onMinimize: (() -> Unit)? = null,
    var onMaximize: (() -> Unit)? = null,
    var onRestore: (() -> Unit)? = null,
    var onFocusGet: (() -> Unit)? = null,
    var onFocusLost: (() -> Unit)? = null,
    var onResize: ((IntSize) -> Unit)? = null,
    var onRelocate: ((IntOffset) -> Unit)? = null
) {

    internal fun invokeOnOpen() {
        onOpen?.invoke()
    }

    internal fun invokeOnClose() {
        onClose?.invoke()
    }

    internal fun invokeOnMinimize() {
        onMinimize?.invoke()
    }

    internal fun invokeOnMaximize() {
        onMaximize?.invoke()
    }

    internal fun invokeOnRestore() {
        onRestore?.invoke()
    }

    internal fun invokeOnFocusGet() {
        onFocusGet?.invoke()
    }

    internal fun invokeOnFocusLost() {
        onFocusLost?.invoke()
    }

    internal fun invokeOnResize(size: IntSize) {
        onResize?.invoke(size)
    }

    internal fun invokeOnRelocate(location: IntOffset) {
        onRelocate?.invoke(location)
    }
}
