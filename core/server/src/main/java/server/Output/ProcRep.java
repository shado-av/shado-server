package server.Output;

import server.Engine.*;

import server.Input.loadparam;

import java.io.*;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

/***************************************************************************
 *
 * 	FILE: 			ProcRep.java
 *
 * 	AUTHOR: 		ROCKY LI
 * 	                Richard Chen
 *
 * 	DATE:			2017/9/10, 201712/2
 *
 * 	VER: 			1.0
 *
 * 	Purpose: 		Process each replication in Data individually
 *
 **************************************************************************/

public class ProcRep {

    private Data[] RemoteOpdata;

    private Data[] operatordata;

    private Replication rep;

    private VehicleSim[][] vehicles;

    private int repID;

    private int numoperator;

    private int numtasktypes;

    private double hours;

    private Data[] repdisdata;

    private Data[] repdisdata_s;

    private Data[] repopsdata;

    private int[] completed;

    private int[] expired;

    private int[] completed_specical;

    private int[] expired_special;

    private loadparam vars;

    private int totalRemoteOp;

    private int numSpecialTasks;
    // INSPECTORS

    public int[] getExpired() { return expired; }

    public int[] getCompleted() { return completed; }

    public Data[] getRepdisdata() { return repdisdata; }

    public String[] attributes = {"\t", "Average","Minimum","1st Quartile","Median",
            "3rd Quartile","Maximum", "Variance","Count of utilization 0-30%","Count of utilization 30%-70%","Count of utilization 70%-100%"};
    public String[] rowName = {"Workload","Error","Expired",""};

    /****************************************************************************
     *
     *	Shado Object:	ProcRep
     *
     *	Purpose:		Create a ProcRep Object with a Data object and Replication
     *                  object as input
     *
     ****************************************************************************/

    public ProcRep(Data[] dis, Data[] ops, Replication rep, loadparam vars, int SpecialTasks){

        this.rep = rep;
        RemoteOpdata = dis;
        operatordata = ops;
        vehicles = rep.getvehicles();
        repID = rep.getRepID();
        numoperator = rep.vars.numTeams;
        numtasktypes = rep.vars.numTaskTypes;
        hours = rep.vars.numHours;
        numSpecialTasks = SpecialTasks;
        expired = new int[numtasktypes + vars.leadTask.length + numSpecialTasks];
        completed = new int[numtasktypes + vars.leadTask.length + numSpecialTasks];
        //     [ normal task | followed task | special task  ]

        this.vars = vars;
//        totalRemoteOp = rep.vars.numRemoteOp;
        int totalRemoteOp = 0;
    }

    /****************************************************************************
     *
     *	Method:			tmpData
     *
     *	Purpose:		creating temporary Data object to be appended.
     *
     ****************************************************************************/

    public void tmpData(){

        repdisdata = new Data[totalRemoteOp];
        for (int i = 0; i < totalRemoteOp; i++){
            repdisdata[i] = new Data(numtasktypes + vars.leadTask.length + numSpecialTasks,(int) hours*6, 1);
        }

        repopsdata = new Data[numoperator];
        for (int i = 0; i < numoperator; i++) {
            for (int j = 0; j < rep.vars.fleetTypes; j++) {
                //TODO: it looks wried here, do you k is tha max length of all the vehicle types?
                repopsdata[i] = new Data(numtasktypes + vars.leadTask.length + numSpecialTasks, (int) hours * 6, vehicles[j].length);
            }
        }
    }

    /****************************************************************************
     *
     *	Method:			fillRepDataCell
     *
     *	Purpose:		Fill the Rep Data object with simulated data.
     *
     ****************************************************************************/

    public void fillRepDataCell(Operator operator, Data incremented){

        // Get Operator's task record.

        ArrayList<Task> records = operator.getQueue().records();

        // Cycle through records of each operator in 10 minutes intervals.

        for (Task each: records){

            //[ normal task | followed task | special task  ]

            int taskType = each.getType();

            if (taskType > vars.numTaskTypes){ // This is a followed task
                taskType = vars.numTaskTypes + (taskType % 100);
            }
            else if(taskType < 0){ // This is a special task
                taskType = vars.numTaskTypes + vars.leadTask.length - taskType - 1;
            }


            if (each.checkexpired()){
                expired[taskType]++;
                continue;
            } else {
                completed[taskType]++;
            }

            for(int i = 0; i < each.workSchedule.size(); i++){
                if (each.workSchedule.get(i)[0] >= each.workSchedule.get(i)[1]) {
                    continue;
                }
                double beginscale = each.workSchedule.get(i)[0] / 10;
                double endscale = each.workSchedule.get(i)[1] / 10;
                fill(beginscale, endscale, incremented, taskType);
            }
        }

    }


    private void fill(double beginscale, double endscale, Data incremented, int taskType){

        boolean startcheck = false;

        for (int i = 1; i < (int) hours*6 + 1; i++) {

            // If task hasn't began yet

            if (beginscale > i) {
                continue;
            }

            // If task began but not finished in this interval.

            if (endscale > i) {
                if (!startcheck) {
                    incremented.datainc(taskType, i - 1, 0, i - beginscale);
                    startcheck = true;
                } else {
                    incremented.datainc(taskType, i - 1, 0, 1);
                }
            } else {
                if (!startcheck) {
                    incremented.datainc(taskType, i - 1, 0, endscale - beginscale);
                    break;
                } else {
                    incremented.datainc(taskType, i - 1, 0, endscale - i + 1);
                    break;
                }
            }
        }

    }

    /****************************************************************************
     *
     *	Method:			fillRepData
     *
     *	Purpose:		Call fillRepDataCell on appropriate Data objects.
     *
     ****************************************************************************/

    public void fillRepData(){

        //SCHEN 11/29/17
        Operator[] RemoteOpers = rep.getRemoteOp().getRemoteOp();

        for (int i = 0; i < totalRemoteOp; i++){
            fillRepDataCell(RemoteOpers[i], repdisdata[i]);
        }

        for (Data each: repopsdata){
            each.avgdata();
        }

    }

    /****************************************************************************
     *
     *	Method:			appendData
     *
     *	Purpose:		Add the data of the replication to the main data field
     *
     ****************************************************************************/

    public void appendData(){

        // Process the RemoteOp data

        for (int i = 0; i < totalRemoteOp; i++){
            Data processed = RemoteOpdata[i];
            for (int x = 0; x < processed.data.length; x++){
                for (int y = 0; y < processed.data[0].length; y++){
                    processed.datainc(x, y, repID, repdisdata[i].dataget(x, y, 0));
                }
            }
        }

        // Process the operator data

        for (int i = 0; i < numoperator; i++){
            Data processed = operatordata[i];
            for (int x = 0; x < processed.data.length; x++){
                for (int y = 0; y < processed.data[0].length; y++){
                    processed.datainc(x, y, repID, repopsdata[i].avgget(x, y));
                }
            }
        }
    }

    /****************************************************************************
     *
     *	Method:			testClass
     *
     *	Purpose:		Test the ProcRep class.
     *
     ****************************************************************************/

    public void outputProcRep(int currRep) throws IOException {
        //get mapping for Operator->Num
        ArrayList<Integer>remoteNum = new ArrayList<>();
        ArrayList<Integer>remoteType = new ArrayList<>();
        for(int i = 0 ;i< vars.teamSize.length;i++){
           for(int j = 0; j < vars.teamSize[i]; j++){
              remoteNum.add(j);
              remoteType.add(i);
           }
        }
        System.out.println("repdisdata.size(): "+repdisdata.length);
        System.out.println(Arrays.toString(remoteNum.toArray()));

        int i = 0;
        for (Data each: repdisdata) {
            each.avgdata();
            String opName = vars.opNames[remoteType.get(i)]+"_"+remoteNum.get(i);
//            sepCSV(each, currRep, opName,remoteType.get(i),remoteNum.get(i));
            i++;
        }


    }

    /****************************************************************************
     *
     *	Method:			setTotalRemoteOps
     *
     *	Purpose:		Set the total number of operators.
     *
     ****************************************************************************/

    private void setTotalRemoteOps(){
        for(int i : vars.teamSize){
            totalRemoteOp += i;
        }
    }

    /****************************************************************************
     *
     *	Method:			Run
     *
     *	Purpose:		A wrapper that runs the ProcRep class.
     *
     ****************************************************************************/

    public void run(int currRep){
        setTotalRemoteOps();
        tmpData();
        fillRepData();
        appendData();
//       try{
//           outputProcRep(currRep);
//       }catch(Exception e){
//           System.out.println("JAVA IO EXCEPTION");
//           System.out.println(e);
//       };
    }
    /****************************************************************************
     *
     *	Method:			SepCSV
     *
     *	Purpose:		output CSV for every replication (per Operator, per Replication)
     *
     ****************************************************************************/
    public void sepCSV(Data RemoteOpout, int repNum,String opName,int opType,int opID)throws IOException{
//        String  file_head = FileWizard.getabspath();
        //SCHEN 11/30/17
        //Make RemoteOper dir if not exists
//        String directoryName = "/home/rapiduser/shado-server/core/server/out/repCSV/";
        String directoryName = "/Users/zhanglian1/shado-server/core/server/out/repCSV/";
        File directory = new File(directoryName);

        if (!directory.exists()){
            directory.mkdir();
        }

        String file_name = directoryName + "Op_"+opName+"_Rep_"+repNum+ ".csv";
        System.setOut(new PrintStream(new BufferedOutputStream(
                new FileOutputStream(file_name, false)), true));
        for(String s : attributes){
            System.out.print(s + ",");
        }
        System.out.println();
        for(String s :rowName){
            System.out.print(s +",");
            //Feed Data
            RemoteOpout.printMetaData(s,repNum,vars,this,opName,opType,opID);
            System.out.println();
        }
        //Print raw data
        System.out.println();
        System.out.println("\t\t---Raw data in 10 min interval---");
        RemoteOpout.outputdata();
    }

}
