# 多阶段构建：减小最终镜像体积（面试可讲：镜像分层 + 构建缓存）
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
# 先只拷 pom，利用 Docker 层缓存：依赖不变时不重复下载
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /app/target/heartbeat-backend-1.0.0.jar app.jar
EXPOSE 8090
ENV SPRING_PROFILES_ACTIVE=mysql
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
