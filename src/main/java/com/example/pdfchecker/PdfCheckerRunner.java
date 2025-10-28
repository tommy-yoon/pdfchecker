package com.example.pdfchecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfCheckerRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PdfCheckerRunner.class);
    private final PdfDiscrepancyChecker checker;

    public PdfCheckerRunner(PdfDiscrepancyChecker checker) {
        this.checker = checker;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== PDF Page Count Discrepancy Checker ===\n");
        
        if (args.length == 0) {
            logger.error("Usage: java -jar pdf-checker.jar <pdf-file-path> [<pdf-file-path2> ...]");
            logger.error("Example: java -jar pdf-checker.jar /path/to/document.pdf");
            return;
        }

        List<DiscrepancyResult> results = new ArrayList<>();
        
        for (String filePath : args) {
            File pdfFile = new File(filePath);
            
            if (!pdfFile.exists()) {
                logger.error("File not found: {}", filePath);
                continue;
            }
            
            if (!pdfFile.getName().toLowerCase().endsWith(".pdf")) {
                logger.warn("Skipping non-PDF file: {}", filePath);
                continue;
            }
            
            logger.info("Checking: {}", pdfFile.getName());
            DiscrepancyResult result = checker.checkPdfDiscrepancy(pdfFile);
            results.add(result);
            System.out.println(result);
            System.out.println();
        }
        
        // Summary
        long discrepancyCount = results.stream()
            .filter(DiscrepancyResult::hasDiscrepancy)
            .count();
        
        logger.info("=== Summary ===");
        logger.info("Total files checked: {}", results.size());
        logger.info("Files with discrepancy: {}", discrepancyCount);
        logger.info("Files without discrepancy: {}", results.size() - discrepancyCount);
    }
}
