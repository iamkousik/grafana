//def environmentFileName = ".\\${params.DockerDeploymentFiles}\\image.properties"
def jobName = currentBuild.fullDisplayName

def loadEnvironmentVariables(path){
    def props = readProperties  file: path
    keys= props.keySet()
    for(key in keys) {
        value = props["${key}"]
        env."${key}" = "${value}"
        echo "env.${key} = ${value}"
        powershell script: """
          (Get-Content -path .\\docker\\$params.ModuleName\\${DEPLOY_RELEASE}\\docker-compose-base.yml -Raw).replace('${key}', '${value}') | Set-Content -path .\\docker\\$params.ModuleName\\${DEPLOY_RELEASE}\\docker-compose-base.yml -NoNewline
          (Get-Content -path .\\docker\\$params.ModuleName\\${DEPLOY_RELEASE}\\docker-compose-env.yml.token -Raw).replace('${key}', '${value}') | Set-Content -path .\\docker\\$params.ModuleName\\${DEPLOY_RELEASE}\\docker-compose-env.yml.token -NoNewline
          cat .\\docker\\$params.ModuleName\\${DEPLOY_RELEASE}\\docker-compose-base.yml
          cat .\\docker\\$params.ModuleName\\${DEPLOY_RELEASE}\\docker-compose-env.yml.token
        """
    }
}

choiceArray = ["Accounting-API-Docker","AR-Docker","BFF-Docker","CRM-RestApi-Docker","DIA-Excel-Docker","DIA-WebApi-Docker","DIA-Web-Docker","DIH-ReportEngine-Docker","DIH-SyncEngine-Docker","WEBAPI","WEBAPP","FPM-Service-Docker","Investran-SignalR","RS-Delivery-Docker","RS-Docker","RS-ExportService-Docker","RS-ImportService-Docker","RW-ExcelAddIn-Docker","RW-Export-Docker","RW-Import-Docker","RW-WebApi-Docker","RW-WebServices-Docker" ]

properties([
    parameters([
            choice(choices: choiceArray.collect { "$it\n" }.join(''),
                    description: 'Choose an Apllication',
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
      string(name: 'RepositoryURL', defaultValue: 'https://bitbucket.fis.dev/scm/investran/investraninv.git',  description: 'Bitbucket repository URL.')
      string(name: 'BranchName',  defaultValue: 'feature/investran-36',  description: 'Bitbucket repository branch name.')
      string(name: 'BitBucketCredential',  defaultValue: '9fa06b24-ae6e-4ca8-87ba-8c4e8c0319dd',  description: 'Bitbucket repository branch name.')
      string(name: 'ModuleName',  defaultValue: 'DIH',  description: 'Change Module Name')
      string(name: 'DockerEnv',  defaultValue: 'DEV',  description: 'Change Docker Environment Name')
      credentials(
        credentialType: 'com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials',
        defaultValue: 'docker-dev-env',
        description: 'The credentials needed to deploy.',
        name: 'deployCredentialsId',
        required: true
      )
    }                 
    stages {
      stage ('Source Code Checkout') {
        steps {
	        dir ("${WORKSPACE}") {
            cleanWs()
            checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "${params.RepositoryURL}", credentialsId: "${params.BitBucketCredential}"]],
                branches: [[name: "${params.BranchName}"]]], poll: false
                load ".\\docker\\/${params.ModuleName}\\/${DEPLOY_RELEASE}\\/${params.DockerEnv}\\env.properties"
            script {
              path = '.\\docker\\${params.ModuleName}\\${DEPLOY_RELEASE}\\${params.DockerEnv}\\env.properties'
              loadEnvironmentVariables(path)
            }
          }
        }
      }        
      stage('Copy Deployment Script') {
        steps {
          dir ("${WORKSPACE}") {
            powershell script:
            """
            Copy-Item ".\\docker\\Docker_Deployment.sh" -Destination ".\\docker\\$params.ModuleName\\${DEPLOY_RELEASE}"
            """
          }
        }
      }
      stage('Docker Cluster Deployment') {
          steps {
            script {
              def remote = [:]
                  remote.name = "${params.DockerEnv}"
                  remote.host = "$__dockerhost__"
                  remote.allowAnyHosts = true
            withCredentials([usernamePassword(credentialsId: '${deployCredentialsId}', 
              passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                  remote.user = USERNAME
                  remote.password = PASSWORD
            dir ("${WORKSPACE}") {
            sshCommand remote: remote, command: "mkdir -p /tmp/${DEPLOY_RELEASE}"
            sshPut remote: remote, from: ".\\docker\\${params.ModuleName}\\${DEPLOY_RELEASE}", into: "/tmp/${DEPLOY_RELEASE}/"
            sshCommand remote: remote, command: "ls -ltrh /tmp/${DEPLOY_RELEASE}/${DEPLOY_RELEASE}"
            sshCommand remote: remote, command: "cd /tmp/${DEPLOY_RELEASE}/${DEPLOY_RELEASE}; dos2unix Docker_Deployment.sh; sh Docker_Deployment.sh $__RepositoryName__:$__imageversion__ ${DEPLOY_RELEASE}"
            sshRemove remote: remote, path: "/tmp/${DEPLOY_RELEASE}"
              }
            }
          }
        }
      }
    }     
    post { 
        always {
          sleep 5
          emailext attachLog: true, body: 'Please find the attachment', mimeType: 'text/plain', subject: "[Jenkins] ${jobName} Report", from: "manish.sanike@fisglobal.com", to: "manish.sanike@fisglobal.com", recipientProviders: [[$class: 'CulpritsRecipientProvider']]
          cleanWs()
        }
     }    
  }