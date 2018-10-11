package server;

/**
 * SHADO HTTP request handler
 * Created by siyuchen on 3/1/18.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import server.Engine.Shado;

import javax.servlet.http.HttpServletResponse;

@CrossOrigin(maxAge = 3600)
@RestController
public class GreetingController {

    private final AtomicLong counter = new AtomicLong();
    private Shado shado;
    DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmssZ");
    Date date = new Date();

    private String homeDirectory = System.getProperty("user.home") + "/out/";
    private String directory = homeDirectory;

    @RequestMapping(value= "/shado/runShado",method = RequestMethod.POST)
    public String index(@RequestBody String payload) throws Exception{
        String sessionNum = dateFormat.format(date)+"_"+counter.incrementAndGet();
        directory = homeDirectory + sessionNum + "/";
        shado = new Shado(sessionNum, directory);
        shado.runShado(payload);
//        System.out.println(payload);
        return "Shado Successfully Run! SESSION #: "+ sessionNum + "\n";
    }

    //Output file download (For web-simple)
    @RequestMapping(value = "/shado/getRepDetail", method = RequestMethod.GET)
    public StreamingResponseBody getRepFile(@RequestParam(value="sessionN", defaultValue="") String sessionN, HttpServletResponse response) throws IOException {

        String fileName = homeDirectory + sessionN + "/repCSV.zip";

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"repCSV.zip\"");
        InputStream inputStream = new FileInputStream(new File(fileName));

        return outputStream -> {
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                System.out.println("Writing some bytes for RepCSV..");
                outputStream.write(data, 0, nRead);
            }
            inputStream.close();               
        };
    }

    @RequestMapping(value = "/shado/getSummary", method = RequestMethod.GET)
    public StreamingResponseBody getSummaryFile(@RequestParam(value="sessionN", defaultValue="") String sessionN, HttpServletResponse response) throws IOException {

        String fileName = homeDirectory + sessionN + "/Summary.zip";

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"Summary.zip\"");
        InputStream inputStream = new FileInputStream(new File(fileName));
        return outputStream -> {
            int iRead;
            byte[] data = new byte[1024];
            while ((iRead = inputStream.read(data, 0, data.length)) != -1) {
                System.out.println("Writing some bytes for Summary..");
                outputStream.write(data, 0, iRead);
            }
            inputStream.close();   
        };
    }


    @RequestMapping(value = "/shado/getUtilizationJSON", method = RequestMethod.GET)
    public StreamingResponseBody getUtilization(@RequestParam(value="sessionN", defaultValue="") String sessionN, HttpServletResponse response) throws IOException{

        String fileName = homeDirectory + sessionN + "/Utilization.json";

        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=\"Utilization.json\"");
        InputStream inputStream = new FileInputStream(new File(fileName));
        return outputStream -> {
            int iRead;
            byte[] data = new byte[1024];
            while ((iRead = inputStream.read(data, 0, data.length)) != -1) {
                outputStream.write(data, 0, iRead);
            }
            inputStream.close();               
        };
    }

    @RequestMapping(value = "/shado/getTaskJSON", method = RequestMethod.GET)
    public StreamingResponseBody getTaskRecord(@RequestParam(value="sessionN", defaultValue="") String sessionN, HttpServletResponse response) throws IOException{

        String fileName = homeDirectory + sessionN + "/TaskRecord.json";

        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=\"TaskRecord.json\"");
        InputStream inputStream = new FileInputStream(new File(fileName));
        return outputStream -> {
            int iRead;
            byte[] data = new byte[1024];
            while ((iRead = inputStream.read(data, 0, data.length)) != -1) {
                //System.out.println("Writing some bytes for Task Record's report...");
                outputStream.write(data, 0, iRead);
            }
            inputStream.close();               
        };
    }

    @RequestMapping(value = "/shado/getWaitTimeJSON", method = RequestMethod.GET)
    public StreamingResponseBody getWaitTime(@RequestParam(value="sessionN", defaultValue="") String sessionN, HttpServletResponse response) throws IOException{

        String fileName = homeDirectory + sessionN + "/WaitTime.json";

        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=\"WaitTime.json\"");
        InputStream inputStream = new FileInputStream(new File(fileName));
        return outputStream -> {
            int iRead;
            byte[] data = new byte[1024];
            while ((iRead = inputStream.read(data, 0, data.length)) != -1) {
                //System.out.println("Writing some bytes for WaitTime's report...");
                outputStream.write(data, 0, iRead);
            }
            inputStream.close();               
        };
    }

    @RequestMapping(value = "/shado/validation", method = RequestMethod.GET)
    public StreamingResponseBody getValidation(@RequestParam(value="sessionN", defaultValue="") String sessionN, HttpServletResponse response) throws IOException{

        String fileName = homeDirectory + sessionN + "/validation.zip";

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"validation.zip\"");
        InputStream inputStream = new FileInputStream(new File(fileName));
        return outputStream -> {
            int iRead;
            byte[] data = new byte[1024];
            while ((iRead = inputStream.read(data, 0, data.length)) != -1) {
                System.out.println("Getting the validation results...");
                outputStream.write(data, 0, iRead);
            }
            inputStream.close();            
        };
    }

}