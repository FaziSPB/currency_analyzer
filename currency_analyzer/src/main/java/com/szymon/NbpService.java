package com.szymon;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

record Rate(String currency, String code, double mid, String date) {
    public Rate(String currency, String code, double mid) {
        this(currency, code, mid, null);
    }
}
record NbpTable(String table, String no, String effectiveDate, List<Rate> rates) {}
record CurrencyStats(String code, double avg, double min, String minDate, double max, String maxDate,
                     double stdDev, double changePercent) {}

public class NbpService {
    public static final String BASE_CURRENCY = "PLN";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    public Map<String, List<Rate>> getGroupedRates() throws IOException, InterruptedException {
        Map<String, List<Rate>> groupedRates = new TreeMap<>();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.nbp.pl/api/exchangerates/tables/a/last/30/?format=json"))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("NBP API returned HTTP " + response.statusCode());
        }
        NbpTable[] tables = mapper.readValue(response.body(), NbpTable[].class);
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
        return groupedRates;
    }
    //multithreading
    public Map<String, CurrencyStats> calculateStatsParallel(Map<String, List<Rate>> groupedRates) {
        return groupedRates.entrySet().parallelStream()
        .map(entry -> calculateStats(entry.getKey(), entry.getValue()))
        .collect(Collectors.toConcurrentMap(CurrencyStats::code, stats -> stats));
    }

    // rates are expected in chronological order (as returned by the NBP API)
    private CurrencyStats calculateStats(String code, List<Rate> rates) {
        double avg = rates.stream().mapToDouble(Rate::mid).average().orElse(0);
        Rate minRate = rates.stream().min(Comparator.comparingDouble(Rate::mid)).orElseThrow();
        Rate maxRate = rates.stream().max(Comparator.comparingDouble(Rate::mid)).orElseThrow();
        double variance = rates.stream().mapToDouble(rate -> Math.pow(rate.mid() - avg, 2)).average().orElse(0);
        double first = rates.get(0).mid();
        double last = rates.get(rates.size() - 1).mid();
        return new CurrencyStats(code, avg, minRate.mid(), minRate.date(), maxRate.mid(), maxRate.date(),
                Math.sqrt(variance), (last - first) / first * 100);
    }

    public List<Rate> movingAverage(List<Rate> rates, int window) {
        List<Rate> result = new ArrayList<>();
        for (int i = window - 1; i < rates.size(); i++) {
            double avg = rates.subList(i - window + 1, i + 1).stream().mapToDouble(Rate::mid).average().orElse(0);
            Rate rate = rates.get(i);
            result.add(new Rate(rate.currency(), rate.code(), avg, rate.date()));
        }
        return result;
    }

    // price of one unit of the currency in PLN, based on the most recent table
    public double latestRate(String code, Map<String, List<Rate>> groupedRates) {
        if (BASE_CURRENCY.equals(code)) {
            return 1.0;
        }
        List<Rate> rates = groupedRates.get(code);
        if (rates == null || rates.isEmpty()) {
            throw new IllegalArgumentException("Unknown currency: " + code);
        }
        return rates.get(rates.size() - 1).mid();
    }

    public double convert(double amount, String fromCode, String toCode, Map<String, List<Rate>> groupedRates) {
        return amount * latestRate(fromCode, groupedRates) / latestRate(toCode, groupedRates);
    }
}