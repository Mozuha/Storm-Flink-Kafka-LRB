package main;

import org.apache.flink.core.fs.Path;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.util.Collector;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.configuration.MemorySize;
import java.time.Duration;

public class KafkaFlinkPipeline {

  private static final String KAFKA_BROKER = "data-producer:9092";
  public static final String TOPIC = "lrb";
  private static final String GROUP_ID = "kafkaFlinkTestGroup";
  private static final Path OUTPUT_FILE = new Path("/tmp/flink_output");

  public static void main(String[] args) throws Exception {

    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    env.setRuntimeMode(RuntimeExecutionMode.STREAMING);

    KafkaSource<String> source = KafkaSource.<String>builder()
        .setBootstrapServers(KAFKA_BROKER)
        .setTopics(TOPIC)
        .setGroupId(GROUP_ID)
        .setStartingOffsets(OffsetsInitializer.earliest())
        .setValueOnlyDeserializer(new SimpleStringSchema())
        .setProperty("max.poll.records", "1000")
        .setProperty("max.poll.interval.ms", "1000")
        .build();

    DataStream<String> tuples = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

    DataStream<Tuple1<String>> prints = tuples.flatMap(new Printer())
        .name("printer");

    prints.sinkTo(
        FileSink.<Tuple1<String>>forRowFormat(
            OUTPUT_FILE, new SimpleStringEncoder<>())
            .withRollingPolicy(
                DefaultRollingPolicy.builder()
                    .withMaxPartSize(MemorySize.ofMebiBytes(1))
                    .withRolloverInterval(Duration.ofSeconds(10))
                    .build())
            .build())
        .name("file-sink");

    env.execute("KafkaFlinkPipeline");
  }

  public static final class Printer
      implements FlatMapFunction<String, Tuple1<String>> {

    @Override
    public void flatMap(String value, Collector<Tuple1<String>> out) {
      out.collect(new Tuple1<>(value));
    }
  }
}
