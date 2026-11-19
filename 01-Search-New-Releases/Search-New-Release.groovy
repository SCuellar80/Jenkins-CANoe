def lastFlashed         //to read last JSON data
def searchOutput = ''   //Returns file data string (to be converted to JSON) after found a new release

pipeline {
    agent any

    options {
        // Evita ejecuciones simultáneas del pipeline.
        disableConcurrentBuilds()
    }

    environment {
        RELEASE_FOLDER = 'C:\\Users\\copca\\OneDrive\\Trainings\\CICD\\CICD with CANoe\\SoftwareReleases'
        STATE_FOLDER = 'C:\\Users\\copca\\OneDrive\\Trainings\\CICD\\CICD with CANoe\\SoftwareReleases\\LastRelease'
        STATE_FILE = 'C:\\Users\\copca\\OneDrive\\Trainings\\CICD\\CICD with CANoe\\SoftwareReleases\\LastRelease\\last_release.json'
    }

    stages {

        stage('Create Folder') {
            when {
                expression {
                    // Creará el folder sí no existe
                    fileExists(env.STATE_FOLDER) == false
                }
            }

            steps {
                powershell '''
                    if (!(Test-Path -LiteralPath $env:STATE_FOLDER -PathType Container)) {
                        New-Item -ItemType Directory -Path $env:STATE_FOLDER -Force -ErrorAction Stop | Out-Null
                    }
                '''
                echo 'LastRelease creado correctamente.'
            }
        }

        stage('Create JSON') {
            when {
                expression {
                    // Crear el JSON si no existe.
                    fileExists(env.STATE_FILE) == false
                }
            }

            steps {
                script {
                    def initialState = [
                        NEW_RELEASE_NAME        : null,
                        NEW_RELEASE_DATE        : null,
                        NEW_RELEASE_PROCESSED   : null
                    ]
                    writeJSON(
                        file: env.STATE_FILE,
                        json: initialState,
                        pretty: 4
                    )
                    echo 'last_release.json creado correctamente.'
                }
            }
        }

        stage('Read JSON') {
            steps {
                script {
                    // Leer el estado actual.
                        lastFlashed = readJSON(
                        file: env.STATE_FILE,
                        returnPojo: true
                    )

                    echo """
                    NEW_RELEASE_NAME        : ${lastFlashed.NEW_RELEASE_NAME}
                    NEW_RELEASE_DATE        : ${lastFlashed.NEW_RELEASE_DATE}
                    NEW_RELEASE_PROCESSED   : ${lastFlashed.NEW_RELEASE_PROCESSED}
                    """

                    stage("Current: ${lastFlashed.NEW_RELEASE_NAME}") {
                        echo "NEW_RELEASE_NAME: ${lastFlashed.NEW_RELEASE_NAME}"
                    }
                    stage("Next: ${lastFlashed.NEW_RELEASE_PROCESSED}") {
                        echo "NEW_RELEASE_PROCESSED: ${lastFlashed.NEW_RELEASE_PROCESSED}"
                    }
                }
            }
        }

        stage('Search New Release') {
            when {
                expression {
                    // Buscará sólo sí es primera vez (null) o sí ya fue procesado
                    lastFlashed.NEW_RELEASE_PROCESSED == null || lastFlashed.NEW_RELEASE_PROCESSED == 'Yes'
                }
            }

            steps {
                script {
                    // CORRECCIÓN: Pasar la fecha del último release a PowerShell.
                    env.LAST_RELEASE_DATE = lastFlashed.NEW_RELEASE_DATE ?: ''

                    // Buscar el siguiente release cronológicamente.
                    searchOutput = powershell(
                    returnStdout: true,
                    script: '''
                        $releaseFolder = $env:RELEASE_FOLDER

                        $releases = @(
                            Get-ChildItem -LiteralPath $releaseFolder -File -Filter '*.bin' |
                            Sort-Object LastWriteTime, Name
                        )

                        if ($env:LAST_RELEASE_DATE) {
                            $lastDate = [DateTimeOffset]::Parse(
                                $env:LAST_RELEASE_DATE
                            )

                            $releases = @(
                                $releases | Where-Object {
                                    [DateTimeOffset]::Parse(
                                        $_.LastWriteTime.ToString('o')
                                    ) -gt $lastDate
                                }
                            )
                        }

                        if ($releases.Count -gt 0) {
                            $candidate = $releases[0]

                            [PSCustomObject]@{
                                NEW_RELEASE_NAME = $candidate.Name
                                NEW_RELEASE_DATE = $candidate.LastWriteTime.ToString('o')
                            } | ConvertTo-Json -Compress
                        }
                    '''
                    ).trim()

                    //Sí no encuentra release nuevo, lo escribe en la consola
                    if (!searchOutput) {
                        echo "No new release found!"
                    }
                    else {
                        echo "Release found!: $searchOutput"
                    }
                }
            }
        }

        stage('Update JSON') {
            when {
                expression {
                    // Actulizará datos sí searchOutput no está vacío
                    searchOutput != ''
                }
            }

            steps {
                script {
                    // convierte el searchOutput (string de datos) a JSON
                    def candidate = readJSON(
                        text: searchOutput,
                        returnPojo: true
                    )

                    // Preparar el nuevo estado.
                    def newState = [
                        NEW_RELEASE_NAME        : candidate.NEW_RELEASE_NAME,
                        NEW_RELEASE_DATE        : candidate.NEW_RELEASE_DATE,
                        NEW_RELEASE_PROCESSED   : 'Pending'
                    ]

                    // Actualizar last_release.json.
                    writeJSON(
                        file: env.STATE_FILE,
                        json: newState,
                        pretty: 4
                    )

                    echo """
                    Nuevo release encontrado:
                    NEW_RELEASE_NAME        : ${newState.NEW_RELEASE_NAME}
                    NEW_RELEASE_DATE        : ${newState.NEW_RELEASE_DATE}
                    NEW_RELEASE_PROCESSED   : ${newState.NEW_RELEASE_PROCESSED}
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Search-New-Release finalizado correctamente.'
        }

        failure {
            echo 'Search-New-Release terminó con un error.'
        }
    }
}
