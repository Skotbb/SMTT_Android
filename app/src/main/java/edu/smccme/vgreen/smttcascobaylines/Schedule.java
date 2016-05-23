package edu.smccme.vgreen.smttcascobaylines;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Scott on 5/20/2016.
 */
public class Schedule {
    private int scheduleId;
    private String pod,
                    poa;
    private Calendar departureDate;

    public Schedule(int scheduleId, String pod, String poa, Calendar departureDate) {
        this.scheduleId = scheduleId;
        this.pod = pod;
        this.poa = poa;
        this.departureDate = departureDate;

    }

    public int getScheduleId() {
        return scheduleId;
    }

    public String getPod() {
        return pod;
    }

    public String getPoa() {
        return poa;
    }

    public Calendar getDepartureDate() {
        return departureDate;
    }

}
