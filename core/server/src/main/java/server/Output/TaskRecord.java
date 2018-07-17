package server.Output;
import server.Input.loadparam;

public class TaskRecord {

    private String[] operatorName;
    private String[] taskName;
    private int[][][][][] numFailedTask; //[replication][phase][team][task type][4 kinds of failed tasks]
    private int [][][][] numSuccessTask; //[replication][phase][team][task type]
    private int [] numTotalTask; // total # of succesful tasks, total # of each failed tasks
                                 // [success, missed, incompleted, failed but not caught, failed and caughgt]
    private double[][][] averageFailed; //[team][task][4 kinds of failed tasks]
    private double[][][] stdFailed; //[team][task][4 kinds of failed tasks]

    /****************************************************************************
     *
     *	Shado Object:	TaskRecods
     *
     *  Author: Naixin 07/17/2018
     *
     *	Purpose:	Create a object to record the number of task
     *                  1. per replication
     *                  2. per phase
     *                  3. per team
     *                  4. per task type
     *
     *              The records including:
     *                  1. number of failed tasks
     *                  2. number of success tasks
     *                  3. number of total task
     *
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

        //create the matrix
        numFailedTask = new int[vars.numReps][vars.numPhases][vars.numTeams][vars.totalTaskType][4];
        numSuccessTask = new int[vars.numReps][vars.numPhases][vars.numTeams][vars.totalTaskType];
        numTotalTask = new int[5];
        averageFailed = new double[vars.numTeams][vars.totalTaskType][4];
        stdFailed = new double[vars.numTeams][vars.totalTaskType][4];

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
                        for (int i = 0; i < 4; i++) {
                            numTotalTask[i+1] += numFailedTask[rep][phase][team][task][i];
                            averageFailed[team][task][i] += numFailedTask[rep][phase][team][task][i];
                        }
                        numTotalTask[0] += numSuccessTask[rep][phase][team][task];
                    }
                }
            }
        }

    }

    public void failedAnalysis(){

        int numRep = numFailedTask.length;
        int numPhase = numFailedTask[0].length;
        int numTeam = numFailedTask[0][0].length;
        int numTask = numFailedTask[0][0][0].length;

        //compute the average # of failed tasks over replication
        for (int team = 0; team < numTeam; team++) {
            for (int task = 0; task < numTask; task++) {
                for (int i = 0; i < 4; i++) {
                    averageFailed[team][task][i] /= numRep;
                }
            }
        }

        //compute the standard deviation
        for (int team = 0; team < numTeam; team++) {
            for (int task = 0; task < numTask; task++) {
                for (int i = 0; i < 4; i++) {
                    double sum = 0;
                    for (int rep = 0; rep < numRep; rep++) {
                        double temp = 0;
                        for (int phase = 0; phase < numPhase; phase++) {
                            temp += numFailedTask[rep][phase][team][task][i];
                        }
                        sum = sum + (temp - averageFailed[team][task][i]) * (temp - averageFailed[team][task][i]);
                    }
                    stdFailed[team][task][i] = Math.sqrt(sum / (numRep - 1));
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

