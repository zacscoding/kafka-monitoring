# :eyes: Kafka monitoring example with Spring Boot Application, Burrow, CMAK
; TBD after add CMAK

---  

```shell
// build spring boot demo application
$ ./tools/scripts/compose.sh burrow build

// run all components
$ ./tools/scripts/compose.sh burrow up
$ docker ps -a
CONTAINER ID   IMAGE                           COMMAND                  CREATED         STATUS         PORTS                                                           NAMES
6ad2a2305512   generalmills/burrowui           "node server"            9 minutes ago   Up 9 minutes   0.0.0.0:3100->3000/tcp, :::3100->3000/tcp                       burrow-ui
1840cf41be3c   joway/burrow-dashboard:latest   "./start.sh"             9 minutes ago   Up 9 minutes   0.0.0.0:3000->80/tcp, :::3000->80/tcp                           burrow-dashboard
7c0f0db7b438   burrow_burrow                   "/app/burrow --confi…"   9 minutes ago   Up 8 minutes   0.0.0.0:8000->8000/tcp, :::8000->8000/tcp                       burrow1
9dc263a23ea9   burrow_burrow                   "/app/burrow --confi…"   9 minutes ago   Up 9 minutes   0.0.0.0:8100->8000/tcp, :::8100->8000/tcp                       burrow2
f0d186d5a02c   kafka-monitoring/demo-app       "java org.springfram…"   9 minutes ago   Up 9 minutes   0.0.0.0:8080->8080/tcp, :::8080->8080/tcp                       demo-app
71e4e24c32b7   obsidiandynamics/kafdrop        "/kafdrop.sh"            9 minutes ago   Up 9 minutes   0.0.0.0:9000->9000/tcp, :::9000->9000/tcp                       kafdrop
032cb6641934   confluentinc/cp-kafka:5.2.1     "/etc/confluent/dock…"   9 minutes ago   Up 9 minutes   9092/tcp, 0.0.0.0:9094->9094/tcp, :::9094->9094/tcp             kafka3
fdb48acc60ab   confluentinc/cp-kafka:5.2.1     "/etc/confluent/dock…"   9 minutes ago   Up 9 minutes   9092/tcp, 0.0.0.0:9093->9093/tcp, :::9093->9093/tcp             kafka2
7c4ddfbd018d   confluentinc/cp-kafka:5.2.1     "/etc/confluent/dock…"   9 minutes ago   Up 9 minutes   0.0.0.0:9092->9092/tcp, :::9092->9092/tcp                       kafka1
93f63ae304af   zookeeper:3.4.9                 "/docker-entrypoint.…"   9 minutes ago   Up 9 minutes   2888/tcp, 0.0.0.0:2181->2181/tcp, :::2181->2181/tcp, 3888/tcp   zoo1
```  

- UI
  - **Kafdrop**: http://localhost:9000/
  - **Burrow-UI**: http://localhost:3100/#/
  - **Burrow-Dashboard**: http://localhost:3000/#/ 
- (intellij) http
  - [tools/http/burrow.http](./tools/http/burrow.http): tests burrow apis
  - [tools/http/test.http](./tools/http/test.http): tests consumer lags(start producer, consumer and then lag will be increased)  
