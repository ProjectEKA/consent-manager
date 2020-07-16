podTemplate(containers: [
    containerTemplate(
      name: 'helm3',
      image: 'vlasovatgmailcom/helm3-kubectl:latest',
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
        stage('List pods') {
            container('helm3') {
                withKubeConfig([credentialsId: 'sandbox_k8s_config']) {
                    sh 'kubectl config view'
                    sh 'kubectl get pods -n nha-app-demo1'
                    sh 'ls -al'
                    sh "helm lint ./${HELM_CHART_DIRECTORY}"
                }
            }
        }
    }
}