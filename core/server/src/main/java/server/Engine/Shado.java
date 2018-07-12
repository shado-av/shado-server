package server.Engine;


import server.Input.FileWizard;
import server.Input.loadparam;
import server.Output.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipOutputStream;


/***************************************************************************
 * 	FILE: 			Shado.java
 *
 * 	AUTHOR: 		ROCKY LI, Naixin Yu
 * 	LATEST_EDIT:	07/06/2018
 *
 * 	VER: 			2.0
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

//        printBasicInfo(data);


		// Runs simulation
		Simulation sim = new Simulation(data);

		sim.run();

		// Generate Output
		DataWrapper analyze = new DataWrapper(sim, data, rootDirectory);
		analyze.outputReports();

		//Zipping file and return for simple web service
		zipOutput(rootDirectory + "repCSV");
		zipOutput(rootDirectory + "Summary");
		zipOutput(rootDirectory + "validation");

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

}
