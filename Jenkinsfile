def environmentFileName       = ".\\buildconfig\\image.properties"
def DockerDeploymentFiles     = ".\\buildconfig\\docker-compose.yml"
//def ApplicationDirPath        = ".\\buildconfig"
def ApplicationDirName        = 'MyApplication'

pipeline {
    agent {
        label 'master'
    }
    options {
    	buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
  	}             
        triggers {
		GenericTrigger(
			// Pull request is opened, modified, edited, reopened, synchronize
			genericVariables: [
				[key: 'pr_action', value: '$.eventKey'],
				[key: 'repository', value: '$.pullRequest.toRef.repository.links'],
				// [key: 'repository', value: '$.pullRequest.toRef.repository.links.clone[]| select(.name=="ssh") | jq -r .href'],
				// [key: 'repository', value: '$.pullRequest.toRef.repository.links.clone[]| select(.name=="ssh") | jq -r .href'],
				[key: 'pr_branch', value: '$.pullRequest.fromRef.id'],
				[key: 'base_branch', value: '$.pullRequest.toRef.id']
			],
			regexpFilterText: '$pr_action:$base_branch',
			regexpFilterExpression: "(?:merged|opened|modified|edited|reopened|synchronize):refs/heads/development", 
			token: "${params.webhookToken}", // Need to create "webhookToken" parameter manually during build plan creation.
			causeString: 'Triggered',
			printContributedVariables: true,
			printPostContent: true,
			silentResponse: false
		)
	}    
      stages {
        stage('Download Application Codebase') {
            steps { 
                dir ("${WORKSPACE}") {
                    sh "mkdir -p ${ApplicationDirName}" 
                    dir("${ApplicationDirName}") {
                        script {
                                if ( repository_clone_0_name == "http" ) {
                                    repository_url = sh (script: "echo ${repository_clone_0_href}", returnStdout: true).trim()
                                } else {
                                    repository_url = sh (script: "echo ${repository_clone_1_href}", returnStdout: true).trim()
                                }
                                if (pr_action == "pr:merged") {
                                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "${repository_url}", credentialsId: "BitbucketUser"]], 
                                        extensions: [[$class: 'CloneOption', shallow: true, depth: 1], [$class: 'CheckoutOption', timeout: 30 ]], 
                                        branches: [[name: "${env.base_branch}"]]], poll: false
                                } else {
                                    checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: "${repository_url}", credentialsId: "BitbucketUser"]], 
                                        extensions: [[$class: 'CloneOption', shallow: true, depth: 1], [$class: 'CheckoutOption', timeout: 30 ]], 
                                        branches: [[name: "${env.pr_branch}"]]], poll: false
                                }                     
                        }
                        load "./${environmentFileName}"
                    }
                }                                 
            }
        }         
	stage('Modify Docker Deployment') {
          steps {
            dir ("${WORKSPACE}\\$ApplicationDirName") {
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
              withCredentials([sshUserPrivateKey(credentialsId: '${deployCredentialsId}', 
                        keyFileVariable: 'JENKINS_PRIVATE_KEY', usernameVariable: 'USERNAME')]) {
                 remote.user = USERNAME
                 remote.identityFile = JENKINS_PRIVATE_KEY
              dir ("${WORKSPACE}") {
	            sshCommand remote: remote, command: "mkdir -p /tmp/$env.ApplicationName"
              sshPut remote: remote, from: "${params.DockerDeploymentFiles}", into: "/tmp/$env.ApplicationName/"
	            sshCommand remote: remote, command: "ls -ltrh /tmp/$env.ApplicationName/${params.ApplicationDir}"
              sshCommand remote: remote, command: "cd /tmp/$env.ApplicationName/${params.ApplicationDir}; dos2unix Docker_Deployment.sh; sh Docker_Deployment.sh $env.RepositoryName:$env.image_version $env.ApplicationName"
              sshRemove remote: remote, path: "/tmp/$env.ApplicationName"
              }
            }
          }
        }
      }
    }     
    post { 
        always {
            script {
               if (currentBuild.result == 'SUCCESS') {
                  currentBuild.result = currentBuild.result ?: 'SUCCESS'
                  notifyBitbucket()
               }
            }
            cleanWs()
        }
    }    	 
}
