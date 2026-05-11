package data;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextArea;
import data.Semantico.Simbolo;

public class Objeto {

    /* Clase para formar los bytes */
    public static class Bit8 {
        boolean b1, b2, b3, b4, b5, b6, b7, b8;

        public Bit8(boolean b1, boolean b2, boolean b3, boolean b4, boolean b5, boolean b6, boolean b7, boolean b8 ) {
            this.b1= b1; this.b2= b2; this.b3= b3; this.b4= b4; this.b5= b5; this.b6= b6; this.b7= b7; this.b8= b8;
        }

        @Override
        public String toString() {
            return "" +
                    (b1 ? "1" : "0") + (b2 ? "1" : "0") + (b3 ? "1" : "0") + (b4 ? "1" : "0") +
                    (b5 ? "1" : "0") + (b6 ? "1" : "0") + (b7 ? "1" : "0") + (b8 ? "1" : "0");
        }   
    }

    /* Clase para generar OPCODES */
    public static class InstruccionBin{
        Bit8[] offset = new Bit8[2];
        ArrayList <Bit8> instruccion = new ArrayList <Bit8> ();
        public InstruccionBin(Bit8[] offset, ArrayList<Bit8> instruccion) {
            this.offset = offset;
            this.instruccion = instruccion;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Bit8 b : offset) {
                String bits = b.toString();
                sb.append(bits, 0, 4).append(" ").append(bits, 4, 8).append(" ");
            }
            sb.append("\t");
            for (Bit8 b : instruccion) {
                String bits = b.toString();
                sb.append(bits, 0, 4).append(" ").append(bits, 4, 8).append(" ");
            }

            return sb.toString();
        }
    }
    
    /* Clase para saltos pendientes */
    private static class SaltoPendiente {
        String etiqueta;
        int indiceInstr;
        int offsetNext;
        SaltoPendiente(String etiqueta, int indiceInstr, int offsetNext) {
            this.etiqueta = etiqueta;
            this.indiceInstr = indiceInstr;
            this.offsetNext = offsetNext;
        }
    }

    /* Variables para la clase Objectt */
    private List<Simbolo> tablaSemantica;
    private JTextArea in;
    private JTextArea out;
    /* Lista para guardar todas las instrucciones en binario */
    ArrayList <InstruccionBin> data = new ArrayList <InstruccionBin> ();
    ArrayList <InstruccionBin> instrucciones = new ArrayList <InstruccionBin> ();
    /* Matriz para guardar todas las instrucciones en código intermedio */
    ArrayList<String[]> temp = new ArrayList<String[]>();
    /* Offsets */
    int offsetVar = 0;
    int offsetIns = 0;

    /* Para el manejo de los saltos */
    private ArrayList<String> etiquetas = new ArrayList<>();
    private ArrayList<Integer> offsetEtiquetas = new ArrayList<>();
    private ArrayList<SaltoPendiente> saltosPendientes = new ArrayList<>();

    /* Constructor */
    public Objeto(List<Simbolo> tablaSemantica, JTextArea in, JTextArea out) {
        this.tablaSemantica = tablaSemantica;
        this.in = in;
        this.out = out;
    }

    /** Para imprimir todo **/
    public void imprimirTodo() {
        generarSegmentoDatos();
        out.append("\t OFFSET" +" \t" + "CONTENIDO \n" );
        for (InstruccionBin ib : data) {
            out.append(ib.toString());
            out.append("\n");
        }
        out.append("\n");
        generarSegmentoCode();
        for (InstruccionBin ib : instrucciones) {
            out.append(ib.toString());
            out.append("\n");
        }
        out.append("\n");
    }

    /** Llena la lista data en binario con las variables de la tabla semántica */
    public void generarSegmentoDatos() {
        offsetVar = 0;
        data.clear();
        ArrayList<String> nombresProcesados = new ArrayList<>();
        for (Simbolo s : tablaSemantica) {
            if (nombresProcesados.contains(s.nombre)) {
                continue;
            }
            nombresProcesados.add(s.nombre);
            int size = tamanoTipo(s);
            Bit8[] offsetBits = offsetBin(offsetVar);
            ArrayList<Bit8> bytesVar = new ArrayList<Bit8>();
            for (int i = 0; i < size; i++) {
                bytesVar.add(binAByte("00000000")); 
            }
            InstruccionBin varBin = new InstruccionBin(offsetBits, bytesVar);
            data.add(varBin);
            offsetVar += size;
        }
    }
    
    /** Llena la lista code en binario con las instrucciones de la tabla de instrucciones creada */
    public void generarSegmentoCode() {
        temp.clear();
        extraerInstrucciones();  
        offsetIns = 0;
        instrucciones.clear();
        etiquetas.clear();
        offsetEtiquetas.clear();
        saltosPendientes.clear();

        for (String[] fila : temp) {
            String instr = fila[0]; 
            String op1   = fila[1];
            String op2   = fila[2];

            if (op1.isEmpty() && op2.isEmpty()) {
                etiquetas.add(instr);          // instr aquí es el nombre de la etiqueta
                offsetEtiquetas.add(offsetIns); // offset de la próxima instrucción
                continue;
            }

            ArrayList<Bit8> bytesInstr = new ArrayList<>();
            switch (instr) {
            case "MOV":
                procesarMOV(op1, op2, bytesInstr);
                break;

            case "ADD":
                procesarADD(op1, op2, bytesInstr);
                break;

            case "SUB":
                procesarSUB(op1, op2, bytesInstr);
                break;

            case "CMP":
                procesarCMP(op1, op2, bytesInstr);
                break;

            case "MUL":
                procesarMUL(op1, op2, bytesInstr);
                break;

            case "JMP":
            case "JGE":
            case "JLE":
                procesarSALTO(instr, op1, bytesInstr);
                break;

            case "INT":
                procesarINT(op1, bytesInstr);
                break;

            default:
                continue; 
            }
        }
        /** Calcular el offset de los saltos */
        resolverSaltos();
    }
    private void procesarMOV(String op1, String op2, ArrayList<Bit8> bytesInstr) {
        // MOV AX, @data
        if (op1.equals("AX") && op2.equalsIgnoreCase("@data")) {
            emitirConImm16(bytesInstr, OP_MOV_AX_IMM, 0);
            return;
        }
        
        // MOV DS, AX 
        if (op1.equals("DS") && op2.equalsIgnoreCase("AX")) {
            agregarBytes(bytesInstr, OP_MOV_DS_AX);
            agregarInstruccion(bytesInstr);
            return;
        }

        // MOV AX, 4C00h
        if (op1.equals("AX") && op2.equals("4C00h")) {
            emitirConImm16(bytesInstr, OP_MOV_AX_400C, 19456);
            return;
        }

        // MOV AX, inm16
        if (op1.equals("AX") && esEntero(op2)) {
            emitirConImm16(bytesInstr, OP_MOV_AX_IMM, Integer.parseInt(op2));
            return;
        }

        // MOV var, AX usa el opcode corto A3 para el acumulador.
        if (!op1.isEmpty() && op2.equals("AX") && esVarEnteraOBool(op1)) {
            emitirConDireccion(bytesInstr, OP_MOV_MEM_AX_CORTO, op1);
            return;
        }

        // MOV AX, var usa el opcode corto A1 para el acumulador.
        if (op1.equals("AX") && !op2.isEmpty() && !esEntero(op2) 
                && esVarEnteraOBool(op2)) {
            emitirConDireccion(bytesInstr, OP_MOV_AX_MEM_CORTO, op2);
            return;
        }

        // MOV AL, var usa el opcode corto A0 para variables de 8 bits.
        if (op1.equals("AL") && !op2.isEmpty() && !esEntero(op2) 
                && getSizeVar(op2) == 1) {
            emitirConDireccion(bytesInstr, OP_MOV_AL_MEM_CORTO, op2);
            return;
        }

        // MOV var, AL usa el opcode corto A2 para variables de 8 bits.
        if (!op1.isEmpty() && op2.equals("AL") && getSizeVar(op1) == 1) {
            emitirConDireccion(bytesInstr, OP_MOV_MEM_AL_CORTO, op1);
            return;
        }

        // MOV DX, var usa el formato general 8B + ModR/M.
        if (op1.equals("DX") && !op2.isEmpty() && !esEntero(op2)
                && esVarEnteraOBool(op2)) {
            emitirConDireccion(bytesInstr, OP_MOV_DX_MEM, op2);
            return;
        }

        // MOV DX, inm16
        if (op1.equals("DX") && esEntero(op2)) {
            emitirConImm16(bytesInstr, OP_MOV_DX_IMM, Integer.parseInt(op2));
            return;
        }

        // MOV var, inm
        if (!op1.isEmpty() && !esEntero(op1) && !op2.isEmpty()) {
            int size = getSizeVar(op1);
            // var de 1 o 2 bytes
            if ((size == 1 || size == 2) && esEntero(op2)) {
                int valor = Integer.parseInt(op2);
                int off = getOffsetVar(op1);

                if (size == 1) {
                    emitirConDireccionYDato(bytesInstr, OP_MOV_MEM_IMM8, off, decimalABinario(valor & 0xFF));
                } else { // size == 2
                    emitirConDireccionYDato(bytesInstr, OP_MOV_MEM_IMM16, off, decimalABinario16(valor));
                }
                agregarInstruccion(bytesInstr);
                return;
            }
        }

        /** Estos mensajes son para hacer debug. Pueden servir para escalar el programa*/
        out.append("(MOV) combinación no soportada: " + op1 + ", " + op2 + "\n");
    }

    private void procesarADD(String op1, String op2, ArrayList<Bit8> bytesInstr) {
        // ADD AX, inm16
        if (op1.equals("AX") && esEntero(op2)) {
            agregarBytes(bytesInstr, OP_ADD_AX_IMM);
            int valor = Integer.parseInt(op2);
            Bit8[] imm16 = decimalABinario16(valor);
            agregarBytes(bytesInstr, imm16);
            agregarInstruccion(bytesInstr);
            return;
        }

        // ADD var, AX
        if (!op1.isEmpty() && op2.equals("AX")) {
            agregarBytes(bytesInstr, OP_ADD_MEM_AX);
            int off = getOffsetVar(op1);
            Bit8[] desp = direccionBin(off);
            agregarBytes(bytesInstr, desp);
            agregarInstruccion(bytesInstr);
            return;
        }

        // ADD AX, var
        if (op1.equals("AX") && !op2.isEmpty() && !esEntero(op2)) {
            agregarBytes(bytesInstr, OP_ADD_AX_MEM);
            int off = getOffsetVar(op2);
            Bit8[] desp = direccionBin(off);
            agregarBytes(bytesInstr, desp);
            agregarInstruccion(bytesInstr);
            return;
        }

        // ADD var, inm
        if (!op1.isEmpty() && !esEntero(op1) && !op2.isEmpty()) {
            int size = getSizeVar(op1);

            // var de 1 o 2 bytes
            if ((size == 1 || size == 2) && esEntero(op2)) {
                int valor = Integer.parseInt(op2);
                int off = getOffsetVar(op1);
                agregarBytes(bytesInstr, OP_ADD_MEM_IMM16);
                Bit8[] desp = direccionBin(off);
                agregarBytes(bytesInstr, desp);
                Bit8[] imm16 = decimalABinario16(valor);
                agregarBytes(bytesInstr, imm16);

                agregarInstruccion(bytesInstr);
                return;
            }
        }
        out.append("(ADD) Combinación no soportada: " + op1 + ", " + op2 + "\n");
    }

    private void procesarSUB(String op1, String op2, ArrayList<Bit8> bytesInstr) {
        // SUB AX, inm16
        if (op1.equals("AX") && esEntero(op2)) {
            agregarBytes(bytesInstr, OP_SUB_AX_IMM);
            int valor = Integer.parseInt(op2);
            Bit8[] imm16 = decimalABinario16(valor);
            agregarBytes(bytesInstr, imm16);
            agregarInstruccion(bytesInstr);
            return;
        }

        // SUB var, AX
        if (!op1.isEmpty() && op2.equals("AX")) {
            agregarBytes(bytesInstr, OP_SUB_MEM_AX);
            int off = getOffsetVar(op1);
            Bit8[] desp = direccionBin(off);
            agregarBytes(bytesInstr, desp);
            agregarInstruccion(bytesInstr);
            return;
        }

        // SUB AX, var 
        if (op1.equals("AX") && !op2.isEmpty() && !esEntero(op2)) {
            agregarBytes(bytesInstr, OP_SUB_AX_MEM);
            int off = getOffsetVar(op2);
            Bit8[] desp = direccionBin(off);
            agregarBytes(bytesInstr, desp);
            agregarInstruccion(bytesInstr);
            return;
        }

        // SUB var, inm
        if (!op1.isEmpty() && !esEntero(op1) && !op2.isEmpty()) {
            int size = getSizeVar(op1);

            // var de 1 o 2 bytes
            if ((size == 1 || size == 2) && esEntero(op2)) {
                int valor = Integer.parseInt(op2);
                int off = getOffsetVar(op1);
                agregarBytes(bytesInstr, OP_SUB_MEM_IMM16);
                Bit8[] desp = direccionBin(off);
                agregarBytes(bytesInstr, desp);
                Bit8[] imm16 = decimalABinario16(valor);
                agregarBytes(bytesInstr, imm16);

                agregarInstruccion(bytesInstr);
                return;
            }
        }

        out.append("(SUB) Combinación no soportada: " + op1 + ", " + op2 + "\n");
    }

    private void procesarMUL(String op1, String op2, ArrayList<Bit8> bytesInstr) {
        if (op2 == null) op2 = "";
        // MUL DX
        if (op1.equals("DX") && op2.isEmpty()) {
            agregarBytes(bytesInstr, OP_MUL_DX);
            agregarInstruccion(bytesInstr);
            return;
        }
        out.append("(MUL) Combinación no soportada: " + op1 + ", " + op2 + "\n");
    }

    private void procesarCMP(String op1, String op2, ArrayList<Bit8> bytesInstr) {
        // CMP AX, inm16 
        if (op1.equals("AX") && esEntero(op2)) {
            agregarBytes(bytesInstr, OP_CMP_AX_IMM);
            int valor = Integer.parseInt(op2);
            Bit8[] imm16 = decimalABinario16(valor);
            agregarBytes(bytesInstr, imm16);
            agregarInstruccion(bytesInstr);
            return;
        }

        // CMP DX, AX
        if (op1.equals("DX") && op2.equals("AX")) {
            agregarBytes(bytesInstr, OP_CMP_DX_AX);
            agregarInstruccion(bytesInstr);
            return;
        }

        out.append("(CMP) Combinación no soportada: " + op1 + ", " + op2 + "\n");
    }

    private void procesarINT(String op1, ArrayList<Bit8> bytesInstr) {
        // INT 21h
        if (op1.equalsIgnoreCase("21h") || op1.equals("21")) {
            agregarBytes(bytesInstr, OP_INT_21);
            agregarInstruccion(bytesInstr);
            return;
        }
        out.append("(INT) Interrupción no soportada: " + op1 + "\n");
    }

    private void procesarSALTO(String instr, String op1, ArrayList<Bit8> bytesInstr) {
        switch (instr) {
        case "JMP":
            agregarBytes(bytesInstr, OP_JMP);
            break;
        case "JGE":
            agregarBytes(bytesInstr, OP_JGE);
            break;
        case "JLE":
            agregarBytes(bytesInstr, OP_JLE);
            break;
        default:
            out.append("(SALTO) Instrucción no soportada: " + instr + " " + op1 + "\n");
            return;
        }

        // desplazamiento temporal = 0 en Little Endian: PB, PA
        Bit8[] desp0 = direccionBin(0);
        agregarBytes(bytesInstr, desp0);

        // offset actual de ESTE salto (antes de agregarlo a la lista)
        int offsetOrigen = offsetIns;
        int sizeInstr = bytesInstr.size();
        int offsetNext = offsetOrigen + sizeInstr;

        // índice donde se guardará en la lista de instrucciones
        int indice = instrucciones.size();

        // Guardar instrucción con desplazamiento 0 por ahora
        agregarInstruccion(bytesInstr);

        // Registrar salto pendiente para corregir el desplazamiento al final
        saltosPendientes.add(new SaltoPendiente(op1, indice, offsetNext));
    }
    
    /** -- HELPERS --*/
    /** Agregar una instrucción a la lista, en binario*/
    public void agregarInstruccion(ArrayList<Bit8> bytesInstruccion) {
        Bit8[] offset = offsetBin(offsetIns);
        InstruccionBin nueva = new InstruccionBin(offset, bytesInstruccion);
        instrucciones.add(nueva);
        offsetIns += bytesInstruccion.size(); 
    }

    private void emitirConDireccion(ArrayList<Bit8> bytesInstr, Bit8[] opcode, String nombreVar) {
        agregarBytes(bytesInstr, opcode);
        agregarBytes(bytesInstr, direccionBin(getOffsetVar(nombreVar)));
        agregarInstruccion(bytesInstr);
    }

    private void emitirConImm16(ArrayList<Bit8> bytesInstr, Bit8[] opcode, int valor) {
        agregarBytes(bytesInstr, opcode);
        agregarBytes(bytesInstr, decimalABinario16(valor));
        agregarInstruccion(bytesInstr);
    }

    private void emitirConDireccionYDato(ArrayList<Bit8> bytesInstr, Bit8[] opcode, int offset, Bit8[] dato) {
        agregarBytes(bytesInstr, opcode);
        agregarBytes(bytesInstr, direccionBin(offset));
        agregarBytes(bytesInstr, dato);
    }

    /** Revisa si una cadena es un entero valido. */
    private boolean esEntero(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Calcula el offset (en bytes) de una variable por nombre en el segmento .DATA. */
    private int getOffsetVar(String nombreVar) {
        int offset = 0;
        if (tablaSemantica == null) return 0;
        ArrayList<String> nombresProcesados = new ArrayList<>();
        for (Simbolo s : tablaSemantica) {
            if (nombresProcesados.contains(s.nombre)) {
                continue;
            }
            nombresProcesados.add(s.nombre);
            int size = tamanoTipo(s);

            if (s.nombre.equals(nombreVar)) {
                return offset;
            }
            offset += size;
        }
        out.append("[OBJETO] variable no encontrada: " + nombreVar + "\n");
        return 0;
    }

    /** Tamaño de una variable por nombre, en bytes. */
    private int getSizeVar(String nombreVar) {
        if (tablaSemantica == null) return 0;

        for (Simbolo s : tablaSemantica) {
            if (s.nombre.equals(nombreVar)) {
                return tamanoTipo(s);
            }
        }
        return 0;
    }
    
    /** Devolver tamaño de una variable con el nombre*/
    private boolean esVarEnteraOBool(String nombreVar) {
        int size = getSizeVar(nombreVar);
        return size == 1 || size == 2;
    }

    /** Para meter las instrucciones a la lista que representa el código intermedio que será traducido */
    public void extraerInstrucciones() {
        String texto = in.getText();
        String[] lineas = texto.split("\\R");
        for (String linea : lineas) {
            String trim = linea.trim(); /* Quita espacios antes y después de la línea*/
            if (trim.isEmpty()) continue;
            // Etiquetas
            if (trim.endsWith(":")) {
                String label = trim.substring(0, trim.length() - 1).trim(); 
                if (!label.isEmpty()) {
                    temp.add(new String[]{label, "", ""});
                }
                continue; 
            }
            // Saltar directivas
            if (trim.startsWith(".")) {
                continue;
            }
            // Instrucciones
            String[] partes = trim.split("\\s+", 2);
            String mnem = partes[0];
            String resto = partes.length > 1 ? partes[1] : "";

            String[] instruccionesValidas = {
                    "ADD", "SUB", "MOV", "CMP",
                    "MUL", "JGE", "JLE", "JMP", "INT"
            };
            boolean relevante = java.util.Arrays.asList(instruccionesValidas).contains(mnem);
            if (!relevante) continue;
            String instr = partes[0]; 
            String op1 = "";
            String op2 = "";
            if (!resto.isBlank()) {
                String[] ops = resto.split(",");
                op1 = ops[0].trim();
                if (ops.length > 1) {
                    op2 = ops[1].trim();
                }
            }
            temp.add(new String[]{instr, op1, op2});
        }
    }
    
    /** Devuelve el offset de la instrucción que está debajo de la etiqueta */
    private int getOffsetInstruccionDebajo(String etiqueta) {
        for (int i = 0; i < etiquetas.size(); i++) {
            if (etiquetas.get(i).equals(etiqueta)) {
                return offsetEtiquetas.get(i);
            }
        }
        return -1;
    }

    /** Ajusta los desplazamientos de todos los saltos pendientes */
    private void resolverSaltos() {
        for (SaltoPendiente sp : saltosPendientes) {
            int offsetDestino = getOffsetInstruccionDebajo(sp.etiqueta);
            if (offsetDestino < 0) {
                out.append("  [SALTO] etiqueta no encontrada: " + sp.etiqueta + "\n");
                continue;
            }

            int disp = offsetDestino - sp.offsetNext;
            Bit8[] despBits = direccionBin(disp);
            InstruccionBin ins = instrucciones.get(sp.indiceInstr);
            ArrayList<Bit8> lista = ins.instruccion;
            int n = lista.size();
            lista.set(n - 2, despBits[0]); // PB
            lista.set(n - 1, despBits[1]); // PA
        }
    }

    /** Devuelve el tamaño en bytes del tipo de dato del símbolo. */
    private int tamanoTipo(Simbolo s) { 
        return s.tipo.equalsIgnoreCase("int") ? 2
                : s.tipo.equalsIgnoreCase("boolean") ? 1
                : 0;
    }

    /** Agrega todos los bytes al ArrayList destino */
    private void agregarBytes(ArrayList<Bit8> destino, Bit8[] origen) {
        for (Bit8 b : origen) {
            destino.add(b);
        }
    }

    /** OFFSET a Binario */
    public Bit8[] offsetBin(int numero) {
        Bit8[] bytes = new Bit8[2];
        String bin = String.format("%16s", Integer.toBinaryString(numero & 0xFFFF))
                .replace(' ', '0');
        String high = bin.substring(0, 8);
        String low  = bin.substring(8, 16);
        bytes[0] = binAByte(high);
        bytes[1] = binAByte(low);
        return bytes;
    }

    /** Direccion/desplazamiento de 16 bits en formato x86: byte bajo, byte alto. */
    private Bit8[] direccionBin(int numero) {
        return decimalABinario16(numero);
    }

    /** Decimal a 8-bits */
    public Bit8[] decimalABinario(int numero) {
        Bit8[] bytes = new Bit8[1]; 
        String bin = String.format("%8s", Integer.toBinaryString(numero & 0xFF))
                .replace(' ', '0');
        bytes[0] = binAByte(bin);
        return bytes;
    }

    /** Decimal a 16-bits */
    public Bit8[] decimalABinario16(int numero) {
        Bit8[] bytes = new Bit8[2];
        String bin = String.format("%16s", Integer.toBinaryString(numero & 0xFFFF))
                .replace(' ', '0');
        String high = bin.substring(0, 8);
        String low  = bin.substring(8, 16);
        bytes[0] = binAByte(low);
        bytes[1] = binAByte(high);
        return bytes;
    }
    
    /** Convierte un string binario a bits booleanos */
    private static Bit8 binAByte(String bin) {
        return new Bit8(
                bin.charAt(0) == '1',
                bin.charAt(1) == '1',
                bin.charAt(2) == '1',
                bin.charAt(3) == '1',
                bin.charAt(4) == '1',
                bin.charAt(5) == '1',
                bin.charAt(6) == '1',
                bin.charAt(7) == '1'
                );
    }

    /** Helper para construir varios bytes seguidos */
    public static Bit8[] bits(String... bytes) {
        Bit8[] arr = new Bit8[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            arr[i] = binAByte(bytes[i]);
        }
        return arr;
    }

    /** TODOS LOS OPCODES ESTÁTICOS LISTOS PARA USARSE**/
    /* ADD */
    public static final Bit8[] OP_ADD_AX_MEM = bits(
            "00000011",
            "00000110"
            );
    public static final Bit8[] OP_ADD_MEM_AX = bits(
            "00000001",
            "00000110"
            );
    public static final Bit8[] OP_ADD_AX_IMM = bits(
            "10000001",
            "11000000"
            );
    public static final Bit8[] OP_ADD_MEM_IMM16 = bits(
            "10000001",
            "00000110"
            );


    /* SUB */
    public static final Bit8[] OP_SUB_AX_MEM = bits(
            "00101011",
            "00000110"
            );
    public static final Bit8[] OP_SUB_MEM_AX = bits(
            "00101001",
            "00000110"
            );
    public static final Bit8[] OP_SUB_AX_IMM = bits(
            "10000001",
            "11101000"
            );
    public static final Bit8[] OP_SUB_MEM_IMM16 = bits(
            "10000001",
            "00101110"
            );


    /* MUL */
    public static final Bit8[] OP_MUL_DX = bits(
            "11110111",
            "11100010"
            );
    

    /* MOV */
    public static final Bit8[]OP_MOV_AX_400C = bits(
            "10111000" 
            );
    public static final Bit8[] OP_MOV_DS_AX = bits(
            "10001110",
            "11011000" 
            );
    public static final Bit8[] OP_MOV_AX_MEM = bits(
            "10001011",
            "00000110"
            );
    public static final Bit8[] OP_MOV_DX_MEM = bits(
            "10001011",
            "00010110"
            );
    public static final Bit8[] OP_MOV_MEM_AX = bits(
            "10001001",
            "00000110"
            );
    public static final Bit8[] OP_MOV_AL_MEM_CORTO = bits(
            "10100000"
            );
    public static final Bit8[] OP_MOV_AX_MEM_CORTO = bits(
            "10100001"
            );
    public static final Bit8[] OP_MOV_MEM_AL_CORTO = bits(
            "10100010"
            );
    public static final Bit8[] OP_MOV_MEM_AX_CORTO = bits(
            "10100011"
            );
    public static final Bit8[] OP_MOV_AX_IMM = bits(
            "10111000"
            );
    public static final Bit8[] OP_MOV_DX_IMM = bits(
            "10111010"
            );
    public static final Bit8[] OP_MOV_MEM_IMM8 = bits(
            "11000110",
            "00000110"
            );
    public static final Bit8[] OP_MOV_MEM_IMM16 = bits(
            "11000111",
            "00000110"
            );

    
    /* CMP */
    public static final Bit8[] OP_CMP_AX_IMM = bits(
            "10000001",
            "11111000"
            );
    public static final Bit8[] OP_CMP_DX_AX = bits(
            "00111011",
            "11010000"
        );

    
    /* SALTOS */
    public static final Bit8[] OP_JGE = bits(
            "00001111",
            "10001101"
            );
    public static final Bit8[] OP_JLE = bits(
            "00001111",
            "10001110"
            );
    public static final Bit8[] OP_JMP = bits(
            "11101001"
            );

    
    /* INT */
    public static final Bit8[] OP_INT_21 = bits(
            "11001101",
            "00100001"
            );
}
