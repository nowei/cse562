package com.example.hw2;

import java.util.Arrays;

public class Quaternion {
    double[] q;

    public Quaternion() {
        q = new double[4];
    }

    public Quaternion(double q0, double q1, double q2, double q3) {
        q = new double[]{q0, q1, q2, q3};
    }

    public Quaternion copy() {
        return new Quaternion(q[0], q[1], q[2], q[3]);
    }

    public static Quaternion identity() {
        return new Quaternion(0.0, 0.0, 0.0, 1.0);
    }

    // Angle given in radians
    public static Quaternion setFromAngleAxis(double angle, double vx, double vy, double vz) {
        double hrad = angle;
        double s = Math.sin(hrad);
        return new Quaternion(Math.cos(hrad), vx * s, vy * s, vz * s);
    }

    public double length() {
        return Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
    }

    public Quaternion normalize() {
        double l = length();
        return new Quaternion(q[0]/l, q[1]/l, q[2]/l, q[3]/l);
    }

    public Quaternion inverse() {
        double l2 = length();
        l2 *= l2;
        return new Quaternion(q[0]/l2,q[1]/l2,q[2]/l2,q[3]/l2);
    }

    public static Quaternion multiply(Quaternion a, Quaternion b) {
        Quaternion q = new Quaternion();
        q.q[0] = a.q[0] * b.q[0] - a.q[1] * b.q[1] - a.q[2] * b.q[2] - a.q[3] * b.q[3];
        q.q[1] = a.q[0] * b.q[1] + a.q[1] * b.q[0] + a.q[2] * b.q[3] - a.q[3] * b.q[2];
        q.q[2] = a.q[0] * b.q[2] - a.q[1] * b.q[3] + a.q[2] * b.q[0] + a.q[3] * b.q[1];
        q.q[3] = a.q[0] * b.q[3] + a.q[1] * b.q[2] - a.q[2] * b.q[1] + a.q[3] * b.q[0];
        return q;
    }

    public Quaternion rotate(Quaternion r) {
        Quaternion rq = multiply(r, this);
        return multiply(rq, r.inverse());
    }

    @Override
    public String toString() {
        return Arrays.toString(q);
    }
}
