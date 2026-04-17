# 先在模块目录执行: mvn -q package -DskipTests
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
