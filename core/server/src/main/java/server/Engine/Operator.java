package server.Engine;
import server.Input.loadparam;

/***************************************************************************
 *
 * 	FILE: 			Operator.java
 *
 * 	AUTHOR: 		ROCKY LI
 *
 * 	LATEST EDIT:	07/09/2018
 *
 * 	VER: 			1.0
 * 					2.0 Naixin Yu
 *
 * 	Purpose: 		generate operator that wraps Queue objects. This is where
 * 					the distinction between an operator and a RemoteOper is made.
 *
 **************************************************************************/

public class Operator {

	public 	int 		dpID;
	public 	String 		name;
	private Queue 		myQueue;
	public 	loadparam 	vars;
	public 	boolean		isAI;


	// Inspector
	public Queue  getQueue(){ return this.myQueue; }
	public String getName()	{ return this.name; }

	@Override
	public String toString(){ return "This is " + name + "\n"; }

	/****************************************************************************
	 *
	 *	Shado Object:	RemoteOperator
	 *
	 *	Purpose:		Generate a RemoteOperator from the vars file imported
	 *
	 ****************************************************************************/

	public Operator() { }

	public Operator(int dpid, String name, loadparam param) {

		isAI = false;
		dpID = dpid;
		this.name =  name +" No." + Integer.toString(dpid%100);
		myQueue = new Queue(this);
		vars = param;

	}

	/****************************************************************************
	 *
	 *	Method:		checkPhase
	 *
	 *	Purpose:	Return the phase this operator currently in
	 *
	 ****************************************************************************/

	public int checkPhase(){

		double time = myQueue.getTime();

		if (time > vars.numHours * 60) {
			return vars.numPhases;
		}

		int phase = 0;
		for (; phase < vars.numPhases; phase++) {
			if (time <= vars.phaseBegin[phase]) {
				break;
			}
		}
		return phase - 1;

	}

	/****************************************************************************
	 *
	 *	Method:		getBusyIn10min
	 *
	 *	Purpose:	Return how much time this operator is working in the
	 *				past 10 mins
	 *
	 ****************************************************************************/

	public double getBusyIn10min(double timeNow){

		double time = 0;

		//check if there is currently doing task
		if (!myQueue.taskqueue.isEmpty()){

			Task t = myQueue.taskqueue.peek();

			for(double[] workingTime : t.workSchedule) {

				if (workingTime[1] == 0 && workingTime[0] != 0) {
					if (workingTime[0] < timeNow - 10)
						return 10;
					time += timeNow - workingTime[0];
					continue;
				}
				if (workingTime[1] < timeNow - 10) {
					continue;
				}
				time += Math.min(timeNow, workingTime[1]) - Math.max(timeNow - 10, workingTime[0]);

			}
		}


		//check finished tasks
		for (int i = myQueue.records().size() - 1; i >= 0; i--) {

			if (timeNow - myQueue.records().get(i).getEndTime() > 10) {
				break;
			}

			Task t =  myQueue.records().get(i);

			for(double[] workingTime : t.workSchedule) {

				if (workingTime[1] < timeNow - 10) {
					continue;
				}

				time += Math.min(timeNow, workingTime[1]) - Math.max(timeNow - 10, workingTime[0]);

			}
		}
		return time;
	}


}
