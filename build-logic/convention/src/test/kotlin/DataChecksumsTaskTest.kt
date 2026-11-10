import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataChecksumsTaskTest {
    @Test
    fun removesStaleFilesWithoutIncrementalRemovalEvents() {
        val dir = Files.createTempDirectory("checksums-regression").toFile()
        try {
            val project = ProjectBuilder.builder().withProjectDir(dir).build()
            val root = dir.resolve("assets").apply { mkdirs() }
            val kept = root.resolve("shared/opencc/current.txt").apply {
                parentFile.mkdirs()
                writeText("current")
            }
            val output = root.resolve("checksums.json")
            output.writeText("""{"sha256":"old","files":{"shared/opencc/JPVariants.txt":"old","obsolete":""}}""")
            val task = project.tasks.create("checksumsTest", DataChecksumsPlugin.DataChecksumsTask::class.java)
            task.inputDir.set(root)
            task.outputFile.set(output)
            task.execute()
            val files = Json.parseToJsonElement(output.readText()).jsonObject.getValue("files").jsonObject
            assertEquals(setOf("shared", "shared/opencc", "shared/opencc/current.txt"), files.keys)
            assertFalse(files.containsKey("checksums.json"))
            kept.delete()
            task.execute()
            assertTrue(Json.parseToJsonElement(output.readText()).jsonObject.getValue("files").jsonObject.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun resolvesWindowsGitSymlinkStubsBeforeHashing() {
        val dir = Files.createTempDirectory("portable-assets").toFile()
        try {
            val project = ProjectBuilder.builder().withProjectDir(dir).build()
            val root = dir.resolve("app/src/main/assets").apply { mkdirs() }
            val rime = dir.resolve("app/data/rime").apply { mkdirs() }
            rime.resolve("default.yaml").writeText("config_version: 1\n")
            root.resolve("shared/default.yaml").apply {
                parentFile.mkdirs()
                writeText("../../../../data/rime/default.yaml")
            }
            val output = dir.resolve("generated")
            output.mkdirs()
            output.resolve("stale.txt").writeText("stale")
            val task = project.tasks.create("portableTest", DataChecksumsPlugin.PortableAssetsTask::class.java)
            task.inputDir.set(root)
            task.rimeDir.set(rime)
            task.outputDir.set(output)
            task.prepare()
            assertEquals("config_version: 1\n", output.resolve("shared/default.yaml").readText())
            assertFalse(output.resolve("stale.txt").exists())
        } finally {
            dir.deleteRecursively()
        }
    }
}
