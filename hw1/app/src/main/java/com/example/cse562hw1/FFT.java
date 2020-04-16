package com.example.cse562hw1;

import android.util.Log;

// From: http://www.ee.columbia.edu/~ronw/code/MEAPsoft/doc/html/FFT_8java-source.html
public class FFT {
    int n, m;

    // Lookup tables.
    double[] cos;
    double[] sin;

    double[] window;
    public FFT(int n) {
        this.n = n;
        this.m = (int)(Math.log(n) / Math.log(2));
        // Make sure it's a power of 2
        if (n != (1 << m))
            throw new RuntimeException("FFT length wasn't a power of 2");
        cos = new double[n/2];
        sin = new double[n/2];
        for (int i=0; i < n/2; i++) {
            cos[i] = Math.cos(-2 * Math.PI * i / n);
            sin[i] = Math.sin(-2 * Math.PI * i / n);
        }

        makeWindow();
    }

    protected void makeWindow() {
        // blackman window
        // w(n) = 0.42 - 0.5 * cos{(2*PI*n)/(N-1)} + 0.08cos{(4*PI*n)/(N-1)};
        window = new double[n];
        for (int i = 0; i < window.length; i++) {
            window[i] = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1)) +
                    0.08 * Math.cos(4 * Math.PI * i / (n - 1));
        }
    }

    public double[] getWindow() {
        return window;
    }

    /***************************************************************
    00089   * fft.c
    00090   * Douglas L. Jones
    00091   * University of Illinois at Urbana-Champaign
    00092   * January 19, 1992
    00093   * http://cnx.rice.edu/content/m12016/latest/
    00094   *
    00095   *   fft: in-place radix-2 DIT DFT of a complex input
    00096   *
    00097   *   input:
    00098   * n: length of FFT: must be a power of two
    00099   * m: n = 2**m
    00100   *   input/output
    00101   * x: double array of length n with real part of data
    00102   * y: double array of length n with imag part of data
    00103   *
    00104   *   Permission to copy and use this program is granted
    00105   *   as long as this header is included.
    ****************************************************************/
//    public void fft(double[] x, double[] y) {
//        int i, j, k, n1, n2, a;
//        double c, s, e, t1, t2;
//
//        // Bit reverse
//        j = 0;
//        n2 = n / 2;
//        for (i = 1; i < n - 1; i++) {
//            n1 = n2;
//            while (j >= n1) {
//                j = j - n1;
//                n1 = n1 / 2;
//            }
//            j = j + n1;
//
//            if (i < j) {
//                t1 = x[i];
//                x[i] = x[j];
//                x[j] = t1;
//                t1 = y[i];
//                y[i] = y[j];
//                y[j] = t1;
//            }
//        }
//        // FFT
//        n1 = 0;
//        n2 = 1;
//        for (i = 0; i < m; i++) {
//            n1 = n2;
//            n2 = n2 + n2;
//            a = 0;
//
//            for (j = 0; j < n1; j++) {
//                c = cos[a];
//                s = sin[a];
//                a += 1 << (m - i - 1);
//
//                for (k = j; k < n; k = k + n2) {
//                    t1 = c * x[k+n1] - s * y[k+n1];
//                    t2 = s * x[k + n1] + c * y[k+n1];
//                    x[k + n1] = x[k] - t1;
//                    y[k+n1] = y[k] - t2;
//                    x[k] = x[k] + t1;
//                    y[k] = y[k] + t2;
//                }
//            }
//        }
//    }
    public Complex[] fft(Complex[] x) {
        int n = x.length;
        if (n == 1) return new Complex[]{ x[0] };
        Complex[] even = new Complex[n / 2];
        for (int k = 0; k < n / 2; k++) {
            even[k] = x[2*k];
        }
        Complex[] evenFFT = fft(even);

        Complex[] odd = even;
        for (int k = 0; k < n / 2; k++) {
            odd[k] = x[2 * k + 1];
        }
        Complex[] oddFFT = fft(odd);

        Complex[] y = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            Complex wkOdd = wk.times(oddFFT[k]);
            y[k]       = evenFFT[k].plus(wkOdd);
            y[k + n/2] = evenFFT[k].minus(wkOdd);
        }
        return y;
    }
}
