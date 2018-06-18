package server.Engine;
import com.google.gson.Gson;
import server.Input.ShadoVar;
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
        /*
         Gson g = new Gson();
         Person person = g.fromJson("{\"name\": \"John\"}", Person.class);
         System.out.println(person.name); //John
         System.out.println(g.toJson(person)); // {"name":"John"}
         */
    }
    public loadparam parseJSON(loadparam in){
        Gson g = new Gson();
        System.out.println("PARSING JSON...");
        in = g.fromJson(this.input, loadparam.class);
        printloadparam(in);
        in.setGlobalData();
//        System.out.println("Traffic: "+ Arrays.toString(in.traffic));
//        System.out.println("OpStrats: "+in.opStrats);
////        System.out.println("OpNames: "+ in.opNames[0]);
//        System.out.println("Fleet Hetero: "+Arrays.toString(in.fleetHetero));
        return in;
    }

    private void printloadparam(loadparam in){
        System.out.println("Here is the JSON pasing result: ");
        System.out.println("numHours: " + in.numHours);
        System.out.println("traffic: "+ Arrays.toString(in.traffic));
        System.out.println("numReps: " + in.numReps);
        System.out.println("numvehicles: "+ Arrays.toString(in.numvehicles));
        System.out.println("numRemoteOp: " + in.numRemoteOp);
        System.out.println("numTeams: " + in.numTeams);
        System.out.println("numPhases: " + in.numPhases);
        System.out.println("phaseBegin: " + Arrays.toString(in.phaseBegin));
        System.out.println("autolvl: " + Arrays.toString(in.autolvl));
        System.out.println("the exo: " + Arrays.toString(in.hasExogenous));
        System.out.println("failThreshold: " + in.failThreshold);
        System.out.println("OpStrats: "+in.opStrats);
        System.out.println("OpNames: "+ in.opNames[0]);
//        System.out.println("opTasks: : " + Arrays.toString(in.opTasks));
        System.out.println("teamComm: "+ Arrays.toString(in.teamComm));
        System.out.println("teamSize: "+ Arrays.toString(in.teamSize));
        System.out.println("fleetTypes: "+in.fleetTypes);
//        System.out.println("Fleet Hetero: "+Arrays.toString(in.fleetHetero));
        System.out.println("numTaskTypes: "+in.numTaskTypes);
        System.out.println("taskNames: "+ Arrays.toString(in.taskNames));
        for(int i = 0; i < in.numTaskTypes; i++) {
            System.out.println("For task type " + i);
            System.out.print("taskPrty:");
            for(int j = 0; j < in.numPhases; j++) {
                System.out.print(in.taskPrty[i][j] + " ");
            }
            System.out.println(" ");
        }

        for(int i = 0; i < in.numTeams; i++){
            System.out.print("The AIDA for team " + i + ": ");
            for(int j = 0; j < 3; j++){
                System.out.print(" " + in.AIDAtype[i][j]);
            }
            System.out.println(" ");
        }
        //System.out.println("arrpms: " + Arrays.toString(in.arrPms));
    }

}
