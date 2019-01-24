import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import org.junit.*;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

import server.Engine.*;
import server.Input.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Replication.class)
public class ReplicationTest
{
    loadparam vars;
    Operator op, op2, op3, op4;
    Task mockTask;
    Replication rep;
    private boolean called;
    RemoteOp mockROp;
    Queue q;

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    @Before
    public void setUp() throws Exception {
        vars = new loadparam();

        vars.repNumTasks = new int[] { 0 };
        vars.replicationTracker = 0;
        vars.numHours = 1;
        vars.numPhases = 3;
        vars.phaseBegin = new double[] { 0, 10, 50, 60 };

        vars.numTaskTypes = 4;
        vars.taskName_all = new String[] { "Task 1", "Task 2", "Misc", "Essential" };
        vars.essential = new int[] { 0, 0, 0, 1  };
        vars.interruptable = new int[] { 1, 1, 1, 0};
        vars.leadTask = new int[] { -1, -1, -1, 1};

        vars.serDists = new char[] { 'C', 'E', 'E', 'C' };
        vars.serPms = new double[][] { {5}, {60}, {60}, {3}};
        vars.expDists = new char[] { 'N', 'C', 'C', 'N' };
        vars.expPms = new double[][] { {5}, {5}, {60}, {10}};

        vars.arrDists = new char[] { 'C', 'C', 'C', 'C' };
        vars.arrPms = new double[][] { {5}, {5}, {60}, {12}};
        vars.autolvl = new char[] { 'N', 'N', 'N', 'N' };
        vars.hasExogenous = new int[] { 0, 0 };
        vars.affByTraff = new int[] {1, 1, 0, 0};
        vars.traffic = new double[][] {
            {1}, {1}, {1}, {1},
        };
        vars.exoType2Aff = new int[] { 0, 0, 1, 0 };

        // TurnOVer Task
        vars.hasTurnOver = new int[] { 0, 0 };

        vars.numTeams = 1;                      // number of teams
        vars.teamComm = new char[] { 'N' };     // team communication
        vars.numRemoteOp = 4;                   // sum of all the team size
        vars.flexTeamSize = 0;                  // flexible team size
        vars.teamSize = new int[] { 3, 1 };        // number of members of each team
        vars.AIDAtype = new int[][] { { 0, 0, 0} }; // AI types...
        vars.opNames = new String[] { "Team 1" };

        vars.allTasksPerRep = new ArrayList<>();

        vars.TC_SOME_TASK = vars.numTaskTypes;
        vars.TC_FULL_TASK = vars.numTaskTypes + 1;
        vars.EXOGENOUS_TASK = vars.numTaskTypes + 2;
        vars.TURN_OVER_BEGIN_TASK = vars.numTaskTypes + 3;
        vars.TURN_OVER_END_TASK = vars.numTaskTypes + 4;
        vars.FLEXTEAM = vars.numTeams;
        // vars.numTaskTypes += 5;

        mockTask = mock(Task.class);
        called = false;

        rep = new Replication(vars, 0);

        mockROp = mock(RemoteOp.class);
        q = mock(Queue.class);
        op = mock(Operator.class);
        op2 = mock(Operator.class);
        op3 = mock(Operator.class);
        op4 = mock(Operator.class);

        when(op.getQueue()).thenReturn(q);
        op4.dpID = 100; // team 1 and member id 0
        when(mockROp.getRemoteOp()).thenReturn(new Operator[] { op });

        // set remoteOps...
        Whitebox.setInternalState(rep, "remoteOps", mockROp); // set remote OPs.
    }

    /**
     * Tears down the test fixture.
     * (Called after every test case method.)
     */
    // @After
    // public void tearDown() {
    //     for(int i=0;i<vars.numTaskTypes;i++) {
    //         Task task = taskList.get(i);
    //         task = null;
    //     }
    //     vars = null;
    // }

    @Test
    public void sortTaskTest() throws Exception {
        // Given
        System.out.println("Given there are task1, task2, and task3, and each of these has arrival time 20, 10, and 15 respectively,");
        ArrayList<Task> tasks = new ArrayList<>();
        Task task1 = mock(Task.class);
        when(task1.getArrTime()).thenReturn(20.0);
        Task task2 = mock(Task.class);
        when(task2.getArrTime()).thenReturn(10.0);
        Task task3 = mock(Task.class);
        when(task3.getArrTime()).thenReturn(15.0);

        tasks.addAll(Arrays.asList(task1, task2, task3));

        Whitebox.setInternalState(rep, "globalTasks", tasks);

        // When
        System.out.println("When soring is done,");
        rep.sortTask();

        // Then
        System.out.println("Then tasks should be ascedning order of arrival time - task2, task3, task1.");
        ArrayList<Task> expectedTasks = new ArrayList<>();
        expectedTasks.addAll(Arrays.asList(task2, task3, task1));
        assertEquals(expectedTasks, rep.getTasks());
        System.out.println();
    }

    @Test
    public void workingUntilNewTaskArriveTestCallingDone() throws Exception {

        //Given
        when(q.size()).thenReturn(1).thenReturn(0);
        when(q.peek()).thenReturn(mockTask).thenReturn(null);
        System.out.println("Given there are two tasks which has finishing possible time at 10 and 55,");
        when(q.getFinTime()).thenReturn(10.0).thenReturn(55.0);
        System.out.println("Given new task arrives at 50,");

        when(mockTask.getArrTime()).thenReturn(50.0);

        //When
        System.out.println("When workingUntilNewTaskArrive is executed,");
        rep.workingUntilNewTaskArrive(mockROp, mockTask);

        //Then
        System.out.println("The task finishing at 10 should be done and thus done called once, also never called clearTask!");
        verify(q, times(1)).done(vars, op);
        verify(q, never()).clearTask(vars, op);
        System.out.println();
    }

    @Test
    public void workingUntilNewTaskArriveTestCallingClearTask() throws Exception {

        //Given
        when(q.size()).thenReturn(1);
        when(q.peek()).thenReturn(mockTask).thenReturn(null);

        System.out.println("Given there are begining/ending turn over tasks and current phase is 1,");
        when(op.checkPhase()).thenReturn(1);
        System.out.println("Given there is a task which has finishing possible time at 52,");
        when(q.getFinTime()).thenReturn(52.0);
        System.out.println("Given new task arrives at 51 which is over phase 2 start time,");
        when(mockTask.getArrTime()).thenReturn(51.0);

        //When
        rep.workingUntilNewTaskArrive(mockROp, mockTask);

        //Then
        System.out.println("The clearTask should be called once, and the task will be removed, thus never called done!");
        verify(q, times(1)).clearTask(vars, op);

        // clearTask will complete the task
        when(q.size()).thenReturn(0);
        verify(q, never()).done(vars, op);


        System.out.println();
    }

    @Test
    public void workingRemainedTasksTest() throws Exception {
        //Given
        System.out.println("Given there are begining/ending turn over tasks and current phase is 1,");
        when(op.checkPhase()).thenReturn(1);
        System.out.println("Given there is a task which has finishing possible time at 52,");
        when(q.getFinTime()).thenReturn(52.0);
        //System.out.println("Given new task arrives at 51 which is over phase 2 start time,");
        //when(mockTask.getArrTime()).thenReturn(51.0);

        doAnswer(invocation -> {
            called = true;
            return null;
        }).when(q).clearTask(vars, op);

        doAnswer(invocation -> {
            called = true;
            return null;
        }).when(q).done(vars, op);

        when(q.size()).then(invocation -> called ? 0 : 1);
        when(q.peek()).then(invocation -> called ? null: mockTask);

        doAnswer(invocation -> {
            called = true;
            return null;
        }).when(q).clearTask(vars, op);

        doAnswer(invocation -> {
            called = true;
            return null;
        }).when(q).done(vars, op);

        when(q.size()).then(invocation -> called ? 0 : 1);
        when(q.peek()).then(invocation -> called ? null: mockTask);

        //When
        rep.workingRemainedTasks(mockROp);

        //Then
        System.out.println("The clearTask should never be called and the done function be called once.");
        verify(q, times(1)).done(vars, op);
        verify(q, never()).clearTask(vars, op);

        System.out.println();
    }

    @Test
    public void workingRemainedTasksTestWithClearTask() throws Exception {
        //Given
        System.out.println("Given there are begining/ending turn over tasks and current phase is 1,");
        when(op.checkPhase()).thenReturn(1);
        System.out.println("Given there is a task which has finishing possible time at 64,");
        when(q.getFinTime()).thenReturn(64.0);
        //System.out.println("Given new task arrives at 51 which is over phase 2 start time,");
        //when(mockTask.getArrTime()).thenReturn(51.0);

        doAnswer(invocation -> {
            called = true;
            return null;
        }).when(q).clearTask(vars, op);

        doAnswer(invocation -> {
            called = true;
            return null;
        }).when(q).done(vars, op);

        when(q.size()).then(invocation -> called ? 0 : 1);
        when(q.peek()).then(invocation -> called ? null: mockTask);

        //When
        rep.workingRemainedTasks(mockROp);

        //Then
        System.out.println("The clearTask should be called once and never done be called.");
        verify(q, times(1)).clearTask(vars, op);

        //Clear task should not be called
        verify(q, never()).done(vars, op);


        System.out.println();
    }

    @Test
    public void putTaskTest() throws Exception {

        //Given
        when(mockTask.getType()).thenReturn(vars.TURN_OVER_BEGIN_TASK);

         //When
        rep.puttask(mockTask);

        //Then
        System.out.println("The add method of the operator's queue should have been called to add the task.");

        verify(mockROp, times(1)).getRemoteOp();
        verify(op, times(1)).getQueue();
        verify(q, times(1)).add(mockTask, false);

        System.out.println();
    }

    @Test
    public void findAvailableOperatorTest() throws Exception {

        //Given
        vars.fleetTypes = 1;
        vars.numvehicles = new int[] { 1 };
        vars.fleetHetero = new int[][] { { 0, 1, 2 } };
        vars.opExpertise = new int[][][] { { { 1 }, { 1 }, { 1 } }, { { 1 }, { 1 }, { 1 } } }; // team x task x fleet

        when(mockROp.getRemoteOp()).thenReturn(new Operator[] { op,op2,op3,op4 });
        System.out.println("Given there are three operators and one operator in teams,");

        //When
        ArrayList<Operator> availableWorkers = new ArrayList<>();
        ArrayList<Operator> flexPosition = new ArrayList<>(vars.flexTeamSize);
        rep.findAvaliableOperator(availableWorkers, mockTask, flexPosition);

        //Then
        System.out.println("Three operators should be available.");
        assertEquals(4, availableWorkers.size());

        System.out.println();
    }

    @Test
    public void findAvailableOperatorTestWithTC_FULL_TASK() throws Exception {

        //Given
        when(mockTask.getType()).thenReturn(vars.TC_FULL_TASK);

        vars.AIDAtype = new int[][] { { 0, 0, 1} };
        vars.TCALevel = new char[] { 'F', 'F', 'F' };
        System.out.println("Given a communication task with team communication AI with full capacity,");
        when(mockROp.getRemoteOp()).thenReturn(new Operator[] { op,op2,op3,op4 });

        //When
        ArrayList<Operator> availableWorkers = new ArrayList<>();
        ArrayList<Operator> flexPosition = new ArrayList<>(vars.flexTeamSize);
        rep.findAvaliableOperator(availableWorkers, mockTask, flexPosition);

        //Then
        System.out.println("Then only one operator is available.");
        assertEquals(3, availableWorkers.size());

        // check TCA Level...with Full level 0.3
        System.out.println("Also, team communication AI will reduce the service time by 0.3.");
        verify(mockTask, times(1)).changeServTime(0.3);

        System.out.println();
    }

    @Test
    public void findAvailableOperatorTestWithExo_TASK() throws Exception {

        //Given
        when(mockTask.getType()).thenReturn(vars.EXOGENOUS_TASK);
        when(mockROp.getRemoteOp()).thenReturn(new Operator[] { op,op2,op3,op4 });

        //When
        ArrayList<Operator> availableWorkers = new ArrayList<>();
        ArrayList<Operator> flexPosition = new ArrayList<>(vars.flexTeamSize);
        rep.findAvaliableOperator(availableWorkers, mockTask, flexPosition);

        //Then
        System.out.println("Then tasks with all the operators are created.");
        assertEquals(4, availableWorkers.size());

        System.out.println();
    }


    @Test
    public void applyIndividufindAvailableOperatorTestWithExo_TASK() throws Exception {

        //Given
        when(mockTask.getType()).thenReturn(vars.EXOGENOUS_TASK);
        when(mockROp.getRemoteOp()).thenReturn(new Operator[] { op,op2,op3,op4 });

        //When
        ArrayList<Operator> availableWorkers = new ArrayList<>();
        ArrayList<Operator> flexPosition = new ArrayList<>(vars.flexTeamSize);
        rep.findAvaliableOperator(availableWorkers, mockTask, flexPosition);

        //Then
        System.out.println("Then tasks with all the operators are created.");
        assertEquals(4, availableWorkers.size());

        System.out.println();
    }

    // this is integration test...
    // @Test
    // public void initTestWithOneOpAndOneFleetAndTwoTask() throws Exception {

    //     vars.fleetTypes = 1;
    //     vars.numvehicles = new int[] { 1 };
    //     vars.fleetHetero = new int[][] { { 0, 1, 2 } };

    //     rep = new Replication(vars, 0);
    //     System.out.println("Task 1,2,3 for fleet 1 should be created and number of tasks should be 25.");
    //     assertEquals(25, rep.getTasks().size());
    // }

}
