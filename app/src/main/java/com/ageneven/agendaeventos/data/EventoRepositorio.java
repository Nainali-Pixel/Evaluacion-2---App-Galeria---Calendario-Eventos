package com.ageneven.agendaeventos.data;

import java.util.ArrayList;
import java.util.List;
import com.ageneven.agendaeventos.util.ImagenUtil;

import com.ageneven.agendaeventos.model.Evento;

/**
 * Guarda las anotaciones en memoria mientras la app está abierta.
 * Cuando el equipo agregue la base de datos, solo cambia esta clase.
 */
public class EventoRepositorio {

    private static EventoRepositorio instancia;
    private final List<Evento> eventos = new ArrayList<>();
    private long siguienteId = 1;

    private EventoRepositorio() {
        // Datos de ejemplo (los mismos del mockup)
        agregar("Cumpleaños de Camila", "12/10/2026", "18:00", Evento.TIPO_CUMPLEANOS, null);
        agregar("Presentación Android", "20/10/2026", "10:30", Evento.TIPO_EVENTO, null);
        agregar("Renovar carnet", "28/10/2026", "09:00", Evento.TIPO_OTRO, null);
    }

    /** Devuelve siempre la MISMA instancia (patrón Singleton). */
    public static synchronized EventoRepositorio getInstancia() {
        if (instancia == null) {
            instancia = new EventoRepositorio();
        }
        return instancia;
    }

    /** Todas las anotaciones (copia de la lista). */
    public List<Evento> obtenerTodos() {
        return new ArrayList<>(eventos);
    }

    /** Solo las anotaciones de un tipo (para los filtros). */
    public List<Evento> obtenerPorTipo(String tipo) {
        List<Evento> filtrados = new ArrayList<>();
        for (Evento e : eventos) {
            if (e.getTipo().equals(tipo)) {
                filtrados.add(e);
            }
        }
        return filtrados;
    }

    /** Crea y guarda una nueva anotación (lo usará el Form). */
    public Evento agregar(String titulo, String fecha, String hora, String tipo, String fotoUri) {
        Evento nuevo = new Evento(siguienteId++, titulo, fecha, hora, tipo, fotoUri);
        eventos.add(nuevo);
        return nuevo;
    }

    /** Busca una anotación por su id (null si no existe). */
    public Evento buscarPorId(long id) {
        for (Evento e : eventos) {
            if (e.getId() == id) {
                return e;
            }
        }
        return null;
    }

    /** Elimina una anotación y su foto. Devuelve false si no existía. */
    public boolean eliminar(long id) {
        Evento encontrado = buscarPorId(id);
        if (encontrado == null) {
            return false;
        }
        eventos.remove(encontrado);
        ImagenUtil.borrar(encontrado.getFotoUri());
        return true;
    }

    /** Reemplaza los datos de una anotación (mismo id). Devuelve false si no existe. */
    public boolean actualizar(long id, String titulo, String fecha, String hora, String tipo, String fotoUri) {
        for (int i = 0; i < eventos.size(); i++) {
            if (eventos.get(i).getId() == id) {
                eventos.set(i, new Evento(id, titulo, fecha, hora, tipo, fotoUri));
                return true;
            }
        }
        return false;
    }
}