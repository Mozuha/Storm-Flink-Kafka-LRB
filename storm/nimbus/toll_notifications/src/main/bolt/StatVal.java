package main.bolt;

import java.util.Set;
import lombok.Data;

@Data
public class StatVal {
  public Integer count;
  public Integer spd;
  public Set<Integer> cars;

  public StatVal(Integer count, Integer spd, Set<Integer> cars) {
    this.count = count;
    this.spd = spd;
    this.cars = cars;
  }
}
