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
          image 'maven:3.8.6-jdk-11'
          label 'docker'
        }
      }
      steps {
        sh 'mvn --batch-mode --errors --fail-at-end --show-version clean test'
      }
    }
  }
}
