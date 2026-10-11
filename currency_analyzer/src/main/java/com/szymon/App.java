package com.szymon;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class App extends Application {
    private record LoadedData(Map<String, List<Rate>> ratesMap, Map<String, CurrencyStats> statsMap) {}
    private static final int MOVING_AVERAGE_WINDOW = 5;

    private NbpService nbpService = new NbpService();
    private Map<String, List<Rate>> ratesMap = Map.of();
    private Map<String, CurrencyStats> statsMap = Map.of();

    private Stage primaryStage;
    private LineChart<String, Number> lineChart;
    private ComboBox<String> currencySelector;
    private Button refreshButton;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;
    private Label statsLabel;
    private CheckBox movingAverageCheckBox;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Data");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Exchange rate (PLN)");
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Exchange rates graph");
        lineChart.setAnimated(false);
        VBox.setVgrow(lineChart, Priority.ALWAYS);
        currencySelector = new ComboBox<>();
        currencySelector.setPromptText("Choose the currency...");
        currencySelector.setOnAction(event -> showCurrency(currencySelector.getValue()));
        refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> loadData());
        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(20, 20);
        progressIndicator.setVisible(false);
        statusLabel = new Label();
        HBox toolbar = new HBox(10, currencySelector, refreshButton, progressIndicator, statusLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        movingAverageCheckBox = new CheckBox("Show " + MOVING_AVERAGE_WINDOW + "-day moving average");
        movingAverageCheckBox.setOnAction(event -> showCurrency(currencySelector.getValue()));
        HBox chartOptions = new HBox(10, movingAverageCheckBox);
        chartOptions.setAlignment(Pos.CENTER_LEFT);
        statsLabel = new Label("Choose the currency to see the stats.");
        statsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getChildren().addAll(new Label("Choose the currency from the list:"), toolbar, lineChart, chartOptions, statsLabel);
        Scene scene = new Scene(root, 900, 650);
        primaryStage.setTitle("NBP Exchange Rates Analyzer");
        primaryStage.setScene(scene);
        primaryStage.show();
        loadData();
    }

    // fetches rates on a background thread so the window stays responsive
    private void loadData() {
        Task<LoadedData> task = new Task<>() {
            @Override
            protected LoadedData call() throws Exception {
                Map<String, List<Rate>> rates = nbpService.getGroupedRates();
                if (rates.isEmpty()) {
                    throw new IOException("NBP API returned no exchange rates.");
                }
                return new LoadedData(rates, nbpService.calculateStatsParallel(rates));
            }
        };
        progressIndicator.visibleProperty().bind(task.runningProperty());
        refreshButton.disableProperty().bind(task.runningProperty());
        statusLabel.setText("Loading exchange rates...");
        task.setOnSucceeded(event -> applyData(task.getValue()));
        task.setOnFailed(event -> {
            updateStatus();
            showError("Could not fetch exchange rates from the NBP API.", task.getException());
        });
        Thread thread = new Thread(task, "nbp-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyData(LoadedData data) {
        String selectedCode = currencySelector.getValue();
        ratesMap = data.ratesMap();
        statsMap = data.statsMap();
        currencySelector.setItems(FXCollections.observableArrayList(ratesMap.keySet()));
        if (selectedCode != null && ratesMap.containsKey(selectedCode)) {
            currencySelector.setValue(selectedCode);
        }
        showCurrency(currencySelector.getValue());
        updateStatus();
    }

    private void showCurrency(String code) {
        lineChart.getData().clear();
        List<Rate> selectedRates = code == null ? null : ratesMap.get(code);
        if (selectedRates == null) {
            statsLabel.setText("Choose the currency to see the stats.");
            return;
        }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(code);
        for (Rate rate : selectedRates) {
            series.getData().add(new XYChart.Data<>(rate.date(), rate.mid()));
        }
        lineChart.getData().add(series);
        installTooltips(series);
        if (movingAverageCheckBox.isSelected() && selectedRates.size() >= MOVING_AVERAGE_WINDOW) {
            XYChart.Series<String, Number> averageSeries = new XYChart.Series<>();
            averageSeries.setName(MOVING_AVERAGE_WINDOW + "-day moving average");
            for (Rate rate : nbpService.movingAverage(selectedRates, MOVING_AVERAGE_WINDOW)) {
                averageSeries.getData().add(new XYChart.Data<>(rate.date(), rate.mid()));
            }
            lineChart.getData().add(averageSeries);
            averageSeries.getData().forEach(point -> point.getNode().setVisible(false));
        }
        CurrencyStats stats = statsMap.get(code);
        if (stats != null) {
            statsLabel.setText(String.format(
                "Stats (30 days) -> Average: %.4f PLN | Min: %.4f PLN (%s) | Max: %.4f PLN (%s)%n"
                    + "Std deviation: %.4f PLN | Change: %+.2f%%",
                stats.avg(), stats.min(), stats.minDate(), stats.max(), stats.maxDate(),
                stats.stdDev(), stats.changePercent()
            ));
        }
    }

    private void installTooltips(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> point : series.getData()) {
            Tooltip tooltip = new Tooltip(String.format("%s%n%.4f PLN", point.getXValue(), point.getYValue().doubleValue()));
            tooltip.setShowDelay(Duration.millis(100));
            Tooltip.install(point.getNode(), tooltip);
        }
    }

    private void updateStatus() {
        if (ratesMap.isEmpty()) {
            statusLabel.setText("No data loaded.");
            return;
        }
        List<Rate> anyRates = ratesMap.values().iterator().next();
        statusLabel.setText(String.format("%d currencies, data from %s to %s",
                ratesMap.size(), anyRates.get(0).date(), anyRates.get(anyRates.size() - 1).date()));
    }

    private void showError(String header, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(primaryStage);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        if (error != null) {
            alert.setContentText(Objects.toString(error.getMessage(), error.getClass().getSimpleName()));
        }
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
