# 使用多阶段构建减小镜像体积
# 第一阶段：构建
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# 1. 复制 Maven Wrapper 配置（含阿里云地址）
COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/
COPY .mvn/ .mvn
COPY mvnw ./
RUN chmod +x mvnw

# 2. 复制阿里云镜像配置
COPY .m2/settings.xml /app/.m2/settings.xml

# 3. 分阶段下载依赖
COPY pom.xml .
RUN ./mvnw -s /app/.m2/settings.xml dependency:go-offline

# 4. 编译打包
COPY src ./src
RUN ./mvnw -s /app/.m2/settings.xml package -DskipTests

# 第二阶段：运行
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# 安全建议：使用非 root 用户运行
RUN useradd -m myuser
USER myuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]