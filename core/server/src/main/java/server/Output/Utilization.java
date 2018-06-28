package server.Output;
import server.Input.loadparam;

public class Utilization {

    String[] operatorName;
    String[] taskName;
    Double[][][][] utilization; //[operator][replication][task][time];
    Double[][] averageUtilization; //[operator][replication];

    /****************************************************************************
     *
     *	Shado Object:	Utilization
     *
     *  Author: Naixin 05/29/2018
     *
     *	Purpose:	Create a object to record the utilization
     *                  1. per operator
     *                  2. per replication
     *                  3. per 10-minutes interval
     *                  4. per task type
     *
     ****************************************************************************/

    public Utilization (loadparam vars){
        // get operators' name
        operatorName = new String[vars.numRemoteOp];
        for(int i = 0; i < vars.numRemoteOp; i++){
            operatorName[i] = vars.reps[0].getRemoteOp().getRemoteOp()[i].getName();
        }

        //get tasks' name
        taskName = new String[vars.numTaskTypes + vars.leadTask.length + 3];
        for(int i = 0; i < vars.numTaskTypes; i++){
            taskName[i] = vars.taskNames[i];
        }
        for(int i = 0; i < vars.leadTask.length; i++){
            taskName[vars.numTaskTypes + i] = vars.taskNames_f[i];
        }
        int totalTasks = vars.numTaskTypes + vars.leadTask.length;
        taskName[totalTasks] = "TC task (some)";
        taskName[totalTasks + 1] = "TC task (full)";
        taskName[totalTasks + 2] = "Exogenous task";

        //create the utilization matrix and averageUtilization matrix
        int numColumn = (int) Math.ceil(vars.numHours * 6);
        utilization = new Double[vars.numRemoteOp][vars.numReps][vars.numTaskTypes + vars.leadTask.length + 3][numColumn];
        averageUtilization = new Double[vars.numRemoteOp][vars.numReps];

    }

}
