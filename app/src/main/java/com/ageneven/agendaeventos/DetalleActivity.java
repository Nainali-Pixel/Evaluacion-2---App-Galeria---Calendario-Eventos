package com.ageneven.agendaeventos;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.ageneven.agendaeventos.model.Evento;
import com.ageneven.agendaeventos.util.Constantes;
import com.ageneven.agendaeventos.util.ImagenUtil;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ageneven.agendaeventos.data.EventoRepositorio;

/**
 * Detalle de una anotación. Recibe los datos desde MainActivity (putExtra) y permite
 * agregarla al calendario, enviarla por correo, editarla y eliminarla.
 */
public class DetalleActivity extends AppCompatActivity {

    private static final long UNA_HORA_MS = 60L * 60L * 1000L;

    private String titulo;
    private String fecha;
    private String hora;
    private String tipo;
    private String fotoUri;

    private ImageView imgDetalle;
    private TextView txtAmpliar;

    private long idEvento;
    private TextView txtTitulo, txtFechaHora, txtTipo;

    //Recibe el resultado de la pantalla de edición
    private final ActivityResultLauncher<Intent> lanzadorEdicion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult resultado) {
                    if (resultado.getResultCode() == RESULT_OK) {
                        recargarDesdeRepositorio();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_detalle);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        imgDetalle = findViewById(R.id.imgDetalle);
        txtAmpliar = findViewById(R.id.txtAmpliar);
        txtTitulo = findViewById(R.id.txtTituloDetalle);
        txtFechaHora = findViewById(R.id.txtFechaHoraDetalle);
        txtTipo = findViewById(R.id.txtTipoDetalle);
        MaterialButton btnCalendario = findViewById(R.id.btnAgregarCalendario);
        MaterialButton btnCorreo = findViewById(R.id.btnEnviarCorreo);
        MaterialButton btnEditar = findViewById(R.id.btnEditar);
        MaterialButton btnEliminar = findViewById(R.id.btnEliminar);

        ajustarBordes(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 1. Leer los datos enviados por MainActivity
        if (!leerExtras()) {
            Toast.makeText(this, R.string.error_datos_detalle, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Mostrar los datos
        mostrarDatos();

        // 3. Las dos únicas acciones
        btnCalendario.setOnClickListener(v -> agregarAlCalendario());
        btnCorreo.setOnClickListener(v -> enviarPorCorreo());


        // 4. Editar y eliminar
        btnEditar.setOnClickListener(v -> abrirEdicion());
        btnEliminar.setOnClickListener(v -> confirmarEliminacion());
    }

    /** Devuelve false si faltan los datos mínimos para mostrar la pantalla. */
    private boolean leerExtras() {
        Intent intent = getIntent();
        titulo = intent.getStringExtra(Constantes.EXTRA_TITULO);
        fecha = intent.getStringExtra(Constantes.EXTRA_FECHA);
        hora = intent.getStringExtra(Constantes.EXTRA_HORA);
        tipo = intent.getStringExtra(Constantes.EXTRA_TIPO);
        fotoUri = intent.getStringExtra(Constantes.EXTRA_FOTO_URI);
        idEvento = intent.getLongExtra(Constantes.EXTRA_ID, -1);

        if (tipo == null || tipo.isEmpty()) tipo = Evento.TIPO_OTRO;
        return titulo != null && fecha != null && hora != null && idEvento != -1;
    }


    /** Después de editar, vuelve a leer la anotación y actualiza la pantalla. */
    private void recargarDesdeRepositorio() {
        Evento actualizada = EventoRepositorio.getInstancia().buscarPorId(idEvento);
        if (actualizada == null) {
            finish();
            return;
        }
        titulo = actualizada.getTitulo();
        fecha = actualizada.getFecha();
        hora = actualizada.getHora();
        tipo = actualizada.getTipo();
        fotoUri = actualizada.getFotoUri();
        mostrarDatos();
    }

    /** Muestra los datos de la anotación en pantalla. */
    private void mostrarDatos() {
        txtTitulo.setText(titulo);
        txtFechaHora.setText(getString(R.string.fecha_hora, fecha, hora));
        txtTipo.setText(tipo);
        txtTipo.setTextColor(ContextCompat.getColor(this, colorPorTipo(tipo)));
        mostrarImagen();
    }

    /** EXPLÍCITO con resultado: DetalleActivity → FormActivity (modo edición). */
    private void abrirEdicion() {
        Intent intent = new Intent(this, FormActivity.class);
        intent.putExtra(Constantes.EXTRA_ID, idEvento);
        lanzadorEdicion.launch(intent);
    }

    /** Primer mensaje: pregunta si de verdad quiere eliminar. */
    private void confirmarEliminacion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_confirmar_eliminar)
                .setMessage(getString(R.string.mensaje_confirmar_eliminar, titulo))
                .setPositiveButton(R.string.btn_si_eliminar, (dialogo, boton) -> eliminarAnotacion())
                .setNegativeButton(R.string.btn_cancelar, null)
                .show();
    }

    /** Elimina la anotación y muestra el segundo mensaje validando la eliminación. */
    private void eliminarAnotacion() {
        boolean fueEliminada = EventoRepositorio.getInstancia().eliminar(idEvento);
        if (!fueEliminada) {
            Toast.makeText(this, R.string.error_eliminar, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_eliminada)
                .setMessage(getString(R.string.mensaje_eliminada, titulo))
                .setCancelable(false)
                .setPositiveButton(R.string.btn_aceptar, (dialogo, boton) -> finish())
                .show();
    }

    /** Foto del usuario; si no hay (o ya no existe), imagen predeterminada del tipo. */
    private void mostrarImagen() {
        if (ImagenUtil.existeFoto(fotoUri)) {
            imgDetalle.setImageURI(Uri.fromFile(ImagenUtil.archivoDesdeUri(fotoUri)));
            if (imgDetalle.getDrawable() != null) {
                txtAmpliar.setVisibility(View.VISIBLE);
                imgDetalle.setOnClickListener(v -> {
                    // EXPLÍCITO: DetalleActivity → PhotoActivity (foto a pantalla completa)
                    Intent intent = new Intent(this, PhotoActivity.class);
                    intent.putExtra(Constantes.EXTRA_FOTO_URI, fotoUri);
                    startActivity(intent);
                });
                return;
            }
        }
        imgDetalle.setImageResource(ImagenUtil.drawablePorTipo(tipo));
        imgDetalle.setOnClickListener(null);
        imgDetalle.setClickable(false);
        txtAmpliar.setVisibility(View.GONE);
    }

    /**
     * Botón 1: abre la app de calendario con el evento ya rellenado.
     * No requiere permiso WRITE_CALENDAR: el usuario confirma el guardado en su calendario.
     */
    private void agregarAlCalendario() {
        Date inicio = parsearFechaHora();
        if (inicio == null) {
            Toast.makeText(this, R.string.error_fecha_invalida, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, titulo)
                .putExtra(CalendarContract.Events.DESCRIPTION, tipo)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicio.getTime())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, inicio.getTime() + UNA_HORA_MS);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_sin_calendario, Toast.LENGTH_SHORT).show();
        }
    }

    /** Botón 2: envía el recordatorio por correo (con la foto adjunta si el usuario puso una). */
    private void enviarPorCorreo() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822"); // solo apps de correo
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.asunto_correo, titulo));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.cuerpo_correo, titulo, fecha, hora, tipo));

        Uri adjunto = ImagenUtil.uriParaCompartir(this, fotoUri);
        if (adjunto != null) {
            intent.putExtra(Intent.EXTRA_STREAM, adjunto);
            intent.setClipData(ClipData.newRawUri("foto", adjunto));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.elegir_app_correo)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_sin_correo, Toast.LENGTH_SHORT).show();
        }
    }

    /** Convierte "dd/MM/yyyy" + "HH:mm" en una fecha; null si el formato no es válido. */
    private Date parsearFechaHora() {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
        formato.setLenient(false);
        try {
            return formato.parse(fecha + " " + hora);
        } catch (ParseException e) {
            return null;
        }
    }

    private int colorPorTipo(String tipo) {
        if (Evento.TIPO_CUMPLEANOS.equals(tipo)) return R.color.tipo_cumpleanos;
        if (Evento.TIPO_EVENTO.equals(tipo)) return R.color.tipo_evento;
        return R.color.tipo_otro;
    }

    /** Evita que el contenido quede debajo de la barra de estado o de navegación. */
    private void ajustarBordes(View toolbar) {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(barras.left, barras.top, barras.right, 0);
            v.setPadding(0, 0, 0, barras.bottom);
            return insets;
        });
    }
}
