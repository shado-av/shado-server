package server.Input;
import server.Engine.Data;
import server.Engine.Operator;
import server.Engine.Replication;
import server.Engine.Task;
import javafx.util.Pair;
import server.Output.FailedTask;

import java.io.*;
import java.util.*;

/***************************************************************************
 * 
 * 	FILE: 			loadparam.java
 * 
 * 	AUTHOR: 		BRANCH VINCENT
 * 
 * 	TRANSLATOR: 	ROCKY LI, RICHARD CHEN
 * 	
 * 	LATEST EDIT:	2017/12/2
 * 
 * 	VER: 			1.0 SHOW
 * 					1.1 SHADO
 *
 * 	Purpose: 		Load vars in text.
 * 
 **************************************************************************/

public class loadparam {
	
	// Global input variables
	public double       numHours;
    public double[]     traffic;
    public int          numReps;
    public int          numPhases;
    public double[]     phaseBegin;
    public int []       hasExogenous;

    // Team Variables
    public int          numTeams;
    public int[]        teamSize;
    public String[]     opNames;
    public String[]     opStrats;
    public int[][]      opTasks;
    public int[][][]    taskPrty; //phase * team * task
    public char[]       teamComm;
    public double[][][] humanError;
    public double[][][] ECC; //short for "Error Catching Chance"

    //AIDA Variables
    public int[][]      AIDAtype;
    public double[]     ETServiceTime;
    public double[]     ETErrorRate;
    public double[]     ETFailThreshold;
    public int[][]      IAtasks;
    public char[]       IALevel;
    public char[]       TCALevel;

    // Fleet Variables
    public int          fleetTypes;
    public int[]        numvehicles;
    public char[]       autolvl;
    public int[][]      fleetHetero;

    // Task Variables
    public int          numTaskTypes;
    public String[]     taskNames;
    public char[][]     arrDists;
    public double[][][] arrPms;
    public char[][]     serDists;
    public double[][][] serPms;
    public char[][]     expDists;
    public double[][][] expPms;
    public int[][]      affByTraff;
    public int[]        teamCoordAff;
    public int[]        interruptable;
    public int[]        essential;
    public ArrayList<ArrayList<Integer>> followedTask;

    //Followed Task Variables

    public int[]        leadTask;
    public String[]     taskNames_f;
    public char[][]     arrDists_f;
    public double[][][] arrPms_f;
    public char[][]     serDists_f;
    public double[][][] serPms_f;
    public char[][]     expDists_f;
    public double[][][] expPms_f;
    public int[][]      affByTraff_f;
    public int[]        teamCoordAff_f;
    public int[][][]    taskPrty_f; //phase * team * task
    public int[]        interruptable_f;
    public int[]        essential_f;
    public double[][][] humanError_f;
    public double[][][] ECC_f; //short for "Error Catching Chance"

    // Other parameters
    public String[]                     taskName_all;
    public int                          totalTaskType;
    public int                          numRemoteOp;
    public int[]                        ETteam; //which team has ET for this task type
    public boolean                      hasET = false;
    public int[]                        RemoteOpTasks;
    public int                          replicationTracker;
    public int currRepnum = 0;

    // Records
    public Replication[]                                reps;
    public HashMap<Integer,ArrayList>                   rep_failTask;
    public FailedTask                                   failedTask;
    public HashMap<Integer,Integer>                     failTaskCount;
    public Data[][]                                     utilizationOutput;   //utilization[numRep][numOperator]
    public ArrayList<ArrayList<Task>>                   allTasksPerRep;
    public ArrayList<Task>                              AITasks;
    public ArrayList<ArrayList<Pair<Operator,Task>>>    expiredTasks;


    // Operator settings
    public double[][] crossRepCount;
	public int[][] opNums;
	public int[][] trigger;


	// Toggle Global Variables
	public static boolean TRAFFIC_ON = true;
	public static boolean FATIGUE_ON = true;
	public static boolean DEBUG_ON = false;
	public static boolean OUTPUT_ON = true;
	public static boolean RAND_RUN_ON = true;

	//SCHEN 11/15/17 test separated replication
	public double[][][] repUtilOp;
	public int[] repNumTasks;
	public int processedRepId;
	public int debugCnt;
	public int maxTeamSize;
	public int metaSnapShot;

	
	/****************************************************************************
	*																			
	*	Shado Object:	loadvars
	*																			
	*	Purpose:		Load ALL vars in text
	*																			
	****************************************************************************/
	public loadparam(){
    }
    /*
    * Set Global data after reading from JSON
    * */
    public void setGlobalData(){

        getNumRemoteOp();
        totalTaskType = numTaskTypes + leadTask.length + 3;
        collectTaskNames();
        failTaskCount = new HashMap<>();
        failedTask = new FailedTask(this, taskName_all);
        replicationTracker = 0;
        processedRepId = 0;
        debugCnt = 0;
        maxTeamSize = 0;
        metaSnapShot = 0;
        allTasksPerRep = new ArrayList<>();
		crossRepCount = new double[numReps][];
		repNumTasks = new int[numReps];
		//Utilization for each type of operator across replications
		repUtilOp = new double[numReps][numTeams][];
		for(int i = 0; i < numReps;i++){
		    for(int j = 0; j < numTeams;j++){
		        repUtilOp[i][j] = new double[teamSize[j]];
            }
        }
		reps = new Replication[numReps];
        rep_failTask = new HashMap<>();
        expiredTasks = new ArrayList<ArrayList<Pair<Operator,Task>>>();
        for(int i = 0; i < numReps; i++){
            expiredTasks.add(new ArrayList<Pair<Operator, Task>>());
        }
        opNums = new int[numTaskTypes][];

        for (int i = 0; i < numTaskTypes; i++){
            ArrayList<Integer> wha = new ArrayList<Integer>();
            for (int j = 0; j < numTeams; j++){
                if (Arrays.asList(opTasks[j]).contains(i)){
                    wha.add(j);
                }
            }
            opNums[i] = wha.stream().mapToInt(Integer::intValue).toArray();
        }
        for(int i = 0; i < teamSize.length; i++){
            if(teamSize[i] > maxTeamSize){
                maxTeamSize = teamSize[i];
            }
        }
        utilizationOutput = new Data[numReps][numRemoteOp];

        // create the ET team matrix
        ETteam = new int[numTaskTypes];
        checkET();

        // create the followed task matrx
        followedTask = new ArrayList<>();
        for(int i = 0; i < numTaskTypes; i++){
            ArrayList<Integer> n = new ArrayList<>();
            followedTask.add(n);
        }
        checkFollowedTask();
    }

    private void getNumRemoteOp(){
        numRemoteOp = 0;
        for(int i : teamSize){
            numRemoteOp += i;
        }
    }

    /****************************************************************************
     *
     *	Shado Object:	collectTaskNames
     *
     *	Purpose:		Put the task name, followed task name, TC task name and
     *              	Exogenous task name into one matrix.
     *
     ****************************************************************************/

    private void collectTaskNames(){

        String[] specialTaskName = {"TC task (some)", "TC task (full)", "Exogenous task"};
        taskName_all = new String[totalTaskType];
        for (int i = 0; i < totalTaskType; i++) {
            if (i < numTaskTypes) {
                taskName_all[i] = taskNames[i];
            }
            else if (i < numTaskTypes + leadTask.length) {
                taskName_all[i] = taskNames_f[i - numTaskTypes];
            }
            else {
                taskName_all[i] = specialTaskName[i - numTaskTypes - leadTask.length];
            }
        }

    }

    /****************************************************************************
     *
     *	Shado Object:	checkET
     *
     *	Purpose:		Build the checkET matrix to record the Equal Teammate AIDA
     *                  for each task.
     *
     ****************************************************************************/
    private void checkET(){

        //set hasET default to false
        for(int i = 0; i < numTaskTypes; i++){
            ETteam[i] = -1;
        }

        for(int team = 0; team < numTeams; team++){
            //if this team has equal teammate AIDA
            if(AIDAtype[team][0] == 1){
                hasET = true;
                for(int i : opTasks[team]) {
                    ETteam[i] = team;
                }
            }
        }

    }

    /****************************************************************************
     *
     *	Shado Object:	checkFollowedTask
     *
     *	Purpose:		Build the followedTask matrix to record the followed tasks'
     *                  type for each task.
     *
     ****************************************************************************/
    private void checkFollowedTask(){
        if (leadTask.length > 0){
            for(int i = 0; i < leadTask.length; i++){
                followedTask.get(leadTask[i]).add(i);
            }
        }
    }

//
//	public loadparam(String file) throws FileNotFoundException{
//
//		//Declare a scanner for the file
//
//		Scanner in = new Scanner(new File(file));
//
//		//Read the header of the file
//
//		outputPath = readString(in);
//		numHours = readDouble(in);
//		traffic = readTraff(in);
//		numReps = readInt(in);
//		//SCHEN 11/10/17 fleetTypes represents the combination of different vehicles
//		fleetTypes = readInt(in);
//        failTaskCount = new HashMap<>();
//       	replicationTracker = 0;
//		crossRepCount = new double[numReps][];
//
//		//SCHEN 11/10/17 Read numvehicles Array
////		numvehicles = readInt(in);
//		numvehicles = readIntArr(in);
//		numTeams = readInt(in);
//		numRemoteOp = readInt(in);
//		RemoteOpTasks = readIntArr(in);
//		numTaskTypes = readInt(in);
//        numPhases = readInt(in);
//		//SCHEN 12/4/15 Fleet Autonomous level
//		autolvl = readInt(in);
////		teamComm = readInt(in);
//		hasExogenous = readIntArr(in);
//        opStrats = readString(in);
//		failThreshold = readDouble(in);
//		reps = new Replication[numReps];
//		rep_failTask = new HashMap<>();
//		expiredTasks = new ArrayList<ArrayList<Pair<Operator,Task>>>();
//		for(int i = 0; i < numReps; i++){
//			expiredTasks.add(new ArrayList<Pair<Operator, Task>>());
//		}
//
//		//Has exo-factors
//		int numExos = hasExogenous[1];
//        exNames = new String[numExos];
//        exTypes = new String[numExos];
//        for(int i = 0; i < numExos; i++){
//            exNames[i] = readString(in);
//            exTypes[i] = readString(in);
//        }
//
//
//		//SCHEN 11/10/2017
//		//Load Fleet Heterogeneity info
//
//		fleetHetero = new int[fleetTypes][];
//		for(int i = 0 ; i < fleetTypes; i++){
//			fleetHetero[i] = readIntArr(in);
//		}
//
//
//        //SCHEN 1/20/2018 Individualize team_comm to each operator type
//		teamSizeTotal = 0;
//		opNames = new String[numTeams];
//		opTasks = new int[numTeams][];
//		teamComm = new char[numTeams];
//        teamSize = new int[numTeams];
//        ops = new int[numTeams];
//		for (int i = 0; i < numTeams; i++){
//			opNames[i] = readString(in);
//			opTasks[i] = readIntArr(in);
//
//            //Team settings
//            teamSize[i] = readInt(in);
//            teamSizeTotal += teamSize[i];
//			teamComm[i] = readChar(in);
//			ops[i] = i;
//		}
//
//
//		//Initiate array sizes
//
//
//		taskNames = new String[numTaskTypes];
//		taskPrty = new int[numTaskTypes][];
//		arrDists = new char[numTaskTypes];
//		arrPms = new double[numTaskTypes][];
//		serDists = new char[numTaskTypes];
//		serPms = new double[numTaskTypes][];
//		expDists = new char[numTaskTypes];
//	   	expPmsLo = new double[numTaskTypes][];
//	    expPmsHi = new double[numTaskTypes][];
//		affByTraff = new int[numTaskTypes][];
//		opNums = new int[numTaskTypes][];
//		linked = new int[numTaskTypes];
//		trigger = new int[numTaskTypes][];
//		teamCoordAff = new int[numTaskTypes];
//		humanError = new double[numTaskTypes][];
//
//		// Read in vehicle operators by vehicle ID.
////		for ()
//
//		//Read in agent type and tasks they can do
//
//		//Read in the task vars
//
//		for (int i = 0; i< numTaskTypes; i++){
//
//			taskNames[i] = readString(in);
//			taskPrty[i] = readIntArr(in);
//			arrDists[i] = readChar(in);
//			arrPms[i] = readDoubleArr(in);
//			serDists[i] = readChar(in);
//			serPms[i] = readDoubleArr(in);
//			expDists[i] = readChar(in);
//			expPmsLo[i] = readDoubleArr(in);
//			expPmsHi[i] = readDoubleArr(in);
//			affByTraff[i] = readIntArr(in);
//			linked[i] = readInt(in);
//			trigger[i] = readIntArr(in);
//			teamCoordAff[i] = readInt(in);
//			humanError[i] = readDoubleArr(in);
//
//		}
//
//		for (int i = 0; i < numTaskTypes; i++){
//			ArrayList<Integer> wha = new ArrayList<Integer>();
//			for (int j = 0; j < numTeams; j++){
//				if (Arrays.asList(opTasks[j]).contains(i)){
//					wha.add(j);
//				}
//			}
//			opNums[i] = wha.stream().mapToInt(Integer::intValue).toArray();
//		}
//	}
//
//	/****************************************************************************
//	*
//	*	Method:		ridvarsname
//	*
//	*	Purpose:	Read a line in the text and remove the vars name, also
//	*				returns the line as a scanner while moving the main scanner to
//	*				the next line. Also ignore lines if it's empty.
//	*
//	*	NOTE:		ALL OF THE FOLLOWING METHODS include this method to skip the
//	*				name in the file read.
//	*
//	****************************************************************************/
//
//	public Scanner ridvarsname(Scanner in){
//
//		//get rid of the vars name in source file.
//		String line = "";
//		while (true){
//			line = in.nextLine();
//			if (!line.isEmpty())
//				break;
//		}
//		Scanner input = new Scanner(line);
//		input.next();
//		return input;
//
//	}
//
//	/****************************************************************************
//	*
//	*	Method:		readString
//	*
//	*	Purpose:	Read a string line in text and return string
//	*
//	****************************************************************************/
//
//	public String readString(Scanner in){
//
//		//Read string object
//
//		Scanner input = ridvarsname(in);
//		String ret = input.nextLine();
//		ret = ret.trim();
//		input.close();
//		return ret;
//
//	}
//
//	/****************************************************************************
//	*
//	*	Method:		readTraff
//	*
//	*	Purpose:	Read traffic line in text and return a double array
//	*
//	****************************************************************************/
//
//	public double[] readTraff(Scanner in){
//
//		Scanner input = ridvarsname(in);
//
//		ArrayList<String> traff = new ArrayList<String>();
//		ArrayList<Double> traffic = new ArrayList<Double>();
//
//		while (input.hasNext()){
//			traff.add(input.next());
//		}
//
//		for (int i = 0; i<traff.size() ; i++){
//			String get = traff.get(i);
//			double myDouble = 0;
//			switch(get){
//			case "l": myDouble = 0.5; break;
//			case "m": myDouble = 1.0; break;
//			case "h": myDouble = 2.0; break;
//			}
//			traffic.add(myDouble);
//		}
//		input.close();
//
//		return traffic.stream().mapToDouble(Double::doubleValue).toArray();
//
//	}
//
//	/****************************************************************************
//	*
//	*	Method:		readInt
//	*
//	*	Purpose:	Read a integer line in text and return ONE int value
//	*
//	****************************************************************************/
//
//	public int readInt(Scanner in){
//
//		Scanner input = ridvarsname(in);
//		return input.nextInt();
//
//	}
//
//	/****************************************************************************
//	*
//	*	Method:		readDouble
//	*
//	*	Purpose:	Read a integer line in text and return ONE int value
//	*
//	****************************************************************************/
//
//	public double readDouble(Scanner in){
//
//		Scanner input = ridvarsname(in);
//		return Double.parseDouble(input.next());
//
//	}
//
//	/****************************************************************************
//	*
//	*	Method:		readIntArr
//	*
//	*	Purpose:	read an integer array from one line
//	*
//	****************************************************************************/
//
//	public int[] readIntArr(Scanner in){
//
//		Scanner input = ridvarsname(in);
//		ArrayList<Integer> ints = new ArrayList<Integer>();
//		while (input.hasNextInt()){
//			ints.add(input.nextInt());
//		}
//		input.close();
//		return ints.stream().mapToInt(Integer::intValue).toArray();
//
//	}
//
//	/****************************************************************************
//	*
//	*	Method:		readDuobleArr
//	*
//	*	Purpose:	read a double array from one line
//	*
//	****************************************************************************/
//
//	public double[] readDoubleArr(Scanner in){
//
//		Scanner input = ridvarsname(in);
//		ArrayList<Double> doubs = new ArrayList<Double>();
//		while (input.hasNext()){
//			double myDouble = Double.parseDouble(input.next());
//			doubs.add(myDouble);
//		}
//		input.close();
//		return doubs.stream().mapToDouble(Double::doubleValue).toArray();
//
//	}
//
//	/****************************************************************************
//	*
//	*	Method:		readChar
//	*
//	*	Purpose:	read a character from one line
//	*
//	****************************************************************************/
//
//	public char readChar(Scanner in){
//
//		Scanner input = ridvarsname(in);
//		char myChar = input.next().charAt(0);
//		input.close();
//		return myChar;
//
//	}
//
//	/****************************************************************************
//	*
//	*	Method:		invertArr
//	*
//	*	Purpose:	read a double array and invert each element of it unless 0
//	*
//	****************************************************************************/
//
//	public double[] invertArr(double[] input){
//
//		for (int i = 0; i<input.length ; i++){
//			if (input[i] != 0.0){
//				input[i] = 1.0/input[i];
//			}
//		}
//		return input;
//
//	}
}
