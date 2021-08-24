/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.inspection.gradle

import com.android.build.gradle.LibraryExtension
import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufConvention
import com.google.protobuf.gradle.ProtobufPlugin
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protoc
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.StopExecutionException
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getPlugin
import java.io.File

/**
 * A plugin which, when present, ensures that intermediate inspector
 * resources are generated at build time
 */
@Suppress("SyntheticAccessor")
class InspectionPlugin : Plugin<Project> {
    // project.register* are marked with @ExperimentalStdlibApi, because they use experimental
    // string.capitalize call.
    @ExperimentalStdlibApi
    override fun apply(project: Project) {
        var foundLibraryPlugin = false
        var foundReleaseVariant = false
        val extension = project.extensions.create<InspectionExtension>(EXTENSION_NAME, project)

        val publishInspector = project.configurations.create("publishInspector") {
            it.isCanBeConsumed = true
            it.isCanBeResolved = false
            it.setupInspectorAttribute()
        }

        project.pluginManager.withPlugin("com.android.library") {
            foundLibraryPlugin = true
            val libExtension = project.extensions.getByType(LibraryExtension::class.java)
            includeMetaInfServices(libExtension)
            libExtension.libraryVariants.all { variant ->
                if (variant.name == "release") {
                    foundReleaseVariant = true
                    val unzip = project.registerUnzipTask(variant)
                    val shadowJar = project.registerShadowDependenciesTask(variant, unzip)
                    val bundleTask = project.registerBundleInspectorTask(
                        variant, libExtension, extension.name, shadowJar
                    )

                    publishInspector.outgoing.variants {
                        val configVariant = it.create("inspectorJar")
                        configVariant.artifact(bundleTask)
                    }
                }
            }
            libExtension.sourceSets.findByName("main")!!.resources.srcDirs(
                File(project.rootDir, "src/main/proto")
            )
        }

        project.apply(plugin = "com.google.protobuf")
        project.plugins.all {
            if (it is ProtobufPlugin) {
                // https://github.com/google/protobuf-gradle-plugin/issues/505
                @Suppress("DEPRECATION")
                val protobufConvention = project.convention.getPlugin<ProtobufConvention>()
                protobufConvention.protobuf.apply {
                    protoc {
                        this.artifact = "com.google.protobuf:protoc:3.10.0"
                    }
                    generateProtoTasks {
                        all().forEach { task: GenerateProtoTask ->
                            task.builtins.create("java") { options ->
                                options.option("lite")
                            }
                        }
                    }
                }
            }
        }

        project.dependencies {
            add("implementation", "com.google.protobuf:protobuf-javalite:3.10.0")
        }

        project.afterEvaluate {
            if (!foundLibraryPlugin) {
                throw StopExecutionException(
                    """A required plugin, com.android.library, was not found.
                        The androidx.inspection plugin currently only supports android library
                        modules, so ensure that com.android.library is applied in the project
                        build.gradle file."""
                        .trimIndent()
                )
            }
            if (!foundReleaseVariant) {
                throw StopExecutionException(
                    "The androidx.inspection plugin requires " +
                        "release build variant."
                )
            }
        }
    }
}

private fun includeMetaInfServices(library: LibraryExtension) {
    library.sourceSets.getByName("main").resources.include("META-INF/services/*")
    library.sourceSets.getByName("main").resources.include("**/*.proto")
}

/**
 * Use this function in [libraryProject] to include inspector that will be compiled into
 * inspector.jar and packaged in the library's aar.
 *
 * @param libraryProject project that is inspected and which aar will host inspector.jar . E.g
 * work-runtime
 * @param inspectorProject project of inspector, that will be compiled into inspector.jar. E.g
 * work-inspection
 */
@ExperimentalStdlibApi
fun packageInspector(libraryProject: Project, inspectorProject: Project) {
    val consumeInspector = libraryProject.createConsumeInspectionConfiguration()

    libraryProject.dependencies {
        add(consumeInspector.name, inspectorProject)
    }

    generateProguardDetectionFile(libraryProject)
    val libExtension = libraryProject.extensions.getByType(LibraryExtension::class.java)
    libExtension.libraryVariants.all { variant ->
        variant.packageLibraryProvider.configure { zip ->
            zip.from(consumeInspector)
            zip.rename {
                if (it == consumeInspector.asFileTree.singleFile.name) {
                    "inspector.jar"
                } else it
            }
        }
    }
}

fun Project.createConsumeInspectionConfiguration(): Configuration =
    configurations.create("consumeInspector") {
        it.setupInspectorAttribute()
    }

private fun Configuration.setupInspectorAttribute() {
    attributes {
        it.attribute(Attribute.of("inspector", String::class.java), "inspectorJar")
    }
}

@ExperimentalStdlibApi
private fun generateProguardDetectionFile(libraryProject: Project) {
    val libExtension = libraryProject.extensions.getByType(LibraryExtension::class.java)
    libExtension.libraryVariants.all { variant ->
        libraryProject.registerGenerateProguardDetectionFileTask(variant)
    }
}

const val EXTENSION_NAME = "inspection"

open class InspectionExtension(@Suppress("UNUSED_PARAMETER") project: Project) {
    /**
     * Name of built inspector artifact, if not provided it is equal to project's name.
     */
    var name: String? = null
}