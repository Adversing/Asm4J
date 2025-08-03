plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.adversing"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("org.jetbrains:annotations:26.0.1")
    annotationProcessor("org.jetbrains:annotations:26.0.1")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
    options.compilerArgs.add("--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED")
}

application {
    mainClass.set("me.adversing.Main")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "me.adversing.Main"
        )
    }
}

tasks.withType<JavaExec> {
    jvmArgs(
        "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED"
    )
}

tasks.shadowJar {
    archiveClassifier.set("all")
    manifest {
        attributes(
            "Main-Class" to "me.adversing.Main",
            "Add-Exports" to "java.base/jdk.internal.misc",
            "Add-Opens" to "java.base/jdk.internal.misc"
        )
    }
}

