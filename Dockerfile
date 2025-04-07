FROM gcr.io/distroless/java21

COPY ./build/libs/*all.jar "app.jar"
ENV TZ="Europe/Oslo"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
CMD ["app.jar"]