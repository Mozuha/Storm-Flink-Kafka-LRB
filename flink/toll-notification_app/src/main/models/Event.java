package main.models;

import lombok.Data;

@Data
public class Event {

  public Integer type;
  public Short time;
  public Integer vid;
  public Integer speed;
  public Integer xWay;
  public Integer lane;
  public Integer direction;
  public Integer segment;
  public Integer position;
  public Integer minute;

  public Long ingestTime = -1L;
  public Boolean inNewSeg = false;
  public Integer numVehicles = -1;
  public Integer lav = -1;
  public Integer toll = -1;

  public static Event parseFromString(String s) {

    /*
     * incoming value looks like "2,32,126,-1,-1,-1,-1,-1,-1,4,-1,-1,-1,-1,-1" where
     * (type, time, vid, spd, xway, lane, dir, seg, pos, qid, sinit, send, dow, tod,
     * day)
     */
    String[] arr = s.split(",");

    Event e = new Event();
    e.setType(Integer.parseInt(arr[0]));
    e.setTime(Short.parseShort(arr[1]));
    e.setVid(Integer.parseInt(arr[2]));
    e.setSpeed(Integer.parseInt(arr[3]));
    e.setXWay(Integer.parseInt(arr[4]));
    e.setLane(Integer.parseInt(arr[5]));
    e.setDirection(Integer.parseInt(arr[6]));
    e.setSegment(Integer.parseInt(arr[7]));
    e.setPosition(Integer.parseInt(arr[8]));
    e.setMinute(e.time / 60 + 1);

    return e;
  }

}