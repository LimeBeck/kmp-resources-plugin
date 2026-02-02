import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import dev.limebeck.kmpResources.codeGenerator.ResourceNode
import dev.limebeck.kmpResources.codeGenerator.generateNode
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for GenerateNodeKt, the top-level holder for resource generation helpers.
 * The generateNode function builds KotlinPoet object trees for resource directories and files.
 */
class GenerateNodeKtTest {

    @Test
    fun `generateNode creates expect declarations for directories and files`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("logo.txt").toFile()
        file.writeText("logo")

        val node = ResourceNode(
            name = "assets",
            isCommon = true,
            children = mutableMapOf("images" to ResourceNode("images", true)),
            files = mutableMapOf("logo.txt" to ResourceNode.FileInfo(file, "assets/logo.txt", true))
        )

        val builder = TypeSpec.objectBuilder("Res")
        generateNode(builder, node, isExpect = true, platformType = KotlinPlatformType.jvm)
        val output = FileSpec.builder("dev.limebeck.res", "Res")
            .addType(builder.build())
            .build()
            .toString()

        assertTrue(output.contains("expect object images : ResourceDirectory"))
        assertTrue(output.contains("expect val logo: ResourceFile"))
        assertTrue(output.contains("override val name: String"))
        assertTrue(output.contains("override val items: List<ResourceItem>"))
        assertFalse(output.contains("by lazy"))
    }

    @Test
    fun `generateNode builds actual items list and classpath reader on jvm`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("logo.txt").toFile()
        file.writeText("logo")

        val childNode = ResourceNode(name = "images.icons", isCommon = true)
        val node = ResourceNode(
            name = "root",
            isCommon = true,
            children = mutableMapOf("images.icons" to childNode),
            files = mutableMapOf("logo.txt" to ResourceNode.FileInfo(file, "assets/logo.txt", true))
        )

        val builder = TypeSpec.objectBuilder("Res")
        generateNode(builder, node, isExpect = false, platformType = KotlinPlatformType.jvm)
        val output = FileSpec.builder("dev.limebeck.res", "Res")
            .addType(builder.build())
            .build()
            .toString()

        assertTrue(output.contains("actual override val name: String = \"root\""))
        assertTrue(output.contains("actual override val items: List<ResourceItem> by lazy { listOf(images_icons, logo) }"))
        assertTrue(output.contains("Thread.currentThread().contextClassLoader.getResourceAsStream(\"assets/logo.txt\").readAllBytes()"))
        assertFalse(output.contains("Base64.Default.decode"))
    }

    @Test
    fun `generateNode encodes file contents for js and uses Res fallback name`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("logo.dark.txt").toFile()
        file.writeText("hello")

        val node = ResourceNode(
            name = "",
            isCommon = false,
            children = mutableMapOf(),
            files = mutableMapOf("logo.dark.txt" to ResourceNode.FileInfo(file, "assets/logo.dark.txt", false))
        )

        val builder = TypeSpec.objectBuilder("Res")
        generateNode(builder, node, isExpect = false, platformType = KotlinPlatformType.js)
        val output = FileSpec.builder("dev.limebeck.res", "Res")
            .addType(builder.build())
            .build()
            .toString()

        assertTrue(output.contains("override val name: String = \"Res\""))
        assertTrue(output.contains("val logo_dark: ResourceFile = object : ResourceFile"))
        assertTrue(output.contains("Base64.Default.decode(\"aGVsbG8=\")"))
        assertTrue(output.contains("@OptIn(ExperimentalEncodingApi::class)"))
    }

    @Test
    fun `generateNode omits actual modifiers for non-common nodes`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("banner.txt").toFile()
        file.writeText("banner")

        val node = ResourceNode(
            name = "assets",
            isCommon = false,
            children = mutableMapOf(),
            files = mutableMapOf("banner.txt" to ResourceNode.FileInfo(file, "assets/banner.txt", false))
        )

        val builder = TypeSpec.objectBuilder("Res")
        generateNode(builder, node, isExpect = false, platformType = KotlinPlatformType.jvm)
        val output = FileSpec.builder("dev.limebeck.res", "Res")
            .addType(builder.build())
            .build()
            .toString()

        assertTrue(output.contains("override val name: String = \"assets\""))
        assertTrue(output.contains("override val items: List<ResourceItem> by lazy { listOf(banner) }"))
        assertFalse(output.contains("actual override val name"))
        assertFalse(output.contains("actual override val items"))
        assertFalse(output.contains("actual val banner"))
    }

    @Test
    fun `generateNode encodes file contents for wasm targets`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("icon.txt").toFile()
        file.writeText("icon")

        val node = ResourceNode(
            name = "assets",
            isCommon = true,
            children = mutableMapOf(),
            files = mutableMapOf("icon.txt" to ResourceNode.FileInfo(file, "assets/icon.txt", true))
        )

        val builder = TypeSpec.objectBuilder("Res")
        generateNode(builder, node, isExpect = false, platformType = KotlinPlatformType.wasm)
        val output = FileSpec.builder("dev.limebeck.res", "Res")
            .addType(builder.build())
            .build()
            .toString()

        assertTrue(output.contains("Base64.Default.decode(\"aWNvbg==\")"))
        assertTrue(output.contains("@OptIn(ExperimentalEncodingApi::class)"))
        assertFalse(output.contains("getResourceAsStream"))
    }
}
