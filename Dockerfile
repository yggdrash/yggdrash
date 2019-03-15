# For docker hub automated build only

FROM openjdk:8 as builder
ADD . /yggdrash/
RUN apt update && apt install -y libatomic1
RUN \
    cd /yggdrash && \
    ./gradlew clean build -x test && \
    mv /yggdrash/yggdrash-node/build/libs/*node.jar /app.jar

FROM openjdk:8-jre-alpine
MAINTAINER YGGDRASH
ENV SPRING_PROFILES_ACTIVE=prod \
    RUN_SLEEP=0 \
    JAVA_OPTS=""
EXPOSE 8080 32918
VOLUME /.yggdrash
CMD echo "The Yggdrash Node will start in ${RUN_SLEEP}s..." && \
    sleep ${RUN_SLEEP} && \
    java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.jar

COPY --from=builder /app.jar .
