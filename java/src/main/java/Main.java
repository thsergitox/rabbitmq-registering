import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LP1 - Persistencia Java
 * Persiste datos en BD1 (PostgreSQL) y responde vía RabbitMQ
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        logger.info("🏁 Iniciando LP1 - Persistencia Java");

        // Crear instancia del servicio de persistencia
        LP1Persistence persistence = new LP1Persistence();

        // Configurar shutdown hook para cierre limpio
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("🛑 Cerrando LP1 Persistence...");
            persistence.stop();
        }));

        try {
            // Iniciar el servicio
            persistence.start();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error fatal en LP1: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}

/**
 * Servicio principal de persistencia LP1
 */
class LP1Persistence {
    private static final Logger logger = Logger.getLogger(LP1Persistence.class.getName());

    private DatabaseManager dbManager;
    private RabbitMQClient rabbitmqClient;
    private ExecutorService executorService;
    private volatile boolean running;

    public LP1Persistence() {
        this.dbManager = new DatabaseManager();
        this.rabbitmqClient = new RabbitMQClient();
        this.executorService = Executors.newFixedThreadPool(10); // Pool de hilos para concurrencia
        this.running = false;
    }

    public void start() {
        logger.info("🚀 Iniciando LP1 Persistence...");

        try {
            // Conectar a la base de datos
            if (!dbManager.connect()) {
                logger.severe("❌ No se pudo conectar a PostgreSQL");
                return;
            }

            // Conectar a RabbitMQ
            if (!rabbitmqClient.connect()) {
                logger.severe("❌ No se pudo conectar a RabbitMQ");
                return;
            }

            // Configurar el consumidor
            rabbitmqClient.setupConsumer("queue_lp1", this::processPersistenceRequest);

            this.running = true;
            logger.info("✅ LP1 Persistence iniciado correctamente");
            logger.info("📋 Esperando solicitudes de persistencia...");

            // Iniciar el consumo de mensajes
            rabbitmqClient.startConsuming();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error iniciando LP1: " + e.getMessage(), e);
            stop();
        }
    }

    public void stop() {
        logger.info("🛑 Deteniendo LP1 Persistence...");

        this.running = false;

        if (rabbitmqClient != null) {
            rabbitmqClient.disconnect();
        }

        if (dbManager != null) {
            dbManager.disconnect();
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        logger.info("✅ LP1 Persistence detenido");
    }

    /**
     * Procesa una solicitud de persistencia
     */
    private void processPersistenceRequest(String message) {
        // Procesar en un hilo separado para mejorar concurrencia
        executorService.submit(() -> {
            try {
                logger.info("📨 Procesando solicitud de persistencia...");

                // Parsear el mensaje JSON
                UserModel user = UserModel.fromJson(message);

                if (user == null) {
                    logger.warning("❌ Error parseando datos de usuario");
                    return;
                }

                logger.info("💾 Persistiendo usuario: " + user.getNombre() + " (DNI: " + user.getDni() + ")");

                // Persistir en la base de datos
                boolean success = dbManager.persistUser(user);

                if (success) {
                    logger.info("✅ Usuario persistido exitosamente: " + user.getDni());

                    // Enviar confirmación de persistencia
                    sendPersistenceConfirmation(user, true, "Usuario persistido correctamente");
                } else {
                    logger.warning("❌ Error persistiendo usuario: " + user.getDni());

                    // Enviar confirmación de error
                    sendPersistenceConfirmation(user, false, "Error en la persistencia");
                }

            } catch (Exception e) {
                logger.log(Level.SEVERE, "❌ Error procesando persistencia: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Envía confirmación de persistencia vía RabbitMQ
     */
    private void sendPersistenceConfirmation(UserModel user, boolean success, String message) {
        try {
            // Crear respuesta JSON
            String response = String.format(
                    "{\"request_id\":\"%s\",\"dni\":\"%s\",\"persisted\":%s,\"message\":\"%s\",\"timestamp\":%d}",
                    user.getRequestId(),
                    user.getDni(),
                    success,
                    message,
                    System.currentTimeMillis());

            // Enviar mensaje de confirmación
            boolean sent = rabbitmqClient.publishMessage(
                    "registro_bus",
                    "lp1.persisted",
                    response);

            if (sent) {
                String status = success ? "✅ PERSISTIDO" : "❌ ERROR";
                logger.info(status + " - Confirmación enviada para DNI: " + user.getDni());
            } else {
                logger.warning("❌ Error enviando confirmación para DNI: " + user.getDni());
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error enviando confirmación: " + e.getMessage(), e);
        }
    }
}