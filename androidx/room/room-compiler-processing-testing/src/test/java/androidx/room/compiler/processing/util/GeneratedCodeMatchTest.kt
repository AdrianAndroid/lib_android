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

package androidx.room.compiler.processing.util

import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec as KTypeSpec
import org.junit.Test
import org.junit.AssumptionViolatedException
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class GeneratedCodeMatchTest internal constructor(
    private val runTest: TestRunner
) : MultiBackendTest() {
    @Test
    fun successfulGeneratedCodeMatch() {
        val file = JavaFile.builder(
            "foo.bar",
            TypeSpec.classBuilder("Baz").build()
        ).build()
        runTest { invocation ->
            if (invocation.processingEnv.findTypeElement("foo.bar.Baz") == null) {
                invocation.processingEnv.filer.write(
                    file
                )
            }
            invocation.assertCompilationResult {
                generatedSource(
                    Source.java(
                        "foo.bar.Baz",
                        file.toString()
                    )
                )
            }
        }
    }

    @Test
    fun missingGeneratedCode() {
        val result = runCatching {
            runTest { invocation ->
                invocation.assertCompilationResult {
                    generatedSource(
                        Source.java(
                            "foo.bar.Baz",
                            ""
                        )
                    )
                }
            }
        }
        assertThat(result.exceptionOrNull())
            .hasMessageThat()
            .contains("Didn't generate SourceFile[foo/bar/Baz.java]")
    }

    @Test
    fun missingGeneratedCode_contentMismatch() {
        val generated = JavaFile.builder(
            "foo.bar",
            TypeSpec.classBuilder("Baz")
                .addField(
                    TypeName.BOOLEAN, "bar"
                )
                .build()
        ).build()
        val expected = JavaFile.builder(
            "foo.bar",
            TypeSpec.classBuilder("Baz").addField(
                TypeName.BOOLEAN, "foo"
            ).build()
        ).build()
        val result = runCatching {
            runTest { invocation: XTestInvocation ->
                if (invocation.processingEnv.findTypeElement("foo.bar.Baz") == null) {
                    invocation.processingEnv.filer.write(generated)
                }
                invocation.assertCompilationResult {
                    generatedSource(
                        Source.java("foo.bar.Baz", expected.toString())
                    )
                }
            }
        }

        val mismatch = SourceFileMismatch(
            expected = Line(
                pos = 4,
                content = "boolean foo;"
            ),
            actual = Line(
                pos = 4,
                content = "boolean bar;"
            )
        )
        assertThat(result.exceptionOrNull()).hasMessageThat().contains(mismatch.toString())
    }

    @Test
    fun successfulGeneratedKotlinCodeMatch() {
        // java environment will not generate kotlin files
        if (runTest.toString() == "java") {
            throw AssumptionViolatedException("javaAP won't generate kotlin code.")
        }

        val file = FileSpec.builder("foo.bar", "Baz")
            .addType(KTypeSpec.classBuilder("Baz").build())
            .build()
        runTest { invocation ->
            if (invocation.processingEnv.findTypeElement("foo.bar.Baz") == null) {
                invocation.processingEnv.filer.write(file)
            }
            invocation.assertCompilationResult {
                generatedSource(
                    Source.kotlin("foo/bar/Baz.kt", file.toString())
                )
            }
        }
    }

    @Test
    fun missingGeneratedKotlinCode_mismatch() {
        // java environment will not generate kotlin files
        if (runTest.toString() == "java") {
            throw AssumptionViolatedException("javaAP won't generate kotlin code.")
        }

        val generated = FileSpec.builder("foo.bar", "Baz")
            .addType(
                KTypeSpec.classBuilder("Baz")
                    .addProperty("bar", BOOLEAN)
                    .build()
            )
            .build()
        val expected = FileSpec.builder("foo.bar", "Baz")
            .addType(
                KTypeSpec.classBuilder("Baz")
                    .addProperty("foo", BOOLEAN)
                    .build()
            )
            .build()

        val result = runCatching {
            runTest { invocation ->
                if (invocation.processingEnv.findTypeElement("foo.bar.Baz") == null) {
                    invocation.processingEnv.filer.write(generated)
                }
                invocation.assertCompilationResult {
                    generatedSource(
                        Source.kotlin("foo/bar/Baz.kt", expected.toString())
                    )
                }
            }
        }

        val mismatch = SourceFileMismatch(
            expected = Line(
                pos = 6,
                content = "public val foo: Boolean"
            ),
            actual = Line(
                pos = 6,
                content = "public val bar: Boolean"
            )
        )
        assertThat(result.exceptionOrNull()).hasMessageThat().contains(mismatch.toString())
    }

    @Test
    fun missingGeneratedKotlinCode_javaAP() {
        if (runTest.toString() != "java") {
            throw AssumptionViolatedException("Testing scenario for javaAP only.")
        }

        val file = FileSpec.builder("foo.bar", "Baz")
            .addType(KTypeSpec.classBuilder("Baz").build())
            .build()

        val result = runCatching {
            runTest { invocation ->
                if (invocation.processingEnv.findTypeElement("foo.bar.Baz") == null) {
                    invocation.processingEnv.filer.write(file)
                }
                invocation.assertCompilationResult {
                    generatedSource(
                        Source.kotlin("foo/bar/Baz.kt", file.toString())
                    )
                }
            }
        }

        assertThat(result.exceptionOrNull())
            .hasCauseThat()
            .hasMessageThat()
            .contains(
                "Could not generate kotlin file foo/bar/Baz.kt. The annotation processing " +
                    "environment is not set to generate Kotlin files."
            )
    }
}
