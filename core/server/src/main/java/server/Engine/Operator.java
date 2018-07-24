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
	public Queue 	getQueue()	{ return this.myQueue; }
	public String 	getName()	{ return this.name; }

	@Override
	public String toString() { return "This is " + name; }

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
		this.name =  name +" " + Integer.toString(dpid%100);
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

}
