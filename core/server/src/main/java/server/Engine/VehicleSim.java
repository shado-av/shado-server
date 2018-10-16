package server.Engine;
import java.util.*;
import server.Input.loadparam;

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

    private int fleetType;


    /****************************************************************************
     *
     *	Shado Object:	VehicleSim
     *
     *	Purpose:		Create a simulation for a single vehicle.
     *
     ****************************************************************************/

    public VehicleSim(loadparam param, int vehicleID,  Operator[] remoteOps, ArrayList<Task> list) {

        globalTasks = list;
        operators = remoteOps;
        vars = param;
        this.vehicleID = vehicleID;

        fleetType = vehicleID/100;
    }

    /****************************************************************************
     *
     *	Method:			taskgen
     *
     *	Purpose:		Generate a list of task based on time order.
     *
     ****************************************************************************/

    public synchronized void taskgen() throws Exception{

        //If teamCoord Presents task number = total tasknum -1
        for (int i = 0; i < vars.fleetHetero[fleetType].length; i++) {

            int taskType = vars.fleetHetero[fleetType][i];
            if (vars.leadTask[i] >= 0) continue;    // only lead tasks can be created here

            // Create a new empty list of Tasks
            ArrayList<Task> indlist = new ArrayList<Task>();

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

    /****************************************************************************
     *
     *	Method:			genRegularTask
     *
     *	Purpose:		Generate regular tasks.
     *
     ****************************************************************************/

    private Task genRegularTask(int taskType, ArrayList<Task> indlist, Task preTask) throws Exception{

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

        // if any followed task is available and that task is belonged to this vehicle
        for (int i = 0; i < vars.fleetHetero[fleetType].length; i++) {
            if (vars.leadTask[vars.fleetHetero[fleetType][i]] == taskType) { // Followed Task
                genLinkedTask(i, indlist, newTask);      // add it
            }
        }

        // if (!vars.followedTask.get(taskType).isEmpty()) {
        //     genLinkedTask(indlist, newTask);
        // }

        return newTask;
    }

    /****************************************************************************
     *
     *	Method:			genLinkedTask
     *
     *	Purpose:		When a linked task's lead task is generated, create a
     *                  corresponding linked task.
     *
     ****************************************************************************/

    private void genLinkedTask(int taskType, ArrayList<Task> indlist, Task leadTask) throws Exception{

        double prevTime = leadTask.getArrTime();

        Task newTask = new Task(taskType, prevTime, vars, true, vehicleID);

        if(newTask.getArrTime() >= 0) {
            //no need it's already assigned at new
            //newTask.setID(vehicleID);
            indlist.add(newTask);

            //TODO: Followed task should have its lead task as a reference, which should be used at sorting.
            // newTask.setLeadTask(leadTask);
        }
    }
}
