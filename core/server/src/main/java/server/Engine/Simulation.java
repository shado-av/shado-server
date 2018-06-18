package server.Engine;
import server.Input.FileWizard;
import server.Input.loadparam;
import server.Output.ProcRep;
import server.Input.loadparam;

import java.io.*;
import java.util.ArrayList;

/***************************************************************************
 *
 * 	FILE: 			Simulation.java
 *
 * 	AUTHOR: 		ROCKY LI
 *
 * 	LATEST EDIT:	2017/9/12
 *
 * 	VER: 			1.1
 *
 * 	Purpose: 		Wraps the simulation, included intra-thread data processing to
 * 	                relieve cost on memory size.
 *
 **************************************************************************/

public class Simulation {

	private loadparam vars;

    private int[] expiredtaskcount;

    private int[] completedtaskcount;

    private Data[] operatoroutput;

    private Data[] RemoteOpoutput;

    private int repnumber;

    private int totalRemoteOp;

    private int numSpecialTasks = 0;



    public int[] getExpiredtask() {
        return expiredtaskcount;
    }

    public int[] getCompletedtaskcount() {
        return completedtaskcount;
    }

    public Data getOperatoroutput(int i) {
        return operatoroutput[i];
    }

    public Data getRemoteOpoutput(int i) {
        return RemoteOpoutput[i];
    }

    public Data[] getopsdata() { return operatoroutput; }

    public Data[] getdisdata() { return RemoteOpoutput; }

    /****************************************************************************
     *
     *	Shado Object:	Simulation
     *
     *	Purpose:		Create the simulation Object.
     *
     ****************************************************************************/

    public Simulation(loadparam param) {

        // Get overall vars

        vars = param;
        repnumber = param.numReps;
        System.out.println("NumReps: " + repnumber);
        getNumSpecialTasks();
        // Generate overall data field

        checkExogenousFactor();

        operatoroutput = new Data[param.numTeams];
        for (int i = 0; i < param.numTeams; i++) {
            operatoroutput[i] = new Data(param.numTaskTypes + numSpecialTasks, (int) param.numHours * 6, param.numReps);
        }
        setTotalRemoteOps();
        RemoteOpoutput = new Data[totalRemoteOp];
        for (int i = 0; i < totalRemoteOp; i++) {
            RemoteOpoutput[i] = new Data(param.numTaskTypes + numSpecialTasks, (int) param.numHours * 6, param.numReps);
        }

        expiredtaskcount = new int[param.numTaskTypes + numSpecialTasks];
        completedtaskcount = new int[param.numTaskTypes + numSpecialTasks];

    }

    /****************************************************************************
     *
     *	Method:			getNumSpecialTasks
     *
     *	Purpose:		Find the number of special task types
     *                  e.g. Coordination task level 1 & 2, Exogenous task
     *
     ****************************************************************************/


    private void getNumSpecialTasks(){
        numSpecialTasks = 2;
        return;
    }

    /****************************************************************************
     *
     *	Method:			processReplication
     *
     *	Purpose:		process a SINGLE replication, and then remove the reference.
     *
     ****************************************************************************/

    public void processReplication(int repID){

        Replication processed = new Replication(vars, repID);
        processed.run();
        vars.reps[repID] = processed;
//        vars.currRepnum = repID;
//        ProcRep process = new ProcRep(RemoteOpoutput, operatoroutput, processed);
//
//        process.run(repID);
//
//        for (int i = 0; i < vars.numTaskTypes; i++) {
//            expiredtaskcount[i] += process.getExpired()[i];
//            completedtaskcount[i] += process.getCompleted()[i];
//        }
    }

    /****************************************************************************
     *
     *	Method:			run
     *
     *	Purpose:		Run Simulation
     *
     ***************************************************************************/

    public void run() throws IOException {

        for (int i = 0; i < repnumber; i++) {

            //Run simulation
            processReplication(i);

            //Global tracker for current replication
            vars.replicationTracker ++;
        }
        //Data Processing for Replications
        for(int i = 0; i < repnumber; i++){
            ProcRep process = new ProcRep(RemoteOpoutput, operatoroutput, vars.reps[i],vars,numSpecialTasks);
            process.run(i);
            vars.utilizationOutput[i] = process.getRepdisdata();

            //Global Tracker for replication processed
            vars.currRepnum++;
            for (int j = 0; j < vars.numTaskTypes + numSpecialTasks; j++) {
                expiredtaskcount[j] += process.getExpired()[j];
                completedtaskcount[j] += process.getCompleted()[j];
            }
        }

        for (Data each: RemoteOpoutput){
            each.avgdata();
        }
        for (Data each: operatoroutput){
            each.avgdata();
        }
    }
    private void setTotalRemoteOps(){
        for(int i : vars.teamSize){
            totalRemoteOp += i;
        }
    }
    /*************************************************************************************
     *
     *	Method:			checkExogeneousFactor
     *
     *	Purpose:		modify the input parameters according to the exogeneous factor
     *
     **************************************************************************************/
    private void checkExogenousFactor(){
        if(vars.hasExogenous[0] == 1){
            int numExo = vars.hasExogenous[1];
            for(int i = 0; i < numExo; i++){
//                if (vars.exTypes[i].equals("add_task")) {
//                    addTask();
//                }
                if(vars.exTypes[i].equals("inc_arrival")){
                    changeArrivalRate(1.1);
                }
            }
        }
    }

    private void changeArrivalRate(Double changeRate){
        for(int i = 0; i < vars.arrPms.length; i++){
            for(int j = 0; j < vars.arrPms[0].length; j++){
                vars.arrPms[i][j] *= changeRate;
            }
        }
    }

//    private void addTask(){
//
//        loadparam vars_n = new loadparam();
//
//        vars_n.numHours = vars.numHours;
//        vars_n.traffic = vars.traffic;
//        vars_n.numReps = vars.numReps;
//        vars_n.numRemoteOp = vars.numRemoteOp;
//        vars_n.numTeams = vars.numTeams;
//        vars_n.autolvl = vars.autolvl;
//        vars_n.numPhases = vars.numPhases;
//        vars_n.hasExogenous = vars.hasExogenous;
//        vars_n.exNames = vars.exNames;
//        vars_n.exTypes = vars.exTypes;
//        vars_n.failThreshold = vars.failThreshold;
//        vars_n.opStrats = vars.opStrats;
//        vars_n.opNames = vars.opNames;
//        vars_n.teamComm = vars.teamComm;
//        vars_n.teamSize = vars.teamSize;
//        vars_n.fleetTypes = vars.fleetTypes + 1;
//        vars_n.numTaskTypes = vars.numTaskTypes + 1;
//
//
//
//        vars_n.numvehicles = new int[vars.numvehicles.length + 1];
//        vars_n.opTasks = new int[vars_n.numTeams][vars.opTasks[0].length + 1];
//        vars_n.fleetHetero;
//        vars_n.taskNames;
//        vars_n.taskPrty;
//
//        for(int i = 0; i < vars.numvehicles.length; i++){
//            vars_n.numvehicles[i] = vars.numvehicles[i];
//        }
//        vars_n.numvehicles[vars.numvehicles.length] = 1;
//
//        vars_n.numTaskTypes = vars.numTaskTypes + 1;
//        String[] taskNames_n = new String[vars.numTaskTypes];
//
//        //copy the original parameters
//        for(int i = 0; i < vars.numTaskTypes-1; i++){
//            taskNames_n[i] = vars.taskNames[i];
//        }
//
//        //add new parameters
//        taskNames_n[vars.numTaskTypes-1] = "Exogenous Event";
//
//        //replace the old vars with the new one

//    }


}
