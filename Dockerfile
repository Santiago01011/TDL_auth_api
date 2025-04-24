FROM eclipse-temurin:21.0.7_6-jdk

EXPOSE 8080

WORKDIR /root

COPY ./pom.xml /root
COPY ./.mvn /root/.mvn
COPY ./mvnw /root

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY ./src /root/src
RUN ./mvnw clean package -DskipTests

ENTRYPOINT ["java","-jar","/root/target/auth-0.0.1-SNAPSHOT.jar"]