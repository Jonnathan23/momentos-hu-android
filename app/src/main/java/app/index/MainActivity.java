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
        double minHuCir = minDistance(huDesc, DescriptorUtils.getHuList("circle"));
        if (minHuCir <= DescriptorUtils.getThreshold("circle")) {
            return "circle";
        }

        // 2) Ahora polígonos: Signature
        double minSigSq  = minDistance(sigDesc, DescriptorUtils.getSignatureList("square"));
        double minSigTri = minDistance(sigDesc, DescriptorUtils.getSignatureList("triangle"));
        return (minSigSq < minSigTri) ? "square" : "triangle";
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

    // Declaración de JNI
    public native float[] computeHuDescriptor(Bitmap bitmap);
    public native float[] computeShapeSignature(Bitmap bitmap);
}