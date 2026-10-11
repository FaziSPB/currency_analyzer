package com.szymon;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConverterPane extends TitledPane {
    private final NbpService nbpService;
    private final TextField amountField = new TextField("100");
    private final ComboBox<String> fromSelector = new ComboBox<>();
    private final ComboBox<String> toSelector = new ComboBox<>();
    private final Label resultLabel = new Label("No data loaded.");
    private Map<String, List<Rate>> ratesMap = Map.of();

    public ConverterPane(NbpService nbpService) {
        this.nbpService = nbpService;
        setText("Currency converter");
        amountField.setPrefColumnCount(8);
        amountField.textProperty().addListener((observable, oldValue, newValue) -> recalculate());
        fromSelector.setOnAction(event -> recalculate());
        toSelector.setOnAction(event -> recalculate());
        resultLabel.setStyle("-fx-font-weight: bold;");
        HBox content = new HBox(10, new Label("Amount:"), amountField, fromSelector, new Label("->"), toSelector,
                new Label("="), resultLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(10));
        setContent(content);
    }

    public void setRates(Map<String, List<Rate>> ratesMap) {
        this.ratesMap = ratesMap;
        List<String> codes = new ArrayList<>();
        codes.add(NbpService.BASE_CURRENCY);
        codes.addAll(ratesMap.keySet());
        String fromCode = fromSelector.getValue();
        String toCode = toSelector.getValue();
        fromSelector.setItems(FXCollections.observableArrayList(codes));
        toSelector.setItems(FXCollections.observableArrayList(codes));
        fromSelector.setValue(codes.contains(fromCode) ? fromCode : codes.contains("EUR") ? "EUR" : NbpService.BASE_CURRENCY);
        toSelector.setValue(codes.contains(toCode) ? toCode : NbpService.BASE_CURRENCY);
        recalculate();
    }

    private void recalculate() {
        String fromCode = fromSelector.getValue();
        String toCode = toSelector.getValue();
        if (ratesMap.isEmpty() || fromCode == null || toCode == null) {
            resultLabel.setText("No data loaded.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            amount = Double.NaN;
        }
        if (!Double.isFinite(amount) || amount < 0) {
            resultLabel.setText("Invalid amount");
            return;
        }
        List<Rate> anyRates = ratesMap.values().iterator().next();
        resultLabel.setText(String.format("%.2f %s  (1 %s = %.4f %s, rates from %s)",
                nbpService.convert(amount, fromCode, toCode, ratesMap), toCode,
                fromCode, nbpService.convert(1, fromCode, toCode, ratesMap), toCode,
                anyRates.get(anyRates.size() - 1).date()));
    }
}
