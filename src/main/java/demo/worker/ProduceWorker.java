package demo.worker;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProduceWorker extends AbstractWorker {

    private final KafkaProducer<String, String> producer;
    private final String topic;
    private int processed;

    public ProduceWorker(String name, long intervalMills, KafkaProducer<String, String> producer, String topic) {
        super(name, intervalMills);
        this.producer = checkNotNull(producer, "producer");
        this.topic = checkNotNull(topic, "topic");
    }

    @Override
    protected void workInternal() {
        try {
            final ProducerRecord<String, String> data = new ProducerRecord<>(topic,
                                                                             String.format("Message-%d", processed));
            final RecordMetadata metadata = producer.send(data).get();
            processed++;
            if (processed % 100 == 0) {
                logger.debug("Success to produce record. topic: {} / partition: {} / offset: {}",
                             metadata.topic(),
                             metadata.partition(),
                             metadata.offset());
            }
        } catch (Exception e) {
            logger.error("Exception occur while producing records", e);
        }
    }

    @Override
    protected void onStop() {
        producer.close();
    }
}
