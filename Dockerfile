FROM openjdk:11 as backend-image
EXPOSE 8080:8080
COPY . ./home
WORKDIR ./home/endringslogg

CMD ./gradlew build
CMD ./gradlew flywayMigrate -i
CMD ./gradlew run
