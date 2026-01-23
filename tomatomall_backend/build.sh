#!/bin/bash

# 构建后端项目
mvn clean package -DskipTests

echo "构建完成，生成的jar文件位于 target/TomatoMall-0.0.1-SNAPSHOT.jar"
