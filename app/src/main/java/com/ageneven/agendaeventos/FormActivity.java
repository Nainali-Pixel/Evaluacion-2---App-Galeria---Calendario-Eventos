package com.ageneven.agendaeventos;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

import com.ageneven.agendaeventos.data.EventoRepositorio;
import com.ageneven.agendaeventos.model.Evento;
import com.ageneven.agendaeventos.util.Constantes;

public class FormActivity extends AppCompatActivity {

    //Variables
    MaterialToolbar barraSuperior;
    TextInputLayout contenedorTitulo;
    TextInputLayout contenedorFecha;
    TextInputLayout contenedorHora;
    TextInputEditText campoTitulo;
    TextInputEditText campoFecha;
    TextInputEditText campoHora;
    Spinner spinnerTipo;
    MaterialButton botonTomarFoto;
    MaterialButton botonElegirGaleria;
    MaterialButton botonGuardar;
    MaterialCardView tarjetaVistaPrevia;
    ImageView imagenVistaPrevia;
    TextView textoVistaPrevia;

    //Datos de la anotacion que se esta creando
    String fechaSeleccionada = null;
    String horaSeleccionada = null;
    Uri uriFotoSeleccionada = null;
    Uri uriFotoCamaraTemporal = null;
    String[] tiposDisponibles = {
            Evento.TIPO_CUMPLEANOS,
            Evento.TIPO_EVENTO,
            Evento.TIPO_OTRO
    };

    //Lanzadores para recibir resultados de otras pantallas
    ActivityResultLauncher<Intent> lanzadorCamara;
    ActivityResultLauncher<Intent> lanzadorGaleria;
    ActivityResultLauncher<Intent> lanzadorConfirmacion;

    //Claves para conservar los datos si la pantalla se recrea (ej: al rotar)
    final String CLAVE_FECHA = "estado_fecha";
    final String CLAVE_HORA = "estado_hora";
    final String CLAVE_FOTO = "estado_foto";
    final String CLAVE_FOTO_CAMARA = "estado_foto_camara";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_form);

        //Referencias
        barraSuperior = findViewById(R.id.toolbar);
        contenedorTitulo = findViewById(R.id.contenedorTitulo);
        contenedorFecha = findViewById(R.id.contenedorFecha);
        contenedorHora = findViewById(R.id.contenedorHora);
        campoTitulo = findViewById(R.id.campoTitulo);
        campoFecha = findViewById(R.id.campoFecha);
        campoHora = findViewById(R.id.campoHora);
        spinnerTipo = findViewById(R.id.spinnerTipo);
        botonTomarFoto = findViewById(R.id.botonTomarFoto);
        botonElegirGaleria = findViewById(R.id.botonElegirGaleria);
        botonGuardar = findViewById(R.id.botonGuardar);
        tarjetaVistaPrevia = findViewById(R.id.tarjetaVistaPrevia);
        imagenVistaPrevia = findViewById(R.id.imagenVistaPrevia);
        textoVistaPrevia = findViewById(R.id.textoVistaPrevia);

        ajustarMargenesDelSistema();

        //Llenar el Spinner con los tipos (Cumpleaños, Evento, Otro)
        ArrayAdapter<String> adaptadorTipos = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, tiposDisponibles
        );
        adaptadorTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adaptadorTipos);

        //Recuperar los datos si la pantalla se recreo
        if (savedInstanceState != null) {
            recuperarDatosGuardados(savedInstanceState);
        }

        prepararLanzadores();

        //Boton atras de la barra superior
        barraSuperior.setNavigationOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                    }
                }
        );

        //Campo fecha: abre el calendario para elegir
        campoFecha.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mostrarSelectorDeFecha();
                    }
                }
        );

        //Campo hora: abre el reloj para elegir
        campoHora.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mostrarSelectorDeHora();
                    }
                }
        );

        //Al cambiar el tipo se actualiza la imagen predeterminada
        spinnerTipo.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> padre, View vista, int posicion, long id) {
                        mostrarVistaPrevia();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> padre) {
                        //No se hace nada
                    }
                }
        );

        //Boton Tomar foto
        botonTomarFoto.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        abrirCamara();
                    }
                }
        );

        //Boton Elegir de galeria
        botonElegirGaleria.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        abrirGaleria();
                    }
                }
        );

        //Boton Guardar: valida y pasa a la pantalla de confirmacion
        botonGuardar.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!validarFormulario()) {
                            return;
                        }

                        //Intent Explicito con resultado: FormActivity -> ConfirmActivity
                        Intent irAConfirmacion = new Intent(
                                FormActivity.this, ConfirmActivity.class
                        );
                        irAConfirmacion.putExtra(Constantes.EXTRA_TITULO, obtenerTituloEscrito());
                        irAConfirmacion.putExtra(Constantes.EXTRA_FECHA, fechaSeleccionada);
                        irAConfirmacion.putExtra(Constantes.EXTRA_HORA, horaSeleccionada);
                        irAConfirmacion.putExtra(Constantes.EXTRA_TIPO, (String) spinnerTipo.getSelectedItem());
                        irAConfirmacion.putExtra(Constantes.EXTRA_FOTO_URI, obtenerFotoComoTexto());
                        lanzadorConfirmacion.launch(irAConfirmacion);
                    }
                }
        );

        mostrarVistaPrevia();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putString(CLAVE_FECHA, fechaSeleccionada);
        estado.putString(CLAVE_HORA, horaSeleccionada);
        estado.putString(CLAVE_FOTO, obtenerFotoComoTexto());
        if (uriFotoCamaraTemporal != null) {
            estado.putString(CLAVE_FOTO_CAMARA, uriFotoCamaraTemporal.toString());
        }
    }

    private void recuperarDatosGuardados(Bundle estado) {
        fechaSeleccionada = estado.getString(CLAVE_FECHA);
        horaSeleccionada = estado.getString(CLAVE_HORA);

        String fotoGuardada = estado.getString(CLAVE_FOTO);
        if (fotoGuardada != null) {
            uriFotoSeleccionada = Uri.parse(fotoGuardada);
        }

        String fotoCamaraGuardada = estado.getString(CLAVE_FOTO_CAMARA);
        if (fotoCamaraGuardada != null) {
            uriFotoCamaraTemporal = Uri.parse(fotoCamaraGuardada);
        }
    }

    //Registra los lanzadores que reciben el resultado de la camara, la galeria y la confirmacion
    private void prepararLanzadores() {

        //Resultado de la camara
        lanzadorCamara = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult resultado) {
                        if (resultado.getResultCode() == RESULT_OK) {
                            uriFotoSeleccionada = uriFotoCamaraTemporal;
                            mostrarVistaPrevia();
                        } else if (uriFotoCamaraTemporal != null) {
                            //El usuario cancelo: se borra el archivo vacio que se habia creado
                            getContentResolver().delete(uriFotoCamaraTemporal, null, null);
                        }
                        uriFotoCamaraTemporal = null;
                    }
                }
        );

        //Resultado de la galeria (devuelve la URI de la imagen elegida)
        lanzadorGaleria = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult resultado) {
                        Intent datos = resultado.getData();
                        if (resultado.getResultCode() != RESULT_OK || datos == null || datos.getData() == null) {
                            return; //El usuario cancelo o no llego ninguna imagen
                        }

                        Uri uriElegida = datos.getData();
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uriElegida, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (SecurityException e) {
                            //Algunas galerias no permiten guardar el permiso, se sigue igual
                        }
                        uriFotoSeleccionada = uriElegida;
                        mostrarVistaPrevia();
                    }
                }
        );

        //Resultado de la confirmacion
        lanzadorConfirmacion = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult resultado) {
                        if (resultado.getResultCode() == RESULT_OK) {
                            guardarAnotacion();
                        }
                    }
                }
        );
    }

    //Intent Implicito: abrir la camara y guardar la foto en la galeria
    private void abrirCamara() {
        Uri uriDestino = crearUriParaFotoNueva();
        if (uriDestino == null) {
            Toast.makeText(this, R.string.mensaje_error_foto, Toast.LENGTH_SHORT).show();
            return;
        }
        uriFotoCamaraTemporal = uriDestino;

        Intent intentCamara = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intentCamara.putExtra(MediaStore.EXTRA_OUTPUT, uriDestino);

        try {
            lanzadorCamara.launch(intentCamara);
        } catch (ActivityNotFoundException e) {
            //Validacion: el telefono no tiene app de camara
            getContentResolver().delete(uriDestino, null, null);
            uriFotoCamaraTemporal = null;
            Toast.makeText(this, R.string.mensaje_sin_camara, Toast.LENGTH_SHORT).show();
        }
    }

    //Intent Implicito: elegir una imagen de la galeria
    private void abrirGaleria() {
        Intent intentGaleria = new Intent(Intent.ACTION_GET_CONTENT);
        intentGaleria.setType("image/*");
        intentGaleria.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            lanzadorGaleria.launch(intentGaleria);
        } catch (ActivityNotFoundException e) {
            //Validacion: el telefono no tiene app de galeria
            Toast.makeText(this, R.string.mensaje_sin_galeria, Toast.LENGTH_SHORT).show();
        }
    }

    //Crea un archivo vacio en Imagenes/AgendaEventos donde la camara dejara la foto
    private Uri crearUriParaFotoNueva() {
        ContentValues datosFoto = new ContentValues();
        datosFoto.put(MediaStore.Images.Media.DISPLAY_NAME, "agenda_" + System.currentTimeMillis() + ".jpg");
        datosFoto.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        datosFoto.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AgendaEventos");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, datosFoto);
    }

    private void mostrarSelectorDeFecha() {
        Calendar hoy = Calendar.getInstance();
        DatePickerDialog dialogoFecha = new DatePickerDialog(
                FormActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker vista, int anio, int mes, int dia) {
                        //El mes empieza en 0, por eso se suma 1
                        fechaSeleccionada = String.format(Locale.ROOT, "%02d/%02d/%04d", dia, mes + 1, anio);
                        campoFecha.setText(fechaSeleccionada);
                        contenedorFecha.setError(null);
                    }
                },
                hoy.get(Calendar.YEAR),
                hoy.get(Calendar.MONTH),
                hoy.get(Calendar.DAY_OF_MONTH)
        );
        dialogoFecha.show();
    }

    private void mostrarSelectorDeHora() {
        Calendar ahora = Calendar.getInstance();
        TimePickerDialog dialogoHora = new TimePickerDialog(
                FormActivity.this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker vista, int hora, int minuto) {
                        horaSeleccionada = String.format(Locale.ROOT, "%02d:%02d", hora, minuto);
                        campoHora.setText(horaSeleccionada);
                        contenedorHora.setError(null);
                    }
                },
                ahora.get(Calendar.HOUR_OF_DAY),
                ahora.get(Calendar.MINUTE),
                true
        );
        dialogoHora.show();
    }

    //Muestra la foto elegida o, si no hay, la imagen predeterminada segun el tipo
    private void mostrarVistaPrevia() {
        String tipoSeleccionado = (String) spinnerTipo.getSelectedItem();
        int colorDelTipo = ContextCompat.getColor(this, obtenerColorPorTipo(tipoSeleccionado));
        tarjetaVistaPrevia.setCardBackgroundColor(colorDelTipo);

        boolean fotoMostrada = false;
        if (uriFotoSeleccionada != null) {
            try {
                imagenVistaPrevia.setImageURI(uriFotoSeleccionada);
                fotoMostrada = imagenVistaPrevia.getDrawable() != null;
            } catch (SecurityException e) {
                //Sin permiso para leer la foto: se mostrara la imagen predeterminada
            }
        }

        if (fotoMostrada) {
            textoVistaPrevia.setVisibility(View.GONE);
        } else {
            //Si la foto no se pudo cargar, se descarta para no guardar una URI rota
            uriFotoSeleccionada = null;
            imagenVistaPrevia.setImageDrawable(null);
            textoVistaPrevia.setText(getString(R.string.vista_previa_predeterminada, tipoSeleccionado));
            textoVistaPrevia.setVisibility(View.VISIBLE);
        }
    }

    //Devuelve el color que corresponde a cada tipo
    private int obtenerColorPorTipo(String tipo) {
        if (Evento.TIPO_CUMPLEANOS.equals(tipo)) {
            return R.color.tipo_cumpleanos;
        }
        if (Evento.TIPO_EVENTO.equals(tipo)) {
            return R.color.tipo_evento;
        }
        return R.color.tipo_otro;
    }

    //Validaciones: titulo, fecha y hora son obligatorios
    private boolean validarFormulario() {
        boolean todoCorrecto = true;

        if (obtenerTituloEscrito().isEmpty()) {
            contenedorTitulo.setError(getString(R.string.error_titulo_vacio));
            todoCorrecto = false;
        } else {
            contenedorTitulo.setError(null);
        }

        if (fechaSeleccionada == null) {
            contenedorFecha.setError(getString(R.string.error_fecha_vacia));
            todoCorrecto = false;
        } else {
            contenedorFecha.setError(null);
        }

        if (horaSeleccionada == null) {
            contenedorHora.setError(getString(R.string.error_hora_vacia));
            todoCorrecto = false;
        } else {
            contenedorHora.setError(null);
        }

        return todoCorrecto;
    }

    //Titulo sin espacios al inicio ni al final (nunca devuelve null)
    private String obtenerTituloEscrito() {
        if (campoTitulo.getText() == null) {
            return "";
        }
        return campoTitulo.getText().toString().trim();
    }

    //La foto como texto (null si el usuario no eligio ninguna)
    private String obtenerFotoComoTexto() {
        if (uriFotoSeleccionada == null) {
            return null;
        }
        return uriFotoSeleccionada.toString();
    }

    //Guarda la anotacion en el repositorio y vuelve a la lista
    private void guardarAnotacion() {
        EventoRepositorio repositorioEventos = EventoRepositorio.getInstancia();
        repositorioEventos.agregar(
                obtenerTituloEscrito(),
                fechaSeleccionada,
                horaSeleccionada,
                (String) spinnerTipo.getSelectedItem(),
                obtenerFotoComoTexto()
        );
        Toast.makeText(this, R.string.mensaje_guardado, Toast.LENGTH_SHORT).show();
        finish();
    }

    //Evita que el contenido quede debajo de las barras del sistema o del teclado
    private void ajustarMargenesDelSistema() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                new OnApplyWindowInsetsListener() {
                    @NonNull
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(View vista, WindowInsetsCompat insets) {
                        Insets barrasDelSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                        Insets zonaInferior = insets.getInsets(
                                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
                        );
                        barraSuperior.setPadding(barrasDelSistema.left, barrasDelSistema.top, barrasDelSistema.right, 0);
                        vista.setPadding(0, 0, 0, zonaInferior.bottom);
                        return insets;
                    }
                }
        );
    }
}