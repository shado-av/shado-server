package server.Output;
import server.Engine.Data;
import server.Input.loadparam;

public class Utilization {

    loadparam param;
    String[] operatorName;
    String[] taskName;
    Double[][][][] utilization; //[operator][replication][task][time];
    Double[][] averageUtilization; //[operator][replication];


    public Double[][][][] getUtilization() { return utilization; }

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

    public Utilization(loadparam vars){

        param = vars;
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

        fillUtilization();

    }

    private void fillUtilization(){

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

    public Double[][] timeSectionSum(int operator, int replication, int size) throws Exception{

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
                    if (time * size + offset > rawData[0].length) {
                        break;
                    }
                    result[task][time] += rawData[task][time * size + offset];
                }
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
