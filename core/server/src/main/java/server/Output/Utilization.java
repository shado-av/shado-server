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

        taskName = vars.taskName_all;

        // get operators' name
        operatorName = new String[vars.numRemoteOp];
        int count = 0;
        for (int i = 0; i < vars.opNames.length; i++) {
            for (int j = 0; j < vars.teamSize[i]; j++) {
                operatorName[count] = vars.opNames[i] + "_" + Integer.toString(j);
                count++;
            }
        }

        //create the utilization matrix and averageUtilization matrix
        int numColumn = (int) Math.ceil(vars.numHours * 6);
        utilization = new Double[vars.numRemoteOp][vars.numReps][vars.numTaskTypes + vars.leadTask.length + 3][numColumn];
        averageUtilization = new Double[vars.numRemoteOp][vars.numReps];

    }

}
