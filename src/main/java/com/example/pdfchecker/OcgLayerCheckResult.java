package com.example.pdfchecker;

public class OcgLayerCheckResult {
    public boolean hasLayers;
    public int layerCount;
    public String baseState;              // e.g., "/ON", "/OFF", or null
    public boolean hasCustomOrder;        // /D /Order present
    public boolean hasLockedLayers;       // /D /Locked present
    public java.util.List<LayerInfo> layers = new java.util.ArrayList<>();
    public java.util.Map<Integer, java.util.Set<String>> pageLayerUsage = new java.util.HashMap<>();
    public String error;                  // non-null if check failed
}
