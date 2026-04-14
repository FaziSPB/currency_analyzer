package com.szymon;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

record Rate(String currency, String code, double mid) {}
record NbpTable(String table, String no, String effectiveDate, List<Rate> rates) {}

public class App {
    public static void main(String[] args) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://api.nbp.pl/api/exchangerates/tables/a/last/30/?format=json"))
                .build();
        try {
            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            ObjectMapper mapper = new ObjectMapper();
            NbpTable[] tables = mapper.readValue(body, NbpTable[].class);
            Map<String, List<Rate>> groupedRates = new HashMap<>();
            for (NbpTable table : tables) {
                for (Rate rate : table.rates()) {
                    groupedRates.putIfAbsent(rate.currency(), new ArrayList<>());
                    groupedRates.get(rate.currency()).add(rate);
                }
            }
            System.out.println("---------------------------");
            groupedRates.entrySet().parallelStream().forEach(entry -> {
                String currency = entry.getKey();
                List<Rate> ratesList = entry.getValue();
                String code = ratesList.get(0).code();
                double max = ratesList.stream()
                        .mapToDouble(Rate::mid)
                        .max()
                        .orElse(0.0);
                        
                double avg = ratesList.stream()
                        .mapToDouble(Rate::mid)
                        .average()
                        .orElse(0.0);
                String threadName = Thread.currentThread().getName();
                System.out.println(String.format("[Wątek: %-20s] Waluta: %s | Kod: %s | Max: %.4f | Średnia: %.4f", 
                        threadName, currency, code, max, avg));
            });           
            } catch (Exception e) {
                        e.printStackTrace();
            }
    }
}