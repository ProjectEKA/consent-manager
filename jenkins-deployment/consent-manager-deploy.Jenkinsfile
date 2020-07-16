podTemplate(containers: [
    containerTemplate(
      name: 'helm3',
      image: 'projecteka/helm3:324',
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
//         stage('Deploy Image to k8s'){
//             container('helm'){
//             //                 sh 'helm init --upgrade'
//                 sh 'helm list'
//                 sh "helm lint ./${HELM_CHART_DIRECTORY}"
//             //                 sh "helm upgrade --wait --timeout 60 ${HELM_APP_NAME} ./${HELM_CHART_DIRECTORY} --dry-run"
//             //                 sh "helm list | grep ${HELM_APP_NAME}"
//             }
//         }
        stage('List pods') {
            container('helm3') {
                withKubeConfig([credentialsId: 'sandbox_k8s_config'
//                                 caCertificate: 'LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUN5RENDQWJDZ0F3SUJBZ0lCQURBTkJna3Foa2lHOXcwQkFRc0ZBREFWTVJNd0VRWURWUVFERXdwcmRXSmwKY201bGRHVnpNQjRYRFRJd01EWXlOakU1TWpZeU5sb1hEVE13TURZeU5ERTVNall5Tmxvd0ZURVRNQkVHQTFVRQpBeE1LYTNWaVpYSnVaWFJsY3pDQ0FTSXdEUVlKS29aSWh2Y05BUUVCQlFBRGdnRVBBRENDQVFvQ2dnRUJBTExqClBLcDd5WlR0TEVTUWZtT3c3K21wL09qYjJWaXJmcnVialczcWdHczhTclVTMUwwK0ErNEFJL000N1p1a0lVa1UKdE1SZWFHWG90UmlyeFMxQUs1VWdMNDg1cGJIRnpWcm96aUtnM0JXVmxjbmN4NzBhNEdUOUdOL3NPdGdJTnJMNQo3VmNyVk9ORlZkemhiUVFPNFFlRmNKWHpnZ2VMYXAyQlRTN3ZwTE5xZ3BSUWNQemJWMVdhNUM4Q1ZMMVFpRC9jCmRER0g3cFhMM01BU1VDbXh6NVBRanpTbDdBbzZRaGZPalcybHUvODZSalhDanpZSUhpdGI4VnFFVFZuRUhoUnAKNUdvWHgrZ3d5akMxaEw2U0wzbjhkWUszbzVYS1kxOFNoaEpTWEc3SXJKeTJlRG1Sbjl4WjB2UldKb1o4U0lTaQo0RGFReGtQa1pYREZKSjF1bWRzQ0F3RUFBYU1qTUNFd0RnWURWUjBQQVFIL0JBUURBZ0trTUE4R0ExVWRFd0VCCi93UUZNQU1CQWY4d0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFLMlJDWGhqREwxcUh3SitvanZEamRkQUE3OG4Ka0Y5UFVrS0xYOVZ3eWg0amdlUTlHOVpvUDZvL3ovKzh1MGt5R25mZERnVzRrVmI0WGdTeXMrZFNmcXlucDU3RwpEbWY2SWVNa0FTRTdJb1d1TFZxUFU4VVJzazdRTERvNWlBVkhLcU9JVnpKZ2NCdm9qaE93TWdmVEk4Z1ZtUEJZCk9ic0x0enIvNG96TjJWMXFXRWVGMkowSkRSOXptYUxoM0xRY0RybFpJeFk1T1VJZ25idmE5SlhLSlduYjQ1NkMKVFB2SXM4ZGM3NExZbVRibndMQVdjNXF1ODlWTGRuZXZIRVZGWXJRNjFSL1Q5cUdqd1pZZlZOenkyUG42aUt4NApGU0lLazQvcG5hbVhXK2IyazY1RXIzSkZIbzB1a3BoVVpUUGhENGNHWjA0QkNXRVM4bkF3Vm9QVW1JYz0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=',
//                                 serverUrl: 'https://100.65.203.124:6443',
//                                 contextName: 'nhadmin@cluster.local',
//                                 clusterName: 'cluster.local',
//                                 namespace: 'nha-app-demo1'
                 ]) {
                    sh 'kubectl config view'
                    sh 'kubectl get pods'
                    sh 'helm lint ./${HELM_CHART_DIRECTORY}'
                }
            }
        }
    }
}