#build the JAR phase
FROM maven:3.5-jdk-8-alpine AS build
#Copies source code and pom.xml to /usr/src/cashcard, then builds the JAR.
COPY src /usr/src/cashcard/src
COPY pom.xml /usr/src/cashcard
RUN mvn -f /usr/src/cashcard/pom.xml clean package

#run the JAR phase
FROM openjdk:8-jre-alpine
COPY --from=build /usr/src/cashcard/target/cashcard-0.0.1-SNAPSHOT.jar /usr/cashcard/cashcard-0.0.1-SNAPSHOT.jar

#container set-up phase
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/cashcard/cashcard-0.0.1-SNAPSHOT.jar"]



