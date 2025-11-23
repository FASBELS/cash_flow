package com.dsi.spring.flujoreal.spring_cashflow.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static volatile DBConnection instance;
    private volatile Connection connection;

    // Oracle de Fabio:
    /*private final String URL = "jdbc:oracle:thin:@//localhost:1522/XEPDB1";
    private final String USER = "HR";
    private final String PASSWORD = "hr";*/

    // Oracle de Alex:
    /*private final String URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1";
    private final String USER = "PY DEV02"; 
    private final String PASSWORD = "123";*/

    // Oracle de Anthony:
    private final String URL = "jdbc:oracle:thin:@//localhost:1521/xe";
    private final String USER = "PY DEV02";
    private final String PASSWORD = "123";

    private static final int VALID_TIMEOUT = 5;

    // Constructor privado
    private DBConnection() {
        openNewConnection();
    }

    private synchronized void openNewConnection() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");

            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexión Oracle abierta (Singleton).");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver Oracle no encontrado: " + e.getMessage());
            this.connection = null;
        } catch (SQLException e) {
            System.err.println("Error conectando a Oracle: " + e.getMessage());
            this.connection = null;
        }
    }

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

    public synchronized Connection getConnection() throws SQLException {
        try {
            if (this.connection == null
                    || this.connection.isClosed()
                    || !this.connection.isValid(VALID_TIMEOUT)) {
                System.out.println("Conexión inválida/cerrada. Reabriendo...");
                openNewConnection();
            }
        } catch (SQLException e) {
            // Si falla la validación, intentamos reabrir
            System.out.println("Validación falló (" + e.getMessage() + "). Reabriendo...");
            openNewConnection();
        }

        if (this.connection == null) {
            throw new SQLException("No se pudo establecer conexión con Oracle.");
        }
        return this.connection;
    }
}
