package com.hereisalexius.sp;

import com.xeiam.xchart.Chart;
import com.xeiam.xchart.ChartBuilder;
import com.xeiam.xchart.SeriesMarker;
import com.xeiam.xchart.XChartPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.LearningRule;

import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.util.TransferFunctionType;

public class DefaultPrediction extends javax.swing.JDialog {

    private final Series series;
    private final TransferFunctionType tft;
    private final LearningRule lr;

    private int daysTopredict;
    private int testSelection;
    private int input;
    private int hidden;

    public DefaultPrediction(java.awt.Frame parent, boolean modal, Series series, TransferFunctionType tft, LearningRule lr, int daysToPredict, int testSelection, int input, int hidden) {
        super(parent, modal);
        this.tft = tft;
        this.series = series;
        this.lr = lr;
        this.daysTopredict = daysToPredict;
        this.testSelection = testSelection;
        this.input = input;
        this.hidden = hidden;
        //series.convertForPowerOfTwo();

        initComponents();
        findAll();

    }

    private void findAll() {
        predict();
        setUpErrors();
    }

    private void setUpErrors() {

        int daysToPredict = testSelection;
        int windowSize = input;
        int input = windowSize;
        int hidden = this.hidden;
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
//     } else {
//       neuralNetwork.randomizeWeights(new GaussianRandomizer(0.5, 0.7));
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
        rowPredicted.put(series.size() - 1, series.get(series.size() - 1));
        jTextField2.setText(String.format("%.2f",(sum / daysToPredict)));
        SortedMap<Integer, Double> realRow = new TreeMap<>();
        for (int j = 0; j < series.size(); j++) {
            realRow.put(j, series.get(j));

        }

        drawChartE(realRow, rowPredicted, predictTest());

    }

    private void drawChartE(Map<Integer, Double> row, Map<Integer, Double> row2, Map<Integer, Double> row3) {
        Chart chart = new ChartBuilder().width(jPanel1.getWidth()).height(jPanel1.getHeight()).title("").build();
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
        NeuralNetwork neuralNetwork = new MultiLayerPerceptron(tft, input, hidden, 1);
        neuralNetwork.setLearningRule(lr);
        neuralNetwork.randomizeWeights(new SecureRandom());
        neuralNetwork.learn(series.getTrainingSet(input));
        neuralNetwork.setInput(series.getLast(input));
        neuralNetwork.calculate();
        jTextField1.setText(String.format("%.2f", neuralNetwork.getOutput()[0] * series.getMaxValue()));
        this.series.appendValues(neuralNetwork.getOutput()[0] * series.getMaxValue());
    }

    private Map<Integer, Double> predictTest() {
        Series sInterp = new Series(series.getValues());
        sInterp.convertForPowerOfTwo();
        Series iy = new Series();

        for (int i = 0; i < daysTopredict; i++) {
            NeuralNetwork nn2 = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, input, hidden, 1);
            BackPropagation l2 = new BackPropagation();
            l2.setMaxError(0.0001);
            l2.setLearningRate(0.7);
            l2.setMaxIterations(10000);
            nn2.setLearningRule(l2);
            nn2.randomizeWeights(new SecureRandom());
            nn2.learn(sInterp.getTrainingSet(input));
            nn2.setInput(sInterp.getLast(input));
            nn2.calculate();
            sInterp.appendValues(nn2.getOutput()[0] * sInterp.getMaxValue());
            iy.appendValues(nn2.getOutput()[0] * sInterp.getMaxValue());
        }
        Map<Integer, Double> m = new HashMap<>();
        m.put(series.size() - 1, series.get(series.size() - 1));
        for (int i = 0; i < iy.size(); i++) {
            m.put(i + series.size(), iy.get(i));

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
        jTextField2 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jTextField1.setEditable(false);

        jLabel1.setText("Прогноз");

        jTextField2.setEditable(false);

        jLabel3.setText("Похибка прогнозу");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 935, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 552, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("", jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables
}
