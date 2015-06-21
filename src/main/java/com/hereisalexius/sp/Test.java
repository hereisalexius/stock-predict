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

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.transfer.TransferFunction;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.util.TransferFunctionType;
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
        Series series = initSeries();
        NeuralNetwork nn = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, 30, 80, 1);
        BackPropagation l = new BackPropagation();
        l.setMaxError(0.0001);
        l.setLearningRate(0.7);
        l.setMaxIterations(10000);
        nn.setLearningRule(l);
        nn.randomizeWeights(new SecureRandom());
        nn.learn(series.getTrainingSet(30));
        nn.setInput(series.getLast(30));
        nn.calculate();
        series.appendValues(nn.getOutput()[0] * series.getMaxValue());
        double r = nn.getOutput()[0] * series.getMaxValue();

        Series sInterp = new Series(series.getValues());

        NeuralNetwork nn2 = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, 30, 80, 30);
        BackPropagation l2 = new BackPropagation();
        l2.setMaxError(0.0001);
        l2.setLearningRate(0.7);
        l2.setMaxIterations(10000);
        nn2.setLearningRule(l);
        nn2.randomizeWeights(new SecureRandom());
        nn2.learn(sInterp.getTrainingForFindError(30, 30));
        nn2.setInput(sInterp.getLast(30));
        nn2.calculate();
        Series iy = new Series(nn2.getOutput());

        for (int i = 0; i < 29; i++) {
            System.out.println(r + iy.getInterpolatedValues()[i]);
        }

    }

    public static Series initSeries() {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -1);

        Stock stock = YahooFinance.get("GE", from, to, Interval.DAILY);

        List<HistoricalQuote> hql = stock.getHistory();
        Collections.reverse(hql);
        Series s = new Series();

        for (HistoricalQuote hq : hql) {
            s.appendValues(hq.getAdjClose().doubleValue());
        }
        return s;
    }

}
