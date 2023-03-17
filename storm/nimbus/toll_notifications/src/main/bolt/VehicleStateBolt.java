package main.bolt;

import java.util.Map;
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

public class VehicleStateBolt extends BaseStatefulBolt<KeyValueState<Integer, Integer>> {
  private transient KeyValueState<Integer, Integer> prevSeg; // <vid, seg>
  protected static final Logger LOG = LoggerFactory.getLogger(VehicleStateBolt.class);
  private OutputCollector collector;

  @Override
  public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
  }

  @Override
  public void initState(KeyValueState<Integer, Integer> state) {
    prevSeg = state;
  }

  @Override
  public void execute(Tuple tuple) {
    Integer type = tuple.getInteger(0);
    Integer vid = tuple.getInteger(2);
    Integer seg = tuple.getInteger(7);

    if (type == 0) {
      Integer prev = prevSeg.get(vid, -1);
      if (prev == -1) { // no mapping found
        prevSeg.put(vid, seg);
        collector.emit(tuple,
            new Values(type, tuple.getShort(1), vid, tuple.getInteger(3), tuple.getInteger(4), tuple.getInteger(5),
                tuple.getInteger(6), seg, tuple.getInteger(8), tuple.getInteger(9), tuple.getLong(10)));
      }

      if (prev.equals(seg)) {
        collector.emit(tuple,
            new Values(type, tuple.getShort(1), vid, tuple.getInteger(3), tuple.getInteger(4), tuple.getInteger(5),
                tuple.getInteger(6), seg, tuple.getInteger(8), tuple.getInteger(9), tuple.getLong(10)));
      }
    }

    LOG.info("checked vehicle state");
    collector.ack(tuple);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer
        .declare(new Fields("type", "time", "vid", "spd", "xway", "lane", "dir", "seg", "pos", "minute", "ingestTime"));
  }
}
