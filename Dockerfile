FROM eclipse-temurin:17-alpine
MAINTAINER Josef (kwart) Cacek <josef.cacek@gmail.com>

RUN mkdir /mvnindex

COPY target/mvnquery.jar /mvnquery.jar
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/mvnquery.jar", "--config-data-dir", "/data"]
VOLUME /data
CMD ["--help" ]
