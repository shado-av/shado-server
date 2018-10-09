package server.Util;

public class Util {

    /****************************************************************************
     *
     *	Method:     round
     *
     *	Purpose:    Round double numbers
     *
     ****************************************************************************/

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}