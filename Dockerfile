FROM ghcr.io/navikt/poao-baseimages/java:11
COPY ./build/libs/*all.jar "app.jar"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
