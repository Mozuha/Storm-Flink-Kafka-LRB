package main.java;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;

/*
 * Read the file and output each read line as a String value to certain topic.
 * 
 * Adjust event rate by batch.size and linger.ms properties of Kafka producer and elapsedTime.
 * We want:
 *   - 1000 events/s
 *   - 100000 events/s
 * 
 * Event rates are referred to record-send-rate obtained by Kafka producer metrics.
 * It is difficult to achieve exact event rate, so the const values below are to achieve event rates around the target rates.
 */

public class Producer {
  private static final Logger log = LoggerFactory.getLogger(Producer.class);

  // 1000 evt/s
  private static final Integer THOUS_EVT_BATCH_SIZE = 3500;
  private static final Integer THOUS_EVT_ELAPSED_TIME = 1200;

  // 100000 evt/s
  private static final Integer HUND_THOUS_EVT_BATCH_SIZE = 540000;
  private static final Integer HUND_THOUS_ELAPSED_TIME = 4000;

  public static void main(String[] args) throws Exception {

    Integer BATCH_SIZE = 16384;
    Integer ELAPSED_TIME = 1000;
    Boolean IMM_CLOSE = false;

    switch (args[0]) {
      case "1000":
        BATCH_SIZE = THOUS_EVT_BATCH_SIZE;
        ELAPSED_TIME = THOUS_EVT_ELAPSED_TIME;
        IMM_CLOSE = true;
        break;
      case "100000":
        BATCH_SIZE = HUND_THOUS_EVT_BATCH_SIZE;
        ELAPSED_TIME = HUND_THOUS_ELAPSED_TIME;
        break;
    }

    // Set up the Kafka Producer properties
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("batch.size", BATCH_SIZE); // upper bound of the batch size
    props.put("linger.ms", 1000); // limit time to wait until a batch is fully filled

    KafkaProducer<String, String> producer = new KafkaProducer<>(props);

    BufferedReader br = new BufferedReader(new FileReader("/usr/local/lrb/kafka_connect/datafile3hours.dat"));
    String line;
    long startTime = System.currentTimeMillis();
    long elapsedTime = 0;

    while ((line = br.readLine()) != null) {
      elapsedTime = System.currentTimeMillis() - startTime;

      ProducerRecord<String, String> record = new ProducerRecord<>("lrb", line);
      producer.send(record);
      if (ELAPSED_TIME <= elapsedTime)
        break;
    }

    br.close();

    // Clean up the Kafka Producer
    if (IMM_CLOSE)
      producer.close(Duration.ZERO);
    else
      producer.close();

    producer.metrics().entrySet().forEach(entry -> {
      if (entry.getKey().name() == "record-size-avg" || entry.getKey().name() == "record-send-rate"
          || entry.getKey().name() == "record-send-total")
        log.info(entry.getKey() + ": " + entry.getValue().metricValue());
    });
  }
}
