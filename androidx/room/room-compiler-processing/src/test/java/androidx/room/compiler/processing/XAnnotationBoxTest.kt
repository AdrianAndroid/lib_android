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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults
import androidx.room.compiler.processing.testcode.JavaAnnotationWithPrimitiveArray
import androidx.room.compiler.processing.testcode.JavaAnnotationWithTypeReferences
import androidx.room.compiler.processing.testcode.JavaEnum
import androidx.room.compiler.processing.testcode.MainAnnotation
import androidx.room.compiler.processing.testcode.OtherAnnotation
import androidx.room.compiler.processing.testcode.RepeatableJavaAnnotation
import androidx.room.compiler.processing.testcode.TestSuppressWarnings
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.getParameter
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.runProcessorTestWithoutKsp
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.squareup.javapoet.ClassName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.LinkedHashMap

@RunWith(Parameterized::class)
class XAnnotationBoxTest(
    private val preCompiled: Boolean
) {
    private fun runTest(
        sources: List<Source>,
        handler: (XTestInvocation) -> Unit
    ) {
        if (preCompiled) {
            val compiled = compileFiles(sources)
            val hasKotlinSources = sources.any {
                it is Source.KotlinSource
            }
            val kotlinSources = if (hasKotlinSources) {
                listOf(
                    Source.kotlin("placeholder.kt", "class PlaceholderKotlin")
                )
            } else {
                emptyList()
            }
            val newSources = kotlinSources + Source.java(
                "PlaceholderJava",
                "public class " +
                    "PlaceholderJava {}"
            )
            runProcessorTest(
                sources = newSources,
                handler = handler,
                classpath = compiled
            )
        } else {
            runProcessorTest(
                sources = sources,
                handler = handler
            )
        }
    }

    @Test
    fun readSimpleAnnotationValue() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings;
            @TestSuppressWarnings({"warning1", "warning 2"})
            public class Baz {
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            val annotationBox = element.getAnnotation(TestSuppressWarnings::class)
            assertThat(annotationBox).isNotNull()
            assertThat(
                annotationBox!!.value.value
            ).isEqualTo(
                arrayOf("warning1", "warning 2")
            )
        }
    }

    @Test
    fun typeReference() {
        val mySource = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.MainAnnotation;
            import androidx.room.compiler.processing.testcode.OtherAnnotation;
            @MainAnnotation(
                typeList = {String.class, Integer.class},
                singleType = Long.class,
                intMethod = 3,
                otherAnnotationArray = {
                    @OtherAnnotation(
                        value = "other list 1"
                    ),
                    @OtherAnnotation("other list 2"),
                },
                singleOtherAnnotation = @OtherAnnotation("other single")
            )
            public class Baz {
            }
            """.trimIndent()
        )
        // re-enable after fixing b/175144186
        runProcessorTestWithoutKsp(
            listOf(mySource)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getAnnotation(MainAnnotation::class)!!.let { annotation ->
                assertThat(
                    annotation.getAsTypeList("typeList")
                ).containsExactly(
                    it.processingEnv.requireType(java.lang.String::class),
                    it.processingEnv.requireType(java.lang.Integer::class)
                )
                assertThat(
                    annotation.getAsType("singleType")
                ).isEqualTo(
                    it.processingEnv.requireType(java.lang.Long::class)
                )

                assertThat(annotation.value.intMethod).isEqualTo(3)
                annotation.getAsAnnotationBox<OtherAnnotation>("singleOtherAnnotation")
                    .let { other ->
                        assertThat(other.value.value).isEqualTo("other single")
                    }
                annotation.getAsAnnotationBoxArray<OtherAnnotation>("otherAnnotationArray")
                    .let { boxArray ->
                        assertThat(boxArray).hasLength(2)
                        assertThat(boxArray[0].value.value).isEqualTo("other list 1")
                        assertThat(boxArray[1].value.value).isEqualTo("other list 2")
                    }
            }
        }
    }

    @Test
    fun readSimpleAnnotationValue_kotlin() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            @TestSuppressWarnings("warning1", "warning 2")
            class Subject {
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("Subject")
            val annotationBox = element.getAnnotation(TestSuppressWarnings::class)
            assertThat(annotationBox).isNotNull()
            assertThat(
                annotationBox!!.value.value
            ).isEqualTo(
                arrayOf("warning1", "warning 2")
            )
        }
    }

    @Test
    fun typeReference_kotlin() {
        val mySource = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.compiler.processing.testcode.MainAnnotation
            import androidx.room.compiler.processing.testcode.OtherAnnotation

            @MainAnnotation(
                typeList = [String::class, Int::class],
                singleType = Long::class,
                intMethod = 3,
                otherAnnotationArray = [
                    OtherAnnotation(
                        value = "other list 1"
                    ),
                    OtherAnnotation(
                        value = "other list 2"
                    )
                ],
                singleOtherAnnotation = OtherAnnotation("other single")
            )
            public class Subject {
            }
            """.trimIndent()
        )
        runTest(
            listOf(mySource)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("Subject")
            element.getAnnotation(MainAnnotation::class)!!.let { annotation ->
                assertThat(
                    annotation.getAsTypeList("typeList").map {
                        it.typeName
                    }
                ).containsExactly(
                    String::class.typeName(),
                    Int::class.typeName()
                )
                assertThat(
                    annotation.getAsType("singleType")
                ).isEqualTo(
                    invocation.processingEnv.requireType(Long::class.typeName())
                )

                assertThat(annotation.value.intMethod).isEqualTo(3)
                annotation.getAsAnnotationBox<OtherAnnotation>("singleOtherAnnotation")
                    .let { other ->
                        assertThat(other.value.value).isEqualTo("other single")
                    }
                if (invocation.isKsp) {
                    // TODO fix when KSP support them
                    //  https://github.com/google/ksp/issues/356
                } else {
                    annotation.getAsAnnotationBoxArray<OtherAnnotation>("otherAnnotationArray")
                        .let { boxArray ->
                            assertThat(boxArray).hasLength(2)
                            assertThat(boxArray[0].value.value).isEqualTo("other list 1")
                            assertThat(boxArray[1].value.value).isEqualTo("other list 2")
                        }
                }
            }
        }
    }

    @Test
    fun typeReferenceArray_singleItemInJava() {
        val src = Source.java(
            "Subject",
            """
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithTypeReferences;
            @JavaAnnotationWithTypeReferences(String.class)
            class Subject {
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(src)
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val annotationValue = subject.getAnnotation(
                JavaAnnotationWithTypeReferences::class
            )?.getAsTypeList("value")
            assertThat(annotationValue?.map { it.typeName }).containsExactly(
                ClassName.get(String::class.java)
            )
        }
    }

    @Test
    fun propertyAnnotations() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.compiler.processing.testcode.OtherAnnotation
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            class Subject {
                @TestSuppressWarnings("onProp1")
                var prop1:Int = TODO()

                @get:TestSuppressWarnings("onGetter2")
                @set:TestSuppressWarnings("onSetter2")
                @field:TestSuppressWarnings("onField2")
                @setparam:TestSuppressWarnings("onSetterParam2")
                var prop2:Int = TODO()

                @get:TestSuppressWarnings("onGetter3")
                @set:TestSuppressWarnings("onSetter3")
                @setparam:TestSuppressWarnings("onSetterParam3")
                var prop3:Int
                    @OtherAnnotation("_onGetter3")
                    get() = 3

                    @OtherAnnotation("_onSetter3")
                    set(@OtherAnnotation("_onSetterParam3") value) = Unit
            }
            """.trimIndent()
        )
        runTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")

            subject.getField("prop1").assertHasSuppressWithValue("onProp1")
            subject.getMethod("getProp1").assertDoesNotHaveAnnotation()
            subject.getMethod("setProp1").assertDoesNotHaveAnnotation()
            subject.getMethod("setProp1").parameters.first().assertDoesNotHaveAnnotation()

            subject.getField("prop2").assertHasSuppressWithValue("onField2")
            subject.getMethod("getProp2").assertHasSuppressWithValue("onGetter2")
            subject.getMethod("setProp2").assertHasSuppressWithValue("onSetter2")
            subject.getMethod("setProp2").parameters.first().assertHasSuppressWithValue(
                "onSetterParam2"
            )

            subject.getMethod("getProp3").assertHasSuppressWithValue("onGetter3")
            subject.getMethod("setProp3").assertHasSuppressWithValue("onSetter3")
            subject.getMethod("setProp3").parameters.first().assertHasSuppressWithValue(
                "onSetterParam3"
            )

            assertThat(
                subject.getMethod("getProp3").getOtherAnnotationValue()
            ).isEqualTo("_onGetter3")
            assertThat(
                subject.getMethod("setProp3").getOtherAnnotationValue()
            ).isEqualTo("_onSetter3")
            val otherAnnotationValue =
                subject.getMethod("setProp3").parameters.first().getOtherAnnotationValue()
            assertThat(
                otherAnnotationValue
            ).isEqualTo("_onSetterParam3")
        }
    }

    @Test
    fun methodAnnotations() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.compiler.processing.testcode.OtherAnnotation
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            class Subject {
                fun noAnnotations(x:Int): Unit = TODO()
                @TestSuppressWarnings("onMethod")
                fun methodAnnotation(
                    @TestSuppressWarnings("onParam") annotated:Int,
                    notAnnotated:Int
                ): Unit = TODO()
            }
            """.trimIndent()
        )
        runTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.getMethod("noAnnotations").let { method ->
                method.assertDoesNotHaveAnnotation()
                method.getParameter("x").assertDoesNotHaveAnnotation()
            }
            subject.getMethod("methodAnnotation").let { method ->
                method.assertHasSuppressWithValue("onMethod")
                method.getParameter("annotated").assertHasSuppressWithValue("onParam")
                method.getParameter("notAnnotated").assertDoesNotHaveAnnotation()
            }
        }
    }

    @Test
    fun constructorParameterAnnotations() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.compiler.processing.testcode.TestSuppressWarnings
            @TestSuppressWarnings("onClass")
            data class Subject(
                @field:TestSuppressWarnings("onField")
                @param:TestSuppressWarnings("onConstructorParam")
                @get:TestSuppressWarnings("onGetter")
                @set:TestSuppressWarnings("onSetter")
                var x:Int
            )
            """.trimIndent()
        )
        runTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            subject.assertHasSuppressWithValue("onClass")
            assertThat(subject.getConstructors()).hasSize(1)
            val constructor = subject.getConstructors().single()
            constructor.getParameter("x").assertHasSuppressWithValue("onConstructorParam")
            subject.getMethod("getX").assertHasSuppressWithValue("onGetter")
            subject.getMethod("setX").assertHasSuppressWithValue("onSetter")
            subject.getField("x").assertHasSuppressWithValue("onField")
        }
    }

    @Test
    fun defaultValues() {
        val kotlinSrc = Source.kotlin(
            "KotlinClass.kt",
            """
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults
            @JavaAnnotationWithDefaults
            class KotlinClass
            """.trimIndent()
        )
        val javaSrc = Source.java(
            "JavaClass.java",
            """
            import androidx.room.compiler.processing.testcode.JavaAnnotationWithDefaults;
            @JavaAnnotationWithDefaults
            class JavaClass {}
            """.trimIndent()
        )
        runTest(sources = listOf(kotlinSrc, javaSrc)) { invocation ->
            listOf("KotlinClass", "JavaClass")
                .map {
                    invocation.processingEnv.requireTypeElement(it)
                }.forEach { typeElement ->
                    val annotation =
                        typeElement.getAnnotation(JavaAnnotationWithDefaults::class)
                    checkNotNull(annotation)
                    assertThat(annotation.value.intVal).isEqualTo(3)
                    assertThat(annotation.value.intArrayVal).isEqualTo(intArrayOf(1, 3, 5))
                    assertThat(annotation.value.stringArrayVal).isEqualTo(arrayOf("x", "y"))
                    assertThat(annotation.value.stringVal).isEqualTo("foo")
                    assertThat(
                        annotation.getAsType("typeVal")?.rawType?.typeName
                    ).isEqualTo(
                        ClassName.get(HashMap::class.java)
                    )
                    assertThat(
                        annotation.getAsTypeList("typeArrayVal").map {
                            it.rawType.typeName
                        }
                    ).isEqualTo(
                        listOf(ClassName.get(LinkedHashMap::class.java))
                    )

                    assertThat(
                        annotation.value.enumVal
                    ).isEqualTo(
                        JavaEnum.DEFAULT
                    )

                    assertThat(
                        annotation.value.enumArrayVal
                    ).isEqualTo(
                        arrayOf(JavaEnum.VAL1, JavaEnum.VAL2)
                    )

                    assertThat(
                        annotation.getAsAnnotationBox<OtherAnnotation>("otherAnnotationVal")
                            .value.value
                    ).isEqualTo("def")

                    assertThat(
                        annotation
                            .getAsAnnotationBoxArray<OtherAnnotation>("otherAnnotationArrayVal")
                            .map {
                                it.value.value
                            }
                    ).containsExactly("v1")
                }
        }
    }

    @Test
    fun javaPrimitiveArray() {
        // TODO: expand this test for other primitive types: 179081610
        val javaSrc = Source.java(
            "JavaSubject.java",
            """
            import androidx.room.compiler.processing.testcode.*;
            class JavaSubject {
                @JavaAnnotationWithPrimitiveArray(intArray = {1, 2, 3})
                Object annotated1;
            }
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "KotlinSubject.kt",
            """
            import androidx.room.compiler.processing.testcode.*;
            class KotlinSubject {
                @JavaAnnotationWithPrimitiveArray(intArray = [1, 2, 3])
                val annotated1:Any = TODO()
            }
            """.trimIndent()
        )
        runTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            arrayOf("JavaSubject", "KotlinSubject").map {
                invocation.processingEnv.requireTypeElement(it)
            }.forEach { subject ->
                val annotation = subject.getField("annotated1").getAnnotation(
                    JavaAnnotationWithPrimitiveArray::class
                )
                assertThat(
                    annotation?.value?.intArray
                ).isEqualTo(
                    intArrayOf(1, 2, 3)
                )
            }
        }
    }

    @Test
    fun javaRepeatableAnnotation() {
        val javaSrc = Source.java(
            "JavaSubject",
            """
            import ${RepeatableJavaAnnotation::class.qualifiedName};
            @RepeatableJavaAnnotation("x")
            @RepeatableJavaAnnotation("y")
            @RepeatableJavaAnnotation("z")
            public class JavaSubject {}
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "KotlinSubject.kt",
            """
            import ${RepeatableJavaAnnotation::class.qualifiedName}
            // TODO update when https://youtrack.jetbrains.com/issue/KT-12794 is fixed.
            // right now, kotlin does not support repeatable annotations.
            @RepeatableJavaAnnotation.List(
                RepeatableJavaAnnotation("x"),
                RepeatableJavaAnnotation("y"),
                RepeatableJavaAnnotation("z")
            )
            public class KotlinSubject
            """.trimIndent()
        )
        runTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            listOf("JavaSubject", "KotlinSubject")
                .map(invocation.processingEnv::requireTypeElement)
                .forEach { subject ->
                    if (invocation.isKsp && preCompiled) {
                        // TODO remove once https://github.com/google/ksp/issues/356 is fixed
                        // KSP cannot read array of annotation values in compiled code
                    } else {
                        val annotations = subject.getAnnotations(
                            RepeatableJavaAnnotation::class
                        )
                        assertThat(
                            subject.hasAnnotation(
                                RepeatableJavaAnnotation::class
                            )
                        ).isTrue()
                        val values = annotations
                            .map {
                                it.value.value
                            }
                        assertWithMessage(subject.qualifiedName)
                            .that(values)
                            .containsExactly("x", "y", "z")
                    }
                }
        }
    }

    @Test
    fun javaRepeatableAnnotation_notRepeated() {
        val javaSrc = Source.java(
            "JavaSubject",
            """
            import ${RepeatableJavaAnnotation::class.qualifiedName};
            @RepeatableJavaAnnotation("x")
            public class JavaSubject {}
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "KotlinSubject.kt",
            """
            import ${RepeatableJavaAnnotation::class.qualifiedName}
            @RepeatableJavaAnnotation("x")
            public class KotlinSubject
            """.trimIndent()
        )
        runTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            listOf("JavaSubject", "KotlinSubject")
                .map(invocation.processingEnv::requireTypeElement)
                .forEach { subject ->
                    if (invocation.isKsp && preCompiled) {
                        // TODO remove once https://github.com/google/ksp/issues/356 is fixed
                        // KSP cannot read array of annotation values in compiled code
                    } else {
                        val annotations = subject.getAnnotations(
                            RepeatableJavaAnnotation::class
                        )
                        assertThat(
                            subject.hasAnnotation(
                                RepeatableJavaAnnotation::class
                            )
                        ).isTrue()
                        val values = annotations
                            .map {
                                it.value.value
                            }
                        assertWithMessage(subject.qualifiedName)
                            .that(values)
                            .containsExactly("x")
                    }
                }
        }
    }

    // helper function to read what we need
    private fun XAnnotated.getSuppressValues(): Array<String>? {
        return this.getAnnotation(TestSuppressWarnings::class)?.value?.value
    }

    private fun XAnnotated.assertHasSuppressWithValue(vararg expected: String) {
        assertWithMessage("has suppress annotation $this")
            .that(this.hasAnnotation(TestSuppressWarnings::class))
            .isTrue()
        assertWithMessage("$this")
            .that(this.hasAnnotationWithPackage(TestSuppressWarnings::class.java.packageName))
            .isTrue()
        assertWithMessage("$this")
            .that(getSuppressValues())
            .isEqualTo(expected)
    }

    private fun XAnnotated.assertDoesNotHaveAnnotation() {
        assertWithMessage("$this")
            .that(this.hasAnnotation(TestSuppressWarnings::class))
            .isFalse()
        assertWithMessage("$this")
            .that(this.hasAnnotationWithPackage(TestSuppressWarnings::class.java.packageName))
            .isFalse()
        assertWithMessage("$this")
            .that(this.getSuppressValues())
            .isNull()
    }

    private fun XAnnotated.getOtherAnnotationValue(): String? {
        return this.getAnnotation(OtherAnnotation::class)?.value?.value
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "preCompiled_{0}")
        fun params() = arrayOf(false, true)
    }
}
