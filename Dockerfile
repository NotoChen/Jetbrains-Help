FROM ibm-semeru-runtimes:open-21-jdk as build
ENV MAVEN_VENSION=3.9.6
RUN apt-get update && apt-get install -y curl
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VENSION}/binaries/apache-maven-${MAVEN_VENSION}-bin.tar.gz -o /tmp/apache-maven-${MAVEN_VENSION}-bin.tar.gz && \
    tar -xzf /tmp/apache-maven-${MAVEN_VENSION}-bin.tar.gz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VENSION}/bin/mvn /usr/local/bin/mvn
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