package hamming.bitutilities;
import java.util.BitSet;
import java.util.Random;

public class bitUtilities {

        //MATEMATICAS
        public static int randomInt(int limiteSup){
            return new Random().nextInt(limiteSup);
        }

        public static boolean isPotenciaDeDos(int n) {
            return n > 0 && (n & (n - 1)) == 0;
        }

        //MANEJO DE BITS
        public static BitSet concatBits(BitSet receptorBits, BitSet bitsToAppend, int posInicial, int cantidad){
        /*
        EJEMPLO DE COMO FUNCIONA, sea:

        recptorBits: 0101       posicionDesde = 4
        bitsToAppend: 000111    cantidad = 6

        return -> receptorBits = 0101000111

        OBSERVACION 1: Si posición desde no es igual al tamaño de receptorBits, sobreescribira lo que se encuentre en el camino
            esto puede resultar util para copiar/duplicar/sobreescribir si es necesario

        OBSERVACION 2: no existe nullPointerException
        */

            for(int i = 0; i<cantidad; i++)
                receptorBits.set(posInicial + i, bitsToAppend.get(i));

            return receptorBits;
        }


        public static BitSet integerToBinary(int number, int longitud){
            //retorna bitset de tamaño longitud, con la representacion binaria de number
            //comportamiento inesperado con numeros negativos
            BitSet bitset = new BitSet(longitud);

            for(int i = longitud-1; i>=0; i--){
                int modulo = number % 2;
                if(modulo == 0)
                    bitset.set(i, false);
                else
                    bitset.set(i, true);

                number /= 2;
            }
            return bitset;
        }

        public static BitSet octalTo8bits(int ascii){
            //octal usualmente son los bytes con los que trabajan los Byte buffer[]
            BitSet bitset = new BitSet(8);

            if(ascii <0) ascii += 256;

            for(int i = 7; i>=0; i--){
                int modulo = ascii % 2;
                if(modulo == 0)
                    bitset.set(i, false);
                else
                    bitset.set(i, true);

                ascii /= 2;
            }
            return bitset;
        }

        public static byte binaryToAscii(BitSet bitset){

            int valor = 0;
            int potencia = 7;
            for(int i = 0; i<8; i++){
                if(bitset.get(i))
                    valor += Math.pow(2, potencia);
                potencia --;
            }

            if(valor > 127) valor -= 256;

            return Byte.decode(String.valueOf(valor));
        }

        public static int binaryToInt(BitSet bitset, int size){
            int valor = 0;
            int potencia = size-1;
            for(int i = 0; i<size; i++){
                if(bitset.get(i))
                    valor += Math.pow(2, potencia);
                potencia --;
            }
            return valor;
        }

        public static BitSet bufferToBitset(byte[] buffer, int cantBytes){
            //transforma lo leido por buffer en bitset de tamaño cantBytes*8
            BitSet bitset = new BitSet(cantBytes*8);

            for(int i=0; i<cantBytes; i++)
                bitset = concatBits(bitset, octalTo8bits(buffer[i]), i*8, 8);

            return bitset;
        }

        public static BitSet booleanArrayToBitset(boolean boo[], int size){
            BitSet bitset = new BitSet(size);
            for (int i = 0; i < size; i++)
                bitset.set(i, boo[i]);

            return bitset;
        }

        public static BitSet repartirInfo(BitSet input, int vectorSize){
        /*
        Por ejemplo en Hamming 16, se trabaja con 11 bits de informacion
        pero esos 11 bits de informacion hay que distribuirlos en 16bits
        en las posiciones correspondientes

        en este caso:
            C C I C I I I C I I I I I I I P

        Entonces esta funcion retorna para este ejemplo
            0 0 I 0 I I I 0 I I I I I I I 0

        */
            BitSet output = new BitSet(vectorSize);
            int j = 0;
            for(int i=0; i<vectorSize; i++){
                if(!bitUtilities.isPotenciaDeDos(i+1)){
                    output.set(i, input.get(j));
                    j++;
                }
            }
            return output;
        }

        

        //FUNCIONES DE DEBUG
        public static void printBitSet(BitSet bitset, int size){
            for(int i = 0; i<size; i++){
                if(bitset.get(i))
                    System.out.print(" 1 ");
                else
                    System.out.print(" 0 ");
            }
            System.out.println("");
        }

        public static void printBooleanMatriz(boolean[][] matriz, int filas, int columnas){
            for (int i = 0; i < filas; i++) {
                for (int j = 0; j < columnas; j++) {
                    if(matriz[i][j])
                        System.out.print("1");
                    else
                        System.out.print("0");
                    System.out.print(" ");
                }
                System.out.println("");
            }
        }
    }




