package server.Engine;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.*;
import server.Input.loadparam;
import javafx.util.Pair;

/***************************************************************************
 *
 * 	FILE: 			VehicleSim.java
 *
 * 	AUTHOR: 		ROCKY LI
 *
 * 	DATA:			07/09/2018
 *
 * 	VER: 			1.0
 * 	                2.0     Naixin Yu
 *
 * 	Purpose: 		Create and manage the simulation of a single vehicle.
 *
 **************************************************************************/


public class VehicleSim  {

    // The vars loaded from file

    public loadparam vars;

    public Operator[] operators;

    public int vehicleID;

    // This is an arraylist of ALL tasks in the order that they're arriving.
    public ArrayList<Task> globalTasks;


    /****************************************************************************
     *
     *	Shado Object:	VehicleSim
     *
     *	Purpose:		Create a simulation for a single vehicle.
     *
     ****************************************************************************/

    public VehicleSim(loadparam param, int vehicleid,  Operator[] remoteOps, ArrayList<Task> list) {

        globalTasks = list;
        operators = remoteOps;
        vars = param;
        vehicleID = vehicleid;
    }

    /****************************************************************************
     *
     *	Method:			taskgen
     *
     *	Purpose:		Generate a list of task based on time order.
     *
     ****************************************************************************/

    public synchronized void taskgen() {

        // For each type of tasks:
        int fleetType = this.vehicleID/100;

        //If teamCoord Presents task number = total tasknum -1
        for (int i = 0; i < vars.fleetHetero[fleetType].length; i++) {

            // Create a new empty list of Tasks
            ArrayList<Task> indlist = new ArrayList<Task>();
            int taskType = vars.fleetHetero[fleetType][i];

            // Start a new task with PrevTime = 0
            Task newTask = genRegularTask(taskType, indlist, null);

            if (newTask == null) {
                continue;
            }

            // While the next task is within the time frame, generate.
            while (newTask.getArrTime() < vars.numHours * 60) {
                newTask = genRegularTask(taskType, indlist, newTask);
                if (newTask == null) {
                    break;
                }
            }

            // Put all task into the master task list.
            globalTasks.addAll(indlist);
            vars.repNumTasks[vars.replicationTracker]+= indlist.size();
        }

    }

    private Task genRegularTask(int taskType, ArrayList<Task> indlist, Task preTask){

        Task newTask;

        if (preTask == null) {
            newTask = new Task(taskType, 0, vars, true, vehicleID);
        }
        else {
            newTask = new Task(taskType, preTask.getArrTime(), vars, true, vehicleID);
        }

        if (newTask.getArrTime() < 0) {
            return null;
        }

        indlist.add(newTask);

        if (!vars.followedTask.get(taskType).isEmpty()) {
            genLinkedTask(indlist, newTask);
        }

        return newTask;
    }

    private void genLinkedTask(ArrayList<Task> indlist, Task leadTask){

        int leadTaskType = leadTask.getType();
        double prevTime = leadTask.getArrTime();
        ArrayList<Integer> followedTaskType = vars.followedTask.get(leadTaskType);

        if (followedTaskType.isEmpty()) {
            return;
        }

        for(int taskType : followedTaskType){

            Task newTask = new Task(taskType, prevTime, vars, true, vehicleID);
            if(newTask.getArrTime() < 0) continue;
            newTask.setID(vehicleID);
            indlist.add(newTask);

        }

    }

}
