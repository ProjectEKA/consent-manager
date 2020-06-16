create database keycloak;
create user keycloak with encrypted password 'password';
grant all privileges on database keycloak to keycloak;

SELECT 'CREATE DATABASE consent_manager'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'consent_manager')\gexec
SELECT 'CREATE DATABASE otpservice'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'otpservice')\gexec
SELECT 'CREATE DATABASE health_information_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'health_information_user')\gexec