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
    TaskRecord taskRecord;

    public JasonBuilder(String output, Utilization u, TaskRecord t){
        outputDirectory = output;
        utilization = u;
        taskRecord = t;
    }

    public void outputJSON() throws IOException {
        PrintStream stdout = System.out;
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        String summary_file_name = outputDirectory + "Utilization.json";
        System.setOut(new PrintStream(new BufferedOutputStream(
                new FileOutputStream(summary_file_name, false)), true));
        System.out.println(gson.toJson(utilization));

        summary_file_name = outputDirectory + "TaskRecord.json";
        System.setOut(new PrintStream(new BufferedOutputStream(
                new FileOutputStream(summary_file_name, false)), true));
        System.out.println(gson.toJson(taskRecord));
        System.setOut(stdout);
    }
}
