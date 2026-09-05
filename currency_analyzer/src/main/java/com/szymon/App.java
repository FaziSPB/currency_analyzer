package com.szymon;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.List;
import java.util.Map;

public class App extends Application {
    private NbpService nbpService = new NbpService();
    
    @Override
    public void start(Stage primaryStage) {
        Map<String, List<Rate>> ratesMap = nbpService.getGroupedRates();
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Data");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Exchange rate (PLN)");
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Exchange rates graph");
        lineChart.setAnimated(false); 
        ComboBox<String> currencySelector = new ComboBox<>();
        currencySelector.setItems(FXCollections.observableArrayList(ratesMap.keySet()));
        currencySelector.setPromptText("Choose the currency...");
        currencySelector.setOnAction(event -> {
            String selectedCode = currencySelector.getValue();
            if (selectedCode != null) {
                List<Rate> selectedRates = ratesMap.get(selectedCode);
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(selectedCode);
                for (Rate rate : selectedRates) {
                    series.getData().add(new XYChart.Data<>(rate.date(), rate.mid()));
                }
                lineChart.getData().clear();
                lineChart.getData().add(series);
            }
        });
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getChildren().addAll(new Label("Choose the currency from the list:"), currencySelector, lineChart);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("NBP Exchange Rates Analyzer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args); 
    }
}