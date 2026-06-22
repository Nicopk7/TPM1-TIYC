package hamming.file_mngmt;

import org.apache.tika.Tika;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class LectorDocumentos {

    private static final Tika tika = new Tika();

    /**
     * Extrae el texto limpio de cualquier archivo soportado para mostrarlo en la GUI.
     * @param nombreOriginal El nombre base del archivo (ej: "documento.pdf")
     * @param datosCrudos Los bytes decodificados listos para leerse
     */
    public static String extraerTextoParaVista(String nombreOriginal, byte[] datosCrudos) {
        String lower = nombreOriginal.toLowerCase();

        try {
            // Si es TXT, leemos los bytes directamente sin Tika
            if (lower.endsWith(".txt")) {
                return new String(datosCrudos);
            }

            // Si es un documento complejo, delegamos a Tika leyendo desde memoria
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

    /**
     * Verifica si la extensión corresponde a un documento de texto legible por la UI.
     */
    public static boolean esFormatoSoportado(String nombre) {
        String lower = nombre.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".doc") ||
                lower.endsWith(".docx") || lower.endsWith(".wp") ||
                lower.endsWith(".pdf");
    }
}