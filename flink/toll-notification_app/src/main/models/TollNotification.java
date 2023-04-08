package main.models;

import lombok.Data;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class TollNotification {

  private SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  public Integer carId;
  public Short time;
  public Long emit;
  public Integer lav;
  public Integer toll;

  public TollNotification(Integer carId, Short time, Long emit, Integer lav, Integer toll) {
    this.carId = carId;
    this.time = time;
    this.emit = emit;
    this.lav = lav;
    this.toll = toll;
  }

  @Override
  public String toString() {
    return String.format("%s,%d,%s,%s,%d,%s,%s", dtf.format(new Date()), 0, carId, time, emit, lav, toll);
  }
}
