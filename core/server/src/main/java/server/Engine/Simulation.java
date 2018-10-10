package server.Engine;

import server.Input.loadparam;
import server.Output.ProcRep;


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

    //private Data[] operatorOutput;

    private Data[] RemoteOpOutput;


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

        // Generate overall data field

        // operatorOutput = new Data[param.numTeams];
        // for (int i = 0; i < param.numTeams; i++) {
        //     operatorOutput[i] = new Data(param.totalTaskType, (int) param.numHours * 6, param.numReps);
        // }
        RemoteOpOutput = new Data[vars.numRemoteOp + vars.flexTeamSize];
        for (int i = 0; i < vars.numRemoteOp + vars.flexTeamSize; i++) {
            RemoteOpOutput[i] = new Data(param.totalTaskType, (int) param.numHours * 6, param.numReps);
        }

    }


    /****************************************************************************
     *
     *	Method:			processReplication
     *
     *	Purpose:		process a SINGLE replication, and then remove the reference.
     *
     ****************************************************************************/

    public void processReplication(int repID) throws Exception{

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

        for (int i = 0; i < vars.numReps; i++) {

            vars.refreshHumanErrorRate();
            processReplication(i);
            vars.replicationTracker++;

        }

        //reset the replicationTracker to data processing for each replication
        vars.replicationTracker = 0;

        //Data Processing for Replications
        for(int i = 0; i < vars.numReps; i++){
            ProcRep process = new ProcRep(RemoteOpOutput, vars.reps[i], vars);
            process.run();
            vars.utilization.fillTaskUtilization(i, process.getUtilization_task(), vars);
            vars.utilization.fillFleetUtilization(i, process.getUtilization_fleet(), vars);
            vars.waitTime.fillTaskWaitTime(i, process.getWaitTime_task(), vars);
            vars.waitTime.fillFleetWaitTime(i, process.getWaitTime_fleet(), vars);

            //Global Tracker for replication processed
            vars.replicationTracker++;

        }

        vars.utilization.utilizationToBusyTime(vars,2);
        vars.utilization.utilizationToBusyTime(vars,1);
        vars.waitTime.computeWaitTime(vars,2);
        vars.waitTime.computeWaitTime(vars,1);        
        vars.taskRecord.computeTotalTaskNumber();
        vars.taskRecord.failedAnalysis();

        for (Data each: RemoteOpOutput){
            each.avgData();
        }
        // for (Data each: operatorOutput){
        //     each.avgdata();
        // }
    }


}
