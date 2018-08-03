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
    String[] fleetName;
    Double[][][][] taskUtilization; //[operator][replication][task][time]
    Double[][][] timeUtilization; //[operator][replication][time]
    Double[][] averageTaskUtilization; //[operator][replication]
    Double[][][][] fleetUtilization; //[operator][replication][fleet][time]

    Double[][] averageBusyTimePerFleet; //[team][fleet]
    Double[][] stdBusyTimePerFleet; //[team][fleet]
    Double[][] averageBusyTimePerTask; //[team][task]
    Double[][] stdBusyTimePerTask; //[team][task]

    public Double[][] getBusyTime() { return averageBusyTimePerFleet; }

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

        int numOperators = vars.numRemoteOp + vars.flexTeamSize;
        int numTeams = vars.numTeams + vars.hasFlexPosition;

        taskName = vars.taskName_all;

        // get operators' name
        operatorName = new String[vars.numRemoteOp + vars.flexTeamSize];
        int count = 0;
        for (int i = 0; i < vars.opNames.length; i++) {
            for (int j = 0; j < vars.teamSize[i]; j++) {
                operatorName[count] = vars.opNames[i] + "_" + Integer.toString(j);
                count++;
            }
        }
        for (int i = 0; i < vars.flexTeamSize; i++) {
            operatorName[count + i] = "FlexPosition_" + Integer.toString(i);
        }

        fleetName = vars.fleetNames;
        // for (int i = 0; i < vars.fleetTypes; i++) {
        //     fleetName[i] = vars.fleetNames[i];
        // }

        //create the utilization matrix and averageUtilization matrix
        int numColumn = (int) Math.ceil(vars.numHours * 6);
        taskUtilization = new Double[numOperators][vars.numReps][vars.totalTaskType][numColumn];
        timeUtilization = new Double[numOperators][vars.numReps][numColumn];
        averageTaskUtilization = new Double[numOperators][vars.numReps];
        fleetUtilization = new Double[numOperators][vars.numReps][vars.fleetTypes][numColumn];

        averageBusyTimePerFleet = new Double[numTeams][vars.fleetTypes];
        stdBusyTimePerFleet = new Double[numTeams][vars.fleetTypes];
        averageBusyTimePerTask = new Double[numTeams][vars.totalTaskType];
        stdBusyTimePerTask = new Double[numTeams][vars.totalTaskType];

        for (int i = 0; i < numTeams; i++) {
            for (int j = 0; j < vars.fleetTypes; j++) {
                averageBusyTimePerFleet[i][j] = 0.0;
                stdBusyTimePerFleet[i][j] = 0.0;
            }

            for (int j = 0; j < vars.totalTaskType; j++) {
                averageBusyTimePerTask[i][j] = 0.0;
                stdBusyTimePerTask[i][j] = 0.0;
            }
        }

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

        for (int op = 0; op < param.numRemoteOp + param.flexTeamSize; op++) {

                double sum = 0;
                Data currentUtilization = taskU[op];
                for (int task = 0; task < param.totalTaskType; task++) {
                    for (int time = 0; time < numColumn; time++) {
                        double u = currentUtilization.dataget(task, time, 0);
                        taskUtilization[op][rep][task][time] = round(u,4);
                        if(task == 0) {
                            timeUtilization[op][rep][time] = round(u,4);
                        }
                        else{
                            timeUtilization[op][rep][time] += round(u,4);
                        }

                        sum += u;
                    }
                }
                sum /= param.numHours * 6;
                averageTaskUtilization[op][rep] = sum;

        }
    }

    public void fillFleetUtilization(int rep, Data[] fleetU, loadparam param) throws Exception {

        int numColumn = (int) Math.ceil(param.numHours * 6);

        for (int op = 0; op < param.numRemoteOp + param.flexTeamSize; op++) {

            Data currentUtilization = fleetU[op];

            for (int fleet = 0; fleet < param.fleetTypes; fleet++) {
                for (int time = 0; time < numColumn; time++) {

                    double u = currentUtilization.dataget(fleet, time, 0);

                    if (u > 1.02) {
                        throw new Exception("Simulation or Computation Error: max 10 mins utilization is greater than 1");
                    }

                    fleetUtilization[op][rep][fleet][time] = Math.min(round(u,4),1);

                }
            }
        }

    }

    /****************************************************************************
     *
     *	Method:     averageBusyTime
     *
     *	Purpose:
     *
     ****************************************************************************/

    public void utilizationToBusyTime(loadparam vars, int typeU){

        //define two type of typeU
        int TASK_RECORD = 1;
        int FLEET_RECORD = 2;
        int numOperators = vars.numRemoteOp + vars.flexTeamSize;
        int numTeams = vars.numTeams + vars.hasFlexPosition;

        Double[][][][] utilization;
        Double[][] average;
        Double[][] std;
        double[][][] rawBusyTime;
        int length;

        if (typeU == TASK_RECORD) {
            utilization = taskUtilization;
            average = averageBusyTimePerTask;
            std = stdBusyTimePerTask;
            length = vars.totalTaskType;
            rawBusyTime = new double[vars.numReps][numOperators][vars.totalTaskType];
        }
        else {
            utilization = fleetUtilization;
            average = averageBusyTimePerFleet;
            std = stdBusyTimePerFleet;
            length = vars.fleetTypes;
            rawBusyTime = new double[vars.numReps][numOperators][vars.fleetTypes];
        }

        for(int op = 0; op < numOperators; op++) {
            for (int rep = 0; rep < vars.numReps; rep++) {
                for (int i = 0; i < length; i++) {
                    for (int time = 0; time < utilization[0][0][0].length; time++) {
                        rawBusyTime[rep][op][i] += utilization[op][rep][i][time] * 10;
                        int team = findTeam(op, vars.teamSize);
                        average[team][i] += utilization[op][rep][i][time] * 10;
                    }
                }
            }
        }

        int[][] count = new int[numTeams][length];

        for (int team = 0; team < numTeams; team++) {
            for (int i = 0; i < length; i++) {
                count[team][i] = 0;
                average[team][i] /= vars.numReps;
                if (typeU == TASK_RECORD) {
                    if (team == vars.numTeams) {
                        average[team][i] /= vars.flexTeamSize;
                    } else {
                        average[team][i] /= vars.teamSize[team];
                    }
                }
                else {
                    average[team][i] /= vars.allTaskTypes.size();
                }
            }
        }

        for (double[][] busyTimeOneRep : rawBusyTime) {
            for (int op = 0; op < numOperators; op++) {
                int team = findTeam(op,vars.teamSize);
                for (int i = 0; i < length; i++) {
                    std[team][i] += Math.pow(busyTimeOneRep[op][i] - average[team][i], 2);
                    count[team][i]++;
                }
            }
        }

        for (int team = 0; team < numTeams; team++) {
            for (int i = 0; i < length; i++) {
                if (count[team][i] == 1) {
                    std[team][i] = 0.0;
                }
                else{
                    std[team][i] = round(Math.sqrt(std[team][i] / (count[team][i] - 1)), 2);
                }
            }
        }
    }

    public void averageBusyTime(loadparam vars){

        double[][][] rawBusyTime = new double[vars.numReps][vars.numRemoteOp + vars.flexTeamSize][vars.fleetTypes];

        for(int op = 0; op < vars.numRemoteOp + vars.flexTeamSize; op++) {
            for (int rep = 0; rep < vars.numReps; rep++) {
                for (int fleet = 0; fleet < vars.fleetTypes; fleet++) {
                    for (int time = 0; time < fleetUtilization[0][0][0].length; time++) {
                        rawBusyTime[rep][op][fleet] += fleetUtilization[op][rep][fleet][time] * 10;
                        int team = findTeam(op, vars.teamSize);
                        averageBusyTimePerFleet[team][fleet] += fleetUtilization[op][rep][fleet][time] * 10;
                    }
                }
            }
        }

        int[][] count = new int[vars.numTeams + vars.hasFlexPosition][vars.fleetTypes];

        for (int team = 0; team < vars.numTeams + vars.hasFlexPosition; team++) {
            for (int fleet = 0; fleet < vars.fleetTypes; fleet++) {
                count[team][fleet] = 0;
                averageBusyTimePerFleet[team][fleet] /= vars.numReps;
                if (team == vars.numTeams) {
                    averageBusyTimePerFleet[team][fleet] /= vars.flexTeamSize;
                }
                else{
                    averageBusyTimePerFleet[team][fleet] /= vars.teamSize[team];
                }
            }
        }

        for (double[][] busyTimeOneRep : rawBusyTime) {
            for (int op = 0; op < vars.numRemoteOp + vars.flexTeamSize; op++) {
                int team = findTeam(op,vars.teamSize);
                for (int fleet = 0; fleet < vars.fleetTypes; fleet++) {
                    stdBusyTimePerFleet[team][fleet] += Math.pow(busyTimeOneRep[op][fleet] - averageBusyTimePerFleet[team][fleet], 2);
                    count[team][fleet]++;
                }
            }
        }

        for (int team = 0; team < vars.numTeams + vars.hasFlexPosition; team++) {
            for (int fleet = 0; fleet < vars.fleetTypes; fleet++) {
                if (count[team][fleet] == 1) {
                    stdBusyTimePerFleet[team][fleet] = 0.0;
                }
                else{
                    stdBusyTimePerFleet[team][fleet] = Math.sqrt(stdBusyTimePerFleet[team][fleet] / (count[team][fleet] - 1));
                }
            }
        }


    }


    private int findTeam(int operator, int[] teamSize){

        int team = 0;
        for(; team < teamSize.length; team++){
            operator -= teamSize[team];
            if (operator < 0)
                return team;
        }

        return team;
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
                result[task][time] = round(result[task][time] / size, 4);
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

    private void removeTask(int task, loadparam param){

        int numOp = taskUtilization.length;
        int numRep = taskUtilization[0].length;
        int numTask = taskUtilization[0][0].length;
        int numColumn = taskUtilization[0][0][0].length;
        int numTeam = param.numTeams + param.hasFlexPosition;

        String[] newTaskName = new String[numTask - 1];
        Double[][][][] newTaskUtilization = new Double[numOp][numRep][numTask - 1][numColumn];
        Double[][] newAverageBusyTimePerTask = new Double[numTeam][numTask - 1]; //[team][task]
        Double[][] newStdBusyTimePerTask = new Double[numTeam][numTask - 1]; //[team][task]


        for (int op = 0; op < numOp; op++) {
            for (int rep = 0; rep < numRep; rep++) {
                for (int col = 0; col < numColumn; col++) {
                    for (int t = 0; t < task; t++) {
                        newTaskName[t] = taskName[t];
                        newTaskUtilization[op][rep][t][col] = taskUtilization[op][rep][t][col];
                        for (int team = 0; team < numTeam; team++) {
                            newAverageBusyTimePerTask[team][t] = averageBusyTimePerTask[team][t];
                            newStdBusyTimePerTask[team][t] = stdBusyTimePerTask[team][t];
                        }
                    }

                    for (int t = task; t < numTask - 1; t++) {
                        newTaskName[t] = taskName[t + 1];
                        newTaskUtilization[op][rep][t][col] = taskUtilization[op][rep][t + 1][col];
                        for (int team = 0; team < numTeam; team++) {
                            newAverageBusyTimePerTask[team][t] = averageBusyTimePerTask[team][t + 1];
                            newStdBusyTimePerTask[team][t] = stdBusyTimePerTask[team][t + 1];
                        }
                    }
                }
            }
        }

        taskName = newTaskName;
        taskUtilization = newTaskUtilization;
        averageBusyTimePerTask = newAverageBusyTimePerTask;
        stdBusyTimePerTask = newStdBusyTimePerTask;
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
