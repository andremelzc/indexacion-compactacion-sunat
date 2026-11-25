import java.io.*;

// PACK(): elimina fisicamente los registros dados de baja y deja el archivo sin huecos.
//
// Como se hace en paralelo (numHilos hilos):
//   1. Se divide el archivo en rangos, uno por hilo (cortando siempre al inicio de un registro).
//   2. Cada hilo cuenta cuantos bytes de su rango se van a conservar (los registros activos).
//   3. Suma de prefijos: la posicion donde empieza a escribir un hilo en el archivo nuevo es
//      la suma de los bytes conservados por todos los hilos anteriores.
//   4. Cada hilo copia sus registros activos al archivo nuevo, desde su posicion. Como las
//      zonas no se pisan, los hilos escriben a la vez sin problema.
//   5. El archivo nuevo reemplaza al viejo. (Despues hay que rehacer los indices, porque
//      las posiciones de los registros cambiaron.)
public class Compactador {

    // Devuelve cuantos registros quedaron en el archivo.
    public static long pack(ArchivoDatos datos, int numHilos) throws Exception {
        long[] limites = datos.dividirEnRangos(numHilos);

        // Paso 2: contar en paralelo
        HiloContar[] contar = new HiloContar[numHilos];
        for (int i = 0; i < numHilos; i++) {
            contar[i] = new HiloContar(datos, limites[i], limites[i + 1]);
            contar[i].start();
        }
        for (int i = 0; i < numHilos; i++) {
            contar[i].join();
        }

        // Paso 3: suma de prefijos
        long[] inicioSalida = new long[numHilos + 1];
        long registros = 0;
        for (int i = 0; i < numHilos; i++) {
            inicioSalida[i + 1] = inicioSalida[i] + contar[i].bytes;
            registros = registros + contar[i].registros;
        }

        // Se crea el archivo nuevo con su tamano final
        String rutaNueva = datos.rutaDatos + ".nuevo";
        RandomAccessFile nuevo = new RandomAccessFile(rutaNueva, "rw");
        nuevo.setLength(inicioSalida[numHilos]);
        nuevo.close();

        // Paso 4: copiar en paralelo
        HiloCopiar[] copiar = new HiloCopiar[numHilos];
        for (int i = 0; i < numHilos; i++) {
            copiar[i] = new HiloCopiar(datos, rutaNueva, limites[i], limites[i + 1], inicioSalida[i]);
            copiar[i].start();
        }
        for (int i = 0; i < numHilos; i++) {
            copiar[i].join();
        }

        // Paso 5: el archivo nuevo reemplaza al viejo
        File viejo = new File(datos.rutaDatos);
        viejo.delete();
        new File(rutaNueva).renameTo(viejo);
        datos.guardarMeta(registros);

        return registros;
    }
}

// Hilo que cuenta los bytes y registros activos de un rango del archivo.
class HiloContar extends Thread {

    ArchivoDatos datos;
    long ini;
    long fin;
    long bytes = 0;      // bytes que se conservan
    long registros = 0;  // registros que se conservan

    HiloContar(ArchivoDatos datos, long ini, long fin) {
        this.datos = datos;
        this.ini = ini;
        this.fin = fin;
    }

    public void run() {
        try {
            DataInputStream in = datos.abrirDesde(ini);
            long pos = ini;
            while (pos < fin) {
                int borrado = in.read();
                int tamano = in.readUnsignedShort();
                in.skipBytes(tamano - 3);
                if (borrado == 0) {
                    bytes = bytes + tamano;
                    registros++;
                }
                pos = pos + tamano;
            }
            in.close();
        } catch (Exception e) {
            System.out.println("Error en un hilo: " + e);
        }
    }
}

// Hilo que copia los registros activos de un rango al archivo nuevo, empezando en posSalida.
class HiloCopiar extends Thread {

    ArchivoDatos datos;
    String rutaNueva;
    long ini;
    long fin;
    long posSalida;

    HiloCopiar(ArchivoDatos datos, String rutaNueva, long ini, long fin, long posSalida) {
        this.datos = datos;
        this.rutaNueva = rutaNueva;
        this.ini = ini;
        this.fin = fin;
        this.posSalida = posSalida;
    }

    public void run() {
        try {
            DataInputStream in = datos.abrirDesde(ini);
            RandomAccessFile out = new RandomAccessFile(rutaNueva, "rw");
            out.seek(posSalida);

            byte[] buffer = new byte[1 << 16];   // se acumulan registros y se escriben juntos
            int usado = 0;

            long pos = ini;
            while (pos < fin) {
                int borrado = in.read();
                int tamano = in.readUnsignedShort();

                if (borrado == 0) {
                    if (usado + tamano > buffer.length) {
                        out.write(buffer, 0, usado);
                        usado = 0;
                    }
                    buffer[usado] = 0;
                    buffer[usado + 1] = (byte) (tamano >> 8);
                    buffer[usado + 2] = (byte) tamano;
                    in.readFully(buffer, usado + 3, tamano - 3);
                    usado = usado + tamano;
                } else {
                    in.skipBytes(tamano - 3);   // registro de baja: no se copia
                }
                pos = pos + tamano;
            }

            out.write(buffer, 0, usado);
            out.close();
            in.close();
        } catch (Exception e) {
            System.out.println("Error en un hilo: " + e);
        }
    }
}
