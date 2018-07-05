package server.Engine;
import server.Input.loadparam;
import javafx.util.Pair;

import java.util.*;

/***************************************************************************
 *
 * 	FILE: 			Queue.java
 *
 * 	AUTHOR: 		ROCKY LI
 *
 * 	DATE:			2017/6/5
 *
 * 	VER: 			1.1
 *
 * 	Purpose: 		Queue up each of the workers, and order tasks according
 * 	                to their priority and arrival time.
 *
 **************************************************************************/

public class Queue implements Comparable<Queue>{

    // The Queue is represented by a priority queue of task objects:

    public PriorityQueue<Task> taskqueue;

//    public Deque<Task> taskqueue;

    // Operator ID.

    public Operator operator;

    // Set the time to move forward with general time. (Tracer variable)

    private double time;

    // See if the Queue is populated or not

    private boolean isBusy;

    // Expected time to complete the current task in queue

    private double finTime;

    // Number of tasks in the queue

    private int NumTask;

    //Expected Finish Time, used when adding a new taks
    private double expectedFinTime;

    // Record all done tasks for data analysis

    private ArrayList<Task> recordtasks = new ArrayList<>();

    // inspectors:

    public ArrayList<Task> records() {
        return recordtasks;
    }

    public double getfinTime() {
        return finTime;
    }

    public double getExpectedFinTime(){ return expectedFinTime; }

    public int getNumTask() {
        return NumTask;
    }

    public boolean getStatus() {
        return isBusy;
    }

    // Mutator:

    public void SetTime(double Time) {
        this.time = Time;
    }

    public double getTime(){
        return this.time;
    }

    @Override
    public int compareTo(Queue other) {
        return this.NumTask - other.NumTask;
    }

    /****************************************************************************
     *
     *	Shado Object:	Queue
     *
     *	Purpose:		Create an empty queue at the start
     *
     ****************************************************************************/

    public Queue(Operator op) {
//        taskqueue = new ArrayDeque<>();
        taskqueue = new PriorityQueue<>();
        time = 0;
        finTime = Double.POSITIVE_INFINITY;
        this.operator = op;
        expectedFinTime = 0;
        numtask();
    }

    @Override
    public String toString() {
        System.out.println("My queue has " + getNumTask() + " tasks: ");
        printQueue();
//        if(!taskqueue.isEmpty()) System.out.println("The top task is " + taskqueue.peek().toString());
        return "The time is " + time + " , the finTime is " + finTime;
    }

    /****************************************************************************
     *
     *	Method:			Add
     *
     *	Purpose:		add a task to queue.
     *
     ****************************************************************************/

    public void add(Task task) {

        // Set the time of the queue to the arrival time of the task.

        SetTime(task.getArrTime());
        setExpectedFinTime(task);

        if(!taskqueue.isEmpty()){
            if(task.compareTo(taskqueue.peek()) < 0){ //the new task will go in front of the current top task
                System.out.println("In queue.add, the on hand task is interrupted.");
                taskqueue.peek().addInterruptTime(time);
                taskqueue.peek().setELStime(task.getArrTime() - taskqueue.peek().getBeginTime());
            }
        }

        taskqueue.add(task);

        // If the task is processed as first priority, i.e. began immediately, then:

        if (taskqueue.peek().equals(task)) {
            taskqueue.peek().setBeginTime(time);
            taskqueue.peek().addBeginTime(time);
            finTime();
        }

        // Else since the task is put behind in the queue, no other queue attribute need to change
        // except numTask.
        numtask();
    }


    /****************************************************************************
     *
     *	Method:			done
     *
     *	Purpose:		remove a finished task from the queue when its finished
     *					 and update queue attributes
     *
     ****************************************************************************/

    public void done(loadparam vars,Operator op) {

        // This if statement avoids error when calling done on an empty queue.

        if (taskqueue.peek() != null) {

            // Set the end time of the task being finished.

            taskqueue.peek().setEndTime(finTime);
            taskqueue.peek().addInterruptTime(finTime);
            taskqueue.peek().setWaitTime(finTime - taskqueue.peek().getArrTime() - taskqueue.peek().getSerTime());
//            taskqueue.peek().setQueue(NumTask);

            taskqueue.peek().printBasicInfo();

            Task currentTask = taskqueue.peek();

            // Remove the finished task from the queue and put it into record task list.
            recordtasks.add(taskqueue.poll());

            // Renew the queue time.
            SetTime(finTime);

            if(currentTask.getNeedReDo()){
                Task redoTask = new Task(currentTask);
                redoTask.setArrTime(finTime);
                System.out.print("Add the task " + redoTask.getName() + " arrive at " + redoTask.getArrTime() + " back to queue to redo ");
                System.out.println("task " + currentTask.getName() + " arrive at " + currentTask.getArrTime());
                add(redoTask);
            }

            System.out.println("Now " + toString());

        }

        // If there are ANOTHER task in the queue following the completion of this one:

        //Remove all the expired tasks
        while (taskqueue.peek() != null) {

            if (taskqueue.peek().getExpTime() > time) {
                break;
            }

            // Add expired tasks to the record
            taskqueue.peek().setexpired();

            int taskType = taskqueue.peek().getType();
            if (taskType < 0) {
                taskType = vars.numTaskTypes + vars.leadTask.length - taskType - 1;
            }
            else if (taskType > vars.numTaskTypes) {
                taskType = taskType % 100 + vars.numTaskTypes;
            }

            vars.failedTask.getNumFailedTask()[vars.replicationTracker][taskqueue.peek().getPhase()][op.dpID / 100][taskType][0]++;

//            System.out.println("The task " + taskqueue.peek().getName() + " arrive at " + taskqueue.peek().getArrTime() + " is expired.");
            vars.expiredTasks.get(vars.currRepnum).add(new Pair<>(op,taskqueue.peek()));
            recordtasks.add(taskqueue.poll());

        }

        if (taskqueue.peek() != null) {

            // Set the beginTime of the Task in queue to now, i.e. begin working on this task.

            taskqueue.peek().setBeginTime(time);
            taskqueue.peek().addBeginTime(time);
//            taskqueue.peek().setWaitTime(taskqueue.peek().getArrTime()-taskqueue.peek().getBeginTime());

        }

        // Generate a new finTime for the Queue.

        finTime();

        // Generate a new numTask for the Queue.

        numtask();
    }


    /****************************************************************************
     *
     *	Method:			finTime
     *
     *	Purpose:		calculate the finish time of the present task and return it
     *					as an attribute of the queue at current time.
     *
     ****************************************************************************/

    private void finTime() {

        // If there is no current task, the finTime will be infinite.

        if (taskqueue.peek() == null) {
            finTime = Double.POSITIVE_INFINITY;
//            finTime = 0;
        }

        // Otherwise grab the current task and return a finish time.

        else {
            Task onhand = taskqueue.peek();
            finTime = onhand.getBeginTime() + onhand.getSerTime() - onhand.getELSTime();
//            finTime = time + onhand.getSerTime() - onhand.getELSTime();
            // Error checker

//            System.out.println(onhand.getArrTime() + "\t" + onhand.getName() + "\t" +
//            onhand.getBeginTime() + "\t" + onhand.getEndTime());

        }
    }

    /****************************************************************************
     *
     *	Method:			numtask
     *
     *	Purpose:		return the number of tasks in the queue and if there are no
     *					task return state of the current queue as NOT BUSY.
     *
     ****************************************************************************/

    private void numtask() {

        NumTask = taskqueue.size();
        if (NumTask == 0) {
            isBusy = false;
        } else {
            isBusy = true;
        }
    }

    private void setExpectedFinTime(Task task){
        if(taskqueue.isEmpty()){
            expectedFinTime = task.getArrTime()+task.getSerTime();
        }
        else{
            if(task.getArrTime() < expectedFinTime){
                expectedFinTime += task.getSerTime();
            }else{
                // task arrive after all task are done
                expectedFinTime = task.getArrTime()+task.getSerTime();
            }

        }
    }

    private void printQueue(){
        Iterator<Task> it = taskqueue.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            System.out.print(t.getName() + "(" + t.getArrTime() + ")--");
        }
        System.out.println(" ");
    }

}
