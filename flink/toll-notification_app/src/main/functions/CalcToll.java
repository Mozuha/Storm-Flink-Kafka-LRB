package main.functions;

import org.apache.flink.api.common.functions.RichMapFunction;

import main.models.Event;
import main.models.TollNotification;

public class CalcToll extends RichMapFunction<Event, String> {

  @Override
  public String map(Event event) throws Exception {
    if (event.type == 0) {
      if (event.inNewSeg) {
        if (event.lane != 4) {
          Double numVehicles = (double) event.numVehicles;
          Integer lav = event.lav;

          int toll = 0;
          if (50 < numVehicles && lav < 40) {
            toll = (int) (2 * Math.pow(numVehicles - 50, 2));
          }

          long emit = System.currentTimeMillis() - event.ingestTime; // use time passed instead of emit time
          // long emit = System.currentTimeMillis();

          String out = new TollNotification(event.vid, event.time, emit, lav, toll).toString();
          return out;
          // return toll == 0 ? "" : out;
        }
      }
    }
    return "";
  }
}
