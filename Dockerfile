FROM ubuntu:22.04

LABEL maintainer="yss-meta-team"
LABEL description="Metadata Platform Bootstrap Service"

WORKDIR /app

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-8-jre-headless \
    curl \
    tzdata \
    ca-certificates \
    && ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime && echo "${TZ}" > /etc/timezone \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 复制已打包好的 bootstrap jar
COPY metadata-platform-bootstrap/target/metadata-platform-bootstrap-*.jar /app/app.jar

# 环境变量默认值
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
ENV DB_USERNAME=root
ENV DB_PASSWORD=root

EXPOSE ${SERVER_PORT}

# 容器健康检查
HEALTHCHECK --interval=15s --timeout=5s --start-period=35s --retries=3 \
  CMD curl -fsS http://localhost:${SERVER_PORT}/actuator/health || exit 1

# 启动入口
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
