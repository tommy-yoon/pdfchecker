package com.example.pdfchecker;

import com.itextpdf.text.pdf.PdfDictionary;

public class LayerInfo {
    public String name;           // /Name
    public String intent;         // /Intent
    public String defaultState;   // "ON" | "OFF"
    transient PdfDictionary ocgDict; // internal ref used while resolving states
}
