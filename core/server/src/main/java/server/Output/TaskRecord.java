package server.Output;
import server.Input.loadparam;

public class TaskRecord {

    private String[] operatorName;
    private String[] taskName;
    private int[][][][][] numFailedTask; //[replication][phase][team][task type][4 kinds of failed tasks]
    private int [][][][] numSuccessTask; //[replication][phase][team][task type]
    private int [][][][] numTotalTask; //[replication][phase][team][task type]

//TODO: update the comments here
    /****************************************************************************
     *
     *	Shado Object:	TaskRecods
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

    public TaskRecord(loadparam vars){

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

        //create the matrix for task records

        numFailedTask = new int[vars.numReps][vars.numPhases][vars.numTeams][vars.totalTaskType][4];
        numSuccessTask = new int[vars.numReps][vars.numPhases][vars.numTeams][vars.totalTaskType];
        numTotalTask = new int[vars.numReps][vars.numPhases][vars.numTeams][vars.totalTaskType];

    }

    public int[][][][][] getNumFailedTask() {
        return numFailedTask;
    }

    public int[][][][] getNumSuccessTask() { return numSuccessTask; }

    public void computeTotalTaskNumber(){

        int numRep = numFailedTask.length;
        int numPhase = numFailedTask[0].length;
        int numTeam = numFailedTask[0][0].length;
        int numTask = numFailedTask[0][0][0].length;

        for (int rep = 0; rep < numRep; rep++) {
            for (int phase = 0; phase < numPhase; phase++) {
                for (int team = 0; team < numTeam; team++) {
                    for (int task = 0; task < numTask; task++) {
                        numTotalTask[rep][phase][team][task] = 0;
                        int fail = 0;
                        for (int i = 0; i < 4; i++) {
                            fail += numFailedTask[rep][phase][team][task][i];
                        }
                        numTotalTask[rep][phase][team][task] += fail;
                        numTotalTask[rep][phase][team][task] += numSuccessTask[rep][phase][team][task];
                    }
                }
            }
        }

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

