package com.fairbilling.report;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Objects;

import com.fairbilling.domain.UserBillingSummary;

/**
 * Produces human-readable billing reports.
 */
public class ReportPrinter {

    public void print(Collection<UserBillingSummary> summaries, PrintStream output) {
        Objects.requireNonNull(summaries, "summaries");
        Objects.requireNonNull(output, "output");

        for (UserBillingSummary summary : summaries) {
            output.printf("%s %d %d%n",
                    summary.getUsername(),
                    summary.getSessionCount(),
                    summary.getTotalDurationSeconds());
        }
    }
}
