package server.Engine;

import server.Input.loadparam;
import server.Output.ProcRep;
import java.io.*;


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

    private Data[] operatoroutput;

    private Data[] RemoteOpoutput;

    private int repnumber;

    private int numSpecialTasks = 3; //Team Coordinate Task (some), Team Coordinate Task (full), Exogenous Task

    public Data getRemoteOpoutput(int i) {
        return RemoteOpoutput[i];
    }


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

        System.out.println("Number of reputations: " + repnumber);
        // Generate overall data field

        operatoroutput = new Data[param.numTeams];
        for (int i = 0; i < param.numTeams; i++) {
            operatoroutput[i] = new Data(param.totalTaskType, (int) param.numHours * 6, param.numReps);
        }
        RemoteOpoutput = new Data[vars.numRemoteOp];
        for (int i = 0; i < vars.numRemoteOp; i++) {
            RemoteOpoutput[i] = new Data(param.totalTaskType, (int) param.numHours * 6, param.numReps);
        }

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
    }

    /****************************************************************************
     *
     *	Method:			run
     *
     *	Purpose:		Run Simulation
     *
     ***************************************************************************/

    public void run() throws Exception {

        for (int i = 0; i < repnumber; i++) {

            vars.refreshHumanErrorRate();

            //Run simulation
            processReplication(i);

            //Global tracker for current replication
            vars.replicationTracker ++;

        }

        //Data Processing for Replications
        for(int i = 0; i < repnumber; i++){
            ProcRep process = new ProcRep(RemoteOpoutput, vars.reps[i],vars,numSpecialTasks);
            process.run();
            vars.utilizationOutput[i] = process.getRepdisdata();

            //Global Tracker for replication processed
            vars.currRepnum++;

        }

        vars.taskRecord.computeTotalTaskNumber();
        vars.taskRecord.failedAnalysis();

        for (Data each: RemoteOpoutput){
            each.avgdata();
        }
        for (Data each: operatoroutput){
            each.avgdata();
        }
    }


}
