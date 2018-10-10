package server.Engine;

/***************************************************************************
 * 	FILE: 			Data.java
 *
 * 	AUTHOR: 		ERIN SONG
 *
 * 	DATE:			2017/6/26
 *
 * 	VER: 			1.1
 *
 * 	Purpose:        Store the task generated by simulation, greatly compressing
 * 	                the need of memory.
 **************************************************************************/

public class Data {

    public double[][][] data;
    public double[][] avg;
    public double[][] std;

    /****************************************************************************
     *
     *	Shado Object:	Data
     *
     *	Purpose:		Generate an Data object that has a particular size of i,j,k
     *
     ****************************************************************************/


    public Data(int i, int j, int k) {

        data = new double[i][j][k];
        avg = new double[i][j];
        std = new double[i][j];

        for (int ii = 0; ii < i; ii++) {
            for (int jj = 0; jj < j; jj++) {
                for (int kk = 0; kk < k; kk++) {
                    data[ii][jj][kk] = 0;
                }
            }
        }

    }

    // INSPECTORS

    public double dataGet(int i, int j, int k) {
        return data[i][j][k];
    }

    // MUTATOR

    public void dataInc(int i, int j, int k, double inc) {
        data[i][j][k] += inc;
    }

    /****************************************************************************
     *
     *	Method:			avgdata
     *
     *	Purpose:		updating the average 2D array, which is the average across simulations
     *
     ****************************************************************************/

    public void avgData() {

        double delta;

        //calculate mean and std dev across all replications

        for (int x = 0; x < data.length; x++) {
            for (int y = 0; y < data[x].length; y++) {

                int N = 0;
                double mean = 0;
                double devSum = 0;

                for (int z = 0; z < data[x][y].length; z++) {
                    N++;
                    delta = data[x][y][z] - mean;
                    mean += delta / N;
                    devSum += delta * (data[x][y][z] - mean);
                }

                avg[x][y] = mean;
                std[x][y] = Math.sqrt(devSum / (N - 1));
            }
        }
    }

}
