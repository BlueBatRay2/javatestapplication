# First Stage
FROM gradle:8.1.1-jdk17 as build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
#RUN gradle build --no-daemon
RUN gradle build

# Second Stage
FROM openjdk:17

# Install vim todo remove after testing
#RUN apt-get update && apt-get install -y vim

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/testApplication.jar /app/testApplication.jar

ENTRYPOINT ["java","-jar","/app/testApplication.jar"]
