import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Gestor de Base de Datos para LP1 - PostgreSQL BD1
 */
public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private HikariDataSource dataSource;
    private String host;
    private String database;
    private String user;

    public DatabaseManager() {
        // Configuración desde variables de entorno
        this.host = System.getenv().getOrDefault("DB_HOST", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "5432"));
        this.database = System.getenv().getOrDefault("DB_NAME", "bd1_users");
        this.user = System.getenv().getOrDefault("DB_USER", "lp1_user");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "lp1_pass");

        // Configurar HikariCP (Pool de conexiones)
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(user);
        config.setPassword(password);

        // Configuraciones del pool
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Configuraciones adicionales
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
    }

    public boolean connect() {
        try {
            // Probar conexión
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                logger.info("✅ Conectado a PostgreSQL " + meta.getDatabaseProductVersion());

                // Verificar tablas
                if (verifyTables(conn)) {
                    logger.info("✅ Tablas verificadas correctamente");
                    return true;
                } else {
                    logger.severe("❌ Error verificando tablas");
                    return false;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Error conectando a PostgreSQL: " + e.getMessage(), e);
            return false;
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("✅ Conexión a PostgreSQL cerrada");
        }
    }

    private boolean verifyTables(Connection conn) {
        try {
            // Verificar tabla users
            DatabaseMetaData meta = conn.getMetaData();

            try (ResultSet rs = meta.getTables(null, null, "users", new String[] { "TABLE" })) {
                if (!rs.next()) {
                    logger.severe("❌ Tabla 'users' no encontrada");
                    return false;
                }
            }

            // Verificar tabla friend
            try (ResultSet rs = meta.getTables(null, null, "friend", new String[] { "TABLE" })) {
                if (!rs.next()) {
                    logger.warning("⚠️ Tabla 'friend' no encontrada, creando...");
                    createFriendTable(conn);
                }
            }

            // Contar registros en users
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users");
                    ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    int count = rs.getInt(1);
                    logger.info("📊 BD1 tiene " + count + " usuarios registrados");
                }
            }

            return true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Error verificando tablas: " + e.getMessage(), e);
            return false;
        }
    }

    private void createFriendTable(Connection conn) throws SQLException {
        String sql = """
                    CREATE TABLE IF NOT EXISTS friend (
                        user_id INT NOT NULL,
                        friend_id INT NOT NULL,
                        PRIMARY KEY (user_id, friend_id),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
            logger.info("✅ Tabla 'friend' creada exitosamente");
        }
    }

    public boolean persistUser(UserModel user) {
        String sqlUser = "INSERT INTO users (nombre, correo, clave, dni, telefono) VALUES (?, ?, ?, ?, ?) RETURNING id";
        String sqlFriend = "INSERT INTO friend (user_id, friend_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // Iniciar transacción

            int userId;
            // 1. Insertar el usuario y obtener el ID generado
            try (PreparedStatement stmtUser = conn.prepareStatement(sqlUser)) {
                stmtUser.setString(1, user.getNombre());
                stmtUser.setString(2, user.getCorreo());
                stmtUser.setString(3, user.getClave());
                stmtUser.setString(4, user.getDni());
                stmtUser.setString(5, user.getTelefono());

                ResultSet rs = stmtUser.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt(1);
                    user.setId(userId);
                } else {
                    throw new SQLException("Error al crear usuario, no se pudo obtener el ID.");
                }
            }

            // 2. Insertar las relaciones de amistad
            if (user.getAmigosFrecuentes() != null && !user.getAmigosFrecuentes().isEmpty()) {
                try (PreparedStatement stmtFriend = conn.prepareStatement(sqlFriend)) {
                    for (Integer friendId : user.getAmigosFrecuentes()) {
                        stmtFriend.setInt(1, userId);
                        stmtFriend.setInt(2, friendId);
                        stmtFriend.addBatch();

                        // Opcional: Relación bidireccional
                        stmtFriend.setInt(1, friendId);
                        stmtFriend.setInt(2, userId);
                        stmtFriend.addBatch();
                    }
                    stmtFriend.executeBatch();
                }
            }

            conn.commit(); // Confirmar transacción
            logger.info("✅ Usuario persistido exitosamente con ID: " + userId);
            return true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Error persistiendo usuario: " + e.getMessage(), e);
            // El rollback es manejado por el try-with-resources y auto-commit=false
            return false;
        }
    }

    public boolean userExists(String dni) {
        String sql = "SELECT 1 FROM users WHERE dni = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, dni);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            logger.log(Level.WARNING, "❌ Error verificando existencia de usuario: " + e.getMessage(), e);
            return false;
        }
    }

    public List<UserModel> getAllUsers() {
        List<UserModel> users = new ArrayList<>();
        String sql = "SELECT id, nombre, correo, clave, dni, telefono FROM users ORDER BY id";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UserModel user = new UserModel();
                user.setId(rs.getInt("id"));
                user.setNombre(rs.getString("nombre"));
                user.setCorreo(rs.getString("correo"));
                user.setClave(rs.getString("clave"));
                user.setDni(rs.getString("dni"));
                user.setTelefono(rs.getString("telefono"));

                users.add(user);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Error obteniendo usuarios: " + e.getMessage(), e);
        }

        return users;
    }

    public boolean isHealthy() {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {

            stmt.executeQuery();
            return true;

        } catch (SQLException e) {
            logger.log(Level.WARNING, "❌ Health check falló: " + e.getMessage(), e);
            return false;
        }
    }
}