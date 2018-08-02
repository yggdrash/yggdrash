# For docker hub automated build only

FROM openjdk:8 as builder
ADD . /yggdrash/
RUN \
    cd /yggdrash && \
    ./gradlew clean build -x test && \
    mv /yggdrash/yggdrash-node/build/libs/*.jar /app.jar

FROM openjdk:8-jre-alpine
MAINTAINER YGGDRASH
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    RUN_SLEEP=0 \
    JAVA_OPTS=""
EXPOSE 8080 9090
VOLUME /tmp
CMD echo "The Yggdrash Node will start in ${RUN_SLEEP}s..." && \
    sleep ${RUN_SLEEP} && \
    java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.jar

COPY --from=builder /app.jar .
