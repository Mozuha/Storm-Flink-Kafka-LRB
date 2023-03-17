package main.functions;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;

import main.models.Event;

public class VehicleState extends RichMapFunction<Event, Event> {
  private transient ValueState<Event> previousPositionReport;

  @Override
  public void open(Configuration config) {
    ValueStateDescriptor<Event> descriptor = new ValueStateDescriptor<>("vehicleState",
        TypeInformation.of(Event.class));
    previousPositionReport = getRuntimeContext().getState(descriptor);
  }

  @Override
  public Event map(Event value) throws Exception {
    if (value.type.equals(0)) {
      Event e = previousPositionReport.value();
      if (e == null) {
        value.inNewSeg = true;
        previousPositionReport.update(value);
        return value;
      }

      if (!e.segment.equals(value.segment)) {
        value.inNewSeg = true;
        return value;
      }
    }

    return value;
  }
}
