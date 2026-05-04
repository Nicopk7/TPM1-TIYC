package huffman;

import java.io.*;
import java.util.*;

/**
 * Codec Huffman — compresión y descompresión en un solo archivo.
 *
 * Internamente define HuffmanNode como clase privada estática.
 *
 * ── ESTRUCTURA DEL ARCHIVO .huf ─────────────────────────────────────────────
 *   [4 bytes]  cantidad de entradas en la tabla de frecuencias (int)
 *   [N * 5 bytes] por cada entrada: [1 byte char][4 bytes frecuencia int]
 *   [4 bytes]  cantidad de bits útiles en el último byte del stream
 *   [resto]    stream de bits comprimido (bytes)
 *
 * ── FLUJO ENCODE ────────────────────────────────────────────────────────────
 *   1. contarFrecuencias()   → Map<Character, Integer> con freq de cada char
 *   2. construirArbol()      → árbol Huffman con PriorityQueue
 *   3. generarCodigos()      → Map<Character, String> char → "0110..."
 *   4. codificarStream()     → bits → bytes empaquetados
 *   5. escribirArchivo()     → encabezado + stream → .huf
 *
 * ── FLUJO DECODE ────────────────────────────────────────────────────────────
 *   1. leerEncabezado()      → reconstruir tabla de frecuencias
 *   2. construirArbol()      → mismo árbol a partir de las frecuencias
 *   3. decodificarStream()   → recorrer árbol bit a bit → chars → .dhu
 */
public class Huffman {

    // =========================================================================
    // CLASE INTERNA: NODO DEL ÁRBOL
    // =========================================================================

    private static class HuffmanNode implements Comparable<HuffmanNode> {
        char  caracter;
        int   frecuencia;
        HuffmanNode izquierdo;
        HuffmanNode derecho;

        /** Nodo hoja — representa un carácter con su frecuencia */
        HuffmanNode(char caracter, int frecuencia) {
            this.caracter   = caracter;
            this.frecuencia = frecuencia;
        }

        /** Nodo interno — fusión de dos nodos, sin carácter propio */
        HuffmanNode(HuffmanNode izq, HuffmanNode der) {
            this.caracter   = '\0';
            this.frecuencia = izq.frecuencia + der.frecuencia;
            this.izquierdo  = izq;
            this.derecho    = der;
        }

        boolean esHoja() {
            return izquierdo == null && derecho == null;
        }

        /** La PriorityQueue ordena por menor frecuencia primero */
        @Override
        public int compareTo(HuffmanNode otro) {
            return Integer.compare(this.frecuencia, otro.frecuencia);
        }
    }

    // =========================================================================
    // ENCODE — comprime un archivo y genera el .huf
    // =========================================================================

    /**
     * Comprime el contenido de un archivo de texto y lo guarda como .huf.
     *
     * @param datos      Bytes del archivo original
     * @param pathSalida Path donde guardar el .huf
     * @return           true si la compresión fue exitosa
     */
    public static boolean encode(byte[] datos, String pathSalida) {
        if (datos == null || datos.length == 0) return false;

        String texto = new String(datos);

        // 1. Tabla de frecuencias
        Map<Character, Integer> frecuencias = contarFrecuencias(texto);

        // Caso borde: archivo con un solo carácter único
        if (frecuencias.size() == 1) {
            char unico = frecuencias.keySet().iterator().next();
            frecuencias.put(unico == 'a' ? 'b' : 'a', 0);
        }

        // 2. Árbol Huffman
        HuffmanNode raiz = construirArbol(frecuencias);

        // 3. Tabla de códigos
        Map<Character, String> codigos = new HashMap<>();
        generarCodigos(raiz, "", codigos);

        // 4. Codificar el stream de bits
        StringBuilder bitStream = new StringBuilder();
        for (char c : texto.toCharArray()) {
            bitStream.append(codigos.get(c));
        }

        // 5. Empaquetar bits en bytes
        String bits       = bitStream.toString();
        int bitesUtiles   = bits.length() % 8;
        if (bitesUtiles == 0 && bits.length() > 0) bitesUtiles = 8;

        // Rellenar hasta múltiplo de 8
        while (bits.length() % 8 != 0) bits += "0";

        byte[] streamBytes = new byte[bits.length() / 8];
        for (int i = 0; i < streamBytes.length; i++) {
            String byteStr = bits.substring(i * 8, i * 8 + 8);
            streamBytes[i] = (byte) Integer.parseInt(byteStr, 2);
        }

        // 6. Escribir archivo .huf
        return escribirHuf(pathSalida, frecuencias, bitesUtiles, streamBytes);
    }

    // =========================================================================
    // DECODE — descomprime un .huf y genera el .dhu
    // =========================================================================

    /**
     * Descomprime un archivo .huf y devuelve los bytes del contenido original.
     *
     * @param datos Bytes del archivo .huf
     * @return      Bytes del archivo descomprimido, o null si hubo error
     */
    public static byte[] decode(byte[] datos) {
        if (datos == null || datos.length == 0) return null;

        try (DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(datos))) {

            // 1. Leer encabezado — tabla de frecuencias
            int cantEntradas = dis.readInt();
            Map<Character, Integer> frecuencias = new LinkedHashMap<>();
            for (int i = 0; i < cantEntradas; i++) {
                char c   = (char) dis.readByte();
                int freq = dis.readInt();
                frecuencias.put(c, freq);
            }

            // 2. Bits útiles en el último byte
            int bitesUtiles = dis.readInt();

            // 3. Leer el stream comprimido
            byte[] streamBytes = dis.readAllBytes();

            // 4. Reconstruir el árbol
            HuffmanNode raiz = construirArbol(frecuencias);

            // 5. Decodificar
            StringBuilder resultado = new StringBuilder();
            HuffmanNode actual = raiz;

            int totalBytes = streamBytes.length;
            for (int i = 0; i < totalBytes; i++) {
                int bitsEnEsteByte = (i == totalBytes - 1) ? bitesUtiles : 8;
                for (int b = 7; b >= 8 - bitsEnEsteByte; b--) {
                    int bit = (streamBytes[i] >> b) & 1;
                    actual = (bit == 0) ? actual.izquierdo : actual.derecho;
                    if (actual.esHoja()) {
                        resultado.append(actual.caracter);
                        actual = raiz;
                    }
                }
            }

            return resultado.toString().getBytes();

        } catch (IOException e) {
            System.out.println("Error al decodificar: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // HELPERS INTERNOS
    // =========================================================================

    /** Cuenta cuántas veces aparece cada carácter en el texto */
    private static Map<Character, Integer> contarFrecuencias(String texto) {
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : texto.toCharArray()) {
            freq.merge(c, 1, Integer::sum);
        }
        return freq;
    }

    /**
     * Construye el árbol Huffman usando una PriorityQueue (min-heap).
     *
     * Algoritmo:
     *   1. Crear un nodo hoja por cada carácter
     *   2. Extraer los dos nodos con menor frecuencia
     *   3. Fusionarlos en un nodo interno (freq = suma de ambos)
     *   4. Reinsertar el nodo fusionado
     *   5. Repetir hasta que quede un solo nodo (la raíz)
     */
    private static HuffmanNode construirArbol(Map<Character, Integer> frecuencias) {
        PriorityQueue<HuffmanNode> cola = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> e : frecuencias.entrySet()) {
            cola.add(new HuffmanNode(e.getKey(), e.getValue()));
        }
        while (cola.size() > 1) {
            HuffmanNode izq = cola.poll();
            HuffmanNode der = cola.poll();
            cola.add(new HuffmanNode(izq, der));
        }
        return cola.poll();
    }

    /**
     * Recorre el árbol recursivamente asignando códigos binarios.
     * Cada vez que bajamos por la izquierda agregamos "0", por la derecha "1".
     */
    private static void generarCodigos(HuffmanNode nodo, String codigo,
                                       Map<Character, String> codigos) {
        if (nodo == null) return;
        if (nodo.esHoja()) {
            codigos.put(nodo.caracter, codigo.isEmpty() ? "0" : codigo);
            return;
        }
        generarCodigos(nodo.izquierdo, codigo + "0", codigos);
        generarCodigos(nodo.derecho,   codigo + "1", codigos);
    }

    /**
     * Escribe el archivo .huf con encabezado + stream comprimido.
     *
     * Formato:
     *   [int] cantidad de entradas
     *   [byte + int] × N  → carácter + frecuencia
     *   [int] bits útiles en el último byte
     *   [bytes] stream comprimido
     */
    private static boolean escribirHuf(String pathSalida,
                                       Map<Character, Integer> frecuencias,
                                       int bitesUtiles,
                                       byte[] streamBytes) {
        try (DataOutputStream dos = new DataOutputStream(
                new FileOutputStream(pathSalida))) {

            dos.writeInt(frecuencias.size());
            for (Map.Entry<Character, Integer> e : frecuencias.entrySet()) {
                dos.writeByte((byte) e.getKey().charValue());
                dos.writeInt(e.getValue());
            }
            dos.writeInt(bitesUtiles);
            dos.write(streamBytes);
            return true;

        } catch (IOException e) {
            System.out.println("Error al escribir .huf: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // UTILIDADES PÚBLICAS — para la GUI y las estadísticas
    // =========================================================================

    /**
     * Retorna la tabla de códigos de un texto sin comprimirlo.
     * Útil para mostrar en la GUI cuántos bits ocupa cada carácter.
     *
     * @param texto Contenido del archivo original
     * @return      Map con carácter → código binario, ordenado por frecuencia
     */
    public static Map<Character, String> obtenerCodigos(String texto) {
        Map<Character, Integer> frecuencias = contarFrecuencias(texto);
        if (frecuencias.size() == 1) {
            char unico = frecuencias.keySet().iterator().next();
            frecuencias.put(unico == 'a' ? 'b' : 'a', 0);
        }
        HuffmanNode raiz = construirArbol(frecuencias);
        Map<Character, String> codigos = new LinkedHashMap<>();
        generarCodigos(raiz, "", codigos);

        // Ordenar por frecuencia descendente para mostrar en la GUI
        List<Map.Entry<Character, Integer>> lista = new ArrayList<>(frecuencias.entrySet());
        lista.sort((a, b) -> b.getValue() - a.getValue());

        Map<Character, String> ordenado = new LinkedHashMap<>();
        for (Map.Entry<Character, Integer> e : lista) {
            if (codigos.containsKey(e.getKey())) {
                ordenado.put(e.getKey(), codigos.get(e.getKey()));
            }
        }
        return ordenado;
    }

    /**
     * Retorna la tabla de frecuencias de un texto.
     * Útil para mostrar en las estadísticas.
     */
    public static Map<Character, Integer> obtenerFrecuencias(String texto) {
        return contarFrecuencias(texto);
    }

    /**
     * Calcula el tamaño teórico en bits del texto comprimido.
     * Útil para calcular la tasa de compresión antes de escribir el archivo.
     *
     * @param texto    Contenido original
     * @param codigos  Tabla de códigos generada por obtenerCodigos()
     * @return         Cantidad de bits del stream comprimido
     */
    public static long calcularBitsComprimidos(String texto, Map<Character, String> codigos) {
        long total = 0;
        for (char c : texto.toCharArray()) {
            if (codigos.containsKey(c)) {
                total += codigos.get(c).length();
            }
        }
        return total;
    }
}