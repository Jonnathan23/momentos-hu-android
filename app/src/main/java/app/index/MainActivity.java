package app.index;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Bitmap;
import app.index.databinding.ActivityMainBinding;
import android.util.Log;
import java.util.Arrays;


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
            // 1) Capturar bitmap
            Bitmap dibujo = areaDibujo.getBitmap();

            // 2) Llamar a la función nativa
            float[] descriptor = computeHuDescriptor(dibujo);
            Log.i("DEBUG", "Descriptor usuario: " + Arrays.toString(descriptor));

            // 3) Clasificar con el método de vecino más cercano
            String etiqueta = classifyDescriptor(descriptor);

            // 4) Mostrar la etiqueta en el TextView
            binding.textFigure.setText(etiqueta);
        });
    }

    public native float[] computeHuDescriptor(Bitmap bitmap);

    private String classifyDescriptor(float[] descriptor) {
        String[] clases = {"circle","square","triangle"};
        String mejor = null;
        double mejorDist = Double.MAX_VALUE;

        for (String clase : clases) {
            float[] huRef = DescriptorUtils.getHuMoments(clase);
            double dist = 0;
            for (int i = 0; i < descriptor.length; i++) {
                double d = descriptor[i] - huRef[i];
                dist += d * d;
            }
            dist = Math.sqrt(dist);

            Log.i("DEBUG", String.format("Clase=%s, dist=%.4f", clase, dist));

            if (dist < mejorDist) {
                mejorDist = dist;
                mejor = clase;
            }
        }

        // Devolvemos siempre la mejor, sin threshold
        return mejor != null ? mejor : "desconocido";
    }

}