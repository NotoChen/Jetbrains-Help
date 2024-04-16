FROM maven:3-ibm-semeru-21-jammy as build
WORKDIR /app
COPY . .
RUN mvn clean package

FROM ibm-semeru-runtimes:open-21-jre
WORKDIR /app
COPY --from=build /app/target/Jetbrains-Help.jar Jetbrains-Help.jar
ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/{TZ} /etc/localtime && echo "{TZ}" > /etc/timezone
EXPOSE 10768
ENTRYPOINT ["java", "-jar", "Jetbrains-Help.jar"]