#!/bin/bash

# Script de configuración para el Sistema de Registro Distribuido
# PC3 - Sistemas Distribuidos

set -e

echo "🏗️  CONFIGURANDO SISTEMA DE REGISTRO DISTRIBUIDO"
echo "================================================="

# Crear directorios necesarios
echo "📁 Creando directorios..."
mkdir -p java/lib
mkdir -p logs

# Descargar dependencias de Java
echo "☕ Descargando dependencias de Java..."

# PostgreSQL Driver
if [ ! -f "java/lib/postgresql.jar" ]; then
    echo "  📥 Descargando PostgreSQL JDBC Driver..."
    wget -q -O java/lib/postgresql.jar \
        "https://jdbc.postgresql.org/download/postgresql-42.7.1.jar"
fi

# RabbitMQ Client
if [ ! -f "java/lib/amqp-client.jar" ]; then
    echo "  📥 Descargando RabbitMQ AMQP Client..."
    wget -q -O java/lib/amqp-client.jar \
        "https://repo1.maven.org/maven2/com/rabbitmq/amqp-client/5.20.0/amqp-client-5.20.0.jar"
fi

# SLF4J (Logging)
if [ ! -f "java/lib/slf4j-api.jar" ]; then
    echo "  📥 Descargando SLF4J API..."
    wget -q -O java/lib/slf4j-api.jar \
        "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"
fi

if [ ! -f "java/lib/slf4j-simple.jar" ]; then
    echo "  📥 Descargando SLF4J Simple..."
    wget -q -O java/lib/slf4j-simple.jar \
        "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar"
fi

# Gson (JSON parsing)
if [ ! -f "java/lib/gson.jar" ]; then
    echo "  📥 Descargando Gson..."
    wget -q -O java/lib/gson.jar \
        "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
fi

# HikariCP (Connection Pooling)
if [ ! -f "java/lib/hikaricp.jar" ]; then
    echo "  📥 Descargando HikariCP..."
    wget -q -O java/lib/hikaricp.jar \
        "https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar"
fi

echo "✅ Dependencias de Java descargadas"

# Verificar Node.js
echo "📦 Verificando Node.js..."
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo "  ✅ Node.js encontrado: $NODE_VERSION"
else
    echo "  ❌ Node.js no encontrado. Por favor instala Node.js 20+"
    exit 1
fi

# Instalar dependencias Node.js
echo "📦 Instalando dependencias Node.js..."
cd node
npm install
cd ..
echo "✅ Dependencias Node.js instaladas"

# Verificar Python
echo "🐍 Verificando Python..."
if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version)
    echo "  ✅ Python encontrado: $PYTHON_VERSION"
else
    echo "  ❌ Python3 no encontrado. Por favor instala Python 3.11+"
    exit 1
fi

# Instalar dependencias Python
echo "🐍 Instalando dependencias Python..."
cd python
pip3 install -r requirements.txt
cd ..
echo "✅ Dependencias Python instaladas"

# Verificar Docker
echo "🐋 Verificando Docker..."
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    echo "  ✅ Docker encontrado: $DOCKER_VERSION"
else
    echo "  ❌ Docker no encontrado. Por favor instala Docker"
    exit 1
fi

# Verificar Docker Compose
echo "🐋 Verificando Docker Compose..."
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version)
    echo "  ✅ Docker Compose encontrado: $COMPOSE_VERSION"
else
    echo "  ❌ Docker Compose no encontrado. Por favor instala Docker Compose"
    exit 1
fi

# Hacer scripts ejecutables
chmod +x start.sh
chmod +x stop.sh

echo ""
echo "🎉 ¡CONFIGURACIÓN COMPLETADA!"
echo "================================="
echo ""
echo "📖 COMANDOS DISPONIBLES:"
echo "  ./start.sh     - Iniciar todo el sistema"
echo "  ./stop.sh      - Detener todo el sistema"
echo ""
echo "🌐 PUERTOS DE ACCESO:"
echo "  RabbitMQ Management: http://localhost:15672 (admin/admin123)"
echo "  PostgreSQL:          localhost:5432 (lp1_user/lp1_pass)"
echo "  MariaDB:             localhost:3306 (lp2_user/lp2_pass)"
echo "  LP1 (Java):          localhost:8081"
echo "  LP2 (Python):        localhost:8082"
echo "  LP3 (Node.js):       localhost:8083"
echo ""
echo "🚀 Para iniciar el sistema completo:"
echo "  ./start.sh"
echo "" 