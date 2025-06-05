import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;

/**
 * Cliente RabbitMQ para LP1 - Persistencia Java
 */
public class RabbitMQClient {
    private static final Logger logger = Logger.getLogger(RabbitMQClient.class.getName());

    private Connection connection;
    private Channel channel;
    private String host;
    private int port;
    private String username;
    private String password;

    public RabbitMQClient() {
        // Configuración desde variables de entorno
        this.host = System.getenv().getOrDefault("RABBITMQ_HOST", "rabbitmq");
        this.port = Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672"));
        this.username = System.getenv().getOrDefault("RABBITMQ_USER", "admin");
        this.password = System.getenv().getOrDefault("RABBITMQ_PASSWORD", "admin123");
    }

    public boolean connect() {
        try {
            // Configurar factory de conexión
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost("/");

            // Configuraciones adicionales
            factory.setConnectionTimeout(30000);
            factory.setHandshakeTimeout(10000);
            factory.setShutdownTimeout(10000);
            factory.setRequestedHeartbeat(60);

            // Intentar conexión con reintentos
            int maxRetries = 5;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    connection = factory.newConnection();
                    channel = connection.createChannel();

                    logger.info("✅ Conectado a RabbitMQ en " + host + ":" + port);

                    // Verificar configuración
                    if (verifySetup()) {
                        return true;
                    } else {
                        logger.severe("❌ Error verificando configuración RabbitMQ");
                        return false;
                    }

                } catch (IOException | TimeoutException e) {
                    if (attempt < maxRetries) {
                        logger.warning("⚠️ Intento " + attempt + " fallido, reintentando en 5s...");
                        Thread.sleep(5000);
                    } else {
                        logger.log(Level.SEVERE, "❌ Error conectando a RabbitMQ después de " + maxRetries + " intentos",
                                e);
                        return false;
                    }
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error configurando conexión RabbitMQ: " + e.getMessage(), e);
        }

        return false;
    }

    public void disconnect() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }

            if (connection != null && connection.isOpen()) {
                connection.close();
            }

            logger.info("✅ Conexión RabbitMQ cerrada");

        } catch (IOException | TimeoutException e) {
            logger.log(Level.WARNING, "⚠️ Error cerrando conexión RabbitMQ: " + e.getMessage(), e);
        }
    }

    private boolean verifySetup() {
        try {
            // Verificar que el exchange existe
            channel.exchangeDeclarePassive("registro_bus");

            // Verificar que la cola queue_lp1 existe
            channel.queueDeclarePassive("queue_lp1");

            logger.info("✅ Exchange y colas verificados correctamente");
            return true;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "❌ Error verificando configuración: " + e.getMessage(), e);
            return false;
        }
    }

    public void setupConsumer(String queueName, Consumer<String> messageCallback) {
        try {
            // Configurar QoS para procesar de a uno
            channel.basicQos(1);

            // Crear el consumidor
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                try {
                    // Procesar mensaje
                    messageCallback.accept(message);

                    // Confirmar procesamiento
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                } catch (Exception e) {
                    logger.log(Level.SEVERE, "❌ Error procesando mensaje: " + e.getMessage(), e);

                    // Rechazar mensaje y reencolar
                    try {
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "❌ Error enviando NACK: " + ex.getMessage(), ex);
                    }
                }
            };

            CancelCallback cancelCallback = consumerTag -> {
                logger.warning("⚠️ Consumidor cancelado: " + consumerTag);
            };

            // Configurar el consumidor
            channel.basicConsume(queueName, false, deliverCallback, cancelCallback);

            logger.info("✅ Consumidor configurado para cola: " + queueName);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "❌ Error configurando consumidor: " + e.getMessage(), e);
        }
    }

    public void startConsuming() {
        try {
            logger.info("🔄 Iniciando consumo de mensajes...");

            // Mantener el hilo activo para consumir mensajes
            while (connection.isOpen()) {
                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {
            logger.info("🛑 Consumo interrumpido");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error durante el consumo: " + e.getMessage(), e);
        }
    }

    public boolean publishMessage(String exchange, String routingKey, String message) {
        try {
            // Configurar propiedades del mensaje
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2) // Mensaje persistente
                    .contentType("application/json")
                    .timestamp(new java.util.Date())
                    .build();

            // Publicar mensaje
            channel.basicPublish(
                    exchange,
                    routingKey,
                    properties,
                    message.getBytes(StandardCharsets.UTF_8));

            logger.fine("📤 Mensaje enviado: " + routingKey);
            return true;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "❌ Error publicando mensaje: " + e.getMessage(), e);
            return false;
        }
    }

    public QueueInfo getQueueInfo(String queueName) {
        try {
            AMQP.Queue.DeclareOk response = channel.queueDeclarePassive(queueName);
            return new QueueInfo(queueName, response.getMessageCount(), response.getConsumerCount());

        } catch (IOException e) {
            logger.log(Level.WARNING, "❌ Error obteniendo info de cola " + queueName + ": " + e.getMessage(), e);
            return null;
        }
    }

    public boolean isHealthy() {
        try {
            return connection != null && connection.isOpen() &&
                    channel != null && channel.isOpen();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clase para información de cola
     */
    public static class QueueInfo {
        private final String name;
        private final int messageCount;
        private final int consumerCount;

        public QueueInfo(String name, int messageCount, int consumerCount) {
            this.name = name;
            this.messageCount = messageCount;
            this.consumerCount = consumerCount;
        }

        public String getName() {
            return name;
        }

        public int getMessageCount() {
            return messageCount;
        }

        public int getConsumerCount() {
            return consumerCount;
        }

        @Override
        public String toString() {
            return String.format("QueueInfo{name='%s', messages=%d, consumers=%d}",
                    name, messageCount, consumerCount);
        }
    }
}