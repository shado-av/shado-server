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

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();
    private Shado shado;
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    Date date = new Date();

    private String directory = "/home/rapiduser/out/";

//    private String directory = "/Users/zhanglian1/Desktop/out/";

    @RequestMapping("/shado/hello")
    public Greeting greeting(@RequestParam(value="name", defaultValue="This is Shado") String name) {
        return new Greeting(counter.incrementAndGet(),
                String.format(template, name));
    }

    @CrossOrigin

    @RequestMapping(value= "/shado/testpost",method = RequestMethod.POST)
    public String index(@RequestBody String payload) throws Exception{
        //TODO: Sanity Check and pass to Shado Object
        String sessionNum = dateFormat.format(date)+"_"+counter.incrementAndGet();
        shado = new Shado(sessionNum, directory);
        shado.runShado(payload);
//        System.out.println(payload);
        return "Shado Successfully Run! SESSION #: "+ sessionNum;
    }

    //Output file download (For web-simple)
    @RequestMapping(value = "/shado/getRepDetail", method = RequestMethod.GET)
    public StreamingResponseBody getRepFile(HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"repCSV.zip\"");
        InputStream inputStream = new FileInputStream(new File(directory + "repCSV.zip"));

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
    public StreamingResponseBody getSummaryFile(HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"Summary.zip\"");
        InputStream inputStream = new FileInputStream(new File(directory + "Summary.zip"));
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
    public StreamingResponseBody getUtilization(HttpServletResponse response) throws IOException{
        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=\"Utilization.json\"");
        InputStream inputStream = new FileInputStream(new File(directory + "Utilization.json"));
        return outputStream -> {
            int iRead;
            byte[] data = new byte[1024];
            while ((iRead = inputStream.read(data, 0, data.length)) != -1) {
                System.out.println("Writing some bytes for Utiization...");
                outputStream.write(data, 0, iRead);
            }
        };
    }
}