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

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class CircuitSimulatorHelmet {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        System.out.printf("Hello and welcome!");

        for (int i = 1; i <= 5; i++) {
            //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
            // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
            System.out.println("i = " + i);
        }
    }
}