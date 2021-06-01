def call(Map pipelineParams) {

    pipeline {
        agent { label 'slave' }

        tools { maven 'maven 3.6.3' }
       
        parameters {
            booleanParam(
                    name: 'RELEASE_FLAG',
                    defaultValue: false,
                    description: 'Check this to release a version'
            )
        }

        stages{
            stage ('clean and clone') {
                steps {
                    cleanWs()
                    git branch: pipelineParams.branch,
                            credentialsId: 'whitesource-github-user',
                        url: pipelineParams.gitUrl
                }
            }
            stage('Build') {
                steps {
                    sh 'cd ${pipelineParams.path_to_pom} && mvn clean compile'
                }
            }
            stage('Test') {
                steps {
                    sh 'cd ${pipelineParams.path_to_pom} && mvn test'
                }
            }
            stage('Deploy') {
                steps {
                    script {
                        if (params.RELEASE_FLAG) {
                            sh """
                              mvn versions:set versions:commit -DremoveSnapshot                           
                          """

                            script{
                                env.TAG_VERSION = sh(script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
                            }
                            sshagent (credentials: ['whitesource-github-user']) {
                                sh 'git commit -am "committing release version" '
                                sh 'git push --set-upstream origin master'
                                sh 'git tag -a ${TAG_VERSION} -m "tagging release version" '
                                sh 'git push origin --tags'
                            }


                            sh """                            
                              mvn deploy
                          """

                            script{
                                env.NEXT_VERSION = bumpVersion(env.TAG_VERSION)
                            }

                            sh """                            
                              mvn -U versions:set versions:commit -DnewVersion=${NEXT_VERSION}
                          """
                            sshagent (credentials: ['whitesource-github-user'])  {
                                sh 'git commit -am "committing next version snapshot version ${NEXT_VERSION}" '
                                sh 'git push --set-upstream origin master'
                            }

                            sh """                            
                              mvn deploy
                          """

                        } else {
                            sh 'cd ${pipelineParams.path_to_pom} && mvn deploy'
                        }
                    }
                }
            }
        }
        post {
            always {
                junit(
                        allowEmptyResults: true,
                        testResults: '**/surefire-reports/*.xml'
                )
            }
        }
    }
}
