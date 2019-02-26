import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.junit.*;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;

import server.Engine.*;
import server.Input.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Task.class)
public class TaskTest
{
    loadparam data;
    ArrayList<Task> taskList = new ArrayList<Task>();
    Task task, task2, task3, task4, task5, task6;

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    @Before
    public void setUp() throws Exception {
        data = new loadparam();

        data.numHours = 8;
        data.numPhases = 3;
        data.phaseBegin = new double[] { 0, 7.5, 8 };

        data.numTaskTypes = 7;
        data.taskName_all = new String[] { "Train Movement", "Reporting", "Miscellaneous" };
        data.essential = new int[] { 1, 0, 0 };

        data.serDists = new char[] { 'C', 'E', 'E' };
        data.serPms = new double[][] { {5}, {60}, {60}};
        data.expDists = new char[] { 'N', 'C', 'C' };
        data.expPms = new double[][] { {5}, {5}, {60}};

        data.arrDists = new char[] { 'C', 'U', 'C' };
        data.arrPms = new double[][] { {5}, {5,10}, {60}};
        data.autolvl = new char[] { 'N', 'N', 'N' };
        data.hasExogenous = new int[] { 0, 0 };
        data.affByTraff = new int[] {1, 1, 0};
        data.traffic = new double[][] {
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1},
        };
        data.exoType2Aff = new int[] { 0, 0, 1 };

        // Task(int type, double PrevTime, loadparam Param, boolean fromPrev, int vehicle)
        task = new Task(0, 0, data, true, 0 );
        task2 = new Task(1, 0, data, true, 0 );

        task3 = new Task(2, 30, data, true, 0 );    // prev time 30
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
    public void checkSerTime() {
        System.out.println("Check Constant Service Time: " + task.getName());
        System.out.println("Constance Service Time " + task.getSerTime() + " should be 5.");
        assertEquals(5, task.getSerTime(), 0.001);
        System.out.println();

        // how to check 'E'?
        // System.out.println("Check Task Service Time with E: " + task2.getName() + " " + task2.getSerTime());
        // assertEquals(60, task2.getSerTime(), 60);

        System.out.println("Check changeSerTime: " + task.getName());
        System.out.println("ChangeSerTime should change the serviceTime times factor 2 " + task.getSerTime());
        task.changeServTime(2);
        assertEquals(10, task.getSerTime(), 0.001);
        System.out.println();

    }

    @Test
    public void checkExpTime() {
        System.out.println("Check Task Expiration Time: " + task.getName());
        System.out.println("Expiration Time " + task.getExpTime() + " should be INFINITY.");
        assertEquals(Double.POSITIVE_INFINITY, task.getExpTime(), 0.001);
        System.out.println();

        System.out.println("Check Task Expiration Time: " + task2.getName());
        System.out.println("Expiration Time " + task2.getExpTime() + " should be 10~15 which is arrTime(5,10) + expTime(5).");
        assertEquals(12.5, task2.getExpTime(), 2.5);
        System.out.println();
    }

    @Test
    public void checkArrTime() throws Exception {
        //double arrTime = Whitebox.invokeMethod(task, "genArrTime", 0, 0);

        System.out.println("Check Task Constant Arrival Time(5): " + task.getName());
        System.out.println("Arrival Time " + task.getArrTime() + " should be 5.");
        assertEquals(5, task.getArrTime(), 0.001);
        System.out.println();

        System.out.println("Check Task Uniform Arrival Time(5, 10): " + task2.getName());
        System.out.println("Arrival Time " + task.getArrTime() + " should be between 5 and 10.");
        assertEquals(7.5, task2.getArrTime(), 2.5);
        System.out.println();

        // Poor weather affects arrival time
        // poor weather
        data.hasExogenous[1] = 1;
        task4 = new Task(2, 30, data, true, 0 );    // same as task3 except poor weather
        data.hasExogenous[1] = 0;

        data.autolvl[0] = 'F';
        task5 = new Task(0, 0, data, true, 0);      // team communication 'Full'

        data.autolvl[0] = 'S';
        task6 = new Task(0, 0, data, true, 0);      // team communication 'Some'

        System.out.println("Check Task Arrival Time with poor weather: ");
        System.out.println("Arrival Time with poor weather " + task4.getArrTime() + " should be smaller(10% slower - prevTime) than " + task3.getArrTime());
        double arrTime = task3.getArrTime();
        arrTime = 30.0 + (arrTime - 30.0) / 1.1;
        //assertThat("Arrival Time with poor weather should be smaller.", task4.getArrTime(), org.hamcrest.Matchers.lessThan(task3.getArrTime()));
        assertEquals(arrTime, task4.getArrTime(), 0.001);
        System.out.println();

        // Team coordination affects arrival time
        System.out.println("Check Task Arrival Time with vehicle communication 'Some': ");
        System.out.println("Arrival Time " + task5.getArrTime() + " should be actual / 0.3: " + task.getArrTime() / 0.3);
        assertEquals(task5.getArrTime(), task.getArrTime() / 0.3, 0.001);
        System.out.println();

        System.out.println("Check Task Arrival Time with vehicle communication 'Full': ");
        System.out.println("Arrival Time " + task6.getArrTime() + " should be actual / 0.7: " + task.getArrTime() / 0.7);
        assertEquals(task6.getArrTime(), task.getArrTime() / 0.7, 0.001);
        System.out.println();
    }

    @Test
    public void checkArrTimeTraffic() throws Exception {
        double arrTime;

        // Traffic affects arrival time
        // normal traffic
        arrTime = Whitebox.invokeMethod(task, "applyTraffic", 1.0);
        System.out.println("Check Task Normal Traffic: " + task.getName()); // normal traffic
        System.out.println("Arrival Time " + arrTime + " should be 1.");
        assertEquals(1, arrTime, 0.001);
        System.out.println("");

        // high traffic
        for(int i=0;i<task.vars.numHours;i++) {
            task.vars.traffic[0][i] = 2.0;
        }
        arrTime = Whitebox.invokeMethod(task, "applyTraffic", 1.0);
        System.out.println("Check Task High Traffic(1): " + task.getName()); // high traffic => reduce arrival => more tasks
        System.out.println("Arrival Time " + arrTime + " should be 0.5.");
        assertEquals(0.5, arrTime, 0.001);
        System.out.println("");

        // low traffic
        for(int i=0;i<task.vars.numHours;i++) {
            task.vars.traffic[0][i] = 0.5;
        }
        arrTime = Whitebox.invokeMethod(task, "applyTraffic", 1.0);
        System.out.println("Check Task Normal Traffic: " + task.getName()); // low traffic => increase arrival => less tasks
        System.out.println("Arrival Time " + arrTime + " should be 2.0.");
        assertEquals(2.0, arrTime, 0.001);
        System.out.println("");

        // normal/high repeating traffic
        for(int i=0;i<task.vars.numHours;i++) {
            task.vars.traffic[0][i] = (i%2==0) ? 1 : 2;
        }

        arrTime = Whitebox.invokeMethod(task, "applyTraffic", 120.0);
        System.out.println("Check Task Normal/High Traffic(120): " + task.getName()); // high traffic => reduce arrival => more tasks
        System.out.println("Arrival Time " + arrTime + " should be 90 : 60 normal, 30 high.");
        assertEquals(90, arrTime, 0.001);
        System.out.println("");

        // normal/high repeating traffic with prevTime 30
        for(int i=0;i<task3.vars.numHours;i++) {
            task3.vars.traffic[0][i] = (i%2==0) ? 1 : 2;
        }

        arrTime = Whitebox.invokeMethod(task3, "applyTraffic", 120.0);
        System.out.println("Check Task Normal/High Traffic(120) with 30 arrival time: " + task3.getName()); // high traffic => reduce arrival => more tasks
        System.out.println("Arrival Time " + arrTime + " should be 105 : 30 normal, 90 high.");
        assertEquals(105, arrTime, 0.001);
        System.out.println("");
    }

    @Test
    public void checkWorkScheduleSimple() {
        // arrTime 5, serTime 5, beginTime 7, endTime 12
        task.addBeginTime(7);
        task.setDone(12);

        System.out.println("Check task with arrival time 5, service time 5, begin time 7, end time 12: ");
        System.out.println("Then wait time should be 2 and elapsed time 5.");
        assertEquals(2, task.getWaitTime(), 0.001);
        assertEquals(5, task.getELSTime(), 0.001);

        System.out.println("Then work schedule size is 1.");
        assertEquals(1, task.workSchedule.size());

        System.out.println("");
    }

    @Test
    public void checkWorkScheduleWithInterruption() {
        // arrTime 5, serTime 5, beginTime 10, interrupt time 12, beginTime 15, endTime 18
        // workschedule should be 10~12 and 15~18
        task.addBeginTime(10);
        task.addInterruptTime(12);

        task.addBeginTime(15);
        task.setDone(18);

        System.out.println("Check task with arrival time 5, service time 5, begin time 10, interrupt time 12, begin time 15, end time 18: ");
        System.out.println("Then wait time should be 8 and elapsed time 5.");
        assertEquals(8, task.getWaitTime(), 0.001);
        assertEquals(5, task.getELSTime(), 0.001);

        System.out.println("Then work schedule size is 2.");
        assertEquals(2, task.workSchedule.size());
        System.out.println("Then work schedule should be 10~12 and 15~18 :");
        String schedule = "";
        for(int i = 0; i < task.workSchedule.size(); i++){
			schedule += task.workSchedule.get(i)[0] + "~" + task.workSchedule.get(i)[1] + " ";
		}
        assertEquals("10.0~12.0 15.0~18.0 ", schedule);

        System.out.println("");
    }
}