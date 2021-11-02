def environmentFileName       = ".\\buildconfig\\image.properties"
def DockerDeploymentFiles     = ".\\buildconfig\\docker-compose.yml"

pipeline {
    agent {
        label 'master'
    }
    environment {
        PATH = "C:\\Program Files\\Git\\bin"
    }
    options {
    	buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
  	}
    parameters {    
      string(name: 'RepositoryURL', defaultValue: 'https://github.com/iamkousik/grafana.git',  description: 'Bitbucket repository URL.')
      string(name: 'BranchName',  defaultValue: 'development',  description: 'Bitbucket repository branch name.')
    }                 
    stages {
        stage ('Source Code Checkout') {
            steps {
                checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "${params.RepositoryURL}", credentialsId: "BitbucketUser"]],
                branches: [[name: "${params.BranchName}"]]], poll: false
                   
                    load "./${environmentFileName}"                    
                }
        }        
	    stage('Modify Docker Deployment') {
          steps {
            dir ("${WORKSPACE}") {
            powershell script:
            """
            (Get-Content -path $DockerDeploymentFiles -Raw).replace('\${RepositoryName}', '$evn.RepositoryName') | Set-Content -path $DockerDeploymentFiles
            (Get-Content -path $DockerDeploymentFiles -Raw).replace('\${ImageVersion}', '$env.image_version') | Set-Content -path $DockerDeploymentFiles
            cat $DockerDeploymentFiles
            """
		    }
		  }
	    }
        stage('Docker Cluster Deployment') {
          steps {
            script {
              def remote = [:]
                  remote.name = "$env.name"
                  remote.host = "$env.hostname"
                  remote.allowAnyHosts = true
              withCredentials([sshUserPrivateKey(credentialsId: 'rundeck', 
                        keyFileVariable: 'JENKINS_PRIVATE_KEY', usernameVariable: 'USERNAME')]) {
                 remote.user = USERNAME
                 remote.identityFile = JENKINS_PRIVATE_KEY
            dir ("${WORKSPACE}") {
            sshCommand remote: remote, command: "mkdir -p /tmp/$env.ApplicationName"
            sshPut remote: remote, from: "$DockerDeploymentFiles", into: "/tmp/$env.ApplicationName/"
            sshPut remote: remote, from: '.\\buildconfig\\Docker_Deployment.sh', into: "/tmp/$env.ApplicationName/"
            sshCommand remote: remote, command: "cd /tmp/$env.ApplicationName/; sh Docker_Deployment.sh $env.RepositoryName:$env.image_version $env.ApplicationName"
            sshCommand remote: remote, command: "rm -rf /tmp/$env.ApplicationName"
              }
            }
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