package com.example.pdfchecker;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.PdfCopy.PageStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

@Service
public class PdfDiscrepancyChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfDiscrepancyChecker.class);

    /**
     * Check if a PDF file has a discrepancy between declared /Count and actual Kids array
     */
    public DiscrepancyResult checkPdfDiscrepancy(File pdfFile) {
        PdfReader reader = null;
        try {
            reader = new PdfReader(pdfFile.getAbsolutePath());
            
            // Get declared page count from getNumberOfPages()
            int declaredCount = reader.getNumberOfPages();
            
            // Get actual Kids array count from the page tree
            PdfDictionary catalog = reader.getCatalog();
            PdfDictionary pages = catalog.getAsDict(PdfName.PAGES);
            
            int actualKidsCount = countAllPageNodesRecursively(pages);
            
            boolean hasDiscrepancy = declaredCount != actualKidsCount;
            
            return new DiscrepancyResult(
                pdfFile.getName(),
                declaredCount,
                actualKidsCount,
                hasDiscrepancy
            );
            
        } catch (IOException e) {
            logger.error("Error reading PDF file: {}", pdfFile.getName(), e);
            return new DiscrepancyResult(
                pdfFile.getName(),
                -1,
                -1,
                false,
                "Error: " + e.getMessage()
            );
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * NEW: Check for PdfCopy/PdfSmartCopy internal state issues
     * This simulates the merge operation and tracks internal counters
     */
    public CopyOperationResult checkPdfCopyOperation(File pdfFile) {
        PdfReader reader = null;
        Document document = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            reader = new PdfReader(pdfFile.getAbsolutePath());
            int numberOfPages = reader.getNumberOfPages();
            
            logger.info("Simulating PdfSmartCopy operation for: {}", pdfFile.getName());
            logger.info("Number of pages: {}", numberOfPages);
            
            document = new Document();
            PdfSmartCopy copy = new PdfSmartCopy(document, baos);
            document.open();
            
            ArrayList<PageStateSnapshot> snapshots = new ArrayList<>();
            
            for (int i = 1; i <= numberOfPages; i++) {
                PageStateSnapshot beforeImport = captureInternalState(copy, "Before getImportedPage " + i);
                
                PdfImportedPage page = copy.getImportedPage(reader, i);
                
                PageStateSnapshot afterImport = captureInternalState(copy, "After getImportedPage " + i);
                
                // Simulate PageStamp operation (like in your main code)
                PageStamp stamp = copy.createPageStamp(page);
                
                PageStateSnapshot afterCreateStamp = captureInternalState(copy, "After createPageStamp " + i);
                
                // Add some dummy content (simulating your page number addition)
                PdfContentByte content = stamp.getOverContent();
                content.beginText();
                content.setFontAndSize(BaseFont.createFont(), 10);
                content.setTextMatrix(50, 50);
                content.showText("Page " + i);
                content.endText();
                
                stamp.alterContents();
                
                PageStateSnapshot afterAlterContents = captureInternalState(copy, "After alterContents " + i);
                
                copy.addPage(page);
                
                PageStateSnapshot afterAddPage = captureInternalState(copy, "After addPage " + i);
                
                // Store snapshots for this page
                snapshots.add(beforeImport);
                snapshots.add(afterImport);
                snapshots.add(afterCreateStamp);
                snapshots.add(afterAlterContents);
                snapshots.add(afterAddPage);
                
                // Check for mismatch
                if (afterAlterContents.currentPageNumber - 1 != afterAlterContents.pageReferencesSize) {
                    logger.warn("⚠ MISMATCH DETECTED at page {}", i);
                    logger.warn("  currentPageNumber: {}", afterAlterContents.currentPageNumber);
                    logger.warn("  pageReferences.size(): {}", afterAlterContents.pageReferencesSize);
                    
                    return new CopyOperationResult(
                        pdfFile.getName(),
                        numberOfPages,
                        i,
                        true,
                        snapshots,
                        "Mismatch at page " + i + ": currentPageNumber=" + 
                        afterAlterContents.currentPageNumber + ", pageReferences.size()=" + 
                        afterAlterContents.pageReferencesSize
                    );
                }
            }
            
            document.close();
            
            logger.info("✓ No internal state mismatch detected");
            return new CopyOperationResult(
                pdfFile.getName(),
                numberOfPages,
                -1,
                false,
                snapshots,
                "No issues detected"
            );
            
        } catch (Exception e) {
            logger.error("Error during PdfCopy simulation: {}", e.getMessage(), e);
            return new CopyOperationResult(
                pdfFile.getName(),
                -1,
                -1,
                true,
                new ArrayList<>(),
                "Exception: " + e.getMessage()
            );
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * Capture internal state of PdfWriter using reflection
     */
    private PageStateSnapshot captureInternalState(PdfSmartCopy copy, String label) {
        try {
            Field pageReferencesField = PdfWriter.class.getDeclaredField("pageReferences");
            pageReferencesField.setAccessible(true);
            ArrayList<?> pageReferences = (ArrayList<?>) pageReferencesField.get(copy);
            
            Field currentPageNumberField = PdfWriter.class.getDeclaredField("currentPageNumber");
            currentPageNumberField.setAccessible(true);
            int currentPageNumber = currentPageNumberField.getInt(copy);
            
            return new PageStateSnapshot(
                label,
                currentPageNumber,
                pageReferences.size()
            );
            
        } catch (Exception e) {
            logger.error("Failed to capture internal state", e);
            return new PageStateSnapshot(label, -1, -1);
        }
    }

    /**
     * Recursively count all page nodes in the page tree
     */
    private int countAllPageNodesRecursively(PdfDictionary pageTreeNode) {
        if (pageTreeNode == null) {
            return 0;
        }
        
        PdfArray kids = pageTreeNode.getAsArray(PdfName.KIDS);
        if (kids == null) {
            return 0;
        }
        
        int count = 0;
        for (int i = 0; i < kids.size(); i++) {
            PdfDictionary kid = kids.getAsDict(i);
            if (kid == null) {
                continue;
            }
            
            PdfName type = kid.getAsName(PdfName.TYPE);
            if (PdfName.PAGES.equals(type)) {
                // It's an intermediate node, recurse
                count += countAllPageNodesRecursively(kid);
            } else if (PdfName.PAGE.equals(type)) {
                // It's a leaf (actual page)
                count++;
            }
        }
        return count;
    }
    
    /**
     * Inner class to capture state at a point in time
     */
    public static class PageStateSnapshot {
        private final String label;
        private final int currentPageNumber;
        private final int pageReferencesSize;
        
        public PageStateSnapshot(String label, int currentPageNumber, int pageReferencesSize) {
            this.label = label;
            this.currentPageNumber = currentPageNumber;
            this.pageReferencesSize = pageReferencesSize;
        }
        
        public String getLabel() { return label; }
        public int getCurrentPageNumber() { return currentPageNumber; }
        public int getPageReferencesSize() { return pageReferencesSize; }
        
        @Override
        public String toString() {
            return String.format("%-35s | currentPageNumber: %-3d | pageReferences.size(): %-3d", 
                               label, currentPageNumber, pageReferencesSize);
        }
    }
}
