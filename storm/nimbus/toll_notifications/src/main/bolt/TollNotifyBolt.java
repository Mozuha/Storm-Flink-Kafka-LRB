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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TollNotifyBolt extends BaseRichBolt {
  protected static final Logger LOG = LoggerFactory.getLogger(TollNotifyBolt.class);
  private OutputCollector collector;
  Map<List<Integer>, Integer> numVehicles = new HashMap<List<Integer>, Integer>(); // <[xway, seg, dir, minute],
                                                                                   // count>
  Map<Integer, Integer> segState = new HashMap<Integer, Integer>(); // <vid, seg>

  @Override
  public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
  }

  @Override
  public void execute(Tuple input) {
    LOG.info("received from lav");
    int xway = input.getInteger(4);
    int seg = input.getInteger(6);
    int dir = input.getInteger(5);
    int minute = input.getInteger(1);
    // float lav = input.getFloat(8);
    int lane = input.getInteger(8);
    float lav = input.getFloat(9);
    int vid = input.getInteger(2);

    List<Integer> key = Arrays.asList(xway, seg, dir, minute);
    if (numVehicles.containsKey(key)) {
      numVehicles.replace(key, numVehicles.get(key) + 1);
    } else {
      numVehicles.put(key, 1);
    }

    segState.putIfAbsent(vid, seg);

    LOG.info("before lane cond");
    // minute value starts from 1; numVehicles for last 1 min will be available
    // after the
    // minute value has reached 2
    // conditions for notifying toll is that lav is less than 40 MPH and numVehicles
    // as of a minute ago is greater than 50
    if (2 < minute && lav < 40 && lane != 4 && seg != segState.get(vid)) {
      List<Integer> keyPrev1 = Arrays.asList(xway, seg, dir, minute - 1);
      LOG.info("before numVehicles cond");
      if (50 < numVehicles.get(keyPrev1)) {
        LOG.info("inside numVehicles cond");
        int toll = 2 * (numVehicles.get(keyPrev1) - 50) * (numVehicles.get(keyPrev1) - 50);

        // output tuple is (type: 0, vid, time, emit, lav, toll)
        String tollNotif = String.format("0, %d, %d, %d, %.3f, %d", input.getInteger(2), input.getInteger(0),
            input.getInteger(0), lav, toll);

        LOG.info("toll notification = [" + tollNotif + "]");
      }
      segState.replace(vid, seg);
    }

    // List<Integer> keyPrev2 = Arrays.asList(xway, seg, dir, minute-2);
    // if (numVehicles.containsKey(keyPrev2)) numVehicles.remove(keyPrev2);

    collector.ack(input);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {

  }
}