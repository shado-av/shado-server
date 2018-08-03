package server.Engine;
import com.google.gson.Gson;
import server.Input.loadparam;

import java.util.Arrays;

/**
 * Created by siyuchen on 3/3/18.
 * A JSON Parser for SHADO
 */
public class Parser {

    String input;

    public Parser(String input){
        this.input = input;
    }

    /****************************************************************************
     *
     *	Method:			parseJSON
     *
     *	Purpose:		Use the JSON file to fill a loadparam variable.
     *
     ****************************************************************************/

    public loadparam parseJSON(loadparam in) throws Exception{
        Gson g = new Gson();
        in = g.fromJson(this.input, loadparam.class);
        checkInput(in);
        return in;
    }


    /****************************************************************************
     *
     *	Method:			checkInput
     *
     *	Purpose:		Check the input format. Throw an exception if there is
     *                  any error.
     *
     ****************************************************************************/

    private void checkInput(loadparam in) throws Exception{
        if (in.numHours <= 0)
            throw new Exception("The shift hour should be positive.");

        if (in.numReps <= 0)
            throw new Exception("The replication number should be positive.");

        if (in.hasTurnOver.length != 2 ||
                (in.hasTurnOver[0] != 0 && in.hasTurnOver[0] != 1) ||
                (in.hasTurnOver[1] != 0 && in.hasTurnOver[1] != 1))
            throw new Exception("Please check your settings for turn over tasks.");

        checkDistribution("Turn Over Task",2, in.turnOverDists, in.turnOverPms);

        zeroORone("Extrem Conditions", 2, in.hasExogenous);

        if (in.hasFlexPosition != 0 && in.hasFlexPosition != 1)
            throw new Exception("Please indicate if there is a flex position operator.");

        if (in.numTeams <= 0)
            throw new Exception("The team number should be positive.");

        if (in.teamSize.length != in.numTeams)
            throw new Exception("Please check the team size.");

        for (int i : in.teamSize) {
            if (i <= 0)
                throw new Exception("Team size should be positive.");
        }

        if (in.opNames.length != in.numTeams)
            throw new Exception("Please check the team name. Maybe not enough input.");

        if (in.opStrats.length != in.numTeams)
            throw new Exception("Please check the team queuing strategy. Maybe not enough input.");

        if (in.opExpertise.length != in.numTeams || in.opExpertise[0].length != in.numTaskTypes || in.opExpertise[0][0].length != in.fleetTypes)
            throw new Exception("Please check the opExpertise matrix");
        else {
            for (int[][] i : in.opExpertise) {
                for (int[] j : i)
                    zeroORone("opExpertise", in.fleetTypes, j);
            }
        }

        if (in.taskPrty.length != in.numTeams || in.taskPrty[0].length != in.numTaskTypes)
            throw new Exception("Please check the task priority matrix");

        if (in.teamComm.length != in.numTeams)
            throw new Exception("Please check the team communication input");
        else {
            for (char c : in.teamComm)
                if (c != 'N' && c != 'S' && c != 'F')
                    throw new Exception("Please check the team communication input");
        }

        if (in.ECC.length != in.numTeams || in.ECC[0].length != in.numTaskTypes)
            throw new Exception("Please check the Error Catching Chance input");
        else {
            for (double[] oneTeam : in.ECC) {
                for (double ecc : oneTeam) {
                    if (ecc < 0 || ecc > 1)
                        throw new Exception("The Error Catching Chance should between 0 and 1");
                }
            }
        }

        if (in.fleetTypes < 0)
            throw new Exception("The number of fleet types should be positive.");

        if (in.numvehicles.length != in.fleetTypes)
            throw new Exception("Please check the vehicle number input for each fleet input");

        for (int i : in.numvehicles) {
            if (i <= 0 || i > 99)
                throw new Exception("The vehicle number for each fleet should between 0 and 99");
        }

        if (in.autolvl.length != in.fleetTypes)
            throw new Exception("Please check the auto level input");
        else {
            for (char c : in.autolvl)
                if (c != 'N' && c != 'S' && c != 'F')
                    throw new Exception("Please check the auto level input");
        }

        if (in.fleetHetero.length != in.fleetTypes || in.fleetHetero[0].length <= 0 || in.fleetHetero[0].length > in.numTaskTypes)
            throw new Exception("Please check the fleet hetero input");

        if (in.traffic.length != in.fleetTypes || in.traffic[0].length != in.numHours)
            throw new Exception("Please check the fleet traffic input");

        if (in.numTaskTypes <= 0)
            throw new Exception("The number of task types should be positive.");

        if (in.taskNames.length != in.numTaskTypes)
            throw new Exception("Please check the task name input");

        checkDistribution("Task arrive distribution", in.numTaskTypes, in.arrDists, in.arrPms);
        checkDistribution("Task Serve Time Distribution", in.numTaskTypes, in.serDists, in.serPms);
        checkDistribution("Task Expiration Distribution", in.numTaskTypes, in.expDists, in.expPms);

        zeroORone("Affect by Traffic matrix", in.numTaskTypes, in.affByTraff);
        zeroORone("Affect by Team Coordination", in.numTaskTypes, in.teamCoordAff);
        zeroORone("Affect by Extrem Conditions", in.numTaskTypes, in.exoType2Aff);
        zeroORone("Task Interruptable", in.numTaskTypes, in.interruptable);
        zeroORone("Task Essential", in.numTaskTypes, in.essential);

        if (in.humanError.length != in.numTaskTypes || in.humanError[0].length != 3)
            throw new Exception("Please check the human error input");

        if (in.leadTask.length != in.numTaskTypes)
            throw new Exception("Please check the lead task matrix");

    }

    private void checkDistribution(String object, int num, char[] dists, double[][] pms) throws Exception{

        if (dists.length != num) throw new Exception(object + ": Please check the type for distribution");
        if (pms.length != num) throw new Exception(object + ": Please check the parameters for distribution");

        for (int i = 0; i < num; i++) {

            if ((dists[i] == 'E' && pms[i].length == 1) ||
                    (dists[i] == 'L' && pms[i].length == 2) ||
                    (dists[i] == 'U' && pms[i].length == 2) ||
                    (dists[i] == 'C' && pms[i].length == 1) ||
                    (dists[i] == 'T' && pms[i].length == 3) ||
                    dists[i] == 'N')
                continue;
            throw new Exception(object + ": Please check the parameters for distribution");

        }
    }

    private void zeroORone(String object, int num, int[] target) throws Exception{

        if (target.length != num) throw new Exception("Please check the " + object);

        for (int i : target) {
            if (i != 0 && i != 1)
                throw new Exception(": can only be 0 or 1");
        }
    }


}
