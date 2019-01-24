import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import org.junit.*;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;

import server.Engine.*;
import server.Input.*;
import server.Output.TaskRecord;

public class DataTest
{
    Data data;
    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * Tears down the test fixture.
     * (Called after every test case method.)
     */
    // @After
    // public void tearDown() {
    //     for(int i=0;i<data.numTaskTypes;i++) {
    //         Task task = taskList.get(i);
    //         task = null;
    //     }
    //     data = null;
    // }

    @Test
    public void oneValueDataTest() {
        data = new Data(1,1,1);
        data.dataInc(0,0,0,10);

        data.avgData();

        System.out.println("Given data 10, its value and average should be 10.");
        assertEquals(10, data.dataGet(0,0,0), 0.001);
        assertEquals(10, data.avg[0][0], 0.001);

        // for std of 1 value not defined...
        //assertEquals(NaN, data.std[0][0], 0.001);
    }

    @Test
    public void fiveValueDataTest() {
        data = new Data(1,1,5);
        data.dataInc(0,0,0,10);
        data.dataInc(0,0,1,20);
        data.dataInc(0,0,2,30);
        data.dataInc(0,0,3,40);
        data.dataInc(0,0,4,50);

        data.avgData();

        System.out.println("Given data 10,20,30,40,50, average should be 30 and std.dev. be 15.8113883");
        assertEquals(30, data.avg[0][0], 0.001);
        assertEquals(15.8113883, data.std[0][0], 0.001);
    }
}
