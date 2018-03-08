package server.Engine;

import server.Input.FileWizard;
import server.Input.ShadoVar;
import server.Input.loadparam;
import server.Output.DataWrapper;
//import Output.OutputTest;
import server.Output.ProcRep;

import java.io.*;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Scanner;


/***************************************************************************
 * 	FILE: 			Shado.java
 *
 * 	AUTHOR: 		ROCKY LI
 * 	LATEST_EDIT:	2017/9/12
 *
 * 	VER: 			1.2
 * 	Purpose: 		Entry point.
 **************************************************************************/


public class Shado{

	public Shado(){}

	public void runShado(String inputJson) throws Exception{
		String head = FileWizard.getabspath();

        System.out.println("INPUT: "+inputJson);
        loadparam data = new loadparam();
        Parser parser = new Parser(inputJson);
        data = parser.parseJSON(data);
        loadparam txtData;
        try {
             txtData = new loadparam("../in/params.txt");
            printBasicInfo(txtData);
        }catch (FileNotFoundException e){
            System.err.println("ERROR: Cannot find local file!");
        }
//		SCHEN 11/10/17 Test for Reading Fleet Hetero

//        printBasicInfo(data);


		// Runs simulation
//
		Simulation sim = new Simulation(data);
		sim.run();
		System.out.println("Failed Tasks: "+ data.failTaskCount);

		// Generate Output
		DataWrapper analyze = new DataWrapper(sim, data);
		analyze.output();
	}
	
	private static void printBasicInfo(loadparam data){
		System.out.println("FleetHetero: "+ Arrays.deepToString(data.fleetHetero));
		System.out.println("Fleet Types: "+ data.fleetTypes);
		System.out.println("numvehicles: "+ Arrays.toString(data.numvehicles));
		System.out.println("autoLevel: "+ data.autolvl);
		System.out.println("team Communication: "+ Arrays.toString(data.teamComm));
		System.out.println("hasExo: "+ Arrays.toString(data.hasExogenous));
		System.out.println("exNames: "+ Arrays.toString(data.exNames));
		System.out.println("exTypes: "+ Arrays.toString(data.exTypes));
		System.out.println("Total Number of Remote Ops: "+ data.teamSizeTotal);
		System.out.println("Remote Ops taskType"+ Arrays.deepToString(data.opTasks));
		System.out.println("Human Error Input: "+ Arrays.deepToString(data.humanError));
	}

}
