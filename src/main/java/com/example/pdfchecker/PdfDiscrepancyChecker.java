package com.example.pdfchecker;

import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

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
}
