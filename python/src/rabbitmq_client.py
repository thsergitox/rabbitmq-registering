#!/usr/bin/env python3
"""
Cliente RabbitMQ para LP2 - Validador Python
"""

import os
import json
import logging
import pika
from pika.adapters.blocking_connection import BlockingChannel
from typing import Callable, Dict, Any
import time

logger = logging.getLogger('LP2-RabbitMQClient')

class RabbitMQClient:
    def __init__(self):
        self.connection = None
        self.channel = None
        self.config = {
            'host': os.getenv('RABBITMQ_HOST', 'rabbitmq'),
            'port': int(os.getenv('RABBITMQ_PORT', 5672)),
            'username': os.getenv('RABBITMQ_USER', 'admin'),
            'password': os.getenv('RABBITMQ_PASSWORD', 'admin123'),
            'virtual_host': '/'
        }
        
    def connect(self) -> bool:
        """Establece conexión con RabbitMQ"""
        try:
            # Configurar credenciales
            credentials = pika.PlainCredentials(
                self.config['username'], 
                self.config['password']
            )
            
            # Configurar parámetros de conexión
            parameters = pika.ConnectionParameters(
                host=self.config['host'],
                port=self.config['port'],
                virtual_host=self.config['virtual_host'],
                credentials=credentials,
                heartbeat=600,
                blocked_connection_timeout=300,
            )
            
            # Intentar conectar con reintentos
            max_retries = 5
            for attempt in range(max_retries):
                try:
                    self.connection = pika.BlockingConnection(parameters)
                    self.channel = self.connection.channel()
                    
                    logger.info(f"✅ Conectado a RabbitMQ en {self.config['host']}:{self.config['port']}")
                    
                    # Verificar configuración
                    if self._verify_setup():
                        return True
                    else:
                        logger.error("❌ Error verificando configuración RabbitMQ")
                        return False
                        
                except pika.exceptions.AMQPConnectionError as e:
                    if attempt < max_retries - 1:
                        logger.warning(f"⚠️ Intento {attempt + 1} fallido, reintentando en 5s...")
                        time.sleep(5)
                    else:
                        logger.error(f"❌ Error conectando a RabbitMQ después de {max_retries} intentos: {e}")
                        return False
                        
        except Exception as e:
            logger.error(f"❌ Error configurando conexión RabbitMQ: {e}")
            return False
            
    def disconnect(self):
        """Cierra la conexión con RabbitMQ"""
        try:
            if self.channel and not self.channel.is_closed:
                self.channel.close()
                
            if self.connection and not self.connection.is_closed:
                self.connection.close()
                
            logger.info("✅ Conexión RabbitMQ cerrada")
            
        except Exception as e:
            logger.error(f"❌ Error cerrando conexión RabbitMQ: {e}")
            
    def is_connected(self) -> bool:
        """Verifica si hay una conexión activa a RabbitMQ"""
        try:
            return (self.connection and not self.connection.is_closed and 
                    self.channel and not self.channel.is_closed)
        except Exception:
            return False
            
    def _verify_setup(self) -> bool:
        """Verifica que el exchange y colas estén configurados"""
        try:
            # Verificar que el exchange existe
            self.channel.exchange_declare(
                exchange='registro_bus',
                exchange_type='direct',
                passive=True  # Solo verificar, no crear
            )
            
            # Verificar que la cola queue_lp2 existe
            self.channel.queue_declare(
                queue='queue_lp2',
                passive=True
            )
            
            logger.info("✅ Exchange y colas verificados correctamente")
            return True
            
        except pika.exceptions.ChannelClosedByBroker as e:
            logger.error(f"❌ Error verificando configuración: {e}")
            return False
        except Exception as e:
            logger.error(f"❌ Error verificando setup RabbitMQ: {e}")
            return False
            
    def setup_consumer(self, queue_name: str, callback: Callable):
        """Configura el consumidor para una cola específica"""
        try:
            # Configurar QoS para procesar de a uno
            self.channel.basic_qos(prefetch_count=1)
            
            # Configurar el consumidor
            self.channel.basic_consume(
                queue=queue_name,
                on_message_callback=callback,
                auto_ack=False  # Confirmación manual
            )
            
            logger.info(f"✅ Consumidor configurado para cola: {queue_name}")
            return True
            
        except Exception as e:
            logger.error(f"❌ Error configurando consumidor: {e}")
            return False
            
    def start_consuming(self):
        """Inicia el consumo de mensajes"""
        try:
            logger.info("🔄 Iniciando consumo de mensajes...")
            self.channel.start_consuming()
            
        except KeyboardInterrupt:
            logger.info("🛑 Deteniendo consumo por interrupción del usuario")
            self.stop_consuming()
        except Exception as e:
            logger.error(f"❌ Error durante el consumo: {e}")
            
    def stop_consuming(self):
        """Detiene el consumo de mensajes"""
        try:
            if self.channel:
                self.channel.stop_consuming()
                logger.info("✅ Consumo de mensajes detenido")
        except Exception as e:
            logger.error(f"❌ Error deteniendo consumo: {e}")
            
    def publish_message(self, exchange: str, routing_key: str, message: Dict[str, Any]) -> bool:
        """Publica un mensaje en RabbitMQ"""
        try:
            # Convertir mensaje a JSON
            body = json.dumps(message, ensure_ascii=False, default=str)
            
            # Configurar propiedades del mensaje
            properties = pika.BasicProperties(
                delivery_mode=2,  # Mensaje persistente
                content_type='application/json',
                timestamp=int(time.time())
            )
            
            # Publicar mensaje
            self.channel.basic_publish(
                exchange=exchange,
                routing_key=routing_key,
                body=body.encode('utf-8'),
                properties=properties
            )
            
            logger.debug(f"📤 Mensaje enviado: {routing_key}")
            return True
            
        except Exception as e:
            logger.error(f"❌ Error publicando mensaje: {e}")
            return False
            
    def get_queue_info(self, queue_name: str) -> Dict[str, Any]:
        """Obtiene información de una cola"""
        try:
            method = self.channel.queue_declare(queue=queue_name, passive=True)
            return {
                'queue': queue_name,
                'messages': method.method.message_count,
                'consumers': method.method.consumer_count
            }
        except Exception as e:
            logger.error(f"❌ Error obteniendo info de cola {queue_name}: {e}")
            return {}
            
    def health_check(self) -> Dict[str, Any]:
        """Verifica el estado de salud de la conexión"""
        try:
            if not self.connection or self.connection.is_closed:
                return {'status': 'disconnected', 'error': 'No connection'}
                
            if not self.channel or self.channel.is_closed:
                return {'status': 'disconnected', 'error': 'No channel'}
                
            # Verificar conectividad
            self.channel.queue_declare(queue='health_check', passive=False, auto_delete=True)
            
            return {
                'status': 'connected',
                'host': self.config['host'],
                'port': self.config['port'],
                'virtual_host': self.config['virtual_host']
            }
            
        except Exception as e:
            return {
                'status': 'error',
                'error': str(e)
            } 