package org.coreplugin.worldgen;

public class FBMNoise {

    private final SimplexNoise noise;
    private final int octaves;
    private final double frequency;
    private final double lacunarity;
    private final double persistence;
    private final double warpStrength;

    public FBMNoise(long seed, int octaves, double frequency, double lacunarity, double persistence, double warpStrength) {
        this.noise = new SimplexNoise(seed);
        this.octaves = octaves;
        this.frequency = frequency;
        this.lacunarity = lacunarity;
        this.persistence = persistence;
        this.warpStrength = warpStrength;
    }

    public double evaluate(double x, double z) {
        if (warpStrength != 0.0) {
            double wx = fbm(x, z);
            double wz = fbm(x + 3.7, z + 1.3);
            x += warpStrength * wx;
            z += warpStrength * wz;
        }
        return fbm(x, z);
    }

    private double fbm(double x, double z) {
        double value = 0.0;
        double amplitude = 1.0;
        double freq = frequency;
        double maxValue = 0.0;

        for (int i = 0; i < octaves; i++) {
            value += noise.eval(x * freq, z * freq) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            freq *= lacunarity;
        }

        return value / maxValue;
    }
}