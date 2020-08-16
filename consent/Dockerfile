FROM openjdk:12-jdk-alpine
VOLUME /tmp
COPY build/libs/* app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app.jar"]