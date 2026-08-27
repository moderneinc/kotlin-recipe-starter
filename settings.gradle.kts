// The Code Genome Project is where org.openrewrite artifacts are published, so declare it ahead of
// the Gradle Plugin Portal for the org.openrewrite.build.* plugins below. Mirrors the repository
// org.openrewrite.build.recipe-repositories sets up for dependencies, which cannot reach plugin
// resolution because that happens before any project is configured. Declared only when both
// credentials resolve, so forks and credential-less clones fall through to the portal instead of
// failing on a 401; see the README section on Code Genome Project credentials.
pluginManagement {
    val codegenomeUsername = providers.gradleProperty("codegenomeUsername").getOrElse("")
    val codegenomePassword = providers.gradleProperty("codegenomePassword").getOrElse("")

    repositories {
        if (codegenomeUsername.isNotEmpty() && codegenomePassword.isNotEmpty()) {
            maven {
                name = "codegenome"
                url = uri("https://artifacts.codegenomeproject.org/maven")
                credentials {
                    username = codegenomeUsername
                    password = codegenomePassword
                }
                content {
                    includeGroupAndSubgroups("org.openrewrite")
                    includeGroupAndSubgroups("io.moderne")
                }
            }
        }
        gradlePluginPortal {
            content {
                excludeVersionByRegex(".+", ".+", ".+-rc[-]?[0-9]*")
            }
        }
    }
}

rootProject.name = "kotlin-recipe-starter"
