package com.example.pdfchecker;

public class DiscrepancyResult {
    private final String fileName;
    private final int declaredCount;
    private final int actualKidsCount;
    private final boolean hasDiscrepancy;
    private final String errorMessage;

    public DiscrepancyResult(String fileName, int declaredCount, int actualKidsCount, 
                           boolean hasDiscrepancy) {
        this(fileName, declaredCount, actualKidsCount, hasDiscrepancy, null);
    }

    public DiscrepancyResult(String fileName, int declaredCount, int actualKidsCount, 
                           boolean hasDiscrepancy, String errorMessage) {
        this.fileName = fileName;
        this.declaredCount = declaredCount;
        this.actualKidsCount = actualKidsCount;
        this.hasDiscrepancy = hasDiscrepancy;
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        if (errorMessage != null) {
            return String.format("File: %s - %s", fileName, errorMessage);
        }
        
        if (hasDiscrepancy) {
            return String.format(
                "⚠ DISCREPANCY FOUND in '%s':\n" +
                "   Declared /Count: %d pages\n" +
                "   Actual Kids count: %d pages\n" +
                "   Difference: %d page(s)",
                fileName, declaredCount, actualKidsCount, 
                Math.abs(actualKidsCount - declaredCount)
            );
        } else {
            return String.format(
                "✓ NO DISCREPANCY in '%s': Both counts match at %d pages",
                fileName, declaredCount
            );
        }
    }

    // Getters
    public String getFileName() { return fileName; }
    public int getDeclaredCount() { return declaredCount; }
    public int getActualKidsCount() { return actualKidsCount; }
    public boolean hasDiscrepancy() { return hasDiscrepancy; }
    public String getErrorMessage() { return errorMessage; }
}
