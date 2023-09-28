FROM gcr.io/distroless/java17
COPY target/organizationmanager*.jar app.jar
ENV APPLICATIONINSIGHTS_CONFIGURATION_FILE /etc/application/applicationinsights.json
COPY applicationinsights-agent.jar applicationinsights-agent.jar
ENTRYPOINT ["java","-javaagent:/applicationinsights-agent.jar", "-jar","/app.jar","-Xmx=512M"]
