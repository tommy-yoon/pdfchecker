package com.example.pdfchecker;

import com.itextpdf.text.pdf.*;
import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class OcgLayerCheckService {

    public OcgLayerCheckResult check(PdfReader reader) {
        OcgLayerCheckResult result = new OcgLayerCheckResult();
        try {
            PdfDictionary catalog = reader.getCatalog();
            PdfDictionary ocProps = catalog.getAsDict(PdfName.OCPROPERTIES);
            if (ocProps == null) {
                result.hasLayers = false;
                result.layerCount = 0;
                return result;
            }

            // 1) Enumerate /OCGs
            PdfArray ocgs = ocProps.getAsArray(PdfName.OCGS);
            if (ocgs != null) {
                result.layerCount = ocgs.size();
                result.hasLayers = result.layerCount > 0;

                for (int i = 0; i < ocgs.size(); i++) {
                    PdfDictionary ocg = ocgs.getAsDict(i);
                    if (ocg == null) continue;

                    LayerInfo li = new LayerInfo();
                    PdfString name = ocg.getAsString(PdfName.NAME);
                    if (name != null) li.name = name.toString();

                    PdfObject intent = ocg.get(PdfName.INTENT);
                    if (intent instanceof PdfName) {
                        li.intent = intent.toString();
                    } else if (intent instanceof PdfArray) {
                        PdfArray ia = (PdfArray) intent;
                        List<String> intents = new ArrayList<>();
                        for (int j = 0; j < ia.size(); j++) {
                            PdfName nm = ia.getAsName(j);
                            if (nm != null) intents.add(nm.toString());
                        }
                        li.intent = String.join(", ", intents);
                    }

                    li.ocgDict = ocg;
                    result.layers.add(li);
                }
            }

            // 2) Default config /D (BaseState, ON/OFF, Order, Locked)
            PdfName BASESTATE = new PdfName("BaseState");      // literal key name
            PdfDictionary d = ocProps.getAsDict(PdfName.D);
            if (d != null) {
                PdfName baseState = d.getAsName(BASESTATE);
                result.baseState = baseState != null ? baseState.toString() : null;

                Set<PdfDictionary> on = toDictSet(d.getAsArray(PdfName.ON));
                Set<PdfDictionary> off = toDictSet(d.getAsArray(PdfName.OFF));
                for (LayerInfo li : result.layers) {
                    if (li.ocgDict == null) continue;
                    if (on.contains(li.ocgDict)) li.defaultState = "ON";
                    else if (off.contains(li.ocgDict)) li.defaultState = "OFF";
                    else li.defaultState = result.baseState != null ? result.baseState.replace("/", "") : "ON";
                }

                result.hasCustomOrder = d.getAsArray(PdfName.ORDER) != null;
                PdfArray locked = d.getAsArray(PdfName.LOCKED);
                result.hasLockedLayers = locked != null && locked.size() > 0;
            }

            // 3) Per-page usage via /Resources/Properties mapping
            int n = reader.getNumberOfPages();
            for (int p = 1; p <= n; p++) {
                PdfDictionary page = reader.getPageN(p);
                PdfDictionary res = page.getAsDict(PdfName.RESOURCES);
                if (res == null) continue;
                PdfDictionary props = res.getAsDict(PdfName.PROPERTIES);
                if (props == null) continue;

                Set<String> names = new LinkedHashSet<>();
                for (PdfName key : props.getKeys()) {
                    PdfDictionary layerDict = props.getAsDict(key);
                    if (layerDict == null) continue;
                    PdfString name = layerDict.getAsString(PdfName.NAME);
                    if (name != null) names.add(name.toString());
                }
                if (!names.isEmpty()) result.pageLayerUsage.put(p, names);
            }

            return result;
        } catch (Exception e) {
            result.error = e.getMessage();
            return result;
        }
    }

    private Set<PdfDictionary> toDictSet(PdfArray arr) {
        Set<PdfDictionary> s = new HashSet<>();
        if (arr == null) return s;
        for (int i = 0; i < arr.size(); i++) {
            PdfDictionary d = arr.getAsDict(i);
            if (d != null) s.add(d);
        }
        return s;
    }
}
