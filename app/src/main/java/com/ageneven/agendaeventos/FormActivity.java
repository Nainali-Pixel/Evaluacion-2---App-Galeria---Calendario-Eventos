package com.ageneven.agendaeventos;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ageneven.agendaeventos.data.EventoRepositorio;
import com.ageneven.agendaeventos.model.Evento;
import com.ageneven.agendaeventos.util.ImagenUtil;
import com.ageneven.agendaeventos.util.Constantes;

/**
 * Formulario para crear o editar una anotación (si llega un id, se edita). La foto es
 * opcional: puede tomarse con la cámara trasera o frontal, o elegirse de la galería
 * (pidiendo el permiso correspondiente). Si no se agrega ninguna, se usa la imagen
 * predeterminada del tipo.
 */
public class FormActivity extends AppCompatActivity {

    private static final String ESTADO_FOTO = "estado_foto";
    private static final String ESTADO_ARCHIVO_CAMARA = "estado_archivo_camara";
    private static final int MAX_LADO_PX = 1600;

    private final String[] tipos = {Evento.TIPO_CUMPLEANOS, Evento.TIPO_EVENTO, Evento.TIPO_OTRO};
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor();

    private TextInputEditText edtTitulo, edtFecha, edtHora;
    private Spinner spinnerTipo;
    private ImageView imgVistaPrevia;
    private MaterialButton btnQuitarFoto;

    private String fotoUri;        // null = sin foto (se usa la imagen predeterminada)
    private File archivoCamara;    // archivo donde la cámara deja la foto
    private boolean camaraFrontal = false; // true = se tocó el botón "Cámara frontal"
    private static final int LARGO_MAXIMO_TITULO = 60;
    private long idEditando = -1;        // -1 = se está creando una anotación nueva
    private String fotoOriginal = null;  // foto que ya tenía la anotación (solo al editar)

    // ---- Lanzadores (deben registrarse antes de que la Activity inicie) ----

    private final ActivityResultLauncher<String> permisoCamara = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    abrirCamara();
                } else {
                    permisoDenegado(Manifest.permission.CAMERA, R.string.permiso_denegado_camara);
                }
            });

    private final ActivityResultLauncher<String> permisoGaleria = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    abrirGaleria();
                } else {
                    permisoDenegado(permisoImagenes(), R.string.permiso_denegado_galeria);
                }
            });

    private final ActivityResultLauncher<Intent> tomarFoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                boolean exito = resultado.getResultCode() == RESULT_OK;
                if (exito && archivoCamara != null && archivoCamara.exists()) {
                    final File archivo = archivoCamara;
                    procesarEnSegundoPlano(ImageDecoder.createSource(archivo), archivo);
                } else if (archivoCamara != null) {
                    //noinspection ResultOfMethodCallIgnored
                    archivoCamara.delete(); // el usuario canceló: no dejar archivos vacíos
                    archivoCamara = null;
                }
            });

    private final ActivityResultLauncher<String> elegirImagen = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return; // el usuario canceló
                try {
                    File destino = ImagenUtil.crearArchivoFoto(this);
                    procesarEnSegundoPlano(ImageDecoder.createSource(getContentResolver(), uri), destino);
                } catch (IOException e) {
                    Toast.makeText(this, R.string.error_foto, Toast.LENGTH_SHORT).show();
                }
            });

    // ---- Ciclo de vida ----

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_form);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        edtTitulo = findViewById(R.id.edtTitulo);
        edtFecha = findViewById(R.id.edtFecha);
        edtHora = findViewById(R.id.edtHora);
        spinnerTipo = findViewById(R.id.spinnerTipo);
        imgVistaPrevia = findViewById(R.id.imgVistaPrevia);
        btnQuitarFoto = findViewById(R.id.btnQuitarFoto);
        MaterialButton btnTomarFoto = findViewById(R.id.btnTomarFoto);
        MaterialButton btnGaleria = findViewById(R.id.btnGaleria);
        MaterialButton btnGuardar = findViewById(R.id.btnGuardar);
        MaterialButton btnCamaraFrontal = findViewById(R.id.btnCamaraFrontal);

        ajustarBordes(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Recuperar la foto si la pantalla se recreó (por ejemplo, al rotar)
        if (savedInstanceState != null) {
            fotoUri = savedInstanceState.getString(ESTADO_FOTO);
            String ruta = savedInstanceState.getString(ESTADO_ARCHIVO_CAMARA);
            if (ruta != null) archivoCamara = new File(ruta);
        }

        // Tipo
        spinnerTipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tipos));
        spinnerTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                actualizarVistaPrevia(); // cambia la imagen predeterminada si no hay foto
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Fecha y hora (por defecto: ahora)
        Calendar ahora = Calendar.getInstance();
        edtFecha.setText(String.format(Locale.US, "%02d/%02d/%04d",
                ahora.get(Calendar.DAY_OF_MONTH), ahora.get(Calendar.MONTH) + 1, ahora.get(Calendar.YEAR)));
        edtHora.setText(String.format(Locale.US, "%02d:%02d",
                ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE)));
        edtFecha.setOnClickListener(v -> elegirFecha());
        edtHora.setOnClickListener(v -> elegirHora());

        // Modo edición: si llega un id, se cargan los datos de esa anotación
        idEditando = getIntent().getLongExtra(Constantes.EXTRA_ID, -1);
        if (idEditando != -1) {
            Evento anotacion = EventoRepositorio.getInstancia().buscarPorId(idEditando);
            if (anotacion == null) {
                Toast.makeText(this, R.string.error_anotacion_no_encontrada, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            fotoOriginal = anotacion.getFotoUri();
            toolbar.setTitle(R.string.titulo_editar);
            btnGuardar.setText(R.string.btn_guardar_cambios);

            // Los campos se rellenan solo la primera vez (no al rotar la pantalla)
            if (savedInstanceState == null) {
                edtTitulo.setText(anotacion.getTitulo());
                edtFecha.setText(anotacion.getFecha());
                edtHora.setText(anotacion.getHora());
                fotoUri = anotacion.getFotoUri();
                for (int i = 0; i < tipos.length; i++) {
                    if (tipos[i].equals(anotacion.getTipo())) {
                        spinnerTipo.setSelection(i);
                    }
                }
            }
        }

        // Foto
        btnTomarFoto.setOnClickListener(v -> {
            camaraFrontal = false;
            pedirPermisoCamara();
        });
        btnCamaraFrontal.setOnClickListener(v -> abrirCamaraFrontal());
        btnGaleria.setOnClickListener(v -> pedirPermisoGaleria());
        btnQuitarFoto.setOnClickListener(v -> {
            borrarFotoTemporal(fotoUri);
            fotoUri = null;
            actualizarVistaPrevia();
        });

        btnGuardar.setOnClickListener(v -> guardar());
        actualizarVistaPrevia();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_FOTO, fotoUri);
        if (archivoCamara != null) {
            outState.putString(ESTADO_ARCHIVO_CAMARA, archivoCamara.getAbsolutePath());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ejecutor.shutdown();
    }

    // ---- Permisos ----

    /** Permiso de lectura de imágenes según la versión de Android. */
    private String permisoImagenes() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void pedirPermisoCamara() {
        solicitarPermiso(Manifest.permission.CAMERA, permisoCamara,
                R.string.permiso_camara_titulo, R.string.permiso_camara_explicacion, this::abrirCamara);
    }

    private void pedirPermisoGaleria() {
        solicitarPermiso(permisoImagenes(), permisoGaleria,
                R.string.permiso_galeria_titulo, R.string.permiso_galeria_explicacion, this::abrirGaleria);
    }

    /** Flujo estándar: ya concedido → continuar; si lo negó antes → explicar; si no → pedir. */
    private void solicitarPermiso(String permiso, ActivityResultLauncher<String> lanzador,
                                  int titulo, int explicacion, Runnable alConceder) {
        if (ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED) {
            alConceder.run();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permiso)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(titulo)
                    .setMessage(explicacion)
                    .setPositiveButton(R.string.btn_continuar, (d, w) -> lanzador.launch(permiso))
                    .setNegativeButton(R.string.btn_cancelar, null)
                    .show();
        } else {
            lanzador.launch(permiso);
        }
    }

    /** El usuario negó el permiso: avisar, y si lo bloqueó para siempre, llevarlo a Ajustes. */
    private void permisoDenegado(String permiso, int mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permiso)) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.permiso_bloqueado)
                    .setPositiveButton(R.string.btn_abrir_ajustes, (d, w) -> startActivity(
                            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", getPackageName(), null))))
                    .setNegativeButton(R.string.btn_cancelar, null)
                    .show();
        }
    }

    // ---- Cámara y galería ----

    /** IMPLÍCITO: abre la app de cámara (trasera o frontal, según el botón tocado). */
    private void abrirCamara() {
        try {
            archivoCamara = ImagenUtil.crearArchivoFoto(this);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivoCamara);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.setClipData(ClipData.newRawUri("foto", uri));
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (camaraFrontal) {
                // Pistas para abrir directamente la cámara frontal (cada app de cámara las interpreta)
                intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
                intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
                intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
            }
            tomarFoto.launch(intent);
        } catch (IOException | ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_sin_camara, Toast.LENGTH_SHORT).show();
        }
    }

    /** Valida que el equipo tenga cámara frontal antes de pedir el permiso y abrirla. */
    private void abrirCamaraFrontal() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            Toast.makeText(this, R.string.error_sin_camara_frontal, Toast.LENGTH_SHORT).show();
            return;
        }
        camaraFrontal = true;
        pedirPermisoCamara();
    }

    private void abrirGaleria() {
        try {
            elegirImagen.launch("image/*");
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_sin_galeria, Toast.LENGTH_SHORT).show();
        }
    }

    /** Reduce y guarda la imagen fuera del hilo principal, y luego actualiza la pantalla. */
    private void procesarEnSegundoPlano(ImageDecoder.Source origen, File destino) {
        ejecutor.execute(() -> {
            boolean ok = ImagenUtil.guardarReducida(origen, destino, MAX_LADO_PX);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (ok) {
                    borrarFotoTemporal(fotoUri); // reemplaza la foto anterior
                    fotoUri = Uri.fromFile(destino).toString();
                    actualizarVistaPrevia();
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    destino.delete();
                    Toast.makeText(this, R.string.error_foto, Toast.LENGTH_SHORT).show();
                }
                archivoCamara = null;
            });
        });
    }

    // ---- Vista previa ----

    /** Muestra la foto elegida o, si no hay, la imagen predeterminada del tipo. */
    private void actualizarVistaPrevia() {
        if (ImagenUtil.existeFoto(fotoUri)) {
            imgVistaPrevia.setImageURI(Uri.fromFile(ImagenUtil.archivoDesdeUri(fotoUri)));
            btnQuitarFoto.setVisibility(View.VISIBLE);
        } else {
            fotoUri = null;
            String tipo = (String) spinnerTipo.getSelectedItem();
            imgVistaPrevia.setImageResource(ImagenUtil.drawablePorTipo(tipo));
            btnQuitarFoto.setVisibility(View.GONE);
        }
    }

    // ---- Fecha, hora y guardado ----

    private void elegirFecha() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (picker, anio, mes, dia) ->
                edtFecha.setText(String.format(Locale.US, "%02d/%02d/%04d", dia, mes + 1, anio)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void elegirHora() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (picker, hora, minuto) ->
                edtHora.setText(String.format(Locale.US, "%02d:%02d", hora, minuto)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    /** Guarda una anotación nueva o los cambios de una existente. */
    private void guardar() {
        String titulo = edtTitulo.getText() == null ? "" : edtTitulo.getText().toString().trim();
        String fecha = String.valueOf(edtFecha.getText());
        String hora = String.valueOf(edtHora.getText());
        String tipo = (String) spinnerTipo.getSelectedItem();

        // Validaciones: si algo está mal, no se guarda
        if (!validarDatos(titulo, fecha, hora, tipo)) {
            return;
        }

        EventoRepositorio repositorio = EventoRepositorio.getInstancia();

        if (idEditando == -1) {
            // Anotación nueva
            repositorio.agregar(titulo, fecha, hora, tipo, fotoUri);
            Toast.makeText(this, R.string.anotacion_guardada, Toast.LENGTH_SHORT).show();
        } else {
            // Editando: si no cambió nada, se avisa
            Evento original = repositorio.buscarPorId(idEditando);
            if (original != null && sinCambios(original, titulo, fecha, hora, tipo)) {
                Toast.makeText(this, R.string.error_sin_cambios, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean fueActualizada = repositorio.actualizar(idEditando, titulo, fecha, hora, tipo, fotoUri);
            if (!fueActualizada) {
                Toast.makeText(this, R.string.error_actualizar, Toast.LENGTH_SHORT).show();
                return;
            }

            // Si cambió o quitó la foto, ahora sí se borra la anterior
            if (fotoOriginal != null && !fotoOriginal.equals(fotoUri)) {
                ImagenUtil.borrar(fotoOriginal);
            }
            Toast.makeText(this, R.string.anotacion_actualizada, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
        }
        // EXPLÍCITO 13: avisa a MainActivity con LocalBroadcastManager
        Intent aviso = new Intent(Constantes.ACCION_EVENTO_GUARDADO);
        aviso.putExtra(Constantes.EXTRA_TITULO, titulo);
        LocalBroadcastManager.getInstance(this).sendBroadcast(aviso);
        finish(); // MainActivity se actualiza en onResume()
    }

    /** Revisa que todos los datos estén completos y sean válidos. */
    private boolean validarDatos(String titulo, String fecha, String hora, String tipo) {
        if (titulo.isEmpty()) {
            edtTitulo.setError(getString(R.string.error_titulo_vacio));
            edtTitulo.requestFocus();
            return false;
        }
        if (titulo.length() > LARGO_MAXIMO_TITULO) {
            edtTitulo.setError(getString(R.string.error_titulo_largo, LARGO_MAXIMO_TITULO));
            edtTitulo.requestFocus();
            return false;
        }
        if (tipo == null || tipo.isEmpty()) {
            Toast.makeText(this, R.string.error_tipo_vacio, Toast.LENGTH_SHORT).show();
            return false;
        }

        // La fecha y la hora deben existir de verdad (ej: no existe 31/02)
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
        formato.setLenient(false);
        try {
            formato.parse(fecha + " " + hora);
        } catch (ParseException e) {
            Toast.makeText(this, R.string.error_fecha_invalida, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /** Compara los datos del formulario con los que ya tenía la anotación. */
    private boolean sinCambios(Evento original, String titulo, String fecha, String hora, String tipo) {
        return original.getTitulo().equals(titulo)
                && original.getFecha().equals(fecha)
                && original.getHora().equals(hora)
                && original.getTipo().equals(tipo)
                && Objects.equals(original.getFotoUri(), fotoUri);
    }

    /** Borra una foto solo si no es la original de la anotación (esa se borra al guardar). */
    private void borrarFotoTemporal(String uri) {
        if (uri != null && !uri.equals(fotoOriginal)) {
            ImagenUtil.borrar(uri);
        }
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
