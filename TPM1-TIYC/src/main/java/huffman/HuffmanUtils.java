package huffman;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;


public class HuffmanUtils {

    public final int     bytesOriginal;
    public final int     bytesComprimido;
    public final int     bytesDescomprimido;
    public final double  tasaCompresion;
    public final double  ratioCompresion;
    public final boolean descomprimidoIgualOriginal;

    private final Map<Character, String>  codigos;
    private final Map<Character, Integer> frecuencias;
    private final String                  textoOriginal;

    public HuffmanUtils(byte[] bytesOrig, byte[] bytesComp, byte[] bytesDecomp,
                        String textoOriginal,
                        Map<Character, String>  codigos,
                        Map<Character, Integer> frecuencias) {

        this.bytesOriginal          = bytesOrig   != null ? bytesOrig.length   : 0;
        this.bytesComprimido        = bytesComp   != null ? bytesComp.length   : 0;
        this.bytesDescomprimido     = bytesDecomp != null ? bytesDecomp.length : 0;
        this.textoOriginal          = textoOriginal;
        this.codigos                = codigos;
        this.frecuencias            = frecuencias;

        this.tasaCompresion  = this.bytesOriginal > 0
                ? (1.0 - (double) this.bytesComprimido / this.bytesOriginal) * 100 : 0;
        this.ratioCompresion = this.bytesComprimido > 0
                ? (double) this.bytesOriginal / this.bytesComprimido : 0;
        this.descomprimidoIgualOriginal =
                bytesOrig != null && bytesDecomp != null &&
                        java.util.Arrays.equals(bytesOrig, bytesDecomp);
    }

    // REPORTE COMPLETO

    public String generarReporte() {
        StringBuilder sb = new StringBuilder();

        sb.append("══════════════════════════════════════════════\n");
        sb.append("     ESTADÍSTICAS DE COMPRESIÓN HUFFMAN       \n");
        sb.append("══════════════════════════════════════════════\n\n");

        // ── Tamaños ──────────────────────────────────────────────────────────
        sb.append("  TAMAÑOS DE ARCHIVO\n");
        sb.append("  ──────────────────────────────────────────\n");
        sb.append(String.format("  %-20s : %,10d bytes\n", "Original",       bytesOriginal));
        sb.append(String.format("  %-20s : %,10d bytes\n", "Compactado",      bytesComprimido));
        sb.append(String.format("  %-20s : %,10d bytes\n", "Descompactado",   bytesDescomprimido));
        sb.append("\n");

        // ── Gráfico de barras ASCII ───────────────────────────────────────────
        sb.append("  COMPARACIÓN VISUAL\n");
        sb.append("  ──────────────────────────────────────────\n");
        sb.append(generarBarras());
        sb.append("\n");

        // ── Métricas ─────────────────────────────────────────────────────────
        sb.append("  MÉTRICAS\n");
        sb.append("  ──────────────────────────────────────────\n");
        sb.append(String.format("  Reducción de tamaño  : %.2f%%\n",  tasaCompresion));
        sb.append(String.format("  Ratio de compresión  : %.3f:1\n", ratioCompresion));
        sb.append(String.format("  Ahorro               : %,d bytes\n",
                bytesOriginal - bytesComprimido));
        sb.append(String.format("  Descomprimido = Orig : %s\n\n",
                descomprimidoIgualOriginal ? "SÍ ✓" : "NO ✗"));

        // ── Tabla de frecuencias y códigos ────────────────────────────────────
        if (codigos != null && !codigos.isEmpty()) {
            sb.append("  TABLA DE FRECUENCIAS Y CÓDIGOS HUFFMAN\n");
            sb.append("  ──────────────────────────────────────────\n");
            sb.append(String.format("  %-6s  %-8s  %-6s  %-6s  %s\n",
                    "Char", "Frec.", "Bits", "Bits→", "Código"));
            sb.append("  ──────────────────────────────────────────\n");

            // Ordenar por frecuencia descendente
            List<Map.Entry<Character, String>> lista = new ArrayList<>(codigos.entrySet());
            lista.sort((a, b) -> {
                int fa = frecuencias.getOrDefault(a.getKey(), 0);
                int fb = frecuencias.getOrDefault(b.getKey(), 0);
                return Integer.compare(fb, fa);
            });

            for (Map.Entry<Character, String> e : lista) {
                char   c    = e.getKey();
                String cod  = e.getValue();
                int    freq = frecuencias.getOrDefault(c, 0);

                String charDisplay = c == '\n' ? "\\n"
                        : c == '\r' ? "\\r"
                          : c == '\t' ? "\\t"
                            : c == ' '  ? "SP"
                              : String.valueOf(c);

                sb.append(String.format("  %-6s  %-8d  %-6d  %-6d  %s\n",
                        charDisplay, freq, 8, cod.length(), cod));
            }

            // ── Bits totales ─────────────────────────────────────────────────
            long bitsOrig = (long) bytesOriginal * 8;
            long bitsComp = Huffman.calcularBitsComprimidos(
                    textoOriginal, codigos);
            sb.append("  ──────────────────────────────────────────\n");
            sb.append(String.format("  Bits originales      : %,d\n", bitsOrig));
            sb.append(String.format("  Bits comprimidos     : %,d\n", bitsComp));
            sb.append(String.format("  Reducción en bits    : %.2f%%\n",
                    bitsOrig > 0 ? (1.0 - (double) bitsComp / bitsOrig) * 100 : 0));
        }

        sb.append("══════════════════════════════════════════════\n");
        return sb.toString();
    }


    // GRÁFICO DE BARRAS ASCII
    private String generarBarras() {
        int maxAncho = 36; // caracteres de la barra
        int maxBytes = Math.max(bytesOriginal, Math.max(bytesComprimido, bytesDescomprimido));
        if (maxBytes == 0) return "  (sin datos)\n";

        String[] etiquetas = {"Original     ", "Compactado   ", "Descompactado"};
        int[]    valores   = {bytesOriginal, bytesComprimido, bytesDescomprimido};

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            double porcentaje = (double) valores[i] / maxBytes;
            int    bloques    = (int) Math.round(porcentaje * maxAncho);
            int    vacios     = maxAncho - bloques;

            sb.append("  ").append(etiquetas[i]).append("  [");
            sb.append("█".repeat(bloques));
            sb.append("░".repeat(vacios));
            sb.append("]");
            sb.append(String.format("  %5.1f%%  %,d bytes\n",
                    porcentaje * 100, valores[i]));
        }
        return sb.toString();
    }

    // RESUMEN CORTO — para el log inferior de la GUI


    public String resumenCorto() {
        return String.format(
                "Huffman: %,d → %,d bytes (%.1f%% reducción, ratio %.2f:1) | Recuperado: %s",
                bytesOriginal, bytesComprimido,
                tasaCompresion, ratioCompresion,
                descomprimidoIgualOriginal ? "OK ✓" : "ERROR ✗"
        );
    }

    // GETTERS

    public Map<Character, String>  getCodigos()      { return codigos; }
    public Map<Character, Integer> getFrecuencias()   { return frecuencias; }
    public String                  getTextoOriginal() { return textoOriginal; }
}