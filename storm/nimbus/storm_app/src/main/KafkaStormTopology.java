package main;

import static org.apache.storm.kafka.spout.FirstPollOffsetStrategy.EARLIEST;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.storm.tuple.Values;
import org.apache.storm.tuple.Fields;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.kafka.spout.ByTopicRecordTranslator;
import org.apache.storm.kafka.spout.KafkaSpoutRetryService;
import org.apache.storm.kafka.spout.KafkaSpoutRetryExponentialBackoff;
import org.apache.storm.kafka.spout.KafkaSpoutRetryExponentialBackoff.TimeInterval;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.ConfigurableTopology;
import main.bolt.ConsumeBolt;

public class KafkaStormTopology extends ConfigurableTopology {

  private static final String KAFKA_BROKER = "data-producer:9092";
  public static final String TOPIC = "lrb";

  public static void main(String[] args) throws Exception {
    ConfigurableTopology.start(new KafkaStormTopology(), args);
  }

  @Override
  protected int run(String[] args) throws Exception {
    final String brokerUrl = KAFKA_BROKER;
    System.out.println("Running with broker url: " + brokerUrl);

    TopologyBuilder builder = new TopologyBuilder();

    builder.setSpout("kafka-spout", new KafkaSpout<>(getKafkaSpoutConfig(brokerUrl)), 1);
    // builder.setSpout("kafka-spout", new KafkaSpout(), 5) to set parallelism to 5

    builder.setBolt("consume", new ConsumeBolt()).shuffleGrouping("kafka-spout");
    // builder.setBolt("consume", new ConsumeBolt(), 5) to set parallelism to 5;

    conf.setDebug(true);

    String topologyName = "kafka-storm";

    // conf.setNumWorkers(3);

    return submit(topologyName, conf, builder);
  }

  protected KafkaSpoutConfig<String, String> getKafkaSpoutConfig(String bootstrapServers) {
    ByTopicRecordTranslator<String, String> trans = new ByTopicRecordTranslator<>(
        (r) -> new Values(r.value()),
        new Fields("value"));

    return KafkaSpoutConfig.builder(bootstrapServers, TOPIC)
        .setProp(ConsumerConfig.GROUP_ID_CONFIG, "kafkaSpoutTestGroup")
        .setProp(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100000") // set max number of records that a consumer will
                                                                   // fetch
        // in a single poll request to 1000
        .setProp(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "1000") // set max amount of time that a consumer will wait
                                                                     // for new data before issuing another poll request
                                                                     // to 1 sec
        .setRetry(getRetryService())
        .setRecordTranslator(trans)
        // .setOffsetCommitPeriodMs(10_000)
        .setOffsetCommitPeriodMs(1000) // poll records every second
        .setFirstPollOffsetStrategy(EARLIEST)
        // .setMaxUncommittedOffsets(250)
        .setMaxUncommittedOffsets(100_000) // max 100000 records can be pending commit before another poll can take
                                           // place
        .build();
  }

  protected KafkaSpoutRetryService getRetryService() {
    return new KafkaSpoutRetryExponentialBackoff(TimeInterval.microSeconds(500),
        TimeInterval.milliSeconds(2), Integer.MAX_VALUE, TimeInterval.seconds(10));
  }
}