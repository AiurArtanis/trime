// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.task
import org.jetbrains.kotlin.com.google.common.hash.Hashing
import org.jetbrains.kotlin.com.google.common.io.ByteSource
import java.io.File
import java.nio.charset.Charset
import kotlin.collections.set

/**
 * Add task generateDataChecksums
 */
class DataChecksumsPlugin : Plugin<Project> {
    companion object {
        const val TASK = "generateDataChecksums"
        const val CLEAN_TASK = "cleanDatacheksums"
        const val FILE_NAME = "checksums.json"
    }

    override fun apply(target: Project) {
        val resolvedAssets = target.layout.buildDirectory.dir("generated/portableAssets")
        val prepare = target.tasks.register<PortableAssetsTask>("preparePortableAssets") {
            inputDir.set(target.assetsDir)
            rimeDir.set(target.layout.projectDirectory.dir("data/rime"))
            outputDir.set(resolvedAssets)
            dependsOn(OpenCCDataPlugin.INSTALL_TASK)
        }
        target.extensions.configure<ApplicationExtension> {
            sourceSets.getByName("main").assets.setSrcDirs(listOf(resolvedAssets))
        }
        target.extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                val variantName = variant.name.replaceFirstChar { it.uppercase() }
                val verify = target.tasks.register<VerifyApkAssetsTask>("verify${variantName}ApkAssets") {
                    apkDir.set(variant.artifacts.get(SingleArtifact.APK))
                }
                target.tasks.matching { it.name == "assemble$variantName" }.configureEach {
                    dependsOn(verify)
                }
            }
        }
        target.tasks.register<DataChecksumsTask>(TASK) {
            dependsOn(prepare)
            inputDir.set(resolvedAssets)
            outputFile.set(resolvedAssets.map { it.file(FILE_NAME) })
        }
        target.tasks.register<Delete>(CLEAN_TASK) {
            delete(target.assetsDir.resolve(FILE_NAME))
        }.also {
            target.tasks.findByName("clean")?.dependsOn(it)
        }
    }

    abstract class PortableAssetsTask : DefaultTask() {
        @get:InputDirectory
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val inputDir: DirectoryProperty

        @get:InputDirectory
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val rimeDir: DirectoryProperty

        @get:OutputDirectory
        abstract val outputDir: DirectoryProperty

        @TaskAction
        fun prepare() {
            val source = inputDir.get().asFile
            val output = outputDir.get().asFile
            val rime = rimeDir.get().asFile.canonicalFile.toPath()
            output.deleteRecursively()
            source.walkTopDown().filter { it.isFile && it.name != FILE_NAME }.forEach { file ->
                // Git without symlink privileges checks out a relative path as plain text.
                val link = if (file.length() < 512) file.readText().trim() else ""
                val actual = if (link.startsWith("../../../../data/rime/")) {
                    file.parentFile.resolve(link).canonicalFile.also {
                        check(it.toPath().startsWith(rime) && it.isFile) { "Missing Rime asset: $it" }
                    }
                } else {
                    file
                }
                val destination = output.resolve(file.relativeTo(source))
                destination.parentFile.mkdirs()
                actual.copyTo(destination, overwrite = true)
            }
        }
    }

    abstract class DataChecksumsTask : DefaultTask() {
        @Serializable
        data class DataChecksums(
            val sha256: String,
            val files: Map<String, String>,
        )

        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:InputDirectory
        abstract val inputDir: DirectoryProperty

        @get:OutputFile
        abstract val outputFile: RegularFileProperty

        private val file by lazy { outputFile.get().asFile }

        private fun serialize(files: Map<String, String>) {
            val checksums =
                DataChecksums(
                    Hashing
                        .sha256()
                        .hashString(
                            files.entries.joinToString { it.key + it.value },
                            Charset.defaultCharset(),
                        ).toString(),
                    files,
                )
            file.writeText(json.encodeToString(checksums))
        }

        companion object {
            fun sha256(file: File): String = ByteSource.wrap(file.readBytes()).hash(Hashing.sha256()).toString()
        }

        @TaskAction
        fun execute() {
            // Never seed a new manifest from an old output. A non-incremental run
            // has no REMOVED events for files from the previous OpenCC revision.
            val root = inputDir.get().asFile
            val map = sortedMapOf<String, String>()
            root.walkTopDown().filter { it.isFile && it != file }.forEach { asset ->
                val relative = asset.relativeTo(root)
                map[relative.invariantSeparatorsPath] = sha256(asset)
                var parent = relative.parentFile
                while (parent != null) {
                    map[parent.invariantSeparatorsPath] = ""
                    parent = parent.parentFile
                }
            }
            serialize(map)
        }
    }
}
