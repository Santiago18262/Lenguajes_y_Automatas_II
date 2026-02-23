package data;

import java.util.List;

public class Parser {
    private List<Token> listaTokens;
    private int posicionActual = 0;
    private StringBuilder mensajesError = new StringBuilder();

    public static final int C_CLASS = 1;
    public static final int C_BOOLEAN = 2;
    public static final int C_INT = 3;
    public static final int C_WHILE = 4;
    public static final int C_TRUE = 5;
    public static final int C_FALSE = 6;
    public static final int C_PUNTOCOMA = 7;
    public static final int C_LLAVEABRE = 8;
    public static final int C_LLAVECIERRA = 9;
    public static final int C_PARENTABRE = 10;
    public static final int C_PARENTCIERRA = 11;
    public static final int C_OPMAS = 12;
    public static final int C_OPMENOS = 13;
    public static final int C_OPMULTI = 14;
    public static final int C_CMPMAY = 15;
    public static final int C_CMPMEN = 16;
    public static final int C_ASIGNACION = 17;
    public static final int C_IDENTIFICADOR = 18;
    public static final int C_NUMENTERO = 19;
    public static final int C_EOF = 20;

    public Parser(List<Token> tokens) { this.listaTokens = tokens; }

    public void asignarCodigos() {
        for (Token token : listaTokens) {
            switch (token.tipo) {
                case PalabraReservada: token.codigo = codigoReservada(token.valor); break;
                case Identificador:
                    int cod = codigoReservada(token.valor); 
                    if (cod != 0) {
                        token.codigo = cod;
                    } else {
                        token.codigo = C_IDENTIFICADOR;
                    }
                    break;
                case NumEntero: token.codigo = C_NUMENTERO; break;
                case PuntoComa: token.codigo = C_PUNTOCOMA; break;
                case LlaveAbre: token.codigo = C_LLAVEABRE; break;
                case LlaveCierra: token.codigo = C_LLAVECIERRA; break;
                case ParentAbre: token.codigo = C_PARENTABRE; break;
                case ParentCierra: token.codigo = C_PARENTCIERRA; break;
                case OpMAS: token.codigo = C_OPMAS; break;
                case OpMENOS: token.codigo = C_OPMENOS; break;
                case OpMULTI: token.codigo = C_OPMULTI; break;
                case CMPMAY: token.codigo = C_CMPMAY; break;
                case CMPMEN: token.codigo = C_CMPMEN; break;
                case Asignacion: token.codigo = C_ASIGNACION; break;
                case EOF: token.codigo = C_EOF; break;
                default: token.codigo = 0; break;
            }
        }
    }

    private int codigoReservada(String lexema) {
        switch (lexema) {
            case "class": return C_CLASS;
            case "boolean": return C_BOOLEAN;
            case "int": return C_INT;
            case "while": return C_WHILE;
            case "true": return C_TRUE;
            case "false": return C_FALSE;
            case "EOF": return C_EOF;
            default: return 0;
        }
    }

    public boolean analizar(Semantico sem) {
        asignarCodigos();
        this.posicionActual = 0;
        this.mensajesError = new StringBuilder();
        return parsearPrograma(sem);
    }

    /** Programa → class Identificador { ListaDeclaración ListaSentencias } EOF */
    public boolean parsearPrograma(Semantico sem) {
        int inicio = posicionActual;
        if (tokenActualEs(C_CLASS)) {
            posicionActual++;
            if (tokenActualEs(C_IDENTIFICADOR)) {
                String nombre = listaTokens.get(posicionActual).valor;
                if (sem != null) sem.registrarNombreClase(nombre);
                posicionActual++;
                if (tokenActualEs(C_LLAVEABRE)) {
                    posicionActual++;
                    if (parsearListaDeclaracion(sem)) {
                        if (parsearListaSentencias(sem)) {
                            if (tokenActualEs(C_LLAVECIERRA)) {
                                posicionActual++;
                                if (tokenActualEs(C_EOF)) {
                                    posicionActual++;
                                    return true;
                                } else {
                                    mensajesError.append("Error sintáctico: se esperaba EOF al final del programa.");
                                }
                            }
                        }
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** ListaDeclaracion → [[DeclaracionVar;]]* */
    public boolean parsearListaDeclaracion(Semantico sem) {
        boolean encontrado = false;
        while (parsearDeclaracionVar(sem)) encontrado = true;
        if (!encontrado) return true;
        return true;
    }

    /** DeclaracionVar → TipoDato Identificador ; */
    public boolean parsearDeclaracionVar(Semantico sem) {
        int inicio = posicionActual;
        if (parsearTipoDato()) {
            String tipo = tokenActualEs(C_INT) ? "int" : "boolean";
            posicionActual++;
            if (tokenActualEs(C_IDENTIFICADOR)) {
                String nombre = listaTokens.get(posicionActual).valor;
                if (sem != null){ sem.declarar(nombre, tipo);} // ✅ semántico: redeclaración + tabla
                posicionActual++;
                if (tokenActualEs(C_PUNTOCOMA)) {
                    posicionActual++;
                    return true;
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** TipoDato → boolean | int */
    public boolean parsearTipoDato() { return tokenActualEs(C_INT) || tokenActualEs(C_BOOLEAN); }

    /** ListaSentencias → [[Sentencias]]* */
    public boolean parsearListaSentencias(Semantico sem) {
        boolean encontrado = false;
        while (parsearSentencias(sem)) encontrado = true;
        if (!encontrado) return true;
        return true;
    }

    /** Sentencias → while (ExpresionBooleana){ListaSentencias} | Identificador=Expresion; | Identificador=ExpresionBooleana; */
    public boolean parsearSentencias(Semantico sem) {
        int inicio = posicionActual;
        if (tokenActualEs(C_WHILE)) { if (parsearWhile(sem)) return true; }
        posicionActual = inicio;
        if (tokenActualEs(C_IDENTIFICADOR)) { if (parsearAsignacion(sem) || parsearAsignacionBooleana(sem)) return true; }
        posicionActual = inicio;
        return false;
    }

    /** while ( ExpresionBooleana ) { ListaSentencias } */
    public boolean parsearWhile(Semantico sem) {
        int inicio = posicionActual;
        if (tokenActualEs(C_WHILE)) {
            posicionActual++;
            if (tokenActualEs(C_PARENTABRE)) {
                posicionActual++;
                if (parsearExpresionBooleana(sem)) {
                    if (tokenActualEs(C_PARENTCIERRA)) {
                        posicionActual++;
                        if (tokenActualEs(C_LLAVEABRE)) {
                            posicionActual++;
                            if (parsearListaSentencias(sem)) {
                                if (tokenActualEs(C_LLAVECIERRA)) {
                                    posicionActual++;
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }
    
    /** Identificador = Expresion ; */
    public boolean parsearAsignacion(Semantico sem) {
        int inicio = posicionActual;
        if (tokenActualEs(C_IDENTIFICADOR)) {
            String nombreVar = listaTokens.get(posicionActual).valor;
            if (sem != null) sem.usar(nombreVar);
            posicionActual++;
            if (tokenActualEs(C_ASIGNACION)) {
                posicionActual++;
                int iniExpr = posicionActual;
                if (parsearExpresion(sem)) {
                    int finExpr = posicionActual;
                    if (tokenActualEs(C_PUNTOCOMA)) {
                        if (sem != null) sem.asignacionArit(nombreVar, listaTokens.subList(iniExpr, finExpr));
                        posicionActual++;
                        return true;
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** Identificador = ExpresionBooleana ; */
    public boolean parsearAsignacionBooleana(Semantico sem) {
        int inicio = posicionActual;
        if (tokenActualEs(C_IDENTIFICADOR)) {
            String nombreVar = listaTokens.get(posicionActual).valor;
            if (sem != null) sem.usar(nombreVar);
            posicionActual++;
            if (tokenActualEs(C_ASIGNACION)) {
                posicionActual++;
                int iniExpr = posicionActual;
                if (parsearExpresionBooleana(sem)) {
                    int finExpr = posicionActual;
                    if (tokenActualEs(C_PUNTOCOMA)) {
                        if (sem != null) sem.asignacionBool(nombreVar,listaTokens.subList(iniExpr, finExpr));
                        posicionActual++;
                        return true;
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** ExpresionBooleana → Expresion CMP Expresion | true | false */
    public boolean parsearExpresionBooleana(Semantico sem) {
        int inicio = posicionActual;
        if (tokenActualEs(C_TRUE) || tokenActualEs(C_FALSE)) { 
            if (sem != null) {
            // Mandamos solo el token true/false para validar
            sem.validarCondicionWhile(listaTokens.subList(posicionActual, posicionActual + 1));
            }   
            posicionActual++; 
            return true; 
        }
        if (parsearExpresion(sem)) {
            if (parsearComparador()) {
                posicionActual++;
                if (parsearExpresion(sem))
                    if (sem != null) {
                    // Enviamos todos los tokens desde 'inicio' hasta 'posicionActual'
                    sem.validarCondicionWhile(listaTokens.subList(inicio, posicionActual));
                    }
                	return true;
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** Expresion → Termino OP Termino */
    public boolean parsearExpresion(Semantico sem) {
        int inicio=posicionActual;
        if (parsearTermino(sem)) {
            posicionActual++;
            while (posicionActual < listaTokens.size() && parsearOperador()) {
                posicionActual++;
                if (parsearTermino(sem)) {
                    posicionActual++;
                } else {
                    posicionActual=inicio;
                    return false;   
                }
            }
            return true;
        }
        posicionActual=inicio;
        return false;
    }

    /** Expresion → Identificador | NumEntero */
    public boolean parsearTermino(Semantico sem) { 
        if (tokenActualEs(C_IDENTIFICADOR)){
            if (sem != null) sem.usar(listaTokens.get(posicionActual).valor); // Uso de variable dentro de semantico sirve para detectar variables no declaradas
            return true;
        }   
        return tokenActualEs(C_NUMENTERO); 
    }

    /** OP → + | - | * */
    public boolean parsearOperador() { return tokenActualEs(C_OPMAS) || tokenActualEs(C_OPMENOS) || tokenActualEs(C_OPMULTI); }

    /** CMP → > | < */
    public boolean parsearComparador() { return tokenActualEs(C_CMPMAY) || tokenActualEs(C_CMPMEN); }

    private boolean tokenActualEs(int codigoEsperado) {
        if (posicionActual < listaTokens.size()) return listaTokens.get(posicionActual).codigo == codigoEsperado;
        return false;
    }

    public String getErrores() { return mensajesError.toString(); }
    public List<Token> getTokens() { return listaTokens; }
}