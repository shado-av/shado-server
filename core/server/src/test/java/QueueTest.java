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

@RunWith(PowerMockRunner.class)
@PrepareForTest(Queue.class)
public class QueueTest
{
    loadparam data;
    ArrayList<Task> taskList = new ArrayList<Task>();
    Task task, task2, task3, task4, task5, task6, taskEssential;
    Queue q;
    Operator mockOp;

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    @Before
    public void setUp() throws Exception {
        data = new loadparam();

        data.replicationTracker = 0;
        data.numHours = 1;
        data.numPhases = 3;
        data.phaseBegin = new double[] { 0, 59, 60 };

        data.numTaskTypes = 4;
        data.taskName_all = new String[] { "Task 1", "Task 2", "Misc", "Essential" };
        data.essential = new int[] { 0, 0, 0, 1  };
        data.interruptable = new int[] { 1, 1, 1, 0};
        data.leadTask = new int[] { -1, -1, -1, -1};

        data.serDists = new char[] { 'C', 'E', 'E', 'C' };
        data.serPms = new double[][] { {5}, {60}, {60}, {3}};
        data.expDists = new char[] { 'N', 'C', 'C', 'N' };
        data.expPms = new double[][] { {5}, {5}, {60}, {10}};

        data.arrDists = new char[] { 'C', 'U', 'C', 'C' };
        data.arrPms = new double[][] { {5}, {5,10}, {60}, {12}};
        data.autolvl = new char[] { 'N', 'N', 'N', 'N' };
        data.hasExogenous = new int[] { 0, 0 };
        data.affByTraff = new int[] {1, 1, 0, 0};
        data.traffic = new double[][] {
            {1}, {1}, {1}, {1},
        };
        data.exoType2Aff = new int[] { 0, 0, 1, 0 };

        data.taskRecord = mock(TaskRecord.class);
        // stubbing
        when(data.taskRecord.getNumSuccessTask()).thenReturn(new int[1][data.numPhases][1][data.numTaskTypes]);
        when(data.taskRecord.getNumFailedTask()).thenReturn(new int[1][data.numPhases][1][data.numTaskTypes][4]);

        // Task(int type, double PrevTime, loadparam Param, boolean fromPrev, int vehicle)
        task = new Task(0, 0, data, true, 0 );
        task2 = new Task(1, 0, data, true, 0 );

        task3 = new Task(2, 30, data, true, 0 );    // prev time 30
        taskEssential = new Task(3, 0, data, true, 0);

        mockOp = mock(Operator.class);
        mockOp.dpID = 0;

        q = new Queue(mockOp);
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
    public void addAndDoneTest() {
        // arrTime 5, serTime 5, beginTime 10, interrupt time 12
        // workschedule should be 10~12 and 15~18
        q.setTime(10);
        q.add(task, false);     // addBeginTime(10)

        System.out.println("Check adding a task to the empty queue: ");
        System.out.println("Task BeginTime should be same as max(setTime:10, arrivalTime:5) " + task.getBeginTime() + " should be 10.");
        assertEquals(10, task.getBeginTime(), 0.001);
        System.out.println("Task expected finish time (beginTime:10 + serviceTime:5 - elapsed time:0) " + q.getFinTime() + " should be 15.");
        assertEquals(15, q.getFinTime(), 0.001);
        System.out.println();

        // newTask should be essential and arrTime 12, serTime 3
        q.add(taskEssential, false);

        System.out.println("Check adding an essential task to the queue with a normal task: ");
        System.out.println("Task BeginTime should be same as arrivalTime:12 " + taskEssential.getBeginTime() + " should be 12.");
        assertEquals(12, taskEssential.getBeginTime(), 0.001);
        System.out.println("Task expected finish time (beginTime:12 + serviceTime:3 - elapsed time:0) " + q.getFinTime() + " should be 15.");
        assertEquals(15, q.getFinTime(), 0.001);
        System.out.println("Interrupted task elasped time (interruptedTime:12 - beginTime: 10) " + task.getELSTime() + " should be 2.");
        assertEquals(2, task.getELSTime(), 0.001);
        System.out.println();

        // set essential task done, and the other task active at 15
        q.done(data, mockOp);

        System.out.println("Check finishing a task: ");
        System.out.println("Finished task wait time endTime:15 - beginTime:12 - elaspedTime:3 " + taskEssential.getWaitTime() + " should be 0.");
        assertEquals(0, taskEssential.getWaitTime(), 0.001);
        System.out.println("Task new BeginTime should be same as finishing time:15) " + task.getBeginTime() + " should be 15.");
        System.out.println("Get Time: " + q.getTime() + " FinTime: " + q.getFinTime());
        assertEquals(15, task.getBeginTime(), 0.001);
        System.out.println("Task expected finish time (beginTime:15 + serviceTime:5 - elapsed time:2) " + q.getFinTime() + " should be 18.");
        assertEquals(18, q.getFinTime(), 0.001);
        System.out.println();

        q.done(data, mockOp);

        System.out.println("Check finishing a task: ");
        System.out.println("Finished task wait time endTime:18 - arrTime:5 - elaspedTime:5 " + task.getWaitTime() + " should be 8.");
        assertEquals(8, task.getWaitTime(), 0.001);
        System.out.println();
    }

    @Test
    public void addTaskLongEnoughNotToBeCompletedAndDoneTest() {
        q.setTime(57);
        q.add(task, false);     // addBeginTime(10)

        System.out.println("Check adding a task at the nearly end of the time which cannot be completed in time to the empty queue: ");
        System.out.println("Task BeginTime should be same as max(setTime:57, arrivalTime:5) " + task.getBeginTime() + " should be 57.");
        assertEquals(57, task.getBeginTime(), 0.001);
        System.out.println("Task expected finish time (beginTime:57 + serviceTime:5 - elapsed time:0) " + q.getFinTime() + " should be 62.");
        assertEquals(62, q.getFinTime(), 0.001);

        q.done(data, mockOp);
        System.out.println("Task elapsed time (finishTime:60 - beginTime:57) " + task.getELSTime() + " should be 3.");
        assertEquals(3, task.getELSTime(), 0.001);
        System.out.println();
    }

    @Test
    public void clearTaskTest() {
        System.out.println("Check adding a normal task and an essential task at the nearly end of the time and call clearTask: ");
        q.setTime(54);
        q.add(task, false);     // addBeginTime(54),finTime(59)

        System.out.println("Normal Task beginTime should be same as max(setTime:54, arrivalTime:5) " + task.getBeginTime() + " should be 54.");
        assertEquals(54, task.getBeginTime(), 0.001);

        q.setTime(58);
        q.add(taskEssential, false);   // addBeginTime(58),finTime(61)

        System.out.println("Essential Task beginTime should be same as max(setTime:58, arrivalTime:12) " + taskEssential.getBeginTime() + " should be 58.");
        assertEquals(58, taskEssential.getBeginTime(), 0.001);

        q.clearTask(data, mockOp);

        System.out.println("Normal Task elapsed time (finishTime:58 - beginTime:54) " + task.getELSTime() + " should be 4 and incomplete.");
        assertEquals(4, task.getELSTime(), 0.001);

        System.out.println("Essential Task elapsed time (finishTime:60 - beginTime:58) " + taskEssential.getELSTime() + " should be 2 and incomplete.");
        assertEquals(2, taskEssential.getELSTime(), 0.001);
        System.out.println();
    }

    @Test
    public void clearTaskWithEssentialTasksTest() {
        System.out.println("Check adding two essential tasks at the nearly end of the time and call clearTask: ");
        q.setTime(56);
        q.add(taskEssential, false);     // addBeginTime(54),finTime(59)

        //TODO: possible bug without setting setArrTime..., maybe new Task(task, arrTime)?
        Task newEssential = new Task(taskEssential);
        newEssential.setArrTime(58);

        q.add(newEssential, false);
        q.clearTask(data, mockOp);

        System.out.println("Essential Task 1 beginTime should be same as max(setTime:56, arrivalTime:5) " + taskEssential.getBeginTime() + " should be 56.");
        assertEquals(56, taskEssential.getBeginTime(), 0.001);

        System.out.println("Essential Task 1 endTime should be same as (begintTime:56 + serveTime: 3) " + taskEssential.getEndTime() + " should be 59.");
        assertEquals(59, taskEssential.getEndTime(), 0.001);

        System.out.println("Essential Task 2 beginTime should be same as max(setTime:58 + currentTime: 59) " + newEssential.getBeginTime() + " should be 59.");
        assertEquals(59, newEssential.getBeginTime(), 0.001);

        System.out.println("Essential Task 2 endTime should be same as min(begintTime:59 + serveTime: 3, 60) " + newEssential.getEndTime() + " should be 60.");
        assertEquals(60, newEssential.getEndTime(), 0.001);

        System.out.println();
    }
}