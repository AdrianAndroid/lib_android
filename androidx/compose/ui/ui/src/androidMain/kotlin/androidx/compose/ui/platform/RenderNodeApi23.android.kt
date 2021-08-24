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

package androidx.compose.ui.platform

import android.graphics.Outline
import android.view.RenderNode
import android.view.DisplayListCanvas
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.graphics.Path

/**
 * RenderNode on M-O devices, where RenderNode isn't officially supported. This class uses
 * a hidden android.view.RenderNode API that we have stubs for in the ui-android-stubs module.
 * This implementation has higher performance than the View implementation by both avoiding
 * reflection and using the lower overhead RenderNode instead of Views.
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class RenderNodeApi23(val ownerView: AndroidComposeView) : DeviceRenderNode {
    private val renderNode = RenderNode.create("Compose", ownerView)

    init {
        if (needToValidateAccess) {
            // This is only to force loading the DisplayListCanvas class and causing the
            // MRenderNode to fail with a NoClassDefFoundError during construction instead of
            // later.
            @Suppress("UNUSED_VARIABLE")
            val displayListCanvas: DisplayListCanvas? = null

            // Ensure that we can access properties of the RenderNode. We want to force an
            // exception here if there is a problem accessing any of these so that we can
            // fall back to the View implementation.
            renderNode.scaleX = renderNode.scaleX
            renderNode.scaleY = renderNode.scaleY
            renderNode.translationX = renderNode.translationX
            renderNode.translationY = renderNode.translationY
            renderNode.elevation = renderNode.elevation
            renderNode.rotation = renderNode.rotation
            renderNode.rotationX = renderNode.rotationX
            renderNode.rotationY = renderNode.rotationY
            renderNode.cameraDistance = renderNode.cameraDistance
            renderNode.pivotX = renderNode.pivotX
            renderNode.pivotY = renderNode.pivotY
            renderNode.clipToOutline = renderNode.clipToOutline
            renderNode.setClipToBounds(false)
            renderNode.alpha = renderNode.alpha
            renderNode.isValid // only read
            renderNode.setLeftTopRightBottom(0, 0, 0, 0)
            renderNode.offsetLeftAndRight(0)
            renderNode.offsetTopAndBottom(0)
            needToValidateAccess = false // only need to do this once
        }
        if (testFailCreateRenderNode) {
            throw NoClassDefFoundError()
        }
    }

    override val uniqueId: Long get() = 0

    override var left: Int = 0
    override var top: Int = 0
    override var right: Int = 0
    override var bottom: Int = 0
    override val width: Int get() = right - left
    override val height: Int get() = bottom - top

    override var scaleX: Float
        get() = renderNode.scaleX
        set(value) {
            renderNode.scaleX = value
        }

    override var scaleY: Float
        get() = renderNode.scaleY
        set(value) {
            renderNode.scaleY = value
        }

    override var translationX: Float
        get() = renderNode.translationX
        set(value) {
            renderNode.translationX = value
        }

    override var translationY: Float
        get() = renderNode.translationY
        set(value) {
            renderNode.translationY = value
        }

    override var elevation: Float
        get() = renderNode.elevation
        set(value) {
            renderNode.elevation = value
        }

    override var rotationZ: Float
        get() = renderNode.rotation
        set(value) {
            renderNode.rotation = value
        }

    override var rotationX: Float
        get() = renderNode.rotationX
        set(value) {
            renderNode.rotationX = value
        }

    override var rotationY: Float
        get() = renderNode.rotationY
        set(value) {
            renderNode.rotationY = value
        }

    override var cameraDistance: Float
        // Camera distance was negated in older API levels. Maintain the same input parameters
        // and negate the given camera distance before it is applied and also negate it when
        // it is queried
        get() = -renderNode.cameraDistance
        set(value) {
            renderNode.cameraDistance = -value
        }

    override var pivotX: Float
        get() = renderNode.pivotX
        set(value) {
            renderNode.pivotX = value
        }

    override var pivotY: Float
        get() = renderNode.pivotY
        set(value) {
            renderNode.pivotY = value
        }

    override var clipToOutline: Boolean
        get() = renderNode.clipToOutline
        set(value) {
            renderNode.clipToOutline = value
        }

    override var clipToBounds: Boolean = false
        set(value) {
            field = value
            renderNode.setClipToBounds(value)
        }

    override var alpha: Float
        get() = renderNode.alpha
        set(value) {
            renderNode.alpha = value
        }

    override val hasDisplayList: Boolean
        get() = renderNode.isValid

    override fun setOutline(outline: Outline?) {
        renderNode.setOutline(outline)
    }

    override fun setPosition(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
        return renderNode.setLeftTopRightBottom(left, top, right, bottom)
    }

    override fun offsetLeftAndRight(offset: Int) {
        left += offset
        right += offset
        renderNode.offsetLeftAndRight(offset)
    }

    override fun offsetTopAndBottom(offset: Int) {
        top += offset
        bottom += offset
        renderNode.offsetTopAndBottom(offset)
    }

    override fun record(
        canvasHolder: CanvasHolder,
        clipPath: Path?,
        drawBlock: (Canvas) -> Unit
    ) {
        val canvas = renderNode.start(width, height)
        canvasHolder.drawInto(canvas) {
            if (clipPath != null) {
                save()
                clipPath(clipPath)
            }
            drawBlock(this)
            if (clipPath != null) {
                restore()
            }
        }
        renderNode.end(canvas)
    }

    override fun getMatrix(matrix: android.graphics.Matrix) {
        return renderNode.getMatrix(matrix)
    }

    override fun getInverseMatrix(matrix: android.graphics.Matrix) {
        return renderNode.getInverseMatrix(matrix)
    }

    override fun drawInto(canvas: android.graphics.Canvas) {
        (canvas as DisplayListCanvas).drawRenderNode(renderNode)
    }

    override fun setHasOverlappingRendering(hasOverlappingRendering: Boolean): Boolean =
        renderNode.setHasOverlappingRendering(hasOverlappingRendering)

    override fun dumpRenderNodeData(): DeviceRenderNodeData =
        DeviceRenderNodeData(
            // Platform RenderNode for API level 23-29 does not provide bounds/dimension properties
            uniqueId = 0,
            left = 0,
            top = 0,
            right = 0,
            bottom = 0,
            width = 0,
            height = 0,
            scaleX = renderNode.scaleX,
            scaleY = renderNode.scaleY,
            translationX = renderNode.translationX,
            translationY = renderNode.translationY,
            elevation = renderNode.elevation,
            rotationZ = renderNode.rotation,
            rotationX = renderNode.rotationX,
            rotationY = renderNode.rotationY,
            cameraDistance = renderNode.cameraDistance,
            pivotX = renderNode.pivotX,
            pivotY = renderNode.pivotY,
            clipToOutline = renderNode.clipToOutline,
            // No getter on RenderNode for clipToBounds, always return the value we have configured
            // on it since this is a write only field
            clipToBounds = clipToBounds,
            alpha = renderNode.alpha
        )

    companion object {
        // Used by tests to force failing creating a RenderNode to simulate a device that
        // doesn't support RenderNodes before Q.
        internal var testFailCreateRenderNode = false

        // We need to validate that RenderNodes can be accessed before using the RenderNode
        // stub implementation, but we only need to validate it once. This flag indicates that
        // validation is still needed.
        private var needToValidateAccess = true
    }
}