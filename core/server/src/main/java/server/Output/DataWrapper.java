package server.Output;

import java.io.*;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import server.Engine.*;
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
 * 	                2.0     07/17/2018      Naixin Yu
 *
 * 	Purpose: 		Wrapping the data field for analysis.
 *
 **************************************************************************/

public class DataWrapper {

    public loadparam vars;

    private Simulation sim;

    private String outPutDirectory;

    public DataWrapper(Simulation o, loadparam param, String directory) {
        outPutDirectory = directory;
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
    public synchronized void outputReports() throws Exception {

        //Clean previous files in the output directory

        cleanDirectory();

        //Generate JSON files

        Utilization u = vars.utilization;
        TaskRecord t = vars.taskRecord;

        //Out put the report files

        printUtilization(u,1);
//        printValidationReport(u.getTaskUtilization());
        printSummaryReport();
        printTaskRecord();
        printErrorReport();
        externalTest(u);

        u.removeEmptyTask(vars);
        t.removeEmptyTask(vars);
        JasonBuilder builder = new JasonBuilder(outPutDirectory, u, t);
        builder.outputJSON();

    }

    private void externalTest(Utilization u) throws IOException{

        String file_name = System.getProperty("user.home") + "/Desktop/out/ExternalTest" + ".csv";
        PrintStream ps = new PrintStream(new BufferedOutputStream(
                new FileOutputStream(file_name, false)), true);

        ps.println("replication, utilization, waitTime");

        for(int rep = 0; rep < vars.numReps; rep++){

            double utilization = 0;
            for(int op = 0; op < vars.numRemoteOp; op++) {
                utilization += u.averageTaskUtilization[op][rep];
            }
            utilization /= vars.numRemoteOp;

            double waitTime = 0;
            for(Task t : vars.allTasksPerRep.get(rep)){
                waitTime += t.getWaitTime();
            }
            waitTime /= vars.allTasksPerRep.get(rep).size();

            ps.println(rep + ", " + utilization + "," + waitTime);
        }

        ps.close();

    }


    /****************************************************************************
     *
     *	Method:     cleanDirectory
     *
     *	Purpose:    Remove the original files in the output directory and create
     *              the needed directories under this folder
     *
     ****************************************************************************/

    //Naixin 07/12/2018
    private void cleanDirectory() throws  IOException{

        File mainDir = new File(outPutDirectory);
        if (!mainDir.exists()) {
            mainDir.mkdir();
        }
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
     *              2. Number of expired, failed and completed tasks for each task type
     *
     *
     ****************************************************************************/

    public void printSummaryReport() throws IOException {

        // Expired Tasks

        String file_name = outPutDirectory + "Summary/Simulation_Summary" + ".csv";

        PrintStream ps = new PrintStream(new BufferedOutputStream(
                new FileOutputStream(file_name, false)), true);
        ps.println("--- Simulation Summary---");

        //print total number of tasks in each replication

        ps.println("Tasks generated:");
        for (int i = 0; i < vars.numReps; i++) {
            ps.println(", Rep_" + i + "," + vars.repNumTasks[i]);
        }

        //print summary for each task type

        for(int i : vars.allTaskTypes){
            ps.println("Task name: " + vars.taskName_all[i]);

            int expiredTasks = 0;
            int failedTasks = 0;
            int completeTasks = 0;

            for (int rep = 0; rep < vars.numReps; rep++) {
                for (int phase = 0; phase < vars.numPhases; phase++) {
                    for (int team = 0; team < vars.numTeams; team++) {
                        expiredTasks += vars.taskRecord.getNumFailedTask()[rep][phase][team][i][0];
                        expiredTasks += vars.taskRecord.getNumFailedTask()[rep][phase][team][i][1];
                        failedTasks += vars.taskRecord.getNumFailedTask()[rep][phase][team][i][2];
                        failedTasks += vars.taskRecord.getNumFailedTask()[rep][phase][team][i][3];
                        completeTasks += vars.taskRecord.getNumSuccessTask()[rep][phase][team][i];
                    }
                }
            }

            ps.println(",Expired: " + expiredTasks);
            ps.println(",Failed: " + failedTasks);
            ps.println(",Completed: " + completeTasks);
        }

        ps.close();
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

            PrintStream ps = new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(summary_file_name, false)), true);

            ps.println("Fail Task Detail: ");
            ArrayList<Pair<Operator, Task>> failList = vars.rep_failTask.get(i);
            for (int k = 0; k < failList.size(); k++) {
                String opName = failList.get(k).getKey().getName();
                String tName = failList.get(k).getValue().getName();
                ps.print(opName + " Fails " + tName + ",");
                if (failList.get(k).getValue().getFail()) {
                    ps.print(" But still proceed by the Operator");
                }
                ps.println();
            }
            ps.close();
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

        for(int taskType : vars.allTaskTypes) {
            String fileName = outPutDirectory + "task_" + vars.taskName_all[taskType] + ".csv";
            PrintStream ps = new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(fileName, false)), true);
            ps.println("arrTime, beginTime, serveTime, waitTime, finTime, expireTime");
            for(int i = 0; i < vars.numReps; i++){
                ps.println("Replication " + i);
                for(Task t : vars.allTasksPerRep.get(i)){
                    if(t.getType() == taskType){
                        ps.println(t.getArrTime() + "," + t.getBeginTime() + "," +
                                t.getSerTime() + "," + t.getWaitTime() + "," + t.getEndTime() + "," + t.getExpTime());
                    }
                }
            }
            ps.close();
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
    private void printUtilization(Utilization u, int timeSize) throws Exception {

        // print utilization per operator
        for (int op = 0; op < vars.numRemoteOp + vars.flexTeamSize; op++) {

            double max10mins = 0; //max utiliazation in 10 mins across replications

            String fileName = outPutDirectory + "repCSV/Utilization_" + u.operatorName[op] + ".csv";
            PrintStream ps = new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(fileName, false)), true);

            // print utilization per repulication
            for (int rep = 0; rep < vars.numReps; rep++) {

                Double[][] utilization = u.timeSectionSum(vars, op, rep, timeSize);
                printUtilizationLabels(timeSize, rep, ps);
                max10mins = printUtilizationPerReplication(max10mins, utilization, ps);

                // print the sum of timeSectionSum
                ps.println(u.averageTaskUtilization[op][rep] + ",");
                ps.println(" ");

            }
            if (max10mins > 1.02) {
                throw new Exception("Simulation or Computation Error: max 10 mins utilization is greater than 1");
            }
            ps.println("The max utilization in 10 mins is " + max10mins);

            averageAll(u.averageTaskUtilization[op], ps);
            findMinMax(u.averageTaskUtilization[op], ps);

            ps.close();
        }



    }

    /****************************************************************************
     *
     *	Method:     printUtilizationPerReplication
     *
     *	Purpose:    Print the matrix of percentage utilization for a certain
     *              operator in a certain replication. Return the max percentage
     *              utilization among all time intervals.
     *
     ****************************************************************************/

    //Naixin 05/21/18
    private double printUtilizationPerReplication(double max10mins, Double[][] utilization, PrintStream ps) {

        int numColumn = utilization[0].length;

        Double[] timeSectionSum = new Double[numColumn];
        for (int i = 0; i < numColumn; i++) {
            timeSectionSum[i] = 0.0;
        }

        // one row per task
        for (int task : vars.allTaskTypes) {
            ps.print(vars.taskName_all[task] + ",");
            for (int time = 0; time < numColumn; time++) {
                Double percentage = utilization[task][time];
                timeSectionSum[time] += percentage;
                ps.print(percentage + ",");
            }
            ps.println(" ");
        }

        // print a line for timeSectionSum
        ps.print(",");

        for (int time = 0; time < numColumn; time++) {

            max10mins = Math.max(timeSectionSum[time], max10mins);

            ps.print(timeSectionSum[time] + ",");
        }

        return max10mins;

    }

    /****************************************************************************
     *
     *	Method:     printUtilizationLable
     *
     *	Purpose:    Print the labels for utilization output, including replication
     *              index and time intervals
     *
     ****************************************************************************/

    //Naixin 05/21/18
    private void printUtilizationLabels(int timeSize, int rep, PrintStream ps) {
        int numColumn = (int) Math.ceil((double)vars.numHours * 6 / timeSize);
        ps.print("Replication" + rep + ",");
        for(int col = 0; col < numColumn; col++){
            ps.print(String.valueOf(col * 10 * timeSize) + "~" + String.valueOf((col + 1) * 10 * timeSize) + " mins,");
        }
        ps.println("Average");
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

        for (int op = 0; op < vars.numRemoteOp + vars.flexTeamSize; op++) {

            String fileName = outPutDirectory + "validation/rep_vs_time:operator" + op + ".csv";
            PrintStream ps = new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(fileName, false)), true);


            for (int time = 0; time < numColumn; time++) {
                for (int rep = 0; rep < vars.numReps; rep++) {

                    Double utilization10min = 0.0;
                    for (int taskType : vars.allTaskTypes) {
                         utilization10min += utilization[op][rep][taskType][time];
                    }
                    ps.print(utilization10min + ",");
                }
                ps.println(" ");
            }

            ps.close();
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
    private void findMinMax (Double[] averageUtilization, PrintStream ps) {

        Double max = averageUtilization[0];
        Double min = averageUtilization[0];

        for (Double u : averageUtilization) {
            if (u > max) max = u;
            if (u < min) min = u;
        }

        ps.println("The max average utilization cross replication is " + max);
        ps.println("The min average utilization cross replication is " + min);

    }


    /****************************************************************************
     *
     *	Method:     averageAll
     *
     *	Purpose:    Find and print the average utilization across replication
     *
     ****************************************************************************/

    //Naixin 07/04/2018
    private void averageAll(Double[] u, PrintStream ps){

        int count = 0;
        double sum = 0;
        for (int i = 0; i < u.length; i++) {
                sum += u[i];
                count++;
        }
        ps.println("Average Utilization is " + sum / count);

    }

}

