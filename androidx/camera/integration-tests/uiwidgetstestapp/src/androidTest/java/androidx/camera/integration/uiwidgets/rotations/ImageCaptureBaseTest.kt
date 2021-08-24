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

package androidx.camera.integration.uiwidgets.rotations

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.integration.uiwidgets.R
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.TestRule
import java.util.concurrent.TimeUnit

/**
 * Base class for rotation image capture tests.
 *
 * The flow of the tests is:
 * - Launch the activity
 * - Wait fo the camera to be set up
 * - Rotate
 * - Wait a couple of frames
 * - Take a picture
 * - Wait for the image capture callback
 * - Verify the picture's rotation or resolution
 */
abstract class ImageCaptureBaseTest<A : CameraActivity> {

    @get:Rule
    val mUseCameraRule: TestRule = CameraUtil.grantCameraPermissionAndPreTest(testCameraRule)

    @get:Rule
    val mCameraActivityRules: GrantPermissionRule =
        GrantPermissionRule.grant(*CameraActivity.PERMISSIONS)

    protected val mDevice: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    protected fun setUp(lensFacing: Int) {
        // TODO(b/147448711) Cuttlefish seems to have an issue handling rotation. Might be
        //  related to the attached bug.
        assumeFalse(
            "Cuttlefish does not correctly handle rotating. Unable to test.",
            Build.MODEL.contains("Cuttlefish")
        )

        CoreAppTestUtil.assumeCompatibleDevice()
        assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        // Ensure it's in a natural orientation
        mDevice.setOrientationNatural()

        // Create pictures folder if it doesn't exist on the device. If this fails, abort test.
        assumeTrue("Failed to create pictures directory", createPicturesFolder())
    }

    protected fun tearDown() {
        CameraX.shutdown().get(10, TimeUnit.SECONDS)
        mDevice.unfreezeRotation()
    }

    @Suppress("DEPRECATION")
    private fun createPicturesFolder(): Boolean {
        val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (folder.exists()) {
            return true
        }
        return folder.mkdir()
    }

    protected inline fun <reified A : CameraActivity> verifyRotation(
        lensFacing: Int,
        captureMode: Int,
        rotate: ActivityScenario<A>.() -> Unit
    ) {
        val activityScenario: ActivityScenario<A> = launchActivity(lensFacing, captureMode)
        activityScenario.use { scenario ->

            // Wait until the camera is set up and analysis starts receiving frames
            scenario.waitOnCameraFrames()

            // Rotate
            rotate(scenario)

            // Reset received camera frames count
            scenario.resetFramesCount()

            // Wait a couple of frames after rotation
            scenario.waitOnCameraFrames()

            // Take picture
            scenario.onActivity {
                val view = it.findViewById<View>(R.id.previewView)
                view.performClick()
            }

            // Wait until a picture is taken and the capture callback is invoked
            val captureDone = scenario.withActivity { mCaptureDone }
            assertThat(captureDone.tryAcquire(TIMEOUT, TimeUnit.SECONDS)).isTrue()

            // If the camera HAL doesn't rotate the image, the captured image should contain a
            // rotation that's equal to the sensor rotation relative to target rotation
            val (sensorToTargetRotation, imageRotationDegrees) = scenario.withActivity {
                Pair(
                    getSensorRotationRelativeToCaptureTargetRotation(),
                    mCaptureResult?.getRotation()
                )
            }
            val areRotationsEqual = sensorToTargetRotation == imageRotationDegrees

            // If the camera HAL did rotate the image, verifying the image's rotation isn't
            // possible, so we make sure the image has the correct orientation/resolution.
            val (expectedResolution, imageSize) = scenario.withActivity {
                Pair(getCaptureResolution(), mCaptureResult?.getResolution())
            }
            val areResolutionsEqual = expectedResolution == imageSize

            assertWithMessage(
                "The captured image rotation degrees [$imageRotationDegrees] was expected to be " +
                    "equal to [$sensorToTargetRotation], or the captured image's resolution " +
                    "[$imageSize] was expected to be equal to [$expectedResolution]"
            )
                .that(areRotationsEqual || areResolutionsEqual)
                .isTrue()

            // Delete captured image
            scenario.withActivity { mCaptureResult?.delete() ?: Unit }
        }
    }

    protected inline fun <reified A : CameraActivity> launchActivity(
        lensFacing: Int,
        captureMode: Int
    ): ActivityScenario<A> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            A::class.java
        ).apply {
            putExtra(CameraActivity.KEY_LENS_FACING, lensFacing)
            putExtra(CameraActivity.KEY_IMAGE_CAPTURE_MODE, captureMode)
        }
        return ActivityScenario.launch<A>(intent)
    }

    protected inline fun <reified A : CameraActivity> ActivityScenario<A>.waitOnCameraFrames() {
        val analysisRunning = withActivity { mAnalysisRunning }
        assertWithMessage("Timed out waiting on image analysis frames")
            .that(analysisRunning.tryAcquire(IMAGES_COUNT, TIMEOUT, TimeUnit.SECONDS))
            .isTrue()
    }

    protected inline fun <reified A : CameraActivity> ActivityScenario<A>.resetFramesCount() {
        withActivity { mAnalysisRunning.drainPermits() }
    }

    companion object {
        protected const val IMAGES_COUNT = 30
        protected const val TIMEOUT = 10L
        @JvmStatic
        protected val captureModes = arrayOf(
            CameraActivity.IMAGE_CAPTURE_MODE_IN_MEMORY,
            CameraActivity.IMAGE_CAPTURE_MODE_FILE,
            CameraActivity.IMAGE_CAPTURE_MODE_OUTPUT_STREAM,
            CameraActivity.IMAGE_CAPTURE_MODE_MEDIA_STORE
        )
        @JvmStatic
        protected val lensFacing =
            arrayOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT)

        @JvmStatic
        lateinit var testCameraRule: CameraUtil.PreTestCamera

        @BeforeClass
        @JvmStatic
        fun classSetup() {
            testCameraRule = CameraUtil.PreTestCamera()
        }
    }
}