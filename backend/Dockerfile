# 多阶段构建：Maven 在容器内完成编译，宿主机无需安装 JDK/Maven
FROM maven:3-eclipse-temurin-17 AS build

WORKDIR /build

# 先拷贝 pom 单独解析依赖，充分利用 Docker 层缓存（源码变更不重复下载依赖）
COPY backend/pom.xml ./pom.xml
RUN mvn -q -B dependency:go-offline

COPY backend/src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /build/target/smart-parking-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
