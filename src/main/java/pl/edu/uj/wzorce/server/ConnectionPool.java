package pl.edu.uj.wzorce.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class ConnectionPool {

    private Vector<Connection> freeConnections = new Vector<>();
    private Map<Connection, LocalDateTime> occupiedConnections = new HashMap<>();
    private int connectionLiveTime = 3; // czas ważności połączenia w sekundach
    private static ConnectionPool instance = null;

    public synchronized static ConnectionPool getInstance() {
        if (instance == null) {
            instance = new ConnectionPool();
            class CustomTask extends TimerTask {
                public CustomTask() {
                }

                public void run() {
                    try {
                        Set<Connection> set = instance.occupiedConnections.keySet();
                        int connectionLiveTime = instance.connectionLiveTime;
                        for (Connection connection : set) {
                            if (instance.occupiedConnections.get(connection).until(LocalDateTime.now(), ChronoUnit.SECONDS) > connectionLiveTime) {
                                instance.releaseConnection(connection);
                            }
                        }
                    } catch (Exception ex) {
                    }
                }
            }//koniec klasy CustomTask

            Timer timer = new Timer();
            timer.schedule(new CustomTask(), 1000 * 5, 1000 * 30); //uruchomienie za 5s, a potem co 30s
        }
        return instance;
    }

    public synchronized Connection getConnection() {
        while (freeConnections.isEmpty())
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        int size = freeConnections.size();
        Connection conn = freeConnections.get(size - 1);
        freeConnections.remove(size - 1);
        occupiedConnections.put(conn, LocalDateTime.now());
        return conn;

    }

    public synchronized void releaseConnection(Connection connection) {
        if (occupiedConnections.containsKey(connection)) {
            occupiedConnections.remove(connection);
            freeConnections.add(connection);

            notify();
        }
    }

    public int getFreeConnectionNumber() {
        return freeConnections.size();
    }

    private ConnectionPool() {

        try {
            Class.forName("com.mysql.jdbc.Driver");
            for (int temp = 0; temp < 20; temp++)
                freeConnections.add(DriverManager.getConnection("jdbc:mysql://localhost:3306/rpg?" +
                        "useUnicode=true" +
                        "&useJDBCCompliantTimezoneShift=true" +
                        "&useLegacyDatetimeCode=false" +
                        "&serverTimezone=UTC", "root", ""));
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
