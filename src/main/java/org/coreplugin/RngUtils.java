package org.coreplugin;

import java.util.Random;

public final class RngUtils {

    private RngUtils() {}

    // Knuth algorithm for Poisson(lambda) sampling
    public static int poissonSample(Random rng, double lambda) {
        if (lambda <= 0) return 0;
        double L = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do { k++; p *= rng.nextDouble(); } while (p > L);
        return k - 1;
    }
}