create database keycloak;
create user keycloak with encrypted password 'password';
grant all privileges on database keycloak to keycloak;

create database consent_manager;
create database otpservice;