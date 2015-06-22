package com.hereisalexius.sp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.neuroph.core.data.DataSet;

public class Series {

    private double[] values;

    public Series() {
        this.values = new double[0];
    }

    public Series(double... values) {
        this.values = values;
    }

    public double[] getValues() {
        return values;
    }

    public void setValues(double[] values) {
        this.values = values;
    }

    public double[] getIds() {
        double[] ids = new double[this.values.length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;
        }
        return ids;
    }

    public double[] appendValues(double... data) {
        double[] oldValues = this.values;
        this.values = new double[oldValues.length + data.length];
        System.arraycopy(oldValues, 0, this.values, 0, oldValues.length);
        System.arraycopy(data, 0, this.values, oldValues.length, data.length);
        return values;
    }

    public double get(int id) {
        return this.values[id];
    }

    public int getId(double value) {
        int id = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                id = i;
                break;
            }
        }
        return id;
    }

    public int size() {
        return values.length;
    }

    public Series getSubSeries(int from, int to) {
        return new Series(Arrays.copyOfRange(values, from, to));
    }

    public double[] getInterpolatedValues() {
        double[] result = new double[this.values.length - 1];

        for (int i = 0; i < result.length; i++) {
            result[i] = (get(i + 1) - get(i)) * 10;
        }
        return result;
    }

//    public Map<double[], double[]> getTrainingSet(int timeToPredict) {
//        Map<double[], double[]> trainingSet = new TreeMap<>();
//
//        int window = (int) this.getBestFFTSpectralAnalizeId();
//        double[] normalized = this.getNormalizedValues();
//
//        for (int i = 0; i < normalized.length - timeToPredict; i++) {
//            double[] in = Arrays.copyOfRange(normalized, i, window + i);
//            double[] out = Arrays.copyOfRange(normalized, window + i, timeToPredict + (i + 2));
//            trainingSet.put(in, out);
//        }
//
//        return trainingSet;
//    }
    public DataSet getTrainingForFindError() {
        int input = (int) this.getBestFFTSpectralAnalizeId();
        double[] norm = this.getNormalizedValues();
        int output = norm.length - input;
        DataSet traningSet = new DataSet(input, output);
        double[] in = new double[input];
        double[] out = new double[output];

        for (int i = 0; i < norm.length - (output + input); i++) {
            for (int j = i; j < i + input; j++) {
                in[j - i] = norm[j];
            }
            for (int k = 0; k < output; k++) {
                out[k]
                        = norm[(i + input + k)];
            }
            traningSet.addRow(in, out);
        }
        return traningSet;
    }

    public DataSet getTrainingForFindError(int input, int output) {

        double[] norm = this.getNormalizedValues();

        DataSet traningSet = new DataSet(input, output);
        double[] in = new double[input];
        double[] out = new double[output];

        for (int i = 0; i < norm.length - (output + input); i++) {
            for (int j = i; j < i + input; j++) {
                in[j - i] = norm[j];
            }
            for (int k = 0; k < output; k++) {
                out[k]
                        = norm[(i + input + k)];
            }
            traningSet.addRow(in, out);
        }
        return traningSet;
    }

    public DataSet getTrainingSet() {
        int input = (int) this.getBestFFTSpectralAnalizeId();
        DataSet traningSet = new DataSet(input, 1);
        double[] norm = this.getNormalizedValues();
        double[] in = new double[input];
        double[] out = new double[1];

        for (int i = 0; i < norm.length - (1 + input); i++) {
            for (int j = i; j < i + input; j++) {
                in[j - i] = norm[j];
            }

            out[0]
                    = norm[i + input];

            traningSet.addRow(in, out);
        }
        return traningSet;
    }

    public DataSet getTrainingSet(int input) {

        DataSet traningSet = new DataSet(input, 1);
        double[] norm = this.getNormalizedValues();
        double[] in = new double[input];
        double[] out = new double[1];

        for (int i = 0; i < norm.length - (1 + input); i++) {
            for (int j = i; j < i + input; j++) {
                in[j - i] = norm[j];
                in[(j - i)] = j / norm.length;
            }

            out[0]
                    = norm[i + input];

            traningSet.addRow(in, out);
        }
        return traningSet;
    }

    public double getMaxValue() {
        double max = 0;
        for (double value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public double getMinValue() {
        double min = 0;
        for (double value : values) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    public double[] getNormalizedValues() {
        double[] normalized = new double[values.length];

        for (int i = 0; i < normalized.length; i++) {
            normalized[i] = values[i] / this.getMaxValue();
        }

        return normalized;
    }

    public Iterator<Double> getValuesIterator() {
        List<Double> list = new ArrayList<>();
        for (double v : values) {
            list.add(v);
        }
        return list.iterator();
    }

    public double[] getFFTSpectralAnalizeResults() {
        FastFourierTransformer ffts = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] r = ffts.transform(values, TransformType.FORWARD);

        double[] fft = new double[r.length / 2];

        for (int i = 0; i < fft.length; i++) {
            fft[i] = r[i + 1].abs();
        }
        return fft;
    }

    public double getBestFFTSpectralAnalizeId() {// Except 0
        double result = 2;
        double max = 0;
        double[] fft = getFFTSpectralAnalizeResults();
        for (int i = 0; i < fft.length; i++) {
            if (fft[i] > max && i > 1) {
                max = fft[i];
                result = i;
            }
        }
        return result;
    }

//    public double getMaxFFTSpectralAnalizeValue() {
//        return getFFTSpectralAnalizeResults()[(int) getBestFFTSpectralAnalizeId()];
//    }
    public void convertForPowerOfTwo() {
        int l = values.length;
        values = Arrays.copyOfRange(values, l - getMaxOfPowerOf2(l), l);
    }

    public double[] getLast() {
        return Arrays.copyOfRange(values, size() - (int) getBestFFTSpectralAnalizeId(), size());
    }

    public double[] getFirst() {
        return Arrays.copyOfRange(values, 0, (int) getBestFFTSpectralAnalizeId());
    }

    public double[] getFirst(int outerWindowSize) {
        return Arrays.copyOfRange(values, 0, outerWindowSize);
    }

    public double[] getLast(int outerWindowSize) {
        return Arrays.copyOfRange(values, size() - outerWindowSize, size());
    }

    public static int getMaxOfPowerOf2(int size) {
        int fpot = 2;
        while (fpot < size) {
            fpot *= 2;
        }
        return fpot / 2;
    }

}
