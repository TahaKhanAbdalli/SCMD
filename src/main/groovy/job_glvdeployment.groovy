

def commonDeploySwitches = '-Papp_name=${APP_NAME} -Papp_memory=${APP_MEMORY} -Ptarget_env=${TARGET_ENVIRONMENT} -Partifact_id=${artifact_id} -PconfigServergitrepo=https://github.gapinc.com/GLV/glv-config.git'

def deploymentJobs = [
        [
                'title'          : 'deploy-to-local-lfw',
                'envs'           : ['development', 'integration','configuration'],
                'env_desc'       : 'Deploy to TDEV PCF Space',
                'artifactory_url': 'http://localhost:8082/artifactory/scmd-lfw',

        ]

]

for (deploymentJob in deploymentJobs) {
    def deployJob = job(deploymentJob['title']) {
        wrappers {
            credentialsBinding {

                usernamePassword('GITHUB_USERNAME', 'GITHUB_PASSWORD', '85766806-3cb8-474e-966b-9e6d00f27764')

                usernamePassword('ARTIFACTORY_USERNAME', 'ARTIFACTORY_PASSWORD', 'jfrog-creds')



            }
        }
        parameters {
            textParam('artifact_id', '', 'Input valid artifactId for the service')
            choiceParam('TARGET_ENVIRONMENT', deploymentJob['envs'], deploymentJob['env_desc'])
            choiceParam('APP_MEMORY', [
                    '1024',
                    '512',
                    '2048',
                    '4096',
                    '8192'
            ], 'Select the Amount of Memory Needed for your Application')
            choiceParam('APP_NAME',
                    [
                            'glv-lfw-listener',

                    ],
                    'Select the Application')
        }

        scm {
            git {
                remote {
                    url('https://github.com/imran-ishaq/SCMD.git')
                    credentials('85766806-3cb8-474e-966b-9e6d00f27764')
                }
                branch("main")
            }
        }
        steps {
            gradle {
                useWrapper(true)
                makeExecutable(true)
                tasks('deploy')
                fromRootBuildScriptDir(true)
                switches(deploymentJob['deploy_switches'])
            }
            sh './upload-war.sh' ${buildDir/libs}
        }

        logRotator {
            numToKeep(10)
            artifactNumToKeep(1)
        }
    }

}

listView('Preprod Deploy Job') {
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
    jobs { name('deploy-to-pre-prod') }

    listView('PROD Deploy Job') {
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
        jobs { name('deploy-to-prod') }
    }
}
