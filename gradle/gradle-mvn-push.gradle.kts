apply {
    plugin("org.gradle.maven")
}

val versionName: String by project

fun isReleaseBuild(): Boolean {
    return !versionName.contains("SNAPSHOT")
}

fun getReleaseRepositoryUrl(): String {
    return properties["RELEASE_REPOSITORY_URL"] as? String
        ?: "http://0.0.0.0:8081/repository/maven-releases/"
}

fun getSnapshotRepositoryUrl(): String {
    return properties["SNAPSHOT_REPOSITORY_URL"] as? String
        ?: "http://0.0.0.0:8081/repository/maven-snapshots/"
}

fun getRepositoryUsername(): String {
    return properties["SONATYPE_NEXUS_USERNAME"] as? String ?: ""
}

fun getRepositoryPassword(): String {
    return properties["SONATYPE_NEXUS_PASSWORD"] as? String ?: ""
}

afterEvaluate {
    tasks.named<Upload>("uploadArchives") {
        repositories.withGroovyBuilder {
            "mavenDeployer" {

                "repository"("url" to getReleaseRepositoryUrl()) {
                    "authentication"(
                        "userName" to getRepositoryUsername(),
                        "password" to getRepositoryPassword()
                    )
                }
                "snapshotRepository"("url" to getSnapshotRepositoryUrl()) {
                    "authentication"(
                        "userName" to getRepositoryUsername(),
                        "password" to getRepositoryPassword()
                    )
                }
                "pom" {
                    val group: String by project
                    val pomArtifactId: String by project
                    val versionName: String by project

                    setProperty("groupId", group)
                    setProperty("artifactId", pomArtifactId)
                    setProperty("version", versionName)

                    "project" {
                        val pomName: String by project
                        val pomPackaging: String by project
                        val pomDescription: String by project
                        val pomUrl: String by project

                        setProperty("name", pomName)
                        setProperty("packaging", pomPackaging)
                        setProperty("description", pomDescription)
                        setProperty("url", pomUrl)

                        "licenses" {
                            "license" {
                                val pomLicenceName: String by project
                                val pomLicenceUrl: String by project
                                val pomLicenceDist: String by project

                                setProperty("name", pomLicenceName)
                                setProperty("url", pomLicenceUrl)
                                setProperty("distribution", pomLicenceDist)
                            }
                        }

                        "developers" {
                            "developer" {
                                val pomDeveloperId: String by project
                                val pomDeveloperName: String by project

                                setProperty("id", pomDeveloperId)
                                setProperty("name", pomDeveloperName)
                            }
                        }
                    }
                }
            }
        }
    }
}