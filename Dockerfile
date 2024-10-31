FROM amazoncorretto:22 as build

MAINTAINER mramz55555@gmail.com

COPY target/gateway-server-0.0.1-SNAPSHOT.jar gateway-server-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java" ,"-jar", "gateway-server-0.0.1-SNAPSHOT.jar"]