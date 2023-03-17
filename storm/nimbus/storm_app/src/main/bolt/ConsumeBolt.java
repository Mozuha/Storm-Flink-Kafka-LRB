package main.bolt;

import java.util.Map;
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
    // value/string has a shape e.g. "2,32,126,-1,-1,-1,-1,-1,-1,4,-1,-1,-1,-1,-1"

    LOG.info("input = [" + input.getString(0) + "]");
    collector.ack(input);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {

  }
}