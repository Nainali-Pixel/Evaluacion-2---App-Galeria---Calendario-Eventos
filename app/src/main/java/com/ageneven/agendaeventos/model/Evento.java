package com.ageneven.agendaeventos.model;

/** Representa una anotación de la agenda. */
public class Evento {

    // Tipos permitidos: usar SIEMPRE estas constantes
    public static final String TIPO_CUMPLEANOS = "Cumpleaños";
    public static final String TIPO_EVENTO = "Evento";
    public static final String TIPO_OTRO = "Otro";

    private final long id;
    private final String titulo;
    private final String fecha;   // dd/MM/yyyy
    private final String hora;    // HH:mm
    private final String tipo;
    private final String fotoUri; // puede ser null

    public Evento(long id, String titulo, String fecha, String hora, String tipo, String fotoUri) {
        this.id = id;
        this.titulo = titulo;
        this.fecha = fecha;
        this.hora = hora;
        this.tipo = tipo;
        this.fotoUri = fotoUri;
    }

    public long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getFecha() { return fecha; }
    public String getHora() { return hora; }
    public String getTipo() { return tipo; }
    public String getFotoUri() { return fotoUri; }

    /** Validación: ¿el usuario adjuntó foto? */
    public boolean tieneFoto() {
        return fotoUri != null && !fotoUri.isEmpty();
    }
}