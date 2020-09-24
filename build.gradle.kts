import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

plugins {
    kotlin("jvm") version "1.4.0"
    kotlin("plugin.serialization") version "1.4.0"
    kotlin("kapt") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "sunrise"
version = "0.2.0"

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    maven(url = "https://mirrors.huaweicloud.com/repository/maven")
    mavenCentral()
    jcenter()
}

val miraiCoreVersion = "1.3.0"
val miraiConsoleVersion = "1.0-M4"
val ktorVersion = "1.3.71"
val kotlinVersion = "1.3.71"
val kotlinSerializationVersion = "1.0.0-RC"
val kotlinx_io_version = "0.1.1"

//fun ktor(id: String, version: String = this@Build_gradle.ktorVersion) = "io.ktor:ktor-$id:$version"
//fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("net.mamoe:mirai-core:$miraiCoreVersion")
    compileOnly("net.mamoe:mirai-core-qqandroid:$miraiCoreVersion")
    compileOnly("net.mamoe:mirai-console:$miraiConsoleVersion")
    compileOnly ("org.jetbrains.kotlinx:kotlinx-io-jvm:$kotlinx_io_version")

    val autoService = "1.0-rc7"
    kapt("com.google.auto.service", "auto-service", autoService)
    compileOnly("com.google.auto.service", "auto-service-annotations", autoService)

    api("net.mamoe:mirai-console:$miraiConsoleVersion")

//    implementation("net.mamoe:mirai-serialization-common:$miraiCoreVersion")
//    implementation("net.mamoe:mirai-serialization:$miraiCoreVersion")
    implementation("org.brotli:dec:0.1.2")
    implementation(files("src/main/resources/libs/commons-compress-1.19.jar"))
    implementation("io.github.biezhi:webp-io:0.0.5")
//    implementation("com.twelvemonkeys.imageio:imageio-tiff:3.4.1")
    implementation("org.dom4j:dom4j:2.1.1")
    implementation("org.xerial:sqlite-jdbc:3.32.3.1")
    implementation("com.alibaba:fastjson:1.2.41")

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation("net.mamoe:mirai-core:$miraiCoreVersion")
//    testImplementation("org.jline:jline-terminal-jansi:3.9.0")
    testImplementation("net.mamoe:mirai-core-qqandroid:$miraiCoreVersion")
    testImplementation("net.mamoe:mirai-console:$miraiConsoleVersion")
    testImplementation("net.mamoe:mirai-console-pure:$miraiConsoleVersion")
//    testImplementation("net.mamoe:mirai-console-terminal:$miraiConsoleVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-io-jvm:$kotlinx_io_version")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
    }

    val runMiraiConsole by creating(JavaExec::class.java) {
        group = "mirai"
        dependsOn(shadowJar)
        dependsOn(testClasses)

        val testConsoleDir = "test"

        doFirst {
            fun removeOldVersions() {
                File("$testConsoleDir/plugins/").walk()
                    .filter { it.name.matches(Regex("""${project.name}-.*-all.jar""")) }
                    .forEach {
                        it.delete()
                        println("deleting old files: ${it.name}")
                    }
            }

            fun copyBuildOutput() {
                File("build/libs/").walk()
                    .filter { it.name.contains("-all") }
                    .maxBy { it.lastModified() }
                    ?.let {
                        println("Coping ${it.name}")
                        it.inputStream()
                            .transferTo1(File("$testConsoleDir/plugins/${it.name}").apply { createNewFile() }
                                .outputStream())
                        println("Copied ${it.name}")
                    }
            }

            workingDir = File(testConsoleDir)
            workingDir.mkdir()
            File(workingDir, "plugins").mkdir()
            removeOldVersions()
            copyBuildOutput()

            classpath = sourceSets["test"].runtimeClasspath
            main = "mirai.RunMirai"
            standardInput = System.`in`
            args(miraiCoreVersion, miraiConsoleVersion)
        }
    }
}


@Throws(IOException::class)
fun InputStream.transferTo1(out: OutputStream): Long {
    Objects.requireNonNull(out, "out")
    var transferred: Long = 0
    val buffer = ByteArray(8192)
    var read: Int
    while (this.read(buffer, 0, 8192).also { read = it } >= 0) {
        out.write(buffer, 0, read)
        transferred += read.toLong()
    }
    return transferred
}