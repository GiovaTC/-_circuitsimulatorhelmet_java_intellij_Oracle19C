# -_circuitsimulatorhelmet_java_intellij_Oracle19C .  

<img width="1024" height="1024" alt="image" src="https://github.com/user-attachments/assets/8aef28e5-792e-4dbf-b5ce-e72d45f34bf6" />  

# CircuitSimulatorHelmet.java  
Aplicación Java Swing (Single-file) para IntelliJ  
Tema: **Casco Colombia 2026 – F32 Piloto de Combate**

---

## Características

- Dibujo simple de circuito (Arduino-style) + componentes.  
- Interfaz gráfica para agregar LED, Resistor y Wire.  
- Alternancia de Power ON/OFF con cambios visuales.  
- Registro del circuito en Oracle 19c mediante Stored Procedure.  
- Incluye SQL para crear tabla y SP.

---

## Instrucciones rápidas

1. Añadir `ojdbc11.jar` u `ojdbc8.jar` al classpath de IntelliJ.  
2. Ajustar valores en DB CONFIG (`DB_URL`, `DB_USER`, `DB_PASSWORD`).  
   - Ejemplo URL:  
     `jdbc:oracle:thin:@localhost:1521/orcl`  
     `jdbc:oracle:thin:@//localhost:1521/orcl`
3. Crear tabla y Stored Procedure incluidos al final.  
4. Ejecutar como una aplicación Java normal.

---

## Código completo

```java
/*
CircuitSimulatorHelmet.java
Single-file Java Swing application for IntelliJ

Features:
- Simple circuit-like drawing (Arduino-like board + components) themed "Casco Colombia 2026 - F32 Piloto de Combate"
- Interactive GUI to add basic components (LED, Resistor, Wire) onto the canvas
- Power ON/OFF toggle that visually updates components
- Saves circuit information to Oracle 19c via a stored procedure call
- Includes example SQL (table + stored procedure) to create on Oracle side

INSTRUCCIONES RÁPIDAS:
1) Añada el driver de Oracle (ojdbc11.jar u ojdbc8.jar) al classpath / module dependencies en IntelliJ.
2) Ajuste las constantes DB_URL, DB_USER y DB_PASSWORD en la sección DB CONFIG.
   - Ejemplo de URL (service name): "jdbc:oracle:thin:@localhost:1521/orcl"
   - Ejemplo de URL (using double slash and service name): "jdbc:oracle:thin:@//localhost:1521/orcl"
   Use la que corresponda a su instalación.
3) Cree la tabla y el stored procedure en Oracle usando el SQL provisto abajo antes de ejecutar la aplicación.
4) Ejecutar desde IntelliJ como una aplicación Java normal.

NOTAS SOBRE EL STORED PROCEDURE:
- La app invoca un stored procedure llamado INSERT_CIRCUIT_LOG con parámetros
  (p_circuit_name IN VARCHAR2, p_theme IN VARCHAR2, p_components IN NUMBER, p_power_state IN VARCHAR2, p_ts IN TIMESTAMP)
  Asegúrese de crearla con el SQL incluido.

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
    // Ajuste estas constantes según su entorno Oracle 19c
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521/orcl"; // service name format
    private static final String DB_USER = "YOUR_DB_USER";
    private static final String DB_PASSWORD = "YOUR_DB_PASSWORD";
    // Stored procedure name expected in Oracle
    private static final String SP_INSERT_LOG = "INSERT_CIRCUIT_LOG";

    // ---------------- UI / STATE ----------------
    private final DrawingPanel drawingPanel = new DrawingPanel();
    private final DefaultTableModel logTableModel = new DefaultTableModel(new Object[]{"Timestamp", "Event", "Details"}, 0);
    private boolean powerOn = false;
    private final JLabel powerLabel = new JLabel("OFF");
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Opcional: Look & Feel del sistema
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

        // Left: controls
        JPanel left = new JPanel();
        left.setPreferredSize(new Dimension(300, 0));
        left.setLayout(new BorderLayout(8,8));

        left.add(buildControlPanel(), BorderLayout.NORTH);
        left.add(buildLogPanel(), BorderLayout.CENTER);

        add(left, BorderLayout.WEST);

        // Center: drawing
        drawingPanel.setBorder(new TitledBorder("Circuit Canvas"));
        add(drawingPanel, BorderLayout.CENTER);

        // Bottom: status bar
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT));
        status.add(new JLabel("Power:"));
        powerLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        powerLabel.setForeground(Color.RED);
        status.add(powerLabel);
        add(status, BorderLayout.SOUTH);

        // Initialize with a themed sample layout
        drawingPanel.setupSampleCascoTheme();
    }

    private JPanel buildControlPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel addPanel = new JPanel(new GridLayout(0,1,4,4));
        addPanel.setBorder(new TitledBorder("Add Component"));
        JButton addLed = new JButton("Add LED");
        JButton addRes = new JButton("Add Resistor");
        JButton addWire = new JButton("Add Wire");
        addPanel.add(addLed);
        addPanel.add(addRes);
        addPanel.add(addWire);

        addLed.addActionListener(e -> {
            drawingPanel.addComponent(new LedComponent(100 + (int)(Math.random()*400), 100 + (int)(Math.random()*300)));
            appendLog("UI", "LED added");
        });
        addRes.addActionListener(e -> {
            drawingPanel.addComponent(new ResistorComponent(100 + (int)(Math.random()*400), 100 + (int)(Math.random()*300)));
            appendLog("UI", "Resistor added");
        });
        addWire.addActionListener(e -> {
            drawingPanel.addComponent(new WireComponent(150 + (int)(Math.random()*400), 150 + (int)(Math.random()*300)));
            appendLog("UI", "Wire added");
        });

        JPanel powerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        powerPanel.setBorder(new TitledBorder("Power"));
        JButton togglePower = new JButton("Toggle Power");
        powerPanel.add(togglePower);
        togglePower.addActionListener(e -> togglePower());

        JPanel dbPanel = new JPanel(new GridLayout(0,1,4,4));
        dbPanel.setBorder(new TitledBorder("Database"));
        JButton saveDb = new JButton("Save Circuit (SP)");
        JButton testDb = new JButton("Test DB Connection");
        dbPanel.add(saveDb);
        dbPanel.add(testDb);

        saveDb.addActionListener(e -> saveCircuitToDatabase());
        testDb.addActionListener(e -> testDbConnection());

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

    private void appendLog(String event, String details) {
        String ts = LocalDateTime.now().format(dtf);
        logTableModel.addRow(new Object[]{ts, event, details});
    }

    private void togglePower() {
        powerOn = !powerOn;
        powerLabel.setText(powerOn ? "ON" : "OFF");
        powerLabel.setForeground(powerOn ? new Color(0,128,0) : Color.RED);
        drawingPanel.setPower(powerOn);
        appendLog("Power", "Power turned " + (powerOn ? "ON" : "OFF"));
    }

    private void testDbConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            JOptionPane.showMessageDialog(this, "Connection OK: " + conn.getMetaData().getDatabaseProductName());
            appendLog("DB", "Connection successful");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB connection failed: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            appendLog("DB", "Connection failed: " + ex.getMessage());
        }
    }

    private void saveCircuitToDatabase() {
        // Prepare values
        String circuitName = "Casco Colombia 2026 - F32 Piloto de Combate";
        String theme = "Casco Colombia 2026";
        int components = drawingPanel.getComponentCount();
        String powerState = powerOn ? "ON" : "OFF";
        Timestamp ts = Timestamp.valueOf(LocalDateTime.now());

        String call = "{call " + SP_INSERT_LOG + "(?,?,?,?,?)}"; // (p_circuit_name, p_theme, p_components, p_power_state, p_ts)

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             CallableStatement cs = conn.prepareCall(call)) {

            cs.setString(1, circuitName);
            cs.setString(2, theme);
            cs.setInt(3, components);
            cs.setString(4, powerState);
            cs.setTimestamp(5, ts);

            cs.execute();

            appendLog("DB", "Saved via SP: components=" + components + ", power=" + powerState);
            JOptionPane.showMessageDialog(this, "Circuit saved to DB via stored procedure.");
        } catch (SQLException ex) {
            appendLog("DB", "Save failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving to DB: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- Drawing and Components ----------------
    private static abstract class CircuitComponent {
        int x, y;
        boolean powered = false;
        CircuitComponent(int x, int y) { this.x = x; this.y = y; }
        abstract void draw(Graphics2D g, boolean power);
        abstract String getType();
    }

    private static class LedComponent extends CircuitComponent {
        LedComponent(int x, int y) { super(x,y); }
        @Override
        void draw(Graphics2D g, boolean power) {
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x-8, y-8, 16, 16);
            if (power) {
                g.setColor(Color.YELLOW);
                g.fillOval(x-7, y-7, 14, 14);
                g.setColor(Color.BLACK);
            }
            g.drawString("LED", x+12, y+5);
        }
        @Override
        String getType() { return "LED"; }
    }

    private static class ResistorComponent extends CircuitComponent {
        ResistorComponent(int x, int y) { super(x,y); }
        @Override
        void draw(Graphics2D g, boolean power) {
            int w = 40, h = 10;
            g.setStroke(new BasicStroke(2f));
            g.drawRect(x - w/2, y - h/2, w, h);
            g.drawString("R", x - 4, y + 4);
            g.drawString("Resistor", x + w/2 + 8, y + 4);
            if (power) {
                g.drawLine(x - w/2 - 20, y, x - w/2, y);
                g.drawLine(x + w/2, y, x + w/2 + 20, y);
            }
        }
        @Override
        String getType() { return "Resistor"; }
    }

    private static class WireComponent extends CircuitComponent {
        private final int x2, y2;
        WireComponent(int x, int y) { super(x,y); this.x2 = x+60; this.y2 = y+15; }
        @Override
        void draw(Graphics2D g, boolean power) {
            Stroke old = g.getStroke();
            g.setStroke(new BasicStroke(3f));
            g.draw(new Line2D.Float(x, y, x2, y2));
            g.setStroke(old);
            g.drawString("Wire", (x+x2)/2 + 6, (y+y2)/2 + 4);
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
                    // click shows component info if any
                    for (CircuitComponent c : components) {
                        if (Math.hypot(e.getX()-c.x, e.getY()-c.y) < 20) {
                            appendLog("Canvas", "Clicked component: " + c.getType());
                            repaint();
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

        public int getComponentCount() { return components.size(); }

        public void setPower(boolean power) {
            this.power = power;
            for (CircuitComponent c : components) c.powered = power;
            repaint();
        }

        public void setupSampleCascoTheme() {
            components.clear();
            // Draw an Arduino-like board using shapes (purely illustrative)
            // Add a helmet graphic as part of the theme
            components.add(new LedComponent(240, 180));
            components.add(new ResistorComponent(340, 220));
            components.add(new WireComponent(420, 260));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background grid
            g2.setColor(new Color(32, 32, 42));
            for (int x=0;x<getWidth();x+=30) g2.drawLine(x,0,x,getHeight());
            for (int y=0;y<getHeight();y+=30) g2.drawLine(0,y,getWidth(),y);

            // Draw a stylized "Arduino" board
            int bx = 60, by = 60, bw = 480, bh = 180;
            g2.setColor(new Color(10, 70, 110));
            g2.fillRoundRect(bx, by, bw, bh, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawString("ARDUINO SIM (illustrative)", bx+12, by+20);

            // Draw the helmet theme on the board (Casco Colombia)
            drawHelmetTheme(g2, bx + bw - 140, by + 10);

            // Draw components
            for (CircuitComponent c : components) {
                g2.setColor(c.powered ? new Color(255, 230, 150) : Color.LIGHT_GRAY);
                c.draw(g2, power);
                g2.setColor(Color.WHITE);
            }

            // Draw a legend
            g2.setColor(Color.WHITE);
            g2.drawString("Theme: Casco Colombia 2026 - F32 Piloto de Combate", 70, by + bh + 30);

            g2.dispose();
        }

        private void drawHelmetTheme(Graphics2D g2, int x, int y) {
            // Simple helmet silhouette and Colombian flag strip
            int w = 120, h = 80;
            // helmet shell
            g2.setColor(new Color(40,40,40));
            g2.fillRoundRect(x, y+10, w, h, 30, 30);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(x, y+10, w, h, 30, 30);
            // visor
            g2.setColor(new Color(10,10,40));
            g2.fillOval(x+10, y+30, 40, 30);
            // Colombian flag stripe on helmet
            g2.setColor(new Color(255, 215, 0)); // yellow
            g2.fillRect(x+60, y+20, 40, 10);
            g2.setColor(new Color(0, 122, 61)); // blue? (using greenish for stylized)
            g2.fillRect(x+60, y+30, 40, 10);
            g2.setColor(new Color(206, 17, 38)); // red
            g2.fillRect(x+60, y+40, 40, 10);

            g2.setColor(Color.WHITE);
            g2.drawString("F32", x + 14, y + h - 6);
            g2.drawString("Piloto de Combate", x + 10, y + h + 8);
        }
    }

    // ---------------- SQL: Table + Stored Procedure (Oracle) ----------------
    /*
    -- Ejemplo de DDL para Oracle 19c
    CREATE TABLE CIRCUIT_LOG (
      ID NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      CIRCUIT_NAME VARCHAR2(200),
      THEME VARCHAR2(100),
      COMPONENT_COUNT NUMBER,
      POWER_STATE VARCHAR2(10),
      LOG_TS TIMESTAMP
    );

    -- Stored procedure INSERT_CIRCUIT_LOG
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
```

---

## SQL: Tabla + Stored Procedure Oracle 19c

```sql
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
```
:. .. .
