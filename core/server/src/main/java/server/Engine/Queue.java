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
 * 	DATE:			07/09/2018
 *
 * 	VER: 			1.1
 * 	                2.0 Naixin Yu
 *
 * 	Purpose: 		Queue up each of the workers, and order tasks according
 * 	                to their priority and arrival time.
 *
 **************************************************************************/

public class Queue implements Comparable<Queue>{

    public  PriorityQueue<Task> taskqueue;
    public  Operator            operator;
    private double              time;
    private double              finTime;
    private boolean             blockLastSecondPhase;
    private ArrayList<Task>     recordtasks; // Record all done tasks for data analysis

    // inspectors:
    public ArrayList<Task>  records()      { return recordtasks; }
    public double           getfinTime()   { return finTime; }
    public double           getTime()      { return time; }
    public boolean          checkBlock()   { return blockLastSecondPhase; }

    // Mutator:
    public void             SetTime(double Time) {
        this.time = Time;
    }

    /****************************************************************************
     *
     *	Shado Object:	Queue
     *
     *	Purpose:		Create an empty queue at the start
     *
     ****************************************************************************/

    public Queue(Operator op) {

        taskqueue = new PriorityQueue<>();
        operator = op;
        time = 0;
        finTime = Double.POSITIVE_INFINITY;
        blockLastSecondPhase = false;
        recordtasks = new ArrayList<>();

    }

    @Override
    public int compareTo(Queue other) {
        return this.taskqueue.size() - other.taskqueue.size();
    }

    @Override
    public String toString() {
        System.out.println("My queue has " + taskqueue.size() + " tasks: ");
        printQueue();
        return "The time is " + time + " , the finTime is " + finTime;
    }

    private void printQueue(){
        Iterator<Task> it = taskqueue.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            System.out.print(t.getName() + "(" + t.getArrTime() + ")--");
        }
        System.out.println(" ");
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

        if(!taskqueue.isEmpty()){
            if(task.compareTo(taskqueue.peek()) < 0){ //the new task will go in front of the current top task
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

            taskqueue.peek().setEndTime(finTime);
            taskqueue.peek().addInterruptTime(finTime);
            taskqueue.peek().setWaitTime(finTime - taskqueue.peek().getArrTime() - taskqueue.peek().getSerTime());

//            taskqueue.peek().printBasicInfo();

            Task currentTask = taskqueue.peek();

            recordtasks.add(taskqueue.poll());

            SetTime(finTime);

            if(currentTask.getNeedReDo()){
                Task redoTask = new Task(currentTask);
                redoTask.setArrTime(finTime);
                add(redoTask);
            }
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

            vars.failedTask.getNumFailedTask()[vars.replicationTracker][taskqueue.peek().getPhase()][op.dpID / 100][taskType][0]++;
            vars.expiredTasks.get(vars.currRepnum).add(new Pair<>(op,taskqueue.peek()));
            recordtasks.add(taskqueue.poll());

        }

        if (taskqueue.peek() != null) {

            // Set the beginTime of the Task in queue to now, i.e. begin working on this task.

            taskqueue.peek().setBeginTime(time);
            taskqueue.peek().addBeginTime(time);

        }

        // Generate a new finTime for the Queue.

        finTime();

    }

    /****************************************************************************
     *
     *	Method:			clearTask
     *
     *	Purpose:		Specially used for the second last phase. When it
     *              	researches the end of the second last phase, we should
     *                  stop the task the operator is working on and set all the
     *                  tasks in the queue to expire (except for essential task)
     *                  to get ready for the last phase.
     *
     ****************************************************************************/

    public void clearTask(loadparam vars, Operator op){

        System.out.println("About to go into last Phase......");

        blockLastSecondPhase = true;

        if (taskqueue.peek() != null) {

            Task onHandTask = taskqueue.peek();

            if (vars.essential[onHandTask.getType()] == 1) {
                done(vars, op);
            }
            else {
                onHandTask.addInterruptTime(vars.phaseBegin[vars.numPhases - 1]);
                onHandTask.setEndTime(vars.phaseBegin[vars.numPhases - 1]);
                onHandTask.setWaitTime(vars.phaseBegin[vars.numPhases - 1] - onHandTask.getBeginTime() - onHandTask.getSerTime());
                vars.failedTask.getNumFailedTask()[vars.replicationTracker][onHandTask.getPhase()][operator.dpID / 100][onHandTask.getType()][1]++;
                recordtasks.add(taskqueue.poll());
            }

        }

        while (taskqueue.peek() != null) {

            if (vars.essential[taskqueue.peek().getType()] == 1) {
                done(vars, op);
            }
            else {
                taskqueue.peek().setexpired();
                vars.failedTask.getNumFailedTask()[vars.replicationTracker][taskqueue.peek().getPhase()][operator.dpID / 100][taskqueue.peek().getType()][0]++;
                recordtasks.add(taskqueue.poll());
            }

        }

        SetTime(vars.phaseBegin[vars.numPhases - 1]);
        finTime();

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
        }

        // Otherwise grab the current task and return a finish time.

        else {
            Task onhand = taskqueue.peek();
            finTime = onhand.getBeginTime() + onhand.getSerTime() - onhand.getELSTime();
        }
    }

}
