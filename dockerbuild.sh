## chmod u+x *.sh
# please run me ↑
mvn clean package

docker build -t jenkins-help .

docker run -d -p 10768:10768 --name jetbrains-help jenkins-help
