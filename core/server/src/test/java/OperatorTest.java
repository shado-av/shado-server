import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import org.junit.*;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.PriorityQueue;

import server.Engine.*;
import server.Input.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Operator.class)
public class OperatorTest
{
    loadparam data;
    Operator op;
    Task mockTask;

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    @Before
    public void setUp() throws Exception {
        data = new loadparam();

        data.repNumTasks = new int[] { 0 };
        data.replicationTracker = 0;
        data.numHours = 1;
        data.numPhases = 3;
        data.phaseBegin = new double[] { 0, 10, 50, 60 };

        data.numTaskTypes = 4;
        data.taskName_all = new String[] { "Task 1", "Task 2", "Misc", "Essential" };
        data.essential = new int[] { 0, 0, 0, 1  };
        data.interruptable = new int[] { 1, 1, 1, 0};
        data.leadTask = new int[] { -1, -1, -1, 1};

        data.serDists = new char[] { 'C', 'E', 'E', 'C' };
        data.serPms = new double[][] { {5}, {60}, {60}, {3}};
        data.expDists = new char[] { 'N', 'C', 'C', 'N' };
        data.expPms = new double[][] { {5}, {5}, {60}, {10}};

        data.arrDists = new char[] { 'C', 'C', 'C', 'C' };
        data.arrPms = new double[][] { {5}, {5}, {60}, {12}};
        data.autolvl = new char[] { 'N', 'N', 'N', 'N' };
        data.hasExogenous = new int[] { 0, 0 };
        data.affByTraff = new int[] {1, 1, 0, 0};
        data.traffic = new double[][] {
            {1}, {1}, {1}, {1},
        };
        data.exoType2Aff = new int[] { 0, 0, 1, 0 };

        mockTask = mock(Task.class);
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
    public void checkPhaseTest() throws Exception {

        // Phase 0 : 0<= t <=10
        // Phase 1 : 10< t <=50
        // Phase 2 : 50< t <=60
        op = new Operator(0, "Tester", data);
        System.out.println("Given a phase begin array with [0, 10, 50, 60],");

        op.getQueue().setTime(5);
        System.out.println("Phase at 5 should be 0");
        assertEquals(0, op.checkPhase());
        System.out.println();

        op.getQueue().setTime(10);
        System.out.println("Phase at 10 should be 1");
        assertEquals(0, op.checkPhase());
        System.out.println();

        op.getQueue().setTime(10.01);
        System.out.println("Phase at 10.01 should be 1");
        assertEquals(1, op.checkPhase());
        System.out.println();

        op.getQueue().setTime(51);
        System.out.println("Phase at 51 should be 2");
        assertEquals(2, op.checkPhase());
        System.out.println();
    }

    @Test
    public void getBusyIn10minTestWithOnlyCurrentTask() throws Exception {

        op = new Operator(0, "Tester", data);

        // mockTask workSchedule : 0~5, 7~12, 16~20
        mockTask.workSchedule = new ArrayList<>();
        mockTask.workSchedule.add(new double[] { 0, 5});
        mockTask.workSchedule.add(new double[] { 7, 0});    // working

        op.getQueue().taskqueue.add(mockTask);

        System.out.println("Given a current task with work schedule 0~5, 7~0,");
        System.out.println("Busy in 10 mins at time 10 should be 8 mins.");
        assertEquals(8, op.getBusyIn10min(10), 0.001);
        System.out.println();

        mockTask.workSchedule.get(1)[1] = 12;   // 7~12
        mockTask.workSchedule.add(new double[] { 16, 20});

        System.out.println("Given a current task with work schedule 0~5, 7~12, 16~20,");
        System.out.println("Busy in 10 mins at time 21 should be 5 mins.");
        assertEquals(5, op.getBusyIn10min(21), 0.001);
        System.out.println();
    }

    @Test
    public void getBusyIn10minTestWithCurrentTaskAndOneRecord() throws Exception {
        Queue q = mock(Queue.class);
        PowerMockito.whenNew(Queue.class).withAnyArguments().thenReturn(q);

        ArrayList<Task> records = new ArrayList<Task>();
        when(q.records()).thenReturn(records);
        when(q.peek()).thenReturn(mockTask);

        op = new Operator(0, "Tester", data);

        // mockTask workSchedule : 0~5, 7~12, 16~20
        mockTask.workSchedule = new ArrayList<>();
        mockTask.workSchedule.add(new double[] { 0, 5});
        mockTask.workSchedule.add(new double[] { 7, 0});    // working

        Task task1 = mock(Task.class);
        task1.workSchedule = new ArrayList<>();
        task1.workSchedule.add(new double[] { 6, 7});
        records.add(task1);

        System.out.println("Given a current task with work schedule 0~5, 7~0");
        System.out.println("Given two completed records 6~7,");
        System.out.println("Busy in 10 mins at time 10 should be 9 mins.");
        assertEquals(9, op.getBusyIn10min(10), 0.001);
        System.out.println();
    }

    @Test
    public void getBusyIn10minTestWithCurrentTaskAndTwoRecords() throws Exception {
        Queue q = mock(Queue.class);
        PowerMockito.whenNew(Queue.class).withAnyArguments().thenReturn(q);

        ArrayList<Task> records = new ArrayList<Task>();
        when(q.records()).thenReturn(records);
        when(q.peek()).thenReturn(mockTask);

        op = new Operator(0, "Tester", data);

        // mockTask workSchedule : 0~5, 7~12, 16~20
        mockTask.workSchedule = new ArrayList<>();
        mockTask.workSchedule.add(new double[] { 0, 5});
        mockTask.workSchedule.add(new double[] { 7, 12});
        mockTask.workSchedule.add(new double[] { 16, 20});

        Task task1 = mock(Task.class);  // finished 12~14
        task1.workSchedule = new ArrayList<>();
        task1.workSchedule.add(new double[] { 12, 14});
        when(task1.getEndTime()).thenReturn(14.0);
        records.add(task1);

        Task task2 = mock(Task.class);  // finished 20~21
        task2.workSchedule = new ArrayList<>();
        task2.workSchedule.add(new double[] { 20, 21});
        when(task2.getEndTime()).thenReturn(21.0);
        records.add(task2);

        System.out.println("Given a current task with work schedule 0~5, 7~12, 16~20");
        System.out.println("Given two completed records 12~14 and 20~21,");
        System.out.println("Busy in 10 mins at time 21 should be 8 mins.");
        assertEquals(8, op.getBusyIn10min(21), 0.001);
        System.out.println();
    }
}
