plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

val libGroup = "io.github.gayatripatel10091992"
val libVersion = providers.gradleProperty("libVersion").getOrElse("1.0.0")

android {
    namespace = "com.gayatripatel.logkit"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
        aarMetadata { minCompileSdk = 21 }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    jvmToolchain(17)
    // Library hygiene: every public declaration must state its visibility and type.
    explicitApi()
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = libGroup
            artifactId = "logkit"
            version = libVersion
            afterEvaluate { from(components["release"]) }

            pom {
                name.set("LogKit")
                description.set("Small, dependency-light logging library for Android: pluggable sinks, PII redaction, rolling files and an in-app log buffer.")
                url.set("https://github.com/gayatripatel10091992/android-logkit")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("gayatripatel10091992")
                        name.set("Gayatri Patel")
                    }
                }
                scm {
                    url.set("https://github.com/gayatripatel10091992/android-logkit")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            val repo = System.getenv("GITHUB_REPOSITORY") ?: "gayatripatel10091992/android-logkit"
            url = uri("https://maven.pkg.github.com/$repo")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
