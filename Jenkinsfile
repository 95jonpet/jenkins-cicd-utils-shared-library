#!/usr/bin/env groovy

pipeline {
  agent none
  options {
    timeout(time: 1, unit: 'HOURS')
    timestamps()
  }
  stages {
    stage('Test') {
      agent {
        docker {
          image 'maven:3.6.3-jdk-8'
          label 'docker'
        }
      }
      steps {
        sh 'mvn --batch-mode --errors --fail-at-end --show-version clean test'
      }
    }
  }
}
