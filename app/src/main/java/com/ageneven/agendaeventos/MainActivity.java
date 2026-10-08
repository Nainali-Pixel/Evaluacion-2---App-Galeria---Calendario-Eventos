package com.ageneven.agendaeventos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import com.ageneven.agendaeventos.adapter.EventoAdapter;
import com.ageneven.agendaeventos.data.EventoRepositorio;
import com.ageneven.agendaeventos.model.Evento;
import com.ageneven.agendaeventos.util.Constantes;

/** Pantalla principal: lista de anotaciones con filtros por tipo. */
public class MainActivity extends AppCompatActivity implements EventoAdapter.OnEventoClickListener {

    private EventoAdapter adapter;
    private TextView txtVacio;
    private ExtendedFloatingActionButton fabNueva;
    private String filtroActual = null;     // null = "Todos"
    private String mensajePendiente = null; // aviso que se muestra al volver a la pantalla

    /** EXPLÍCITO 13: recibe el aviso de FormActivity (LocalBroadcastManager). */
    private final BroadcastReceiver receptorGuardado = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String titulo = intent.getStringExtra(Constantes.EXTRA_TITULO);
            mensajePendiente = (titulo == null)
                    ? getString(R.string.anotacion_guardada)
                    : getString(R.string.aviso_lista_guardada, titulo);
            cargarEventos();
        }
    };

    /** EXPLÍCITO 8: recibe el broadcast interno de DetalleActivity. */
    private final BroadcastReceiver receptorEliminado = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String titulo = intent.getStringExtra(Constantes.EXTRA_TITULO);
            mensajePendiente = (titulo == null)
                    ? null
                    : getString(R.string.aviso_lista_eliminada, titulo);
            cargarEventos();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        setContentView(R.layout.activity_main);

        // 1. Conectar las vistas del XML con Java
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        RecyclerView recycler = findViewById(R.id.recyclerEventos);
        ChipGroup chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        fabNueva = findViewById(R.id.fabNuevaAnotacion);
        txtVacio = findViewById(R.id.txtVacio);

        ajustarBordes(toolbar);

        // 2. Configurar la lista
        adapter = new EventoAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        // 3. Filtros
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.chipTodos : checkedIds.get(0);
            if (id == R.id.chipCumpleanos) {
                filtroActual = Evento.TIPO_CUMPLEANOS;
            } else if (id == R.id.chipEvento) {
                filtroActual = Evento.TIPO_EVENTO;
            } else {
                filtroActual = null;
            }
            cargarEventos();
        });

        // 4. Botón "+ Nueva anotación" → FormActivity (intent explícito)
        fabNueva.setOnClickListener(v -> startActivity(new Intent(this, FormActivity.class)));

        // 5. Registrar los receptores de avisos (se quitan en onDestroy)
        LocalBroadcastManager.getInstance(this).registerReceiver(
                receptorGuardado, new IntentFilter(Constantes.ACCION_EVENTO_GUARDADO));
        ContextCompat.registerReceiver(this, receptorEliminado,
                new IntentFilter(Constantes.ACCION_EVENTO_ELIMINADO),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarEventos();          // al volver, la lista se actualiza
        mostrarAvisoPendiente();  // y se muestra el aviso recibido, si hay uno
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Quitar los receptores para evitar fugas de memoria
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receptorGuardado);
        unregisterReceiver(receptorEliminado);
    }

    /** Lee del repositorio según el filtro y actualiza la lista. */
    private void cargarEventos() {
        EventoRepositorio repo = EventoRepositorio.getInstancia();
        List<Evento> lista = (filtroActual == null)
                ? repo.obtenerTodos()
                : repo.obtenerPorTipo(filtroActual);

        adapter.actualizarLista(lista);
        txtVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Muestra un mensaje breve abajo (Snackbar) con el aviso recibido. */
    private void mostrarAvisoPendiente() {
        if (mensajePendiente == null) return;
        Snackbar.make(findViewById(R.id.main), mensajePendiente, Snackbar.LENGTH_LONG)
                .setAnchorView(fabNueva)
                .show();
        mensajePendiente = null;
    }

    /** EXPLÍCITO 1: MainActivity → DetalleActivity enviando datos con putExtra. */
    @Override
    public void onEventoClick(Evento evento) {
        if (evento == null) return; // validación

        Intent intent = new Intent(this, DetalleActivity.class);
        intent.putExtra(Constantes.EXTRA_ID, evento.getId());
        intent.putExtra(Constantes.EXTRA_TITULO, evento.getTitulo());
        intent.putExtra(Constantes.EXTRA_FECHA, evento.getFecha());
        intent.putExtra(Constantes.EXTRA_HORA, evento.getHora());
        intent.putExtra(Constantes.EXTRA_TIPO, evento.getTipo());
        intent.putExtra(Constantes.EXTRA_FOTO_URI, evento.getFotoUri());
        startActivity(intent);
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