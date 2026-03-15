package data;

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

    private void col(String etiqueta, String instruccion, String operandos) {
        String fila = String.format("%-10s %-8s %s\n", 
                                    (etiqueta != null ? etiqueta : ""), 
                                    (instruccion != null ? instruccion : ""), 
                                    (operandos != null ? operandos : ""));
        out.append(fila);
    }

    private String nuevaTemporal() {
        String nombre = "T" + contadorTemporales++;
        tablaSemantica.add(new Simbolo(nombre, "int", "?", 0)); 
        return nombre;
    }

    public void imprimirTodo() {         
        JTextArea finalOut = this.out; 
        this.out = new JTextArea(); 
        this.posicionActual = 0; 
        capturarNombreClase();
        leerProgram(); 
        
        this.out = finalOut; 
        this.contadorTemporales = 0;
        this.posicionActual = 0; 
        
        imprimirHeader();
        imprimirData(); 
        imprimirCODE(); 
        leerProgram(); 
        imprimirEND();
    }
    
    public void imprimirHeader() {
        col("TITLE", nombreClase, "");
        col(null, ".MODEL", "SMALL");
        col(null, ".STACK", "100h");
    }

    public void imprimirData() {
        out.append(".DATA\n");
        for (Simbolo s : tablaSemantica) {
            String directiva = s.getTipo().equals("boolean") ? "DB" : "DW";
            col(s.getNombre(), directiva, "?");
        }
    }
    
    public void imprimirCODE() {
        out.append("\n.CODE\n");
        col("MAIN", "PROC", "FAR");
        col(null, "MOV", "AX, @data");
        col(null, "MOV", "DS, AX");
        out.append("\n");
    }
    
    public void imprimirEND() {
        out.append("\n");
        col(null, "MOV", "AX, 4C00h");
        col(null, "INT", "21h");
        col("MAIN", "ENDP", "");
        col(null, "END", "MAIN");
    }

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

    private void leerListaDeclaracion() {
        while (tokenActualEs(Parser.C_INT) || tokenActualEs(Parser.C_BOOLEAN)) {
            posicionActual += 2;
            if (tokenActualEs(Parser.C_PUNTOCOMA)) posicionActual++;
        }
    }

    private void leerListaSentencias() {
        while (leerSentencias()) { }
    }

    private boolean leerSentencias() {
        int inicio = posicionActual;

        if (tokenActualEs(Parser.C_WHILE)) {
            posicionActual += 2; 
            int condIni = posicionActual;
            saltarExpresionBooleana();
            int condFin = posicionActual;
            
            if (tokenActualEs(Parser.C_PARENTCIERRA)) {
                posicionActual++;
                String etqIni = "W_INI" + inicio;
                String etqFin = "W_FIN" + inicio;
                
                col(etqIni + ":", null, null);
                
                // --- CORRECCIÓN AQUÍ ---
                // Resolvemos la comparación compleja
                imprimirComparacionCompleja(listaTokens.subList(condIni, condFin), etqFin);
                
                if (tokenActualEs(Parser.C_LLAVEABRE)) {
                    posicionActual++;
                    leerListaSentencias();
                    if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                        posicionActual++;
                        col(null, "JMP", etqIni);
                        col(etqFin + ":", null, null);
                        return true;
                    }
                }
            }
        }

        posicionActual = inicio;
        if (tokenActualEs(Parser.C_IDENTIFICADOR)) {
            String varDestino = listaTokens.get(posicionActual).valor;
            posicionActual += 2; 
            int exprIni = posicionActual;
            saltarExpresion(); 
            int exprFin = posicionActual;
            
            if (tokenActualEs(Parser.C_PUNTOCOMA)) {
                posicionActual++;
                generarCodigoAritmetico(exprIni, exprFin, "AX");
                col(null, "MOV", varDestino + ", AX");
                return true;
            }
        }
        return false;
    }

    // Procesa aritmética y deja el resultado en el registro indicado (normalmente AX)
    private void generarCodigoAritmetico(int ini, int fin, String registroDestino) {
        col(null, "MOV", registroDestino + ", " + listaTokens.get(ini).valor);
        
        int i = ini + 1;
        while (i < fin) {
            int op = listaTokens.get(i).codigo;
            String val = listaTokens.get(i + 1).valor;
            
            if (op == Parser.C_OPMAS)         col(null, "ADD", registroDestino + ", " + val);
            else if (op == Parser.C_OPMENOS)  col(null, "SUB", registroDestino + ", " + val);
            else if (op == Parser.C_OPMULTI) {
                String tmp = nuevaTemporal();
                col(null, "MOV", tmp + ", " + val);
                col(null, "MUL", tmp); 
            }
            i += 2;
        }
    }

    /**
     * CORRECCIÓN DE COMPARACIÓN COMPLEJA:
     * Divide la lista en [Expresion Izquierda] [CMP] [Expresion Derecha]
     */
    private void imprimirComparacionCompleja(List<Token> tokens, String etiquetaFalsa) {
        int iCmp = -1;
        for (int i = 0; i < tokens.size(); i++) {
            int cod = tokens.get(i).codigo;
            if (cod == Parser.C_CMPMAY || cod == Parser.C_CMPMEN) {
                iCmp = i; break;
            }
        }

        if (iCmp != -1) {
            // 1. Resolver lado izquierdo y guardar en un temporal
            String tempIzq = nuevaTemporal();
            generarCodigoAritmetico(listaTokens.indexOf(tokens.get(0)), listaTokens.indexOf(tokens.get(iCmp)), "AX");
            col(null, "MOV", tempIzq + ", AX");

            // 2. Resolver lado derecho y dejar en AX
            generarCodigoAritmetico(listaTokens.indexOf(tokens.get(iCmp + 1)), listaTokens.indexOf(tokens.get(tokens.size()-1)) + 1, "AX");

            // 3. Comparar Temporal (Izq) con AX (Der)
            col(null, "CMP", tempIzq + ", AX");
            
            String salto = (tokens.get(iCmp).codigo == Parser.C_CMPMAY) ? "JLE" : "JGE";
            col(null, salto, etiquetaFalsa);
        }
    }

    private void saltarExpresion() {
        while (posicionActual < listaTokens.size() && listaTokens.get(posicionActual).codigo != Parser.C_PUNTOCOMA) {
            posicionActual++;
        }
    }

    private void saltarExpresionBooleana() {
        while (posicionActual < listaTokens.size() && listaTokens.get(posicionActual).codigo != Parser.C_PARENTCIERRA) {
            posicionActual++;
        }
    }

    private void capturarNombreClase() {
        if (listaTokens.size() > 1 && listaTokens.get(0).codigo == Parser.C_CLASS) {
            nombreClase = listaTokens.get(1).valor;
        }
    }

    private boolean tokenActualEs(int codigo) {
        return posicionActual < listaTokens.size() && listaTokens.get(posicionActual).codigo == codigo;
    }
}