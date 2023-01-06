FROM azul/zulu-openjdk-debian:11
#RUN addgroup -S spring && adduser -S spring -G spring
RUN groupadd spring && useradd -g spring spring
RUN apt update && apt install -y curl \
    && curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/master/contrib/install.sh | sh -s -- -b /usr/local/bin \
    && trivy filesystem --severity HIGH,CRITICAL --exit-code 1 --no-progress /
USER spring:spring
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
