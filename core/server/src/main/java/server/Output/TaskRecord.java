package server.Output;
import server.Input.loadparam;

public class TaskRecord {

    private String[]        teamName;
    private String[]        taskName;
    private int[][][][][]   numFailedTask; //[replication][phase][team][task type][4 kinds of failed tasks]
    private int [][][][]    numSuccessTask; //[replication][phase][team][task type]
    private int []          numTotalTask; // total # of succesful tasks, total # of each failed tasks
                                          // [success, missed, incompleted, failed but not caught, failed and caughgt]
    private double[][][]    averageFailed; //[team][task][4 kinds of failed tasks]
    private double[][][]    stdFailed; //[team][task][4 kinds of failed tasks]

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
     *                  4. average number of failed tasks and its standard deviation
     *
     *              Four types of failed tasks are record:
     *                  1. missed task (never start)
     *                  2. incomplete task (start but not finish)
     *                  3. failed task (failed and not caught)
     *                  4. failed task (failed and caught)
     *
     ****************************************************************************/

    public TaskRecord(loadparam vars){

        taskName = new String[vars.allTaskTypes.size()];
        int index = 0;
        for(int i : vars.allTaskTypes) {
            taskName[index] = vars.taskName_all[i];
            index++;
        }

        int teamCount = vars.numTeams + vars.hasFlexPosition;

        // get operators' name
        teamName = new String[teamCount];
        for (int i = 0; i < vars.opNames.length; i++) {
            teamName[i] = vars.opNames[i];
        }
        if (vars.hasFlexPosition == 1) {
            teamName[teamCount - 1] = "FlexPosition";
        }

        //create the matrix

        numFailedTask  = new int[vars.numReps][vars.numPhases][teamCount][vars.totalTaskType][4];
        numSuccessTask = new int[vars.numReps][vars.numPhases][teamCount][vars.totalTaskType];
        numTotalTask   = new int[5];
        averageFailed  = new double[teamCount][vars.totalTaskType][4];
        stdFailed      = new double[teamCount][vars.totalTaskType][4];

    }

    public int[][][][][] getNumFailedTask() { return numFailedTask; }

    public int[][][][] getNumSuccessTask() { return numSuccessTask; }


    /****************************************************************************
     *
     *	Method:     computeTotalTaskNumber
     *
     *	Purpose:    Compute the total number of tasks from the success task and
     *              failed task records.
     *
     ****************************************************************************/

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

    /****************************************************************************
     *
     *	Method:     failedAnalysis
     *
     *	Purpose:    Compute the average and std for failed tasks.
     *
     ****************************************************************************/

    public void failedAnalysis(){

        int numRep   = numFailedTask.length;
        int numPhase = numFailedTask[0].length;
        int numTeam  = numFailedTask[0][0].length;
        int numTask  = numFailedTask[0][0][0].length;

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
                    stdFailed[team][task][i] = numRep == 1 ? 0 : Math.sqrt(sum / (numRep - 1));
                }
            }
        }

    }


    public void removeEmptyTask(loadparam vars){

        int[][][][][] newNumFailedTask  = new int[vars.numReps][vars.numPhases][vars.numTeams][vars.allTaskTypes.size()][4];
        int[][][][]   newNumSuccessTask = new int[vars.numReps][vars.numPhases][vars.numTeams][vars.allTaskTypes.size()];
        double[][][]  newAverageFailed  = new double[vars.numTeams][vars.allTaskTypes.size()][4];
        double[][][]  newStdFailed      = new double[vars.numTeams][vars.allTaskTypes.size()][4];

        for (int rep = 0; rep < vars.numReps; rep++) {
            for (int phase = 0; phase < vars.numPhases; phase++) {
                for(int team = 0; team < vars.numTeams; team++) {
                    int count = 0;
                    for(int task : vars.allTaskTypes) {
                        for(int i = 0; i < 4; i++){
                            newNumFailedTask[rep][phase][team][count][i] = numFailedTask[rep][phase][team][task][i];
                            newAverageFailed[team][count][i] = averageFailed[team][task][i];
                            newStdFailed[team][count][i] = stdFailed[team][task][i];
                        }
                        newNumSuccessTask[rep][phase][team][count] = numSuccessTask[rep][phase][team][task];
                        count++;
                    }
                }
            }
        }

        numFailedTask = newNumFailedTask;
        numSuccessTask = newNumSuccessTask;
        averageFailed = newAverageFailed;
        stdFailed = newStdFailed;

    }

}

