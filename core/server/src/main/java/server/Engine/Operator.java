package server.Engine;
import java.util.*;
import server.Input.loadparam;

/***************************************************************************
 *
 * 	FILE: 			Operator.java
 *
 * 	AUTHOR: 		ROCKY LI
 *
 * 	LATEST EDIT:	2017/5/24
 *
 * 	VER: 			1.0
 *
 * 	Purpose: 		generate operator that wraps Queue objects. This is where
 * 					the distinction between an operator and a RemoteOper is made.
 *
 **************************************************************************/

public class Operator {

	public int dpID;

	public String name;

	public int[] taskType;

	private Queue myQueue;

	// Inspector

	public Queue getQueue(){
		return this.myQueue;
	}

	public String getName(){return this.name;}

	@Override
	public String toString() { return "This is operator " + name; }

	/****************************************************************************
	 *
	 *	Shado Object:	RemoteOperator
	 *
	 *	Purpose:		Generate a RemoteOperator from the vars file imported
	 *
	 ****************************************************************************/

	public Operator(int dpid, String name,int[] task) {

		taskType = task;
		this.name =  name +" " + Integer.toString(dpid%100);
		dpID = dpid;
		myQueue = new Queue(this);

	}

}
