import java.io.*;

// INDEX() y busqueda binaria.
// Un indice es un archivo con entradas de 16 bytes: [clave (8 bytes)][posicion en locales.dat (8 bytes)],
// ordenadas por clave. Como no todo cabe en memoria, se ordena por bloques (ordenamiento externo):
//   1. El archivo de datos se divide en rangos, uno por hilo.
//   2. Cada hilo lee SU rango, arma bloques de TAM_BLOQUE entradas, ordena cada bloque
//      (merge sort propio) y lo guarda en un archivo temporal.
//   3. Se mezclan todos los temporales en el archivo de indice final.
public class Indice {

    public static final int TAM_ENTRADA = 16;
    public static final int TAM_BLOQUE = 100000;

    // INDEX(): crea el indice de un campo (0 = RUC, 1 = UBIGEO). Devuelve cuantas entradas tiene.
    public static long crear(ArchivoDatos datos, int campo, String rutaIdx,
                             String carpetaTemp, int numHilos) throws Exception {
        new File(carpetaTemp).mkdirs();
        long[] limites = datos.dividirEnRangos(numHilos);

        HiloIndice[] hilos = new HiloIndice[numHilos];
        for (int i = 0; i < numHilos; i++) {
            hilos[i] = new HiloIndice(datos, campo, limites[i], limites[i + 1], i, carpetaTemp);
            hilos[i].start();
        }
        for (int i = 0; i < numHilos; i++) {
            hilos[i].join();   // esperamos a que todos terminen
        }

        // Lista de archivos temporales, en el mismo orden en que aparecen en el archivo de datos
        long total = 0;
        int cuantos = 0;
        for (int i = 0; i < numHilos; i++) {
            total = total + hilos[i].entradas;
            cuantos = cuantos + hilos[i].numBloques;
        }
        String[] rutas = new String[cuantos];
        int k = 0;
        for (int i = 0; i < numHilos; i++) {
            for (int b = 0; b < hilos[i].numBloques; b++) {
                rutas[k] = carpetaTemp + "/h" + i + "_b" + b + ".tmp";
                k++;
            }
        }

        mezclar(rutas, rutaIdx);

        for (int i = 0; i < rutas.length; i++) {
            new File(rutas[i]).delete();
        }
        return total;
    }

    // Mezcla los archivos temporales (ya ordenados) en un solo archivo ordenado.
    // En cada vuelta se escribe la entrada con la clave mas pequena de las que estan "en mano".
    private static void mezclar(String[] rutas, String rutaIdx) throws Exception {
        int k = rutas.length;
        DataOutputStream salida = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(rutaIdx), 1 << 16));

        DataInputStream[] entradas = new DataInputStream[k];
        long[] clave = new long[k];
        long[] pos = new long[k];
        boolean[] activo = new boolean[k];

        for (int i = 0; i < k; i++) {
            entradas[i] = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(rutas[i]), 1 << 16));
            clave[i] = entradas[i].readLong();
            pos[i] = entradas[i].readLong();
            activo[i] = true;
        }

        int vivos = k;
        while (vivos > 0) {
            int menor = -1;
            for (int i = 0; i < k; i++) {
                if (activo[i] && (menor == -1 || clave[i] < clave[menor])) {
                    menor = i;
                }
            }

            salida.writeLong(clave[menor]);
            salida.writeLong(pos[menor]);

            try {
                clave[menor] = entradas[menor].readLong();
                pos[menor] = entradas[menor].readLong();
            } catch (EOFException e) {
                activo[menor] = false;   // ese bloque se acabo
                vivos--;
            }
        }

        salida.close();
        for (int i = 0; i < k; i++) {
            entradas[i].close();
        }
    }

    // Busca una clave en el archivo de indice con busqueda binaria (sin cargar el indice en memoria).
    // Devuelve las posiciones en locales.dat de todos los registros con esa clave.
    public static long[] buscar(String rutaIdx, long clave) throws Exception {
        return buscarRango(rutaIdx, clave, clave + 1);
    }

    // Igual, pero para todas las claves que cumplan: desde <= clave < hasta.
    // Sirve, por ejemplo, para tomar todos los UBIGEO de un departamento.
    public static long[] buscarRango(String rutaIdx, long desde, long hasta) throws Exception {
        RandomAccessFile f = new RandomAccessFile(rutaIdx, "r");
        long n = f.length() / TAM_ENTRADA;

        long primera = primeraEntrada(f, n, desde);   // primera entrada con clave >= desde
        long despues = primeraEntrada(f, n, hasta);   // primera entrada con clave >= hasta
        int cuantas = (int) (despues - primera);

        long[] posiciones = new long[cuantas];
        for (int i = 0; i < cuantas; i++) {
            f.seek((primera + i) * TAM_ENTRADA + 8);
            posiciones[i] = f.readLong();
        }
        f.close();
        return posiciones;
    }

    // Busqueda binaria: encuentra la primera entrada cuya clave es mayor o igual a la buscada.
    private static long primeraEntrada(RandomAccessFile f, long n, long clave) throws Exception {
        long ini = 0;
        long fin = n;
        while (ini < fin) {
            long medio = (ini + fin) / 2;
            f.seek(medio * TAM_ENTRADA);
            long c = f.readLong();
            if (c < clave) {
                ini = medio + 1;
            } else {
                fin = medio;
            }
        }
        return ini;
    }

    // Merge sort: ordena claves[ini..fin) y mueve las posiciones junto con sus claves.
    static void mergeSort(long[] claves, long[] posiciones, long[] auxClaves, long[] auxPos,
                          int ini, int fin) {
        if (fin - ini < 2) {
            return;
        }
        int medio = (ini + fin) / 2;
        mergeSort(claves, posiciones, auxClaves, auxPos, ini, medio);
        mergeSort(claves, posiciones, auxClaves, auxPos, medio, fin);

        // mezclar las dos mitades ya ordenadas
        int i = ini;
        int j = medio;
        int k = ini;
        while (i < medio && j < fin) {
            if (claves[i] <= claves[j]) {
                auxClaves[k] = claves[i];
                auxPos[k] = posiciones[i];
                i++;
            } else {
                auxClaves[k] = claves[j];
                auxPos[k] = posiciones[j];
                j++;
            }
            k++;
        }
        while (i < medio) {
            auxClaves[k] = claves[i];
            auxPos[k] = posiciones[i];
            i++;
            k++;
        }
        while (j < fin) {
            auxClaves[k] = claves[j];
            auxPos[k] = posiciones[j];
            j++;
            k++;
        }
        for (k = ini; k < fin; k++) {
            claves[k] = auxClaves[k];
            posiciones[k] = auxPos[k];
        }
    }
}

// Hilo que lee un rango del archivo de datos y lo convierte en bloques ordenados (archivos temporales).
class HiloIndice extends Thread {

    ArchivoDatos datos;
    int campo;
    long ini;
    long fin;
    int id;
    String carpetaTemp;

    long[] claves = new long[Indice.TAM_BLOQUE];
    long[] posiciones = new long[Indice.TAM_BLOQUE];
    long[] auxClaves = new long[Indice.TAM_BLOQUE];
    long[] auxPos = new long[Indice.TAM_BLOQUE];

    int numBloques = 0;   // cuantos temporales escribio este hilo
    long entradas = 0;    // cuantas entradas en total

    HiloIndice(ArchivoDatos datos, int campo, long ini, long fin, int id, String carpetaTemp) {
        this.datos = datos;
        this.campo = campo;
        this.ini = ini;
        this.fin = fin;
        this.id = id;
        this.carpetaTemp = carpetaTemp;
    }

    public void run() {
        try {
            DataInputStream in = datos.abrirDesde(ini);
            int n = 0;
            long pos = ini;

            while (pos < fin) {
                Registro r = ArchivoDatos.leerSiguiente(in);
                long clave = r.clave(campo);
                if (!r.borrado && clave != -1) {
                    claves[n] = clave;
                    posiciones[n] = pos;
                    n++;
                    if (n == Indice.TAM_BLOQUE) {
                        guardarBloque(n);
                        n = 0;
                    }
                }
                pos = pos + r.tamano;
            }
            if (n > 0) {
                guardarBloque(n);
            }
            in.close();
        } catch (Exception e) {
            System.out.println("Error en un hilo: " + e);
        }
    }

    // Ordena el bloque que esta en memoria y lo escribe en un archivo temporal.
    void guardarBloque(int n) throws Exception {
        Indice.mergeSort(claves, posiciones, auxClaves, auxPos, 0, n);

        String ruta = carpetaTemp + "/h" + id + "_b" + numBloques + ".tmp";
        DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(ruta), 1 << 16));
        for (int i = 0; i < n; i++) {
            out.writeLong(claves[i]);
            out.writeLong(posiciones[i]);
        }
        out.close();

        numBloques++;
        entradas = entradas + n;
    }
}
