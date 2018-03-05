package server;

/**
 * Created by siyuchen on 3/1/18.
 */

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import server.Engine.Shado;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();
    private Shado shado = new Shado();
    @RequestMapping("/shado/hello")
    public Greeting greeting(@RequestParam(value="name", defaultValue="This is Shado") String name) {
        return new Greeting(counter.incrementAndGet(),
                String.format(template, name));
    }

    @RequestMapping(value= "/shado/testpost",method = RequestMethod.POST)
    public String index(@RequestBody String payload) throws Exception{
        //TODO: Sanity Check and pass to Shado Object

        shado.runShado(payload);
//        System.out.println(payload);
        return "Post Success!";
    }
}