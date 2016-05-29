package edu.smccme.vgreen.smttcascobaylines;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Scott on 5/1/2016.
 */
public class PortManager {
    private List<Port> mPorts;
    private static PortManager sPortManager;

    public static PortManager getInstance(){
        if(sPortManager == null){
            sPortManager = new PortManager();
        }

        return sPortManager;
    }

    private PortManager(){
        mPorts = new ArrayList<>();

        Port newPort = new Port(1, "PK", "Peaks Island", 43.655373, -70.200236);
        mPorts.add(newPort);
        newPort = new Port(2, "109", "Portland", 43.657263, -70.248666);
        mPorts.add(newPort);
        newPort = new Port(3, "LD", "Little Diamond Island", 43.662700, -70.209480);
        mPorts.add(newPort);
        newPort = new Port(4, "GD", "Great Diamond Island", 43.670383, -70.199366);
        mPorts.add(newPort);
        newPort = new Port(5, "DC", "Diamond Cove", 43.684826, -70.191357);
        mPorts.add(newPort);
        newPort = new Port(6, "LO", "Long Island", 43.691730, -70.165353);
        mPorts.add(newPort);
        newPort = new Port(7, "CH", "Chebeague Island", 43.716190, -70.126771);
        mPorts.add(newPort);
        newPort = new Port(8, "CF", "Cliff Island", 43.694991, -70.110180);
        mPorts.add(newPort);
        newPort = new Port(9, "BI", "Bailey Island", 43.749388, -69.990737);
        mPorts.add(newPort);
    }

    public List<Port> getPorts(){
        return mPorts;
    }

    public Port getPortById(int id){
        //Port ID should coincide with ArrayList index, but in case it doesn't
        for(Port port : mPorts){
            if(port.getPortId() == id){
                return port;
            }
        }
        return null;
    }

    public String getPortIdByLabel(String label){
        for (Port port : mPorts){
            if(port.getFullLabel().equalsIgnoreCase(label) ||
                    port.getShortLabel().equalsIgnoreCase(label) ||
                    String.valueOf(port.getPortId()).equalsIgnoreCase(label)){
                return String.valueOf(port.getPortId());
            }
        }
        return null;
    }

}
