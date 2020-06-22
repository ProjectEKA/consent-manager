# :convenience_store: Consent Manager

> A Health Data Consent Manager (HDCM) is a new type of entity proposed here whose task is to provide
> health information aggregation services to customers of health care services. It enables customers to
> fetch their health information from one or more Health Information Providers (e.g., Hospitals,
> Diagnostic Labs, Medical Device Companies), based on their explicit Consent and to share such
> aggregated information with Health Information Users i.e. entities in need of such data (e.g., Insurers,
> Doctors, Medical Researchers).

## Components

![components](docs/diagrams/ProjectEKA-Simplified-Arch.jpg)

## System Architecture

![Architecture](docs/diagrams/architecture.png)

## Dev setup for whole consent eco system

* [Dev setup](docs/dev-setup.md)


## Consent Manager Architecture

* [System Architecture](./docs/architecture.md)

# Pre-requisites & Setting up the environment

 * Git (also setup SSH Key)
 * Intelli Idea IDE
   * Install Lombok Plugin in the IDE and enable Lombok Annotations in preferences
 
## Import the project

When using IntelliJ Idea, open this project by importing the gradle file and check `Auto Import` option to resolve 
all the dependencies automatically.

## :muscle: Motivation

> Consent Manager must provide its customers an interface using which they can view
> and manage consent artefacts associated with them and, optionally, an interface for
> the customers to view their aggregated health information.

## Build Status

[![ci/cd](https://github.com/ProjectEKA/hdaf/workflows/GitHub%20Actions/badge.svg)](https://github.com/ProjectEKA/hdaf/actions)

## :+1: Code Style

[JAVA Naming Conventions](https://google.github.io/styleguide/javaguide.html)

## :tada: Language/Frameworks

*   [JAVA](https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/)
*   [spring webflux](https://docs.microsoft.com/en-us/aspnet/core/?view=aspnetcore-3.1)
*   [Easy Random](https://github.com/j-easy/easy-random)
*   [Vavr](https://www.vavr.io/vavr-docs/)
*   [gradle](https://docs.gradle.org/5.6.4/userguide/userguide.html)

## :checkered_flag: Requirements

*   [docker >= 19.03.5](https://www.docker.com/)
*   [graphviz](https://graphviz.gitlab.io/download/)

## Setting up local machine
1. Consent Manager needs a bunch of backends like RabbitMQ, Keycloak, Postgres.  These backends also need some initialization, 
e.g. 2 Realms (consent-manager & central-registry) & a user *consent-service-admin-user* need to be created for user authentication in Keycloak.  Also, relevant databases & schemas need to be created in postgresql.
Run the following docker-compose command for initializing the backend

```alpha
docker-compose -f docker-compose-backend.yml up
```  
Leave the terminal window for these backends to be running.

Start and stop the backend using the following commands
```alpha
#Stop backends
docker-compose -f docker-compose-backend.yml stop
#Start backends
docker-compose -f docker-compose-backend.yml start
```  


If you want to remove all the backends for good

```alpha
docker-compose -f docker-compose-backend.yml down
docker volume rm consent-manager_postgres_data
```

2. With the above setup, you'll have all the required database created.  Now run the database migrations to create the required schema for consent-manager app.


```alpha
git clone https://github.com/ProjectEKA/cm-db-initializer
mvn clean install
java -Djdbc.url=jdbc:postgresql://localhost:5432/consent_manager -Djdbc.username=postgres -Djdbc.password=password -jar target/cmdb-initializer-1.0-SNAPSHOT.jar
docker volume rm consent-manager_postgres_data
```
  
3. After you Keycloak setup, you'll need to keep track of the Secret associated to consent-manager 
* Navigate to ConsentManager Realm
* Go to "Clients" > "Credentials"
* Get the Secret field
* Update the secret in [docker-compose.yml](docker-compose.yml) under variables CONSENTMANAGER_CLIENTREGISTRY_XAUTHTOKEN & CONSENTMANAGER_KEYCLOAK_CLIENTSECRET
CONSENTMANAGER_CLIENTREGISTRY_XAUTHTOKEN => this one needs to be copied from central-registry realm
CONSENTMANAGER_KEYCLOAK_CLIENTSECRET => this one needs to be copied from ConsentManager realm 

4. Now run the rest of the containers with the following command

```alpha
docker-compose -f docker-compose.yml up --build
```  
This command will run consent-manager, otp & client-registry service.   

You can choose to comment out the consent-manager part in the docker-compose.yml file and run it from your favorite IDE.

## :whale: Running From The Docker Image

Create docker image

```alpha
docker build -t consent-manager .
```

To run the image

```alpha
docker run -d -p 8000:8000 consent-manager
```

## :rocket: Running From Source

To run

```alpha
./gradlew bootRun
```

or if you want to run in dev environment setup

```alpha
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Running The Tests

To run the tests
```alpha
./grdlew test
```

## Features

1.  Consent Management
2.  Aggregate Health Information

## API Contract

Once ran the application, navigate to

```alpha
{HOST}/index.html
```

## Commands to Know

Generates PNGs for all `*.puml` files located in `docs/diagrams` and `<project>/docs/diagrams`.

 ```alpha
 make png
```
