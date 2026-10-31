FROM azul/zulu-openjdk:23.0.2-jdk

WORKDIR /app

COPY --chown=10001:10001 --chmod=0444 target/*.jar app.jar

EXPOSE 8081

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]