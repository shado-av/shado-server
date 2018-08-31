package server.Engine;
import java.util.*;
import server.Input.loadparam;

/***************************************************************************
 *
 * 	FILE: 			Task.java
 *
 * 	AUTHOR:			ROCKY LI
 *
 * 	LATEST EDIT:	07/09/2018
 *
 * 	VER: 			1.1		Rocky Li
 * 					2.0		Naixin Yu
 *
 * 	Purpose: 		generate task objects.
 *
 **************************************************************************/

public class Task implements Comparable<Task> {

	//General task input params.
	private String name;
	private int Type;
	private int Priority;
	private int teamType;
	private int Phase;
	private int shiftPeriod;
	public loadparam vars;
	private int vehicleID;
	private double lvl_SOME = 0.7;
	private double lvl_FULL = 0.3;
	private double lvl_None = 1.0;

	//Task specific variables.
	private double prevTime; //Time last task in same type arrived
	private double arrTime;
	private double beginTime;
	private double serTime;
	private double endTime;
	private double expTime;
	private double elapsedTime;
	public ArrayList<double[]> workSchedule;
	private double waitTime;
	private boolean expired;
	private boolean fail; // Indicates fail
	private boolean needReDo = false; // Indicates fail but caught
	private int repeatTimes; // Indicates how many times this task has been redone


	// Inspector functions.

	public boolean getFail(){return this.fail;}

	public boolean getExpired(){ return this.expired; }

	public int getPhase(){ return Phase;}

	public int getTeamType() { return teamType; }

	public boolean getNeedReDo() { return needReDo; }

	public int getRepeatTimes() { return repeatTimes; }

	public int getVehicleID() { return vehicleID; }

	public double getWaitTime() { return waitTime; }

	public String getName() {return this.name;}

	public int getType() {return this.Type;}

	public double getArrTime(){return this.arrTime;}

	public double getSerTime(){return this.serTime;}

	public double getEndTime(){return this.endTime;}

	public double getExpTime() {return this.expTime;}

	public double getELSTime() {return this.elapsedTime;}

	public double getBeginTime() {return this.beginTime;}

	public int getShiftTime(double time){ return (int)time/60; }

	// Mutators

	public void setFail(){ this.fail = true; }

	public void setNeedReDo(boolean b){ this.needReDo = b; }

	public void setArrTime(double time) { this.arrTime = time; }

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

	public void setEndTime(double time){
		endTime = time;
	}

	public void setBeginTime(double time){
		beginTime = time;
	}

	@Override
	public String toString() {
		return name + " ("+ arrTime + ")";
	}

	/****************************************************************************
	 *
	 *	Shado Object:	Task
	 *
	 *	Purpose:		Generate a new task on completion of old task. And return
	 *					it's vars.
	 *
	 ****************************************************************************/

	public Task() { }

	// Copy Constructor ---- used for creating a redo task
	// deep copy the original task
	// and set the arrival time follow the original task

	public Task(Task t){
		vehicleID = t.vehicleID;
		Type = t.getType();
		vars = t.vars;
		Priority = t.Priority;
		prevTime = t.getEndTime();
		Phase = getPhase(prevTime);
		if (Phase == vars.numPhases) {
			arrTime = -1;
			return;
		}
		shiftPeriod = getShiftTime(prevTime);
		this.fail = false;
		elapsedTime = 0;
		waitTime = 0;
		expired = false;
		workSchedule = new ArrayList<>();

		arrTime = prevTime;
		serTime = t.serTime;
		expTime = t.expTime;
		name = t.name;
		repeatTimes = t.repeatTimes + 1;
	}

	// Constructor

	public Task(int type, double PrevTime, loadparam Param, boolean fromPrev, int vehicle) throws Exception{

		Type = type;
		vehicleID = vehicle;
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
		repeatTimes = 0;
		name = vars.taskName_all[type];

		if(type < vars.numTaskTypes){

			if (fromPrev == true) {
				arrTime = genArrTime(PrevTime, Type);
			} else {
				arrTime = PrevTime;
			}

			Phase = getPhase(arrTime);

			if (Phase == vars.numPhases) {
				arrTime = -1;
				return;
			}

			expTime = genExpTime();
			serTime = GenTime(vars.serDists[Type], vars.serPms[Type]);

		}
		else {

			expTime = Double.POSITIVE_INFINITY;
			Priority = 0;

			if(type == vars.TC_SOME_TASK){
				arrTime = PrevTime + Exponential(10);
				serTime = Exponential(0.1667);
				Phase = getPhase(arrTime);
			}
			else if(type == vars.TC_FULL_TASK){
				arrTime = PrevTime + Exponential(5);
				serTime = Exponential(0.1667);
				Phase = getPhase(arrTime);
			}
			else if(type == vars.EXOGENOUS_TASK){
				arrTime = PrevTime + Exponential(480);
				serTime = Uniform(20,40);
				Priority = 7;
				Phase = getPhase(arrTime);
			}
			else if(type == vars.TURN_OVER_BEGIN_TASK){
				arrTime = PrevTime;
				if(vars.phaseBegin[1] == 0) {
					serTime = GenTime(vars.turnOverDists[0], vars.turnOverPms[0]);
					vars.phaseBegin[1] = serTime;
				}
				else{
					serTime = vars.phaseBegin[1];
				}
				Priority = 7;
				Phase = 0;
			}
			else if(type == vars.TURN_OVER_END_TASK){

				if(vars.phaseBegin[vars.numPhases - 1] == 0){
					serTime = GenTime(vars.turnOverDists[1], vars.turnOverPms[1]);
					arrTime = vars.numHours * 60 - serTime;
					vars.phaseBegin[vars.numPhases - 1] = arrTime;
				}
				else{
					arrTime = vars.phaseBegin[vars.numPhases - 1];
					serTime = vars.numHours * 60 - arrTime;
				}

				Priority = 7;
				Phase = vars.numPhases - 1;
			}

			if (Phase == vars.numPhases) {
				arrTime = -1;
				return;
			}

		}

		beginTime = arrTime;

		//shift schedule 1% fatigue increase serve time
		if(type != vars.TURN_OVER_BEGIN_TASK && type != vars.TURN_OVER_END_TASK)
			changeServTime(1 + 0.01 * shiftPeriod);

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

		//the followed task should always behind the lead task
		if(this.getType() < vars.numTaskTypes && vars.leadTask[this.getType()] >= 0){
			int leadType = vars.leadTask[this.getType()];
			if(other.getType() == leadType){
				if (this.arrTime > other.arrTime){ //the old task(other) arrives first, it should come first
					return 1;
				} else {
					return -1;
				}
			}
		}

		// If the old task cannot be interrupted
		if(vars.interruptable[other.getType()] == 0 || vars.essential[other.getType()] == 1)
			return 1;

		// If the new task is essential task, which can interrupt any other task
		if(vars.essential[this.getType()] == 1)
			return -1;

		// If the new task is one of the special tasks
		if(this.getType() >= vars.numTaskTypes)
			return 1;

		if(teamType == vars.FLEXTEAM || vars.opStrats[teamType].equals("PRTY")){
			if(other.Priority > this.Priority) return 1;
			else if(other.Priority < this.Priority) return -1;
			// If two tasks have same priority, use FIFO
			if (this.arrTime > other.arrTime) return 1;
			return -1;
		}

		else if(teamType != vars.FLEXTEAM && vars.opStrats[teamType].equals("FIFO")){
			if (this.arrTime > other.arrTime){ //the old task(other) arrives first, it should come first
				return 1;
			} else {
				return -1;
			}
		}

		else if(teamType != vars.FLEXTEAM && vars.opStrats[teamType].equals("STF")){
			if(other.getSerTime() < this.getSerTime() - this.getELSTime()){ //this task needs more serve time, other task should come first
				return 1;
			} else {
				return -1;
			}
		}

		return 1;

	}


	/****************************************************************************
	 *
	 *	Method:			GetPhase
	 *
	 *	Purpose:		Return the Phase
	 *
	 ****************************************************************************/

	public int getPhase(double time){
		if (time > vars.numHours * 60) {
			return vars.numPhases;
		}
		int currentPhase = 0;
		for (int i = 0; i < vars.numPhases; i++) {
			if (vars.phaseBegin[i] <= time) {
				currentPhase = i;
			}
			else break;
		}
		return currentPhase;
	}


	/****************************************************************************
	 *
	 *	Method:			genTime
	 *
	 *	Purpose:		Generate a new time with the specified type and vars.
	 *
	 ****************************************************************************/

	private double GenTime (char type, double[] param) throws Exception{

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
			case 'N':
				return Double.POSITIVE_INFINITY;
			default:
				throw new IllegalArgumentException("Wrong Letter for the distribution.");
		}
	}

	/****************************************************************************
	 *
	 *	Method:			Exponential
	 *
	 *	Purpose:		Return an exponential distributed random number with input
	 *					vars lambda.
	 *
	 ****************************************************************************/

	private double Exponential(double beta) throws Exception{

		if(beta <= 0){
			throw new Exception("Exponential distribution: Please offer a positive mean value.");
		}

		double lambda = 1 / beta;
		double result = Math.log(1- Math.random()) / (- lambda);
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

	private double Lognormal(double mean, double stddev) throws Exception{

		if(mean <= 0 || stddev < 0){
			throw new Exception("Lognormal distribution: Please offer positive mean and std value.");
		}

		double phi = Math.sqrt(stddev * stddev + mean * mean);
		double mu = Math.log(mean * mean / phi);
		double sigma = Math.sqrt(Math.log((phi * phi) / (mean * mean)));

		Random rng = new Random();
		double normal = rng.nextGaussian();
		double l = Math.exp(mu + sigma * normal);

		return l;
	}

	/****************************************************************************
	 *
	 *	Method:			Uniform
	 *
	 *	Purpose:		Return a uniform distribution with input minimum and maximum
	 *
	 ****************************************************************************/

	private double Uniform(double min, double max) throws Exception{

		if(min > max){
			throw new Exception("Uniform distribution(Error): min > max.");
		}

		return min + (max-min) * Math.random();

	}

	/****************************************************************************
	 *
	 *	Method:			Triangular
	 *
	 *	Purpose:		Return a triangular distributed random variables
	 *
	 ****************************************************************************/

	private double Triangular(double min, double mode, double max) throws Exception{

		if (!(min < mode && mode < max)) {
			throw new Exception("For the triangular distribution: please offer min < mode < max.");
		}

		if (min <= 0) {
			throw new Exception("Please offer positive value for the triangular distribution.");
		}

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
	 *	Method:			genArrTime
	 *
	 *	Purpose:		Generate a new exponentially distributed arrival time by phase.
	 *
	 ****************************************************************************/

	private double genArrTime(double PrevTime, int type) throws Exception{

		int fleet = vehicleID / 100;

		double[] arrivalRate = changeArrivalRate(getFleetAutonomy(fleet), type);
		double TimeTaken = GenTime(vars.arrDists[type], arrivalRate);

		if (TimeTaken == Double.POSITIVE_INFINITY) {
			return -1;
		}

		double newArrTime = TimeTaken + PrevTime;

		if (type < vars.numTaskTypes && vars.affByTraff[type] == 1 ){
			newArrTime = applyTraffic(TimeTaken);
		}

		return newArrTime;
	}


	//SCHEN 12/16/17 Add changing the arrival rate based on the traffic level
	private double applyTraffic(double TimeTaken){

		double budget = TimeTaken;
		double currTime = prevTime;
		int currHour = (int) currTime/60;
		double traffLevel = vars.traffic[vehicleID / 100][currHour];
		double TimeToAdj = (currHour + 1) * 60 - currTime;
		double adjTime = TimeToAdj * traffLevel;

		while (budget > adjTime) {

			budget -= adjTime;
			currTime += TimeToAdj;
			currHour ++;

			if (currHour >= vars.traffic.length) {
				return Double.POSITIVE_INFINITY;
			}

			traffLevel = vars.traffic[vehicleID / 100][currHour];
			TimeToAdj = (currHour + 1) * 60 - currTime;
			adjTime = TimeToAdj * traffLevel;

		}
		return currTime + budget / traffLevel;

	}

	/****************************************************************************
	 *
	 *	Method:			genExpTime
	 *
	 *	Purpose:		Generate a new Expiration time.
	 *
	 ****************************************************************************/

	private double genExpTime() throws Exception{

		double expiration;
		expiration = GenTime(vars.expDists[Type], vars.expPms[Type]);
		return arrTime + serTime + expiration;

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
	}

	private double[] changeArrivalRate(double num, int type){

		double[] arrivalRate;

		//check if it has type 2 exogenous factor (increasing arrival rate)
		if(vars.hasExogenous[1] == 1 && vars.exoType2Aff[type] == 1){
			num *= 1.1;
		}

		arrivalRate = new double[vars.arrPms[Type].length];
		if(vars.arrDists[Type] == 'L'){
			arrivalRate[0] = vars.arrPms[Type][0] * num;
			arrivalRate[1] = vars.arrPms[Type][1];
			return arrivalRate;
		}

		int count = 0;
		for(double d : vars.arrPms[Type]){
			arrivalRate[count] = d * num;
			count++;
		}

		return arrivalRate;
	}

	/****************************************************************************
	 *
	 *	Method:			addBeginTime, addInterruptTime
	 *
	 *	Purpose:		To add a working period in the workSchedule by adding a
	 *					pair of begin time and end time.
	 *
	 ****************************************************************************/

	public void addBeginTime(double beginTime){
		double[] newSchedule = new double[2];
		newSchedule[0] = beginTime;
		newSchedule[1] = 0;
		workSchedule.add(newSchedule);
	}

	public void addInterruptTime(double time){
		int lastOne = workSchedule.size() - 1;
		workSchedule.get(lastOne)[1] = time;
	}

	/****************************************************************************
	 *
	 *	Method:			printBasicInfo
	 *
	 *	Purpose:		Print the basic information for a task. Used for debugging.
	 *
	 ****************************************************************************/

	public void printBasicInfo(){

		System.out.println("Name : " + name + " Priority : " + Priority);
		System.out.println("Arrival time : " + arrTime);
		System.out.println("Begin Time : " + beginTime);
		System.out.println("Service Time : " + serTime);
		System.out.println("Expire Time : " + expTime);
		System.out.println("Finish Time : " + endTime);
		System.out.println("Wait Time : " + waitTime);
		System.out.println("Wait Time(Calc) : " + Queue.round(endTime - arrTime - serTime, 2));
		System.out.print("Here is my work schedule: ");
		for(int i = 0; i < workSchedule.size(); i++){
			System.out.print(workSchedule.get(i)[0] + "~" + workSchedule.get(i)[1] + "|");
		}
		System.out.println(" ");

	}

}


