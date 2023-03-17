package main.bolt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.apache.storm.state.KeyValueState;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseStatefulBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.bolt.StatVal;

public class LAVBolt extends BaseStatefulBolt<KeyValueState<Integer, StatVal>> {
  protected static final Logger LOG = LoggerFactory.getLogger(LAVBolt.class);
  private OutputCollector collector;
  private transient KeyValueState<Integer, StatVal> segStat; // <minute, (count, spd, numVehicles)>

  @Override
  public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
  }

  @Override
  public void initState(KeyValueState<Integer, StatVal> state) {
    segStat = state;
  }

  @Override
  public void execute(Tuple tuple) {
    Integer type = tuple.getInteger(0);
    Integer vid = tuple.getInteger(2);
    Integer minute = tuple.getInteger(9);
    Integer spd = tuple.getInteger(3);

    if (type == 0) {
      if (segStat.get(minute) != null) {
        StatVal prev = segStat.get(minute);
        prev.count += 1;
        prev.spd += spd;
        prev.cars.add(vid);
        segStat.put(minute, prev);
      } else {
        Set<Integer> cars = new HashSet<>();
        cars.add(vid);
        segStat.put(minute, new StatVal(1, spd, cars));
      }

      int totalCount = 0;
      int totalSpd = 0;
      for (int i = 1; i <= 5; i++) {
        if (segStat.get(minute - i) != null) {
          StatVal prev = segStat.get(minute - i);
          totalCount += prev.count;
          totalSpd += prev.spd;
        }
      }

      if (1 < minute) {
        StatVal prev = segStat.get(minute - 1);
        int lav = 0 < totalCount ? (Math.round(totalSpd / (float) totalCount)) : 0;
        int numVehicles = prev == null ? 0 : prev.cars.size(); // numVehicles a minute ago
        LOG.info("numVehicles: " + numVehicles);

        collector.emit(tuple,
            new Values(type, tuple.getShort(1), vid, spd, tuple.getInteger(4), tuple.getInteger(5), tuple.getInteger(6),
                tuple.getInteger(7), tuple.getInteger(8), minute, tuple.getLong(10), lav, numVehicles));
      } else {
        collector.emit(tuple, new Values(type, tuple.getShort(1), vid, spd, tuple.getInteger(4), tuple.getInteger(5),
            tuple.getInteger(6), tuple.getInteger(7), tuple.getInteger(8), minute, tuple.getLong(10), 0, 0));
      }

      LOG.info("lav calculated");
    }

    collector.ack(tuple);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("type", "time", "vid", "spd", "xway", "lane", "dir", "seg", "pos", "minute",
        "ingestTime", "lav", "numVehicles"));
  }
}
