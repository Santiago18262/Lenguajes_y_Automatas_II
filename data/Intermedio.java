package data;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;
import data.Semantico.Simbolo;

public class Intermedio {
    private List<Token> listaTokens;
    private List<Simbolo> tablaSemantica;
    private JTextArea out;
    private int posicionActual = 0;
    private int contadorTemporales = 0;
    private String nombreClase;

    public Intermedio(List<Token> listaTokens, List<Simbolo> tablaSemantica, JTextArea out) {
        this.listaTokens = listaTokens;
        this.tablaSemantica = tablaSemantica;
        this.out = out;
    }

    // Imprime una línea de código en formato tipo ensamblador
    private void col(String etiqueta, String instruccion, String operandos) {
        String fila = String.format("%-16s %-14s %s\n", 
                                    (etiqueta != null ? etiqueta : ""), 
                                    (instruccion != null ? instruccion : ""), 
                                    (operandos != null ? operandos : ""));
        out.append(fila);
    }

    // Crea una variable temporal para guardar resultados intermedios de operaciones
    private String nuevaTemporal() {
        String nombre = "T" + contadorTemporales++;
        if (!existeSimbolo(nombre)) {
            tablaSemantica.add(new Simbolo(nombre, "int", "?", 0));
        }
        return nombre;
    }

    private boolean existeSimbolo(String nombre) {
        for (Simbolo s : tablaSemantica) {
            if (s.getNombre().equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    public void imprimirTodo() {         
        JTextArea finalOut = this.out; 
        this.out = new JTextArea(); 
        this.posicionActual = 0; 
        capturarNombreClase();
        leerProgram(); 
        
        // Se reinicia el análisis para generar ahora el código final
        this.out = finalOut; 
        this.out.setText(""); // Limpiar el área de texto antes de imprimir el código final
        this.contadorTemporales = 0;
        this.posicionActual = 0; 
        
        imprimirHeader();
        imprimirData(); 
        imprimirCODE(); 
        leerProgram(); 
        imprimirEND();
    }
    
    // Encabezado del programa ensamblador
    public void imprimirHeader() {
        col(null, "TITLE", nombreClase);
        col(null, ".MODEL", "SMALL");
        col(null, ".STACK", "100h");
    }

    // Sección de variables del programa
    public void imprimirData() {
        out.append("\n");
        col(null, ".DATA", null);

        // Recorre todos los símbolos detectados por el análisis semántico
        for (Simbolo s : tablaSemantica) {

            // Boolean se guarda como byte, int como word
            String directiva = s.getTipo().equals("boolean") ? "DB" : "DW";
            col(s.getNombre(), directiva, "?");
        }
    }
    
    // Inicio del código ejecutable
    public void imprimirCODE() {
        out.append("\n");
        col(null, ".CODE", null);

        col("MAIN", "PROC", "FAR");
        col(null, "MOV", "AX, @data");
        col(null, "MOV", "DS, AX");
        out.append("\n");
    }
    
    // Finalización del programa
    public void imprimirEND() {
        col(null, "MOV", "AX, 4C00h");
        col(null, "INT", "21h");
        col(null, "MAIN", "ENDP");
        col(null, "END", "MAIN");
    }

    // Lee la estructura general del programa: class { ListaDeclaraciones Sentencias } EOF
    public boolean leerProgram() {
        if (tokenActualEs(Parser.C_CLASS)) {
            posicionActual += 2; 
            if (tokenActualEs(Parser.C_LLAVEABRE)) {
                posicionActual++;
                leerListaDeclaracion();
                leerListaSentencias();
                if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                    posicionActual++;
                    return tokenActualEs(Parser.C_EOF);
                }
            }
        }
        return false;
    }

    // Procesa declaraciones de variables
    private void leerListaDeclaracion() {
        while (tokenActualEs(Parser.C_INT) || tokenActualEs(Parser.C_BOOLEAN)) {
            posicionActual += 2;
            if (tokenActualEs(Parser.C_PUNTOCOMA)) posicionActual++;
        }
    }

    // Procesa múltiples sentencias hasta que ya no encuentre más
    private void leerListaSentencias() {
        while (leerSentencias()) { }
    }

    // Detecta qué tipo de sentencia se está leyendo (while o asignación)
    private boolean leerSentencias() {
        int inicio = posicionActual;

        // --- SENTENCIA WHILE ---
        if (tokenActualEs(Parser.C_WHILE)) {

            // Se avanza hasta el inicio de la condición
            posicionActual += 2; 

            int condIni = posicionActual;

            // Se salta la expresión booleana para ubicar su final
            saltarExpresionBooleana();
            int condFin = posicionActual;
            
            if (tokenActualEs(Parser.C_PARENTCIERRA)) {
                posicionActual++;

                // Etiquetas para controlar el ciclo
                String etqIni = "W_INI" + inicio;
                String etqFin = "W_FIN" + inicio;
                
                // Etiqueta inicio del ciclo
                col(etqIni + ":", null, null);
                
                // Genera la comparación de la condición
                imprimirComparacionCompleja(listaTokens.subList(condIni, condFin), etqFin);
                
                if (tokenActualEs(Parser.C_LLAVEABRE)) {
                    posicionActual++;
                    leerListaSentencias();
                    if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                        posicionActual++;

                        // Salto al inicio del ciclo
                        col(null, "JMP", etqIni);

                        // Etiqueta de salida del ciclo
                        col(etqFin + ":", null, null);
                        return true;
                    }
                }
            }
        }

        // --- SENTENCIA DE ASIGNACIÓN ---
        posicionActual = inicio;
        if (tokenActualEs(Parser.C_IDENTIFICADOR)) {

            // Variable destino
            String varDestino = listaTokens.get(posicionActual).valor;

            posicionActual += 2; 

            int exprIni = posicionActual;

            // Se salta la expresión aritmética
            saltarExpresion(); 

            int exprFin = posicionActual;
            
            if (tokenActualEs(Parser.C_PUNTOCOMA)) {
                posicionActual++;

                // Genera el código aritmético y deja el resultado en AX
                generarCodigoAritmetico(exprIni, exprFin, "AX");

                // Guarda el resultado en la variable destino
                col(null, "MOV", varDestino + ", AX");

                return true;
            }
        }
        return false;
    }

    // Genera código para expresiones aritméticas respetando prioridad de operadores
    private void generarCodigoAritmetico(int ini, int fin, String registroDestino) {

        // Se copia la sublista de tokens de la expresión para manipularla
        List<Token> tokensExp = new ArrayList<>(listaTokens.subList(ini, fin));

        // --- PRIMERA PASADA: MULTIPLICACIONES ---
        // Se resuelven primero porque tienen mayor prioridad
        for (int i = 0; i < tokensExp.size(); i++) {                        
            if (tokensExp.get(i).codigo == Parser.C_OPMULTI) {                 

                // Operandos de la multiplicación
                String izq = tokensExp.get(i - 1).valor;
                String der = tokensExp.get(i + 1).valor;

                // Se crea un temporal para guardar el resultado
                String tmp = nuevaTemporal();

                // Se genera el código ensamblador
                col(null, "MOV", "AX, " + izq);
                col(null, "MOV", "DX, " + der);
                col(null, "MUL", "DX");
                col(null, "MOV", tmp + ", AX");
 
                // Se reduce la expresión reemplazando "a * b" por el temporal
                tokensExp.remove(i + 1);
                tokensExp.remove(i);
                
                Token tokenTmp = new Token(Token.TokenTipo.Identificador, tmp);
                tokenTmp.codigo = Parser.C_IDENTIFICADOR;

                tokensExp.set(i - 1, tokenTmp);

                // Se retrocede el índice para continuar evaluando correctamente
                i--;
            }
        }
    
        // --- SEGUNDA PASADA: SUMAS Y RESTAS ---
        if (tokensExp.size() > 0) {

            // Cargar el primer valor en AX
            col(null, "MOV", "AX, " + tokensExp.get(0).valor);

            for (int i = 1; i < tokensExp.size(); i += 2) {

                int op = tokensExp.get(i).codigo;
                String val = tokensExp.get(i + 1).valor;

                // Generar instrucción según operador
                if (op == Parser.C_OPMAS) {
                    col(null, "ADD", "AX, " + val);
                } else if (op == Parser.C_OPMENOS) {
                    col(null, "SUB", "AX, " + val);
                }
            }
            
            // Si el resultado debe guardarse en otro registro
            if (!registroDestino.equals("AX")) {
                col(null, "MOV", registroDestino + ", AX");
            }
        }
    }

    // Genera la comparación para condiciones del while
    private void imprimirComparacionCompleja(List<Token> tokens, String etiquetaFalsa) {

        int iCmp = -1;

        // Busca el operador de comparación dentro de la lista
        for (int i = 0; i < tokens.size(); i++) {
            int cod = tokens.get(i).codigo;
            if (cod == Parser.C_CMPMAY || cod == Parser.C_CMPMEN) {
                iCmp = i; break;
            }
        }

        if (iCmp != -1) {

            // Temporal para guardar el resultado de la expresión izquierda
            String tempIzq = nuevaTemporal();

            generarCodigoAritmetico(listaTokens.indexOf(tokens.get(0)), listaTokens.indexOf(tokens.get(iCmp)), "AX");
            col(null, "MOV", tempIzq + ", AX");

            // Se evalúa la expresión derecha
            generarCodigoAritmetico(listaTokens.indexOf(tokens.get(iCmp + 1)), listaTokens.indexOf(tokens.get(tokens.size()-1)) + 1, "AX");

            // Comparación entre ambos resultados
            col(null, "MOV", "DX, " + tempIzq);  // cargar temporal en DX
            col(null, "CMP", "DX, AX");  // comparar registro vs registro
            
            // Dependiendo del operador se genera el salto correspondiente
            String salto = (tokens.get(iCmp).codigo == Parser.C_CMPMAY) ? "JLE" : "JGE";
            col(null, salto, etiquetaFalsa);
        }
    }

    // Avanza hasta encontrar punto y coma
    private void saltarExpresion() {
        while (posicionActual < listaTokens.size() && listaTokens.get(posicionActual).codigo != Parser.C_PUNTOCOMA) {
            posicionActual++;
        }
    }
    

    // Avanza hasta cerrar el paréntesis de la condición
    private void saltarExpresionBooleana() {
        while (posicionActual < listaTokens.size() && listaTokens.get(posicionActual).codigo != Parser.C_PARENTCIERRA) {
            posicionActual++;
        }
    }

    // Guarda el nombre de la clase para el TITLE del programa
    private void capturarNombreClase() {
        if (listaTokens.size() > 1 && listaTokens.get(0).codigo == Parser.C_CLASS) {
            nombreClase = listaTokens.get(1).valor;
        }
    }

    // Verifica si el token actual coincide con el código esperado
    private boolean tokenActualEs(int codigo) {
        return posicionActual < listaTokens.size() && listaTokens.get(posicionActual).codigo == codigo;
    }
}
