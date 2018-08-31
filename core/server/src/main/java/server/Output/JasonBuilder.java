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
    WaitTime waitTime;

    public JasonBuilder(String output, Utilization u, TaskRecord t, WaitTime w){
        outputDirectory = output;
        utilization = u;
        // simplify utilization (no need for visualization)
        utilization.taskUtilization = null;
        utilization.fleetUtilization = null;        
        
        taskRecord = t;

        waitTime = w;
        waitTime.taskWaitTime = null;
        waitTime.fleetWaitTime = null;
    }

    public synchronized void outputJSON() throws IOException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        String summary_file_name = outputDirectory + "Utilization.json";
        PrintStream ps = new PrintStream(new BufferedOutputStream(
                new FileOutputStream(summary_file_name, false)), true);
        ps.println(gson.toJson(utilization));
        ps.close();

        summary_file_name = outputDirectory + "TaskRecord.json";
        ps = new PrintStream(new BufferedOutputStream(
                new FileOutputStream(summary_file_name, false)), true);
        ps.println(gson.toJson(taskRecord));
        ps.close();

        summary_file_name = outputDirectory + "WaitTime.json";
        ps = new PrintStream(new BufferedOutputStream(
                new FileOutputStream(summary_file_name, false)), true);
        ps.println(gson.toJson(waitTime));
        ps.close();        
    }
}
