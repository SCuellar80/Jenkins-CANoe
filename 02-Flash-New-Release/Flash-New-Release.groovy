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

        stage('Read Last Release') {
            steps {
                script {
                    def releaseData = readJSON file: env.JSON_PATH

                    env.NEW_RELEASE_NAME = releaseData.NEW_RELEASE_NAME ?: ''
                    env.NEW_RELEASE_DATE = releaseData.NEW_RELEASE_DATE ?: ''
                    env.NEW_RELEASE_PROCESSED = releaseData.NEW_RELEASE_PROCESSED ?: ''

                    env.NEW_RELEASE_FILEPATH = "${env.RELEASES_PATH}\\${env.NEW_RELEASE_NAME}"

                    echo "NEW_RELEASE_NAME      : ${env.NEW_RELEASE_NAME}"
                    echo "NEW_RELEASE_DATE      : ${env.NEW_RELEASE_DATE}"
                    echo "NEW_RELEASE_PROCESSED : ${env.NEW_RELEASE_PROCESSED}"

                    stage("Rel: ${env.NEW_RELEASE_NAME}") {
                        echo "Release seleccionado: ${env.NEW_RELEASE_NAME}"
                    }
                }
            }
        }

        stage('Flashing') {
            when {
                expression {
                    return env.NEW_RELEASE_PROCESSED == 'Pending'
                }
            }

            steps {
                echo "Reflasheando ${env.NEW_RELEASE_FILEPATH}"
                echo 'Esperando 30 minutos...'
                sleep(time: 30, unit: 'SECONDS')
            }
        }

        stage('Update JSON') {
            when {
                expression {
                    return env.NEW_RELEASE_PROCESSED == 'Pending'
                }
            }

            steps {
                script {
                    def releaseData = readJSON file: env.JSON_PATH

                    releaseData.NEW_RELEASE_PROCESSED = 'Yes'

                    writeJSON file: env.JSON_PATH,
                              json: releaseData,
                              pretty: 4

                    echo 'NEW_RELEASE_PROCESSED actualizado a Yes'
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
