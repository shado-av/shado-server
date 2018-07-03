package server.Output;
import server.Input.loadparam;

public class FailedTask {

    String[] operatorName;
    String[] taskName;
    double[][][][][] numFailedTask; //[replication][phase][team][task type][4 kinds of failed tasks]

    /****************************************************************************
     *
     *	Shado Object:	FailedTask
     *
     *  Author: Naixin 07/02/2018
     *
     *	Purpose:	Create a object to record the number of failed task
     *                  1. per replication
     *                  2. per phase
     *                  3. per team
     *                  4. per task type
     *              Four types of failed tasks are record:
     *                  1. missed task (never start)
     *                  2. incomplete task (start but not finish)
     *                  3. failed task (failed and not caught)
     *                  4. failed task (failed and caught)
     *
     ****************************************************************************/

    public FailedTask(loadparam vars, String[] taskname){

        taskName = taskname;

        // get operators' name

        operatorName = new String[vars.numRemoteOp];
        for(int i = 0; i < vars.numRemoteOp; i++){
            operatorName[i] = vars.reps[0].getRemoteOp().getRemoteOp()[i].getName();
        }

        //create the matrix for failed task

        numFailedTask = new double[vars.numReps][vars.numPhases][vars.numTeams][vars.numTaskTypes][4];

    }

}
