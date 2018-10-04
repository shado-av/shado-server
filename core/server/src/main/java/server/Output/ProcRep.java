package server.Output;

import server.Engine.*;
import server.Input.loadparam;
import java.util.ArrayList;

/***************************************************************************
 *
 * 	FILE: 			ProcRep.java
 *
 * 	AUTHOR: 		ROCKY LI
 * 	                Richard Chen
 *
 * 	DATE:			2017/9/10, 201712/2
 *
 * 	VER: 			1.0
 *
 * 	Purpose: 		Process each replication in Data individually
 *
 **************************************************************************/

public class ProcRep {

    private Data[] RemoteOpdata;

    private Replication rep;

    private int repID;

    private double hours;

    private Data[] utilization_task;

    private Data[] utilization_fleet;

    private Data[] waittime_task;

    private Data[] waittime_fleet;

    private loadparam vars;

    private int TASK_RECORD = 1;
    private int FLEET_RECORD = 2;

    // INSPECTORS

    public Data[] getUtilization_task() { return utilization_task; }

    public Data[] getUtilization_fleet() { return utilization_fleet; }

    public Data[] getWaitTime_task() { return waittime_task; }

    public Data[] getWaitTime_fleet() { return waittime_fleet; }

     /****************************************************************************
     *
     *	Shado Object:	ProcRep
     *
     *	Purpose:		Create a ProcRep Object with a Data object and Replication
     *                  object as input
     *
     ****************************************************************************/

    public ProcRep(Data[] dis, Replication rep, loadparam vars){

        this.rep = rep;
        RemoteOpdata = dis;
        repID = rep.getRepID();
        hours = vars.numHours;
        this.vars = vars;

        utilization_task = new Data[vars.numRemoteOp + vars.flexTeamSize];
        utilization_fleet = new Data[vars.numRemoteOp + vars.flexTeamSize];
        waittime_task = new Data[vars.numRemoteOp + vars.flexTeamSize];
        waittime_fleet = new Data[vars.numRemoteOp + vars.flexTeamSize];

        for (int i = 0; i < vars.numRemoteOp + vars.flexTeamSize; i++){
            utilization_task[i] = new Data(vars.totalTaskType,(int) hours * 6, 1);
            utilization_fleet[i] = new Data(vars.fleetTypes, (int) hours * 6, 1);
            waittime_task[i] = new Data(vars.totalTaskType,(int) hours * 6, 1);
            waittime_fleet[i] = new Data(vars.fleetTypes, (int) hours * 6, 1);
        }
    }

    /****************************************************************************
     *
     *	Method:			run
     *
     *	Purpose:		A wrapper that runs the ProcRep class.
     *
     ****************************************************************************/

    public void run(){
        fillRepData();
        appendData();
    }

    /****************************************************************************
     *
     *	Method:			fillRepData
     *
     *	Purpose:		Call fillRepDataCell on appropriate Data objects.
     *
     ****************************************************************************/

    public void fillRepData(){

        //SCHEN 11/29/17
        Operator[] RemoteOpers = rep.getRemoteOp().getRemoteOp();
 
        for (int i = 0; i < vars.numRemoteOp + vars.flexTeamSize; i++){
            fillRepDataCell(RemoteOpers[i], utilization_task[i], TASK_RECORD);
            fillRepDataCell(RemoteOpers[i], utilization_fleet[i], FLEET_RECORD);
            fillRepDataCellWaitTime(RemoteOpers[i], waittime_task[i], TASK_RECORD);
            fillRepDataCellWaitTime(RemoteOpers[i], waittime_fleet[i], FLEET_RECORD);
        }

    }

    /****************************************************************************
     *
     *	Method:			fillRepDataCellWaitTime
     *
     *	Purpose:		Fill the Rep Data object with simulated wait time data.
     *
     ****************************************************************************/

    public void fillRepDataCellWaitTime(Operator operator, Data incremented, int recordType){

        ArrayList<Task> records = operator.getQueue().records();

        for (Task each: records){

            // easy way to calculate total wait time... using this for verification
            if(!each.getFail() && !each.getExpired()) {
                if (recordType == TASK_RECORD) {
                    vars.waitTime.getWaitTime()[operator.dpID / 100][each.getVehicleID() / 100] += each.getWaitTime();
                } else {
                    vars.waitTime.getWaitTimePerTask()[operator.dpID / 100][each.getType()] += each.getWaitTime();
                }
            }

            int index;

            if (recordType == TASK_RECORD) {
                index = each.getType();
            }
            else { //recordType == FLEET_RECORD
                index = each.getVehicleID() / 100;
            }
            
            // Reverse for the wait time
            double beginscale = each.getArrTime() / 10;
            for(int i = 0; i < each.workSchedule.size(); i++){
                double endscale = each.workSchedule.get(i)[0] / 10;
                if (beginscale < endscale) {
                    fill(beginscale, endscale, incremented, index);
                }
                double candidate = each.workSchedule.get(i)[1] / 10;
                if (endscale < candidate)
                    beginscale = endscale;
            }
        }

    }

    /****************************************************************************
     *
     *	Method:			fillRepDataCell
     *
     *	Purpose:		Fill the Rep Data object with simulated data.
     *
     ****************************************************************************/

    public void fillRepDataCell(Operator operator, Data incremented, int recordType){

        ArrayList<Task> records = operator.getQueue().records();

        for (Task each: records){

            int index;

            if (recordType == TASK_RECORD) {
                index = each.getType();
            }
            else { //recordType == FLEET_RECORD
                index = each.getVehicleID() / 100;
            }

            for(int i = 0; i < each.workSchedule.size(); i++){
                if (each.workSchedule.get(i)[0] >= each.workSchedule.get(i)[1]) {
                    continue;
                }
                double beginscale = each.workSchedule.get(i)[0] / 10;
                double endscale = each.workSchedule.get(i)[1] / 10;

                //System.out.println("Rep " + this.rep.getRepID() + " index " + index + " BeginTime: " + beginscale + " EndTime: " + endscale);

                fill(beginscale, endscale, incremented, index);
            }
        }

    }


    private void fill(double beginscale, double endscale, Data incremented, int taskType){

        boolean startcheck = false;

        for (int i = 1; i < (int) hours*6 + 1; i++) {

            // If task hasn't began yet

            if (beginscale > i) {
                continue;
            }

            // If task began but not finished in this interval.

            if (endscale > i) {
                if (!startcheck) {
                    incremented.datainc(taskType, i - 1, 0, i - beginscale);
                    startcheck = true;
                } else {
                    incremented.datainc(taskType, i - 1, 0, 1);
                }
            } else {
                if (!startcheck) {
                    incremented.datainc(taskType, i - 1, 0, endscale - beginscale);
                    break;
                } else {
                    incremented.datainc(taskType, i - 1, 0, endscale - i + 1);
                    break;
                }
            }
            // if (incremented.dataget(taskType, i - 1, 0) > 1.02) {
            //    System.out.println("Fill index " + taskType + " BeginTime: " + beginscale + " EndTime: " + endscale + " time: " + (i-1) + " val: " + incremented.dataget(taskType, i - 1, 0));
            // }
        }
    }


    /****************************************************************************
     *
     *	Method:			appendData
     *
     *	Purpose:		Add the data of the replication to the main data field
     *
     ****************************************************************************/

    public void appendData(){

        // Process the RemoteOp data

        for (int i = 0; i < vars.numRemoteOp + vars.flexTeamSize; i++){
            Data processed = RemoteOpdata[i];
            for (int x = 0; x < processed.data.length; x++){
                for (int y = 0; y < processed.data[0].length; y++){
                    processed.datainc(x, y, repID, utilization_task[i].dataget(x, y, 0));
                }
            }
        }

    }


}
