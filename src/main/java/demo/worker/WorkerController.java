package demo.worker;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkerController {

    private final KafkaProperties kafkaProperties;
    private final Map<String, AbstractWorker> workers = new ConcurrentHashMap<>();

    @PreDestroy
    public void tearDown() {
        logger.info("Try to shutdown workers #{}", workers.size());
        for (Entry<String, AbstractWorker> entry : workers.entrySet()) {
            entry.getValue().stop();
        }
    }

    /**
     * Handle "POST /api/worker/producer/{name}/{topic}?interval=500" to create a new producer
     */
    @PostMapping("/producer/{name}/{topic}")
    public ResponseEntity<CommandResult> startProducer(@PathVariable("name") String name,
                                                       @PathVariable("topic") String topic,
                                                       @RequestParam("interval") long interval) {

        logger.info("Try to start a new producer. name: {} / topic: {} / interval: {}[ms]", name, topic, interval);

        final String id = convertId(name, topic);
        final KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProps());
        final ProduceWorker worker = new ProduceWorker(name, interval, producer, topic);

        if (workers.putIfAbsent(id, worker) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        worker.start();

        return ResponseEntity.ok(CommandResult.builder()
                                              .status("created")
                                              .name(id)
                                              .topic(topic)
                                              .metadata(ImmutableMap.<String, Object>builder()
                                                                    .put("interval", interval)
                                                                    .build())
                                              .build());
    }

    /**
     * Handle "DELETE /api/worker/producer/{name}/{topic}" to terminate the producer.
     */
    @DeleteMapping("/producer/{name}/{topic}")
    public ResponseEntity<CommandResult> stopProducer(@PathVariable("name") String name,
                                                      @PathVariable("topic") String topic) {
        logger.info("Try to stop a producer. name: {} / topic: {}", name, topic);

        final String id = convertId(name, topic);
        final AbstractWorker worker = workers.get(id);
        if (!(worker instanceof ProduceWorker)) {
            return ResponseEntity.notFound().build();
        }

        workers.remove(id);
        worker.stop();

        return ResponseEntity.ok(CommandResult.builder()
                                              .status("deleted")
                                              .name(id)
                                              .topic(topic)
                                              .build());
    }

    /**
     * Handle "POST /api/worker/consumer/{name}/{topic}" to start a new consumer.
     */
    @PostMapping("/consumer/{name}/{topic}")
    public ResponseEntity<CommandResult> startConsumer(@PathVariable("name") String name,
                                                       @PathVariable("topic") String topic,
                                                       @RequestParam("groupId") String groupId,
                                                       @RequestParam(value = "interval", required = false)
                                                               long interval,
                                                       @RequestParam(value = "shouldFail", required = false,
                                                               defaultValue = "true") boolean shouldFail) {
        logger.info("Try to start a new consumer. name: {} / topic: {} / groupId: {} / "
                    + "shouldFail: {} / commitInterval: {}[ms]",
                    name, topic, groupId, shouldFail, interval);

        final String id = convertId(name, topic);
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerProps(groupId));

        final ConsumeWorker worker = new ConsumeWorker(name, 0, consumer, topic, interval, shouldFail);
        if (workers.putIfAbsent(id, worker) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        consumer.subscribe(Collections.singletonList(topic), worker);
        worker.start();

        return ResponseEntity.ok(CommandResult.builder()
                                              .status("created")
                                              .name(name)
                                              .topic(topic)
                                              .metadata(ImmutableMap.<String, Object>builder()
                                                                    .put("groupId", groupId)
                                                                    .put("shouldFail", shouldFail)
                                                                    .put("interval", interval)
                                                                    .build()
                                              ).build());
    }

    /**
     * Handle "PUT /api/worker/consumer/{name}/{topic}" to update consumer's commit state.
     */
    @PutMapping("/consumer/{name}/{topic}")
    public ResponseEntity<CommandResult> updateConsumer(@PathVariable("name") String name,
                                                        @PathVariable("topic") String topic,
                                                        @RequestParam("groupId") String groupId,
                                                        @RequestParam(value = "interval", required = false)
                                                                Long interval,
                                                        @RequestParam(value = "shouldFail", required = false)
                                                                Boolean shouldFail) {
        logger.info("Try to update a consumer. name: {} / topic: {} / groupId: {} / "
                    + "shouldFail: {} / commitInterval: {}[ms]",
                    name, topic, groupId, shouldFail, interval);

        final String id = convertId(name, topic);
        final AbstractWorker worker = workers.get(id);

        if (!(worker instanceof ConsumeWorker)) {
            return ResponseEntity.notFound().build();
        }

        final ConsumeWorker consumeWorker = (ConsumeWorker) worker;
        consumeWorker.update(interval, shouldFail);

        return ResponseEntity.ok(CommandResult.builder()
                                              .status("updated")
                                              .name(name)
                                              .topic(topic)
                                              .metadata(ImmutableMap.<String, Object>builder()
                                                                    .put("groupId", groupId)
                                                                    .put("shouldFail", consumeWorker.isShouldFail())
                                                                    .put("interval", consumeWorker.getConsumeInterval())
                                                                    .build()
                                              ).build());
    }

    /**
     * Handle "DELETE /api/worker/consumer/{name}/{topic}" to terminate the consumer.
     */
    @DeleteMapping("/consumer/{name}/{topic}")
    public ResponseEntity<CommandResult> stopConsumer(@PathVariable("name") String name,
                                                      @PathVariable("topic") String topic) {
        logger.info("Try to stop a consumer. name: {} / topic: {}", name, topic);

        final String id = convertId(name, topic);
        final AbstractWorker worker = workers.get(id);
        if (!(worker instanceof ConsumeWorker)) {
            return ResponseEntity.notFound().build();
        }

        workers.remove(id);
        worker.stop();

        return ResponseEntity.ok(CommandResult.builder()
                                              .status("deleted")
                                              .name(id)
                                              .topic(topic)
                                              .build());
    }

    @GetMapping("/consumer/leader")
    public ResponseEntity<Map<String, TopicSummary>> getPartitionAssigned() {
        final Stream<ConsumeWorker> consumers = workers.values()
                                                       .stream()
                                                       .filter(w -> w instanceof ConsumeWorker)
                                                       .map(w -> (ConsumeWorker) w);

        final Map<String, TopicSummary> topics = new HashMap<>();

        consumers.forEach(c -> {
            final String topic = c.getTopic();
            final TopicSummary summary = topics.computeIfAbsent(topic, k -> new TopicSummary(topic, new ArrayList<>()));
            summary.getPartitions().addAll(
                    c.getAssignedTopics()
                     .stream()
                     .map(p -> PartitionSummary.builder()
                                               .partition(p.partition())
                                               .consumerName(c.getName())
                                               .build())
                     .collect(Collectors.toList())
            );
        });

        return ResponseEntity.ok(topics);
    }

    /**
     * See https://docs.confluent.io/platform/current/installation/configuration/producer-configs.html
     */
    private Properties createProducerProps() {
        Properties producerProps = new Properties();

        producerProps.put("bootstrap.servers", String.join(",", kafkaProperties.getBootstrapServers()));
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        return producerProps;
    }

    /**
     * See https://docs.confluent.io/platform/current/installation/configuration/consumer-configs.html
     */
    private Properties createConsumerProps(final String groupId) {
        checkNotNull(groupId, "groupId");
        Properties consumerProps = new Properties();

        consumerProps.put("bootstrap.servers", String.join(",", kafkaProperties.getBootstrapServers()));
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("group.id", groupId);
        consumerProps.put("auto.offset.reset", "latest");
        consumerProps.put("enable.auto.commit", false);

        return consumerProps;
    }

    private String convertId(String name, String topic) {
        return String.format("%s_%s", name, topic);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class CommandResult {
        private String status;
        private String name;
        private String topic;
        private Map<String, Object> metadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class TopicSummary {
        private String topic;
        private List<PartitionSummary> partitions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class PartitionSummary {
        private int partition;
        private String consumerName;
    }
}
