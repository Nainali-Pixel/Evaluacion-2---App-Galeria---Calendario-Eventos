package com.ageneven.agendaeventos.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.util.Size;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.ageneven.agendaeventos.R;
import com.ageneven.agendaeventos.model.Evento;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Utilidades para las fotos de las anotaciones y sus imágenes predeterminadas. */
public final class ImagenUtil {

    private static final String DIR_FOTOS = "fotos";

    private ImagenUtil() { }

    /** Imagen predeterminada según el tipo (se usa cuando no hay foto). */
    @DrawableRes
    public static int drawablePorTipo(String tipo) {
        if (Evento.TIPO_CUMPLEANOS.equals(tipo)) return R.drawable.img_predeterminada_cumpleanos;
        if (Evento.TIPO_EVENTO.equals(tipo)) return R.drawable.img_predeterminada_evento;
        return R.drawable.img_predeterminada_otro;
    }

    /** Crea un archivo vacío dentro de /files/fotos/ para guardar una foto. */
    public static File crearArchivoFoto(Context ctx) throws IOException {
        File dir = new File(ctx.getFilesDir(), DIR_FOTOS);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de fotos");
        }
        File archivo = new File(dir, "foto_" + System.currentTimeMillis() + ".jpg");
        if (!archivo.createNewFile()) {
            throw new IOException("No se pudo crear el archivo de la foto");
        }
        return archivo;
    }

    /**
     * Decodifica la imagen (respetando la rotación EXIF), la reduce a un máximo de
     * {@code maxLado} píxeles y la guarda como JPEG. Evita listas lentas y falta de memoria.
     */
    public static boolean guardarReducida(ImageDecoder.Source origen, File destino, int maxLado) {
        try {
            Bitmap bmp = ImageDecoder.decodeBitmap(origen, (decoder, info, src) -> {
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                Size t = info.getSize();
                int mayor = Math.max(t.getWidth(), t.getHeight());
                if (mayor > maxLado) {
                    float f = (float) maxLado / mayor;
                    decoder.setTargetSize(
                            Math.max(1, Math.round(t.getWidth() * f)),
                            Math.max(1, Math.round(t.getHeight() * f)));
                }
            });
            try (FileOutputStream out = new FileOutputStream(destino)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** Convierte el texto guardado en el Evento (file://...) en un File; null si no aplica. */
    @Nullable
    public static File archivoDesdeUri(@Nullable String uri) {
        if (uri == null || uri.isEmpty()) return null;
        Uri u = Uri.parse(uri);
        if ("file".equals(u.getScheme()) && u.getPath() != null) {
            return new File(u.getPath());
        }
        return null;
    }

    /** ¿La foto existe realmente en el almacenamiento de la app? */
    public static boolean existeFoto(@Nullable String uri) {
        File f = archivoDesdeUri(uri);
        return f != null && f.exists();
    }

    /** Uri content:// segura para compartir la foto con otra app (correo, etc.). */
    @Nullable
    public static Uri uriParaCompartir(Context ctx, @Nullable String uri) {
        File f = archivoDesdeUri(uri);
        if (f == null || !f.exists()) return null;
        return FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", f);
    }

    /** Borra una foto guardada por la app (si existe). */
    public static void borrar(@Nullable String uri) {
        File f = archivoDesdeUri(uri);
        if (f != null && f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
