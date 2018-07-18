package server.Output;
import server.Engine.Data;
import server.Input.loadparam;

import java.util.HashSet;
import java.util.Set;

/***************************************************************************
 *
 * 	FILE: 			Utilization.java
 *
 * 	AUTHOR: 		Naixin Yu
 *
 * 	LATEST EDIT:	07/16/2018
 *
 * 	VER: 			1.0
 *
 * 	Purpose: 		A class to records and manage the percentage utilization.
 * 	                Also used for JSON output to the website.
 *
 **************************************************************************/

public class Utilization {

    String[] operatorName;
    String[] taskName;
    Double[][][][] taskUtilization; //[operator][replication][task][time]
    Double[][] averageTaskUtilization; //[operator][replication]
    Double[][][][] fleetUtilization; //[operator][replication][fleet][time]
//    Double[][] averageFleetUtilization; //[operator][fleet]

    public Double[][][][] getTaskUtilization() { return taskUtilization; }

    /****************************************************************************
     *
     *	Shado Object:	Utilization
     *
     *	Purpose:	Create a object to record the utilization
     *                  1. per operator
     *                  2. per replication
     *                  3. per task type
     *                  4. per 10-minutes interval
     *
     ****************************************************************************/

    public Utilization(loadparam vars){

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
        taskUtilization = new Double[vars.numRemoteOp][vars.numReps][vars.totalTaskType][numColumn];
        averageTaskUtilization = new Double[vars.numRemoteOp][vars.numReps];
        fleetUtilization = new Double[vars.numRemoteOp][vars.numReps][vars.fleetTypes][numColumn];
//        averageFleetUtilization = new Double[vars.numRemoteOp][vars.fleetTypes];

    }

    /****************************************************************************
     *
     *	Method:     fillUtilization
     *
     *	Purpose:    Filled the utilization and averageUtilization matrix based on
     *              utilizationOutput records.
     *
     ****************************************************************************/

    public void fillTaskUtilization(int rep, Data[] taskU, loadparam param){

        int numColumn = (int) Math.ceil(param.numHours * 6);

        for (int op = 0; op < param.numRemoteOp; op++) {

                double sum = 0;
                Data currentUtilization = taskU[op];
                for (int task = 0; task < param.totalTaskType; task++) {
                    for (int time = 0; time < numColumn; time++) {
                        double u = currentUtilization.dataget(task, time, 0);
                        taskUtilization[op][rep][task][time] = round(u,2);
                        sum += u;
                    }
                }
                sum /= param.numHours * 6;
                averageTaskUtilization[op][rep] = sum;

        }
    }

    public void fillFleetUtilization(int rep, Data[] fleetU, loadparam param){

        int numColumn = (int) Math.ceil(param.numHours * 6);

        for (int op = 0; op < param.numRemoteOp; op++) {

            Data currentUtilization = fleetU[op];

            for (int fleet = 0; fleet < param.fleetTypes; fleet++) {
                for (int time = 0; time < numColumn; time++) {

                    double u = currentUtilization.dataget(fleet, time, 0);
                    fleetUtilization[op][rep][fleet][time] = round(u,2);

                }
            }
        }

    }

    /****************************************************************************
     *
     *	Method:     timeSectionSum
     *
     *	Purpose:    Return a utilization matrix for particular operator and
     *          	replication. The length of time, by which each column of this
     *          	matrix represent, can be customized using the size parameter.
     *
     *              e.g.    size        percentage utilization in
     *                       1                  10 min
     *                       2                  20 min
     *
     ****************************************************************************/

    public Double[][] timeSectionSum(loadparam param, int operator, int replication, int size) throws Exception{

        if (size > param.numHours * 6 || size < 1) {
            throw new Exception("Incorrect time interval size for utilization output");
        }

        Double[][] rawData = taskUtilization[operator][replication];

        double i = param.numHours * 6;
        int numColumn = (int)Math.ceil(i / size);

        Double[][] result = new Double[param.totalTaskType][numColumn];
        for (int k = 0; k < param.totalTaskType; k++){
            for (int j = 0; j < numColumn; j++) {
                result[k][j] = 0.0;
            }
        }

        for (int task = 0; task < param.totalTaskType; task++) {
            for (int time = 0; time < numColumn; time++) {
                for (int offset = 0; offset < size; offset++) {
                    result[task][time] += rawData[task][time * size + offset];
                }
                result[task][time] = round(result[task][time] / size, 2);
            }
        }

        return result;

    }

    public void removeEmptyTask(loadparam param){

        //Check if there is any exogenous task
        int sum = 0;
        for (int i : param.hasExogenous) {
            sum += i;
        }
        if (sum == 0) {
            removeTask(param.EXOGENOUS_TASK, param);
            param.totalTaskType--;
        }


        //Check if there is any team communication task
        Set<Character> teamCommunication = new HashSet<>();
        for (Character c: param.teamComm) {
            if (!c.equals('N') && !teamCommunication.contains(c))
                teamCommunication.add(c);
        }

        if (!teamCommunication.contains('F')) {
            removeTask(param.TC_FULL_TASK, param);
            param.totalTaskType--;
        }


        if (!teamCommunication.contains('S')) {
            removeTask(param.TC_SOME_TASK, param);
            param.totalTaskType--;
        }

    }

    /*

    String[] taskName;
    Double[][][][] taskUtilization; //[operator][replication][task][time]

     */



    private void removeTask(int task, loadparam param){

        int numOp = taskUtilization.length;
        int numRep = taskUtilization[0].length;
        int numTask = taskUtilization[0][0].length;
        int numColumn = taskUtilization[0][0][0].length;

        String[] newTaskName = new String[numTask - 1];
        Double[][][][] newTaskUtilization = new Double[numOp][numRep][numTask - 1][numColumn];

        for (int op = 0; op < numOp; op++) {
            for (int rep = 0; rep < numRep; rep++) {
                for (int col = 0; col < numColumn; col++) {
                    for (int t = 0; t < task; t++) {
                        newTaskName[t] = taskName[t];
                        newTaskUtilization[op][rep][t][col] = taskUtilization[op][rep][t][col];
                    }

                    for (int t = task; t < numTask - 1; t++) {
                        newTaskName[t] = taskName[t + 1];
                        newTaskUtilization[op][rep][t][col] = taskUtilization[op][rep][t + 1][col];
                    }
                }
            }
        }


//        if (task == param.EXOGENOUS_TASK || task == param.TC_FULL_TASK) { //remove the last task
//            for (int op = 0; op < numOp; op++) {
//                for (int rep = 0; rep < numRep; rep++) {
//                    for (int col = 0; col < numColumn; col++) {
//                        for (int t = 0; t < numTask - 1; t++) {
//                            newTaskName[t] = taskName[t];
//                            newTaskUtilization[op][rep][t][col] = taskUtilization[op][rep][t][col];
//                        }
//                    }
//                }
//            }
//        }
//        else {
//            for (int op = 0; op < numOp; op++) {
//                for (int rep = 0; rep < numRep; rep++) {
//                    for (int col = 0; col < numColumn; col++) {
//                        for (int t = 0; t < task; t++) {
//                            newTaskName[t] = taskName[t];
//                            newTaskUtilization[op][rep][t][col] = taskUtilization[op][rep][t][col];
//                        }
//
//                        for (int t = task; t < numTask - 1; t++) {
//                            newTaskName[t] = taskName[t + 1];
//                            newTaskUtilization[op][rep][t][col] = taskUtilization[op][rep][t + 1][col];
//                        }
//                    }
//                }
//            }
//        }

        taskName = newTaskName;
        taskUtilization = newTaskUtilization;
    }

    /****************************************************************************
     *
     *	Method:     round
     *
     *	Purpose:    Round double numbers
     *
     ****************************************************************************/

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

}
