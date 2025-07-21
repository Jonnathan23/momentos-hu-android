package app.index;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawingView extends View {
    private final Paint pincel = new Paint();
    private final Path trazo = new Path();

    // 1) Constructor obligatorio para inflar desde XML
    public DrawingView(Context contexto, AttributeSet atributos) {
        super(contexto, atributos);
        inicializar();
    }

    // 2) Configuración inicial del pincel y fondo
    private void inicializar() {
        pincel.setColor(Color.BLACK);
        pincel.setStyle(Paint.Style.STROKE);
        pincel.setStrokeWidth(1f);
        setBackgroundColor(Color.WHITE);
    }

    // 3) Dibujar en pantalla lo que el usuario ha trazado
    @Override
    protected void onDraw(Canvas lienzo) {
        super.onDraw(lienzo);
        lienzo.drawPath(trazo, pincel);
    }

    // 4) Capturar eventos táctiles para construir el Path
    @Override
    public boolean onTouchEvent(MotionEvent evento) {
        float x = evento.getX();
        float y = evento.getY();
        switch (evento.getAction()) {
            case MotionEvent.ACTION_DOWN:
                trazo.moveTo(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                trazo.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                // nada extra
                break;
        }
        invalidate();  // pide redibujar la vista
        return true;
    }

    // 5) Obtener un Bitmap con todo lo dibujado
    public Bitmap getBitmap() {
        Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        draw(canvas);
        return bmp;
    }
}