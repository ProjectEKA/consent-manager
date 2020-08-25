# 🚀 Dev Setup 2.0

## 👋🏼 Prerequisites for development

- MVN
    - [https://maven.apache.org/install.html](https://maven.apache.org/install.html) (official install, top download link will take you to the artefacts)
    - [https://github.com/rajivkanaujia/alphaworks/wiki/Installing-Maven](https://github.com/rajivkanaujia/alphaworks/wiki/Installing-Maven) (brew install)
- Postgres (optional)
    - You can use postgres docker container if you would like to. There will be instructions about how to do it.
- Intellij
    - [https://www.jetbrains.com/idea/download/#section=mac](https://www.jetbrains.com/idea/download/#section=mac)
- Rider (for c# - HIP)
    - [https://www.jetbrains.com/rider/download/#section=mac](https://www.jetbrains.com/rider/download/#section=mac)
- VS Code
    - [https://code.visualstudio.com/download](https://code.visualstudio.com/download)
- Android Studio (only for app development)
    - [https://developer.android.com/studio](https://developer.android.com/studio)
    
## 👋🏼 Prerequisites for just running the services

- Java 11
    - [https://docs.oracle.com/en/java/javase/11/install/installation-jdk-macos.html#GUID-F575EB4A-70D3-4AB4-A20E-DBE95171AB5F](https://docs.oracle.com/en/java/javase/11/install/installation-jdk-macos.html#GUID-F575EB4A-70D3-4AB4-A20E-DBE95171AB5F) (Official steps to install Java 11)
    - [https://www.oracle.com/java/technologies/javase-jdk11-downloads.html](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) (JDK can be downloaded here)
- Docker
    - [https://docs.docker.com/docker-for-mac/install/](https://docs.docker.com/docker-for-mac/install/) (Official docker install)
- .Net core (v3.1.100)
    - [https://dotnet.microsoft.com/download/dotnet-core/3.1](https://dotnet.microsoft.com/download/dotnet-core/3.1)
- Postman (optional)
    - [https://www.postman.com/downloads/](https://www.postman.com/downloads/)
    
## Services:

- [Gateway](https://github.com/ProjectEKA/gateway)
- [Consent Manager](https://github.com/ProjectEKA/consent-manager)
- [HIU](https://github.com/ProjectEKA/health-information-user)
- [HIP](https://github.com/ProjectEKA/hip-service)
- [Central Registry](https://github.com/ProjectEKA/Client-Registry)
- [Otp-Service](https://github.com/ProjectEKA/otp_service)

## Front-ends:

- [Jatayu](https://github.com/ProjectEKA/Jataayu)
- [HIU-UI](https://github.com/ProjectEKA/hiu-ui)

## Infrastructure used in the project:

- Rabbitmq
- Postgres
- Keycloak
- Orthanc *
- Elastic **
- Kibana **
- Redis **

(*) - Needed only for HIU when dicom image transferred from HIP
(**) - Optional for local setup

## Clean up (recommended always)

1. Run following commands to clean up your local system, before running any infra setup

  ```bash
  docker-compose -f docker-compose-infra-lite.yml down
  docker system prune -a -f
  docker volume rm $(docker volume ls -qf dangling=true)
  ```

## Setup infra using docker before running services:

1. Clone the consent-manager repository
2. In the root directory, you should see the docker-compose-infra-lite.yml
3. In the command line, run the following

    ```bash
    docker pull projecteka/monitor-db-initializer
    docker pull projecteka/gateway-db-initializer
    docker pull projecteka/cm-db-initializer
    docker pull projecteka/hiu-db-initializer
    docker-compose -f docker-compose-infra-lite.yml up -d
     
    docker logs $(docker ps -aqf "name=^cm-db-setup$")
    docker logs $(docker ps -aqf "name=^hiu-db-setup$")
    docker logs $(docker ps -aqf "name=^keycloak-setup$")
        # if you see any errors, run the docker-compose again
   
    docker exec -it $(docker ps -aqf "name=^postgres$") /bin/bash
    psql -U postgres consent_manager
    \d # should list all the tables
    \c health_information_user
    \d # should list all the tables
    exit # twice
    ```
4. In order to have route table for bridges and CM, clone the gateway repository, and run the following commands.

    ```bash
    brew install postgresql # only if you don't have psql in your machine
    chmod +x db-init-local.sh
    ./db-init-local.sh
    ```
   
**Note:** In case, you want to run Kibana, elastic, use *docker-compose-backend.yml*

5. Keycloak runs at [http://localhost:9001](http://localhost:9001)

    1. Login with user-name: admin, password: welcome
    2. There are two realms `Consent-Manager` and `Central-Registry`
    3. `Consent-Manager` is only for activities with consent-manager service (consent-manager internal service calls and user-management)
    4. `Central-Registry` is only for intra-service authentication and authorisation, and being used by **Gateway** service. For example if *consent-manager* wants to call to *gateway,* then consent-manager needs to get a token from **Gateway** using the client-id and client-secret of its own, and it should have a role of `CM` assigned.
    **Note:** All the clients needed for local development added into the keycloak automatically. Need to assign the role manually.
        - 10000002 with role `HIU` and `HIP`
        - 10000005 with role `HIU` and `HIP`
        - gateway with a role `gateway`
        - ncg with a role 'CM'
        
    ### How to add a client

    1. Click on `Clients`
    2. Click on `Create` button in the top right corner of the clients table.
    3. Enter the client id, i.e. `10000002`
    4. Click on `Create`
    5. On the clients page make the following the changes
        - Change `Access Type` to Confidential.
        - Turn on `Service Accounts Enabled` flag.
        - Turn on `Authorization` flag too.
        - Enter some random url in the `Valid Redirect URIs`. for example [*http://localhost:8080*](http://localhost:8080/).
        - Click `save` (**tip:** From `Credentials` you can copy the `Secret` always)

    ### How to add a role in the realm

    1. On the left-hand menu, click on `Roles`
    2. Click on `Add Role`
    3. Enter Role Name, for example `HIU`
    4. Click `Save`
    5. Repeat the same steps for the roles **(HIP, HIU, Gateway, CM)** you want to add.

    ### How to add a service role to a client

    1. Click on `Clients`
    2. Go to the client (for example: ncg) which you want to add role
    3. Click on `Service Account Roles` tab
    4. On the `Available Roles` you should see the roles you just created, select the role you want to assign, and then click `Add Selected`
    5. Repeat the same steps for all the clients.
        
5. Setup RabbitMQ

    1. Clone [infrastructure](https://github.com/ProjectEKA/infrastructure) repo
    2. Run the following commands

        ```bash
        docker-compose -f docker-compose-rabbitmq.yml up -d
        ./rabbitmqDeploy.sh
    
        docker exec -it $(docker ps -aqf "name=^rabbitmq$") /bin/bash
        rabbitmqctl list_queues  # should see all the queues
        exit
        ```

## How to 🏃🏻‍♀️ services

### Central-Registry a.k.a Client-Registry

1. Clone [central-registry](https://github.com/ProjectEKA/Client-Registry)
2. Run through command line

    ```bash
    cd client-registry
    ./gradlew bootRun
    ```

**Note:** In case, if default values specified in the [application.properties](http://application.properties) are not you use, you can change them accordingly and run.

### OTP-Service

1. Clone [otp-service](https://github.com/ProjectEKA/otp_service)
2. Run through command line

    ```bash
    cd otp-service
    dotnet run --project src/In.ProjectEKA.OtpService/In.ProjectEKA.OtpService.csproj --environment "local"
    ```

### Gateway

1. Clone [gateway](https://github.com/ProjectEKA/gateway)
2. Copy the client-secret [http://localhost:9001/auth/admin/master/console/#/realms/central-registry/clients](http://localhost:9001/auth/admin/master/console/#/realms/central-registry/clients) of `gateway` under `credentials` tab, and use it for a CLIENT_SECRET (client under *central-registry* realm)
3. Run through command line

    ```bash
    cd gateway
    CLIENT_SECRET=${CLIENT_SECRET} ./gradlew bootRunLocal
    ```

### Consent-Manager

1. Clone [Consent-Manager](https://github.com/ProjectEKA/consent-manager)
2. You need to get client secret from keycloak 
3. Copy the client-secret [http://localhost:9001/auth/admin/master/console/#/realms/consent-manager/clients](http://localhost:9001/auth/admin/master/console/#/realms/consent-manager/clients) of `consent-manager` under `credentials` tab, and use it for **KEYCLOAK_CLIENTSECRET** (client under *consent-manager* realm)
4. Copy the client-secret [http://localhost:9001/auth/admin/master/console/#/realms/central-registry/clients](http://localhost:9001/auth/admin/master/console/#/realms/central-registry/clients) of `ncg` under `credentials` tab, and use it for **GATEWAY_CLIENTSECRET** (client under *central-registry* realm)
5. Run through command line
    
    ```bash
    cd consent-manager
    GATEWAY_CLIENTSECRET=${GATEWAY_CLIENTSECRET} KEYCLOAK_CLIENTSECRET=${KEYCLOAK_CLIENTSECRET} ./gradlew :consent:bootRunLocal
    ```

### User-Service

1. Follow the steps from 1 to 4 in consent-manager if not already
2. Run through command line
    
    ```bash
   cd consent-manager
   GATEWAY_CLIENTSECRET=${GATEWAY_CLIENTSECRET} KEYCLOAK_CLIENTSECRET=${KEYCLOAK_CLIENTSECRET} ./gradlew :user:bootRunLocal
    ```

### Reverse-Proxy

1. Since there are two services to form a consent-manager application, we need proxy to route the request. 
2. Hence, haproxy is part of the project to route the request between consent and user service.
3. Run the following command 

    ```bash
    cd consent-manager
    ./gradlew composeUp
   ```
4. In case if you want to bring down,

    ```bash
    ./gradlew composeDown
   ```
5. In order to access consent manager, you can use http://localhost:8081

### Hip-Service

1. Clone [hip-service](https://github.com/ProjectEKA/hip-service)
2. Copy the client-secret [http://localhost:9001/auth/admin/master/console/#/realms/central-registry/clients](http://localhost:9001/auth/admin/master/console/#/realms/central-registry/clients) of `10000005` under `credentials` tab, and use it for **CLIENT_SECRET** (client under *central-registry* realm)
3. Run through command line
    ```bash
    cd hip-service
    cp src/In.ProjectEKA.DefaultHip/Resources/*.json src/In.ProjectEKA.HipService/
    export Gateway__clientSecret=${CLIENT_SECRET}
    dotnet run --project src/In.ProjectEKA.HipService/In.ProjectEKA.HipService.csproj --environment="local"
    ```

### health-information-user (HIU)

1. Clone [hiu-service](https://github.com/ProjectEKA/health-information-user)
2. Do the one time user setup, before proceeding further.

    ```bash
    docker exec -it $(docker ps -aqf "name=^postgres$") /bin/bash
    psql -U postgres health_information_user
    insert into "user" (username, password, role, verified) values ('admin', '$2a$04$WW.a3wKaiL2/7xWJc4jUmu4/55aJnwBJscZ.o18X.zLZcOdpwQGQa', 'ADMIN', true);
    insert into "user" (username, password, role, verified) values ('lakshmi', '$2a$04$WW.a3wKaiL2/7xWJc4jUmu4/55aJnwBJscZ.o18X.zLZcOdpwQGQa', 'DOCTOR', true);
    ```
3. Copy the client-secret [http://localhost:9001/auth/admin/master/console/#/realms/central-registry/clients](http://localhost:9001/auth/admin/master/console/#/realms/central-registry/clients) of `10000002` under `credentials` tab, and use it for **CLIENT_SECRET** (client under *central-registry* realm)
4. Run through command line

    ```bash
    CLIENT_SECRET=${CLIENT_SECRET} ./gradlew bootRunLocal
    ```
   
## How to 🏃🏻‍♀️ front-ends

### HIU-UI

1. Run the following command

    *Note:* This is with an assumption that hiu-service is running on port **8003**
    
    ```bash
    docker pull projecteka/hiu-ui:local # to pull latest `local` image
    docker run -d -p 5002:5000 projecteka/hiu-ui:local
    ```

2. Since hiu-service is running on different port, **same-origin-policy** will prevent you to access the UI by default.
   In order to avoid, run the following command to get an independent instance of chrome without **same-origin-policy**

    ```bash
   open -na Google\ Chrome --args --disable-web-security --user-data-dir=$HOME/profile-folder-name
   ``` 
   
3. Later versions of **Safari** allow you to Disable Cross-Origin Restrictions.
   Just enable the developer menu from Preferences >> Advanced, and select **"Disable Cross-Origin Restrictions"** from the develop menu.
   If you want **local** only, then you only need to enable the developer menu, and select **"Disable local file restrictions"** from the develop menu. 
