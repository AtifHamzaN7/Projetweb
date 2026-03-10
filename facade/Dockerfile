FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the packaged Spring Boot WAR produced by Maven.
ARG JAR_FILE=target/*.war
COPY ${JAR_FILE} app.war

EXPOSE 8080

# Allow runtime tuning through JAVA_OPTS while keeping a simple default startup.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.war"]
