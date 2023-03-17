package main.bolt;

import java.util.Map;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TollNotifyBolt extends BaseRichBolt {
  protected static final Logger LOG = LoggerFactory.getLogger(TollNotifyBolt.class);
  private OutputCollector collector;

  @Override
  public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
    this.collector = collector;
  }

  @Override
  public void execute(Tuple tuple) {
    Integer type = tuple.getInteger(0);
    Integer lane = tuple.getInteger(5);

    if (type == 0) {
      if (lane != 4) {
        Integer lav = tuple.getInteger(11);
        Integer numVehicles = tuple.getInteger(12);

        int toll = 0;
        if (50 < numVehicles && lav < 40) {
          toll = (int) (2 * Math.pow(numVehicles - 50, 2));
        }

        long emit = System.currentTimeMillis() - tuple.getLong(10); // use time passed instead of emit time

        // output tuple is (type: 0, vid, time, emit, lav, toll)
        String tollNotif = String.format("0, %d, %d, %d, %d, %d", tuple.getInteger(2), tuple.getShort(1),
            emit, lav, toll);

        LOG.info("toll notification = [" + tollNotif + "]");
      }
    }

    collector.ack(tuple);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {

  }
}