package server.Engine;
import java.util.*;
import server.Input.loadparam;

/***************************************************************************
 *
 * 	FILE: 			Task.java
 *
 * 	AUTHOR:			ROCKY LI
 *
 * 	LATEST EDIT:	2017/5/24
 *
 * 	VER: 			1.1
 *
 * 	Purpose: 		generate task objects.
 *
 **************************************************************************/

public class Task implements Comparable<Task> {

	//General task input params.
	private int Type;
	private int Priority;
	private int teamType;
	public loadparam vars;
	private double lvl_SOME = 0.7;
	private double lvl_FULL = 0.3;
	private double lvl_None = 1.0;
	private double[] arrivalRate;



	//Task specific variables.
	private int Phase;
	private int shiftPeriod;
	private double prevTime;
	private double arrTime;
	private double serTime;
	private double expTime;
	private double elapsedTime;
	private ArrayList<Double[]> workSchedule;
	private double waitTime;
	private double beginTime;
	private double endTime;
	public int[] opNums;
	private String name;
	private int vehicleID;
	private boolean expired;
	private boolean fail; // Indicates fail but proceed


	// This adds functionalities of the RemoteOper

	private boolean isLinked;

	// This adds the ability for task to track queue retroactively

	private int queued;

	// Mutators
	public boolean checkexpired() { return expired; }

	public boolean getFail(){return this.fail;}

	public int getPhase(){ return Phase;}

	public int getTeamType() { return teamType; }

	public void setFail(){this.fail = true;}

	public void setPriority(int Priority){ this.Priority = Priority; }

	public void setexpired() {
		expired = true;
	}

	public void setELStime (double time){
		elapsedTime = time;
	}

	public void setWaitTime(double time){waitTime = time; }

	public void setID(int id){
		vehicleID = id;
	}

	public void setTeamType(int type) {teamType = type;}

	public void setQueue(int q){
		queued = q-1;
	}

	public void setEndTime(double time){
		endTime = time;
	}

	public void setBeginTime(double time){
		beginTime = time;
	}

	@Override
	public String toString() {
		return name + " arrive at "+ arrTime;
	}

	/****************************************************************************
	 *
	 *	Shado Object:	Task
	 *
	 *	Purpose:		Generate a new task on completion of old task. And return
	 *					it's vars.
	 *
	 ****************************************************************************/

	public Task(int type, double PrevTime, loadparam Param, boolean fromPrev) {

		Type = type;
		vars = Param;
		prevTime = PrevTime;
		Phase = getPhase(PrevTime);
		shiftPeriod = getShiftTime(PrevTime);
		this.fail = false;
		elapsedTime = 0;
		waitTime = 0;
		expired = false;
		Priority = 0;
		workSchedule = new ArrayList<>();

		if(type >= 0){

			if (fromPrev == true) {
				arrTime = genArrTime(PrevTime, Type);
			} else {
				arrTime = PrevTime;
			}

			//SCHEN 12/10/17 Fleet Autonomy, Team Coord and Exogenous factor added
			serTime = GenTime(vars.serDists[Phase][Type], vars.serPms[Phase][Type]);

			expTime = genExpTime();
			opNums = vars.opNums[Type];
			name = vars.taskNames[Type];
//		isLinked = vars.linked[Type] == 1;

		}
		else{

			if(type == -1){
				arrTime = PrevTime + Exponential(0.1);
				serTime = Exponential(0.1667);
				name = "Team Coordination Task level some";
			}
			else if(type == -2){
				arrTime = PrevTime + Exponential(0.2);
				serTime = Exponential(0.1667);
				name = "Team Coordination Task level full";
			}
			else if(type == -3){
				arrTime = PrevTime + Exponential(0.0021);
				serTime = Uniform(20,40);
				name = "Exogenous Task";
			}

			expTime = Double.POSITIVE_INFINITY;
		}

		beginTime = arrTime;
		//shift schedule 1% fatigue increase serve time
		changeServTime(1 + 0.01 * (shiftPeriod+1));
	}

	/****************************************************************************
	 *
	 *	Method:			compareTo
	 *
	 *	Purpose:		Compare two task based on operator's strategy
	 *
	 *  Notes:  		When using the priority queue: "this" is the new task;
	 *  				"other" is the original top task. Return positive value
	 *  				means put the new task behind the original top task.
	 *
	 ****************************************************************************/

	@Override
	public int compareTo(Task other){

		if(this.getType() < 0) return 1;

		if(vars.interruptable[other.getType()] == 0 ||
				vars.essential[other.getType()] == 1){ // If the old task cannot be interrupted
			System.out.println("In task.compareTo, old task cannot be interrupted.");
			return 1;
		}

		if(vars.essential[this.getType()] == 1){ // If the new task is essential task, which can interrupt any other task
			System.out.println("In task.compareTo, the new task is essential.");
			return -1;
		}

		if(vars.opStrats[teamType].equals("PRTY")){
			if(other.Priority > this.Priority) return 1;
			else if(other.Priority < this.Priority) return -1;
			// If two tasks have same priority, use FIFO
			if (this.arrTime > other.arrTime) return 1;
			return -1;
		}

		else if(vars.opStrats[teamType].equals("FIFO")){
			if (this.arrTime > other.arrTime){ //the old task(other) arrives first, it should come first
				return 1;
			} else {
				return -1;
			}
		}

		else if(vars.opStrats[teamType].equals("STF")){
			System.out.println("In task.compareTo, we are using STF now.");
			if(other.getSerTime() < this.getSerTime() - this.getELSTime()){ //this task needs more serve time, other task should come first
				return 1;
			} else {
				return -1;
			}
		}

		return 1;

	}

	// The following are inspector functions.

	public String getName() {return this.name;}

	public int getQueued() {return this.queued;}

	public boolean linked() {return this.isLinked;}

	public int getType() {return this.Type;}

	public int getPriority(){return this.Priority;}

	public double getArrTime(){return this.arrTime;}

	public double getSerTime(){return this.serTime;}

	public double getEndTime(){return this.endTime;}

	public double getExpTime() {return this.expTime;}

	public double getELSTime() {return this.elapsedTime;}

	public double getBeginTime() {return this.beginTime;}


	/****************************************************************************
	 *
	 *	Method:			GetPhase
	 *
	 *	Purpose:		Return the Phase
	 *
	 ****************************************************************************/

	public int getPhase(double time){
		int currentPhase = 0;
		for(int i = 0; i < vars.numPhases; i++){
			if(vars.phaseBegin[i] <= time){
				currentPhase = i;
			}
			else break;
		}
		return currentPhase;
	}

    /****************************************************************************
     *
     *	Method:			getShiftTime
     *
     *	Purpose:		Return shift period
     *
     ****************************************************************************/

    public int getShiftTime(double time){
//        System.out.println("at shift period: "+(int)time/60);
        return (int)time/60;
    }

	/****************************************************************************
	 *
	 *	Method:			Exponential
	 *
	 *	Purpose:		Return an exponential distributed random number with input
	 *					vars lambda.
	 *
	 ****************************************************************************/

	private double Exponential(double lambda){

		if (lambda == 0){
			return Double.POSITIVE_INFINITY;
		}
		double result = Math.log(1- Math.random())/(-lambda);

		return result;
	}

	/****************************************************************************
	 *
	 *	Method:			Lognormal
	 *
	 *	Purpose:		Return a lognormal distributed random number with input
	 *					mean and standard deviation.
	 *
	 ****************************************************************************/

	private double Lognormal(double mean, double stddev){

		Random rng = new Random();
		double normal = rng.nextGaussian();
		double l = Math.exp(mean + stddev * normal);

		return l;
	}

	/****************************************************************************
	 *
	 *	Method:			Uniform
	 *
	 *	Purpose:		Return a uniform distribution with input minimum and maximum
	 *
	 ****************************************************************************/

	private double Uniform(double min, double max){

		return min + (max-min) * Math.random();

	}

	/****************************************************************************
	 *
	 *	Method:			Triangular
	 *
	 *	Purpose:		Return a triangular distributed random variables
	 *
	 ****************************************************************************/

	private double Triangular(double min, double mode, double max){
		double F = (mode - min)/(max - min);
		double rand = Math.random();
		if (rand < F) {
			return min + Math.sqrt(rand * (max - min) * (mode - min));
		} else {
			return max - Math.sqrt((1 - rand) * (max - min) * (max - mode));
		}
	}

	/****************************************************************************
	 *
	 *	Method:			genTime
	 *
	 *	Purpose:		Generate a new time with the specified type and vars.
	 *
	 ****************************************************************************/

	private double GenTime (char type, double[] param){
		switch (type){
			case 'E':
				return Exponential(param[0]);
			case 'L':
				return Lognormal(param[0], param[1]);
			case 'U':
				return Uniform(param[0], param[1]);
			case 'T':
				return Triangular(param[0], param[1], param[2]);
			case 'C':
				return param[0];
			default:
				throw new IllegalArgumentException("Wrong Letter");
		}
	}

	/****************************************************************************
	 *
	 *	Method:			genArrTime
	 *
	 *	Purpose:		Generate a new exponentially distributed arrival time by phase.
	 *
	 ****************************************************************************/

	private double genArrTime(double PrevTime, int taskType){
		//SCHEN 12/16/17 Add fleet autonomy function by decreasing the arrival rate
		int fleet = vehicleID / 100;
		double[] arrivalRate = changeArrivalRate(getFleetAutonomy(fleet));
		double TimeTaken = GenTime(vars.arrDists[Phase][taskType], arrivalRate);

		if (TimeTaken == Double.POSITIVE_INFINITY){
			return Double.POSITIVE_INFINITY;
		}

		double newArrTime = TimeTaken + PrevTime;

		if (loadparam.TRAFFIC_ON && vars.affByTraff[Type][Phase] == 1 ){

			double budget = TimeTaken;
			double currTime = prevTime;
			int currHour = (int) currTime/60;
			double traffLevel = vars.traffic[currHour];
			double TimeToAdj = (currHour+1)*60 - currTime;
			double adjTime = TimeToAdj * traffLevel;

			while (budget > adjTime){

				budget -= adjTime;
				currTime += TimeToAdj;
				currHour ++;

				if (currHour >= vars.traffic.length){
					return Double.POSITIVE_INFINITY;
				}

				traffLevel = vars.traffic[currHour];
				TimeToAdj = (currHour + 1)*60 - currTime;
				adjTime = TimeToAdj * traffLevel;

			}

			newArrTime = currTime + budget/traffLevel;
		}

		return newArrTime;
	}


	/****************************************************************************
	 *
	 *	Method:			genExpTime
	 *
	 *	Purpose:		Generate a new Expiration time.
	 *
	 ****************************************************************************/

	private double genExpTime(){

		double expiration = GenTime(vars.expDists[Phase][Type], vars.expPms[Phase][Type]);
		return arrTime + 2 * serTime + expiration;

	}

	/****************************************************************************
	 *
	 *	Method:			getFleetAutonomy
	 *
	 *	Purpose:		Fetch fleet autonomy level
	 *					0: default
	 *					1: Some 70% of arrival rate
	 *					2: Full 30% of arrival rate
	 *
	 ****************************************************************************/

	private double getFleetAutonomy(int fleet){
		//SCHEN 12/10/17: Add Fleet autonomy -> adjust arrival rate
		double autoLevel = lvl_None; //default

		if(vars.autolvl[fleet] == 'S') autoLevel = lvl_SOME;
		if(vars.autolvl[fleet] == 'F') autoLevel = lvl_FULL;

		return  autoLevel;
	}

	/****************************************************************************
	 *
	 *	Method:			changeServTime, changeArrivalRate
	 *
	 *	Purpose:		change Service time and arrival rate by multiply by a number
	 *
	 ****************************************************************************/
	public void changeServTime(double num){
		serTime *= num;
		return;
	}

	private double[] changeArrivalRate(double num){
		double[] arrivalRate = new double[vars.arrPms[Phase][Type].length];
		int count = 0;
		for(double d : vars.arrPms[Phase][Type]){
			arrivalRate[count] = d * num;
			count++;
		}
		return arrivalRate;
	}
//
//	public void addBeginTime(double beginTime){
//		double[] newSchedule = new double[2];
//		newSchedule[0] = beginTime;
//		newSchedule[1] = 0;
//		workSchedule.add(newSchedule);
//	}

	public void printBasicInfo(){
		System.out.println("Name : " + name + " Priority : " + Priority);
		System.out.println("Arrival time : " + arrTime);
		System.out.println("I was interrupted at : " + (arrTime + getELSTime()));
		System.out.println("Begin Time : " + beginTime);
		System.out.println("Service Time : " + serTime);
		System.out.println("ELS TIme : " + elapsedTime);
		System.out.println("Finish Time : " + endTime);
	}

}


