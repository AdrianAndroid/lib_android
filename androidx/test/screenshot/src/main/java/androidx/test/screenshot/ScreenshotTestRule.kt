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

package androidx.test.screenshot

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.matchers.BitmapMatcher
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.test.screenshot.matchers.PixelPerfectMatcher
import androidx.test.screenshot.proto.ScreenshotResultProto
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Config for [ScreenshotTestRule].
 *
 * To be used to set up paths to golden images. These paths are not used to retrieve the goldens
 * during the test. They are just directly stored into the result proto file. The proto file can
 * then be used by CI to determined where to put the new approved goldens. Your tests assets
 * directory should be pointing to exactly the same path.
 *
 * @param repoRootPathForGoldens Path to the repo's root that contains the goldens. To be used by
 * CI.
 * @param pathToGoldensInRepo Relative path to goldens inside your [repoRootPathForGoldens].
 */
class ScreenshotTestRuleConfig(
    val repoRootPathForGoldens: String = "",
    val pathToGoldensInRepo: String = ""
)

/**
 * Type of file that can be produced by the [ScreenshotTestRule].
 */
internal enum class OutputFileType {
    IMAGE_ACTUAL,
    IMAGE_EXPECTED,
    IMAGE_DIFF,
    RESULT_PROTO
}

/**
 * Rule to be added to a test to facilitate screenshot testing.
 *
 * This rule records current test name and when instructed it will perform the given bitmap
 * comparison against the given golden. All the results (including result proto file) are stored
 * into the device to be retrieved later.
 *
 * @param config To configure where this rule should look for goldens.
 *
 * @see Bitmap.assertAgainstGolden
 */
@SuppressLint("SyntheticAccessor")
open class ScreenshotTestRule(
    config: ScreenshotTestRuleConfig = ScreenshotTestRuleConfig()
) : TestRule {

    /**
     * Directory on the device that is used to store the output files.
     */
    val deviceOutputDirectory
        get() = File(
            InstrumentationRegistry.getInstrumentation().getContext().externalCacheDir,
            "androidx_screenshots"
        )

    private val repoRootPathForGoldens = config.repoRootPathForGoldens.trim('/')
    private val pathToGoldensInRepo = config.pathToGoldensInRepo.trim('/')
    private val imageExtension = ".png"
    // This is used in CI to identify the files.
    private val resultProtoFileSuffix = "goldResult.textproto"

    // Magic number for an in-progress status report
    private val bundleStatusInProgress = 2
    private val bundleKeyPrefix = "androidx_screenshots_"

    private lateinit var testIdentifier: String
    private lateinit var deviceId: String

    private var goldenIdentifierResolver: ((String) -> String) = ::resolveGoldenName

    private val testWatcher = object : TestWatcher() {
        override fun starting(description: Description?) {
            deviceId = getDeviceModel()
            testIdentifier = "${description!!.className}_${description.methodName}_$deviceId"
        }
    }

    override fun apply(base: Statement, description: Description?): Statement {
        return ScreenshotTestStatement(base)
            .run { testWatcher.apply(this, description) }
    }

    class ScreenshotTestStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            // We currently only support Cuttlefish API 29 because of the storage access.
            Assume.assumeTrue("Requires Cuttlefish", Build.MODEL.contains("Cuttlefish"))
            Assume.assumeTrue(
                "Requires SDK 29.",
                Build.VERSION.SDK_INT == 29
            )
            base.evaluate()
        }
    }

    internal fun setCustomGoldenIdResolver(resolver: ((String) -> String)) {
        goldenIdentifierResolver = resolver
    }

    internal fun clearCustomGoldenIdResolver() {
        goldenIdentifierResolver = ::resolveGoldenName
    }

    private fun resolveGoldenName(goldenIdentifier: String): String {
        return "${goldenIdentifier}_$deviceId$imageExtension"
    }

    private fun fetchExpectedImage(goldenIdentifier: String): Bitmap? {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

        try {
            context.assets.open(goldenIdentifierResolver(goldenIdentifier)).use {
                return BitmapFactory.decodeStream(it)
            }
        } catch (e: FileNotFoundException) {
            // Golden not present
            return null
        }
    }

    /**
     * Asserts the given bitmap against the golden identified by the given name.
     *
     * Note: The golden identifier should be unique per your test module (unless you want multiple
     * tests to match the same golden). The name must not contain extension. You should also avoid
     * adding strings like "golden", "image" and instead describe what is the golder referring to.
     *
     * @param actual The bitmap captured during the test.
     * @param goldenIdentifier Name of the golden. Allowed characters: 'A-Za-z0-9_-'
     * @param matcher The algorithm to be used to perform the matching.
     *
     * @see MSSIMMatcher
     * @see PixelPerfectMatcher
     * @see Bitmap.assertAgainstGolden
     *
     * @throws IllegalArgumentException If the golden identifier contains forbidden characters or
     * is empty.
     */
    fun assertBitmapAgainstGolden(
        actual: Bitmap,
        goldenIdentifier: String,
        matcher: BitmapMatcher
    ) {
        if (!goldenIdentifier.matches("^[A-Za-z0-9_-]+$".toRegex())) {
            throw IllegalArgumentException(
                "The given golden identifier '$goldenIdentifier' does not satisfy the naming " +
                    "requirement. Allowed characters are: '[A-Za-z0-9_-]'"
            )
        }

        val expected = fetchExpectedImage(goldenIdentifier)
        if (expected == null) {
            reportResult(
                status = ScreenshotResultProto.ScreenshotResult.Status.MISSING_GOLDEN,
                goldenIdentifier = goldenIdentifier,
                actual = actual
            )
            throw AssertionError(
                "Missing golden image " +
                    "'${goldenIdentifierResolver(goldenIdentifier)}'. " +
                    "Did you mean to check in a new image?"
            )
        }

        if (actual.width != expected.width || actual.height != expected.height) {
            reportResult(
                status = ScreenshotResultProto.ScreenshotResult.Status.SIZE_MISMATCH,
                goldenIdentifier = goldenIdentifier,
                actual = actual,
                expected = expected
            )
            throw AssertionError(
                "Sizes are different! Expected: [${expected.width}, ${expected
                    .height}], Actual: [${actual.width}, ${actual.height}]"
            )
        }

        val comparisonResult = matcher.compareBitmaps(
            expected = expected.toIntArray(),
            given = actual.toIntArray(),
            width = actual.width,
            height = actual.height
        )

        val status = if (comparisonResult.matches) {
            ScreenshotResultProto.ScreenshotResult.Status.PASSED
        } else {
            ScreenshotResultProto.ScreenshotResult.Status.FAILED
        }

        reportResult(
            status = status,
            goldenIdentifier = goldenIdentifier,
            actual = actual,
            comparisonStatistics = comparisonResult.comparisonStatistics,
            expected = expected,
            diff = comparisonResult.diff
        )

        if (!comparisonResult.matches) {
            throw AssertionError(
                "Image mismatch! Comparison stats: '${comparisonResult
                    .comparisonStatistics}'"
            )
        }
    }

    private fun reportResult(
        status: ScreenshotResultProto.ScreenshotResult.Status,
        goldenIdentifier: String,
        actual: Bitmap,
        comparisonStatistics: String? = null,
        expected: Bitmap? = null,
        diff: Bitmap? = null
    ) {
        val resultProto = ScreenshotResultProto.ScreenshotResult
            .newBuilder()
            .setResult(status)

        resultProto.comparisonStatistics = comparisonStatistics.orEmpty()
        resultProto.repoRootPath = repoRootPathForGoldens
        resultProto.locationOfGoldenInRepo =
            if (pathToGoldensInRepo.isEmpty()) {
                goldenIdentifierResolver(goldenIdentifier)
            } else {
                "$pathToGoldensInRepo/${goldenIdentifierResolver(goldenIdentifier)}"
            }

        val report = Bundle()

        if (status != ScreenshotResultProto.ScreenshotResult.Status.PASSED) {
            actual.writeToDevice(OutputFileType.IMAGE_ACTUAL).also {
                resultProto.currentScreenshotFileName = it.name
                report.putString(bundleKeyPrefix + OutputFileType.IMAGE_ACTUAL, it.absolutePath)
            }
            diff?.run {
                writeToDevice(OutputFileType.IMAGE_DIFF).also {
                    resultProto.diffImageFileName = it.name
                    report.putString(bundleKeyPrefix + OutputFileType.IMAGE_DIFF, it.absolutePath)
                }
            }
            expected?.run {
                writeToDevice(OutputFileType.IMAGE_EXPECTED).also {
                    resultProto.expectedImageFileName = it.name
                    report.putString(
                        bundleKeyPrefix + OutputFileType.IMAGE_EXPECTED,
                        it.absolutePath
                    )
                }
            }
        }

        writeToDevice(OutputFileType.RESULT_PROTO) {
            it.write(resultProto.build().toString().toByteArray())
        }.also {
            report.putString(bundleKeyPrefix + OutputFileType.RESULT_PROTO, it.absolutePath)
        }

        InstrumentationRegistry.getInstrumentation().sendStatus(bundleStatusInProgress, report)
    }

    internal fun getPathOnDeviceFor(fileType: OutputFileType): File {
        val fileName = when (fileType) {
            OutputFileType.IMAGE_ACTUAL -> "${testIdentifier}_actual$imageExtension"
            OutputFileType.IMAGE_EXPECTED -> "${testIdentifier}_expected$imageExtension"
            OutputFileType.IMAGE_DIFF -> "${testIdentifier}_diff$imageExtension"
            OutputFileType.RESULT_PROTO -> "${testIdentifier}_$resultProtoFileSuffix"
        }
        return File(deviceOutputDirectory, fileName)
    }

    private fun Bitmap.writeToDevice(fileType: OutputFileType): File {
        return writeToDevice(fileType) {
            compress(Bitmap.CompressFormat.PNG, 0 /*ignored for png*/, it)
        }
    }

    private fun writeToDevice(
        fileType: OutputFileType,
        writeAction: (FileOutputStream) -> Unit
    ): File {
        if (!deviceOutputDirectory.exists() && !deviceOutputDirectory.mkdir()) {
            throw IOException("Could not create folder.")
        }

        var file = getPathOnDeviceFor(fileType)
        try {
            FileOutputStream(file).use {
                writeAction(it)
            }
        } catch (e: Exception) {
            throw IOException(
                "Could not write file to storage (path: ${file.absolutePath}). " +
                    " Stacktrace: " + e.stackTrace
            )
        }
        return file
    }

    private fun getDeviceModel(): String {
        var model = android.os.Build.MODEL.lowercase()
        arrayOf("phone", "x86", "x64", "gms").forEach {
            model = model.replace(it, "")
        }
        return model.trim().replace(" ", "_")
    }
}

internal fun Bitmap.toIntArray(): IntArray {
    val bitmapArray = IntArray(width * height)
    getPixels(bitmapArray, 0, width, 0, 0, width, height)
    return bitmapArray
}

/**
 * Asserts this bitmap against the golden identified by the given name.
 *
 * Note: The golden identifier should be unique per your test module (unless you want multiple tests
 * to match the same golden). The name must not contain extension. You should also avoid adding
 * strings like "golden", "image" and instead describe what is the golder referring to.
 *
 * @param rule The screenshot test rule that provides the comparison and reporting.
 * @param goldenIdentifier Name of the golden. Allowed characters: 'A-Za-z0-9_-'
 * @param matcher The algorithm to be used to perform the matching. By default [MSSIMMatcher]
 * is used.
 *
 * @see MSSIMMatcher
 * @see PixelPerfectMatcher
 */
fun Bitmap.assertAgainstGolden(
    rule: ScreenshotTestRule,
    goldenIdentifier: String,
    matcher: BitmapMatcher = MSSIMMatcher()
) {
    rule.assertBitmapAgainstGolden(this, goldenIdentifier, matcher = matcher)
}
