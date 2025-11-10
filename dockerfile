FROM amazoncorretto:17-alpine
EXPOSE 5000
ADD target/bootdocker.jar bootdocker.jar
ENTRYPOINT ["java","-jar","bootdocker.jar"]
