def jobName = currentBuild.fullDisplayName
currentBuild.displayName = "${ModuleName}-${params.DockerEnv}-${DEPLOY_RELEASE}-#"+currentBuild.number

choiceArray = ["Accounting-API-Docker.json","AR-Docker.json","BFF-Docker.json","CRM-RestApi-Docker.json","DIA-Excel-Docker.json","DIA-WebApi-Docker.json","DIA-Web-Docker.json","DIH-ReportEngine-Docker.json","DIH-SyncEngine-Docker.json","WEBAPI.json","WEBAPP.json","FPM-Service-Docker.json","Investran-SignalR.json","RS-Delivery-Docker.json","RS-Docker.json","RS-ExportService-Docker.json","RS-ImportService-Docker.json","RW-ExcelAddIn-Docker.json","RW-Export-Docker.json","RW-Import-Docker.json","RW-WebApi-Docker.json","RW-WebServices-Docker.json" ]
properties([
    parameters([
            choice(choices: choiceArray.collect { "$it\n" }.join(''),
                    description: 'Choose an Apllication Json File',
                    name: 'DEPLOY_RELEASE')
    ])
])

pipeline {
    agent {
      label 'master'
    }
    options {
    	buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
    }
    parameters { 
      string(name: 'JsonRepositoryURL', defaultValue: 'https://bitbucket.fis.dev/scm/investran/investraninv.git',  description: 'Bitbucket repository URL.')
      string(name: 'JsonBranchName',  defaultValue: 'development',  description: 'Bitbucket repository branch name.')
      string(name: 'BitBucketCrds',  defaultValue: '9fa06b24-ae6e-4ca8-87ba-8c4e8c0319dd',  description: 'Bitbucket repository branch name.')
    }
    stages {
      stage ('Source Code Checkout') {
        steps {
	        dir ("${WORKSPACE}") {
            cleanWs()
            checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "${params.JsonRepositoryURL}", credentialsId: "${params.BitBucketCrds}"]],
                branches: [[name: "${params.JsonBranchName}"]]], poll: false
            }
          }
        }
      }
      post { 
        always {
          sleep 5
          cleanWs()
        }
     }    
  }
