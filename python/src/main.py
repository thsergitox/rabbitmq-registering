#!/usr/bin/env python3
"""
LP2 - Validador Python
Valida datos contra BD2 (MariaDB) y responde vía RabbitMQ
"""

import os
import json
import logging
import time
import asyncio
from typing import Dict, List, Optional
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
import uvicorn

from .db_manager import DatabaseManager
from .rabbitmq_client import RabbitMQClient

# Configuración de logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('LP2-Validator')

# Variable global para el validador
validator_instance = None

class LP2Validator:
    def __init__(self):
        self.db_manager = DatabaseManager()
        self.rabbitmq_client = RabbitMQClient()
        self.running = False
        
    def start(self):
        """Inicia el servicio validador"""
        logger.info("🚀 Iniciando LP2 Validator...")
        
        # Conectar a la base de datos
        if not self.db_manager.connect():
            logger.error("❌ No se pudo conectar a la base de datos")
            return False
            
        # Conectar a RabbitMQ
        if not self.rabbitmq_client.connect():
            logger.error("❌ No se pudo conectar a RabbitMQ")
            return False
            
        # Configurar el consumidor
        self.rabbitmq_client.setup_consumer(
            queue_name='queue_lp2',
            callback=self.process_validation_request
        )
        
        self.running = True
        logger.info("✅ LP2 Validator iniciado correctamente")
        logger.info("📋 Esperando solicitudes de validación...")
        
        return True
        
    def start_consuming_background(self):
        """Inicia el consumo de mensajes en background"""
        try:
            self.rabbitmq_client.start_consuming()
        except Exception as e:
            logger.error(f"❌ Error en consumo RabbitMQ: {e}")
            
    def stop(self):
        """Detiene el servicio"""
        self.running = False
        self.rabbitmq_client.disconnect()
        self.db_manager.disconnect()
        logger.info("✅ LP2 Validator detenido")
        
    def process_validation_request(self, ch, method, properties, body):
        """Procesa una solicitud de validación"""
        try:
            # Parsear el mensaje
            data = json.loads(body.decode('utf-8'))
            logger.info(f"📨 Validando registro: {data.get('dni', 'N/A')}")
            
            # Validar los datos
            validation_result = self.validate_user_data(data)
            
            # Preparar respuesta y routing key según el resultado
            if validation_result['valid']:
                routing_key = 'lp2.query.ok'
                # Para LP1 (Java), enviamos los datos originales del usuario
                message_to_publish = data
                status_log = "✅ VÁLIDO"
            else:
                routing_key = 'lp2.query.fail'
                # Para LP3 (Node), enviamos un objeto de respuesta con el error
                message_to_publish = {
                    'request_id': data.get('request_id'),
                    'dni': data.get('dni'),
                    'valid': False,
                    'message': validation_result['message'],
                    'timestamp': time.time()
                }
                status_log = "❌ INVÁLIDO"

            # Enviar respuesta a la cola correspondiente
            if self.rabbitmq_client.publish_message(
                exchange='registro_bus',
                routing_key=routing_key,
                message=message_to_publish
            ):
                logger.info(f"{status_log} - DNI: {data.get('dni')} - {validation_result.get('message', '')}")
            else:
                logger.error(f"❌ Error enviando respuesta para DNI: {data.get('dni')}")

            # Confirmar el mensaje a RabbitMQ
            ch.basic_ack(delivery_tag=method.delivery_tag)

        except json.JSONDecodeError as e:
            logger.error(f"❌ Error parseando JSON: {e}")
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        except Exception as e:
            logger.error(f"❌ Error procesando validación: {e}")
            # Reencolar en caso de error transitorio
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
            
    def validate_user_data(self, data: Dict) -> Dict:
        """Valida los datos del usuario contra BD2"""
        try:
            dni = data.get('dni')
            amigos_ids = data.get('amigos_frecuentes', [])
            
            if not dni:
                return {
                    'valid': False,
                    'message': 'DNI es requerido'
                }
                
            # Validar que el DNI existe en BD2
            if not self.db_manager.validate_dni(dni):
                return {
                    'valid': False,
                    'message': f'DNI {dni} no encontrado en BD2'
                }
                
            # Validar amigos frecuentes si existen
            if amigos_ids:
                invalid_friends = []
                for amigo_id in amigos_ids:
                    if not self.db_manager.validate_person_id(amigo_id):
                        invalid_friends.append(amigo_id)
                        
                if invalid_friends:
                    return {
                        'valid': False,
                        'message': f'Amigos no encontrados en BD2: {invalid_friends}'
                    }
                    
            return {
                'valid': True,
                'message': 'Validación exitosa'
            }
            
        except Exception as e:
            logger.error(f"❌ Error en validación: {e}")
            return {
                'valid': False,
                'message': f'Error interno de validación: {str(e)}'
            }

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Gestiona el ciclo de vida de la aplicación FastAPI"""
    global validator_instance
    
    # Startup
    logger.info("🚀 Iniciando aplicación LP2 Validator...")
    validator_instance = LP2Validator()
    
    if validator_instance.start():
        # Iniciar consumo de RabbitMQ en background
        loop = asyncio.get_event_loop()
        task = loop.create_task(
            asyncio.to_thread(validator_instance.start_consuming_background)
        )
        logger.info("✅ LP2 Validator FastAPI iniciado correctamente")
    else:
        logger.error("❌ Error iniciando LP2 Validator")
    
    yield
    
    # Shutdown
    logger.info("🛑 Deteniendo LP2 Validator...")
    if validator_instance:
        validator_instance.stop()

# Crear la aplicación FastAPI
app = FastAPI(
    title="LP2 Validator API",
    description="Validador de datos contra BD2 (MariaDB) con comunicación via RabbitMQ",
    version="1.0.0",
    lifespan=lifespan
)

@app.get("/")
async def root():
    """Endpoint de salud básico"""
    return {
        "service": "LP2 Validator",
        "status": "running",
        "version": "1.0.0"
    }

@app.get("/health")
async def health_check():
    """Endpoint de verificación de salud del servicio"""
    global validator_instance
    
    if not validator_instance:
        raise HTTPException(status_code=503, detail="Validator not initialized")
    
    health_status = {
        "status": "healthy" if validator_instance.running else "unhealthy",
        "database": "connected" if validator_instance.db_manager.is_connected() else "disconnected",
        "rabbitmq": "connected" if validator_instance.rabbitmq_client.is_connected() else "disconnected",
        "timestamp": time.time()
    }
    
    if health_status["status"] == "unhealthy":
        raise HTTPException(status_code=503, detail=health_status)
    
    return health_status

@app.post("/validate")
async def validate_dni(data: dict):
    """Endpoint para validación directa de DNI (sin RabbitMQ)"""
    global validator_instance
    
    if not validator_instance:
        raise HTTPException(status_code=503, detail="Validator not initialized")
    
    if not validator_instance.running:
        raise HTTPException(status_code=503, detail="Validator not running")
    
    result = validator_instance.validate_user_data(data)
    
    return {
        "dni": data.get('dni'),
        "valid": result['valid'],
        "message": result['message'],
        "timestamp": time.time()
    }

def main():
    """Función principal - solo para testing local"""
    logger.info("🏁 Iniciando LP2 - Validador Python en modo standalone")
    
    validator = LP2Validator()
    
    try:
        if validator.start():
            validator.start_consuming_background()
    except KeyboardInterrupt:
        logger.info("🛑 Deteniendo LP2 Validator...")
    except Exception as e:
        logger.error(f"❌ Error fatal: {e}")
    finally:
        validator.stop()

if __name__ == "__main__":
    # Si se ejecuta directamente, modo standalone (sin FastAPI)
    main() 