package server.Output;

import java.io.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import server.Engine.*;
import server.Input.FileWizard;
import server.Input.loadparam;
import java.util.*;
import javafx.util.Pair;


/***************************************************************************
 *
 * 	FILE: 			DataWrapper.java
 *
 * 	CREATOR: 		ROCKY LI
 *
 * 	VER: 			1.0                     Rocky Li
 * 	                1.?                     Richard Chen
 * 	                2.0     07/06/2018      Naixin Yu
 *
 * 	Purpose: 		Wrapping the data field for analysis.
 *
 **************************************************************************/

public class DataWrapper {

    public loadparam vars;

    private Simulation sim;

    private String outPutDirectory;

    PrintStream stdout;

    public DataWrapper(Simulation o, loadparam param) {
        stdout = System.out;
        outPutDirectory = "/Users/zhanglian1/Desktop/out/";
//        outPutDirectory = "/home/rapiduser/out/";
        vars = param;
        sim = o;
    }

    /****************************************************************************
     *
     *	Method:     testOutput
     *
     *	Purpose:    Generate different reports
     *
     ****************************************************************************/

    //Naixin 07/06/2018
    public void outputReports() throws IOException {

        //Clean previous files in the output directory

        cleanDirectory();


        //Generate JSON files

        Utilization u = printUtilization();
        FailedTask f = vars.failedTask;

        JasonBuilder builderUtilization = new JasonBuilder(outPutDirectory, u, f);
        builderUtilization.outputJSON();


        //Out put the report files

        printSummaryReport();
        printErrorReport();
        printTaskRecord();
        printValidationReport(u.getUtilization());

    }


    /****************************************************************************
     *
     *	Method:     cleanDirectory
     *
     *	Purpose:    Remove the original files in the output directory and create
     *              the needed directories under this folder
     *
     ****************************************************************************/

    //Naixin 07/06/2018
    private void cleanDirectory() throws  IOException{

        File mainDir = new File(outPutDirectory);
        FileUtils.cleanDirectory(mainDir);

        File summaryDir = new File(outPutDirectory + "Summary");
        if (!summaryDir.exists()) {
            summaryDir.mkdir();
        }

        File csvDir = new File( outPutDirectory + "repCSV");
        if (!csvDir.exists()) {
            csvDir.mkdir();
        }

        File validationDir = new File(outPutDirectory + "validation");
        if (!validationDir.exists()) {
            validationDir.mkdir();
        }
    }


    /****************************************************************************
     *
     *	Method:     printSummaryReport
     *
     *	Purpose:    Output the summary report, including:
     *
     *              1. Total number of tasks generated in each replication
     *              2. Number of expired and completed tasks for each task type
     *              3. Number of failed tasks in each replication
     *
     ****************************************************************************/

    public void printSummaryReport() throws IOException {

        // Expired Tasks

        String file_name = outPutDirectory + "Summary/Simulation_Summary" + ".csv";

        System.setOut(new PrintStream(new BufferedOutputStream(
                new FileOutputStream(file_name, false)), true));
        System.out.println("--- Simulation Summary---");

        //print total number of tasks in each replication

        System.out.println("Tasks generated:");
        for (int i = 0; i < vars.numReps; i++) {
            System.out.println(", Rep_" + i + "," + vars.repNumTasks[i]);
        }

        //print summary for each task type

        for(int i = 0; i < vars.totalTaskType; i++){
            System.out.println("Task name: " + vars.taskName_all[i]);
            System.out.println(",Expired: " + sim.getExpiredtask()[i]);
            System.out.println(",Completed: " + sim.getCompletedtaskcount()[i]);
        }

        //print failed task count

        System.out.println("*** FAILED TASKS ***");
        for (int i = 0; i < vars.numReps; i++) {
            HashMap<Integer, Integer> failCnt = vars.failTaskCount;
            int currFailCnt = failCnt.get(i);
            System.out.println("In Replication " + i + ": " + "Number of Fail Tasks: " + currFailCnt);
        }

    }


    /****************************************************************************
     *
     *	Method:     printErrorReport
     *
     *	Purpose:    Output the error report for each replication, including:
     *
     *              1. Failed task name
     *              2. Operators who failed the task
     *              3. Whether this failed task was caught
     *
     ****************************************************************************/

    private void printErrorReport() throws IOException{
        for (int i = 0; i < vars.numReps; i++) {

            String summary_file_name = outPutDirectory + "Summary/Error_Summary_Rep_" + i + ".csv";

            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(summary_file_name, false)), true));
            System.out.println("Fail Task Detail: ");
            ArrayList<Pair<Operator, Task>> failList = vars.rep_failTask.get(i);
            for (int k = 0; k < failList.size(); k++) {
                String opName = failList.get(k).getKey().getName();
                String tName = failList.get(k).getValue().getName();
                System.out.print(opName + " Fails " + tName + ",");
                if (failList.get(k).getValue().getFail()) {
                    System.out.print(" But still proceed by the Operator");
                }
                System.out.println();
            }
        }
    }


    /****************************************************************************
     *
     *	Method:     printTaskRecord
     *
     *	Purpose:    Output the task record per replication for each task type.
     *
     *              Notes:
     *
     *              1. Contains regular task, followed task, team communication
     *                 task and exogenous task.
     *              2. Doesn't contain redo tasks, which are caused by catching
     *                 the failed tasks.
     *              3. For each task, recording: arrival time, begin time, serve
     *                 time, wait time, finish time and expected expire time
     *
     ****************************************************************************/


    //Naixin 05/23/18
    private void printTaskRecord() throws IOException{

        //print task information per task
        System.out.println();
        for(int taskType = 0; taskType < vars.totalTaskType; taskType++) {
            String fileName = outPutDirectory + "task_" + vars.taskName_all[taskType] + ".csv";
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(fileName, false)), true));
            System.out.println("arrTime, beginTime, serveTime, waitTime, finTime, expireTime");
            for(int i = 0; i < vars.numReps; i++){
                System.out.println("Replication " + i);
                for(Task t : vars.allTasksPerRep.get(i)){
                    if(t.getType() == taskType){
                        double waitTime = t.getEndTime() - t.getArrTime() - t.getSerTime();
                        waitTime = round(waitTime, 2);
                        System.out.println(t.getArrTime() + "," + t.getBeginTime() + "," + t.getSerTime() + "," + waitTime + "," + t.getEndTime() + "," + t.getExpTime());

                    }
                }
            }
            System.setOut(stdout);
        }
    }


    /****************************************************************************
     *
     *	Method:     printUtilization
     *
     *	Purpose:    Output the utilization record.
     *
     *              Notes:
     *
     *              1. Create the Utilization object for JSON output.
     *              2. Print a utilization spreadsheet for user reference, including:
     *
     *                  * utilization (percentage of busy time)
     *                      per operator
     *                      per replication
     *                      per task
     *                      per 10 mins time interval
     *
     *                  * max & min average utilization across replication
     *
     *                  * max 10 min interval utilization
     *
     *                  * overall average utilization
     *
     ****************************************************************************/

    //Naixin 05/21/18
    private Utilization printUtilization() throws IOException {

        Utilization utilization = new Utilization(vars);
        int numColumn = (int) Math.ceil(vars.numHours * 6);

        // print utilization per operator
        for (int k = 0; k < vars.numRemoteOp; k++) {

            double max10mins = 0; //max utiliazation in 10 mins across replications

            String fileName = outPutDirectory + "repCSV/Utilization_" + k + ".csv";
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(fileName, false)), true));

            // print utilization per repulication
            for (int i = 0; i < vars.numReps; i++) {

                // an extra column for the sum utilization of whole replication
                double[] timeSectionSum = new double[numColumn + 1];

                //get the utilization data for replication i, operator k
                Data taskUtilization = vars.utilizationOutput[i][k];

                //print the labels
                System.out.print("Replication" + i + ",");
                for(int col = 0; col < numColumn; col++){
                    System.out.print(String.valueOf(col * 10) + "~" + String.valueOf((col + 1) * 10) + " mins,");
                }
                System.out.println("Sum per task");

                // one row per task
                for (int j = 0; j < vars.totalTaskType; j++) {
                    double taskSum = 0;
                    System.out.print(vars.taskName_all[j] + ",");

                    for (int time = 0; time < numColumn; time++) {
                        double u = taskUtilization.dataget(j, time, 0);
                        utilization.utilization[k][i][j][time] = round(u,2);
                        taskSum += u;
                        timeSectionSum[time] += u;
                        System.out.print(u + ",");
                    }

                    taskSum /= vars.numHours * 6;
                    timeSectionSum[numColumn] += taskSum;
                    System.out.println(taskSum + ",");
                }

                // print a line for timeSectionSum
                System.out.print(",");

                for (int time = 0; time < numColumn; time++) {
                    if (timeSectionSum[time] > max10mins) {
                        max10mins = timeSectionSum[time];
                    }
                    System.out.print(timeSectionSum[time] + ",");
                }

                // print the sum of timeSectionSum
                System.out.print(timeSectionSum[numColumn] + ",");
                utilization.averageUtilization[k][i] = timeSectionSum[numColumn];

                System.out.println(" ");
                System.out.println(" ");
            }

            System.out.println("The max utilization in 10 mins is " + max10mins);

        }

        averageAll(utilization.averageUtilization);
        findMinMax(utilization.averageUtilization);

        System.setOut(stdout);

        return utilization;
    }


    /****************************************************************************
     *
     *	Method:     printValidationReport
     *
     *	Purpose:    Find and print the max & min average utilization
     *          	across replication
     *
     ****************************************************************************/

    //Naixin 06/30/2018
    private void printValidationReport(Double[][][][] utilization) throws IOException{

        int numColumn = (int) Math.ceil(vars.numHours * 6);

        for (int op = 0; op < vars.numRemoteOp; op++) {

            String fileName = outPutDirectory + "validation/rep_vs_time:operator" + op + ".csv";
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(fileName, false)), true));


            for (int time = 0; time < numColumn; time++) {
                for (int rep = 0; rep < vars.numReps; rep++) {

                    Double utilization10min = 0.0;
                    for (int taskType = 0; taskType < vars.totalTaskType; taskType++) {
                         utilization10min += utilization[op][rep][taskType][time];
                    }
                    System.out.print(utilization10min + ",");
                }
                System.out.println(" ");
            }


            System.setOut(stdout);
        }

    }


    /****************************************************************************
     *
     *	Method:     findMinMax
     *
     *	Purpose:    Find and print the max & min average utilization
     *          	across replication
     *
     ****************************************************************************/

    //Naixin Yu 07/06/2018
    private void findMinMax (Double[][] averageUtilization) {

        Double max = averageUtilization[0][0];
        Double min = averageUtilization[0][0];

        for (Double[] oneOperator : averageUtilization) {
            for (Double u : oneOperator) {
                if (u > max) max = u;
                if (u < min) min = u;
            }
        }

        System.out.println("The max average utilization cross replication is " + max);
        System.out.println("The min average utilization cross replication is " + min);

    }


    /****************************************************************************
     *
     *	Method:     averageAll
     *
     *	Purpose:    Find and print the average utilization across replication
     *
     ****************************************************************************/

    //Naixin 07/04/2018
    private void averageAll(Double[][] u){

        int count = 0;
        double sum = 0;
        for (int i = 0; i < u.length; i++) {
            for (int j = 0; j < u[0].length; j++) {
                sum += u[i][j];
                count++;
            }
        }
        System.out.println("Average Utilization is " + sum / count);

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

