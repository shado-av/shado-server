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

        //If this is a team coordination task, which can only be handled within certain team
        if(task.getType() < 0){
            int operatorType = task.opNums[0]; //I save the operator type in opNums[0] for special tasks
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
        else {
            // If the task can be operated by this operator, get his queue.
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
            //The ET AIDA can process the task with 0 serve time ans 0 error (default, hard code for now)
            //TODO: make AI's serve time and error rate adjustable by user
            if (!optimal_op.getQueue().taskqueue.isEmpty()) {
                if (vars.hasET[task.getType()]) return;
            }

            //AI feature: check if this operator has IA AIDA, if so reduce the service time by 50%
            //TODO: only change the serve time for certain tasks, make the change rate adjustable
            if (vars.AIDAtype[optimal_op.dpID / 100][1] == 1) {
                task.changeServTime(0.5);
            }

            //check team communication, change the serve time
            task.changeServTime(getTeamComm(optimal_op.dpID));

        }
        //TODO: find the human error rate for team coordinate task, we are using the first task's fail rate to fail CT
        int taskType = Math.max(task.getType(),0);

        if (!failTask(optimal_op, task, taskType, getTriangularDistribution(taskType))) {
            optimal_op.getQueue().add(task);
        }

    }

    /****************************************************************************
     *
     *	Method:		    getTriangularDistribution
     *
     *	Purpose:	    generate a TriangularDistribution value
     *
     ****************************************************************************/
    private double getTriangularDistribution(int Type){
        double c = vars.humanError[Type][0];
        double a = vars.humanError[Type][1];
        double b = vars.humanError[Type][2];

        double F = (c - a)/(b - a);
        double rand = Math.random();
//        System.out.print("Triangular Distribution: ");
        if (rand < F) {
//            System.out.println( a + Math.sqrt(rand * (b - a) * (c - a)));
            return a + Math.sqrt(rand * (b - a) * (c - a));
        } else {
//            System.out.println( b - Math.sqrt((1 - rand) * (b - a) * (b - c)));
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
    private boolean failTask(Operator operator,Task task,int type, double distValue){

        double rangeMin = vars.humanError[type][1];
        double rangeMax = vars.humanError[type][2];
        Random r = new Random();
        double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
//        System.out.println("comparing" +distValue+" and "+randomValue);

        if(Math.abs(randomValue - distValue) <= 0.0001){
            HashMap<Integer,Integer> failCnt = vars.failTaskCount;
            int currCnt = failCnt.get(vars.replicationTracker);
            failCnt.put(vars.replicationTracker,++currCnt);
//            System.out.println(operator.getName()+" fails " +task.getName()+", Total Fail "+ currCnt);
            this.failedTasks.add(new Pair <Operator,Task>(operator,task));

            //If there is team communication, it will be easier to catch error. The failThreshold will decrease.
            if(Math.random() < vars.failThreshold * getTeamComm(operator.dpID)){
                //Task Failed but still processed by operator
                task.setFail();
                return false;
            }
            return true;
        }
        return false;
    }

    public void sortTask() {

        // Sort task by time.
        Collections.sort(globalTasks, (o1, o2) -> Double.compare(o1.getArrTime(), o2.getArrTime()));

    }

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

        //When a new task is added, let operator finish all there tasks
        for(Operator op: remoteOp.getRemoteOp()) {
            while (op.getQueue().getNumTask() > 0 &&
                    op.getQueue().getExpectedFinTime() < task.getArrTime()) {
                op.getQueue().done(vars, op);
//            System.out.println("--op "+op.getName()+"'s queue -1 , length == "+op.getQueue().taskqueue.size());
            }
        }
//        System.out.println("Operators working...");
    }
    /****************************************************************************
     *
     *	Method:		run
     *
     *	Purpose:	Run the simulation once given vars.
     *
     ****************************************************************************/

    public void run() {

        // Initialize control center.

        //TODO 1.generate a global queue and can be modified
        globalTasks = new ArrayList<Task>();

        remoteOps = new RemoteOp(vars,globalTasks);
        remoteOps.run();
//        linked = remoteOps.gettasks();

        //SCHEN 11/10/17 For this version of Fleet hetero, assume each batch has 10 vehicles
        int maxLen = 0;
        for(int i = 0; i < vars.fleetTypes; i++ )
            if(vars.numvehicles[i] > maxLen)
                maxLen = vars.numvehicles[i];

        vehicles = new VehicleSim[vars.fleetTypes][maxLen];

        for (int i = 0; i < vars.fleetTypes; i++) {
            for(int j = 0; j < vars.numvehicles[i]; j++) {
                // vehicleId to for 2d Array
                vehicles[i][j] = new VehicleSim(vars,i*100 + j,remoteOps.getRemoteOp(),globalTasks,globalWatingTasks);
//                System.out.println("Vehicle "+(i*100+j)+" generates tasks");
                vehicles[i][j].genVehicleTask();
            }
        }

        //TODO: generate team communication tasks and exogenous task
        for(int i = 0; i < vars.numTeams; i++){
            if(vars.teamComm[i] == 'S') genTeamCommTask('S',i);
            if(vars.teamComm[i] == 'F') genTeamCommTask('F',i);
            if(vars.hasExogenous[0] == 1) genExoTask();
        }

        //Put all tasks in a timely order
        sortTask();
//        System.out.println("Total Tasks: "+ globalTasks.size());

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
        System.out.println("Curr Replication: " + vars.replicationTracker);

    }

    private double getTeamComm(int dpID){
        int type = dpID / 100;
        double teamComm = 1;
        if(vars.teamComm[type] == 'S') teamComm = 0.7;
        if(vars.teamComm[type] == 'F') teamComm = 0.3;
        return teamComm;
    }

    private void genTeamCommTask(char level, int team){
        int taskType = -1;
        if(level == 'S') taskType = -1;
        if(level == 'F') taskType = -2;

        ArrayList<Task> indlist = new ArrayList<Task>();
        Task newTask = new Task(taskType, 0, vars, true);
        newTask.opNums[0] = team;
        indlist.add(newTask);

        while(newTask.getArrTime() < vars.numHours * 60){
            newTask = new Task(taskType, newTask.getArrTime(), vars, true);
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



