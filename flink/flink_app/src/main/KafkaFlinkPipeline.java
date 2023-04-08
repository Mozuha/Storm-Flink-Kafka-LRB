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
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.configuration.MemorySize;
import java.time.Duration;
import java.text.SimpleDateFormat;
import java.util.Date;

public class KafkaFlinkPipeline {

  private static final String KAFKA_BROKER = "data-producer:9092";
  public static final String TOPIC = "lrb";
  private static final String GROUP_ID = "kafkaFlinkTestGroup";
  private static final Path OUTPUT_FILE = new Path("/tmp/flink_output");

  public static void main(String[] args) throws Exception {

    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    env.setRuntimeMode(RuntimeExecutionMode.STREAMING);
    // start a checkpoint every 1000 ms
    env.enableCheckpointing(1000);

    KafkaSource<String> source = KafkaSource.<String>builder()
        .setBootstrapServers(KAFKA_BROKER)
        .setTopics(TOPIC)
        .setGroupId(GROUP_ID)
        .setStartingOffsets(OffsetsInitializer.earliest())
        .setValueOnlyDeserializer(new SimpleStringSchema())
        .setProperty("max.poll.records", "100000")
        .setProperty("max.poll.interval.ms", "1000")
        .build();

    DataStream<String> tuples = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

    DataStream<Tuple2<String, String>> prints = tuples.flatMap(new Printer())
        .name("printer");

    prints.sinkTo(
        FileSink.<Tuple2<String, String>>forRowFormat(
            OUTPUT_FILE, new SimpleStringEncoder<>())
            .withRollingPolicy(
                DefaultRollingPolicy.builder()
                    /*
                     * rolls the in-progress part file when it contains at least 1 sec worth of data
                     * i.e. one in-progress part file contains output for 1 sec
                     */
                    .withRolloverInterval(Duration.ofSeconds(1))
                    .build())
            .build())
        .name("file-sink");

    env.execute("KafkaFlinkPipeline");
  }

  public static final class Printer
      implements FlatMapFunction<String, Tuple2<String, String>> {
    SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public void flatMap(String value, Collector<Tuple2<String, String>> out) {
      out.collect(new Tuple2<>(dtf.format(new Date()), value));
    }
  }
}
