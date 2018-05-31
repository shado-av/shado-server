package server.Engine;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import server.Input.FileWizard;
import server.Input.loadparam;
import server.Output.*;
//import Output.OutputTest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipOutputStream;


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
    String sessionNum;
    private String rootDirectory;
	public Shado(String sess, String directory){
		sessionNum = sess;
		rootDirectory = directory;
    }

    private static ZipOutputStream zos;
	public void runShado(String inputJson) throws Exception{
		String head = FileWizard.getabspath();

        System.out.println("INPUT: "+inputJson);
        loadparam data = new loadparam();
        Parser parser = new Parser(inputJson);
        data = parser.parseJSON(data);
//        loadparam txtData;
//        try {
//             txtData = new loadparam("../in/params.txt");
//             printBasicInfo(txtData);
//        }catch (FileNotFoundException e){
//            System.err.println("ERROR: Cannot find local txt file!");
//        }
//		SCHEN 11/10/17 Test for Reading Fleet Hetero

//        printBasicInfo(data);


		// Runs simulation
		Simulation sim = new Simulation(data);

<<<<<<< HEAD
//		String directoryName = "/Users/zhanglian1/shado-server/core/server/out/repCSV/";
		String directoryName = "/home/rapiduser/shado-server/core/server/out/repCSV/";
=======
		String directoryName = rootDirectory + "repCSV/";
>>>>>>> origin/naixin-JSON
        File directory = new File(directoryName);
        FileUtils.cleanDirectory(directory);

		sim.run();
		System.out.println("Failed Tasks: "+ data.failTaskCount);

		// Generate Output
		DataWrapper analyze = new DataWrapper(sim, data);
		analyze.testOutput();
<<<<<<< HEAD
		//Zipping file and return for simple web service
        zipOutput("/home/rapiduser/shado-server/core/server/out/repCSV");
        zipOutput("/home/rapiduser/shado-server/core/server/out/Summary");

        //Local path
//		zipOutput("/Users/zhanglian1/shado-server/core/server/out/repCSV");
//		zipOutput("/Users/zhanglian1/shado-server/core/server/out/Summary");
        System.out.println("SIMULATION DONE");
=======

		//Zipping file and return for simple web service
		zipOutput(rootDirectory + "repCSV");
		zipOutput(rootDirectory + "Summary");
//        System.out.println("SIMULATION DONE");
>>>>>>> origin/naixin-JSON
    }

	public void zipOutput(String path){
        String dirPath = path;
        Path sourceDir = Paths.get(dirPath);

        try {
            String zipFileName = dirPath.concat(".zip");
            zos = new ZipOutputStream(new FileOutputStream(zipFileName));

            Files.walkFileTree(sourceDir, new ZipDir(sourceDir,zos));

            zos.close();
        } catch (IOException ex) {
            System.err.println("I/O Error: " + ex);
        }
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
