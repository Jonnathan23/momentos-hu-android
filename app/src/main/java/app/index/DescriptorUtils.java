package app.index;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DescriptorUtils {
    private static final Map<String, List<float[]>> huMomentList = new HashMap<>();
    private static final Map<String, List<float[]>> shapeSignatureList = new HashMap<>();
    private static final Map<String, Float> classThresholdMap = new HashMap<>();
    private static boolean initialized = false;

    private DescriptorUtils() {
        // No instanciable
    }

    /**
     * Inicializa cargando todos los CSV y umbrales.
     * Idempotente: solo carga la primera vez.
     */
    public static void init(AssetManager assets) {
        if (initialized) return;

        // Carga listas completas de descriptores
        huMomentList.putAll(readAll(assets, "hu_moments.csv"));
        shapeSignatureList.putAll(readAll(assets, "shape_signature.csv"));

        // Umbrales precomputados (desde script externo)
        classThresholdMap.put("circle",   1.3028271f);
        classThresholdMap.put("square",   1.6212168f);
        classThresholdMap.put("triangle", 1.7396645f);

        initialized = true;
    }

    /**
     * Lee un CSV completo y devuelve un mapa label → lista de vectores.
     * Se salta cabecera y columna de imagen, e ignora valores no numéricos.
     */
    private static Map<String, List<float[]>> readAll(
            AssetManager assets, String fileName) {
        Map<String, List<float[]>> temp = new HashMap<>();
        try (InputStream is = assets.open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] parts = line.split(",");
                if (parts.length < 3) continue;
                String label = parts[0];
                int dim = parts.length - 2; // omitimos etiqueta e imagen
                float[] vals = new float[dim];
                for (int i = 2; i < parts.length; i++) {
                    try {
                        vals[i - 2] = Float.parseFloat(parts[i]);
                    } catch (NumberFormatException nfe) {
                        Log.w("DescriptorUtils",
                                "Valor no numérico en " + fileName + ": " + parts[i]);
                        vals[i - 2] = 0f;
                    }
                }
                temp.computeIfAbsent(label, k -> new ArrayList<>()).add(vals);
            }
        } catch (IOException e) {
            Log.e("DescriptorUtils", "Error leyendo " + fileName, e);
        }
        return temp;
    }

    public static List<float[]> getHuList(String label) {
        ensureInit();
        return huMomentList.getOrDefault(label, Collections.emptyList());
    }

    public static List<float[]> getSignatureList(String label) {
        ensureInit();
        return shapeSignatureList.getOrDefault(label, Collections.emptyList());
    }

    public static float getThreshold(String label) {
        ensureInit();
        Float t = classThresholdMap.get(label);
        return t != null ? t : Float.NaN;
    }

    private static void ensureInit() {
        if (!initialized) {
            throw new IllegalStateException("DescriptorUtils not initialized. Call init() first.");
        }
    }
}
