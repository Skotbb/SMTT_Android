package edu.smccme.vgreen.smttcascobaylines;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class SingleScheduleLayout extends RelativeLayout{

    Port m_port;
    FerryTrip.FerryStop m_ferry;
    Context m_context;
    TextView m_portId;
    TextView m_destination;
    TextView m_time;

    public SingleScheduleLayout(Context context){
        super(context);
        m_context = context;
    }

    public SingleScheduleLayout(Context context, Port port){
        super(context);
        m_context = context;
        LayoutInflater inflater = (LayoutInflater) m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        boolean attachToRoot = true; // "The ViewGroup I am passing is the root"
        inflater.inflate(R.layout.single_layout, this, attachToRoot);

        m_portId = (TextView) findViewById(R.id.port_textView);
        m_destination = (TextView) findViewById(R.id.destination_textView);
        m_time = (TextView) findViewById(R.id.time_textView);

        updateSchedule(port);
    }

    public void updateSchedule(Port port){
        m_port = port;
        m_portId.setText(m_port.getPortId());
        m_destination.setText(m_port.getFullLabel());
        m_time.setText(m_ferry.getDepartureTime());


    }
}
