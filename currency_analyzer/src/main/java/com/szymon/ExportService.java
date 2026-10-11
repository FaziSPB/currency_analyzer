package com.szymon;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExportService {
    public void exportCsv(File file, List<Rate> rates, CurrencyStats stats) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("date,code,mid");
        for (Rate rate : rates) {
            lines.add(rate.date() + "," + rate.code() + "," + rate.mid());
        }
        if (stats != null) {
            lines.add("");
            lines.add("stat,value");
            lines.add(String.format(Locale.ROOT, "average,%.6f", stats.avg()));
            lines.add(String.format(Locale.ROOT, "min,%.6f", stats.min()));
            lines.add("min_date," + stats.minDate());
            lines.add(String.format(Locale.ROOT, "max,%.6f", stats.max()));
            lines.add("max_date," + stats.maxDate());
            lines.add(String.format(Locale.ROOT, "std_dev,%.6f", stats.stdDev()));
            lines.add(String.format(Locale.ROOT, "change_percent,%.2f", stats.changePercent()));
        }
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    // must be called on the JavaFX Application Thread
    public void exportPng(File file, Node node) throws IOException {
        WritableImage image = node.snapshot(null, null);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
    }
}
