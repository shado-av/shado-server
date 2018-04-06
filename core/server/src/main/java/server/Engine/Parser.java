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
        in.setGlobalData();
//        System.out.println("Traffic: "+ Arrays.toString(in.traffic));
//        System.out.println("OpStrats: "+in.opStrats);
////        System.out.println("OpNames: "+ in.opNames[0]);
//        System.out.println("Fleet Hetero: "+Arrays.toString(in.fleetHetero));
        return in;
    }

}
