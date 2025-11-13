package com.fairbilling.app;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.fairbilling.domain.LogEntry;
import com.fairbilling.domain.UserBillingSummary;
import com.fairbilling.io.LogFileParser;
import com.fairbilling.report.ReportPrinter;
import com.fairbilling.service.BillingCalculator;

/**
 * Application entry point for the Fair Billing solution.
 */
public final class FairBilling {

    private final LogFileParser logFileParser;
    private final BillingCalculator billingCalculator;
    private final ReportPrinter reportPrinter;

    public FairBilling() {
        this(new LogFileParser(), new BillingCalculator(), new ReportPrinter());
    }

    FairBilling(LogFileParser logFileParser,
            BillingCalculator billingCalculator,
            ReportPrinter reportPrinter) {
        this.logFileParser = Objects.requireNonNull(logFileParser, "logFileParser");
        this.billingCalculator = Objects.requireNonNull(billingCalculator, "billingCalculator");
        this.reportPrinter = Objects.requireNonNull(reportPrinter, "reportPrinter");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java com.fairbilling.app.FairBilling <log_file_path>");
            System.exit(1);
        }

        FairBilling application = new FairBilling();
        int exitCode = application.run(args[0], System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String filePath, PrintStream output, PrintStream error) {
        Objects.requireNonNull(filePath, "filePath");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(error, "error");

        Path path = Paths.get(filePath);
        try {
            List<LogEntry> entries = logFileParser.parse(path);
            Collection<UserBillingSummary> summaries = billingCalculator.calculate(entries);
            reportPrinter.print(summaries, output);
            return 0;
        } catch (NoSuchFileException e) {
            error.println("Error: File not found: " + path);
            return 1;
        } catch (IOException e) {
            error.println("Error: Unable to read file: " + path);
            return 1;
        }
    }
}
