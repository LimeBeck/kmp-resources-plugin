import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KmpResourcesPluginTest {

    @TempDir
    lateinit var testProjectDir: Path

    private lateinit var gradleRunner: GradleRunner

    @BeforeEach
    fun setup() {
        gradleRunner = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir.toFile())
            .withTestKitDir(testProjectDir.resolve("./testKit").createDirectories().toFile())
    }

    fun initGradleBuild(buildGradleContent: String) {
        testProjectDir
            .resolve("build.gradle.kts")
            .createFile()
            .writeText(buildGradleContent)
        testProjectDir
            .resolve("settings.gradle.kts")
            .createFile()
            .writeText("rootProject.name = \"kmp-resources-test\"")
    }

    @Test
    fun `Generate KMP resources`() {
        // Create resource files
        val commonResDir = testProjectDir.resolve("src/commonMain/resources/images").createDirectories()
        commonResDir.resolve("logo.png").createFile().writeText("common-logo")
        
        val linuxResDir = testProjectDir.resolve("src/linuxX64Main/resources/images").createDirectories()
        linuxResDir.resolve("logo.png").createFile().writeText("linux-logo")
        linuxResDir.resolve("linux_only.txt").createFile().writeText("linux-only")

        initGradleBuild(
            """
            plugins {
                kotlin("multiplatform") version "2.1.0"
                id("dev.limebeck.kmp-resources")
            }
            
            kotlin {
                linuxX64()
                jvm()
                
                sourceSets {
                    val commonMain by getting
                    val linuxX64Main by getting {
                        dependsOn(commonMain)
                    }
                }
            }
            
            kmpResources {
                packageName.set("dev.limebeck.res")
            }
        """.trimIndent()
        )

        val result = gradleRunner.withArguments("generateKmpResourcesMetadata", "generateKmpResourcesLinuxX64", "--stacktrace").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateKmpResourcesMetadata")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateKmpResourcesLinuxX64")!!.outcome)
        
        // Check commonMain generated code
        val commonGeneratedFile = testProjectDir.resolve("build/generated/kmpResources/metadata/dev/limebeck/res/Res.kt")
        assertTrue(commonGeneratedFile.exists())
        val commonContent = commonGeneratedFile.readText()
        assertTrue(commonContent.contains("expect object Res : ResourceDirectory"))
        assertTrue(commonContent.contains("expect object images : ResourceDirectory"))
        assertTrue(commonContent.contains("val logo: ResourceFile"))
        println(commonContent)

        val expectedCommon = """
            package dev.limebeck.res

            import dev.limebeck.kmpResources.ResourceDirectory
            import dev.limebeck.kmpResources.ResourceFile
            import dev.limebeck.kmpResources.ResourceItem
            import kotlin.String
            import kotlin.collections.List

            public expect object Res : ResourceDirectory {
              override val name: String

              override val items: List<ResourceItem>

              public expect object images : ResourceDirectory {
                override val name: String

                override val items: List<ResourceItem>

                public val logo: ResourceFile
              }
            }
            
        """.trimIndent()
        assertEquals(expectedCommon, commonContent, "Generated common code differs from expected")


        // Check linuxX64Main generated code
        val linuxGeneratedFile = testProjectDir.resolve("build/generated/kmpResources/linuxX64/dev/limebeck/res/Res.kt")
        assertTrue(linuxGeneratedFile.exists())
        val linuxContent = linuxGeneratedFile.readText()
        println(linuxContent)
        assertTrue(linuxContent.contains("actual object Res : ResourceDirectory"))
        assertTrue(linuxContent.contains("actual object images : ResourceDirectory"))
        assertTrue(linuxContent.contains("actual val logo: ResourceFile"))
        assertTrue(linuxContent.contains("public val linux_only: ResourceFile"))
        
        // Logo in linux should be overriden (linux-logo base64)
        // "linux-logo" in base64 is "bGludXgtbG9nbw=="
        assertTrue(linuxContent.contains("bGludXgtbG9nbw=="), "Base64 not found. Content: $linuxContent")
        assertTrue(linuxContent.contains("10"), "Size 10 not found. Content: $linuxContent")
        // Mime type might vary by OS
        assertTrue(linuxContent.contains("text/plain") || linuxContent.contains("application/octet-stream"), "Mime type not found. Content: $linuxContent")
        
        // Check items list
        assertTrue(linuxContent.contains("listOf(images)"), "Res items not found. Content: $linuxContent")
        assertTrue(linuxContent.contains("listOf(logo, linux_only)"), "images items not found. Content: $linuxContent")

        val expectedLinux = """
            package dev.limebeck.res

            import dev.limebeck.kmpResources.ResourceDirectory
            import dev.limebeck.kmpResources.ResourceFile
            import dev.limebeck.kmpResources.ResourceItem
            import kotlin.ByteArray
            import kotlin.Long
            import kotlin.OptIn
            import kotlin.String
            import kotlin.collections.List
            import kotlin.io.encoding.Base64
            import kotlin.io.encoding.ExperimentalEncodingApi

            public actual object Res : ResourceDirectory {
              actual override val name: String = "Res"

              actual override val items: List<ResourceItem> by lazy { listOf(images) }

              public actual object images : ResourceDirectory {
                actual override val name: String = "images"

                actual override val items: List<ResourceItem> by lazy { listOf(logo, linux_only) }

                public actual val logo: ResourceFile = object : ResourceFile {
                  override val name: String = "logo.png"

                  override val size: Long = 10

                  override val type: String = "image/png"

                  @OptIn(ExperimentalEncodingApi::class)
                  override fun readBytes(): ByteArray = Base64.Default.decode("bGludXgtbG9nbw==")
                }

                public val linux_only: ResourceFile = object : ResourceFile {
                  override val name: String = "linux_only.txt"

                  override val size: Long = 10

                  override val type: String = "text/plain"

                  @OptIn(ExperimentalEncodingApi::class)
                  override fun readBytes(): ByteArray = Base64.Default.decode("bGludXgtb25seQ==")
                }
              }
            }
            
        """.trimIndent()
        assertEquals(expectedLinux, linuxContent, "Generated linux code differs from expected")
    }

    @Test
    fun `Check task dependencies`() {
        initGradleBuild(
            """
            plugins {
                kotlin("multiplatform") version "2.1.0"
                id("dev.limebeck.kmp-resources")
            }
            
            kotlin {
                jvm()
                linuxX64()
            }
            
            kmpResources {
                packageName.set("dev.limebeck.res")
            }

            tasks.register("checkDependencies") {
                doLast {
                    val jvmProcessResources = tasks.findByName("jvmProcessResources")
                    val linuxX64ProcessResources = tasks.findByName("linuxX64ProcessResources")
                    
                    if (jvmProcessResources != null) {
                        val jvmDeps = jvmProcessResources.taskDependencies.getDependencies(jvmProcessResources).map { it.name }
                        println("JVM_DEPS: " + jvmDeps.sorted().joinToString(","))
                    }
                    if (linuxX64ProcessResources != null) {
                        val linuxDeps = linuxX64ProcessResources.taskDependencies.getDependencies(linuxX64ProcessResources).map { it.name }
                        println("LINUX_DEPS: " + linuxDeps.sorted().joinToString(","))
                    }

                    val generateJvm = tasks.findByName("generateKmpResourcesJvm")
                    if (generateJvm != null) {
                        val generateJvmDeps = generateJvm.taskDependencies.getDependencies(generateJvm).map { it.name }
                        println("GENERATE_JVM_DEPS: " + generateJvmDeps.sorted().joinToString(","))
                    }
                }
            }
        """.trimIndent()
        )

        val result = gradleRunner.withArguments("checkDependencies", "-q").build()
        val output = result.output
        
        val jvmDeps = output.lineSequence().find { it.contains("JVM_DEPS: ") }?.substringAfter("JVM_DEPS: ")?.split(",")?.map { it.trim() } ?: emptyList()
        val linuxDeps = output.lineSequence().find { it.contains("LINUX_DEPS: ") }?.substringAfter("LINUX_DEPS: ")?.split(",")?.map { it.trim() } ?: emptyList()
        val generateJvmDeps = output.lineSequence().find { it.contains("GENERATE_JVM_DEPS: ") }?.substringAfter("GENERATE_JVM_DEPS: ")?.split(",")?.map { it.trim() } ?: emptyList()
        
        assertTrue(jvmDeps.contains("generateKmpResourcesJvm"), "jvmProcessResources should depend on generateKmpResourcesJvm. Found: $jvmDeps")
//        assertTrue(jvmDeps.contains("metadataProcessResources") || jvmDeps.contains("commonProcessResources"), "jvmProcessResources should depend on metadataProcessResources or commonProcessResources. Found: $jvmDeps")
        
        assertTrue(linuxDeps.contains("generateKmpResourcesLinuxX64"), "linuxX64ProcessResources should depend on generateKmpResourcesLinuxX64. Found: $linuxDeps")
//        assertTrue(linuxDeps.contains("metadataProcessResources") || linuxDeps.contains("commonProcessResources"), "linuxX64ProcessResources should depend on metadataProcessResources or commonProcessResources. Found: $linuxDeps")

        assertTrue(generateJvmDeps.contains("generateKmpResourcesMetadata"), "generateKmpResourcesJvm should depend on generateKmpResourcesMetadata. Found: $generateJvmDeps")
    }
}
