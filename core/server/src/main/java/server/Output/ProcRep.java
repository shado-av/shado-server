package server.Output;

import server.Engine.*;

import server.Input.loadparam;

import java.io.*;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

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

    private int numtasktypes;

    private double hours;

    private Data[] repdisdata;

    private loadparam vars;

    private int totalRemoteOp;

    private int numSpecialTasks;

    // INSPECTORS

    public Data[] getRepdisdata() { return repdisdata; }


    /****************************************************************************
     *
     *	Shado Object:	ProcRep
     *
     *	Purpose:		Create a ProcRep Object with a Data object and Replication
     *                  object as input
     *
     ****************************************************************************/

    public ProcRep(Data[] dis, Replication rep, loadparam vars, int SpecialTasks){

        this.rep = rep;
        RemoteOpdata = dis;
        repID = rep.getRepID();
        numtasktypes = rep.vars.numTaskTypes;
        hours = rep.vars.numHours;
        numSpecialTasks = SpecialTasks;

        this.vars = vars;

    }

    /****************************************************************************
     *
     *	Method:			run
     *
     *	Purpose:		A wrapper that runs the ProcRep class.
     *
     ****************************************************************************/

    public void run(){
        setTotalRemoteOps();
        tmpData();
        fillRepData();
        appendData();
    }

    /****************************************************************************
     *
     *	Method:			setTotalRemoteOps
     *
     *	Purpose:		Set the total number of operators.
     *
     ****************************************************************************/

    private void setTotalRemoteOps(){
        for(int i : vars.teamSize){
            totalRemoteOp += i;
        }
    }

    /****************************************************************************
     *
     *	Method:			tmpData
     *
     *	Purpose:		creating temporary Data object to be appended.
     *
     ****************************************************************************/

    public void tmpData(){

        repdisdata = new Data[totalRemoteOp];
        for (int i = 0; i < totalRemoteOp; i++){
            repdisdata[i] = new Data(numtasktypes + numSpecialTasks,(int) hours*6, 1);
        }

    }

    /****************************************************************************
     *
     *	Method:			fillRepDataCell
     *
     *	Purpose:		Fill the Rep Data object with simulated data.
     *
     ****************************************************************************/

    public void fillRepDataCell(Operator operator, Data incremented){

        // Get Operator's task record.

        ArrayList<Task> records = operator.getQueue().records();

        // Cycle through records of each operator in 10 minutes intervals.

        for (Task each: records){

            //[ normal task | followed task | special task  ]

            int taskType = each.getType();

            if(taskType < 0){ // This is a special task
                taskType = numtasktypes - taskType - 1;
            }

            for(int i = 0; i < each.workSchedule.size(); i++){
                if (each.workSchedule.get(i)[0] >= each.workSchedule.get(i)[1]) {
                    continue;
                }
                double beginscale = each.workSchedule.get(i)[0] / 10;
                double endscale = each.workSchedule.get(i)[1] / 10;
                fill(beginscale, endscale, incremented, taskType);
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
        }

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

        for (int i = 0; i < totalRemoteOp; i++){
            fillRepDataCell(RemoteOpers[i], repdisdata[i]);
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

        for (int i = 0; i < totalRemoteOp; i++){
            Data processed = RemoteOpdata[i];
            for (int x = 0; x < processed.data.length; x++){
                for (int y = 0; y < processed.data[0].length; y++){
                    processed.datainc(x, y, repID, repdisdata[i].dataget(x, y, 0));
                }
            }
        }

    }


}
