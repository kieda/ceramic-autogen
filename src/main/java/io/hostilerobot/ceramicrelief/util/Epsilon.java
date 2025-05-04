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
    public static boolean isZero(double d1) {
        return d1 * d1 <= epsilonSq();
    }

    // true if d1 < v
    public static boolean lessThanZeroExclusive(double d1) {
        return d1 < -epsilon;
    }
    public static boolean lessThanZeroInclusive(double d1) {
        return d1 <= epsilon;
    }
    public static boolean greaterThanOneInclusive(double d1) {
        return d1 >= 1.0 - epsilon;
    }
    public static boolean betweenZeroOneExclusive(double d) {
        return epsilon < d && d < 1.0 - epsilon;
    }
    public static boolean betweenZeroOneInclusive(double d) {
        return -epsilon <= d && d <= 1.0 + epsilon;
    }
}
