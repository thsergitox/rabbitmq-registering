/**
 * Cliente RabbitMQ para LP3 - Node.js
 */

import amqp from 'amqplib';

class RabbitMQClient {
    constructor() {
        this.connection = null;
        this.channel = null;
        this.config = {
            host: process.env.RABBITMQ_HOST || '192.168.18.72',
            port: process.env.RABBITMQ_PORT || 5672,
            username: process.env.RABBITMQ_USER || 'admin',
            password: process.env.RABBITMQ_PASSWORD || 'admin123',
            vhost: '/'
        };
    }

    async connect() {
        try {
            const url = `amqp://${this.config.username}:${this.config.password}@${this.config.host}:${this.config.port}${this.config.vhost}`;
            
            // Intentar conexión con reintentos
            const maxRetries = 5;
            for (let attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    this.connection = await amqp.connect(url, {
                        heartbeat: 60,
                        connectionTimeout: 30000,
                        channelMax: 0,
                        frameMax: 0
                    });

                    this.channel = await this.connection.createChannel();
                    
                    console.log(`✅ Conectado a RabbitMQ en ${this.config.host}:${this.config.port}`);
                    
                    // Configurar manejo de errores
                    this.connection.on('error', (err) => {
                        console.error('❌ Error de conexión RabbitMQ:', err.message);
                    });

                    this.connection.on('close', () => {
                        console.log('🔌 Conexión RabbitMQ cerrada');
                    });

                    // Verificar configuración
                    if (await this.verifySetup()) {
                        return true;
                    } else {
                        console.error('❌ Error verificando configuración RabbitMQ');
                        return false;
                    }

                } catch (error) {
                    if (attempt < maxRetries) {
                        console.warn(`⚠️ Intento ${attempt} fallido, reintentando en 5s...`);
                        await this.sleep(5000);
                    } else {
                        console.error(`❌ Error conectando a RabbitMQ después de ${maxRetries} intentos:`, error.message);
                        return false;
                    }
                }
            }

        } catch (error) {
            console.error('❌ Error configurando conexión RabbitMQ:', error.message);
            return false;
        }
    }

    async verifySetup() {
        try {
            // Verificar que el exchange existe
            await this.channel.checkExchange('registro_bus');
            
            // Verificar que la cola queue_lp3_ack existe
            await this.channel.checkQueue('queue_lp3_ack');
            
            console.log('✅ Exchange y colas verificados correctamente');
            return true;

        } catch (error) {
            console.error('❌ Error verificando configuración:', error.message);
            return false;
        }
    }

    async setupConsumer(queueName, callback) {
        try {
            // Configurar QoS para procesar de a uno
            await this.channel.prefetch(1);
            
            // Configurar el consumidor
            await this.channel.consume(queueName, async (message) => {
                if (message) {
                    try {
                        const content = message.content.toString();
                        
                        // Procesar mensaje
                        await callback(content);
                        
                        // Confirmar procesamiento
                        this.channel.ack(message);
                        
                    } catch (error) {
                        console.error('❌ Error procesando mensaje:', error.message);
                        
                        // Rechazar mensaje y reencolar
                        this.channel.nack(message, false, true);
                    }
                }
            });
            
            console.log(`✅ Consumidor configurado para cola: ${queueName}`);
            return true;

        } catch (error) {
            console.error('❌ Error configurando consumidor:', error.message);
            return false;
        }
    }

    async publishMessage(exchange, routingKey, message) {
        try {
            const content = Buffer.from(JSON.stringify(message, null, 0));
            
            // Opciones del mensaje
            const options = {
                persistent: true,
                contentType: 'application/json',
                timestamp: Date.now()
            };
            
            // Publicar mensaje
            const published = this.channel.publish(exchange, routingKey, content, options);
            
            if (published) {
                console.log(`📤 Mensaje enviado: ${routingKey}`);
                return true;
            } else {
                console.error('❌ Error publicando mensaje (buffer lleno)');
                return false;
            }

        } catch (error) {
            console.error('❌ Error publicando mensaje:', error.message);
            return false;
        }
    }

    async getQueueInfo(queueName) {
        try {
            const queueInfo = await this.channel.checkQueue(queueName);
            return {
                queue: queueName,
                messages: queueInfo.messageCount,
                consumers: queueInfo.consumerCount
            };

        } catch (error) {
            console.error(`❌ Error obteniendo info de cola ${queueName}:`, error.message);
            return null;
        }
    }

    async isHealthy() {
        try {
            if (!this.connection || !this.channel) {
                return false;
            }

            // Verificar conexión
            await this.channel.checkExchange('registro_bus');
            return true;

        } catch (error) {
            console.error('❌ Health check falló:', error.message);
            return false;
        }
    }

    async disconnect() {
        try {
            if (this.channel) {
                await this.channel.close();
                console.log('✅ Canal RabbitMQ cerrado');
            }

            if (this.connection) {
                await this.connection.close();
                console.log('✅ Conexión RabbitMQ cerrada');
            }

        } catch (error) {
            console.error('❌ Error cerrando conexión RabbitMQ:', error.message);
        }
    }

    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    getConnectionStatus() {
        return {
            connected: this.connection !== null && this.channel !== null,
            host: this.config.host,
            port: this.config.port,
            vhost: this.config.vhost
        };
    }
}

export default RabbitMQClient; 