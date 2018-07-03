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
 * 	AUTHOR: 		ROCKY LI
 *
 * 	DATE:			2017/6/5
 *
 * 	VER: 			1.0
 *
 * 	Purpose: 		Wrapping the data field for analysis.
 *
 **************************************************************************/

public class DataWrapper {

    public loadparam vars;

    private Simulation sim;

    private String outPutDirectory;

    private int numSpecialTasks;

    private int totalTaskNumber;

    private String[] taskNames;

    PrintStream stdout;

    public DataWrapper(Simulation o, loadparam param) {
        stdout = System.out;
//        outPutDirectory = "/Users/zhanglian1/Desktop/out/";
        outPutDirectory = "/home/rapiduser/out/";
        vars = param;
        sim = o;
        numSpecialTasks = o.getNumSpecialTasks();
        totalTaskNumber = vars.numTaskTypes + vars.leadTask.length + numSpecialTasks;
        collectTaskNames();

    }


    private void collectTaskNames(){
        String[] specialTaskName = {"TC task (some)", "TC task (full)", "Exogenous task"};
        taskNames = new String[totalTaskNumber];
        for(int i = 0; i < totalTaskNumber; i++){
            if(i < vars.numTaskTypes){
                taskNames[i] = vars.taskNames[i];
            }
            else if (i < vars.numTaskTypes + vars.leadTask.length){
                taskNames[i] = vars.taskNames_f[i - vars.numTaskTypes];
            }
            else {
                taskNames[i] = specialTaskName[i - vars.numTaskTypes - vars.leadTask.length];
            }
        }
    }



    /****************************************************************************
     *
     *	Method:     testOutput
     *
     *	Purpose:    Generate different reports
     *
     ****************************************************************************/

    public void testOutput() throws IOException {

        //Clean previous files in the output directory

        cleanDirectory();


        //Out put the report files

        printSummaryReport();
        printErrorReport();
        printTaskRecord();


        //Generate JSON files

        Utilization u = printUtilization();
        FailedTask f = printFailedTask();

        JasonBuilder builderUtilization = new JasonBuilder(outPutDirectory, u, f);
        builderUtilization.outputJSON();

    }

    private void cleanDirectory() throws  IOException{
        File summaryDir = new File(outPutDirectory + "Summary");
        File csvDir = new File( outPutDirectory + "repCSV");
        File validationDir = new File(outPutDirectory + "validation");

        FileUtils.cleanDirectory(summaryDir);
        FileUtils.cleanDirectory(csvDir);
        FileUtils.cleanDirectory(validationDir);
    }


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

        for(int i = 0; i < totalTaskNumber; i++){
            System.out.println(",Task name: " + taskNames[i]);
            System.out.println(",Expired: " + sim.getExpiredtask()[i]);
            System.out.println(",Completed: " + sim.getCompletedtaskcount()[i]);
        }

        //print failed task count

        System.out.println("*** FAILED TASKS ***");
//            System.out.println("Operator "+ p.getKey().getName()+" Failed: "+p.getValue().getName());
        for (int i = 0; i < vars.numReps; i++) {
            HashMap<Integer, Integer> failCnt = vars.failTaskCount;
            int currFailCnt = failCnt.get(i);
            System.out.println("In Replication " + i + ": " + "Number of Fail Tasks: " + currFailCnt);
        }

    }

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


    //Naixin 05/23/18
    private void printTaskRecord() throws IOException{

        //print task information per task
        System.out.println();
        for(int taskType = 0; taskType < vars.numTaskTypes; taskType++) {
            String fileName = outPutDirectory + "task_" + vars.taskNames[taskType] + ".csv";
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


    //Naixin 05/21/18
    private Utilization printUtilization() throws IOException {

        Utilization utilization = new Utilization(vars, taskNames);

        // print utilization per operator
        for (int k = 0; k < vars.numRemoteOp; k++) {

            double[][] utilization_for_Sammy = new double[vars.numReps][];
            double max = 0; //max average utilization across replications
            double min = 100; //min average utilization across replications
            double max10mins = 0; //max utiliazation in 10 mins across replications

            String fileName = outPutDirectory + "repCSV/Utilization_" + k + ".csv";
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(fileName, false)), true));

            int numColumn = (int) Math.ceil(vars.numHours * 6);
            double[][] utilizationSum = new double[vars.numReps][numColumn]; //the utilization for each operator per time interval per replication
                                                                             //It is same thing with timeSectionSum. It's just we need another output.

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
                for (int j = 0; j < vars.numTaskTypes + vars.leadTask.length + numSpecialTasks; j++) {
                    double taskSum = 0;
                    if(j < vars.numTaskTypes){
                        System.out.print(vars.taskNames[j] + ",");
                    }
                    else if(j >= vars.numTaskTypes && j < vars.numTaskTypes + vars.leadTask.length){
                        System.out.print(vars.taskNames_f[j - vars.numTaskTypes] + ",");
                    }
                    else if(j == vars.numTaskTypes + vars.leadTask.length){
                        System.out.print("Team Coordinate Task level some: ,");
                    }
                    else if(j == vars.numTaskTypes + vars.leadTask.length + 1){
                        System.out.print("Team Coordinate Task level full: ,");
                    }
                    else if(j == vars.numTaskTypes + vars.leadTask.length + 2){
                        System.out.print("Exogenous Task: ,");
                    }

                    for (int time = 0; time < numColumn; time++) {
                        double u = taskUtilization.dataget(j, time, 0);

                        utilization.utilization[k][i][j][time] = round(u,2);
//                        utilization.utilization[k][i][j][time] = u;

                        taskSum = taskSum + u;
                        timeSectionSum[time] += u;
                        utilizationSum[i][time] += u;

                        System.out.print(u + ",");
                    }
                    taskSum /= vars.numHours * 6;
                    timeSectionSum[numColumn] += taskSum;
                    System.out.print(taskSum + ",");
                    System.out.println(" ");
                }

                // print a line for timeSectionSum
                System.out.print(",");

                for (int time = 0; time < numColumn; time++) {
                    if(timeSectionSum[time] > max10mins){
                        max10mins = timeSectionSum[time];
                    }
                    System.out.print(timeSectionSum[time] + ",");
                }
                utilization_for_Sammy[i] = timeSectionSum;

                // print the sum of timeSectionSum
                System.out.print(timeSectionSum[numColumn] + ",");
                utilization.averageUtilization[k][i] = round(timeSectionSum[numColumn],2);

                // find the max and min average utilization
                if(timeSectionSum[numColumn] > max){
                    max = timeSectionSum[numColumn];
                }
                if(timeSectionSum[numColumn] < min){
                    min = timeSectionSum[numColumn];
                }

                System.out.println(" ");
                System.out.println(" ");
            }

            System.out.println("The max average utilization cross replication is " + max);
            System.out.println("The min average utilization cross replication is " + min);
            System.out.println("The max utilization in 10 mins is " + max10mins);

            fileName = outPutDirectory + "repCSV/Utilization_" + k + "_allTasks" + ".csv";
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(fileName, true)), true));

            for(int i = 1; i <= vars.numReps; i++){
                System.out.print(i + ",");
            }
            System.out.println(" ");

            for(int time = 0; time < numColumn; time++){
                for(int rep = 0; rep < vars.numReps; rep++){
                    System.out.print(utilizationSum[rep][time] + ",");
                }
                System.out.println(" ");
            }


            fileName = outPutDirectory + "validation/rep_vs_time:operator" + k + ".csv";
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(fileName, false)), true));
            for(int i = 0; i < vars.numReps; i++){
                for(int j = 0; j < numColumn; j++){
                    System.out.print(utilization_for_Sammy[i][j] + ",");
                }
                System.out.println(" ");
            }
        }

        System.setOut(stdout);

        return utilization;
    }

    private FailedTask printFailedTask() throws IOException{
        FailedTask f = new FailedTask(vars, taskNames);

        return f;
    }



    private void remoteOperatorTimeTable() throws IOException{
        for (int i = 0; i < vars.numRemoteOp; i++) {

            String file_name = outPutDirectory + "RemoteOperator" + i + ".csv";

            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(file_name, false)), true));

            sim.getRemoteOpoutput(i).outputdata();
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

}

