package com.example.ecommerce.product;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ProductCsvParser {
    public List<ParseResult> parse(MultipartFile file) {
        List<ParseResult> rows = new ArrayList<>();
        forEach(file, rows::add);
        return rows;
    }

    public void forEach(MultipartFile file, Consumer<ParseResult> consumer) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true).setTrim(true).get().parse(reader)) {
            for (CSVRecord row : parser) {
                 int rowNumber = (int) row.getRecordNumber() + 1;
                 String sku = value(row, "sku");
                 try {
                     consumer.accept(ParseResult.success(rowNumber, parseRow(row)));
                 } catch (RuntimeException ex) {
                     consumer.accept(ParseResult.failure(rowNumber, sku, ex));
                 }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to parse CSV: " + ex.getMessage(), ex);
        }
    }

    private ParsedRow parseRow(CSVRecord row) {
        return new ParsedRow(required(row, "sku"), required(row, "name"),
                required(row, "description"),
                required(row, "category"), decimal(row, "price"), integer(row, "stock"),
                decimal(row, "weight_kg"));
    }

    private static String value(CSVRecord row, String column) {
        return row.isMapped(column) ? row.get(column).trim() : "";
    }

    private static String required(CSVRecord row, String column) {
        String value = value(row, column);
        if (value.isBlank()) {
            throw new IllegalArgumentException(column + " is required");
        }
        return value;
    }

    private static BigDecimal decimal(CSVRecord row, String column) {
        String value = required(row, column);
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.signum() < 0) {
                throw new IllegalArgumentException(column + " must not be negative");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(column + " must be a decimal number; received '" + value + "'");
        }
    }

    private static int integer(CSVRecord row, String column) {
        String value = required(row, column);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(column + " must not be negative");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(column + " must be an integer; received '" + value + "'");
        }
    }

    public record ParsedRow(String sku, String name, String description, String category,
                            BigDecimal price, int stock, BigDecimal weightKg) {
    }

    public record ParseResult(int rowNumber, String sku, ParsedRow row, RuntimeException error) {
        static ParseResult success(int rowNumber, ParsedRow row) {
            return new ParseResult(rowNumber, row.sku(), row, null);
        }

        static ParseResult failure(int rowNumber, String sku, RuntimeException error) {
            return new ParseResult(rowNumber, sku, null, error);
        }
    }
}
