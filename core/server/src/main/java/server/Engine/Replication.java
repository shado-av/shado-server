package server.Engine;

import server.Input.loadparam;
import java.util.*;
import java.lang.*;
import java.util.concurrent.BlockingQueue;
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

    //TEST: Multithreaded producer with global timing
    private BlockingQueue<Task> globalWatingTasks;

    private ArrayList<Pair <Operator,Task>> failedTasks;

    // Inspectors:

    public VehicleSim[][] getvehicles() {
        return vehicles;
    }

    public RemoteOp getRemoteOp() {
        return remoteOps;
    }

    public int getRepID() {
        return repID;
    }

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
        vars.failTaskCount.put(vars.replicationTracker,0);
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


        if(task.getType() == -1 || task.getType() == -2){  // team coordination task, which can only be handled within certain team
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
        else if(task.getType() == -3 || task.getType() > vars.numTaskTypes){ // exogenous task and followed tasks can be handled by all the operator
            for(int j = 0; j < remoteOps.getRemoteOp().length; j++){
                proc.add(remoteOps.getRemoteOp()[j].getQueue());
                working.add(remoteOps.getRemoteOp()[j]);
            }
        }
        else { // regular task. If the task can be operated by this operator, get his queue.
            for (int j = 0; j < remoteOps.getRemoteOp().length; j++) {
                if (remoteOps.getRemoteOp()[j] != null) {
                    if (IntStream.of(remoteOps.getRemoteOp()[j].taskType).anyMatch(x -> x == task.getType())) {
                        //Put task in appropriate Queue
                        proc.add(remoteOps.getRemoteOp()[j].getQueue());
                        working.add(remoteOps.getRemoteOp()[j]);
                    }
                }
            }
        }

        if(working.size()==0)
            return;

        //SCHEN 2/7 Fix: to get the shortest Queue of Operators
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

        // TODO: apply AI on followed tasks
        double errorChangeRate = 1;
        if(task.getType() > 0 && task.getType() < vars.numTaskTypes) {

            //AI feature: If the optimal operator is busy and there is ET AIDA for this task, use ET AIDA to process this task
            if (!optimal_op.getQueue().taskqueue.isEmpty()) {
                int team = vars.ETteam[task.getType()];
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
        }

        // check if the task is failed
        failTask(optimal_op, task, errorChangeRate);

        // assign task priority according to phase, team and task type
        task.setPriority(vars.taskPrty[task.getPhase()][optimal_op.dpID / 100][task.getType()]);
        task.setTeamType(optimal_op.dpID / 100);
        optimal_op.getQueue().add(task);

    }


    /****************************************************************************
     *
     *	Method:		    getTriangularDistribution
     *
     *	Purpose:	    generate a TriangularDistribution value for human error prediction
     *
     ****************************************************************************/
    private double getTriangularDistribution(double[] triangularParams){

        double c = triangularParams[1]; //mode
        double a = triangularParams[0]; //min
        double b = triangularParams[2]; //max

        double F = (c - a)/(b - a);
        double rand = Math.random();
        if (rand < F) {
            return a + Math.sqrt(rand * (b - a) * (c - a));
        } else {
            return b - Math.sqrt((1 - rand) * (b - a) * (b - c));
        }

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

        int taskType = Math.max(task.getType(), 0);
        int teamType = operator.dpID / 100;
        int Phase = task.getPhase();

        double[] humanErrorRate;
        double errorCatching;
        int affByTeamCoord;

        if (taskType < 0) {
            //TODO: find the human error rate for team coordinate task, we are using the first task's fail rate to fail CT
            humanErrorRate = new double[3];
            humanErrorRate[0] = 0.002;
            humanErrorRate[1] = 0.003;
            humanErrorRate[2] = 0.004;
            errorCatching = 0.5;
            affByTeamCoord = 0;
            taskType = vars.numTaskTypes + vars.leadTask.length - taskType - 1;
        }
        else {
            humanErrorRate = vars.humanError[Phase][taskType];
            errorCatching = vars.ECC[Phase][teamType][taskType];
            affByTeamCoord = vars.teamCoordAff[taskType];
        }


        // Modify the human error rate according to the changeRate
        for(int i = 0; i < task.getRepeatTimes(); i++){
            changeRate *= 0.5;
        }
        for (int i = 0; i < humanErrorRate.length; i++) {
            humanErrorRate[i] *= changeRate;
        }

        // Modify the error catching chance according to team coordination
        if (affByTeamCoord == 1) {
            errorCatching = errorCatching * (2 - getTeamComm(operator.dpID));
        }

        // Fail tasks according to humaan error rate

        double distValue = getTriangularDistribution(humanErrorRate);
        distValue = Math.max(distValue, 0.0001);

        if(Math.random() < distValue){
            HashMap<Integer,Integer> failCnt = vars.failTaskCount;
            int currCnt = failCnt.get(vars.replicationTracker);
            failCnt.put(vars.replicationTracker,++currCnt);
            this.failedTasks.add(new Pair <Operator,Task>(operator,task));

            if (Math.random() > errorCatching) {
                //Task failed but wasn't caught
                task.setFail();
                vars.failedTask.getNumFailedTask()[vars.replicationTracker][task.getPhase()][teamType][taskType][2]++;
                return;
            }

            //Task failed and caught
//            System.out.println("The " + task.getName() + " arrived at " + task.getArrTime() + " is failed and caught");
            vars.failedTask.getNumFailedTask()[vars.replicationTracker][task.getPhase()][teamType][taskType][3]++;
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
                if (each != null) {
                    while (each.getQueue().getfinTime() < totaltime) {
                        each.getQueue().done(vars,each);
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

            while (op.getQueue().getNumTask() > 0 &&
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

    public void run() {

        System.out.println("Curr Replication: " + vars.replicationTracker);

        // Initialize control center.

        //TODO 1.generate a global queue and can be modified
        globalTasks = new ArrayList<Task>();

        remoteOps = new RemoteOp(vars,globalTasks);
        remoteOps.run();

        int maxLen = 0;
        for(int i = 0; i < vars.fleetTypes; i++ )
            if(vars.numvehicles[i] > maxLen)
                maxLen = vars.numvehicles[i];

        vehicles = new VehicleSim[vars.fleetTypes][maxLen];

        for (int i = 0; i < vars.fleetTypes; i++) {
            for(int j = 0; j < vars.numvehicles[i]; j++) {

                vehicles[i][j] = new VehicleSim(vars,i*100 + j,remoteOps.getRemoteOp(),globalTasks,globalWatingTasks);
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
        //TODO: not apply the fail task part
        task.setBeginTime(task.getArrTime());
        task.changeServTime(vars.ETServiceTime[team]);
        task.setEndTime(task.getArrTime() + task.getSerTime());
        vars.AITasks.add(task);
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


    private void genTeamCommTask(char level, int team){

        int taskType = -1;
        if(level == 'S') taskType = -1;
        if(level == 'F') taskType = -2;

        ArrayList<Task> indlist = new ArrayList<Task>();
        Task newTask = new Task(taskType, 0, vars, true);
        if (newTask.getArrTime() < 0) return;
        newTask.setTeamType(team);
        indlist.add(newTask);

        while(newTask.getArrTime() < vars.numHours * 60){
            newTask = new Task(taskType, newTask.getArrTime(), vars, true);
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

    private void genExoTask(){
        int taskType = -3;
        ArrayList<Task> indlist = new ArrayList<Task>();
        Task newTask = new Task(taskType, 0, vars, true);
        if(newTask.getArrTime() < 0) return;
        indlist.add(newTask);

        while(newTask.getArrTime() < vars.numHours * 60){
            newTask = new Task(taskType, newTask.getArrTime(), vars, true);
            if(newTask.getArrTime() < 0) break;
            indlist.add(newTask);
        }

        globalTasks.addAll(indlist);
        vars.repNumTasks[vars.replicationTracker]+= indlist.size();
    }

}



