package com.ageneven.agendaeventos.util;

/** Claves compartidas entre pantallas. */
public final class Constantes {

    private Constantes() { }

    // Extras de MainActivity → DetalleActivity (explícito 1)
    public static final String EXTRA_ID = "extra_id";
    public static final String EXTRA_TITULO = "extra_titulo";
    public static final String EXTRA_FECHA = "extra_fecha";
    public static final String EXTRA_HORA = "extra_hora";
    public static final String EXTRA_TIPO = "extra_tipo";
    public static final String EXTRA_FOTO_URI = "extra_foto_uri";

    // Broadcast "se guardó una anotación" (explícitos 8 y 13)
    public static final String ACCION_EVENTO_GUARDADO = "com.ageneven.agendaeventos.EVENTO_GUARDADO";
}