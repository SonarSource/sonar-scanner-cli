@Library('SonarSource@2.2') _
pipeline {
  agent {
    label 'linux'
  }
  parameters {
    string(name: 'GIT_SHA1', description: 'Git SHA1 (provided by travisci hook job)')
    string(name: 'CI_BUILD_NAME', defaultValue: 'sonar-scanner-cli', description: 'Build Name (provided by travisci hook job)')
    string(name: 'CI_BUILD_NUMBER', description: 'Build Number (provided by travisci hook job)')
    string(name: 'GITHUB_BRANCH', defaultValue: 'master', description: 'Git branch (provided by travisci hook job)')
    string(name: 'GITHUB_REPOSITORY_OWNER', defaultValue: 'SonarSource', description: 'Github repository owner(provided by travisci hook job)')
  }
  environment {
    SONARSOURCE_QA = 'true'
    MAVEN_TOOL = 'Maven 3.6.x'
    JDK_VERSION = 'Java 11'
  }
  stages {
    stage('Notify') {
      steps {
        sendAllNotificationQaStarted()
      }
    }
    stage('QA') {
      parallel {
        stage('DOGFOOD/linux') {
          agent {
            label 'linux'
          }
          steps {
            runITs("DOGFOOD")
          }
        }     
        stage('LATEST_RELEASE[6.7]/linux') {
          agent {
            label 'linux'
          }
          environment {
				    JDK_VERSION = 'Java 8'
				  }
          steps {
            runITs("LATEST_RELEASE[6.7]")
          }
        }
        stage('LATEST_RELEASE/linux') {
          agent {
            label 'linux'
          }
          steps {
            runITs("LATEST_RELEASE")            
          }
        }                       

        stage('DOGFOOD/windows') {
          agent {
            label 'windows'
          }
          steps {
            runITs("DOGFOOD")
          }
        }     
        stage('LATEST_RELEASE[6.7]/windows') {
          agent {
            label 'windows'
          }
          environment {
				    JDK_VERSION = 'Java 8'
				  }
          steps {
            runITs("LATEST_RELEASE[6.7]")
          }
        }
        stage('LATEST_RELEASE/windows') {
          agent {
            label 'windows'
          }
          steps {
            runITs("LATEST_RELEASE")            
          }
        }    

        stage('DOGFOOD/macosx') {
          agent {
            label 'macosx'
          }
          steps {
            runITs("DOGFOOD")
          }
        }     
        stage('LATEST_RELEASE[6.7]/macosx') {
          agent {
            label 'macosx'
          }
          environment {
				    JDK_VERSION = 'Java 8'
				  }
          steps {
            runITs("LATEST_RELEASE[6.7]")
          }
        }
        stage('LATEST_RELEASE/macosx') {
          agent {
            label 'macosx'
          }
          steps {
            runITs("LATEST_RELEASE")            
          }
        }    
      }         
      post {
        always {
          sendAllNotificationQaResult()
        }
      }

    }
    stage('Promote') {
      steps {
        repoxPromoteBuild()
      }
      post {
        always {
          sendAllNotificationPromote()
        }
      }
    }
  }
}

def runITs(SQ_VERSION) {    
  withMaven(maven: MAVEN_TOOL) {
    dir("it") {    
      runMavenOrch(JDK_VERSION,"verify -Dsonar.runtimeVersion=$SQ_VERSION -U")
    }
  }
}