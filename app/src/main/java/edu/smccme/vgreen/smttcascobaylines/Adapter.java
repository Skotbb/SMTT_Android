package edu.smccme.vgreen.smttcascobaylines;

import android.content.Context;
import android.graphics.AvoidXfermode;
import android.text.style.TtsSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

/**
 * Created by sochendamean on 5/9/16.
 */
public class Adapter extends BaseAdapter implements ModelManager.VehicleListener{

    Context m_context;
    ArrayList<VehicleInfo> m_vehicle;


    private Adapter(){}

    public Adapter(Context context){
        m_context = context;
        ModelManager.getInstance(m_context).registerVehicleListener(this);


    }
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return m_vehicle.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SingleScheduleLayout ssl = (SingleScheduleLayout) convertView;
        Port port = (Port) getItem(position);

        if(ssl == null){
            ssl = new SingleScheduleLayout(m_context, port);
        } else {
            ssl.updateSchedule(port);
        }

        return ssl;
    }


    @Override
    public void vehiclesChanged(ArrayList<VehicleInfo> vehicles) {
        ModelManager.getInstance(m_context).updateVehicleInfo(m_vehicle);
        ModelManager.getInstance(m_context).notifyVehicleListeners();
    }
}
