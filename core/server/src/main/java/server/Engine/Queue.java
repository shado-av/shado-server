package server.Engine;

import server.Input.loadparam;
import javafx.util.Pair;
import java.util.*;
import java.io.*;

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
    public void SetTime(double Time) {
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

        // Only for Debugging log...
        // String filename = System.getProperty("user.home") + "/out/log.txt";

        // try {
        //     System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(filename))));
        // } catch(Exception e) {
        //     e.printStackTrace();
        // }

        taskqueue = new PriorityQueue<Task>();
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
        System.out.println("My queue has " + taskqueue.size() + " tasks and op is " + operator.name + " and dpID is " + operator.dpID);
        System.out.println("The time is " + time + " , the finTime is " + finTime);

        Iterator<Task> it = taskqueue.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            System.out.print(t.getName() + "(" + t.getArrTime() + ")--");
        }
        System.out.println(" ");

        return "The time is " + time + " , the finTime is " + finTime;
    }

    /****************************************************************************
     *
     *	Method:			Add
     *
     *	Purpose:		add a task to queue.
     *
     ****************************************************************************/

    public void add(Task task, boolean redoTask) {

        boolean switchOut = false;

        // Set the time of the queue to the arrival time of the task.

        SetTime(Math.max(task.getArrTime(), time));

        if(!taskqueue.isEmpty()){
            if(task.compareTo(taskqueue.peek()) < 0){ //the new task will go in front of the current top task
                //System.out.println("Task is switched out at " + time + " finTime: "  + finTime);

                if (!redoTask) {
                    Task top = taskqueue.peek();
                //    top.printBasicInfo();
                    top.addInterruptTime(time);
                //    top.printBasicInfo();
                //    System.out.println("");
                }

                switchOut = true;
            }
        }

        // If the task is processed as first priority, i.e. began immediately, then:

        if (taskqueue.peek() == null || switchOut) {
            task.addBeginTime(time);

            //System.out.println(task.getName() + " new task is at " + time);
        }

        taskqueue.add(task);
        //if (switchOut && taskqueue.peek() != task) {
            // debugging point!...
            // switchOut = false;
            //throw new Exception("Simulation Error: Task ordering doens't work correctly!");
        //}
        finTime();

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
        Task currentTask = taskqueue.poll();    // remove current task from queue
        double totalTime = vars.numHours * 60;
        if (currentTask != null) {

            if (finTime <= totalTime) {
                currentTask.setDone(finTime);

                recordtasks.add(currentTask);
                if (!currentTask.getFail()) {
                    vars.taskRecord.getNumSuccessTask()[vars.replicationTracker][currentTask.getPhase()][op.dpID / 100][currentTask.getType()]++;
                }
                SetTime(finTime);

                if(currentTask.getNeedReDo()){
                    Task redoTask = new Task(currentTask);
                    if (redoTask.getArrTime() > 0) {
                        redoTask.setArrTime(finTime);

                        add(redoTask, true);
                    }
                    else {
                        // This redo task cannot be complete within the shift hours, add it to expired task.
                        vars.taskRecord.getNumFailedTask()[vars.replicationTracker][vars.numPhases - 1][op.dpID / 100][currentTask.getType()][0]++;
                    }
                }
            } else { // Current task is last task, but incomplete because of total hours(including essential)
                currentTask.setDone(totalTime);

                // add it to incomplete task
                vars.taskRecord.getNumFailedTask()[vars.replicationTracker][currentTask.getPhase()][operator.dpID / 100][currentTask.getType()][1]++;
                recordtasks.add(currentTask);
                SetTime(totalTime);
            }
        }

        // If there are ANOTHER task in the queue following the completion of this one:

        //Remove all the expired tasks
        while (taskqueue.peek() != null) {

            if (taskqueue.peek().getExpTime() > time) {
                break;
            }

            currentTask = taskqueue.poll();

            // Add expired tasks to the record
            currentTask.setExpired();

            int taskType = currentTask.getType();

            vars.taskRecord.getNumFailedTask()[vars.replicationTracker][currentTask.getPhase()][op.dpID / 100][taskType][0]++;
            vars.expiredTasks.get(vars.replicationTracker).add(new Pair<>(op,currentTask));
            recordtasks.add(currentTask);
        }

        currentTask = taskqueue.peek();
        if (currentTask != null) {

            // Begin working on this task.
            currentTask.addBeginTime(time); // make it essential doesn't change queue order...no side effect
        }

        // Generate a new finTime for the Queue.
        finTime();

    }

    /****************************************************************************
     *
     *	Method:			clearTask
     *
     *	Purpose:		When it reaches to the end of one phase, calling this
     *              	method can clear any method in the queue, except for
     *              	those essential ones.
     *
     ****************************************************************************/

    public void clearTask(loadparam vars, Operator op){

        blockLastSecondPhase = true;
        Task onHandTask = taskqueue.peek();
        Task currentTask;

        //Last task in the queue has been started, if it is non-essential it will be stopped and recorded as unfinished task

        if (onHandTask != null) {

            if (vars.essential[onHandTask.getType()] == 1) {
                done(vars, op);
            }
            else {
                currentTask = taskqueue.poll();
                double phaseTime = vars.phaseBegin[currentTask.getPhase() + 1];

                currentTask.setDone(phaseTime);
                vars.taskRecord.getNumFailedTask()[vars.replicationTracker][currentTask.getPhase()][operator.dpID / 100][currentTask.getType()][1]++;
                recordtasks.add(currentTask);

                // BugFix - setDone should be completed with setTime
                SetTime(phaseTime);
            }
        }
        else {
            return;
        }

        //other task will be set to missed and clear

        while ((currentTask = taskqueue.peek()) != null) {

            if (vars.essential[currentTask.getType()] == 1) {
                if (time < currentTask.getArrTime()) {
                    time = currentTask.getArrTime();
                }
                // add begin time as the task not yet started
                currentTask.addBeginTime(time);

                // adjust fin time for the task
                finTime();

                done(vars, op);
            }
            else {
                currentTask.setExpired();
                vars.taskRecord.getNumFailedTask()[vars.replicationTracker][currentTask.getPhase()][operator.dpID / 100][currentTask.getType()][0]++;
                recordtasks.add(taskqueue.poll());
            }
        }

        SetTime(Math.max(vars.phaseBegin[onHandTask.getPhase() + 1], time));
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
