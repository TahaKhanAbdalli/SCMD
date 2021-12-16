

def servicesConfig = [

        [
                'servicenameprefix': 'lfw',
                'servicegitrepo'   : 'https://github.gapinc.com/GLV/LFW.git',
                'servicereportroot': 'get-lfw-fc'
        ],
        [
                'servicenameprefix': 'shipcalculator',
                'servicegitrepo'   : 'https://github.gapinc.com/GLV/ShipCalculator.git',
                'servicereportroot': 'glv-tdtx-listener'
        ]

]
def serviceRepoList = []
for (serviceConfig in servicesConfig) {

    def jobNamePrefix = serviceConfig['servicenameprefix']
    def serviceGitRepo = serviceConfig['servicegitrepo']
    serviceRepoList.push(serviceGitRepo)
    def serviceReportRoot = serviceConfig['servicereportroot']
    def jobSwitches = '--refresh-dependencies -PbuildVersion=${BUILD_ID} -Pversion=${BUILD_ID} -PgitSha=${GIT_COMMIT} -PsonarAnalysisMode="publish"'
    def uploadDomain = serviceConfig['uploadDomain']
    def branchBuildAndTest = jobNamePrefix + '-Branch-Build-and-Test'
    def branchBuildSonar = jobNamePrefix + '-Branch-Sonar'
    def masterBuildAndTest = jobNamePrefix + '-Master-Build-and-Test'
    def masterBuildSonar = jobNamePrefix + '-Master-Sonar'
    def masterBuildPublish = jobNamePrefix + '-Publish'
    def hasSonar = (serviceConfig['hasSonar'] != null) ? serviceConfig['hasSonar'] : true
    def scmTrigger = (serviceConfig['scmTrigger'] == null)
    def uploadTask = (serviceConfig['uploadTask'] != null) ? serviceConfig['uploadTask'] : 'uploadArchives'
    def hasKronosBuildFile = (serviceConfig['hasKronosBuildFile'] != null) ? serviceConfig['hasKronosBuildFile'] : false

    def serviceBranchBuild = job(branchBuildAndTest) {
        logRotator {
            numToKeep(10)
            artifactNumToKeep(1)
        }
        wrappers {
            credentialsBinding {
                usernamePassword('ARTIFACTORY_USERNAME', 'ARTIFACTORY_PASSWORD', 'jfrog-creds')
                usernamePassword('GITHUB_USERNAME', 'GITHUB_PASSWORD', '25a7ceb8-3272-4d65-a337-c839bfaa3235')
            }
        }

        scm {
            git {
                remote {
                    url serviceGitRepo
                    credentials('25a7ceb8-3272-4d65-a337-c839bfaa3235')
                }
                branch 'origin/*'
            }
        }

        steps {
            gradle {
                tasks('clean')
                tasks('test')
                tasks(' -g .')
                switches(jobSwitches)
                useWrapper(true)
                makeExecutable(true)
            }
        }

        if (scmTrigger) {
            triggers {
                githubPush() }
        }

        publishers {
            if (hasSonar) {
                downstreamParameterized {
                    trigger(branchBuildSonar) {
                        condition('SUCCESS')
                        parameters { gitRevision() }
                    }
                }
            }
            wsCleanup()

            wrappers {// colorizeOutput()
            }
        }
    }

    if (hasSonar) {
        def serviceBranchSonar = job(branchBuildSonar) {
            logRotator {
                numToKeep(5)
                artifactNumToKeep(1)
            }
            wrappers {
                credentialsBinding {
                    usernamePassword('ARTIFACTORY_USERNAME', 'ARTIFACTORY_PASSWORD', 'pt-inbound-transportation-artifactory-token')
                    string('SONAR_TOKEN','pt-inbound-transportation-sonar-token')
                    usernamePassword('GITHUB_USERNAME', 'GITHUB_PASSWORD', 'github_credential')
                }
            }

            scm {
                git {
                    remote {
                        url serviceGitRepo
                        credentials('github_credential')
                    }
                    branch 'origin/PGLV*'
                }
            }

            steps {
                gradle {
                    tasks('clean')
                    tasks('build')
                    tasks('sonar')
                    tasks(' -g .')
                    switches(jobSwitches)
                    useWrapper()
                }
            }
            publishers {
                wsCleanup()
            }

            wrappers { //colorizeOutput()
            }
        }

    }

    def serviceMasterBuild = job(masterBuildAndTest) {
        logRotator {
            numToKeep(10)
            artifactNumToKeep(1)
        }
        wrappers {
            credentialsBinding {
                usernamePassword('ARTIFACTORY_USERNAME', 'ARTIFACTORY_PASSWORD', 'pt-inbound-transportation-artifactory-token')
                usernamePassword('GITHUB_USERNAME', 'GITHUB_PASSWORD', 'github_credential')
            }
        }

        scm {
            git {
                remote {
                    url serviceGitRepo
                    credentials('github_credential')
                }
                branches('master')
            }
        }

        if (scmTrigger) {
            triggers { githubPush() }
        }

        steps {
            gradle {
                tasks('clean')
                tasks('test')
                tasks(' -g .')
                switches(jobSwitches)
                useWrapper(true)
                makeExecutable(true)
            }

        }


        publishers {

            def jobToTrigger = hasSonar ? masterBuildSonar : masterBuildPublish
            //def jobToTrigger =  masterBuildPublish
            downstreamParameterized {
                trigger(jobToTrigger) {
                    condition('SUCCESS')
                    parameters { gitRevision() }
                }
            }
            wsCleanup()

            wrappers { //colorizeOutput()
            }
        }
    }
}



def branchPublish = job('Publish-Branch-Build') {
    logRotator {
        numToKeep(5)
        artifactNumToKeep(1)
    }
    parameters {
        choiceParam('repo', serviceRepoList, 'Select the repo to build')
        textParam('branchToPublish', '', 'Enter the branch')
    }

    wrappers {
        credentialsBinding {
            usernamePassword('ARTIFACTORY_USERNAME', 'ARTIFACTORY_PASSWORD', 'jfrog-creds')
            usernamePassword('GITHUB_USERNAME', 'GITHUB_PASSWORD', '25a7ceb8-3272-4d65-a337-c839bfaa3235')
        }
    }

    scm {
        git {
            remote {
                url '${repo}'
                credentials('25a7ceb8-3272-4d65-a337-c839bfaa3235')
            }
            branch '${branchToPublish}'
        }
    }

    steps {
        gradle {
            tasks('clean')
            tasks('uploadArchives')
            tasks(' -g .')
            switches('-PbuildVersion=${BUILD_ID}-${branchToPublish} -Pversion=${BUILD_ID}-${branchToPublish} -PgitSha=${GIT_COMMIT}')
            useWrapper(true)
            makeExecutable(true)
        }
    }

    wrappers { //colorizeOutput()
    }
}



listView('GLV Master Jobs') {
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
    //jobFilters {
    //  status { status(Status.UNSTABLE) }
    //}
    jobs {
        for (serviceConfig in servicesConfig) {
            name(serviceConfig['servicenameprefix'] + '-Master-Build-and-Test')
            def hasSonar = (serviceConfig['hasSonar'] != null) ? serviceConfig['hasSonar'] : true
            if (hasSonar) {
                name(serviceConfig['servicenameprefix'] + '-Master-Sonar')
            }
            name(serviceConfig['servicenameprefix'] + '-Publish')
        }
    }
}

listView('GLV Branch Jobs') {
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
    //jobFilters {
    //  status { status(Status.UNSTABLE) }
    //}
    jobs {
        for (serviceConfig in servicesConfig) {
            name(serviceConfig['servicenameprefix'] + '-Branch-Build-and-Test')
            def hasSonar = (serviceConfig['hasSonar'] != null) ? serviceConfig['hasSonar'] : true
            if (hasSonar) {
                name(serviceConfig['servicenameprefix'] + '-Branch-Sonar')
            }
        }
        name('Publish-Branch-Build')
    }
}

listView('SCMD SeedJob') {
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
    jobs { name('SCMD-Seed-Job') }
}
