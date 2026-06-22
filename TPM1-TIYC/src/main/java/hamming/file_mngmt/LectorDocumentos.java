package hamming.file_mngmt;

import org.apache.tika.Tika;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class LectorDocumentos {

    private static final Tika tika = new Tika();


    public static String extraerTextoParaVista(String nombreOriginal, byte[] datosCrudos) {
        String lower = nombreOriginal.toLowerCase();

        try {
            if (lower.endsWith(".txt")) {
                return new String(datosCrudos);
            }

            if (esFormatoSoportado(lower)) {
                try (InputStream is = new ByteArrayInputStream(datosCrudos)) {
                    return tika.parseToString(is);
                }
            }

        } catch (Exception e) {
            return "[Error al extraer texto para visualización: " + e.getMessage() + "]\nNota: Si el archivo tiene errores introducidos (.HE/.DEx), es normal que la estructura del documento se corrompa y no se pueda leer.";
        }

        return "[archivo binario — " + datosCrudos.length + " bytes]";
    }

    public static boolean esFormatoSoportado(String nombre) {
        String lower = nombre.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".doc") ||
                lower.endsWith(".docx") || lower.endsWith(".wp") ||
                lower.endsWith(".pdf");
    }
}