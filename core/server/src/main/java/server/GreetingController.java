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

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();
    private Shado shado;
//    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmssZ");
    Date date = new Date();

//    private String directory = "/home/rapiduser/out/";
    private String homeDirectory = "/Users/zhanglian1/Desktop/out/";
    private String directory = homeDirectory;

    @RequestMapping("/shado/hello")
    public Greeting greeting(@RequestParam(value="name", defaultValue="This is Shado") String name) {
        return new Greeting(counter.incrementAndGet(),
                String.format(template, name));
    }

    @RequestMapping(value= "/shado/testpost",method = RequestMethod.POST)
    public String index(@RequestBody String payload) throws Exception{
        //TODO: Sanity Check and pass to Shado Object
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
                System.out.println("Writing some bytes for Utiization...");
                outputStream.write(data, 0, iRead);
            }
        };
    }

    @RequestMapping(value = "/shado/getFailedJSON", method = RequestMethod.GET)
    public StreamingResponseBody getFailed(@RequestParam(value="sessionN", defaultValue="") String sessionN, HttpServletResponse response) throws IOException{

        String fileName = homeDirectory + sessionN + "/FailedTask.json";

        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=\"FailedTask.json\"");
        InputStream inputStream = new FileInputStream(new File(fileName));
        return outputStream -> {
            int iRead;
            byte[] data = new byte[1024];
            while ((iRead = inputStream.read(data, 0, data.length)) != -1) {
                System.out.println("Writing some bytes for Failed Task's report...");
                outputStream.write(data, 0, iRead);
            }
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
        };
    }

}