package edu.smccme.vgreen.smttcascobaylines;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class ScheduleList extends Fragment {

    private RecyclerView mRecyclerView;
    private ScheduleAdapter mAdapter;

    public ScheduleList() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule_list, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.schedule_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        updateUI();
        // Inflate the layout for this fragment
        return view;
    }

    private void updateUI(){
        ModelManager.ScheduleManager mSchedMgr = ModelManager.ScheduleManager.getInstance();
        List<Schedule> mScheds = mSchedMgr.getSchedules();

        if(mAdapter == null){
            mAdapter = new ScheduleAdapter(mScheds);
            mRecyclerView.setAdapter(mAdapter);
        }else{
            mAdapter.notifyDataSetChanged();
        }
    }

    private class ScheduleHolder extends RecyclerView.ViewHolder{
        TextView mSchedId,
                mSchedPoa,
                mSchedPod,
                mSchedDepartTime;
        Schedule mSchedule;

        public ScheduleHolder(View itemView) {
            super(itemView);

            mSchedId = (TextView) itemView.findViewById(R.id.sched_id_label);
            mSchedPod = (TextView) itemView.findViewById(R.id.sched_pod_label);
            mSchedPoa = (TextView) itemView.findViewById(R.id.sched_poa_label);
            mSchedDepartTime = (TextView) itemView.findViewById(R.id.sched_departTime_label);
        }

        public void bindSchedule(Schedule sched){
            mSchedule = sched;

            String time = mSchedule.getDepartureDate().get(Calendar.HOUR_OF_DAY) +
                    ":" + mSchedule.getDepartureDate().get(Calendar.MINUTE);

            mSchedId.setText(String.valueOf(mSchedule.getScheduleId()));
            mSchedPod.setText(mSchedule.getPod());
            mSchedPoa.setText(mSchedule.getPoa());
            mSchedDepartTime.setText(time);
        }
    }

    private class ScheduleAdapter extends RecyclerView.Adapter<ScheduleHolder>{
        private List<Schedule> mSchedules;

        public ScheduleAdapter(List<Schedule> scheds){
            mSchedules = scheds;
        }

        @Override
        public ScheduleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater li = LayoutInflater.from(getActivity());
            View view = li.inflate(R.layout.list_item_schedule, parent, false);

            return new ScheduleHolder(view);
        }

        @Override
        public void onBindViewHolder(ScheduleHolder holder, int position) {
            Schedule sched = mSchedules.get(position);
            holder.bindSchedule(sched);
        }

        @Override
        public int getItemCount() {
            return mSchedules.size();
        }
    }
}
