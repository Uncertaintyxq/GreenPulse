package com.greenpulse.service;

import com.greenpulse.model.CollectionRoute;
import com.greenpulse.model.RecyclingBin;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportExporterTest {

    @Test
    void csvQuotesAndNeutralisesFormulas() {
        assertEquals("\"Plaza \"\"North\"\"\"", ReportExporter.csv("Plaza \"North\""));
        assertEquals("\"'=SUM(A1)\"", ReportExporter.csv("=SUM(A1)"));
    }

    @Test
    void htmlEscapesMarkup() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", ReportExporter.html("<script>alert(1)</script>"));
    }

    @Test
    void manifestIsWrittenWithoutRawMarkupFromLocations() throws Exception {
        RecyclingBin bin = new RecyclingBin("B1", 120, 70, "<b>Hall</b>", 10);
        CollectionRoute route = new CollectionRoute("RT-1", List.of(bin), 4.5, 22, "Carlos", "EcoTruck-01");
        Path file = Files.createTempFile("manifest", ".html");
        try {
            ReportExporter.exportPrintableReportHTML(route, file);
            String html = Files.readString(file, StandardCharsets.UTF_8);
            assertTrue(html.contains("&lt;b&gt;Hall&lt;/b&gt;"));
            assertFalse(html.contains("<b>Hall</b>"));
            assertTrue(html.contains("4.50 km"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
