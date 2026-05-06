package huffman;

import java.io.*;
import java.util.*;

public class Huffman {

    // CLASE INTERNA: NODO DEL ÁRBOL

    private static class HuffmanNode implements Comparable<HuffmanNode> {
        char        caracter;
        int         frecuencia;
        HuffmanNode izquierdo;
        HuffmanNode derecho;

        HuffmanNode(char caracter, int frecuencia) {
            this.caracter   = caracter;
            this.frecuencia = frecuencia;
        }

        HuffmanNode(HuffmanNode izq, HuffmanNode der) {
            this.caracter   = '\0';
            this.frecuencia = izq.frecuencia + der.frecuencia;
            this.izquierdo  = izq;
            this.derecho    = der;
        }

        boolean esHoja() {
            return izquierdo == null && derecho == null;
        }

        @Override
        public int compareTo(HuffmanNode otro) {
            return Integer.compare(this.frecuencia, otro.frecuencia);
        }
    }

    // ENCODE

    public static boolean encode(byte[] datos, String pathSalida) {
        if (datos == null || datos.length == 0) return false;

        String texto = new String(datos);

        Map<Character, Integer> frecuencias = contarFrecuencias(texto);
        if (frecuencias.size() == 1) {
            char unico = frecuencias.keySet().iterator().next();
            frecuencias.put(unico == 'a' ? 'b' : 'a', 0);
        }
        HuffmanNode raiz = construirArbol(frecuencias);
        Map<Character, String> codigos = new HashMap<>();
        generarCodigos(raiz, "", codigos);

        StringBuilder bitStream = new StringBuilder();
        for (char c : texto.toCharArray()) {
            bitStream.append(codigos.get(c));
        }

        String bits       = bitStream.toString();
        int bitesUtiles   = bits.length() % 8;
        if (bitesUtiles == 0 && bits.length() > 0) bitesUtiles = 8;
        while (bits.length() % 8 != 0) bits += "0";

        byte[] streamBytes = new byte[bits.length() / 8];
        for (int i = 0; i < streamBytes.length; i++) {
            streamBytes[i] = (byte) Integer.parseInt(bits.substring(i * 8, i * 8 + 8), 2);
        }

        return escribirHuf(pathSalida, frecuencias, bitesUtiles, streamBytes);
    }

    // DECODE

    public static byte[] decode(byte[] datos) {
        if (datos == null || datos.length < 8) return null;

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(datos))) {

            int offsetTabla = leerOffsetTabla(datos);
            if (offsetTabla < 4 || offsetTabla >= datos.length) {
                System.out.println("Error: offset de tabla inválido: " + offsetTabla);
                return null;
            }

            int bitesUtiles = dis.readInt();

            int longitudStream = offsetTabla - 4;
            if (longitudStream <= 0) return null;
            byte[] streamBytes = new byte[longitudStream];
            dis.readFully(streamBytes);

            Map<Character, Integer> frecuencias = leerTabla(datos, offsetTabla);
            if (frecuencias == null) return null;

            HuffmanNode raiz   = construirArbol(frecuencias);
            HuffmanNode actual = raiz;
            StringBuilder resultado = new StringBuilder();

            int totalBytes = streamBytes.length;
            for (int i = 0; i < totalBytes; i++) {
                int bitsEnEsteByte = (i == totalBytes - 1) ? bitesUtiles : 8;
                for (int b = 7; b >= 8 - bitsEnEsteByte; b--) {
                    int bit = (streamBytes[i] >> b) & 1;
                    actual  = (bit == 0) ? actual.izquierdo : actual.derecho;
                    if (actual != null && actual.esHoja()) {
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

    // HELPERS DE ESCRITURA / LECTURA

    private static boolean escribirHuf(String pathSalida,
                                       Map<Character, Integer> frecuencias,
                                       int bitesUtiles,
                                       byte[] streamBytes) {
        try (DataOutputStream dos = new DataOutputStream(
                new FileOutputStream(pathSalida))) {


            dos.writeInt(bitesUtiles);
            dos.write(streamBytes);

            int offsetTabla = 4 + streamBytes.length;

            dos.writeInt(frecuencias.size());
            for (Map.Entry<Character, Integer> e : frecuencias.entrySet()) {
                dos.writeChar(e.getKey());   // 2 bytes UTF-16
                dos.writeInt(e.getValue());  // 4 bytes
            }

            dos.writeInt(offsetTabla);

            return true;

        } catch (IOException e) {
            System.out.println("Error al escribir .huf: " + e.getMessage());
            return false;
        }
    }

    private static int leerOffsetTabla(byte[] datos) {
        int pos = datos.length - 4;
        return ((datos[pos]     & 0xFF) << 24) |
                ((datos[pos + 1] & 0xFF) << 16) |
                ((datos[pos + 2] & 0xFF) <<  8) |
                ((datos[pos + 3] & 0xFF));
    }

    private static Map<Character, Integer> leerTabla(byte[] datos, int offsetTabla) {
        try (DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(datos, offsetTabla, datos.length - offsetTabla - 4))) {

            int cantEntradas = dis.readInt();
            Map<Character, Integer> frecuencias = new LinkedHashMap<>();
            for (int i = 0; i < cantEntradas; i++) {
                char c   = dis.readChar();   // 2 bytes UTF-16
                int freq = dis.readInt();    // 4 bytes
                frecuencias.put(c, freq);
            }
            return frecuencias;

        } catch (IOException e) {
            System.out.println("Error al leer tabla: " + e.getMessage());
            return null;
        }
    }

    // HELPERS DEL ÁRBOL
    private static Map<Character, Integer> contarFrecuencias(String texto) {
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : texto.toCharArray()) freq.merge(c, 1, Integer::sum);
        return freq;
    }

    private static HuffmanNode construirArbol(Map<Character, Integer> frecuencias) {
        PriorityQueue<HuffmanNode> cola = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> e : frecuencias.entrySet()) {
            cola.add(new HuffmanNode(e.getKey(), e.getValue()));
        }
        while (cola.size() > 1) {
            cola.add(new HuffmanNode(cola.poll(), cola.poll()));
        }
        return cola.poll();
    }

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

    // UTILIDADES PÚBLICAS

    public static Map<Character, Integer> leerFrecuenciasDeHuf(byte[] datosHuf) {
        if (datosHuf == null || datosHuf.length < 8) return null;
        int offsetTabla = leerOffsetTabla(datosHuf);
        return leerTabla(datosHuf, offsetTabla);
    }

    public static Map<Character, String> obtenerCodigos(String texto) {
        Map<Character, Integer> frecuencias = contarFrecuencias(texto);
        if (frecuencias.size() == 1) {
            char unico = frecuencias.keySet().iterator().next();
            frecuencias.put(unico == 'a' ? 'b' : 'a', 0);
        }
        HuffmanNode raiz = construirArbol(frecuencias);
        Map<Character, String> codigos = new LinkedHashMap<>();
        generarCodigos(raiz, "", codigos);

        List<Map.Entry<Character, Integer>> lista = new ArrayList<>(frecuencias.entrySet());
        lista.sort((a, b) -> b.getValue() - a.getValue());

        Map<Character, String> ordenado = new LinkedHashMap<>();
        for (Map.Entry<Character, Integer> e : lista) {
            if (codigos.containsKey(e.getKey())) ordenado.put(e.getKey(), codigos.get(e.getKey()));
        }
        return ordenado;
    }

    public static Map<Character, Integer> obtenerFrecuencias(String texto) {
        return contarFrecuencias(texto);
    }

    public static long calcularBitsComprimidos(String texto, Map<Character, String> codigos) {
        long total = 0;
        for (char c : texto.toCharArray()) {
            if (codigos.containsKey(c)) total += codigos.get(c).length();
        }
        return total;
    }
}