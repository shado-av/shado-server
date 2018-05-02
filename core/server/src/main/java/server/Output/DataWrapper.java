package server.Output;

import java.io.*;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import server.Engine.*;
import server.Input.FileWizard;
import server.Input.loadparam;
import java.util.*;
import javafx.util.Pair;


/***************************************************************************
 *
 * 	FILE: 			DataWrapper.java
 *
 * 	AUTHOR: 		ROCKY LI
 *
 * 	DATE:			2017/6/5
 *
 * 	VER: 			1.0
 *
 * 	Purpose: 		Wrapping the data field for analysis.
 *
 **************************************************************************/

public class DataWrapper {

    public loadparam vars;

    private Simulation sim;

    private String file_head;

    public void setFileHead(){
        file_head = FileWizard.getabspath();
    }

    public DataWrapper(Simulation o, loadparam param) {
        vars = param;
        sim = o;
    }

    /****************************************************************************
     *
     *	Method:     output
     *
     *	Purpose:    Generate csv files
     *
     ****************************************************************************/

    public void output() throws IOException {

//        setFileHead();
        //Clean previous Summary Dir every output

        String localSummary = "/Users/siyuchen/Documents/CS/DukeCS/shado-server/core/server/out/Summary/";
        String localOut = "/Users/siyuchen/Documents/CS/DukeCS/shado-server/core/server/out/";
//        File summaryDir = new File("/home/rapiduser/shado-server/core/server/out/Summary");
//        File csvDir = new File("/home/rapiduser/shado-server/core/server/out/repCSV");
        File directory = new File("/Users/siyuchen/Documents/CS/DukeCS/shado-server/core/server/out/Summary");
        FileUtils.cleanDirectory(directory);
//        FileUtils.cleanDirectory(summaryDir);
//        FileUtils.cleanDirectory(csvDir);

        // RemoteOp & Engineer timetables
        for (int i = 0; i < vars.numRemoteOp; i++) {
//            String file_name =   "/home/rapiduser/shado-server/core/server/out/" + "RemoteOperator" + ".csv";
            String file_name =   localOut + "RemoteOperator" + ".csv";
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(file_name, false)), true));
            sim.getRemoteOpoutput(i).outputdata();
        }
// "a,b,c"
        for (int j = 0; j < vars.numTeams; j++) {
//            String file_name =  "/home/rapiduser/shado-server/core/server/out/" + vars.opNames[j] + ".csv";
            String file_name =  localOut + vars.opNames[j] + ".csv";
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(file_name, false)), true));
            sim.getOperatoroutput(j).outputdata();

        }

        // Expired Tasks

//        String file_name =  "/home/rapiduser/shado-server/core/server/out/Summary/" + "Simulation_Summary" + ".csv";
        String file_name =  localSummary + "Simulation_Summary" + ".csv";
        System.setOut(new PrintStream(new BufferedOutputStream(
                new FileOutputStream(file_name, false)), true));
        System.out.println("--- Simulation Summary---");
        System.out.println("Tasks generated:");
        for(int i = 0; i < vars.numReps; i++){
            System.out.println("Rep_"+i+","+vars.repNumTasks[i]);
        }
        for (int i = 0; i < vars.numTaskTypes; i++) {
            System.out.println("Task name: " + vars.taskNames[i]);
            System.out.println("expired: " + sim.getExpiredtask()[i]);
            System.out.println("completed: " + sim.getCompletedtaskcount()[i]);


        }
        System.out.println("*** FAILED TASKS ***");
//            System.out.println("Operator "+ p.getKey().getName()+" Failed: "+p.getValue().getName());
        for(int i = 0 ; i< vars.numReps; i++){
            HashMap<Integer,Integer> failCnt = vars.failTaskCount;
            int currFailCnt = failCnt.get(i);
            System.out.println("In Replication " + i +": "+ "Number of Fail Tasks: "+currFailCnt);
        }

        for(int i = 0; i < vars.numReps;i++) {
//            String summary_file_name = "/home/rapiduser/shado-server/core/server/out/Summary/" + "Error_Summary_Rep_" +i+ ".csv";
            String summary_file_name = localSummary + "Error_Summary_Rep_" +i+ ".csv";
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(summary_file_name, false)), true));
            System.out.println("Fail Task Detail: ");
            ArrayList<Pair<Operator,Task>> failList = vars.rep_failTask.get(i);
            for(int k = 0 ; k < failList.size(); k++){
                String opName = failList.get(k).getKey().getName();
                String tName = failList.get(k).getValue().getName();
                System.out.print(opName+" Fails " +tName+ ",");
                if(failList.get(k).getValue().getFail()){
                    System.out.print(" But still proceed by the Operator");
                }
                System.out.println();
            }


        }
        //Cross-Replication Summary for workloads
//        String summary_file_name =   "/home/rapiduser/shado-server/core/server/out/Summary/" + "Workload_Summary.csv";
        String summary_file_name =   localSummary + "Workload_Summary.csv";
        System.setOut(new PrintStream(new BufferedOutputStream(
                new FileOutputStream(summary_file_name, false)), true));

        double[] workloads  = new double[3];
        double workloadSum = 0;
        int columnCnt;
        for(double[] x: vars.crossRepCount){
            columnCnt = 0;
            for(double y: x){
                workloads[columnCnt++] += y;
                workloadSum += y;
            }
            //System.out.println(Arrays.toString(vars.crossRepCount[i]));
        }
        //Display Utilization for each team
        //repUtilOp:[numRep][Team][operator]
        double[][] teamUtil = new double[vars.numReps][vars.numTeams];
        int repPtr = 0;
        for(double[][] rep_team_op: vars.repUtilOp){
            int teamPtr = 0;
            for(double[] team: rep_team_op){
                double teamAvg = 0;
                double teamSum = 0; int teamCnt = 0;
                for(double op: team){
                    teamSum += op;
                    teamCnt++;
                }
                teamAvg = teamSum/teamCnt;
                teamUtil[repPtr][teamPtr] = teamAvg;
                teamPtr++;
            }
            repPtr++;
        }
        //Get mean across replications
        double[] teamUtilCrossRep = new double[vars.numTeams];
        for (int i = 0; i < vars.numTeams; i++){
            double teamSum = 0;
            for(int j = 0; j < vars.numReps;j++){
               teamSum += teamUtil[j][i];
            }
            teamUtilCrossRep[i] = teamSum/vars.numReps;
        }



        double percentage_0 = (workloads[0]/workloadSum) * 100;
        double percentage_30 = workloads[1]/workloadSum * 100;
        double percentage_70 = workloads[2]/workloadSum * 100;
        System.out.println("Workload Summary");
        System.out.println("Percentage of utilization 0-30%,Percentage of utilization 30-70%,Percentage of utilization 70-100% ");
        System.out.println(percentage_0+"% ,"+percentage_30+"% ,"+percentage_70+"%,");
        System.out.println("AVG Utilization For Each Team");
        for(int i = 0 ; i < vars.numTeams;i++ ){
            System.out.println("Team "+ vars.opNames[i]+", "+ teamUtilCrossRep[i]);
        }
        //ADD 1 line
        System.out.println("one more line");
    }

}

