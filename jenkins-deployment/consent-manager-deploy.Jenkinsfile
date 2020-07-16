podTemplate(containers: [
    containerTemplate(
      name: 'helm3',
      image: 'projecteka/helm3:3.2.4',
      resourceRequestCpu: '100m',
      resourceLimitCpu: '300m',
      resourceRequestMemory: '300Mi',
      resourceLimitMemory: '500Mi',
      ttyEnabled: true,
      command: 'cat'
    )
  ],

  volumes: [
    hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
    hostPathVolume(mountPath: '/usr/local/bin/helm', hostPath: '/usr/local/bin/helm')
  ]
  ) {
    node(POD_LABEL) {

        def REPOSITORY_URI = "ganesan92/consent-manager"
        def HELM_APP_NAME = "consent-manager"
        def HELM_CHART_DIRECTORY = "Charts/consent-manager"

        stage('Get latest version of code') {
          checkout scm
        }
//         stage('Check running containers') {
//             container('docker') {
//                 sh 'hostname'
//                 sh 'hostname -i'
//                 sh 'docker ps'
//                 sh 'ls'
//             }
//             container('kubectl') {
//                 sh 'kubectl get pods -n default'
//                 sh 'kubectl config view'
//             }
//             container('helm') {
//                 sh 'helm init --client-only --skip-refresh'
//                 sh 'helm repo update'
//             }
//         }
//         stage('Pull Image'){
//             container('docker'){
//                 sh 'docker image ls'
//                 sh "docker pull ${REPOSITORY_URI}:92f47fd"
//             }
//         }
//         stage('Build Image'){
//             container('docker'){
//
//               withCredentials([usernamePassword(credentialsId: 'docker-login', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
//                 sh 'docker login --username="${USERNAME}" --password="${PASSWORD}"'
//                 sh "docker build -t ${REPOSITORY_URI}:${BUILD_NUMBER} ."
//                 sh 'docker image ls'
//               }
//
//             }
//         }
//
//         stage('Push Image'){
//             container('docker'){
//               withCredentials([usernamePassword(credentialsId: 'docker-login', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
//                 sh 'docker image ls'
//                 sh "docker push ${REPOSITORY_URI}:${BUILD_NUMBER}"
//               }
//             }
//         }
//
        stage('Deploy Image to k8s'){
            withCredentials([string(credentialsId: 'kube_config_sandbox_cluster', variable: 'KUBECONFIG')]) {
                container('helm'){
                //                 sh 'helm init --upgrade'
                    sh 'helm list'
                    sh "helm lint ./${HELM_CHART_DIRECTORY}"
                //                 sh "helm upgrade --wait --timeout 60 ${HELM_APP_NAME} ./${HELM_CHART_DIRECTORY} --dry-run"
                //                 sh "helm list | grep ${HELM_APP_NAME}"
                }
            }

        }
    }
}