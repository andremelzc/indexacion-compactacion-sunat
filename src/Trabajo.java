import java.io.File;

// Un trabajo largo que corre en segundo plano dentro del servidor (cargar, INDEX, PACK, bajas, prueba).
// El cliente recibe el numero (id) del trabajo y despues pregunta como va con GET /estado/<id>.
public class Trabajo extends Thread {

    public int id;
    public String tipo;        // cargar, index, pack, baja_ruc, baja_dep, benchmark
    public int hilos;
    public long valor;         // RUC o departamento, solo para las bajas
    public String estado = "en curso";   // en curso, terminado o error
    public String resultado = "";
    public long inicio;
    public long fin = 0;

    public Trabajo(int id, String tipo, int hilos, long valor) {
        this.id = id;
        this.tipo = tipo;
        this.hilos = hilos;
        this.valor = valor;
    }

    // Milisegundos que lleva (o que tardo, si ya termino).
    public double ms() {
        long hasta = System.nanoTime();
        if (fin != 0) {
            hasta = fin;
        }
        return (hasta - inicio) / 1000000.0;
    }

    public void run() {
        inicio = System.nanoTime();
        try {
            ArchivoDatos datos = Servidor.datos;

            if (tipo.equals("cargar")) {
                long total = datos.cargarDesdeTxt(Main.TXT);
                new File(Main.IDX_RUC).delete();       // los indices viejos ya no sirven
                new File(Main.IDX_UBIGEO).delete();
                resultado = total + " registros cargados, locales.dat de " + new File(Main.DATOS).length()
                        + " bytes. Falta crear los índices.";

            } else if (tipo.equals("index")) {
                long t = System.nanoTime();
                long n1 = Indice.crear(datos, 0, Main.IDX_RUC, Main.TEMP, hilos);
                double ms1 = (System.nanoTime() - t) / 1000000.0;
                t = System.nanoTime();
                long n2 = Indice.crear(datos, 1, Main.IDX_UBIGEO, Main.TEMP, hilos);
                double ms2 = (System.nanoTime() - t) / 1000000.0;
                resultado = "INDEX con " + hilos + " hilo(s). RUC: " + n1 + " entradas en "
                        + Math.round(ms1) + " ms. UBIGEO: " + n2 + " entradas en " + Math.round(ms2) + " ms.";

            } else if (tipo.equals("pack")) {
                long antes = new File(Main.DATOS).length();
                long t = System.nanoTime();
                long registros = Compactador.pack(datos, hilos);
                double msPack = (System.nanoTime() - t) / 1000000.0;
                long despues = new File(Main.DATOS).length();

                // Las posiciones cambiaron, asi que se rehacen los indices
                t = System.nanoTime();
                Indice.crear(datos, 0, Main.IDX_RUC, Main.TEMP, hilos);
                Indice.crear(datos, 1, Main.IDX_UBIGEO, Main.TEMP, hilos);
                double msIndices = (System.nanoTime() - t) / 1000000.0;
                resultado = "PACK con " + hilos + " hilo(s): " + registros + " registros conservados en "
                        + Math.round(msPack) + " ms. El archivo pasó de " + antes + " a " + despues
                        + " bytes. Índices reconstruidos en " + Math.round(msIndices) + " ms.";

            } else if (tipo.equals("baja_ruc")) {
                long[] posiciones = Indice.buscar(Main.IDX_RUC, valor);
                long marcados = datos.marcarBorrados(posiciones);
                resultado = "Locales dados de baja: " + marcados;

            } else if (tipo.equals("baja_dep")) {
                long[] posiciones = Indice.buscarRango(Main.IDX_UBIGEO, valor * 10000, (valor + 1) * 10000);
                long marcados = datos.marcarBorrados(posiciones);
                resultado = "Locales dados de baja en el departamento " + valor + ": " + marcados;

            } else if (tipo.equals("benchmark")) {
                resultado = Main.pruebaRendimiento(datos);
            }
            estado = "terminado";
        } catch (Exception e) {
            estado = "error";
            resultado = e.toString();
        }
        fin = System.nanoTime();
        Servidor.terminar(this);
    }
}
