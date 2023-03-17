package main.functions;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;

import java.util.HashSet;
import java.util.Set;

import main.models.Event;

public class SegmentStatistics extends RichMapFunction<Event, Event> {
  private transient MapState<Integer, Tuple3<Integer, Integer, Set<Integer>>> segmentStatistics;

  @Override
  public void open(Configuration parameters) throws Exception {
    MapStateDescriptor<Integer, Tuple3<Integer, Integer, Set<Integer>>> descriptor = new MapStateDescriptor<>(
        "speedPerSegment",
        TypeInformation.of(Integer.class),
        TypeInformation.of(new TypeHint<Tuple3<Integer, Integer, Set<Integer>>>() {
        }));
    segmentStatistics = getRuntimeContext().getMapState(descriptor);
  }

  @Override
  public Event map(Event value) throws Exception {
    if (value.type == 0) {
      if (segmentStatistics.contains(value.minute)) {
        Tuple3<Integer, Integer, Set<Integer>> prev = segmentStatistics.get(value.minute);
        prev.f0 += 1;
        prev.f1 += value.speed;
        segmentStatistics.put(value.minute, prev);
      } else {
        Set<Integer> cars = new HashSet<>();
        cars.add(value.vid);
        segmentStatistics.put(value.minute, Tuple3.of(1, value.speed, cars));
      }

      // for lav
      int totalCount = 0;
      int totalSpeed = 0;
      for (int i = 1; i <= 5; i++) {
        if (segmentStatistics.contains(value.minute - i)) {
          Tuple3<Integer, Integer, Set<Integer>> v = segmentStatistics.get(value.minute - i);
          totalCount += v.f0;
          totalSpeed += v.f1;
        }
      }

      if (1 < value.minute) {
        Tuple3<Integer, Integer, Set<Integer>> t = segmentStatistics.get(value.minute - 1);
        int lav = 0 < totalCount ? (Math.round(totalSpeed / (float) totalCount)) : 0;
        value.lav = lav;
        value.numVehicles = t == null ? 0 : t.f2.size(); // numVehicles a minute ago
      } else {
        value.lav = 0;
        value.numVehicles = 0;
      }

    }

    return value;
  }
}
