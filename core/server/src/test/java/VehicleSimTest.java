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

@RunWith(PowerMockRunner.class)
@PrepareForTest(VehicleSim.class)
public class VehicleSimTest
{
    loadparam data;
    ArrayList<Task> list = new ArrayList<Task> ();
    Operator mockOp;
    VehicleSim fleet;
    Operator opList[] = new Operator[1];

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
        data.phaseBegin = new double[] { 0, 59, 60 };

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

        mockOp = mock(Operator.class);
        mockOp.dpID = 0;
        opList[0] = mockOp;

        fleet = new VehicleSim(data, 0, opList, list);
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
    public void taskGenTest() throws Exception {

        data.fleetHetero = new int[][] { { 0, 1, 2 } };
        fleet.taskgen();

        // how do we verify task generated...?
        // task 1 is generated every 5 minutes    number 12
        // task 2 is generated every 5 minutes number 12
        // task 3 is generated once at 60 minutes number 1
        // sums 25

        System.out.println("Total number of tasks in array should be the same value of vars.repNumTasks[rep]");
        assertEquals(fleet.globalTasks.size() , data.repNumTasks[data.replicationTracker]);
        System.out.println();

        System.out.println("Total number of tasks " + fleet.globalTasks.size() + " should be 25.");
        assertEquals(25 , fleet.globalTasks.size());
        System.out.println();
    }

    @Test
    public void taskGenTestWithFollowingTasks() throws Exception {
        // add task 4
        data.fleetHetero = new int[][] { { 0, 1, 2,3 } };
        // change task 2 arrival time to 10
        data.arrPms[1][0] = 10;
        
        fleet.taskgen();

        // how do we verify task generated...?
        // task 1 is generated every 5 minutes    number 12
        // task 2 is generated every 10 minutes number 6
        // task 3 is generated once at 60 minutes number 1
        // following task 4 is generated 12 minutes after each task 2 : number 4 (except 50, 60)
        // sums 23

        System.out.println("Total number of tasks " + fleet.globalTasks.size() + " should be 23.");
        assertEquals(23 , fleet.globalTasks.size());

        //Unit test doesn't test randomness, if required using deterministic random generator...
        //assertThat("Total number of tasks should be lte 34", fleet.globalTasks.size(), lessThanOrEqualTo(34));
        //assertThat("Total number of tasks should be gte 23", fleet.globalTasks.size(), greaterThanOrEqualTo(23));
        System.out.println();
    }
}
