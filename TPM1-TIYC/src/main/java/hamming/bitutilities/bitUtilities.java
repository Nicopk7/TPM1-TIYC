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

            for(int i = 0; i<cantidad; i++)
                receptorBits.set(posInicial + i, bitsToAppend.get(i));

            return receptorBits;
        }


        public static BitSet integerToBinary(int number, int longitud){
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

    }




