import java.io.*;
import java.util.Scanner;

// Menu de consola del proyecto: Indexacion y Compactacion de Datos a Gran Escala.
// Caso: consulta de los locales anexos de contribuyentes (datos reales de SUNAT).
// Incluye: carga, INDEX(), busqueda, bajas, PACK() y prueba de rendimiento con hilos.
// Para usarlo como servidor (desde la nube, con pagina web) ver Servidor.java y la carpeta docs.
public class Main {

    static final String TXT = "datos/padron_reducido_local_anexo.txt";
    static final String DATOS = "salida/locales.dat";
    static final String META = "salida/locales.meta";
    static final String IDX_RUC = "salida/ruc.idx";
    static final String IDX_UBIGEO = "salida/ubigeo.idx";
    static final String TEMP = "salida/temp";

    public static void main(String[] args) throws Exception {
        ArchivoDatos datos = new ArchivoDatos(DATOS, META);
        Scanner teclado = new Scanner(System.in);
        new File("salida").mkdirs();

        long opcion = -1;
        while (opcion != 0) {
            System.out.println();
            System.out.println("=== LOCALES ANEXOS SUNAT ===");
            System.out.println("1. Cargar datos desde el TXT");
            System.out.println("2. INDEX(): crear indices (RUC y UBIGEO)");
            System.out.println("3. Buscar por RUC (con indice)");
            System.out.println("4. Buscar por UBIGEO (con indice)");
            System.out.println("5. Comparar: con indice vs recorrido secuencial");
            System.out.println("6. Dar de baja los locales de un RUC");
            System.out.println("7. Dar de baja los locales de un departamento");
            System.out.println("8. Estadisticas del archivo");
            System.out.println("9. PACK(): compactar el archivo (elimina las bajas)");
            System.out.println("10. Prueba de rendimiento con 1, 2, 4 y 8 hilos");
            System.out.println("0. Salir");
            opcion = leerNumero(teclado, "Opcion: ");

            if (opcion == 1) {
                long inicio = System.nanoTime();
                long total = datos.cargarDesdeTxt(TXT);
                System.out.println("Registros cargados: " + total + " en " + milisegundos(inicio) + " ms");
                System.out.println("Tamano de locales.dat: " + new File(DATOS).length() + " bytes");
                System.out.println("(los indices anteriores ya no sirven: ejecuta la opcion 2)");

            } else if (opcion == 2) {
                if (!new File(DATOS).exists()) {
                    System.out.println("Primero carga los datos (opcion 1).");
                } else {
                    int hilos = (int) leerNumero(teclado, "Cuantos hilos: ");
                    if (hilos < 1) {
                        hilos = 1;
                    }
                    long inicio = System.nanoTime();
                    long n1 = Indice.crear(datos, 0, IDX_RUC, TEMP, hilos);
                    System.out.println("Indice RUC: " + n1 + " entradas, " + milisegundos(inicio) + " ms");

                    inicio = System.nanoTime();
                    long n2 = Indice.crear(datos, 1, IDX_UBIGEO, TEMP, hilos);
                    System.out.println("Indice UBIGEO: " + n2 + " entradas, " + milisegundos(inicio) + " ms");
                }

            } else if (opcion == 3) {
                long ruc = leerNumero(teclado, "RUC a buscar: ");
                buscarConIndice(datos, IDX_RUC, ruc);

            } else if (opcion == 4) {
                long ubigeo = leerNumero(teclado, "UBIGEO a buscar: ");
                buscarConIndice(datos, IDX_UBIGEO, ubigeo);

            } else if (opcion == 5) {
                long campo = leerNumero(teclado, "Campo (0 = RUC, 1 = UBIGEO): ");
                long clave = leerNumero(teclado, "Valor a buscar: ");
                comparar(datos, (int) campo, clave);

            } else if (opcion == 6) {
                if (!new File(IDX_RUC).exists()) {
                    System.out.println("Primero crea los indices (opcion 2).");
                } else {
                    long ruc = leerNumero(teclado, "RUC a dar de baja: ");
                    long[] posiciones = Indice.buscar(IDX_RUC, ruc);
                    long marcados = datos.marcarBorrados(posiciones);
                    System.out.println("Locales dados de baja: " + marcados);
                }

            } else if (opcion == 7) {
                if (!new File(IDX_UBIGEO).exists()) {
                    System.out.println("Primero crea los indices (opcion 2).");
                } else {
                    long dep = leerNumero(teclado, "Departamento (2 primeros digitos del UBIGEO, ej. 15 = Lima): ");
                    long[] posiciones = Indice.buscarRango(IDX_UBIGEO, dep * 10000, (dep + 1) * 10000);
                    long marcados = datos.marcarBorrados(posiciones);
                    System.out.println("Locales dados de baja: " + marcados);
                }

            } else if (opcion == 8) {
                if (!new File(DATOS).exists()) {
                    System.out.println("Primero carga los datos (opcion 1).");
                } else {
                    long[] e = datos.estadisticas();
                    long tamano = new File(DATOS).length();
                    System.out.println("Registros activos:      " + e[0]);
                    System.out.println("Registros dados de baja: " + e[1]);
                    System.out.println("Tamano del archivo:      " + tamano + " bytes");
                    System.out.println("Espacio ocupado por bajas: " + e[2] + " bytes ("
                            + (100 * e[2] / tamano) + " %)");
                }

            } else if (opcion == 9) {
                if (!new File(DATOS).exists()) {
                    System.out.println("Primero carga los datos (opcion 1).");
                } else {
                    int hilos = (int) leerNumero(teclado, "Cuantos hilos: ");
                    if (hilos < 1) {
                        hilos = 1;
                    }
                    long antes = new File(DATOS).length();

                    long inicio = System.nanoTime();
                    long registros = Compactador.pack(datos, hilos);
                    System.out.println("PACK(): " + registros + " registros conservados en "
                            + milisegundos(inicio) + " ms");
                    long despues = new File(DATOS).length();
                    System.out.println("Tamano: " + antes + " -> " + despues + " bytes (se recuperaron "
                            + (antes - despues) + ")");

                    // Las posiciones cambiaron, asi que hay que rehacer los indices
                    inicio = System.nanoTime();
                    Indice.crear(datos, 0, IDX_RUC, TEMP, hilos);
                    Indice.crear(datos, 1, IDX_UBIGEO, TEMP, hilos);
                    System.out.println("Indices reconstruidos en " + milisegundos(inicio) + " ms");
                }

            } else if (opcion == 10) {
                if (!new File(TXT).exists()) {
                    System.out.println("No se encuentra " + TXT);
                } else {
                    pruebaRendimiento(datos);
                }
            }
        }
        System.out.println("Fin.");
    }

    // Mide INDEX() y PACK() con 1, 2, 4 y 8 hilos. Cada medicion se repite 3 veces y se toma la mejor.
    // En cada vuelta se recargan los datos y se dan de baja todos los locales de Lima (departamento 15),
    // que es el 40 % del archivo, para que PACK() tenga trabajo real.
    // Devuelve la tabla de resultados como texto (para que tambien la pueda mostrar el servidor).
    static String pruebaRendimiento(ArchivoDatos datos) throws Exception {
        int[] cantidades = { 1, 2, 4, 8 };
        double[] tiempoIndex = new double[4];
        double[] tiempoPack = new double[4];

        for (int c = 0; c < 4; c++) {
            int hilos = cantidades[c];
            tiempoIndex[c] = -1;
            tiempoPack[c] = -1;
            System.out.println("Midiendo con " + hilos + " hilo(s)...");

            for (int vez = 0; vez < 3; vez++) {
                datos.cargarDesdeTxt(TXT);

                long inicio = System.nanoTime();
                Indice.crear(datos, 0, IDX_RUC, TEMP, hilos);
                Indice.crear(datos, 1, IDX_UBIGEO, TEMP, hilos);
                double tI = ms(inicio);

                long[] posiciones = Indice.buscarRango(IDX_UBIGEO, 150000, 160000);
                datos.marcarBorrados(posiciones);

                inicio = System.nanoTime();
                Compactador.pack(datos, hilos);
                double tP = ms(inicio);

                if (tiempoIndex[c] < 0 || tI < tiempoIndex[c]) {
                    tiempoIndex[c] = tI;
                }
                if (tiempoPack[c] < 0 || tP < tiempoPack[c]) {
                    tiempoPack[c] = tP;
                }
            }
        }

        String tabla = "Hilos | INDEX (ms) | speedup | PACK (ms) | speedup\n";
        for (int c = 0; c < 4; c++) {
            tabla = tabla + String.format("%5d | %10.1f | %7.2f | %9.1f | %7.2f\n",
                    cantidades[c], tiempoIndex[c], tiempoIndex[0] / tiempoIndex[c],
                    tiempoPack[c], tiempoPack[0] / tiempoPack[c]);
        }
        tabla = tabla + "(speedup = tiempo con 1 hilo / tiempo con N hilos)";
        System.out.println();
        System.out.println(tabla);

        // Se deja el programa en un estado limpio: datos originales e indices listos
        datos.cargarDesdeTxt(TXT);
        Indice.crear(datos, 0, IDX_RUC, TEMP, 2);
        Indice.crear(datos, 1, IDX_UBIGEO, TEMP, 2);
        System.out.println("Datos originales recargados e indexados.");
        return tabla;
    }

    // Tiempo transcurrido desde "inicio", en milisegundos (como numero).
    static double ms(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1000000.0;
    }

    // Busca con el indice y muestra los primeros registros encontrados (ignora los dados de baja).
    static void buscarConIndice(ArchivoDatos datos, String rutaIdx, long clave) throws Exception {
        if (!new File(rutaIdx).exists()) {
            System.out.println("Primero crea los indices (opcion 2).");
            return;
        }
        long inicio = System.nanoTime();
        long[] posiciones = Indice.buscar(rutaIdx, clave);
        String tiempo = milisegundos(inicio);

        RandomAccessFile f = new RandomAccessFile(DATOS, "r");
        String[] primeros = new String[10];
        int activos = 0;
        for (int i = 0; i < posiciones.length; i++) {
            Registro r = ArchivoDatos.leerEn(f, posiciones[i]);
            if (!r.borrado) {
                if (activos < 10) {
                    primeros[activos] = r.toString();
                }
                activos++;
            }
        }
        f.close();

        System.out.println("Encontrados: " + activos + " (busqueda en el indice: " + tiempo + " ms)");
        for (int i = 0; i < activos && i < 10; i++) {
            System.out.println("  " + primeros[i]);
        }
        if (activos > 10) {
            System.out.println("  ... y " + (activos - 10) + " mas");
        }
        if (posiciones.length > activos) {
            System.out.println("  (" + (posiciones.length - activos) + " locales dados de baja, ignorados)");
        }
    }

    // Hace la misma busqueda de dos formas y muestra los tiempos.
    static void comparar(ArchivoDatos datos, int campo, long clave) throws Exception {
        String rutaIdx = IDX_RUC;
        if (campo == 1) {
            rutaIdx = IDX_UBIGEO;
        }
        if (!new File(rutaIdx).exists()) {
            System.out.println("Primero crea los indices (opcion 2).");
            return;
        }

        long inicio = System.nanoTime();
        long[] posiciones = Indice.buscar(rutaIdx, clave);
        long activos = datos.contarActivos(posiciones);
        System.out.println("Con indice:  " + activos + " encontrados en " + milisegundos(inicio) + " ms");

        inicio = System.nanoTime();
        long encontrados = datos.buscarSecuencial(campo, clave);
        System.out.println("Secuencial:  " + encontrados + " encontrados en " + milisegundos(inicio) + " ms");
    }

    // Convierte el tiempo transcurrido desde "inicio" a milisegundos con 3 decimales.
    static String milisegundos(long inicioNanos) {
        double ms = (System.nanoTime() - inicioNanos) / 1000000.0;
        return String.format("%.3f", ms);
    }

    // Pide un numero por teclado; si escriben otra cosa, vuelve a pedirlo.
    static long leerNumero(Scanner teclado, String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String texto = teclado.nextLine().trim();
            try {
                return Long.parseLong(texto);
            } catch (NumberFormatException e) {
                System.out.println("Escribe un numero.");
            }
        }
    }
}
