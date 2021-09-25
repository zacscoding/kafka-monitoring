package demo.worker;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import com.google.common.collect.ImmutableSet;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ConsumeWorker extends AbstractWorker implements ConsumerRebalanceListener {

    private final KafkaConsumer<String, String> consumer;
    private final String topic;
    private long consumeInterval;
    private boolean shouldFail;
    private long processed;
    private LocalDateTime lastCommitTime;
    private final Set<TopicPartition> assignedTopics = new HashSet<>();

    public ConsumeWorker(String name, long intervalMills, KafkaConsumer<String, String> consumer,
                         String topic, long consumeInterval, boolean shouldFail) {
        super(checkNotNull(name, "name"), intervalMills);
        this.consumer = checkNotNull(consumer, "consumer");
        this.topic = checkNotNull(topic, "topic");
        this.consumeInterval = consumeInterval;
        this.shouldFail = shouldFail;
        this.lastCommitTime = LocalDateTime.now().minusNanos(TimeUnit.MILLISECONDS.toNanos(consumeInterval));
    }

    protected void workInternal() {
        try {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100L));
            for (ConsumerRecord<String, String> record : records) {
                processed++;
                if (processed % 100 == 0) {
                    logger.info("[{}} Consume record. message: {} / topic: {} / partition: {} / offset: {}",
                                name,
                                record.value(),
                                record.topic(),
                                record.partition(),
                                record.offset());
                }
                if (shouldFail) {
                    continue;
                }
                if (consumeInterval != 0L) {
                    // Wait for commit interval
                    final LocalDateTime now = LocalDateTime.now();
                    final LocalDateTime nextCommitTime = lastCommitTime.plus(consumeInterval,
                                                                             ChronoField.MILLI_OF_DAY.getBaseUnit());
                    if (now.isBefore(nextCommitTime)) {
                        TimeUnit.NANOSECONDS.sleep(Duration.between(now, nextCommitTime).toNanos());
                    }
                }
                // commit
                consumer.commitSync();
                lastCommitTime = LocalDateTime.now();
            }
        } catch (Exception e) {
            logger.error("Exception occur while consuming records", e);
        }
    }

    @Override
    protected void onStop() {
        consumer.close();
    }

    public void update(Long consumeInterval, Boolean shouldFail) {
        if (consumeInterval != null) {
            this.consumeInterval = consumeInterval;
        }
        if (shouldFail != null) {
            this.shouldFail = shouldFail;
        }
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        synchronized (assignedTopics) {
            assignedTopics.clear();
        }
        logger.info("[{}] partitions revoked: {}", name,
                    partitions.stream()
                              .map(p -> String.format("%s-%d", p.topic(), p.partition()))
                              .collect(Collectors.joining(",")));
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        synchronized (assignedTopics) {
            assignedTopics.addAll(partitions);
        }
        logger.info("[{}] partitions assigned: {}", name,
                    partitions.stream()
                              .map(p -> String.format("%s-%d", p.topic(), p.partition()))
                              .collect(Collectors.joining(",")));
    }

    public Set<TopicPartition> getAssignedTopics() {
        return ImmutableSet.copyOf(assignedTopics);
    }
}
