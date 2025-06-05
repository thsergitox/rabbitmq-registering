#!/usr/bin/env python3
"""
Gestor de Base de Datos para LP2 - MariaDB BD2
"""

import os
import logging
import mysql.connector
from mysql.connector import Error
from typing import Optional, List

logger = logging.getLogger('LP2-DatabaseManager')

class DatabaseManager:
    def __init__(self):
        self.connection = None
        self.config = {
            'host': os.getenv('DB_HOST', 'mariadb'),
            'port': int(os.getenv('DB_PORT', 3306)),
            'database': os.getenv('DB_NAME', 'bd2_dni'),
            'user': os.getenv('DB_USER', 'lp2_user'),
            'password': os.getenv('DB_PASSWORD', 'lp2_pass'),
            'charset': 'utf8mb4',
            'autocommit': True
        }
        
    def connect(self) -> bool:
        """Establece conexión con MariaDB"""
        try:
            self.connection = mysql.connector.connect(**self.config)
            
            if self.connection.is_connected():
                db_info = self.connection.get_server_info()
                logger.info(f"✅ Conectado a MariaDB {db_info}")
                
                # Verificar que la tabla existe
                if self._verify_tables():
                    logger.info("✅ Tablas verificadas correctamente")
                    return True
                else:
                    logger.error("❌ Error verificando tablas")
                    return False
                    
        except Error as e:
            logger.error(f"❌ Error conectando a MariaDB: {e}")
            return False
            
    def disconnect(self):
        """Cierra la conexión"""
        if self.connection and self.connection.is_connected():
            self.connection.close()
            logger.info("✅ Conexión a MariaDB cerrada")
            
    def is_connected(self) -> bool:
        """Verifica si hay una conexión activa a la base de datos"""
        try:
            return self.connection and self.connection.is_connected()
        except Exception:
            return False
            
    def _verify_tables(self) -> bool:
        """Verifica que las tablas necesarias existan"""
        try:
            cursor = self.connection.cursor()
            
            # Verificar tabla persona
            cursor.execute("SHOW TABLES LIKE 'persona'")
            if not cursor.fetchone():
                logger.error("❌ Tabla 'persona' no encontrada")
                return False
                
            # Verificar estructura de la tabla
            cursor.execute("DESCRIBE persona")
            columns = [row[0] for row in cursor.fetchall()]
            required_columns = ['id', 'dni', 'nombre', 'apellidos']
            
            for col in required_columns:
                if col not in columns:
                    logger.error(f"❌ Columna '{col}' no encontrada en tabla persona")
                    return False
                    
            # Contar registros
            cursor.execute("SELECT COUNT(*) FROM persona")
            count = cursor.fetchone()[0]
            logger.info(f"📊 BD2 tiene {count} registros en tabla persona")
            
            cursor.close()
            return True
            
        except Error as e:
            logger.error(f"❌ Error verificando tablas: {e}")
            return False
            
    def validate_dni(self, dni: str) -> bool:
        """Valida si un DNI existe en BD2"""
        try:
            cursor = self.connection.cursor()
            
            # Buscar DNI en la tabla persona
            query = "SELECT id FROM persona WHERE dni = %s LIMIT 1"
            cursor.execute(query, (dni,))
            result = cursor.fetchone()
            
            cursor.close()
            
            if result:
                logger.debug(f"✅ DNI {dni} encontrado en BD2")
                return True
            else:
                logger.debug(f"❌ DNI {dni} no encontrado en BD2")
                return False
                
        except Error as e:
            logger.error(f"❌ Error validando DNI {dni}: {e}")
            return False
            
    def validate_person_id(self, person_id: int) -> bool:
        """Valida si un ID de persona existe en BD2"""
        try:
            cursor = self.connection.cursor()
            
            # Buscar ID en la tabla persona
            query = "SELECT dni FROM persona WHERE id = %s LIMIT 1"
            cursor.execute(query, (person_id,))
            result = cursor.fetchone()
            
            cursor.close()
            
            if result:
                logger.debug(f"✅ ID persona {person_id} encontrado en BD2")
                return True
            else:
                logger.debug(f"❌ ID persona {person_id} no encontrado en BD2")
                return False
                
        except Error as e:
            logger.error(f"❌ Error validando ID persona {person_id}: {e}")
            return False
            
    def get_person_info(self, dni: str) -> Optional[dict]:
        """Obtiene información completa de una persona por DNI"""
        try:
            cursor = self.connection.cursor(dictionary=True)
            
            query = """
                SELECT id, dni, nombre, apellidos, lugar_nac, ubigeo, direccion 
                FROM persona 
                WHERE dni = %s 
                LIMIT 1
            """
            cursor.execute(query, (dni,))
            result = cursor.fetchone()
            
            cursor.close()
            return result
            
        except Error as e:
            logger.error(f"❌ Error obteniendo info de DNI {dni}: {e}")
            return None
            
    def search_persons_by_ids(self, person_ids: List[int]) -> List[dict]:
        """Busca múltiples personas por sus IDs"""
        if not person_ids:
            return []
            
        try:
            cursor = self.connection.cursor(dictionary=True)
            
            # Crear placeholders para la consulta IN
            placeholders = ','.join(['%s'] * len(person_ids))
            query = f"""
                SELECT id, dni, nombre, apellidos 
                FROM persona 
                WHERE id IN ({placeholders})
            """
            
            cursor.execute(query, person_ids)
            results = cursor.fetchall()
            
            cursor.close()
            return results
            
        except Error as e:
            logger.error(f"❌ Error buscando personas por IDs {person_ids}: {e}")
            return []
            
    def get_connection_status(self) -> dict:
        """Retorna el estado de la conexión"""
        try:
            if self.connection and self.connection.is_connected():
                cursor = self.connection.cursor()
                cursor.execute("SELECT 1")
                cursor.fetchone()
                cursor.close()
                
                return {
                    'connected': True,
                    'host': self.config['host'],
                    'database': self.config['database'],
                    'user': self.config['user']
                }
            else:
                return {'connected': False}
                
        except Exception as e:
            return {
                'connected': False,
                'error': str(e)
            } 