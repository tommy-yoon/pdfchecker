package com.example.pdfchecker;

import java.util.List;

public class CopyOperationResult {
    private final String fileName;
    private final int totalPages;
    private final int problematicPage;
    private final boolean hasMismatch;
    private final List<PdfDiscrepancyChecker.PageStateSnapshot> snapshots;
    private final String message;

    public CopyOperationResult(String fileName, int totalPages, int problematicPage, 
                              boolean hasMismatch, 
                              List<PdfDiscrepancyChecker.PageStateSnapshot> snapshots,
                              String message) {
        this.fileName = fileName;
        this.totalPages = totalPages;
        this.problematicPage = problematicPage;
        this.hasMismatch = hasMismatch;
        this.snapshots = snapshots;
        this.message = message;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (hasMismatch) {
            sb.append(String.format("⚠ PDFCOPY MISMATCH in '%s':\n", fileName));
            sb.append(String.format("   Total pages: %d\n", totalPages));
            sb.append(String.format("   Problematic page: %d\n", problematicPage));
            sb.append(String.format("   Message: %s\n\n", message));
            
            sb.append("   Internal State Timeline:\n");
            sb.append("   " + "-".repeat(85) + "\n");
            
            // Show snapshots for the problematic page and the page before
            int startIndex = Math.max(0, (problematicPage - 2) * 5);
            int endIndex = Math.min(snapshots.size(), problematicPage * 5);
            
            for (int i = startIndex; i < endIndex; i++) {
                PdfDiscrepancyChecker.PageStateSnapshot snapshot = snapshots.get(i);
                sb.append("   ").append(snapshot.toString());
                
                // Highlight mismatch
                int diff = snapshot.getCurrentPageNumber() - 1 - snapshot.getPageReferencesSize();
                if (diff != 0) {
                    sb.append(" ⚠ DIFF: ").append(diff);
                }
                sb.append("\n");
            }
        } else {
            sb.append(String.format("✓ NO PDFCOPY MISMATCH in '%s'\n", fileName));
            sb.append(String.format("   Total pages processed: %d\n", totalPages));
            sb.append(String.format("   Message: %s\n", message));
        }
        
        return sb.toString();
    }

    // Getters
    public String getFileName() { return fileName; }
    public int getTotalPages() { return totalPages; }
    public int getProblematicPage() { return problematicPage; }
    public boolean hasMismatch() { return hasMismatch; }
    public List<PdfDiscrepancyChecker.PageStateSnapshot> getSnapshots() { return snapshots; }
    public String getMessage() { return message; }
}
