package com.szymon;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

record Rate(String currency, String code, double mid, String date) {
    public Rate(String currency, String code, double mid) {
        this(currency, code, mid, null);
    }
}
record NbpTable(String table, String no, String effectiveDate, List<Rate> rates) {}
record CurrencyStats(String code, double max, double min, double avg) {};

public class NbpService {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    public Map<String, List<Rate>> getGroupedRates() {
        Map<String, List<Rate>> groupedRates = new HashMap<>();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://api.nbp.pl/api/exchangerates/tables/a/last/30/?format=json"))
                .build();
        try {
            String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            NbpTable[] tables = mapper.readValue(body, NbpTable[].class);
            for (NbpTable table : tables) {
                for (Rate rawRate : table.rates()) {
                    Rate rateWithDate = new Rate(
                        rawRate.currency(), 
                        rawRate.code(), 
                        rawRate.mid(), 
                        table.effectiveDate()
                    );
                    groupedRates.putIfAbsent(rateWithDate.code(), new ArrayList<>());
                    groupedRates.get(rateWithDate.code()).add(rateWithDate);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groupedRates;
    }
    //multithreading
    public Map<String, CurrencyStats> calculateStatsParallel(Map<String, List<Rate>> groupedRates) {
        return groupedRates.entrySet().parallelStream()
        .map(entry -> {
            String code = entry.getKey();
            List<Rate> rates = entry.getValue();
            DoubleSummaryStatistics stats = rates.stream().mapToDouble(Rate::mid).summaryStatistics();
            return new CurrencyStats(code, stats.getMax(), stats.getMin(), stats.getAverage());
        }).collect(Collectors.toConcurrentMap(CurrencyStats::code, stats -> stats));
    }
}