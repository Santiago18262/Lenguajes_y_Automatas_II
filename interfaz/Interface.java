package interfaz;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import data.Scanner;
import data.Semantico;
import data.Parser;
import data.Token;

public class Interface extends JFrame {

    private static final long serialVersionUID = 1L;

    // Áreas
    private JTextArea areaCodigo = new JTextArea();
    private JTextArea areaErrores = new JTextArea();

    // Tabla de símbolos
    private DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"Tipo", "Token"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private JTable tablaTokens = new JTable(modeloTabla);

    // Botones
    private JButton btnTokens;
    private JButton btnParser;
    private JButton btnSemantico;

    // Ultima lista de tokens
    private List<Token> ultimaLista;     // tokens del último análisis léxico

    public Interface() {
        super("MicroJavaCompiler");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initComponents();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }

    private void initComponents() {
        // Título
        JLabel titulo = new JLabel("MicroJavaCompiler");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 30));
        titulo.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        Dimension tamanoReducido = new Dimension(800, 500);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(titulo, BorderLayout.NORTH);
        
        // Fila única en uso (Programa, Tokens)
        JPanel filaSuperior = new JPanel(new GridLayout(1, 3, 10, 10));

        // Programa
        JPanel panelPrograma = new JPanel(new BorderLayout());
        panelPrograma.setBorder(new TitledBorder("Programa"));
        ((TitledBorder)panelPrograma.getBorder()).setTitleFont(new Font("SansSerif", Font.BOLD, 22));
        areaCodigo.setFont(new Font("Consolas", Font.PLAIN, 22));
        panelPrograma.add(new JScrollPane(areaCodigo), BorderLayout.CENTER);

        // Tokens
        JPanel panelTokens = new JPanel(new BorderLayout());
        panelTokens.setBorder(new TitledBorder("Tabla de símbolos"));
        ((TitledBorder)panelTokens.getBorder()).setTitleFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel barraTokens = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnTokens = new JButton("Tokens");
        btnTokens.setFont(new Font("SansSerif", Font.BOLD, 22));
        btnTokens.addActionListener(e -> analizarLexico());
        barraTokens.add(btnTokens);
        panelTokens.add(barraTokens, BorderLayout.NORTH);

        tablaTokens.setFillsViewportHeight(true);
        tablaTokens.setFont(new Font("Consolas", Font.PLAIN, 22));
        tablaTokens.setRowHeight(28);
        tablaTokens.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 22));
        tablaTokens.setDefaultRenderer(Object.class, new ValidaRenderer());
        panelTokens.add(new JScrollPane(tablaTokens), BorderLayout.CENTER);

        JPanel contenedorSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        contenedorSuperior.add(filaSuperior);

        // Errores
        JPanel panelErrores = new JPanel(new BorderLayout());
        panelErrores.setBorder(new TitledBorder("Errores"));
        ((TitledBorder)panelErrores.getBorder()).setTitleFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel barraErrores = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnParser = new JButton("Parser");
        btnParser.setFont(new Font("SansSerif", Font.BOLD, 22));
        btnParser.setEnabled(false);
        btnParser.addActionListener(e -> ejecutarParser());
        barraErrores.add(btnParser);

        btnSemantico = new JButton("Semantico");
        btnSemantico.setFont(new Font("SansSerif", Font.BOLD, 22));
        btnSemantico.setEnabled(false);
        btnSemantico.addActionListener(e -> ejecutarSemantico());
        barraErrores.add(btnSemantico);

        panelErrores.add(barraErrores, BorderLayout.NORTH);

        areaErrores.setEditable(false);
        areaErrores.setFont(new Font("Consolas", Font.PLAIN, 22));
        panelErrores.add(new JScrollPane(areaErrores), BorderLayout.CENTER);

        filaSuperior.add(panelPrograma);
        filaSuperior.add(panelTokens);
        filaSuperior.add(panelErrores);

        JPanel filaInferior = new JPanel(new GridLayout(1, 3, 10, 10));

        JPanel centro = new JPanel(new GridLayout(2, 1, 10, 10));
        centro.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        centro.add(filaSuperior);
        centro.add(filaInferior);
    
        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(norte, BorderLayout.NORTH);
        getContentPane().add(centro, BorderLayout.CENTER);
    }

    private void analizarLexico() {
        modeloTabla.setRowCount(0);
        areaErrores.setText("");
        areaErrores.setForeground(Color.BLACK);
        areaErrores.setFont(new Font("Consolas", Font.PLAIN, 22));

        String codigo = areaCodigo.getText();
        Scanner analizador = new Scanner(codigo);

        int errores = 0;
        StringBuilder sb = new StringBuilder();
        List<Token> tmp = new ArrayList<>();

        while (true) {
            Token tk = analizador.siguienteToken();
            tmp.add(tk);

            // Agregamos TODAS las filas, incluyendo EOF (se pintará gris en el renderer)
            modeloTabla.addRow(new Object[]{ tk.tipo, tk.valor });

            if (tk.tipo == Token.TokenTipo.Invalido) {
                errores++;
                sb.append("Error léxico: token inválido -> ").append(tk.valor).append('\n');
            }
            if (tk.tipo == Token.TokenTipo.EOF) break;
        }

        ultimaLista = tmp;
        if (errores > 0) {
            areaErrores.setForeground(Color.RED);
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText(sb.toString());
            btnParser.setEnabled(false); // Habilitar Parser cuando Lexico este correcto
            JOptionPane.showMessageDialog(this,
                    "Se detectaron " + errores + " error(es) léxico(s).",
                    "Análisis con errores",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            areaErrores.setForeground(new Color(0, 128, 0));
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText("Análisis léxico correcto.\n");
            btnParser.setEnabled(true);
            JOptionPane.showMessageDialog(this,
                    "Análisis léxico correcto.",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
        tablaTokens.repaint();
    }

    /** Llama a Parser.analizar() y muestra resultado en CI y Errores. */
    private void ejecutarParser() {
        if (ultimaLista == null) {
            JOptionPane.showMessageDialog(this,
                    "Primero ejecuta Tokens.",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Parser p = new Parser(ultimaLista);
        boolean exito = p.analizar(null); 

        if (exito) {
            areaErrores.setForeground(new Color(0, 128, 0));
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText("Análisis sintáctico correcto.\n");
            btnSemantico.setEnabled(true); // Habilitar Semántico cuando Parser este correcto
            JOptionPane.showMessageDialog(this,
                    "El análisis sintáctico se completó sin errores.",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } else {
            areaErrores.setForeground(Color.RED);
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText("SYNTAX ERROR\n\n" + p.getErrores());
            JOptionPane.showMessageDialog(this,
                    "Se encontraron errores sintácticos.",
                    "Errores", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Llama a Semantico.analizar()
    private void ejecutarSemantico() {
        if (ultimaLista == null) {
            JOptionPane.showMessageDialog(this,
                    "Primero ejecuta Tokens.",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Reutilizamos los mismos tokens que ya fueron codificados por Parser en ejecutarParser()
        Semantico sem = new Semantico(ultimaLista);
        boolean ok = sem.analizar();

        if (ok) {
            areaErrores.setForeground(new Color(0, 128, 0));
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText("Análisis semántico correcto.\n");
            JOptionPane.showMessageDialog(this,
                    "El análisis semántico se completó sin errores.",
                    "Semántico", JOptionPane.INFORMATION_MESSAGE);
        } else {
            areaErrores.setForeground(Color.RED);
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText("SEMANTIC ERROR\n\n" + sem.getErrores());
            JOptionPane.showMessageDialog(this,
                    "Se encontraron errores semánticos.",
                    "Semántico", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class ValidaRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final Color VERDE_CLARO = new Color(214, 245, 214);
        private static final Color ROJO_CLARO  = new Color(248, 215, 218);
        private static final Color GRIS_CLARO  = new Color(230, 230, 230);

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            Object tipo = table.getModel().getValueAt(row, 0);
            boolean esInvalido = (tipo instanceof Token.TokenTipo)
                    && ((Token.TokenTipo) tipo) == Token.TokenTipo.Invalido;
            boolean esEOF = (tipo instanceof Token.TokenTipo)
                    && ((Token.TokenTipo) tipo) == Token.TokenTipo.EOF;

            if (!isSelected) {
                if (esEOF) c.setBackground(GRIS_CLARO);
                else c.setBackground(esInvalido ? ROJO_CLARO : VERDE_CLARO);
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}