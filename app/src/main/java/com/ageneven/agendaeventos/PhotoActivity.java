package com.ageneven.agendaeventos;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

import com.ageneven.agendaeventos.util.Constantes;
import com.ageneven.agendaeventos.util.ImagenUtil;

/** Muestra la foto adjunta a pantalla completa. Tocar la imagen cierra la pantalla. */
public class PhotoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_photo);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView imgFoto = findViewById(R.id.imgFoto);
        File archivo = ImagenUtil.archivoDesdeUri(getIntent().getStringExtra(Constantes.EXTRA_FOTO_URI));
        if (archivo == null || !archivo.exists()) {
            finish();
            return;
        }
        imgFoto.setImageURI(Uri.fromFile(archivo));
        imgFoto.setOnClickListener(v -> finish());
    }
}
