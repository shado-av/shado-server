package server.Output;
import server.Input.loadparam;

public class FailedTask {

    String[] operatorName;
    String[] taskName;
    int[][][][][] numFailedTask; //[replication][phase][team][task type][4 kinds of failed tasks]

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

    public FailedTask(loadparam vars){

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

        //create the matrix for failed task

        numFailedTask = new int[vars.numReps][vars.numPhases][vars.numTeams][vars.totalTaskType][4];

    }

    public int[][][][][] getNumFailedTask() {
        return numFailedTask;
    }

    @Override
    public String toString() {
        System.out.println("1. # replications: " + numFailedTask.length);
        System.out.println("2. # phases: " + numFailedTask[0].length);
        System.out.println("3. # team: " + numFailedTask[0][0].length);
        System.out.println("4. # task: " + numFailedTask[0][0][0].length);
        return " ";
    }
}
