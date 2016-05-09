package edu.smccme.vgreen.smttcascobaylines;


import android.app.ListFragment;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;


public class ScheduleView extends ListFragment{


    public ScheduleView() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        ListView view = (ListView) inflater.inflate(R.layout.fragment_schedule_view, container, false);
        // creating adapter
        Adapter adapter = new Adapter(getActivity().getApplicationContext());
        view.setAdapter(adapter);

        return view;
    }

}
