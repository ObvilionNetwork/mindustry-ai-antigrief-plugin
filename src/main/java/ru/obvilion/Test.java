package ru.obvilion;

import org.neuroph.core.data.DataSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.util.TransferFunctionType;

public class Test {
    public static void main(String[] args) {
        MultiLayerPerceptron neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, 2, 3, 2, 1);
        DataSet neuralData = new DataSet(2, 1);

        neuralData.addRow(new double[] {1, 1}, new double[] {1});
        neuralData.addRow(new double[] {0, 0}, new double[] {0});
        neuralData.addRow(new double[] {0, 1}, new double[] {0.5});
        neuralData.addRow(new double[] {1, 0}, new double[] {0.5});

        BackPropagation backPropagation = new BackPropagation();
        backPropagation.setMaxError(0.001d);

        neuralNetwork.learn(neuralData, backPropagation);


        neuralNetwork.setInput(0, 1);
        neuralNetwork.calculate();
        System.out.println(neuralNetwork.getOutput()[0]);

        neuralNetwork.setInput(1, 1);
        neuralNetwork.calculate();
        System.out.println(neuralNetwork.getOutput()[0]);

        neuralNetwork.setInput(0.5, 0.5);
        neuralNetwork.calculate();
        System.out.println(neuralNetwork.getOutput()[0]);
    }
}
