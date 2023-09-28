pipeline {
    environment {
        APIKEY = credentials('api_key_dtrack')
        SONARTOKEN = credentials('sonar-jenkins')
    }
    agent {
        kubernetes {
            inheritFrom 'maven'
            defaultContainer 'maven'
        }
    }
    stages{
        stage('build and sonar') {
            steps {
                container('maven') {
                    script {
                        scannerHome = tool 'SonarQube Scanner'
                    }
                    withSonarQubeEnv('EFS_Sonar') {
                        withMaven(
                           mavenSettingsConfig: '5b6df7e0-ccd7-4cf8-87f1-281c1eebc89f'
                        ) {
                            sh "mvn test"
                            sh "mvn clean verify sonar:sonar -Dsonar.token=$SONARTOKEN -Dsonar.host.url=https://sonarqube.efs-techhub.com -Dsonar.projectKey=organizationmanager"
                        }
                    }
                }
            }
        }
        stage('executing syft') {
            steps {
                container('maven') {
                    script{
                        sh 'wget https://github.com/anchore/syft/releases/download/v0.88.0/syft_0.88.0_linux_amd64.tar.gz'
                        sh 'tar -xf syft_0.88.0_linux_amd64.tar.gz'
                        sh 'chmod +x syft'
                        sh './syft packages -o cyclonedx-json --file result.json dir:.'
                    }
                }
            }
        }
        stage('upload artefact') {
            steps {
                container('maven') {
                    sh 'curl -v -X "POST" "https://dtrack-api.efs-techhub.com/api/v1/bom" -H "Content-Type:multipart/form-data" -H "X-Api-Key: $APIKEY" -F "project=e195d90b-d04b-41d0-9494-5ecfb7868404" -F "bom=@result.json"'
                }
            }
        }
        stage('cleaning Workspace'){
            steps {
                cleanWs()
            }
        }
    }
}