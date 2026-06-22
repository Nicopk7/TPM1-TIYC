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

    private final Map<Byte, String>  codigos;
    private final Map<Byte, Integer> frecuencias;
    private final byte[]             bytesOriginalData;

    public HuffmanUtils(byte[] bytesOrig, byte[] bytesComp, byte[] bytesDecomp,
                        Map<Byte, String>  codigos,
                        Map<Byte, Integer> frecuencias) {

        this.bytesOriginal          = bytesOrig   != null ? bytesOrig.length   : 0;
        this.bytesComprimido        = bytesComp   != null ? bytesComp.length   : 0;
        this.bytesDescomprimido     = bytesDecomp != null ? bytesDecomp.length : 0;
        this.bytesOriginalData      = bytesOrig;
        this.codigos                = codigos;
        this.frecuencias            = frecuencias;

        this.tasaCompresion  = this.bytesOriginal > 0 ? (1.0 - (double) this.bytesComprimido / this.bytesOriginal) * 100 : 0;
        this.ratioCompresion = this.bytesComprimido > 0 ? (double) this.bytesOriginal / this.bytesComprimido : 0;
        this.descomprimidoIgualOriginal = bytesOrig != null && bytesDecomp != null && java.util.Arrays.equals(bytesOrig, bytesDecomp);
    }

    public String generarReporte() {
        StringBuilder sb = new StringBuilder();

        sb.append("══════════════════════════════════════════════\n");
        sb.append("     ESTADÍSTICAS DE COMPRESIÓN HUFFMAN       \n");
        sb.append("══════════════════════════════════════════════\n\n");

        sb.append("  TAMAÑOS DE ARCHIVO\n  ──────────────────────────────────────────\n");
        sb.append(String.format("  %-20s : %,10d bytes\n", "Original",       bytesOriginal));
        sb.append(String.format("  %-20s : %,10d bytes\n", "Compactado",      bytesComprimido));
        sb.append(String.format("  %-20s : %,10d bytes\n", "Descompactado",   bytesDescomprimido));
        sb.append("\n");

        sb.append("  COMPARACIÓN VISUAL\n  ──────────────────────────────────────────\n");
        sb.append(generarBarras()).append("\n");

        sb.append("  MÉTRICAS\n  ──────────────────────────────────────────\n");
        sb.append(String.format("  Reducción de tamaño  : %.2f%%\n",  tasaCompresion));
        sb.append(String.format("  Ratio de compresión  : %.3f:1\n", ratioCompresion));
        sb.append(String.format("  Ahorro               : %,d bytes\n", bytesOriginal - bytesComprimido));
        sb.append(String.format("  Descomprimido = Orig : %s\n\n", descomprimidoIgualOriginal ? "SÍ ✓" : "NO ✗"));

        if (codigos != null && !codigos.isEmpty()) {
            sb.append("  TABLA DE FRECUENCIAS Y CÓDIGOS HUFFMAN\n  ──────────────────────────────────────────\n");
            sb.append(String.format("  %-6s  %-8s  %-6s  %-6s  %s\n", "Byte", "Frec.", "Bits", "Bits→", "Código"));
            sb.append("  ──────────────────────────────────────────\n");

            List<Map.Entry<Byte, String>> lista = new ArrayList<>(codigos.entrySet());
            lista.sort((a, b) -> Integer.compare(frecuencias.getOrDefault(b.getKey(), 0), frecuencias.getOrDefault(a.getKey(), 0)));

            for (Map.Entry<Byte, String> e : lista) {
                byte   b    = e.getKey();
                String cod  = e.getValue();
                int    freq = frecuencias.getOrDefault(b, 0);

                // Mostramos el carácter si es legible (ASCII), de lo contrario su representación Hexadecimal
                char c = (char) (b & 0xFF);
                String charDisplay;
                if (c == '\n') charDisplay = "\\n";
                else if (c == '\r') charDisplay = "\\r";
                else if (c == '\t') charDisplay = "\\t";
                else if (c == ' ') charDisplay = "SP";
                else if (c >= 33 && c <= 126) charDisplay = String.valueOf(c);
                else charDisplay = String.format("0x%02X", b);

                sb.append(String.format("  %-6s  %-8d  %-6d  %-6d  %s\n", charDisplay, freq, 8, cod.length(), cod));
            }

            long bitsOrig = (long) bytesOriginal * 8;
            long bitsComp = Huffman.calcularBitsComprimidos(bytesOriginalData, codigos);
            sb.append("  ──────────────────────────────────────────\n");
            sb.append(String.format("  Bits originales      : %,d\n", bitsOrig));
            sb.append(String.format("  Bits comprimidos     : %,d\n", bitsComp));
            sb.append(String.format("  Reducción en bits    : %.2f%%\n", bitsOrig > 0 ? (1.0 - (double) bitsComp / bitsOrig) * 100 : 0));
        }
        sb.append("══════════════════════════════════════════════\n");
        return sb.toString();
    }

    private String generarBarras() {
        int maxAncho = 36;
        int maxBytes = Math.max(bytesOriginal, Math.max(bytesComprimido, bytesDescomprimido));
        if (maxBytes == 0) return "  (sin datos)\n";

        String[] etiquetas = {"Original     ", "Compactado   ", "Descompactado"};
        int[]    valores   = {bytesOriginal, bytesComprimido, bytesDescomprimido};
        StringBuilder sb = new StringBuilder();

        /*for (int i = 0; i < 3; i++) {
            double porcentaje = (double) valores[i] / maxBytes;
            int    bloques    = (int) Math.round(porcentaje * maxAncho);
            sb.append("  ").append(etiquetas[i]).append("  [");
            sb.append("█".repeat(bloques)).append("░".repeat(maxAncho - bloques)).append("]");
            sb.append(String.format("  %5.1f%%  %,d bytes\n", porcentaje * 100, valores[i]));
        }*/
        return sb.toString();
    }

    public String resumenCorto() {
        return String.format("Huffman: %,d → %,d bytes (%.1f%% reducción, ratio %.2f:1) | Recuperado: %s",
                bytesOriginal, bytesComprimido, tasaCompresion, ratioCompresion, descomprimidoIgualOriginal ? "OK ✓" : "ERROR ✗");
    }
}