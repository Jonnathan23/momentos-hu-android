package app.index;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class DescriptorUtils {
    private static final Map<String, float[]> huMomentMap = new HashMap<>();
    private static final Map<String, float[]> shapeSignatureMap = new HashMap<>();
    private static final Map<String, Float> classThresholdMap = new HashMap<>();
    private static boolean initialized = false;

    private DescriptorUtils() {
        // Evita instanciación
    }

    /**
     * @description Inicia la carga de los CSVs de descriptores y establece los umbrales.
     * Es idempotente: solo carga la primera vez.
     */
    public static void init(AssetManager assets) {
        if (initialized) return;
        // Cargar Hu moments y Shape Signatures, saltando columna de imagen
        readDescriptorCsv(assets, "hu_moments.csv", huMomentMap);
        readDescriptorCsv(assets, "shape_signature.csv", shapeSignatureMap);
        // Umbrales normalizados (constantes en código)
        classThresholdMap.put("circle",   1.3028271f);
        classThresholdMap.put("square",   1.6212168f);
        classThresholdMap.put("triangle", 1.7396645f);
        initialized = true;
    }

    /**
     * @description Lee un CSV donde la primera columna es la etiqueta y la segunda es el nombre de imagen,
     * y las columnas siguientes son valores numéricos.
     * Se salta la cabecera y la columna de imagen.
     */
    private static void readDescriptorCsv(AssetManager assets, String fileName, Map<String, float[]> map) {
        try (InputStream is = assets.open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {  // saltamos header
                    isFirstLine = false;
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 3) continue;
                String label = parts[0];
                int count = parts.length - 2;  // omitimos parts[1]
                float[] values = new float[count];
                for (int i = 2; i < parts.length; i++) {
                    try {
                        values[i - 2] = Float.parseFloat(parts[i]);
                    } catch (NumberFormatException nfe) {
                        Log.w("DescriptorUtils", "Valor no numérico en " + fileName + ": " + parts[i], nfe);
                        values[i - 2] = 0f;
                    }
                }
                map.put(label, values);
            }
        } catch (IOException e) {
            Log.e("DescriptorUtils", "Error cargando " + fileName, e);
        }
    }

    public static float[] getHuMoments(String label) {
        ensureInit();
        return huMomentMap.get(label);
    }

    public static float[] getShapeSignature(String label) {
        ensureInit();
        return shapeSignatureMap.get(label);
    }

    public static float getClassThreshold(String label) {
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
