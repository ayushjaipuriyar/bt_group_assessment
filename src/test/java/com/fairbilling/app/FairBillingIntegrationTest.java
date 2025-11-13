package com.fairbilling.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.Test;

public class FairBillingIntegrationTest {

    @Test
    public void endToEndProcessing() throws IOException {
        Path tempFile = Files.createTempFile("fair-billing", ".log");
        java.util.List<String> lines = Arrays.asList(
                "14:02:03 ALICE99 Start",
                "14:02:05 CHARLIE End",
                "14:02:34 ALICE99 End",
                "14:02:58 ALICE99 Start",
                "14:03:02 CHARLIE Start",
                "14:03:33 ALICE99 Start",
                "14:03:35 ALICE99 End",
                "14:03:37 CHARLIE End",
                "14:04:05 ALICE99 End",
                "14:04:23 ALICE99 End",
                "14:04:41 CHARLIE Start");
        Files.write(tempFile, lines);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        FairBilling application = new FairBilling();

        int exitCode = application.run(tempFile.toString(), new PrintStream(output), new PrintStream(errors));

        assertEquals(0, exitCode);
        String[] resultLines = output.toString().trim().split(System.lineSeparator());
        assertEquals(2, resultLines.length);
        assertEquals("ALICE99 4 240", resultLines[0]);
        assertEquals("CHARLIE 3 37", resultLines[1]);
        assertTrue(errors.toString().isEmpty());
    }

    @Test
    public void reportsMissingFile() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        FairBilling application = new FairBilling();

        int exitCode = application.run("/path/to/missing.log", new PrintStream(output), new PrintStream(errors));

        assertEquals(1, exitCode);
        assertTrue(output.toString().isEmpty());
        assertTrue(errors.toString().contains("Error: File not found"));
    }
}
