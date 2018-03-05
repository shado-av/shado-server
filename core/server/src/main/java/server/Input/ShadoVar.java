package server.Input;

import javafx.util.Pair;
import server.Engine.Operator;
import server.Engine.Replication;
import server.Engine.Task;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by siyuchen on 3/4/18.
 */
public class ShadoVar{

    public String outputPath;
    public double numHours;
    public double[] traffic;
    public int numReps;
    public int[] ops;
    public int[] numvehicles;

    public int numRemoteOp;
    public Replication[] reps;
    public int[] RemoteOpTasks;
    public HashMap<Integer,ArrayList> rep_failTask;
    public HashMap<Integer,Integer> failTaskCount;
    public int replicationTracker;

    //SCHEN 12/4/17 Fleet Autonomy level param
    // None-> default,
    // Some ->70%
    // Full-> 30%

    public int autolvl;
    public int [] hasExogenous;
    public String opStrats;
    public double failThreshold;

    // SCHEN 11/10/17 Fleet heterogeneity
    public int fleetTypes;
    public int[][] fleetHetero;

    //	//SCHEN 12/10/17 Team Corrdination
    public String[] exNames;
    public String[] exTypes;

    // Operator settings

    //Global count for number of operators
    public int teamSizeTotal;
    public int numTeams;
    public String[] opNames;
    public int[][] opTasks;

    public char[] teamComm;
    public int[] teamSize;
    public double[][] crossRepCount;

    // Task Settings

    public int numTaskTypes;
    public String[] taskNames;
    public int[][] taskPrty;
    public char[] arrDists;
    public double[][] arrPms;
    public char[] serDists;
    public double[][] serPms;
    public char[] expDists;
    public double[][] expPmsLo;
    public double[][] expPmsHi;
    public int[][] affByTraff;
    public int[][] opNums;
    public int[][] trigger;
    //SCHEN 12/10/17 Added: whether the task is affected by team coordination
    public int[] teamCoordAff;
    // Adding isLinked
    public int numPhases;
    public int[] linked;
    public double[][] humanError;
    // Toggle Global Variables

    //SCHEN 11/15/17 test separated replication
    public int currRepnum = 0;
    public ArrayList<ArrayList<Pair<Operator,Task>>> expiredTasks;

    public ShadoVar(){

    }
}
