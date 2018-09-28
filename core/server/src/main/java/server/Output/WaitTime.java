package server.Output;
import server.Engine.Data;
import server.Input.loadparam;

import java.util.HashSet;
import java.util.Set;

/***************************************************************************
 *
 * 	FILE: 			WaitTime.java
 *
 * 	AUTHOR: 		Hanwiz
 *
 * 	LATEST EDIT:	08/31/2018
 *
 * 	VER: 			1.0
 *
 * 	Purpose: 		A class to records and manage the waitime.
 * 	                Also used for JSON output to the website.
 *
 **************************************************************************/

public class WaitTime {

    String[] teamName;
    int[]    teamSize;
    String[] operatorName;
    String[] taskName;
    String[] fleetName;
    
    Double[][][][] taskWaitTime; //[operator][replication][task][time]
    Double[][][] timeWaitTime; //[operator][replication][time]
    Double[][][][] fleetWaitTime; //[operator][replication][fleet][time]

    Double[][] averageWaitTimePerFleet; //[team][fleet]
    Double[][] avgWaitTimePerFleet; //[team][fleet] - for verification
    Double[][] stdWaitTimePerFleet; //[team][fleet]
    Double[][] averageWaitTimePerTask; //[team][task]    
    Double[][] avgWaitTimePerTask;
    Double[][] stdWaitTimePerTask; //[team][task]

    public Double[][] getWaitTime() { return avgWaitTimePerFleet; }
    public Double[][] getWaitTimePerTask() { return avgWaitTimePerTask; }
    
    /****************************************************************************
     *
     *	Shado Object:	WaitTime
     *
     *	Purpose:	Create a object to record the WaitTime
     *                  1. per operator
     *                  2. per replication
     *                  3. per task type
     *                  4. per 10-minutes interval
     *
     ****************************************************************************/

    public WaitTime(loadparam vars){

        int numOperators = vars.numRemoteOp + vars.flexTeamSize;
        int numTeams = vars.numTeams + vars.hasFlexPosition;

        taskName = vars.taskName_all;

        // get operators' name
        operatorName = new String[vars.numRemoteOp + vars.flexTeamSize];
        teamSize = new int[numTeams];

        int count = 0;
        for (int i = 0; i < vars.opNames.length; i++) {
            teamSize[i] = vars.teamSize[i];
            for (int j = 0; j < vars.teamSize[i]; j++) {
                operatorName[count] = vars.opNames[i] + " No." + Integer.toString(j+1);
                count++;
            }
            if (vars.AIDAtype[i][0] == 1) { // if equal operator
                operatorName[count-1] = "Equal Operator";
            }
        }
        if (vars.hasFlexPosition>0) {
            teamSize[vars.opNames.length] = vars.flexTeamSize;
        }

        for (int i = 0; i < vars.flexTeamSize; i++) {
            operatorName[count + i] = "FlexPosition No." + Integer.toString(i+1);            
        }

        fleetName = vars.fleetNames;
        teamName = vars.teamName_all;

        //create the WaitTime matrix and averageWaitTime matrix
        int numColumn = (int) Math.ceil(vars.numHours * 6);
        taskWaitTime = new Double[numOperators][vars.numReps][vars.totalTaskType][numColumn];
        timeWaitTime = new Double[numOperators][vars.numReps][numColumn];
        fleetWaitTime = new Double[numOperators][vars.numReps][vars.fleetTypes][numColumn];

        averageWaitTimePerFleet = new Double[numTeams][vars.fleetTypes];
        avgWaitTimePerFleet = new Double[numTeams][vars.fleetTypes];
        stdWaitTimePerFleet = new Double[numTeams][vars.fleetTypes];
        averageWaitTimePerTask = new Double[numTeams][vars.totalTaskType];
        avgWaitTimePerTask = new Double[numTeams][vars.totalTaskType];
        stdWaitTimePerTask = new Double[numTeams][vars.totalTaskType];

        for (int i = 0; i < numTeams; i++) {
            for (int j = 0; j < vars.fleetTypes; j++) {
                averageWaitTimePerFleet[i][j] = 0.0;
                avgWaitTimePerFleet[i][j] = 0.0;
                stdWaitTimePerFleet[i][j] = 0.0;
            }

            for (int j = 0; j < vars.totalTaskType; j++) {
                averageWaitTimePerTask[i][j] = 0.0;
                avgWaitTimePerTask[i][j] = 0.0;               
                stdWaitTimePerTask[i][j] = 0.0;
            }
        }

    }

    /****************************************************************************
     *
     *	Method:     fillTaskWaitTime, fillFleetWaitTime
     *
     *	Purpose:    Filled the WaitTime and averageWaitTime matrix based on
     *              WaitTimeOutput records.
     *
     ****************************************************************************/

    public void fillTaskWaitTime(int rep, Data[] taskU, loadparam param){

        int numColumn = (int) Math.ceil(param.numHours * 6);

        for (int op = 0; op < param.numRemoteOp + param.flexTeamSize; op++) {

            Data currentWaitTime = taskU[op];
            for (int task = 0; task < param.totalTaskType; task++) {
                for (int time = 0; time < numColumn; time++) {
                    double u = currentWaitTime.dataget(task, time, 0);
                    taskWaitTime[op][rep][task][time] = round(u,4);
                    if(task == 0) {
                        timeWaitTime[op][rep][time] = round(u,4);
                    }
                    else{
                        timeWaitTime[op][rep][time] += round(u,4);
                    }
                }
            }
        }
    }

    public void fillFleetWaitTime(int rep, Data[] fleetU, loadparam param) throws Exception {

        int numColumn = (int) Math.ceil(param.numHours * 6);

        for (int op = 0; op < param.numRemoteOp + param.flexTeamSize; op++) {

            Data currentWaitTime = fleetU[op];

            for (int fleet = 0; fleet < param.fleetTypes; fleet++) {
                for (int time = 0; time < numColumn; time++) {

                    double u = currentWaitTime.dataget(fleet, time, 0);

                    fleetWaitTime[op][rep][fleet][time] = round(u,4);
                }
            }
        }

    }

    /****************************************************************************
     *
     *	Method:     computeWaitTime
     *
     *	Purpose:    Fill the averageWaitTime and stdWaitTime matrix
     *
     ****************************************************************************/

    public void computeWaitTime(loadparam vars, int typeU){

        //define two type of typeU
        int TASK_RECORD = 1;
        //int FLEET_RECORD = 2;
        int numOperators = vars.numRemoteOp + vars.flexTeamSize;
        int numTeams = vars.numTeams + vars.hasFlexPosition;

        Double[][][][] WaitTime;
        Double[][] average;
        Double[][] std;
        double[][][] rawWaitTime;
        int length;

        if (typeU == TASK_RECORD) {
            WaitTime = taskWaitTime;
            average = averageWaitTimePerTask;
            std = stdWaitTimePerTask;
            length = vars.totalTaskType;
            rawWaitTime = new double[vars.numReps][numOperators][vars.totalTaskType];
            for (int team = 0; team < numTeams; team++) {
                for (int i = 0; i < length; i++) {
                    avgWaitTimePerTask[team][i] /= vars.numReps;
                    if (team == vars.numTeams) {
                        avgWaitTimePerTask[team][i] /= vars.flexTeamSize;
                    } else {
                        avgWaitTimePerTask[team][i] /= vars.teamSize[team];
                    }
                }
            }
        }
        else {
            WaitTime = fleetWaitTime;
            average = averageWaitTimePerFleet;
            std = stdWaitTimePerFleet;
            length = vars.fleetTypes;
            rawWaitTime = new double[vars.numReps][numOperators][vars.fleetTypes];

            // avgWaitTimePerFleet;
            for (int team = 0; team < numTeams; team++) {
                for (int i = 0; i < length; i++) {
                    avgWaitTimePerFleet[team][i] /= vars.numReps;
                    //avgWaitTimePerFleet[team][i] /= vars.allTaskTypes.size();
                }
            }
        }

        for(int op = 0; op < numOperators; op++) {
            for (int rep = 0; rep < vars.numReps; rep++) {
                for (int i = 0; i < length; i++) {
                    for (int time = 0; time < WaitTime[0][0][0].length; time++) {
                        rawWaitTime[rep][op][i] += WaitTime[op][rep][i][time] * 10;
                        int team = findTeam(op, vars.teamSize);
                        average[team][i] += WaitTime[op][rep][i][time] * 10;
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
                //else {
                //    average[team][i] /= vars.allTaskTypes.size();
                //}
            }
        }

        for (double[][] WaitTimeOneRep : rawWaitTime) {
            for (int op = 0; op < numOperators; op++) {
                int team = findTeam(op,vars.teamSize);
                for (int i = 0; i < length; i++) {
                    std[team][i] += Math.pow(WaitTimeOneRep[op][i] - average[team][i], 2);
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
     *	Purpose:    Return a WaitTime matrix for particular operator and
     *          	replication. The length of time, by which each column of this
     *          	matrix represent, can be customized using the size parameter.
     *
     *              e.g.    size        percentage WaitTime in
     *                       1                  10 min
     *                       2                  20 min
     *
     ****************************************************************************/

    public Double[][] timeSectionSum(loadparam param, int operator, int replication, int size) throws Exception{

        if (size > param.numHours * 6 || size < 1) {
            throw new Exception("Incorrect time interval size for WaitTime output");
        }

        Double[][] rawData = taskWaitTime[operator][replication];

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

    /****************************************************************************
     *
     *	Method:     removeEmptyTask
     *
     *	Purpose:    There are 5 types of special task. No matter whether they
     *              are exist in this simulation, they hold positions in those
     *              record matrx. This method is used to remove these position
     *              holder if this simulation doesn't have a certain type of
     *              special task.
     *
     ****************************************************************************/

    public void removeEmptyTask(loadparam param){

        // order of removing tasks should be in reverse order
        if (param.hasTurnOver[1] == 0) {
            removeTask(param.TURN_OVER_END_TASK, param);
            param.totalTaskType--;
        }

        if (param.hasTurnOver[0] == 0) {
            removeTask(param.TURN_OVER_BEGIN_TASK, param);
            param.totalTaskType--;
        }

        //Check if there is any exogenous task
        // int sum = 0;
        // for (int i : param.hasExogenous) {
        //     sum += i;
        // }
        if (param.hasExogenous[0] == 0) {
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

        int numOp = taskWaitTime.length;
        int numRep = taskWaitTime[0].length;
        int numTask = taskWaitTime[0][0].length;
        int numColumn = taskWaitTime[0][0][0].length;
        int numTeam = param.numTeams + param.hasFlexPosition;

        String[] newTaskName = new String[numTask - 1];
        Double[][][][] newTaskWaitTime = new Double[numOp][numRep][numTask - 1][numColumn];
        Double[][] newAverageWaitTimePerTask = new Double[numTeam][numTask - 1]; //[team][task]
        Double[][] newStdWaitTimePerTask = new Double[numTeam][numTask - 1]; //[team][task]


        for (int op = 0; op < numOp; op++) {
            for (int rep = 0; rep < numRep; rep++) {
                for (int col = 0; col < numColumn; col++) {
                    for (int t = 0; t < task; t++) {
                        newTaskName[t] = taskName[t];
                        newTaskWaitTime[op][rep][t][col] = taskWaitTime[op][rep][t][col];
                        for (int team = 0; team < numTeam; team++) {
                            newAverageWaitTimePerTask[team][t] = averageWaitTimePerTask[team][t];
                            newStdWaitTimePerTask[team][t] = stdWaitTimePerTask[team][t];
                        }
                    }

                    for (int t = task; t < numTask - 1; t++) {
                        newTaskName[t] = taskName[t + 1];
                        newTaskWaitTime[op][rep][t][col] = taskWaitTime[op][rep][t + 1][col];
                        for (int team = 0; team < numTeam; team++) {
                            newAverageWaitTimePerTask[team][t] = averageWaitTimePerTask[team][t + 1];
                            newStdWaitTimePerTask[team][t] = stdWaitTimePerTask[team][t + 1];
                        }
                    }
                }
            }
        }

        taskName = newTaskName;
        taskWaitTime = newTaskWaitTime;
        averageWaitTimePerTask = newAverageWaitTimePerTask;
        stdWaitTimePerTask = newStdWaitTimePerTask;
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
