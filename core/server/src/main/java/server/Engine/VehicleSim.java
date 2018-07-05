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
 * 	DATA:			2017/6/2
 *
 * 	VER: 			1.0
 *
 * 	Purpose: 		Create and manage the simulation of a single vehicle.
 *
 **************************************************************************/


public class VehicleSim  {

    // The vars loaded from file

    public loadparam vars;

    public Operator[] operators;

    public Operator[] RemoteOpers;

    //Test: Multithread variable section
    private BlockingQueue<Task> globalWatingTasks;

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

    public VehicleSim(loadparam param, int vehicleid,  Operator[] remoteOps, ArrayList<Task> list, BlockingQueue<Task> globalWaitingTasks) {

        //Test Concurrency
        this.globalWatingTasks = globalWaitingTasks;
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

//        System.out.println("Generate task");

        // For each type of tasks:
        int fleetType = this.vehicleID/100;

        //If teamCoord Presents task number = total tasknum -1
        for (int i = 0; i < vars.fleetHetero[fleetType].length; i++) {

            // Create a new empty list of Tasks

            ArrayList<Task> indlist = new ArrayList<Task>();

            // Start a new task with PrevTime = 0

            Task newTask;

            int taskType = vars.fleetHetero[fleetType][i];

            newTask = new Task(taskType, 0, vars, true);
            if (newTask.getArrTime() < 0) {
                continue;
            }
            indlist.add(newTask);

            if (!vars.followedTask.get(taskType).isEmpty()) {
                genLinkedTask(indlist, newTask);
            }

            // While the next task is within the time frame, generate.

            while (newTask.getArrTime() < vars.numHours * 60) {
                newTask = new Task(taskType, newTask.getArrTime(), vars, true);
                if (newTask.getArrTime() < 0) {
                    break;
                }
                newTask.setID(vehicleID);
                indlist.add(newTask);
                if (!vars.followedTask.get(taskType).isEmpty()) {
                    genLinkedTask(indlist, newTask);
                }
            }

            // Put all task into the master task list.
            globalTasks.addAll(indlist);
            vars.repNumTasks[vars.replicationTracker]+= indlist.size();
        }

    }

    private void genLinkedTask(ArrayList<Task> indlist, Task leadTask){

        int leadTaskType = leadTask.getType();
        double prevTime = leadTask.getArrTime();
        ArrayList<Integer> followedTaskType = vars.followedTask.get(leadTaskType);

        for(int i : followedTaskType){
            int taskType = (leadTaskType + 1) * 100 + i;
            Task newTask = new Task(taskType, prevTime, vars, true);
            if(newTask.getArrTime() < 0) continue;
            newTask.setID(vehicleID);
            indlist.add(newTask);
        }

    }

    public void addTriggered() {

        for (Task each : globalTasks) {
            int i = each.getType();

            if (vars.trigger[i][0] != -1) {
                for (Integer that : vars.trigger[i]) {
                    globalTasks.add(new Task(that, each.getArrTime(), vars, false));
                }
            }
        }
    }


}
