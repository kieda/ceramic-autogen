package io.hostilerobot.ceramicrelief.util;

public final class Epsilon {
    private Epsilon(){}

    // default values.
    // todo - should we have different values of epsilon based on the class?
    private static double epsilon = 0.001;
    private static double epsilonSq = epsilon * epsilon;


    public static void setEpsilon(double epsilon) {
        Epsilon.epsilon = epsilon;
        Epsilon.epsilonSq = epsilon * epsilon;
    }

    public static double epsilon() {
        return epsilon;
    }

    public static double epsilonSq() {
        return epsilonSq;
    }

    public static boolean equals(double d1, double d2) {
        double difference = d2 - d1;
        return difference * difference <= epsilonSq();
    }
}
