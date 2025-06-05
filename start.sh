#!/bin/bash

# Script de inicio para el Sistema de Registro Distribuido
# PC3 - Sistemas Distribuidos

set -e

echo "🚀 INICIANDO SISTEMA DE REGISTRO DISTRIBUIDO"
echo "============================================="

# Verificar que Docker esté corriendo
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker no está ejecutándose. Por favor inicia Docker."
    exit 1
fi

# Función para mostrar el estado de los servicios
show_status() {
    echo ""
    echo "📊 ESTADO DE SERVICIOS:"
    echo "----------------------"
    docker-compose -f docker-compose.global.yml ps
}

# Función para mostrar logs en tiempo real
show_logs() {
    echo ""
    echo "📋 Para ver logs en tiempo real:"
    echo "  docker-compose -f docker-compose.global.yml logs -f [servicio]"
    echo ""
    echo "Servicios disponibles:"
    echo "  - rabbitmq"
    echo "  - postgres"
    echo "  - mariadb"
    echo "  - lp1_app"
    echo "  - lp2_app"
    echo "  - lp3_app"
}

# Detener servicios existentes si están corriendo
echo "🛑 Deteniendo servicios existentes..."
docker-compose -f docker-compose.global.yml down --remove-orphans 2>/dev/null || true

# Limpiar contenedores huérfanos
echo "🧹 Limpiando contenedores huérfanos..."
docker container prune -f 2>/dev/null || true

echo ""
echo "🏗️  Construyendo e iniciando servicios..."

# Iniciar servicios de infraestructura primero
echo "📡 Iniciando RabbitMQ..."
docker-compose -f docker-compose.global.yml up -d rabbitmq

echo "🗄️  Iniciando bases de datos..."
docker-compose -f docker-compose.global.yml up -d postgres mariadb

echo "⏳ Esperando que la infraestructura esté lista..."
sleep 20

# Verificar que RabbitMQ esté listo
echo "🔍 Verificando RabbitMQ..."
timeout=60
while [ $timeout -gt 0 ]; do
    if docker-compose -f docker-compose.global.yml exec -T rabbitmq rabbitmq-diagnostics status >/dev/null 2>&1; then
        echo "✅ RabbitMQ está listo"
        break
    fi
    echo "  ⏳ Esperando RabbitMQ... ($timeout segundos restantes)"
    sleep 5
    timeout=$((timeout - 5))
done

if [ $timeout -le 0 ]; then
    echo "❌ RabbitMQ no respondió a tiempo"
    exit 1
fi

# Verificar PostgreSQL
echo "🔍 Verificando PostgreSQL..."
timeout=60
while [ $timeout -gt 0 ]; do
    if docker-compose -f docker-compose.global.yml exec -T postgres pg_isready -U lp1_user -d bd1_users >/dev/null 2>&1; then
        echo "✅ PostgreSQL está listo"
        break
    fi
    echo "  ⏳ Esperando PostgreSQL... ($timeout segundos restantes)"
    sleep 5
    timeout=$((timeout - 5))
done

if [ $timeout -le 0 ]; then
    echo "❌ PostgreSQL no respondió a tiempo"
    exit 1
fi

# Verificar MariaDB
echo "🔍 Verificando MariaDB..."
timeout=60
while [ $timeout -gt 0 ]; do
    if docker-compose -f docker-compose.global.yml exec -T mariadb mysql -u lp2_user -plp2_pass -e "SELECT 1" bd2_dni >/dev/null 2>&1; then
        echo "✅ MariaDB está listo"
        break
    fi
    echo "  ⏳ Esperando MariaDB... ($timeout segundos restantes)"
    sleep 5
    timeout=$((timeout - 5))
done

if [ $timeout -le 0 ]; then
    echo "❌ MariaDB no respondió a tiempo"
    exit 1
fi

# Iniciar aplicaciones
echo ""
echo "🚀 Iniciando aplicaciones..."

echo "☕ Iniciando LP1 (Java - Persistencia)..."
docker-compose -f docker-compose.global.yml up -d lp1_app

echo "🐍 Iniciando LP2 (Python - Validador)..."
docker-compose -f docker-compose.global.yml up -d lp2_app

echo "📦 Iniciando LP3 (Node.js - Cliente)..."
docker-compose -f docker-compose.global.yml up -d lp3_app

echo ""
echo "⏳ Esperando que las aplicaciones inicien..."
sleep 30

# Mostrar estado final
show_status

echo ""
echo "🎉 ¡SISTEMA INICIADO EXITOSAMENTE!"
echo "=================================="
echo ""
echo "🌐 ACCESO A SERVICIOS:"
echo "  🐰 RabbitMQ Management: http://localhost:15672"
echo "     Usuario: admin, Contraseña: admin123"
echo ""
echo "  💾 Bases de Datos:"
echo "     PostgreSQL (BD1): localhost:5432"
echo "     MariaDB (BD2):    localhost:3306"
echo ""
echo "  🖥️  Aplicaciones:"
echo "     LP1 (Java):     Puerto 8081"
echo "     LP2 (Python):   Puerto 8082"
echo "     LP3 (Node.js):  Puerto 8083"
echo ""
echo "📋 COMANDOS ÚTILES:"
echo "  Ver logs:           docker-compose -f docker-compose.global.yml logs -f"
echo "  Detener sistema:    ./stop.sh"
echo "  Reiniciar:          ./stop.sh && ./start.sh"
echo ""
echo "🎯 PARA USAR LP3 CLIENTE:"
echo "  docker exec -it lp3-client sh"
echo "  npm start"
echo ""

# Opción para mostrar logs automáticamente
read -p "¿Quieres ver los logs en tiempo real? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "📋 Mostrando logs (Ctrl+C para salir)..."
    docker-compose -f docker-compose.global.yml logs -f
fi 