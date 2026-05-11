package org.coreplugin.worldgen;

import java.util.Random;

public class SimplexNoise {

    private static final int[][] GRAD3 = {
        {1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0},
        {1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},
        {0,1,1},{0,-1,1},{0,1,-1},{0,-1,-1}
    };

    private final int[] perm = new int[512];

    public SimplexNoise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        Random rng = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    public double eval(double xin, double yin) {
        final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
        final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;

        double n0, n1, n2;

        double s = (xin + yin) * F2;
        int i = fastFloor(xin + s);
        int j = fastFloor(yin + s);

        double t = (i + j) * G2;
        double x0 = xin - (i - t);
        double y0 = yin - (j - t);

        int i1 = x0 > y0 ? 1 : 0;
        int j1 = x0 > y0 ? 0 : 1;

        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2;
        double y2 = y0 - 1.0 + 2.0 * G2;

        int ii = i & 255;
        int jj = j & 255;
        int gi0 = perm[ii +     perm[jj    ]] % 12;
        int gi1 = perm[ii + i1 + perm[jj + j1]] % 12;
        int gi2 = perm[ii + 1 +  perm[jj + 1 ]] % 12;

        double t0 = 0.5 - x0*x0 - y0*y0;
        n0 = t0 < 0 ? 0.0 : (t0 * t0) * (t0 * t0) * dot(GRAD3[gi0], x0, y0);

        double t1 = 0.5 - x1*x1 - y1*y1;
        n1 = t1 < 0 ? 0.0 : (t1 * t1) * (t1 * t1) * dot(GRAD3[gi1], x1, y1);

        double t2 = 0.5 - x2*x2 - y2*y2;
        n2 = t2 < 0 ? 0.0 : (t2 * t2) * (t2 * t2) * dot(GRAD3[gi2], x2, y2);

        return 70.0 * (n0 + n1 + n2);
    }

    private static double dot(int[] g, double x, double y) {
        return g[0] * x + g[1] * y;
    }

    private static int fastFloor(double x) {
        return x > 0 ? (int) x : (int) x - 1;
    }
}