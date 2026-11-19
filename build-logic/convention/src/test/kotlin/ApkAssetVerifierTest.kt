import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApkAssetVerifierTest {
    private fun verify(declared: Map<String, String>, actual: Map<String, String>): Int {
        val apk = Files.createTempFile("asset-verifier", ".apk").toFile()
        try {
            ZipOutputStream(apk.outputStream()).use { zip ->
                fun entry(name: String, value: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(value.toByteArray())
                    zip.closeEntry()
                }
                val hashes = declared.mapValues { (_, value) -> MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) } }
                entry("assets/checksums.json", "{\"files\":" + Json.encodeToString(hashes) + "}")
                actual.forEach { (path, value) -> entry("assets/$path", value) }
            }
            return ApkAssetVerifier.verify(apk)
        } finally {
            apk.delete()
        }
    }

    @Test fun matchingApkPasses() {
        assertEquals(1, verify(mapOf("shared/a" to "ok"), mapOf("shared/a" to "ok")))
    }

    @Test fun missingOpenccFileFails() {
        assertFailsWith<IllegalStateException> { verify(mapOf("shared/opencc/JPVariants.txt" to "old"), emptyMap()) }
    }

    @Test fun changedBytesFail() {
        assertFailsWith<IllegalStateException> { verify(mapOf("shared/a" to "old"), mapOf("shared/a" to "new")) }
    }

    @Test fun undeclaredAssetFails() {
        assertFailsWith<IllegalStateException> { verify(emptyMap(), mapOf("shared/a" to "new")) }
    }

    @Test fun unresolvedLinkIsNotContent() {
        assertFailsWith<IllegalStateException> {
            val stub = mapOf("shared/default.yaml" to "../../../../data/rime/default.yaml")
            verify(stub, stub)
        }
    }
}
