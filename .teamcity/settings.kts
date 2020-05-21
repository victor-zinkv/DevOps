import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2019.2"

project {

    vcsRoot(GitHubDevOps)

    buildType(BuildDocker)

    params {
        password("Check_Password", "credentialsJSON:4396e1aa-6538-4218-a82d-a4c2069312cb")
    }
}

object BuildDocker : BuildType({
    name = "Build Docker"

    params {
        param("env.Git_Branch", "${GitHubDevOps.paramRefs.buildVcsBranch}")
        param("GitVersion.SemVer", "")
        param("GitVersion.EscapedBranchName", "")
        param("teamcity.git.fetchAllHeads", "true")
        param("GitVersion.FullSemVer", "")
        param("branch_specification", "+:refs/heads/(*)")
        param("branch_default", "refs/heads/master")
    }

    vcs {
        root(GitHubDevOps)

        cleanCheckout = true
    }

    steps {
        script {
            name = "Get Git Version"
            scriptContent = """
                #!/usr/bin/env bash
                
                cp /data/teamcity_agent/conf/gitversion ./
                
                ./gitversion /output buildserver
            """.trimIndent()
        }
        script {
            name = "Show Git Version"
            scriptContent = """
                #!/usr/bin/env bash
                
                echo "%env.Git_Branch%"
                
                echo "%GitVersion.EscapedBranchName%"
                
                echo "%GitVersion.SemVer%"
                
                echo "%GitVersion.FullSemVer%"
            """.trimIndent()
        }
        dockerCommand {
            name = "Build Dockerfile"
            commandType = build {
                source = file {
                    path = "TestTasks/TeamCity/Docker/teamcity-docker-minimal-agent/Dockerfile"
                }
                namesAndTags = "devops-teamcity-docker-minimal-agent:0.1"
                commandArgs = "--pull"
            }
        }
    }

    triggers {
        vcs {
            branchFilter = ""
            watchChangesInDependencies = true
        }
    }

    dependencies {
        snapshot(AbsoluteId("Example1_Parameters")) {
        }
    }
})

object GitHubDevOps : GitVcsRoot({
    name = "GitHub DevOps"
    url = "https://github.com/spymobilfon/DevOps.git"
    branchSpec = "%branch_specification%"
    authMethod = password {
        userName = "spymobilfon@gmail.com"
        password = "credentialsJSON:91d25ed6-8d9d-4bc3-9e68-e7a21cc45a73"
    }
})
