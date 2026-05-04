package huffman;

import java.util.Map;

/**
 * Estadísticas de compresión Huffman.
 *
 * Genera el reporte comparativo que pide el enunciado:
 *   ORIGINAL vs COMPACTADO vs DESCOMPACTADO
 */
public class HuffmanUtils {

    // =========================================================================
    // MODELO DE DATOS
    // =========================================================================

    public final int    bytesOriginal;
    public final int    bytesComprimido;
    public final int    bytesDescomprimido;
    public final double tasaCompresion;      // % de reducción respecto al original
    public final double ratioCompresion;     // original / comprimido
    public final boolean descomprimidoIgualOriginal;

    private final Map<Character, String>  codigos;
    private final Map<Character, Integer> frecuencias;
    private final String                  textoOriginal;

    public HuffmanUtils(byte[] bytesOrig, byte[] bytesComp, byte[] bytesDecomp,
                        String textoOriginal,
                        Map<Character, String>  codigos,
                        Map<Character, Integer> frecuencias) {

        this.bytesOriginal          = bytesOrig  != null ? bytesOrig.length  : 0;
        this.bytesComprimido        = bytesComp  != null ? bytesComp.length  : 0;
        this.bytesDescomprimido     = bytesDecomp!= null ? bytesDecomp.length: 0;
        this.textoOriginal          = textoOriginal;
        this.codigos                = codigos;
        this.frecuencias            = frecuencias;

        // Tasa de compresión: cuánto se redujo
        this.tasaCompresion  = this.bytesOriginal > 0
                ? (1.0 - (double) this.bytesComprimido / this.bytesOriginal) * 100
                : 0;

        // Ratio: cuántas veces más pesaba el original
        this.ratioCompresion = this.bytesComprimido > 0
                ? (double) this.bytesOriginal / this.bytesComprimido
                : 0;

        // Verificar que la descompresión recuperó el original exactamente
        this.descomprimidoIgualOriginal =
                bytesOrig != null && bytesDecomp != null &&
                        java.util.Arrays.equals(bytesOrig, bytesDecomp);
    }

    // =========================================================================
    // REPORTE EN TEXTO — para el log de la GUI
    // =========================================================================

    /**
     * Genera el reporte completo de estadísticas como String multilinea.
     * Se muestra en el panel de log o en una ventana de estadísticas.
     */
    public String generarReporte() {
        StringBuilder sb = new StringBuilder();

        sb.append("══════════════════════════════════════\n");
        sb.append("  ESTADÍSTICAS DE COMPRESIÓN HUFFMAN  \n");
        sb.append("══════════════════════════════════════\n\n");

        // Tamaños
        sb.append(String.format("  Original      : %,d bytes\n", bytesOriginal));
        sb.append(String.format("  Comprimido    : %,d bytes\n", bytesComprimido));
        sb.append(String.format("  Descomprimido : %,d bytes\n\n", bytesDescomprimido));

        // Compresión
        sb.append(String.format("  Reducción     : %.1f%%\n", tasaCompresion));
        sb.append(String.format("  Ratio         : %.2f:1\n\n", ratioCompresion));

        // Verificación
        sb.append("  Descomprimido = Original : ")
                .append(descomprimidoIgualOriginal ? "SÍ ✓" : "NO ✗")
                .append("\n\n");

        // Tabla de códigos
        if (codigos != null && !codigos.isEmpty()) {
            sb.append("  TABLA DE CÓDIGOS (top 10 por frecuencia):\n");
            sb.append("  ─────────────────────────────────────────\n");
            sb.append(String.format("  %-8s %-8s %-12s %s\n",
                    "Char", "Freq", "Bits orig.", "Código Huffman"));
            sb.append("  ─────────────────────────────────────────\n");

            int count = 0;
            for (Map.Entry<Character, String> e : codigos.entrySet()) {
                if (count++ >= 10) break;
                char c    = e.getKey();
                String cod = e.getValue();
                int freq   = frecuencias.getOrDefault(c, 0);
                String charDisplay = c == '\n' ? "\\n"
                        : c == '\r' ? "\\r"
                          : c == '\t' ? "\\t"
                            : c == ' '  ? "SP"
                              : String.valueOf(c);
                sb.append(String.format("  %-8s %-8d %-12s %s\n",
                        charDisplay, freq, "8 → " + cod.length(), cod));
            }
            if (codigos.size() > 10) {
                sb.append(String.format("  ... y %d caracteres más\n", codigos.size() - 10));
            }
        }

        sb.append("══════════════════════════════════════\n");
        return sb.toString();
    }

    /**
     * Versión corta para el log inferior de la GUI (una sola línea).
     */
    public String resumenCorto() {
        return String.format(
                "Compresión: %,d → %,d bytes (%.1f%% reducción, ratio %.2f:1) | Recuperado: %s",
                bytesOriginal, bytesComprimido, tasaCompresion, ratioCompresion,
                descomprimidoIgualOriginal ? "OK" : "ERROR"
        );
    }

    // =========================================================================
    // GETTERS para la GUI (panel de estadísticas visual)
    // =========================================================================

    public Map<Character, String>  getCodigos()     { return codigos; }
    public Map<Character, Integer> getFrecuencias()  { return frecuencias; }
    public String                  getTextoOriginal(){ return textoOriginal; }
}
