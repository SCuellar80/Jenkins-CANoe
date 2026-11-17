pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {
        JSON_PATH = 'C:\\Users\\copca\\OneDrive\\Trainings\\CICD\\CICD with CANoe\\SoftwareReleases\\LastRelease\\last_release.json'

        RELEASES_PATH = 'C:\\Users\\copca\\OneDrive\\Trainings\\CICD\\CICD with CANoe\\SoftwareReleases'
    }

    stages {

        stage('Read Last Flashed') {
            steps {
                script {
                    def releaseData = readJSON file: env.JSON_PATH

                    env.NEW_RELEASE_NAME = releaseData.NEW_RELEASE_NAME ?: ''
                    env.NEW_RELEASE_DATE = releaseData.NEW_RELEASE_DATE ?: ''
                    env.NEW_RELEASE_FLASHED = releaseData.NEW_RELEASE_FLASHED ?: ''

                    env.NEW_RELEASE_FILEPATH = "${env.RELEASES_PATH}\\${env.NEW_RELEASE_NAME}"

                    echo "NEW_RELEASE_NAME: ${env.NEW_RELEASE_NAME}"
                    echo "NEW_RELEASE_DATE: ${env.NEW_RELEASE_DATE}"
                    echo "NEW_RELEASE_FLASHED: ${env.NEW_RELEASE_FLASHED}"
                }
            }
        }

        stage('Flash New Release') {
            when {
                expression {
                    return env.NEW_RELEASE_FLASHED == 'Pending'
                }
            }

            steps {
                script {
                    echo "Reflasheando ${env.NEW_RELEASE_FILEPATH}"
                    echo 'Esperando 30 minutos...'
                    sleep(time: 30, unit: 'SECONDS')
                }
            }
        }

        stage('Update JSON') {
            when {
                expression {
                    return env.NEW_RELEASE_FLASHED == 'Pending'
                }
            }

            steps {
                script {
                    def releaseData = readJSON file: env.JSON_PATH

                    releaseData.NEW_RELEASE_FLASHED = 'Yes'

                    writeJSON file: env.JSON_PATH,
                              json: releaseData,
                              pretty: 4

                    echo 'NEW_RELEASE_FLASHED actualizado a Yes'
                }
            }
        }
    }

    post {
        always {
            echo 'Flash-New-Release finalizado.'
        }
    }
}
