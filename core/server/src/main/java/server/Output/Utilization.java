package server.Output;
import server.Engine.Data;
import server.Input.loadparam;

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
    Double[][][][] utilization; //[operator][replication][task][time];
    Double[][] averageUtilization; //[operator][replication];
    //Double[][][][] fleetUtilization; //[operator][fleet][replication][time]
    //Double[][][] averageFleetUtilization; //[operator][fleet][replication];


    public Double[][][][] getUtilization() { return utilization; }

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
        utilization = new Double[vars.numRemoteOp][vars.numReps][vars.totalTaskType][numColumn];
        averageUtilization = new Double[vars.numRemoteOp][vars.numReps];

        fillUtilization(vars);

    }

    /****************************************************************************
     *
     *	Method:     fillUtilization
     *
     *	Purpose:    Filled the utilization and averageUtilization matrix based on
     *              utilizationOutput records.
     *
     ****************************************************************************/

    private void fillUtilization(loadparam param){

        int numColumn = (int) Math.ceil(param.numHours * 6);

        for (int op = 0; op < param.numRemoteOp; op++) {
            for (int rep = 0; rep < param.numReps; rep++) {

                double sum = 0;
                Data taskUtilization = param.utilizationOutput[rep][op];

                for (int task = 0; task < param.totalTaskType; task++) {
                    for (int time = 0; time < numColumn; time++) {
                        double u = taskUtilization.dataget(task, time, 0);
                        utilization[op][rep][task][time] = round(u,2);
                        sum += u;
                    }
                }
                sum /= param.numHours * 6;
                averageUtilization[op][rep] = sum;
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

        Double[][] rawData = utilization[operator][replication];

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
