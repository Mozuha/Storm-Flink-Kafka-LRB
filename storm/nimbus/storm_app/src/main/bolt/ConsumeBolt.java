package main.bolt;

import java.util.Map;
import org.json.JSONObject;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumeBolt extends BaseRichBolt {
  protected static final Logger LOG = LoggerFactory.getLogger(ConsumeBolt.class);
  private OutputCollector collector;

  @Override
  public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
  }

  @Override
  public void execute(Tuple input) {
    // value/string has a shape e.g.
    // {"schema":{"type":"string","optional":false},"payload":"2,32,126,-1,-1,-1,-1,-1,-1,4,-1,-1,-1,-1,-1"}

    JSONObject obj = new JSONObject(input.getString(0));
    String payload = obj.getString("payload");
    LOG.info("input = [" + payload + "]");
    collector.ack(input);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {

  }
}