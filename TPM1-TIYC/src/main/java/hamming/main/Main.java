package hamming.main;

import hamming.file_mngmt.FileManagement;

public class Main {

    public static void main (String[] args){    // Prueba de la funcionalidad de lectura y escritura de archivos
        String inputPath= "input.txt";          // Esto se borra despues xd
        String outputpath= "output.txt";

        byte[] dataBytes = FileManagement.readFile(inputPath);

        if(dataBytes!=null){
            System.out.println("Contenido del archivo:");
            System.out.println(new String(dataBytes));

            FileManagement.writeFile(outputpath,dataBytes);
            System.out.println("Archivo copiado.");
        }
    }
}
