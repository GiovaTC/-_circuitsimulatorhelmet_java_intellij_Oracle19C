/*
CircuitSimulatorHelmet.java
Single-file Java Swing application for IntelliJ

Features:
- Simple circuit-like drawing (Arduino-like board + components) themed "Casco Colombia 2026 - F32 Piloto de Combate"
- Interactive GUI to add basic components (LED, Resistor, Wire) onto the canvas
- Power ON/OFF toggle that visually updates components
- Saves circuit information to Oracle 19c via a stored procedure call
- Includes example SQL (table + stored procedure) to create on Oracle side
*/

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CircuitSimulatorHelmet extends JFrame {

    // ---------------- DB CONFIGURATION ----------------
    // URL CORREGIDA (ANTES: jdbc:oracle:thin://@localhost:1521/orcl)
    private static final String DB_URL = "jdbc:oracle:thin://@localhost:1521/orcl";
    private static final String DB_USER = "system";
    private static final String DB_PASSWORD = "Tapiero123";
    private static final String SP_INSERT_LOG = "INSERT_CIRCUIT_LOG";

    // ---------------- UI / STATE ----------------
    private final DrawingPanel drawingPanel = new DrawingPanel();
    private final DefaultTableModel logTableModel =
            new DefaultTableModel(new Object[]{"Timestamp", "Event", "Details"}, 0);

    private boolean powerOn = false;
    private final JLabel powerLabel = new JLabel("OFF");
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            CircuitSimulatorHelmet app = new CircuitSimulatorHelmet();
            app.setVisible(true);
        });
    }

    public CircuitSimulatorHelmet() {
        setTitle("Circuit Simulator - Casco Colombia 2026 (F32 Piloto de Combate)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Left panel (controls)
        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setPreferredSize(new Dimension(300, 0));
        left.add(buildControlPanel(), BorderLayout.NORTH);
        left.add(buildLogPanel(), BorderLayout.CENTER);
        add(left, BorderLayout.WEST);

        // Center (canvas)
        drawingPanel.setBorder(new TitledBorder("Circuit Canvas"));
        add(drawingPanel, BorderLayout.CENTER);

        // Bottom (status)
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT));
        status.add(new JLabel("Power:"));
        powerLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        powerLabel.setForeground(Color.RED);
        status.add(powerLabel);
        add(status, BorderLayout.SOUTH);

        // Sample theme
        drawingPanel.setupSampleCascoTheme();
    }

    private JPanel buildControlPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel addPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        addPanel.setBorder(new TitledBorder("Add Component"));
        JButton addLed = new JButton("Add LED");
        JButton addRes = new JButton("Add Resistor");
        JButton addWire = new JButton("Add Wire");

        addPanel.add(addLed);
        addPanel.add(addRes);
        addPanel.add(addWire);

        addLed.addActionListener(e ->
                drawingPanel.addComponent(new LedComponent(
                        100 + (int) (Math.random() * 400),
                        100 + (int) (Math.random() * 300))));

        addRes.addActionListener(e ->
                drawingPanel.addComponent(new ResistorComponent(
                        100 + (int) (Math.random() * 400),
                        100 + (int) (Math.random() * 300))));

        addWire.addActionListener(e ->
                drawingPanel.addComponent(new WireComponent(
                        150 + (int) (Math.random() * 400),
                        150 + (int) (Math.random() * 300))));

        JPanel powerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        powerPanel.setBorder(new TitledBorder("Power"));
        JButton togglePower = new JButton("Toggle Power");
        togglePower.addActionListener(e -> togglePower());
        powerPanel.add(togglePower);

        JPanel dbPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        dbPanel.setBorder(new TitledBorder("Database"));
        JButton saveDb = new JButton("Save Circuit (SP)");
        JButton testDb = new JButton("Test DB Connection");
        saveDb.addActionListener(e -> saveCircuitToDatabase());
        testDb.addActionListener(e -> testDbConnection());
        dbPanel.add(saveDb);
        dbPanel.add(testDb);

        p.add(addPanel);
        p.add(Box.createVerticalStrut(8));
        p.add(powerPanel);
        p.add(Box.createVerticalStrut(8));
        p.add(dbPanel);

        return p;
    }

    private JScrollPane buildLogPanel() {
        JTable table = new JTable(logTableModel);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new TitledBorder("Event Log"));
        sp.setPreferredSize(new Dimension(280, 400));
        return sp;
    }

    private void togglePower() {
        powerOn = !powerOn;
        powerLabel.setText(powerOn ? "ON" : "OFF");
        powerLabel.setForeground(powerOn ? new Color(0, 128, 0) : Color.RED);
        drawingPanel.setPower(powerOn);
        appendLog("Power", "Power turned " + (powerOn ? "ON" : "OFF"));
    }

    private void appendLog(String event, String details) {
        String ts = LocalDateTime.now().format(dtf);
        logTableModel.addRow(new Object[]{ts, event, details});
    }

    // ---------------- DATABASE OPERATIONS ----------------

    private void testDbConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            JOptionPane.showMessageDialog(this, "Connection OK: " +
                    conn.getMetaData().getDatabaseProductName());
            appendLog("DB", "Connection successful");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB connection failed: " + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveCircuitToDatabase() {

        String circuitName = "Casco Colombia 2026 - F32 Piloto de Combate";
        String theme = "Casco Colombia 2026";
        int components = drawingPanel.getCircuitComponentCount();
        String powerState = powerOn ? "ON" : "OFF";
        Timestamp ts = Timestamp.valueOf(LocalDateTime.now());

        String call =
                "{call " + SP_INSERT_LOG + "(?,?,?,?,?)}";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             CallableStatement cs = conn.prepareCall(call)) {

            cs.setString(1, circuitName);
            cs.setString(2, theme);
            cs.setInt(3, components);
            cs.setString(4, powerState);
            cs.setTimestamp(5, ts);

            cs.execute();

            appendLog("DB", "Saved via SP: components=" +
                    components + ", power=" + powerState);

            JOptionPane.showMessageDialog(this,
                    "Circuit saved to DB via stored procedure.");

        } catch (SQLException ex) {
            appendLog("DB", "Save failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error saving to DB: " + ex.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- DRAWING ENGINE ----------------

    private static abstract class CircuitComponent {
        int x, y;
        boolean powered = false;
        CircuitComponent(int x, int y) { this.x = x; this.y = y; }
        abstract void draw(Graphics2D g, boolean power);
        abstract String getType();
    }

    private static class LedComponent extends CircuitComponent {
        LedComponent(int x, int y) { super(x, y); }
        @Override
        void draw(Graphics2D g, boolean power) {
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x - 8, y - 8, 16, 16);
            if (power) {
                g.setColor(Color.YELLOW);
                g.fillOval(x - 7, y - 7, 14, 14);
                g.setColor(Color.WHITE);
            }
            g.drawString("LED", x + 12, y + 5);
        }
        @Override
        String getType() { return "LED"; }
    }

    private static class ResistorComponent extends CircuitComponent {
        ResistorComponent(int x, int y) { super(x, y); }
        @Override
        void draw(Graphics2D g, boolean power) {
            int w = 40, h = 10;
            g.setStroke(new BasicStroke(2f));
            g.drawRect(x - w/2, y - h/2, w, h);
            g.drawString("R", x - 4, y + 4);
            g.drawString("Resistor", x + w/2 + 8, y + 4);
        }
        @Override
        String getType() { return "Resistor"; }
    }

    private static class WireComponent extends CircuitComponent {
        private final int x2, y2;
        WireComponent(int x, int y) {
            super(x, y);
            this.x2 = x + 60;
            this.y2 = y + 15;
        }
        @Override
        void draw(Graphics2D g, boolean power) {
            Stroke old = g.getStroke();
            g.setStroke(new BasicStroke(3f));
            g.draw(new Line2D.Float(x, y, x2, y2));
            g.setStroke(old);
            g.drawString("Wire",
                    (x + x2) / 2 + 6,
                    (y + y2) / 2 + 4);
        }
        @Override
        String getType() { return "Wire"; }
    }

    private class DrawingPanel extends JPanel {
        private final List<CircuitComponent> components = new ArrayList<>();
        private boolean power = false;

        DrawingPanel() {
            setBackground(new Color(20, 20, 30));
            setPreferredSize(new Dimension(800, 600));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    for (CircuitComponent c : components) {
                        if (Math.hypot(e.getX() - c.x, e.getY() - c.y) < 20) {
                            appendLog("Canvas", "Clicked component: " + c.getType());
                            return;
                        }
                    }
                }
            });
        }

        public void addComponent(CircuitComponent c) {
            components.add(c);
            repaint();
        }

        // MÉTODO CORREGIDO PARA EL CONTEO REAL DE COMPONENTES
        public int getCircuitComponentCount() {
            return components.size();
        }

        public void setPower(boolean power) {
            this.power = power;
            for (CircuitComponent c : components) {
                c.powered = power;
            }
            repaint();
        }

        public void setupSampleCascoTheme() {
            components.clear();
            components.add(new LedComponent(240, 180));
            components.add(new ResistorComponent(340, 220));
            components.add(new WireComponent(420, 260));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(32, 32, 42));
            for (int x = 0; x < getWidth(); x += 30)
                g2.drawLine(x, 0, x, getHeight());
            for (int y = 0; y < getHeight(); y += 30)
                g2.drawLine(0, y, getWidth(), y);

            int bx = 60, by = 60, bw = 480, bh = 180;
            g2.setColor(new Color(10, 70, 110));
            g2.fillRoundRect(bx, by, bw, bh, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawString("ARDUINO SIM (illustrative)", bx + 12, by + 20);

            drawHelmetTheme(g2, bx + bw - 140, by + 10);

            for (CircuitComponent c : components) {
                g2.setColor(c.powered ? new Color(255, 230, 150) : Color.LIGHT_GRAY);
                c.draw(g2, power);
            }

            g2.setColor(Color.WHITE);
            g2.drawString("Theme: Casco Colombia 2026 - F32 Piloto de Combate",
                    70, by + bh + 30);
            g2.dispose();
        }

        private void drawHelmetTheme(Graphics2D g2, int x, int y) {
            int w = 120, h = 80;
            g2.setColor(new Color(40, 40, 40));
            g2.fillRoundRect(x, y + 10, w, h, 30, 30);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(x, y + 10, w, h, 30, 30);

            g2.setColor(new Color(10, 10, 40));
            g2.fillOval(x + 10, y + 30, 40, 30);

            g2.setColor(new Color(255, 215, 0));
            g2.fillRect(x + 60, y + 20, 40, 10);
            g2.setColor(new Color(0, 50, 150));
            g2.fillRect(x + 60, y + 30, 40, 10);
            g2.setColor(new Color(206, 17, 38));
            g2.fillRect(x + 60, y + 40, 40, 10);

            g2.setColor(Color.WHITE);
            g2.drawString("F32", x + 14, y + h - 6);
            g2.drawString("Piloto de Combate", x + 10, y + h + 8);
        }
    }

    // ---------------- Oracle SQL ----------------
    /*
    CREATE TABLE CIRCUIT_LOG (
      ID NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      CIRCUIT_NAME VARCHAR2(200),
      THEME VARCHAR2(100),
      COMPONENT_COUNT NUMBER,
      POWER_STATE VARCHAR2(10),
      LOG_TS TIMESTAMP
    );

    CREATE OR REPLACE PROCEDURE INSERT_CIRCUIT_LOG (
      p_circuit_name IN VARCHAR2,
      p_theme IN VARCHAR2,
      p_components IN NUMBER,
      p_power_state IN VARCHAR2,
      p_ts IN TIMESTAMP
    ) AS
    BEGIN
      INSERT INTO CIRCUIT_LOG (CIRCUIT_NAME, THEME, COMPONENT_COUNT, POWER_STATE, LOG_TS)
      VALUES (p_circuit_name, p_theme, p_components, p_power_state, p_ts);
      COMMIT;
    END INSERT_CIRCUIT_LOG;
    /
    */
}
