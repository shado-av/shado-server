package server.Engine;
import server.Input.loadparam;

/***************************************************************************
 *
 * 	FILE: 			RemoteOp.java
 *
 * 	AUTHOR:			ROCKY LI
 *                  Richard Chen
 *
 * 	DATE:			2017/6/12, 2017/12/3
 *
 * 	VER: 			1.1
 *
 * 	Purpose: 		Create simulation for multiple vehicle and RemoteOp
 *
 **************************************************************************/

public class RemoteOp {

    public loadparam vars;

    private Operator[] RemoteOpers;


    // Constructor is HERE

    public RemoteOp(loadparam Param) {
        vars = Param;
    }

    // Inspectors:

    public Operator[] getRemoteOp() {
        return RemoteOpers;
    }

    /****************************************************************************
     *
     *	Method:			genRemoteOp
     *
     *	Purpose:		Generate RemoteOperators
     *
     *                  e.g. 3 teams, each has 2 ops:
     *
     *                  [dis1,dis2,AI1,AI2,Mgmt1,mgmt2]
     *                   000  002  100 101  200   201
     *                  201 % 100 == 1
     *
     ****************************************************************************/

    public void genRemoteOp() {
        // SCHEN 11/20/17
        // Note: RemoteOper is a 1d array, to fit in the data structure,
        // change it to 2d array with each subarray with length == 1

        RemoteOpers = new Operator[vars.numRemoteOp + vars.flexTeamSize];

        int cnt = 0;
        for (int i = 0; i < vars.numTeams; i++) {
            //generate Operator base on different types of remote Ops
            for (int j = 0; j < vars.teamSize[i]; j++) {
                RemoteOpers[cnt++] = new Operator(i * 100 + j, vars.opNames[i], vars);

            }
        }

        for (int i = 0; i < vars.flexTeamSize; i++) {
            RemoteOpers[vars.numRemoteOp + i] = new Operator(vars.numTeams * 100 + i, "flex position", vars);
        }

    }


}
