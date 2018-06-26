package server.Engine;

import server.Engine.RemoteOp;
import server.Engine.Simulation;
import server.Engine.Task;
import server.Engine.VehicleSim;
import server.Input.loadparam;
import java.util.*;
import java.lang.*;
//import org.javatuples.Tuple;
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

    private ArrayList<Task> linked;

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
//                System.out.println("new optimal op: "+op.getName()+" with queue length: "+op.getQueue().taskqueue.size());
                if(op.getQueue().taskqueue.size() == optimal_op.getQueue().taskqueue.size()) {
                    if (Math.random() > 0.5)
                        optimal_op = op;
                }
                else
                    optimal_op = op;

            }
        }

        if(task.getType() > 0) {

            //AI feature: If the optimal operator is busy and there is ET AIDA for this task, use ET AIDA to process this task
            if (!optimal_op.getQueue().taskqueue.isEmpty()) {
                int team = vars.ETteam[task.getType()];
                if (team > -1){
                    equalTeammateDone(task, team);
                    return;
                }
            }

            //AI feature: Individual Assistant AIDA
            applyIndividualAssistant(optimal_op, task);

            //check team communication, change the serve time
            task.changeServTime(getTeamComm(optimal_op.dpID));
        }

        if (!failTask(optimal_op, task, optimal_op.dpID)) {

            if(task.getType() > 0){
                task.setPriority(vars.taskPrty[task.getPhase()][optimal_op.dpID / 100][task.getType()]);
                task.setTeamType(optimal_op.dpID / 100);
            }

            optimal_op.getQueue().add(task);
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
        //TODO: not apply the fail task part
        task.setBeginTime(task.getArrTime());
        task.changeServTime(vars.ETServiceTime[team]);
//        task.setELStime(task.getSerTime());
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

    private void applyIndividualAssistant(Operator op, Task task){
        if (vars.AIDAtype[op.dpID / 100][1] == 1 &&
                IntStream.of(vars.IAtasks[op.dpID / 100]).anyMatch(x -> x == task.getType())) {
            double changeRate = getIndividualAssistantLevel(op.dpID);
            task.changeServTime(changeRate);
        }
    }

    /****************************************************************************
     *
     *	Method:		    getTriangularDistribution
     *
     *	Purpose:	    generate a TriangularDistribution value for human error prediction
     *
     ****************************************************************************/
    private double getTriangularDistribution(int taskType, int Phase){

        double c = vars.humanError[Phase][taskType][1]; //mode
        double a = vars.humanError[Phase][taskType][0]; //min
        double b = vars.humanError[Phase][taskType][2]; //max

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
    private boolean failTask(Operator operator,Task task, int operatorID){

        //TODO: find the human error rate for team coordinate task, we are using the first task's fail rate to fail CT
        int taskType = Math.max(task.getType(), 0);

        int teamType = operatorID / 100;
        int Phase = task.getPhase();

        double distValue = getTriangularDistribution(taskType, Phase);

        double rangeMin = vars.humanError[Phase][taskType][0];
        double rangeMax = vars.humanError[Phase][taskType][2];
        Random r = new Random();
        double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();

        if(Math.abs(randomValue - distValue) <= 0.0001){
            HashMap<Integer,Integer> failCnt = vars.failTaskCount;
            int currCnt = failCnt.get(vars.replicationTracker);
            failCnt.put(vars.replicationTracker,++currCnt);
            this.failedTasks.add(new Pair <Operator,Task>(operator,task));

            //If there is team communication, it will be easier to catch error. Increase the ECC(Error Catch Chance)
            if(Math.random() > vars.ECC[teamType][taskType] * (2 - getTeamComm(operator.dpID))){
                //Task Failed but still processed by operator
                task.setFail();
                return false;
            }
            return true;
        }
        return false;
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
                    op.getQueue().getfinTime() < task.getArrTime()) { //Naixin: change getExpectedFinTime to getfinTime
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
//        linked = remoteOps.gettasks();

        int maxLen = 0;
        for(int i = 0; i < vars.fleetTypes; i++ )
            if(vars.numvehicles[i] > maxLen)
                maxLen = vars.numvehicles[i];

        vehicles = new VehicleSim[vars.fleetTypes][maxLen];

        for (int i = 0; i < vars.fleetTypes; i++) {
            for(int j = 0; j < vars.numvehicles[i]; j++) {

                vehicles[i][j] = new VehicleSim(vars,i*100 + j,remoteOps.getRemoteOp(),globalTasks,globalWatingTasks);
                vehicles[i][j].genVehicleTask();

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

//        for(Task t : globalTasks){
//            System.out.println(t.getArrTime() + " : " + t.getName());
//        }

        if(vars.replicationTracker == 0){
            vars.allTasks = globalTasks;
        }
        else{
            for(Task a : globalTasks){
                vars.allTasks.add(a);
            }
        }

        for (Task task : globalTasks) {
            workingUntilNewTaskArrive(remoteOps,task);
            puttask(task);
            vars.metaSnapShot++;
        }
        // Finish all remaining tasks
        workingUntilNewTaskArrive(remoteOps,null);

        vars.rep_failTask.put(vars.replicationTracker,this.failedTasks);

    }

    private double getTeamComm(int dpID){
        int type = dpID / 100;
        double teamComm = 1;
        if(vars.teamCoordAff[type] == 0) return teamComm;
        if(vars.teamComm[type] == 'S') teamComm = 0.7;
        if(vars.teamComm[type] == 'F') teamComm = 0.3;
        return teamComm;
    }

    private double getIndividualAssistantLevel(int dpId){
        int type = dpId / 100;
        double IAlvl = 1;
        if(vars.IALevel[type] == 'S') IAlvl = 0.7;
        if(vars.IALevel[type] == 'N') IAlvl = 0.3;
        return IAlvl;
    }

    private void genTeamCommTask(char level, int team){

        int taskType = -1;
        if(level == 'S') taskType = -1;
        if(level == 'F') taskType = -2;

        ArrayList<Task> indlist = new ArrayList<Task>();
        Task newTask = new Task(taskType, 0, vars, true);
        newTask.setTeamType(team);
        newTask.opNums[0] = team;
        indlist.add(newTask);

        while(newTask.getArrTime() < vars.numHours * 60){
            newTask = new Task(taskType, newTask.getArrTime(), vars, true);
            newTask.setTeamType(team);
            newTask.opNums[0] = team;
            indlist.add(newTask);
        }

        globalTasks.addAll(indlist);
        vars.repNumTasks[vars.replicationTracker]+= indlist.size();
    }

    private void genExoTask(){
        int taskType = -3;
        ArrayList<Task> indlist = new ArrayList<Task>();
        Task newTask = new Task(taskType, 0, vars, true);
        indlist.add(newTask);

        while(newTask.getArrTime() < vars.numHours * 60){
            newTask = new Task(taskType, newTask.getArrTime(), vars, true);
            indlist.add(newTask);
        }

        globalTasks.addAll(indlist);
        vars.repNumTasks[vars.replicationTracker]+= indlist.size();
    }

}



