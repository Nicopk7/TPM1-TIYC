package hamming.file_mngmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileManagement {
    public static byte[] readFile(String path){
        try{
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e){
            System.out.println("Error al leer el archivo: " + e.getMessage());
            return null;
        }
    }

    public static void writeFile(String path, byte[] dataBytes){
        try{
            Files.write(Paths.get(path),dataBytes);
        } catch (IOException e){
            System.out.println("Error al escribir en el archivo:" + e.getMessage());
        }
    }
}
