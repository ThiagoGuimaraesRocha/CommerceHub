// Alternative build/test pipeline for the CommerceHub monorepo (Sprint 8).
// Requires a Jenkins agent with JDK 21, Docker (Quarkus Dev Services / Testcontainers)
// and bash. GitHub Actions remains the primary CI.
pipeline {
  agent any

  options {
    timestamps()
    timeout(time: 30, unit: 'MINUTES')
    disableConcurrentBuilds()
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Validate OpenShift manifests') {
      steps {
        sh './scripts/validate-manifests.sh'
      }
    }

    stage('Verify') {
      steps {
        sh './mvnw -B -ntp verify'
      }
    }
  }

  post {
    always {
      junit allowEmptyResults: true, testResults: 'services/*/target/surefire-reports/*.xml,services/*/target/failsafe-reports/*.xml'
      archiveArtifacts artifacts: 'services/*/target/surefire-reports/*,services/*/target/failsafe-reports/*', allowEmptyArchive: true
    }
  }
}
