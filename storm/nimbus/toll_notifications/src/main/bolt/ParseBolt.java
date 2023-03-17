package main.bolt;

import java.util.Map;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParseBolt extends BaseRichBolt {
  protected static final Logger LOG = LoggerFactory.getLogger(ParseBolt.class);
  private OutputCollector collector;

  @Override
  public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
  }

  @Override
  public void execute(Tuple tuple) {
    // value/string has a shape e.g. "2,32,126,-1,-1,-1,-1,-1,-1,4,-1,-1,-1,-1,-1"

    String[] arr = tuple.getString(0).split(",");

    Integer type = Integer.parseInt(arr[0]);
    Short time = Short.parseShort(arr[1]);
    Integer vid = Integer.parseInt(arr[2]);
    Integer spd = Integer.parseInt(arr[3]);
    Integer xway = Integer.parseInt(arr[4]);
    Integer lane = Integer.parseInt(arr[5]);
    Integer dir = Integer.parseInt(arr[6]);
    Integer seg = Integer.parseInt(arr[7]);
    Integer pos = Integer.parseInt(arr[8]);
    Integer minute = time / 60 + 1;
    Long ingestTime = System.currentTimeMillis();

    collector.emit(tuple, new Values(type, time, vid, spd, xway, lane, dir, seg, pos, minute, ingestTime));
    LOG.info("parsed tuple");

    collector.ack(tuple);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer
        .declare(new Fields("type", "time", "vid", "spd", "xway", "lane", "dir", "seg", "pos", "minute", "ingestTime"));
  }
}