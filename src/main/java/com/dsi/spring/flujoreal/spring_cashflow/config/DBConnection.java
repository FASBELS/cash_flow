package com.dsi.spring.flujoreal.spring_cashflow.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Instancia única (Singleton)
    private static volatile DBConnection instance;

    // Conexión mantenida por la clase (se reabre si cae)
    private volatile Connection connection;

    // Datos de conexión a Oracle (ajusta según tu entorno)
    private final String URL = "jdbc:oracle:thin:@//localhost:1522/XEPDB1";
    // private final String URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1";   //para mi oracle (Alex)
    private final String USER = "HR";
    private final String PASSWORD = "hr";

    // Tiempo (seg) para isValid()
    private static final int VALID_TIMEOUT = 5;

    // Constructor privado
    private DBConnection() {
        openNewConnection();
    }

    /** Crea una nueva conexión (reutilizado en constructor y reintentos). */
    private synchronized void openNewConnection() {
        try {
            // Driver (opcional en JDBC 4+, pero lo dejamos)
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // Nueva conexión
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Conexión Oracle abierta (Singleton).");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver Oracle no encontrado: " + e.getMessage());
            this.connection = null;
        } catch (SQLException e) {
            System.err.println("❌ Error conectando a Oracle: " + e.getMessage());
            this.connection = null;
        }
    }

    /** Obtiene la instancia única. */
    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Retorna una conexión válida. Si la existente está cerrada o inválida,
     * se reabre automáticamente.
     */
    public synchronized Connection getConnection() throws SQLException {
        try {
            if (this.connection == null
                    || this.connection.isClosed()
                    || !this.connection.isValid(VALID_TIMEOUT)) {
                System.out.println("ℹ️ Conexión inválida/cerrada. Reabriendo...");
                openNewConnection();
            }
        } catch (SQLException e) {
            // Si falla la validación, intentamos reabrir
            System.out.println("ℹ️ Validación falló (" + e.getMessage() + "). Reabriendo...");
            openNewConnection();
        }

        if (this.connection == null) {
            throw new SQLException("No se pudo establecer conexión con Oracle.");
        }
        return this.connection;
    }
}
