package server.Output;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class JasonBuilder {

    String outputDirectory;
    Utilization utilization;

    public JasonBuilder(String output, Utilization u){
        outputDirectory = output;
        utilization = u;
    }

    public void outputJSON() throws IOException {
        PrintStream stdout = System.out;
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String summary_file_name = outputDirectory + "Utilization.js";
        System.setOut(new PrintStream(new BufferedOutputStream(
                new FileOutputStream(summary_file_name, false)), true));
        System.out.println(gson.toJson(utilization));
        System.setOut(stdout);
    }
}
