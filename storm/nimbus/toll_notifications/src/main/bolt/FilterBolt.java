package main.bolt;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONObject;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterBolt extends BaseRichBolt {
  protected static final Logger LOG = LoggerFactory.getLogger(TollNotifyBolt.class);
  private OutputCollector collector;
  Map<Integer, Integer> segState = new HashMap<Integer, Integer>(); // <vid, seg>

  @Override
  public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
  }

  @Override
  public void execute(Tuple input) {
    // value/string has a shape e.g.
    // {"schema":{"type":"string","optional":false},"payload":"2,32,126,-1,-1,-1,-1,-1,-1,4,-1,-1,-1,-1,-1"}

    JSONObject obj = new JSONObject(input.getString(0));
    int[] payload = Arrays.stream(obj.getString("payload").split(",")).mapToInt(Integer::parseInt).toArray();

    int type = payload[0];

    // only forward position records
    if (type == 0) {
      int time = payload[1];
      int vid = payload[2];
      int spd = payload[3];
      int xway = payload[4];
      int lane = payload[5];
      int dir = payload[6];
      int seg = payload[7];
      int pos = payload[8];

      // if (lane != 4) { // lane != exit ramp
      // segState.putIfAbsent(vid, seg);
      // if (seg != segState.get(vid)) { // vehicle got into new segment
      // int minute = Math.floorDiv(time, 60) + 1;
      // collector.emit(new Values(time, minute, vid, spd, xway, dir, seg, pos));
      // LOG.info("forwarded tuple");
      // segState.replace(vid, seg);
      // }
      // }

      int minute = Math.floorDiv(time, 60) + 1;
      collector.emit(new Values(time, minute, vid, spd, xway, dir, seg, pos, lane));
      LOG.info("forwarded tuple");
    }

    collector.ack(input);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // declarer.declare(new Fields("time", "minute", "vid", "spd", "xway", "dir",
    // "seg", "pos"));
    declarer.declare(new Fields("time", "minute", "vid", "spd", "xway", "dir", "seg", "pos", "lane"));
  }
}