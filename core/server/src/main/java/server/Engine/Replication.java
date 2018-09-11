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
 * 	AUTHOR: 		Erin Song, Rocky, Naixin Yu
 *
 * 	DATE:			2017/6/22, 2018/8.3
 *
 * 	VER: 			2.0
 *
 * 	Purpose: 		A wrapper that execute each replication.
 * 	                Rocky: Add styling and code streamlining.
 * 	                Naixin: Add turn over task, exogenous task, AI,
 * 	                team communication functions.
 *
 **************************************************************************/

public class Replication {

    private loadparam           vars;
    private int                 repID;
    private VehicleSim[][]      vehicles;
    private RemoteOp            remoteOps;
    private ArrayList<Task>     globalTasks;
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
     *	Method:		run
     *
     *	Purpose:	Run the simulation once given vars.
     *
     ****************************************************************************/

    public void run() throws Exception{

        // Initialize control center.

        globalTasks = new ArrayList<Task>();

        // add turn over task
        addTurnOverTask();

        // generate all the operators
        remoteOps = new RemoteOp(vars);
        remoteOps.genRemoteOp();

        // get the maximum array size among each fleet types
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

            // When the current phase ends before fin time, clear tasks
            if (vars.numPhases > 1 && op.checkPhase() == vars.numPhases - 2) {
                if (op.getQueue().getfinTime() > vars.phaseBegin[vars.numPhases - 1]) {
                    op.getQueue().clearTask(vars, op);
                }
            }

            // While finTime is under new task arrival time, complete the tasks
            while (op.getQueue().taskqueue.size() > 0 &&
                    op.getQueue().getfinTime() < task.getArrTime()) {
                op.getQueue().done(vars, op);
            }
        }

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

        // Turn Over tasks should be add to its corresponding operator

        if (task.getType() == vars.TURN_OVER_BEGIN_TASK || task.getType() == vars.TURN_OVER_END_TASK) {
            remoteOps.getRemoteOp()[task.getTeamType()].getQueue().add(task);
            return;
        }

        // Create a new arraylist of queue:

        ArrayList<Queue> proc = new ArrayList<Queue>();
        ArrayList<Operator> working = new ArrayList<>(proc.size());
        ArrayList<Operator> flexPosition = new ArrayList<>(vars.flexTeamSize);

        findAvaliableOperator(proc, working, task, flexPosition);

        if (working.size()==0) {
            task.setexpired();
            return;
        }

        Operator optimal_op = findOptimalOperator(working);

        //check if ET AIDA can help
        if (task.getType() < vars.numTaskTypes && !optimal_op.getQueue().taskqueue.isEmpty()) {
            int team = vars.ETteam[task.getType()][task.getVehicleID() / 100];
            if (team > -1){
                equalTeammateDone(task, team);
                return;
            }
        }

        //check if need flex position operator to help
        if(vars.hasFlexPosition == 1 && optimal_op.getBusyIn10min(task.getArrTime()) > 7){
            optimal_op = findOptimalOperator(flexPosition);
        }

        //AIs and team communication
        double errorChangeRate = 1;
        if(task.getType() < vars.numTaskTypes) {

            //AI feature: Individual Assistant AIDA
            errorChangeRate = applyIndividualAssistant(optimal_op, task);

            //check team communication, change the serve time and error rate
            task.changeServTime(getTeamComm(optimal_op.dpID, task.getType()));
            errorChangeRate *= getTeamComm(optimal_op.dpID, task.getType());

            // assign task priority according to team and task type
            if (optimal_op.dpID / 100 < vars.numTeams)
                task.setPriority(vars.taskPrty[optimal_op.dpID / 100][task.getType()]);
        }

        //If we have turn over task at the end, non-essential tasks will be removed to finish the turn over task
        if (vars.hasTurnOver[1] == 1 && vars.essential[task.getType()] == 0) {
            if ((task.getPhase() == vars.numPhases - 2 && optimal_op.getQueue().checkBlock()) ||
                    (task.getPhase() == vars.numPhases - 1 && vars.interruptable[task.getType()] == 0)) {
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

    private void findAvaliableOperator(ArrayList<Queue> proc, ArrayList<Operator> working, Task task, ArrayList<Operator> flexPosition){

        if(task.getType() == vars.TC_SOME_TASK || task.getType() == vars.TC_FULL_TASK){  // team coordination task, which can only be handled within certain team
            int operatorType = task.getTeamType();
            if(vars.AIDAtype[operatorType][2] == 1){ //If this team has Team Coordination Assistant, reduce the serve time by 50%
                task.changeServTime(0.5);
            }
            for(int j = 0; j < vars.numRemoteOp; j++ ){
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
            for (int j = 0; j < vars.numRemoteOp; j++) {

                Operator eachOperator = remoteOps.getRemoteOp()[j];
                if (eachOperator != null && vars.opExpertise[eachOperator.dpID / 100][task.getType()][task.getVehicleID() / 100] == 1) {
                    //Put task in appropriate Queue
                    proc.add(eachOperator.getQueue());
                    working.add(eachOperator);
                }

            }

        }

        if (vars.hasFlexPosition == 1) {
            for (int i = vars.numRemoteOp; i < remoteOps.getRemoteOp().length; i++) {
                flexPosition.add(remoteOps.getRemoteOp()[i]);
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
     ****************************************************************************/
    private void failTask(Operator operator,Task task, double changeRate){

        int taskType = task.getType();
        int teamType = operator.dpID / 100;

        double humanErrorRate = vars.humanErrorRate[taskType];
        double errorCatching;
        int affByTeamCoord;

        if (taskType >= vars.numTaskTypes) {//settings for special tasks
            //TODO: add human error rate here
            errorCatching = 0.5;
            affByTeamCoord = 0;
        }
        else {
            if(operator.dpID / 100 == vars.FLEXTEAM) {
                errorCatching = 0.5;
            }
            else{
                errorCatching = vars.ECC[teamType][taskType];
            }
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
            errorCatching = errorCatching * (2 - getTeamComm(operator.dpID, taskType));
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

        // first condition checks if this operator is a flex position operator
        if (op.dpID / 100 < vars.numTeams && vars.AIDAtype[op.dpID / 100][1] == 1 &&
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

    private double getTeamComm(int dpID, int taskType){

        int team = dpID / 100;

        if (team == vars.numTeams)
            return 1;

        double teamComm = 1;
        if(vars.teamCoordAff[taskType] == 0) return teamComm;
        if(vars.teamComm[team] == 'S') teamComm = 0.7;
        if(vars.teamComm[team] == 'F') teamComm = 0.3;
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

        if (type == vars.numTeams)
            return 1;

        double IAlvl = 1;
        if(vars.IALevel[type] == 'S') IAlvl = 0.7;
        if(vars.IALevel[type] == 'F') IAlvl = 0.3;
        return IAlvl;
    }

    /****************************************************************************
     *
     *	Method:		    addTurnOverTask
     *
     *	Purpose:	    Generate turn over task and add them to the
     *                  global task queue.
     *
     ****************************************************************************/

    private void addTurnOverTask() throws Exception{

        if (vars.hasTurnOver[0] == 1) {
            for (int i = 0; i < vars.numRemoteOp; i++) {
                Task newTask = new Task(vars.TURN_OVER_BEGIN_TASK, 0, vars, true, 0);
                newTask.setTeamType(i);
                globalTasks.add(newTask);
            }
        }

        if (vars.hasTurnOver[1] == 1) {
            for (int i = 0; i < vars.numRemoteOp; i++) {
                Task newTask = new Task(vars.TURN_OVER_END_TASK, 0, vars, true, 0);
                newTask.setTeamType(i);
                globalTasks.add(newTask);
            }
        }

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

        int taskType = vars.TC_SOME_TASK;
        if(level == 'S') taskType = vars.TC_SOME_TASK;
        if(level == 'F') taskType = vars.TC_FULL_TASK;

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



