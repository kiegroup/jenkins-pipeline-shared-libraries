local_lib=""

pipeline {
  agent any

  stages {
    stage('Test MavenCommand') {
      steps {
        script {
          checkout scm

          local_lib = library identifier: "local-lib@${GIT_BRANCH}", 
            retriever: modernSCM([$class: 'GitSCMSource', remote: "${GIT_URL}", branches: "${GIT_BRANCH}"]), 
            changelog: false

          local_lib.org.kie.jenkins.MavenCommand.new(this).withSettingsXmlId(maven.getSubmarineSettingsXmlId()).run('clean')
        }
      }
    }
  }
  post {
    always {
      cleanWs()
    }
  }
}