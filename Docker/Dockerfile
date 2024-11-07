# Step 1: Build the JAR using Maven
FROM maven:3.8.5-openjdk-17 AS build
#Copies source code and pom.xml to /usr/src/cashcard, then builds the JAR.
COPY ../cashcard/src /usr/src/cashcard/src
COPY ../cashcard/pom.xml /usr/src/cashcard
RUN mvn -f /usr/src/cashcard/pom.xml clean package

# Run phase
FROM openjdk:17
COPY --from=build /usr/src/cashcard/target/cashcard-0.0.1-SNAPSHOT.jar /usr/cashcard/target/cashcard-0.0.1-SNAPSHOT.jar

#container set-up phase
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/cashcard/target/cashcard-0.0.1-SNAPSHOT.jar"]



