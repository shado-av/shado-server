package server.Engine;

import server.Input.loadparam;
import java.util.*;
import java.lang.*;
import javafx.util.Pair;
import java.util.ArrayList;
import java.util.stream.IntStream;

/***************************************************************************
 *
 * 	FILE: 			Replication.java
 *
 * 	AUTHOR: 		Erin Song
 *
 * 	DATE:			2017/6/22
 *
 * 	VER: 			1.1
 *
 * 	Purpose: 		A wrapper that execute each replication.
 * 	                Styling and code streamlining are added by Rocky.
 *
 **************************************************************************/

public class Replication {

    public loadparam vars;

    private int repID;

    private VehicleSim[][] vehicles;

    private RemoteOp remoteOps;

    private ArrayList<Task> globalTasks;

    private ArrayList<Pair <Operator,Task>> failedTasks;

    // Inspectors:

    public RemoteOp getRemoteOp() { return remoteOps; }

    public int getRepID() { return repID; }


    /****************************************************************************
     *
     *	Shado Object:    Replication
     *
     *	Purpose:		The object that contains a simulation run and all its data.
     *
     ****************************************************************************/

    public Replication(loadparam param, int id) {
        vars = param;
        this.repID = id;
        failedTasks = new ArrayList<>();
    }

    /****************************************************************************
     *
     *	Method:			puttask
     *
     *	Purpose:		putting tasks into the operator that can operate the task
     * 					with the least queue.
     *
     ****************************************************************************/

    public void puttask(Task task) {

        // Create a new arraylist of queue:

        ArrayList<Queue> proc = new ArrayList<Queue>();
        ArrayList<Operator> working = new ArrayList<>(proc.size());

        findAvaliableOperator(proc, working, task);

        if (working.size()==0) {
            task.setexpired();
            return;
        }

        Operator optimal_op = findOptimalOperator(working);

        // apply AIs
        double errorChangeRate = 1;
        if(task.getType() < vars.numTaskTypes) {

            //AI feature: If the optimal operator is busy and there is ET AIDA for this task, use ET AIDA to process this task
            if (!optimal_op.getQueue().taskqueue.isEmpty()) {
                int team = vars.ETteam[task.getType()][task.getVehicleID() / 100];
                if (team > -1){
                    equalTeammateDone(task, team);
                    return;
                }
            }

            //AI feature: Individual Assistant AIDA
            errorChangeRate = applyIndividualAssistant(optimal_op, task);

            //check team communication, change the serve time and error rate
            task.changeServTime(getTeamComm(optimal_op.dpID));
            errorChangeRate *= getTeamComm(optimal_op.dpID);

            // assign task priority according to phase, team and task type
            task.setPriority(vars.taskPrty[task.getPhase()][optimal_op.dpID / 100][task.getType()]);
        }

        // In the second last phase, the tasks which cannot be complete within this phase will be stopped
        // and set to expired at the end of this phase
        if (task.getPhase() == vars.numPhases - 2 && optimal_op.getQueue().checkBlock()) {
            task.setexpired();
            vars.taskRecord.getNumFailedTask()[vars.replicationTracker][task.getPhase()][optimal_op.dpID / 100][task.getType()][0]++;
            return;
        }

        // Only the essential task and interruptable tasks can enter the last phase
        if (task.getPhase() == vars.numPhases - 1) {
            if (vars.essential[task.getType()] == 0 && vars.interruptable[task.getType()] == 0) {
                task.setexpired();
                vars.taskRecord.getNumFailedTask()[vars.replicationTracker][task.getPhase()][optimal_op.dpID / 100][task.getType()][0]++;
                return;
            }
        }

            // check if the task is failed
        failTask(optimal_op, task, errorChangeRate);

        task.setTeamType(optimal_op.dpID / 100);
        optimal_op.getQueue().add(task);

    }

    /****************************************************************************
     *
     *	Method:		    findAvaliableOperator
     *
     *	Purpose:	    According to the opExpertise, find a list of operator
     *                  who can do this task
     *
     ****************************************************************************/

    private void findAvaliableOperator(ArrayList<Queue> proc, ArrayList<Operator> working, Task task){

        if(task.getType() == vars.TC_SOME_TASK || task.getType() == vars.TC_FULL_TASK){  // team coordination task, which can only be handled within certain team
            int operatorType = task.getTeamType();
            if(vars.AIDAtype[operatorType][2] == 1){ //If this team has Team Coordination Assistant, reduce the serve time by 50%
                task.changeServTime(0.5);
            }
            for(int j = 0; j < remoteOps.getRemoteOp().length; j++ ){
                if(remoteOps.getRemoteOp()[j] != null && remoteOps.getRemoteOp()[j].dpID / 100 == operatorType) {
                    //Put task in appropriate Queue
                    proc.add(remoteOps.getRemoteOp()[j].getQueue());
                    working.add(remoteOps.getRemoteOp()[j]);
                }
            }
        }
        else if(task.getType() == vars.EXOGENOUS_TASK){ // exogenous task can be handled by all the operator
            for(int j = 0; j < remoteOps.getRemoteOp().length; j++){
                proc.add(remoteOps.getRemoteOp()[j].getQueue());
                working.add(remoteOps.getRemoteOp()[j]);
            }
        }
        else { // regular task. If the task can be operated by this operator, get his queue.
            for (int j = 0; j < remoteOps.getRemoteOp().length; j++) {
                Operator eachOperator = remoteOps.getRemoteOp()[j];
                if (eachOperator != null && vars.opExpertise[eachOperator.dpID / 100][task.getType()][task.getVehicleID() / 100] == 1) {
                    //Put task in appropriate Queue
                    proc.add(eachOperator.getQueue());
                    working.add(eachOperator);
                }
            }
        }

    }

    /****************************************************************************
     *
     *	Method:		    findOptimalOperator
     *
     *	Purpose:	    Return the operator who has the shortest queue in the
     *                  working list
     *
     ****************************************************************************/
    //SCHEN 2/7
    private Operator findOptimalOperator(ArrayList<Operator> working){

        Operator optimal_op = working.get(0);
        for(Operator op: working){
            if(op.getQueue().taskqueue.size() <= optimal_op.getQueue().taskqueue.size()){
                if(op.getQueue().taskqueue.size() == optimal_op.getQueue().taskqueue.size()) {
                    if (Math.random() > 0.5)
                        optimal_op = op;
                }
                else
                    optimal_op = op;
            }
        }
        return optimal_op;

    }

    /****************************************************************************
     *
     *	Method:		    failTask
     *
     *	Purpose:	    determined whethere the task is failed based on the failed param
     *                  add to a fail task map if fails
     *
     *                  NOT TOTALLY SURE, MAY BE FAIL TASKS IN HIGHER LEVEL
     ****************************************************************************/
    private void failTask(Operator operator,Task task, double changeRate){

        int taskType = task.getType();
        int teamType = operator.dpID / 100;
        int Phase = task.getPhase();

        double humanErrorRate = vars.humanErrorRate[Phase][taskType];
        double errorCatching;
        int affByTeamCoord;

        if (taskType >= vars.numTaskTypes) {//settings for special tasks
            errorCatching = 0.5;
            affByTeamCoord = 0;
        }
        else {
            errorCatching = vars.ECC[Phase][teamType][taskType];
            if (operator.isAI)
                errorCatching *= vars.ETFailThreshold[teamType];
            affByTeamCoord = vars.teamCoordAff[taskType];
        }

        // Modify the human error rate according to the changeRate
        for(int i = 0; i < task.getRepeatTimes(); i++){
            changeRate *= 0.5;
        }
        humanErrorRate *= changeRate;
        humanErrorRate = Math.max(humanErrorRate, 0.0001);//0.0001 is the minimum human error rate

        // Modify the error catching chance according to team coordination
        if (affByTeamCoord == 1) {
            errorCatching = errorCatching * (2 - getTeamComm(operator.dpID));
        }

        if(Math.random() < humanErrorRate){
            task.setFail();

            if (Math.random() > errorCatching) {
                //Task failed but wasn't caught
                vars.taskRecord.getNumFailedTask()[vars.replicationTracker][task.getPhase()][teamType][taskType][2]++;
                return;
            }

            //Task failed and caught
            vars.taskRecord.getNumFailedTask()[vars.replicationTracker][task.getPhase()][teamType][taskType][3]++;
            task.setNeedReDo(true);
        }

    }


    /****************************************************************************
     *
     *	Method:		    sortTask
     *
     *	Purpose:	    Sort the tasks in the globalTask in a timely order
     *
     ****************************************************************************/

    public void sortTask() {

        Collections.sort(globalTasks, (o1, o2) -> Double.compare(o1.getArrTime(), o2.getArrTime()));

    }

    /****************************************************************************
     *
     *	Method:		    workingUntilNewTaskArrive
     *
     *	Purpose:	    For each operator, complete all the tasks in its queue,
     *              	which has a end time earlier than the new task's arrival
     *              	time.
     *
     ****************************************************************************/

    public void workingUntilNewTaskArrive(RemoteOp remoteOp,Task task) throws NullPointerException{

        //if no new task arrive, finish the remained tasks in the queue
        if (task == null){
            double totaltime = vars.numHours * 60;
            for (Operator each : remoteOp.getRemoteOp()) {
                while (each != null && each.getQueue().taskqueue.peek() != null) {

                    if (each.getQueue().getfinTime() < totaltime) {
                        each.getQueue().done(vars, each);
                    }
                    else {
                        each.getQueue().clearTask(vars, each);
                    }
                }
            }
            return;
        }

        //When a new task is added, let operator finish all their tasks
        for(Operator op: remoteOp.getRemoteOp()) {
//            System.out.println("-------------------------------------------------------");
//            System.out.print(op.toString() + ", ");
//            System.out.println(op.getQueue().toString());

            if (vars.numPhases > 1 && op.checkPhase() == vars.numPhases - 2) {
                if (op.getQueue().getfinTime() > vars.phaseBegin[vars.numPhases - 1]) {
                    op.getQueue().clearTask(vars, op);
                }
            }

            while (op.getQueue().taskqueue.size() > 0 &&
                    op.getQueue().getfinTime() < task.getArrTime()) {
                op.getQueue().done(vars, op);
            }
        }

    }
    /****************************************************************************
     *
     *	Method:		run
     *
     *	Purpose:	Run the simulation once given vars.
     *
     ****************************************************************************/

    public void run() throws Exception{

        System.out.println("Curr Replication: " + vars.replicationTracker);

        // Initialize control center.

        globalTasks = new ArrayList<Task>();

        remoteOps = new RemoteOp(vars);
        remoteOps.genRemoteOp();

        int maxLen = 0;
        for(int i = 0; i < vars.fleetTypes; i++ )
            if(vars.numvehicles[i] > maxLen)
                maxLen = vars.numvehicles[i];

        vehicles = new VehicleSim[vars.fleetTypes][maxLen];

        // Generate all the vehicles
        for (int i = 0; i < vars.fleetTypes; i++) {
            for(int j = 0; j < vars.numvehicles[i]; j++) {
                vehicles[i][j] = new VehicleSim(vars,i*100 + j,remoteOps.getRemoteOp(),globalTasks);
                vehicles[i][j].taskgen();
            }
        }

        //Generate team communication tasks and exogenous task
        for(int i = 0; i < vars.numTeams; i++){
            if(vars.teamComm[i] == 'S') genTeamCommTask('S',i);
            if(vars.teamComm[i] == 'F') genTeamCommTask('F',i);
            if(vars.hasExogenous[0] == 1) genExoTask();
        }

        //Put all tasks in a timely order
        sortTask();

        vars.allTasksPerRep.add(globalTasks);

        for (Task task : globalTasks) {
            workingUntilNewTaskArrive(remoteOps,task);
            puttask(task);
        }
        // Finish all remaining tasks
        workingUntilNewTaskArrive(remoteOps,null);

        vars.rep_failTask.put(vars.replicationTracker,this.failedTasks);

    }


    /****************************************************************************
     *
     *	Method:		    equalTeammateDone
     *
     *	Purpose:	    A record for the tasks done by equal teammate AIDA
     *
     ****************************************************************************/

    private void equalTeammateDone(Task task, int team){

        //Create an ET operator
        Operator dummyOperator = new Operator(team * 100, "Equal Teammate", vars);
        dummyOperator.isAI = true;
        failTask(dummyOperator, task, vars.ETErrorRate[team]);
        //TODO: if AI failed task, add it to its own queue?
        task.setBeginTime(task.getArrTime());
        task.changeServTime(vars.ETServiceTime[team]);
        task.setEndTime(task.getArrTime() + task.getSerTime());
        vars.AITasks.add(task);
        vars.taskRecord.getNumSuccessTask()[vars.replicationTracker][task.getPhase()][team][task.getType()]++;
    }


    /****************************************************************************
     *
     *	Method:		    applyIndividualAssistant
     *
     *	Purpose:	    check if this operator has IA AIDA, if so reduce the
     *              	service time for certain tasks by 30%(some) or 70%(full)
     *
     ****************************************************************************/

    private double applyIndividualAssistant(Operator op, Task task){
        if (vars.AIDAtype[op.dpID / 100][1] == 1 &&
                IntStream.of(vars.IAtasks[op.dpID / 100]).anyMatch(x -> x == task.getType())) {
            double changeRate = getIndividualAssistantLevel(op.dpID);
            task.changeServTime(changeRate);
            return changeRate;
        }
        return 1;
    }

    /****************************************************************************
     *
     *	Method:		    getTeamComm
     *
     *	Purpose:	    return the change factor according to team coordination level
     *
     ****************************************************************************/

    private double getTeamComm(int dpID){

        int type = dpID / 100;
        double teamComm = 1;
        if(vars.teamCoordAff[type] == 0) return teamComm;
        if(vars.teamComm[type] == 'S') teamComm = 0.7;
        if(vars.teamComm[type] == 'F') teamComm = 0.3;
        return teamComm;

    }

    /****************************************************************************
     *
     *	Method:		    getIndividualAssistantLevel
     *
     *	Purpose:	    return the change factor according to Individual Assistant
     *                  AIDA level
     *
     ****************************************************************************/

    private double getIndividualAssistantLevel(int dpId){
        int type = dpId / 100;
        double IAlvl = 1;
        if(vars.IALevel[type] == 'S') IAlvl = 0.7;
        if(vars.IALevel[type] == 'F') IAlvl = 0.3;
        return IAlvl;
    }

    /****************************************************************************
     *
     *	Method:		    genTeamCommTask
     *
     *	Purpose:	    Generate team communication tasks and add them to the
     *                  global task queue.
     *
     ****************************************************************************/


    private void genTeamCommTask(char level, int team) throws Exception{

        int taskType = vars.numTaskTypes;
        if(level == 'S') taskType = vars.numTaskTypes;
        if(level == 'F') taskType = vars.numTaskTypes + 1;

        ArrayList<Task> indlist = new ArrayList<Task>();
        Task newTask = new Task(taskType, 0, vars, true, team * 100);
        if (newTask.getArrTime() < 0) return;
        newTask.setTeamType(team);
        indlist.add(newTask);

        while(newTask.getArrTime() < vars.numHours * 60){
            newTask = new Task(taskType, newTask.getArrTime(), vars, true, team * 100);
            newTask.setTeamType(team);
            if (newTask.getArrTime() < 0) break;
            indlist.add(newTask);
        }

        globalTasks.addAll(indlist);
        vars.repNumTasks[vars.replicationTracker]+= indlist.size();
    }


    /****************************************************************************
     *
     *	Method:		    genExoTask
     *
     *	Purpose:	    Generate exogenous tasks and add them to the
     *                  global task queue.
     *
     ****************************************************************************/

    private void genExoTask() throws Exception{
        int taskType = vars.numTaskTypes + 2;
        ArrayList<Task> indlist = new ArrayList<Task>();
        Task newTask = new Task(taskType, 0, vars, true,-1);
        if(newTask.getArrTime() < 0) return;
        indlist.add(newTask);

        while(newTask.getArrTime() < vars.numHours * 60){
            newTask = new Task(taskType, newTask.getArrTime(), vars, true,-1);
            if(newTask.getArrTime() < 0) break;
            indlist.add(newTask);
        }

        globalTasks.addAll(indlist);
        vars.repNumTasks[vars.replicationTracker]+= indlist.size();
    }

}



