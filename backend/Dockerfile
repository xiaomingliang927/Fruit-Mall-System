# ---- 构建阶段：Maven + JDK 17（利用 layer 缓存加速依赖下载） ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY settings-aliyun.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# ---- 运行阶段：仅 JRE，镜像精简 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
