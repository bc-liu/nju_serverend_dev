# 修改1：使用国内代理拉取 Maven（带 JDK 8）
FROM docker.1ms.run/library/maven:3.8.5-openjdk-8 AS build

WORKDIR /app

# 配置阿里云 Maven 镜像加速（解决依赖下载慢）
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?> \
    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \
    https://maven.apache.org/xsd/settings-1.0.0.xsd"> \
    <mirrors> \
    <mirror> \
    <id>aliyunmaven</id> \
    <mirrorOf>central</mirrorOf> \
    <name>阿里云公共仓库</name> \
    <url>https://maven.aliyun.com/repository/public</url> \
    </mirror> \
    </mirrors> \
    </settings>' > /root/.m2/settings.xml

COPY pom.xml ./
COPY src ./src

RUN mvn clean package -DskipTests

# 修改2：使用国内代理拉取运行时 JDK（替代已废弃的 openjdk）
FROM docker.1ms.run/library/eclipse-temurin:8-jdk-alpine

WORKDIR /app

COPY --from=build /app/target/TomatoMall-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]