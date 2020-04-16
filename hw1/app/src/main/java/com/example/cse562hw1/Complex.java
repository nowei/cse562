package com.example.cse562hw1;

public class Complex {
    public final double r;
    public final double i;
    public Complex(double real, double imag) {
        r = real;
        i = imag;
    }
    public double abs() {
        return Math.hypot(r, i);
    }

    public Complex plus(Complex b) {
        Complex a = this;
        double real = a.r + b.r;
        double imag = a.i + b.i;
        return new Complex(real, imag);
    }

    public Complex minus(Complex b) {
        Complex a = this;
        double real = a.r - b.r;
        double imag = a.i - b.i;
        return new Complex(real, imag);
    }

    public Complex times(Complex b) {
        Complex a = this;
        double real = a.r * b.r - a.i * b.i;
        double imag = a.r * b.i + a.i * b.r;
        return new Complex(real, imag);
    }
}
