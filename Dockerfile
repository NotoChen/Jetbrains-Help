FROM java:21

ADD target/Jetbrains-Help.jar /Jetbrains-Help.jar

RUN bash -c 'touch /Jetbrains-Help.jar'

ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/{TZ} /etc/localtime && echo "{TZ}" > /etc/timezone


EXPOSE 10768

ENTRYPOINT ["java", "-jar","/Jetbrains-Help.jar"]