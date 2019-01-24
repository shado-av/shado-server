import static org.junit.Assert.*;
import org.junit.*;
import org.junit.runner.RunWith;

import server.Engine.*;
import server.Util.Util;

public class UtilTest
{
    @Test
    public void roundUpTest() {
        System.out.println("Util.round(10.005, 2) should be 10.01.");
        assertEquals(10.01, Util.round(10.0050, 2), 0.001);
    }

    @Test
    public void roundDownTest() {
        System.out.println("Util.round(10.0049, 2) should be 10.00.");
        assertEquals(10.00, Util.round(10.0049, 2), 0.001);
    }
}
