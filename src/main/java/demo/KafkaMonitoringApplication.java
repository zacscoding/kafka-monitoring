package demo;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

@SpringBootApplication
public class KafkaMonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaMonitoringApplication.class, args);
    }

    @Bean
    public NewTopic defaultTopic() {
        return TopicBuilder.name("topic1")
                           .partitions(3)
                           .replicas(3)
                           .build();
    }
}
