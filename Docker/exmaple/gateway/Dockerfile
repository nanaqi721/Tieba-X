# 第一阶段：使用 Maven 和 Java 17 构建 Gateway
FROM maven:3.9.9-eclipse-temurin-17 AS builder

# 设置镜像构建时的工作目录
WORKDIR /workspace

# 将整个多模块项目复制到构建容器
# 因为 gateway 依赖父 POM 和其他模块，不能只复制 gateway
# 第一个. 上下文
# 第二个. 工作目录
COPY . .

# 构建 gateway 及其依赖模块
# -pl gateway 表示构建 gateway
# -am 表示同时构建 gateway 依赖的项目内模块
# -DskipTests 表示构建镜像时跳过测试
RUN mvn -B -pl gateway -am package -DskipTests


# 第二阶段：只使用 Java 17 运行环境
FROM eclipse-temurin:17-jre-jammy

# 设置容器运行目录
WORKDIR /app

# 从第一阶段复制 Spring Boot 可执行 JAR
COPY --from=builder /workspace/gateway/target/gateway-*.jar /app/gateway.jar

# 声明 Gateway 在容器内监听 8080
EXPOSE 8080

# 设置 JVM 默认启动参数
ENV JAVA_OPTS="-Xms128m -Xmx384m -Dfile.encoding=UTF-8"

# 启动 Spring Boot Gateway
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/gateway.jar"]