/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.integration.core

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.integration.core.util.YuvToRgbConverter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.LabTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// Test the YubToRgbConverter to convert the input image from CameraX
@LargeTest
@RunWith(Parameterized::class)
class YuvToRgbConverterTest(
    private val implName: String,
    private val cameraXConfig: CameraXConfig
) {
    @get:Rule
    val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var camera: CameraUseCaseAdapter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        CameraX.initialize(context, cameraXConfig).get(10, TimeUnit.SECONDS)
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::camera.isInitialized) {
            // TODO: The removeUseCases() call might be removed after clarifying the
            // abortCaptures() issue in b/162314023
            withContext(Dispatchers.Main) {
                camera.removeUseCases(camera.useCases)
            }
        }
        CameraX.shutdown().get(10, TimeUnit.SECONDS)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun yubToRgbConverterTest() {
        val yuvToRgbConverter = YuvToRgbConverter(context)
        val countDownLatch = CountDownLatch(30)
        val imageAnalyzer = ImageAnalysis.Builder().build().also {
            it.setAnalyzer(
                Dispatchers.Main.asExecutor(),
                { image ->
                    var bitmap: Bitmap? = null
                    try {
                        bitmap = Bitmap.createBitmap(
                            image.width, image.height, Bitmap.Config.ARGB_8888
                        )
                        yuvToRgbConverter.yuvToRgb(image.image!!, bitmap)

                        // Test the YuvToRgbConverter#yuvToRgb can convert the image to bitmap
                        // successfully without any exception.
                        countDownLatch.countDown()
                    } finally {
                        bitmap?.recycle()
                        image.close()
                    }
                }
            )
        }

        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.DEFAULT_BACK_CAMERA,
            imageAnalyzer
        )

        assertTrue(countDownLatch.await(60, TimeUnit.SECONDS))
    }
}