package main;

import java.time.Duration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.util.Collector;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.configuration.MemorySize;

import main.models.Event;
import main.functions.VehicleState;
import main.functions.SegmentStatistics;
import main.functions.CalcToll;

/*
 * Referenced: https://github.com/wladox/linear-road-flink/tree/master/linear-road-flink/src/main/java/com/github/wladox
 */

public class TollNotificationJob {

  private static final String KAFKA_BROKER = "data-producer:9092";
  public static final String TOPIC = "lrb";
  private static final String GROUP_ID = "kafkaFlinkTestGroup";
  private static final Path OUTPUT_FILE = new Path("/tmp/flink_output/toll-notification");

  public static void main(String[] args) throws Exception {

    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
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

    DataStream<String> stream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

    DataStream<Event> events = stream
        .assignTimestampsAndWatermarks(new AscendingTimestampExtractor<String>() {
          @Override
          public long extractAscendingTimestamp(String element) {
            return Long.valueOf(element.split(",")[1]); // time field
          }
        })
        .map(Event::parseFromString)
        .process(new ProcessFunction<Event, Event>() {
          @Override
          public void processElement(Event value, Context ctx, Collector<Event> out) throws Exception {
            value.setIngestTime(System.currentTimeMillis());
            out.collect(value);
          }
        })
        .name("events");

    DataStream<Event> segmentStatistics = events
        .filter(s -> s.getType() == 0)
        .keyBy("vid")
        .map(new VehicleState())
        .keyBy(new KeySelector<Event, Tuple3<Integer, Integer, Integer>>() {
          @Override
          public Tuple3<Integer, Integer, Integer> getKey(Event value) throws Exception {
            return Tuple3.of(value.xWay, value.direction, value.segment);
          }
        })
        .map(new SegmentStatistics())
        .name("segStats");

    segmentStatistics
        .keyBy("xWay", "vid")
        .map(new CalcToll())
        .filter(s -> !(s == null || s.trim().isEmpty()))
        .sinkTo(
            FileSink.<String>forRowFormat(OUTPUT_FILE, new SimpleStringEncoder<>())
                .withRollingPolicy(DefaultRollingPolicy.builder()
                    .withMaxPartSize(MemorySize.ofMebiBytes(1))
                    .withRolloverInterval(Duration.ofSeconds(10))
                    .build())
                .build())
        .name("calcToll");

    env.execute("TollNotificationJob");
  }
}