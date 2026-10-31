plugins {
    id("fabric-loom")
}

base {
    archivesName.set("bettermob-fabric-mc26")
}

extensions.configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

dependencies {
    "minecraft"("com.mojang:minecraft:26.3")
    "implementation"("net.fabricmc:fabric-loader:0.20.1")
    "implementation"("net.fabricmc.fabric-api:fabric-api:0.165.0+26.3")

    implementation(project(":bettermob-core"))
    include(project(":bettermob-core"))
    include(project(":bettermob-api"))
    include("org.yaml:snakeyaml:2.4")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
