package app.index;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Bitmap;
import app.index.databinding.ActivityMainBinding;
import android.util.Log;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'index' library on application startup.
    static {
        System.loadLibrary("index");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DescriptorUtils.init(getAssets());

        // Area de dibujo
        DrawingView areaDibujo = binding.drawingView;

        binding.buttonClassify.setOnClickListener(view -> {
            // 1) Capturar bitmap
            Bitmap bmp = binding.drawingView.getBitmap();

            float[] huDesc  = computeHuDescriptor(bmp);
            float[] sigDesc = computeShapeSignature(bmp);

            String etiqueta = classifyTwoStage(huDesc, sigDesc);
            binding.textFigure.setText(etiqueta);
        });

        binding.buttonClear.setOnClickListener(v -> {
            // Limpia la zona de dibujo
            binding.drawingView.clear();
            binding.textFigure.setText("");
        });
    }

    /** Stage 1: Hu para “circle” vs “other”; Stage 2: Signature para square vs triangle */
    private String classifyTwoStage(float[] huDesc, float[] sigDesc) {
        // 1) Hu-distance a círculos
        // 0) Stage 0: detectamos círculo por uniformidad de la firma
        if (isCircleBySignature(sigDesc)) {
            Log.i("DEBUG", "Detected circle by signature uniformity");
            return "circle";
        }

        // 2) Stage 2: polígonos → square vs triangle usando la signature
        double minSigSq  = minDistance(sigDesc, DescriptorUtils.getSignatureList("square"));
        double minSigTri = minDistance(sigDesc, DescriptorUtils.getSignatureList("triangle"));
        String polyClass = (minSigSq < minSigTri) ? "square" : "triangle";
        Log.i("DEBUG", String.format("Sig dist → square=%.1f, triangle=%.1f", minSigSq, minSigTri));
        return polyClass;
    }

    // Distancia mínima de usuario→lista de refs
    private double minDistance(float[] user, List<float[]> refs) {
        double best = Double.MAX_VALUE;
        for (float[] ref : refs) {
            double s = 0;
            for (int i = 0; i < user.length; i++) {
                double d = user[i] - ref[i];
                s += d*d;
            }
            best = Math.min(best, Math.sqrt(s));
        }
        return best;
    }

    /**
     * Un círculo perfecto tendrá todos los samples de la signature casi iguales.
     * Calculamos (max-min)/max y lo comparamos contra un pequeño umbral.
     */
    private boolean isCircleBySignature(float[] sigDesc) {
        if (sigDesc == null || sigDesc.length == 0) return false;
        float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
        for (float v : sigDesc) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        // Si la variación relativa es < 10%, lo consideramos un círculo
        return (max - min) / max < 0.10f;
    }

    // Declaración de JNI
    public native float[] computeHuDescriptor(Bitmap bitmap);
    public native float[] computeShapeSignature(Bitmap bitmap);
}