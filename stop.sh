#!/bin/bash

# Script de detención para el Sistema de Registro Distribuido
# PC3 - Sistemas Distribuidos

echo "🛑 DETENIENDO SISTEMA DE REGISTRO DISTRIBUIDO"
echo "=============================================="

# Función para mostrar el estado de los servicios
show_status() {
    echo ""
    echo "📊 ESTADO DE SERVICIOS:"
    echo "----------------------"
    docker-compose -f docker-compose.global.yml ps
}

# Mostrar estado actual
echo "📊 Estado actual de servicios:"
show_status

echo ""
echo "🔄 Deteniendo servicios gracefully..."

# Detener aplicaciones primero
echo "  📦 Deteniendo LP3 (Node.js)..."
docker-compose -f docker-compose.global.yml stop lp3_app

echo "  🐍 Deteniendo LP2 (Python)..."
docker-compose -f docker-compose.global.yml stop lp2_app

echo "  ☕ Deteniendo LP1 (Java)..."
docker-compose -f docker-compose.global.yml stop lp1_app

echo "  ⏳ Esperando que las aplicaciones se detengan..."
sleep 10

# Detener infraestructura
echo "  🗄️  Deteniendo bases de datos..."
docker-compose -f docker-compose.global.yml stop postgres mariadb

echo "  📡 Deteniendo RabbitMQ..."
docker-compose -f docker-compose.global.yml stop rabbitmq

echo ""
echo "🧹 Limpiando contenedores..."
docker-compose -f docker-compose.global.yml down --remove-orphans

# Preguntar si quiere limpiar volúmenes
echo ""
read -p "¿Quieres eliminar los volúmenes de datos? (esto borrará todos los datos) (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🗑️  Eliminando volúmenes de datos..."
    docker-compose -f docker-compose.global.yml down -v
    echo "✅ Volúmenes eliminados"
else
    echo "💾 Volúmenes de datos conservados"
fi

# Limpiar imágenes no utilizadas (opcional)
echo ""
read -p "¿Quieres limpiar imágenes Docker no utilizadas? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🧹 Limpiando imágenes no utilizadas..."
    docker image prune -f
    echo "✅ Imágenes limpiadas"
fi

# Limpiar red
echo "🌐 Limpiando redes..."
docker network prune -f 2>/dev/null || true

# Estado final
echo ""
echo "📊 Estado final:"
docker-compose -f docker-compose.global.yml ps

echo ""
echo "✅ ¡SISTEMA DETENIDO EXITOSAMENTE!"
echo "================================="
echo ""
echo "📖 COMANDOS DISPONIBLES:"
echo "  ./start.sh     - Reiniciar el sistema"
echo "  ./setup.sh     - Reconfigurar dependencias"
echo ""
echo "💡 NOTAS:"
echo "  - Los datos se conservan en volúmenes Docker (a menos que se hayan eliminado)"
echo "  - Para un reinicio completo: ./start.sh"
echo "  - Para limpiar todo: docker system prune -a --volumes"
echo "" 