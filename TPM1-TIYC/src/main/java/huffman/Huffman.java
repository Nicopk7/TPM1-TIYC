package huffman;

import java.io.*;
import java.util.*;

public class Huffman {

    // NODO DEL ÁRBOL: Ahora guarda un Byte en lugar de un char
    private static class HuffmanNode implements Comparable<HuffmanNode> {
        Byte        valor;
        int         frecuencia;
        HuffmanNode izquierdo;
        HuffmanNode derecho;

        HuffmanNode(Byte valor, int frecuencia) {
            this.valor      = valor;
            this.frecuencia = frecuencia;
        }

        HuffmanNode(HuffmanNode izq, HuffmanNode der) {
            this.valor      = null;
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

    public static boolean encode(byte[] datos, String pathSalida) {
        if (datos == null || datos.length == 0) return false;

        // Trabajamos con arreglos de bytes directamente
        Map<Byte, Integer> frecuencias = contarFrecuencias(datos);

        // Si el archivo está compuesto de un solo byte repetido, creamos un nodo extra para que funcione el árbol
        if (frecuencias.size() == 1) {
            byte unico = frecuencias.keySet().iterator().next();
            frecuencias.put((byte)(unico == 0 ? 1 : 0), 0);
        }

        HuffmanNode raiz = construirArbol(frecuencias);
        Map<Byte, String> codigos = new HashMap<>();
        generarCodigos(raiz, "", codigos);

        StringBuilder bitStream = new StringBuilder();
        for (byte b : datos) {
            bitStream.append(codigos.get(b));
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

    public static byte[] decode(byte[] datos) {
        if (datos == null || datos.length < 8) return null;

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(datos))) {

            int offsetTabla = leerOffsetTabla(datos);
            if (offsetTabla < 4 || offsetTabla >= datos.length) return null;

            int bitesUtiles = dis.readInt();
            int longitudStream = offsetTabla - 4;
            if (longitudStream <= 0) return null;

            byte[] streamBytes = new byte[longitudStream];
            dis.readFully(streamBytes);

            Map<Byte, Integer> frecuencias = leerTabla(datos, offsetTabla);
            if (frecuencias == null) return null;

            HuffmanNode raiz   = construirArbol(frecuencias);
            HuffmanNode actual = raiz;

            ByteArrayOutputStream resultado = new ByteArrayOutputStream();

            int totalBytes = streamBytes.length;
            for (int i = 0; i < totalBytes; i++) {
                int bitsEnEsteByte = (i == totalBytes - 1) ? bitesUtiles : 8;
                for (int b = 7; b >= 8 - bitsEnEsteByte; b--) {
                    int bit = (streamBytes[i] >> b) & 1;
                    actual  = (bit == 0) ? actual.izquierdo : actual.derecho;
                    if (actual != null && actual.esHoja()) {
                        resultado.write(actual.valor);
                        actual = raiz;
                    }
                }
            }

            return resultado.toByteArray();

        } catch (IOException e) {
            return null;
        }
    }

    private static boolean escribirHuf(String pathSalida, Map<Byte, Integer> frecuencias, int bitesUtiles, byte[] streamBytes) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(pathSalida))) {
            dos.writeInt(bitesUtiles);
            dos.write(streamBytes);

            int offsetTabla = 4 + streamBytes.length;
            dos.writeInt(frecuencias.size());

            for (Map.Entry<Byte, Integer> e : frecuencias.entrySet()) {
                dos.writeByte(e.getKey());   // Escribimos 1 byte directo
                dos.writeInt(e.getValue());  // Frecuencia (4 bytes)
            }

            dos.writeInt(offsetTabla);
            return true;
        } catch (IOException e) { return false; }
    }

    private static int leerOffsetTabla(byte[] datos) {
        int pos = datos.length - 4;
        return ((datos[pos] & 0xFF) << 24) | ((datos[pos + 1] & 0xFF) << 16) | ((datos[pos + 2] & 0xFF) << 8) | ((datos[pos + 3] & 0xFF));
    }

    private static Map<Byte, Integer> leerTabla(byte[] datos, int offsetTabla) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(datos, offsetTabla, datos.length - offsetTabla - 4))) {
            int cantEntradas = dis.readInt();
            Map<Byte, Integer> frecuencias = new LinkedHashMap<>();
            for (int i = 0; i < cantEntradas; i++) {
                byte b   = dis.readByte();
                int freq = dis.readInt();
                frecuencias.put(b, freq);
            }
            return frecuencias;
        } catch (IOException e) { return null; }
    }

    private static Map<Byte, Integer> contarFrecuencias(byte[] datos) {
        Map<Byte, Integer> freq = new HashMap<>();
        for (byte b : datos) freq.put(b, freq.getOrDefault(b, 0) + 1);
        return freq;
    }

    private static HuffmanNode construirArbol(Map<Byte, Integer> frecuencias) {
        PriorityQueue<HuffmanNode> cola = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> e : frecuencias.entrySet()) {
            cola.add(new HuffmanNode(e.getKey(), e.getValue()));
        }
        while (cola.size() > 1) {
            cola.add(new HuffmanNode(cola.poll(), cola.poll()));
        }
        return cola.poll();
    }

    private static void generarCodigos(HuffmanNode nodo, String codigo, Map<Byte, String> codigos) {
        if (nodo == null) return;
        if (nodo.esHoja()) {
            codigos.put(nodo.valor, codigo.isEmpty() ? "0" : codigo);
            return;
        }
        generarCodigos(nodo.izquierdo, codigo + "0", codigos);
        generarCodigos(nodo.derecho,   codigo + "1", codigos);
    }

    public static Map<Byte, String> obtenerCodigos(byte[] datos) {
        Map<Byte, Integer> frecuencias = contarFrecuencias(datos);
        if (frecuencias.size() == 1) {
            byte unico = frecuencias.keySet().iterator().next();
            frecuencias.put((byte)(unico == 0 ? 1 : 0), 0);
        }
        HuffmanNode raiz = construirArbol(frecuencias);
        Map<Byte, String> codigos = new LinkedHashMap<>();
        generarCodigos(raiz, "", codigos);

        List<Map.Entry<Byte, Integer>> lista = new ArrayList<>(frecuencias.entrySet());
        lista.sort((a, b) -> b.getValue() - a.getValue());

        Map<Byte, String> ordenado = new LinkedHashMap<>();
        for (Map.Entry<Byte, Integer> e : lista) {
            if (codigos.containsKey(e.getKey())) ordenado.put(e.getKey(), codigos.get(e.getKey()));
        }
        return ordenado;
    }

    public static Map<Byte, Integer> obtenerFrecuencias(byte[] datos) {
        return contarFrecuencias(datos);
    }

    public static long calcularBitsComprimidos(byte[] datos, Map<Byte, String> codigos) {
        long total = 0;
        for (byte b : datos) {
            if (codigos.containsKey(b)) total += codigos.get(b).length();
        }
        return total;
    }
}