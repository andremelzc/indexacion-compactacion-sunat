import java.io.*;

// Todo lo que toca el archivo de datos binario (locales.dat) y el de metadata (locales.meta).
public class ArchivoDatos {

    public String rutaDatos;
    public String rutaMeta;

    public ArchivoDatos(String rutaDatos, String rutaMeta) {
        this.rutaDatos = rutaDatos;
        this.rutaMeta = rutaMeta;
    }

    // Lee el TXT de SUNAT y lo guarda como archivo binario con registros de longitud variable.
    // Devuelve cuantos registros guardo.
    public long cargarDesdeTxt(String rutaTxt) throws Exception {
        BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaTxt), "ISO-8859-1"));
        BufferedOutputStream salida = new BufferedOutputStream(new FileOutputStream(rutaDatos));

        lector.readLine(); // la primera linea es el encabezado, la saltamos

        long total = 0;
        String linea = lector.readLine();
        while (linea != null) {
            String[] partes = linea.split("\\|", -1);
            if (partes.length >= Registro.NUM_CAMPOS) {
                Registro r = new Registro();
                for (int i = 0; i < Registro.NUM_CAMPOS; i++) {
                    r.campos[i] = partes[i];
                }
                salida.write(r.aBytes());
                total++;
            }
            linea = lector.readLine();
        }
        salida.close();
        lector.close();

        guardarMeta(total);
        return total;
    }

    // La metadata es un archivo de texto con la descripcion del repositorio.
    public void guardarMeta(long total) throws Exception {
        PrintWriter w = new PrintWriter(new FileWriter(rutaMeta));
        w.println("archivo_datos=" + rutaDatos);
        w.println("registros=" + total);
        w.println("campos=" + Registro.NUM_CAMPOS);
        for (int i = 0; i < Registro.NUM_CAMPOS; i++) {
            w.println("campo" + i + "=" + Registro.NOMBRES[i]);
        }
        w.close();
    }

    // Lee UN registro que empieza en la posicion indicada (acceso directo).
    public static Registro leerEn(RandomAccessFile f, long posicion) throws Exception {
        f.seek(posicion);
        int borrado = f.read();
        int tamano = f.readUnsignedShort();
        byte[] b = new byte[tamano];
        b[0] = (byte) borrado;
        b[1] = (byte) (tamano >> 8);
        b[2] = (byte) tamano;
        f.readFully(b, 3, tamano - 3);
        return Registro.desdeBytes(b);
    }

    // Lee el SIGUIENTE registro de un flujo secuencial. Devuelve null cuando se acaba el archivo.
    public static Registro leerSiguiente(DataInputStream in) throws Exception {
        int borrado = in.read();
        if (borrado == -1) {
            return null;
        }
        int tamano = in.readUnsignedShort();
        byte[] b = new byte[tamano];
        b[0] = (byte) borrado;
        b[1] = (byte) (tamano >> 8);
        b[2] = (byte) tamano;
        in.readFully(b, 3, tamano - 3);
        return Registro.desdeBytes(b);
    }

    // Abre el archivo de datos y lo deja listo para leer desde la posicion indicada.
    // Cada hilo usa su propio flujo, asi no se estorban entre ellos.
    public DataInputStream abrirDesde(long posicion) throws Exception {
        FileInputStream f = new FileInputStream(rutaDatos);
        long saltados = 0;
        while (saltados < posicion) {
            long s = f.skip(posicion - saltados);
            if (s <= 0) {
                break;
            }
            saltados = saltados + s;
        }
        return new DataInputStream(new BufferedInputStream(f, 1 << 16));
    }

    // Divide el archivo en numRangos pedazos de tamano parecido, para repartirlos entre hilos.
    // Como los registros miden distinto, el corte tiene que caer justo al inicio de un registro:
    // por eso se recorre el archivo leyendo solo la cabecera de cada registro (y saltando el resto).
    // Devuelve numRangos + 1 posiciones: el rango i va desde limites[i] hasta limites[i + 1].
    public long[] dividirEnRangos(int numRangos) throws Exception {
        long total = new File(rutaDatos).length();
        long[] limites = new long[numRangos + 1];
        DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(rutaDatos), 1 << 16));

        long pos = 0;
        int rango = 1;
        while (rango < numRangos) {
            if (pos >= total) {
                limites[rango] = total;
                rango++;
            } else if (pos >= total * rango / numRangos) {
                limites[rango] = pos;      // este registro es el primero del rango siguiente
                rango++;
            } else {
                in.read();                                   // byte de borrado
                int tamano = in.readUnsignedShort();         // tamano del registro
                in.skipBytes(tamano - 3);                    // saltamos el resto del registro
                pos = pos + tamano;
            }
        }
        limites[numRangos] = total;
        in.close();
        return limites;
    }

    // Marca registros como borrados (solo cambia el primer byte de cada uno; NO los elimina todavia,
    // el espacio se recupera despues con PACK()). Devuelve cuantos se marcaron.
    public long marcarBorrados(long[] posiciones) throws Exception {
        RandomAccessFile f = new RandomAccessFile(rutaDatos, "rw");
        long marcados = 0;
        for (int i = 0; i < posiciones.length; i++) {
            f.seek(posiciones[i]);
            if (f.read() == 0) {          // solo si todavia estaba activo
                f.seek(posiciones[i]);
                f.write(1);
                marcados++;
            }
        }
        f.close();
        return marcados;
    }

    // De un conjunto de posiciones, cuenta cuantos registros siguen activos (no estan dados de baja).
    public long contarActivos(long[] posiciones) throws Exception {
        RandomAccessFile f = new RandomAccessFile(rutaDatos, "r");
        long activos = 0;
        for (int i = 0; i < posiciones.length; i++) {
            f.seek(posiciones[i]);
            if (f.read() == 0) {
                activos++;
            }
        }
        f.close();
        return activos;
    }

    // Recorre el archivo y devuelve {registros activos, registros de baja, bytes ocupados por las bajas}.
    public long[] estadisticas() throws Exception {
        DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(rutaDatos), 1 << 16));
        long activos = 0;
        long borrados = 0;
        long bytesBorrados = 0;
        long total = new File(rutaDatos).length();
        long pos = 0;
        while (pos < total) {
            int borrado = in.read();
            int tamano = in.readUnsignedShort();
            in.skipBytes(tamano - 3);
            if (borrado == 0) {
                activos++;
            } else {
                borrados++;
                bytesBorrados = bytesBorrados + tamano;
            }
            pos = pos + tamano;
        }
        in.close();
        return new long[] { activos, borrados, bytesBorrados };
    }

    // Busca recorriendo TODO el archivo, sin indice. Sirve para comparar tiempos.
    // Devuelve cuantos registros activos tienen esa clave en ese campo.
    public long buscarSecuencial(int campo, long clave) throws Exception {
        DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(rutaDatos), 1 << 16));
        long encontrados = 0;
        Registro r = leerSiguiente(in);
        while (r != null) {
            if (!r.borrado && r.clave(campo) == clave) {
                encontrados++;
            }
            r = leerSiguiente(in);
        }
        in.close();
        return encontrados;
    }
}
