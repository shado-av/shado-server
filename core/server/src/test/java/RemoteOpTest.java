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

import server.Engine.*;
import server.Input.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RemoteOp.class)
public class RemoteOpTest
{
    loadparam vars;
    Operator op;
    Task mockTask;
    RemoteOp ops;

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

        mockTask = mock(Task.class);
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
    public void checkPhaseTest() throws Exception {

        // Given
        vars.numRemoteOp = 6;                   // sum of all the team size
        vars.flexTeamSize = 1;                  // flexible team size
        vars.numTeams = 3;                      // 3 teams
        vars.teamSize = new int[] { 1, 2, 3 };  //
        vars.AIDAtype = new int[][] { { 0, 0, 0}, { 0, 0, 0}, { 1, 0, 0} }; // third team has AI
        vars.opNames = new String[] { "Team 1", "Team 2", "Team 3" };

        ops = new RemoteOp(vars);

        // When gnerating all the operators...
        ops.genRemoteOp();

        // Then the number of ops generated should be 7
        System.out.println("Then the number of ops generated should be 7.");
        assertEquals(7, ops.getRemoteOp().length);

        System.out.println("Then the last operator of the third team (indexed at 5) should be AI.");
        assertEquals(true, ops.getRemoteOp()[5].isAI);
        System.out.println("Then the last operator of the third team (indexed at 5) should be named 'Equal Operator'.");
        assertEquals("Equal Operator", ops.getRemoteOp()[5].getName());

        System.out.println("Then the last operator of the generated should be named 'Flex Position'.");
        assertEquals("Flex Position No.0", ops.getRemoteOp()[6].getName());
    }

}
