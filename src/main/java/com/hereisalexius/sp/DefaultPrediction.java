/*
 * Copyright (C) 2015 Olexiy Polishcuk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.hereisalexius.sp;

import com.xeiam.xchart.Chart;
import com.xeiam.xchart.ChartBuilder;
import com.xeiam.xchart.SeriesMarker;
import com.xeiam.xchart.XChartPanel;
import java.awt.Color;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.LearningRule;
import org.neuroph.core.transfer.Linear;
import org.neuroph.core.transfer.TransferFunction;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.util.TransferFunctionType;
import org.neuroph.util.random.GaussianRandomizer;

/**
 *
 * @author hereisalexius
 */
public class DefaultPrediction extends javax.swing.JDialog {

    private final Series series;
    private final TransferFunctionType tft;
    private final LearningRule lr;

    private int daysTopredict;
    private int testSelection;

    public DefaultPrediction(java.awt.Frame parent, boolean modal, Series series, TransferFunctionType tft, LearningRule lr, int daysToPredict,int testSelection) {
        super(parent, modal);
        this.tft = tft;
        this.series = series;
        this.lr = lr;
        this.daysTopredict = daysToPredict;
        this.testSelection = testSelection;
       
        //series.convertForPowerOfTwo();

        initComponents();
        findAll();

    }

    private void findAll() {
        predict();
        setUpErrors();
        drawFFT();

        // predictTest();
    }

    private void setUpErrors() {

        int daysToPredict = testSelection;
        int windowSize = 2;
        int input = windowSize;
        int hidden = 2 * input + 1;
        int output = daysToPredict;

        NeuralNetwork neuralNetwork = new MultiLayerPerceptron(tft, input, hidden, output);
//        LearningRule learningRule = new BackPropagation();
//
//        ((BackPropagation) learningRule).setMaxError(0.00001);
//        ((BackPropagation) learningRule).setMaxIterations(10000);
//        ((BackPropagation) learningRule).setLearningRate(0.7);
        neuralNetwork.setLearningRule(lr);

//        if (((BackPropagation) lr).getMaxError() >= 0.00001) {
//            neuralNetwork.randomizeWeights(new SecureRandom());
//        } else {
//            neuralNetwork.randomizeWeights(new GaussianRandomizer(0.5, 0.7));
//        }
  neuralNetwork.randomizeWeights(new SecureRandom());
        neuralNetwork.learn(series.getTrainingForFindError(input, output));

        double[] selection = new double[input];

        for (int i = 0; i < input; i++) {
            selection[i] = series.getNormalizedValues()[series.getNormalizedValues().length - (i + 1)];//FIX
        }

        neuralNetwork.setInput(selection);

        neuralNetwork.calculate();

        SortedMap<Integer, Double> rowPredicted = new TreeMap<>();

        double[] result = neuralNetwork.getOutput();

        int h = daysToPredict;

        double sum = 0;
        double[] norm = series.getNormalizedValues();
        int i = series.size() - 1 - daysToPredict;
        for (double s : result) {

            rowPredicted.put(i, s * series.getMaxValue());

            i++;
            sum += Math.abs(norm[series.size() - h] - s) / norm[series.size() - h];
            h--;
        }
        rowPredicted.put(series.size()-1, series.get(series.size()-1));
        jTextField2.setText(String.valueOf(sum / daysToPredict));
        SortedMap<Integer, Double> realRow = new TreeMap<>();
        for (int j = 0; j < series.size(); j++) {
            realRow.put(j, series.get(j));

        }

        drawChartE(realRow, rowPredicted,predictTest());

    }

    private void drawFFT() {
        series.convertForPowerOfTwo();
        jTextField3.setText(String.valueOf(series.getBestFFTSpectralAnalizeId()));
        Chart chart = new ChartBuilder().width(jPanel2.getWidth()).height(jPanel2.getHeight()).title("Max FFT Frequence = " + series.getBestFFTSpectralAnalizeId() + " FFTF>1").build();
        chart.getStyleManager().setLegendVisible(false);

        double[] ids = new double[series.getFFTSpectralAnalizeResults().length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;

        }
        chart.addSeries("real", ids, series.getFFTSpectralAnalizeResults()).setMarker(SeriesMarker.NONE);
        //chart.addSeries("max", new double[]{series.getBestFFTSpectralAnalizeId(), series.getBestFFTSpectralAnalizeId()}, new double[]{0, series.getMaxFFTSpectralAnalizeValue()}).setMarker(SeriesMarker.NONE);

        XChartPanel cp = new XChartPanel(chart);
        cp.setSize(jPanel2.getSize());
        cp.setVisible(true);
        jPanel2.add(cp);

    }

    private void drawChartE(Map<Integer, Double> row, Map<Integer, Double> row2, Map<Integer, Double> row3) {
        Chart chart = new ChartBuilder().width(jPanel1.getWidth()).height(jPanel1.getHeight()).title("Economics").build();
        chart.getStyleManager().setLegendVisible(false);
        //chart.getStyleManager().setChartType(StyleManager.ChartType.Scatter);
        chart.addSeries("real", row.keySet(), row.values()).setMarker(SeriesMarker.NONE);
        chart.addSeries("checked", row2.keySet(), row2.values()).setMarker(SeriesMarker.NONE);
        chart.addSeries("predicted", row3.keySet(), row3.values()).setMarker(SeriesMarker.NONE).setLineColor(Color.red);
        XChartPanel cp = new XChartPanel(chart);
        cp.setVisible(true);
        cp.setSize(jPanel1.getSize());
        jPanel1.add(cp);
    }

    private void predict() {
        long start = System.currentTimeMillis();
        int window = (int) series.getBestFFTSpectralAnalizeId();
        NeuralNetwork neuralNetwork = new MultiLayerPerceptron(tft, window, 2 * window + 1, 1);
        neuralNetwork.setLearningRule(lr);
        neuralNetwork.randomizeWeights(new SecureRandom());
        neuralNetwork.learn(series.getTrainingSet());
        neuralNetwork.setInput(series.getLast());
        neuralNetwork.calculate();
        jTextField1.setText(String.valueOf(neuralNetwork.getOutput()[0] * series.getMaxValue()));
        this.series.appendValues(neuralNetwork.getOutput()[0] * series.getMaxValue());
        long finish = System.currentTimeMillis();
        jTextField4.setText(String.valueOf(finish - start));
    }

    private Map<Integer, Double> predictTest() {

        Series sInterp = new Series(series.getValues());

        NeuralNetwork nn2 = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, 30, 80, daysTopredict);
        BackPropagation l2 = new BackPropagation();
        l2.setMaxError(0.0001);
        l2.setLearningRate(0.7);
        l2.setMaxIterations(10000);
        nn2.setLearningRule(l2);
        nn2.randomizeWeights(new SecureRandom());
        nn2.learn(sInterp.getTrainingForFindError(30, daysTopredict));
        nn2.setInput(sInterp.getLast(30));
        nn2.calculate();
        Series iy = new Series(nn2.getOutput());

        Map<Integer, Double> m = new HashMap<>();
        for (int i = 0; i < iy.getInterpolatedValues().length; i++) {
            m.put(i + series.size()-1, series.get(series.size() - 1) + iy.getInterpolatedValues()[i]);
            jTextField5.setText(jTextField5.getText()+series.get(series.size() - 1) + iy.getInterpolatedValues()[i]+",");
  
        }
        return m;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextField1 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jTextField1.setEditable(false);

        jLabel1.setText("Predicted value");

        jLabel2.setText("Fast Furiet Transform frequency");

        jTextField2.setEditable(false);

        jLabel3.setText("Error");

        jTextField3.setEditable(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 935, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 561, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Prediction Chart", jPanel1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 935, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 561, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("FFT Chart", jPanel2);

        jLabel4.setText("Time(millis)");

        jTextField4.setEditable(false);

        jLabel5.setText("HRP");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel1))
                        .addGap(93, 93, 93)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 102, Short.MAX_VALUE)
                            .addComponent(jTextField2)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField3)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jTextField5))
                .addContainerGap())
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    // End of variables declaration//GEN-END:variables
}
