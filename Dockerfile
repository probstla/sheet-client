FROM adoptopenjdk/openjdk11:jdk-11.0.10_9-ubuntu-slim
#RUN addgroup -S spring && adduser -S spring -G spring
RUN apt update && apt install -y curl \
    && curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/master/contrib/install.sh | sh -s -- -b /usr/local/bin \
    && trivy filesystem --severity HIGH,CRITICAL --exit-code 1 --no-progress /
RUN groupadd spring && useradd -g spring spring
USER spring:spring
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]