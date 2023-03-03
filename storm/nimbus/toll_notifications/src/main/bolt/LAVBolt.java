package main.bolt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class LAVBolt extends BaseRichBolt {
  protected static final Logger LOG = LoggerFactory.getLogger(TollNotifyBolt.class);
  private OutputCollector collector;
  Map<List<Integer>, List<Integer>> lav = new HashMap<List<Integer>, List<Integer>>(); // <[xway, seg, dir, minute],
                                                                                       // [sum, count]>

  @Override
  public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
  }

  @Override
  public void execute(Tuple input) {
    int xway = input.getInteger(4);
    int seg = input.getInteger(6);
    int dir = input.getInteger(5);
    int minute = input.getInteger(1);
    int spd = input.getInteger(3);

    List<Integer> key = Arrays.asList(xway, seg, dir, minute);
    if (lav.containsKey(key)) {
      lav.replace(key, Arrays.asList(lav.get(key).get(0) + spd, lav.get(key).get(1) + 1));
    } else {
      lav.put(key, Arrays.asList(spd, 1));
    }
    LOG.info("received at lav, minute: " + minute);

    // minute value starts from 1; lav for last 5 mins will be available after the
    // minute value has reached 6
    if (5 < minute) {
      List<Integer> keyPrev1 = Arrays.asList(xway, seg, dir, minute - 1);
      List<Integer> keyPrev2 = Arrays.asList(xway, seg, dir, minute - 2);
      List<Integer> keyPrev3 = Arrays.asList(xway, seg, dir, minute - 3);
      List<Integer> keyPrev4 = Arrays.asList(xway, seg, dir, minute - 4);
      List<Integer> keyPrev5 = Arrays.asList(xway, seg, dir, minute - 5);

      int sum = lav.get(key).get(0) + lav.get(keyPrev1).get(0) + lav.get(keyPrev2).get(0) + lav.get(keyPrev3).get(0)
          + lav.get(keyPrev4).get(0) + lav.get(keyPrev5).get(0);
      int count = lav.get(key).get(1) + lav.get(keyPrev1).get(1) + lav.get(keyPrev2).get(1) + lav.get(keyPrev3).get(1)
          + lav.get(keyPrev4).get(1) + lav.get(keyPrev5).get(1);
      Float avg = (float) (sum / count);
      // collector
      // .emit(new Values(input.getInteger(0), minute, input.getInteger(2), spd, xway,
      // dir, seg, input.getInteger(7),
      // avg));
      collector
          .emit(new Values(input.getInteger(0), minute, input.getInteger(2), spd, xway, dir, seg, input.getInteger(7),
              input.getInteger(8),
              avg));
      LOG.info("lav calculated");
    }

    // List<Integer> keyPrev6 = Arrays.asList(xway, seg, dir, minute-6);
    // if (lav.containsKey(keyPrev6)) lav.remove(keyPrev6);

    collector.ack(input);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // declarer.declare(new Fields("time", "minute", "vid", "spd", "xway", "dir",
    // "seg", "pos", "lav"));
    declarer.declare(new Fields("time", "minute", "vid", "spd", "xway", "dir", "seg", "pos", "lane", "lav"));
  }
}