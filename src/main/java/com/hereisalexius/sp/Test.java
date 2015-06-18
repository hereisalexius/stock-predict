/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hereisalexius.sp;

import com.xeiam.xchart.Chart;
import com.xeiam.xchart.ChartBuilder;
import com.xeiam.xchart.XChartPanel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.JFrame;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.learning.LearningRule;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.nnet.learning.LMS;
import org.neuroph.util.TransferFunctionType;
import org.neuroph.util.random.DistortRandomizer;
import org.neuroph.util.random.GaussianRandomizer;
import org.neuroph.util.random.WeightsRandomizer;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

/**
 *
 * @author hereisalexius
 */
public class Test {

    public static void main(String[] args) {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.MONTH, -18); // интервал ( Calendar.YEAR Calendar.DAY_OF_MONTH)
        // второй параметр разница между from  и to 
        // СЛЕДИТЬ ЧТОБЫ ВРЕМЯ ВЫБОРКИ СООТВЕТСТВОВАЛО РАЗМЕРАМ ОКНА НЕРОНОВ и.т.д.

        Stock stock = YahooFinance.get("GE", from, to, Interval.DAILY);// первый параметр акция

        SortedMap<Date, Double> realRow = new TreeMap<>();

        List<HistoricalQuote> hqs = stock.getHistory();
        Collections.reverse(hqs);

        for (HistoricalQuote hq : hqs) {
            realRow.put(hq.getDate().getTime(), hq.getAdjClose().doubleValue());
        }

        
        /******************************************************/
        /*
        РЕЗЕРВ
        
        int windowSize = 4;   //размер окна 
        int daysToPredict = 5; // колво дней
        int input = windowSize * daysToPredict; // входной слой
        int hidden = 2 * input + 1; // скрытый слой
        int output = daysToPredict; //выходной слой
        
        */
        
        int windowSize = 4;   //размер окна 
        int daysToPredict = 100; // колво дней
        int input = windowSize; // входной слой можна попробовать и просто windowSize
        int hidden = 2 * input + 1; // скрытый слой
        int output = daysToPredict; //выходной слой
        
        /******************************************************/
        
        
        
        Double max = 100.0D;

        SortedMap<Date, Double> normalizedRow = new TreeMap<>();

        for (Map.Entry<Date, Double> entrySet : realRow.entrySet()) {
            Date key = entrySet.getKey();
            Double value = entrySet.getValue();
            normalizedRow.put(key, value / max);
        }

        // В первом параметрее выбор типа активационной фунции, менять после точки контрол пробел
        NeuralNetwork neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, input, hidden, output);

        DataSet traningSet = new DataSet(input, output);
        double[] in = new double[input];
        double[] out = new double[output];
        List<Double> normalizedValues = new ArrayList<>(normalizedRow.values());

        for (int i = 0; i < normalizedValues.size() - (input + output); i++) {
            for (int j = i; j < i + input; j++) {
                in[j - i] = normalizedValues.get(j);
            }
            for (int k = 0; k < output; k++) {
                out[k] = normalizedValues.get(i + input + k);
            }
            traningSet.addRow(in, out);
        }
        
        // выбор BackPropagation и LMS параметры можна менять соответственно форме
        
//               LearningRule learningRule = new BackPropagation(); 
//        ((BackPropagation) learningRule).setMaxError(0.001); //
//        ((BackPropagation) learningRule).setMaxIterations(10000);
//        ((BackPropagation) learningRule).setLearningRate(0.7);
//        neuralNetwork.setLearningRule(learningRule);
        

        LearningRule learningRule = new LMS(); 
        ((LMS) learningRule).setMaxError(0.001); //
        ((LMS) learningRule).setMaxIterations(10000);
        ((LMS) learningRule).setLearningRate(0.7);
        neuralNetwork.setLearningRule(learningRule);

        //neuralNetwork.randomizeWeights(new DistortRandomizer(0.999));
        neuralNetwork.randomizeWeights(new SecureRandom());
        neuralNetwork.learn(traningSet);

        System.out.println(input);

        double[] selection = new double[input];
        for (int i = 0; i < input; i++) {
            selection[i] = normalizedValues.get(input - (i + 1));
        }

        neuralNetwork.setInput(selection);

        neuralNetwork.calculate();

        SortedMap<Date, Double> rowPredicted = new TreeMap<>();
        rowPredicted.put(realRow.lastKey(), realRow.get(realRow.lastKey()));

        double[] result = neuralNetwork.getOutput();

        Calendar c = Calendar.getInstance();
        c.setTime(hqs.get(hqs.size() - 1).getDate().getTime());
        for (double s : result) {
            c.add(Calendar.DAY_OF_MONTH, +1);
            rowPredicted.put(c.getTime(), s * max);
        }

        drawChart(realRow, rowPredicted);
    }

    public static void drawChart(Map<Date, Double> row, Map<Date, Double> row2) {
        Chart chart = new ChartBuilder().width(800).height(600).title("Economics").build();
        chart.getStyleManager().setLegendVisible(true);
        chart.addSeries("real", row.keySet(), row.values());
        chart.addSeries("predicted", row2.keySet(), row2.values());
        XChartPanel cp = new XChartPanel(chart);
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        f.setContentPane(cp);
        f.pack();
        f.setVisible(true);

    }
}
