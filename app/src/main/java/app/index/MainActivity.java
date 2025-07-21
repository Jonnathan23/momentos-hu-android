package app.index;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import app.index.databinding.ActivityMainBinding;


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

        // Area de dibujo
        DrawingView areaDibujo = binding.drawingView;

        binding.buttonClassify.setOnClickListener(view -> {
            // Por ahora solo probamos que se capture el bitmap
            Bitmap dibujo = areaDibujo.getBitmap();
            // TODO: lo convertibles a Mat y llamaremos classifyShape(...)
        });
    }

    /**
     * A native method that is implemented by the 'index' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}