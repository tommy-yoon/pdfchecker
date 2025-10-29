package com.example.pdfchecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.itextpdf.text.pdf.PdfReader;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfCheckerRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PdfCheckerRunner.class);
    private final PdfDiscrepancyChecker checker;
    private final OcgLayerCheckService ocgLayerCheckService;

    public PdfCheckerRunner(PdfDiscrepancyChecker checker, OcgLayerCheckService ocgLayerCheckService) {
        this.checker = checker;
        this.ocgLayerCheckService = ocgLayerCheckService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== PDF Page Count Discrepancy Checker ===\n");
        
        if (args.length == 0) {
            logger.error("Usage: java -jar pdf-checker.jar <pdf-file-path> [<pdf-file-path2> ...]");
            logger.error("Example: java -jar pdf-checker.jar /path/to/document.pdf");
            return;
        }

        List<DiscrepancyResult> structureResults = new ArrayList<>();
        List<CopyOperationResult> copyResults = new ArrayList<>();
        
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
            
            logger.info("Checking: {}\n", pdfFile.getName());
            
            // Check 1: Page tree structure discrepancy
            logger.info("--- Check 1: Page Tree Structure ---");
            DiscrepancyResult structureResult = checker.checkPdfDiscrepancy(pdfFile);
            structureResults.add(structureResult);
            System.out.println(structureResult);
            System.out.println();
            
            // Check 2: PdfCopy internal state tracking
            logger.info("--- Check 2: PdfCopy Internal State Tracking ---");
            CopyOperationResult copyResult = checker.checkPdfCopyOperation(pdfFile);
            copyResults.add(copyResult);
            System.out.println(copyResult);
            System.out.println();

            // Check 3: OCG layer
            try (FileInputStream fis = new FileInputStream(pdfFile)) {
                PdfReader reader = new PdfReader(fis);
                OcgLayerCheckResult ocg = ocgLayerCheckService.check(reader);
                reader.close();

                System.out.println();
                System.out.println("=== Check 3: OCG layers ===");
                if (ocg.error != null) {
                    System.out.println("OCG check error: " + ocg.error);
                } else if (!ocg.hasLayers) {
                    System.out.println("Has OCG layers: NO");
                } else {
                    System.out.println("Has OCG layers: YES");
                    System.out.println("Layer count: " + ocg.layerCount);
                    System.out.println("Base state: " + (ocg.baseState != null ? ocg.baseState : "Default (ON)"));
                    System.out.println("Custom order: " + (ocg.hasCustomOrder ? "YES" : "NO"));
                    System.out.println("Locked layers: " + (ocg.hasLockedLayers ? "YES" : "NO"));

                    if (!ocg.layers.isEmpty()) {
                        System.out.println("Layers:");
                        for (LayerInfo li : ocg.layers) {
                            String nm = li.name != null ? li.name : "(Unnamed)";
                            String st = li.defaultState != null ? li.defaultState : "?";
                            String in = li.intent != null ? (" intent=" + li.intent) : "";
                            System.out.println("  • " + nm + " [" + st + "]" + in);
                        }
                    }
                    if (!ocg.pageLayerUsage.isEmpty()) {
                        System.out.println("Pages using layers:");
                        ocg.pageLayerUsage.forEach((p, set) ->
                            System.out.println("  Page " + p + ": " + String.join(", ", set)));
                    }
                    System.out.println("WARNING: This PDF contains OCG layers; stamping overlays or merging with OCG-based page numbers can conflict with existing layers.");
                }
            } catch (Exception ex) {
                System.out.println("OCG check failed: " + ex.getMessage());
            }

            logger.info("=".repeat(90) + "\n");
        }
        
        // Summary
        long structureDiscrepancyCount = structureResults.stream()
            .filter(DiscrepancyResult::hasDiscrepancy)
            .count();
            
        long copyMismatchCount = copyResults.stream()
            .filter(CopyOperationResult::hasMismatch)
            .count();
        
        logger.info("=== Summary ===");
        logger.info("Total files checked: {}", structureResults.size());
        logger.info("Files with page tree discrepancy: {}", structureDiscrepancyCount);
        logger.info("Files with PdfCopy state mismatch: {}", copyMismatchCount);
        logger.info("Files without issues: {}", 
                   structureResults.size() - structureDiscrepancyCount - copyMismatchCount);
    }
}
