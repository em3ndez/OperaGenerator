plugins {
    id("java")
    id("application")
}

group = "com.kousenit"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    val overrideMain = project.findProperty("mainClass") as String?
    mainClass.set(overrideMain ?: "com.kousenit.OperaGeneratorApp")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("dev.langchain4j:langchain4j-bom:1.5.0"))
    implementation("dev.langchain4j:langchain4j")
    implementation("dev.langchain4j:langchain4j-core")
    implementation("dev.langchain4j:langchain4j-open-ai")
    implementation("dev.langchain4j:langchain4j-mistral-ai")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini")
    implementation("dev.langchain4j:langchain4j-ollama")
    implementation("dev.langchain4j:langchain4j-anthropic")

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("info.picocli:picocli:4.7.5")
    
    // For ElevenLabs API JSON serialization
    implementation("com.google.code.gson:gson:2.11.0")
    
    // For playing generated audio files
    implementation("javazoom:jlayer:1.0.1")

    testImplementation(platform("org.junit:junit-bom:5.13.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.test {
    useJUnitPlatform {
        // Support for JUnit 5 tags
        if (project.hasProperty("test.tags")) {
            includeTags(project.property("test.tags").toString())
        }
        // Exclude expensive tests by default unless explicitly included
        if (!project.hasProperty("test.tags")) {
            excludeTags("expensive", "integration")
        }
    }
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}

// Task to run only unit tests (no API calls)
tasks.register<Test>("unitTest") {
    useJUnitPlatform {
        excludeTags("integration", "expensive")
    }
    description = "Run only unit tests (no API calls)"
}

// Task to run integration tests (API calls but not expensive)
tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
        excludeTags("expensive")
    }
    description = "Run integration tests (API calls)"
}

// Task to run expensive tests (full opera generation)
tasks.register<Test>("expensiveTest") {
    useJUnitPlatform {
        includeTags("expensive")
    }
    description = "Run expensive tests (full opera generation)"
}
