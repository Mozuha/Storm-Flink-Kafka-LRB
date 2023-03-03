package main;

import static org.apache.storm.kafka.spout.FirstPollOffsetStrategy.EARLIEST;
import static org.apache.storm.topology.base.BaseWindowedBolt.Duration;

import java.util.concurrent.TimeUnit;

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
import main.bolt.FilterBolt;
import main.bolt.LAVBolt;
import main.bolt.TollNotifyBolt;

public class TollNotifTopology extends ConfigurableTopology {

  private static final String KAFKA_BROKER = "data-producer:9092";
  public static final String TOPIC = "lrb";

  public static void main(String[] args) throws Exception {
    ConfigurableTopology.start(new TollNotifTopology(), args);
  }

  @Override
  protected int run(String[] args) throws Exception {
    final String brokerUrl = KAFKA_BROKER;
    System.out.println("Running with broker url: " + brokerUrl);

    TopologyBuilder builder = new TopologyBuilder();

    builder.setSpout("kafka-spout", new KafkaSpout<>(getKafkaSpoutConfig(brokerUrl)), 1);

    builder.setBolt("filter", new FilterBolt()).shuffleGrouping("kafka-spout"); // parse json and filter out tuples that
                                                                                // doesn't match the preconditions
    builder.setBolt("lav", new LAVBolt())
        // .fieldsGrouping("filter", new Fields("xway", "seg", "dir"));
        .shuffleGrouping("filter");

    builder.setBolt("toll", new TollNotifyBolt())
        // .fieldsGrouping("lav", new Fields("xway", "seg", "dir"));
        .shuffleGrouping("lav");

    conf.setDebug(true);

    String topologyName = "toll-notification";

    // conf.setNumWorkers(3);

    return submit(topologyName, conf, builder);
  }

  protected KafkaSpoutConfig<String, String> getKafkaSpoutConfig(String bootstrapServers) {
    ByTopicRecordTranslator<String, String> trans = new ByTopicRecordTranslator<>(
        (r) -> new Values(r.value()),
        new Fields("value"));

    return KafkaSpoutConfig.builder(bootstrapServers, TOPIC)
        .setProp(ConsumerConfig.GROUP_ID_CONFIG, "tollNotifTestGroup")
        .setRetry(getRetryService())
        .setRecordTranslator(trans)
        .setOffsetCommitPeriodMs(10_000)
        .setFirstPollOffsetStrategy(EARLIEST)
        .setMaxUncommittedOffsets(250)
        .build();
  }

  protected KafkaSpoutRetryService getRetryService() {
    return new KafkaSpoutRetryExponentialBackoff(TimeInterval.microSeconds(500),
        TimeInterval.milliSeconds(2), Integer.MAX_VALUE, TimeInterval.seconds(10));
  }
}