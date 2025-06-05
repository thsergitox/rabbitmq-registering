#!/usr/bin/env node

/**
 * LP3 - Cliente Node.js
 * Cliente para sistema de registro distribuido
 */

import express from 'express';
import cors from 'cors';
import { v4 as uuidv4 } from 'uuid';
import inquirer from 'inquirer';
import cliProgress from 'cli-progress';
import RabbitMQClient from './client.js';
import PerformanceAnalyzer from './performance.js';
import CSVDataLoader from './csvDataLoader.js';

// Configuración
const PORT = process.env.PORT || 8083;
const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Variables globales
let rabbitmqClient;
let performanceAnalyzer;
let csvDataLoader;
let isRunning = false;

/**
 * Clase principal LP3Client
 */
class LP3Client {
    constructor() {
        this.rabbitmqClient = new RabbitMQClient();
        this.performanceAnalyzer = new PerformanceAnalyzer();
        this.csvDataLoader = new CSVDataLoader();
        this.pendingRequests = new Map();
        this.isConnected = false;
    }

    async start() {
        console.log('🏁 Iniciando LP3 - Cliente Node.js');
        
        try {
            // Cargar datos CSV
            console.log('📊 Cargando datos de usuarios...');
            const csvLoaded = await this.csvDataLoader.loadCSVData();
            if (!csvLoaded) {
                console.error('❌ No se pudieron cargar los datos CSV. Continuando sin datos...');
            } else {
                const stats = this.csvDataLoader.getStats();
                console.log(`✅ Datos CSV cargados: ${stats.totalUsers} usuarios disponibles`);
            }

            // Conectar a RabbitMQ
            if (await this.rabbitmqClient.connect()) {
                this.isConnected = true;
                console.log('✅ LP3 Cliente conectado a RabbitMQ');

                // Configurar consumidor para confirmaciones
                await this.rabbitmqClient.setupConsumer(
                    'queue_lp3_ack',
                    this.handleAckMessage.bind(this)
                );

                // Iniciar servidor web
                this.startWebServer();

                // Mostrar menú interactivo
                await this.showMainMenu();
            } else {
                console.error('❌ No se pudo conectar a RabbitMQ');
                process.exit(1);
            }
        } catch (error) {
            console.error('❌ Error iniciando LP3:', error.message);
            process.exit(1);
        }
    }

    startWebServer() {
        // Endpoint de salud
        app.get('/health', (req, res) => {
            res.json({
                status: 'ok',
                service: 'LP3 Client',
                connected: this.isConnected,
                csvDataLoaded: this.csvDataLoader.hasData(),
                totalUsers: this.csvDataLoader.getStats().totalUsers,
                timestamp: new Date().toISOString()
            });
        });

        // Endpoint para registro manual
        app.post('/register', async (req, res) => {
            try {
                const userData = req.body;
                const requestId = await this.sendRegistrationRequest(userData);
                
                res.json({
                    success: true,
                    requestId: requestId,
                    message: 'Solicitud enviada'
                });
            } catch (error) {
                res.status(500).json({
                    success: false,
                    error: error.message
                });
            }
        });

        // Endpoint para estadísticas
        app.get('/stats', (req, res) => {
            res.json({
                performance: this.performanceAnalyzer.getStats(),
                csvData: this.csvDataLoader.getStats()
            });
        });

        // Endpoint para obtener usuario aleatorio del CSV
        app.get('/random-user', (req, res) => {
            const user = this.csvDataLoader.getRandomUser();
            if (user) {
                res.json(user);
            } else {
                res.status(404).json({ error: 'No hay usuarios disponibles' });
            }
        });

        app.listen(PORT, () => {
            console.log(`🌐 Servidor web LP3 ejecutándose en puerto ${PORT}`);
        });
    }

    async showMainMenu() {
        while (true) {
            console.log('\n' + '='.repeat(50));
            console.log('🎯 LP3 - SISTEMA DE REGISTRO DISTRIBUIDO');
            console.log('='.repeat(50));

            const choices = [
                { name: '👤 Registro manual de usuario', value: 'manual' },
                { name: '📊 Registro desde CSV (1 usuario)', value: 'single_csv' },
                { name: '🚀 Simulación con datos CSV (configurable)', value: 'simulation' },
                { name: '📈 Ver estadísticas', value: 'stats' },
                { name: '🧪 Probar conexión', value: 'test' },
                { name: '📁 Ver información CSV', value: 'csv_info' },
                { name: '❌ Salir', value: 'exit' }
            ];

            const { action } = await inquirer.prompt([{
                type: 'list',
                name: 'action',
                message: 'Selecciona una opción:',
                choices: choices
            }]);

            switch (action) {
                case 'manual':
                    await this.manualRegistration();
                    break;
                case 'single_csv':
                    await this.singleCSVRegistration();
                    break;
                case 'simulation':
                    await this.runSimulation();
                    break;
                case 'stats':
                    this.showStatistics();
                    break;
                case 'test':
                    await this.testConnection();
                    break;
                case 'csv_info':
                    this.showCSVInfo();
                    break;
                case 'exit':
                    await this.stop();
                    process.exit(0);
                    break;
            }
        }
    }

    async manualRegistration() {
        console.log('\n📝 REGISTRO MANUAL DE USUARIO');
        console.log('-'.repeat(30));

        try {
            const userData = await inquirer.prompt([
                {
                    type: 'input',
                    name: 'nombre',
                    message: 'Nombre completo:',
                    validate: input => input.trim().length > 0 || 'El nombre es requerido'
                },
                {
                    type: 'input',
                    name: 'correo',
                    message: 'Correo electrónico:',
                    validate: input => {
                        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                        return emailRegex.test(input) || 'Ingresa un correo válido';
                    }
                },
                {
                    type: 'input',
                    name: 'clave',
                    message: 'Clave (4 dígitos):',
                    validate: input => /^\d{4}$/.test(input) || 'La clave debe tener 4 dígitos'
                },
                {
                    type: 'input',
                    name: 'dni',
                    message: 'DNI (8 dígitos):',
                    validate: input => /^\d{8}$/.test(input) || 'El DNI debe tener 8 dígitos'
                },
                {
                    type: 'input',
                    name: 'telefono',
                    message: 'Teléfono:',
                    validate: input => /^\d{9}$/.test(input) || 'El teléfono debe tener 9 dígitos'
                },
                {
                    type: 'input',
                    name: 'amigos',
                    message: 'Amigos frecuentes (IDs separados por comas, opcional):',
                    default: ''
                }
            ]);

            // Procesar amigos frecuentes
            userData.amigosFrecuentes = userData.amigos
                .split(',')
                .map(id => parseInt(id.trim()))
                .filter(id => !isNaN(id) && id > 0);

            delete userData.amigos;

            console.log('\n🚀 Enviando solicitud de registro...');
            const requestId = await this.sendRegistrationRequest(userData);
            
            console.log(`✅ Solicitud enviada con ID: ${requestId}`);
            console.log('⏳ Esperando respuesta del sistema...');

        } catch (error) {
            console.error('❌ Error en registro manual:', error.message);
        }
    }

    async singleCSVRegistration() {
        console.log('\n📝 REGISTRO DESDE CSV (1 USUARIO)');
        console.log('-'.repeat(30));

        if (!this.csvDataLoader.hasData()) {
            console.log('❌ No hay datos CSV disponibles');
            return;
        }

        try {
            const user = this.csvDataLoader.getNextUser();
            if (!user) {
                console.log('❌ No se pudo obtener usuario del CSV');
                return;
            }

            // Agregar amigos frecuentes aleatorios
            user.amigosFrecuentes = this.csvDataLoader.generateRandomFriends(user.id);

            console.log('\n📋 Datos del usuario:');
            console.log(`   Nombre: ${user.nombre}`);
            console.log(`   Correo: ${user.correo}`);
            console.log(`   DNI: ${user.dni}`);
            console.log(`   Teléfono: ${user.telefono}`);
            console.log(`   Amigos frecuentes: ${user.amigosFrecuentes.join(', ') || 'Ninguno'}`);

            console.log('\n🚀 Enviando solicitud de registro...');
            const requestId = await this.sendRegistrationRequest(user);
            
            console.log(`✅ Solicitud enviada con ID: ${requestId}`);
            console.log('⏳ Esperando respuesta del sistema...');

        } catch (error) {
            console.error('❌ Error en registro desde CSV:', error.message);
        }
    }

    async runSimulation() {
        console.log('\n🚀 SIMULACIÓN CON DATOS CSV');
        console.log('-'.repeat(30));

        if (!this.csvDataLoader.hasData()) {
            console.log('❌ No hay datos CSV disponibles para simulación');
            console.log('💡 Asegúrate de que el archivo user.csv esté disponible');
            return;
        }

        const csvStats = this.csvDataLoader.getStats();
        console.log(`📊 Usuarios disponibles en CSV: ${csvStats.totalUsers}`);

        const { requestCount } = await inquirer.prompt([{
            type: 'input',
            name: 'requestCount',
            message: `Número de registros a simular (máximo ${csvStats.totalUsers}):`,
            default: Math.min(1000, csvStats.totalUsers).toString(),
            validate: input => {
                const num = parseInt(input);
                return (num >= 1 && num <= csvStats.totalUsers) || `Debe ser entre 1 y ${csvStats.totalUsers}`;
            }
        }]);

        const { concurrent } = await inquirer.prompt([{
            type: 'input',
            name: 'concurrent',
            message: 'Número de solicitudes concurrentes (1-50):',
            default: '10',
            validate: input => {
                const num = parseInt(input);
                return (num >= 1 && num <= 50) || 'Debe ser entre 1 y 50';
            }
        }]);

        const { useRandomFriends } = await inquirer.prompt([{
            type: 'confirm',
            name: 'useRandomFriends',
            message: '¿Generar amigos frecuentes aleatorios?',
            default: true
        }]);

        console.log('\n📊 Iniciando simulación con datos CSV...');
        
        // Reiniciar métricas
        this.performanceAnalyzer.reset();
        this.csvDataLoader.reset();
        
        const progress = new cliProgress.SingleBar({
            format: 'Progreso |{bar}| {percentage}% | {value}/{total} | Éxito: {success} | Error: {error}',
            barCompleteChar: '\u2588',
            barIncompleteChar: '\u2591',
            hideCursor: true
        });

        const totalRequests = parseInt(requestCount);
        const concurrentLimit = parseInt(concurrent);
        
        progress.start(totalRequests, 0, { success: 0, error: 0 });

        try {
            // Generar función que usa datos CSV
            const csvRequestGenerator = () => {
                const user = this.csvDataLoader.getNextUser();
                if (!user) {
                    throw new Error('No hay más usuarios disponibles en CSV');
                }

                // Agregar amigos frecuentes si está habilitado
                if (useRandomFriends) {
                    user.amigosFrecuentes = this.csvDataLoader.generateRandomFriends(user.id);
                }

                return this.sendRegistrationRequest(user);
            };

            await this.performanceAnalyzer.runLoadTest(
                totalRequests,
                concurrentLimit,
                csvRequestGenerator,
                (completed, success, error) => {
                    progress.update(completed, { success, error });
                }
            );

            progress.stop();
            
            console.log('\n✅ Simulación completada!');
            this.showSimulationResults();

        } catch (error) {
            progress.stop();
            console.error('\n❌ Error durante la simulación:', error.message);
        }
    }

    showSimulationResults() {
        const stats = this.performanceAnalyzer.getStats();
        
        console.log('\n📈 RESULTADOS DE LA SIMULACIÓN');
        console.log('='.repeat(40));
        console.log(`📊 Total de solicitudes: ${stats.totalRequests}`);
        console.log(`✅ Exitosas: ${stats.successfulRequests} (${stats.successRate}%)`);
        console.log(`❌ Fallidas: ${stats.failedRequests}`);
        console.log(`⏱️  Tiempo total: ${stats.totalTime}ms`);
        console.log(`🚀 Throughput: ${stats.throughput} req/seg`);
        console.log(`⌛ Latencia promedio: ${stats.averageLatency}ms`);
        console.log(`📊 Latencia mínima: ${stats.minLatency}ms`);
        console.log(`📊 Latencia máxima: ${stats.maxLatency}ms`);
    }

    showStatistics() {
        const performanceStats = this.performanceAnalyzer.getStats();
        const csvStats = this.csvDataLoader.getStats();
        
        console.log('\n📊 ESTADÍSTICAS DEL SISTEMA');
        console.log('='.repeat(40));
        console.log('📈 PERFORMANCE:');
        console.log(`   Solicitudes totales: ${performanceStats.totalRequests}`);
        console.log(`   Exitosas: ${performanceStats.successfulRequests}`);
        console.log(`   Fallidas: ${performanceStats.failedRequests}`);
        console.log(`   Tasa de éxito: ${performanceStats.successRate}%`);
        console.log(`   Throughput: ${performanceStats.throughput} req/seg`);
        console.log(`   Latencia promedio: ${performanceStats.averageLatency}ms`);
        
        console.log('\n📁 DATOS CSV:');
        console.log(`   Usuarios cargados: ${csvStats.totalUsers}`);
        console.log(`   Índice actual: ${csvStats.currentIndex}`);
        console.log(`   Archivo: ${csvStats.csvPath}`);
    }

    showCSVInfo() {
        const csvStats = this.csvDataLoader.getStats();
        
        console.log('\n📁 INFORMACIÓN DE DATOS CSV');
        console.log('='.repeat(40));
        console.log(`📊 Total de usuarios: ${csvStats.totalUsers}`);
        console.log(`📍 Índice actual: ${csvStats.currentIndex}`);
        console.log(`📂 Archivo: ${csvStats.csvPath}`);
        console.log(`✅ Estado: ${this.csvDataLoader.hasData() ? 'Datos cargados' : 'Sin datos'}`);
        
        if (this.csvDataLoader.hasData()) {
            const sampleUser = this.csvDataLoader.getRandomUser();
            console.log('\n🔍 Usuario de ejemplo:');
            console.log(`   ID: ${sampleUser.id}`);
            console.log(`   Nombre: ${sampleUser.nombre}`);
            console.log(`   Correo: ${sampleUser.correo}`);
            console.log(`   DNI: ${sampleUser.dni}`);
            console.log(`   Teléfono: ${sampleUser.telefono}`);
        }
    }

    async testConnection() {
        console.log('\n🧪 PROBANDO CONEXIÓN');
        console.log('-'.repeat(20));

        try {
            const isHealthy = await this.rabbitmqClient.isHealthy();
            if (isHealthy) {
                console.log('✅ Conexión a RabbitMQ: OK');
            } else {
                console.log('❌ Conexión a RabbitMQ: ERROR');
            }

            // Probar envío de mensaje de prueba con datos CSV si están disponibles
            let testData;
            if (this.csvDataLoader.hasData()) {
                testData = this.csvDataLoader.getRandomUser();
                testData.amigosFrecuentes = this.csvDataLoader.generateRandomFriends(testData.id);
                console.log('🔄 Usando datos de CSV para prueba...');
            } else {
                testData = {
                    nombre: 'Usuario Test',
                    correo: 'test@example.com',
                    clave: '1234',
                    dni: '12345678',
                    telefono: '987654321',
                    amigosFrecuentes: []
                };
                console.log('🔄 Usando datos de prueba generados...');
            }

            console.log('🔄 Enviando mensaje de prueba...');
            const requestId = await this.sendRegistrationRequest(testData);
            console.log(`✅ Mensaje de prueba enviado: ${requestId}`);

        } catch (error) {
            console.error('❌ Error en prueba de conexión:', error.message);
        }
    }

    async sendRegistrationRequest(userData) {
        const requestId = uuidv4();
        const timestamp = Date.now();

        const message = {
            request_id: requestId,
            timestamp: timestamp,
            ...userData
        };

        // Registrar solicitud pendiente
        this.pendingRequests.set(requestId, {
            startTime: timestamp,
            userData: userData
        });

        // Enviar mensaje
        await this.rabbitmqClient.publishMessage(
            'registro_bus',
            'lp3.signup',
            message
        );

        console.log(`📤 Solicitud enviada - ID: ${requestId} - DNI: ${userData.dni}`);
        return requestId;
    }

    handleAckMessage(message) {
        try {
            const data = JSON.parse(message);
            const requestId = data.request_id;
            const endTime = Date.now();

            if (this.pendingRequests.has(requestId)) {
                const request = this.pendingRequests.get(requestId);
                const latency = endTime - request.startTime;

                // Registrar métricas
                this.performanceAnalyzer.recordRequest(latency, data.valid || data.persisted);

                // Log resultado
                const status = (data.valid || data.persisted) ? '✅ ÉXITO' : '❌ FALLO';
                console.log(`📨 ${status} - DNI: ${request.userData.dni} - Latencia: ${latency}ms - ${data.message || 'Sin mensaje'}`);

                // Limpiar solicitud pendiente
                this.pendingRequests.delete(requestId);
            }

        } catch (error) {
            console.error('❌ Error procesando ACK:', error.message);
        }
    }

    async stop() {
        console.log('🛑 Cerrando LP3 Cliente...');
        
        if (this.rabbitmqClient) {
            await this.rabbitmqClient.disconnect();
        }
        
        console.log('✅ LP3 Cliente cerrado');
    }
}

// Función principal
async function main() {
    const client = new LP3Client();
    
    // Manejar señales de cierre
    process.on('SIGINT', async () => {
        console.log('\n🛑 Recibida señal de interrupción...');
        await client.stop();
        process.exit(0);
    });

    process.on('SIGTERM', async () => {
        console.log('\n🛑 Recibida señal de terminación...');
        await client.stop();
        process.exit(0);
    });

    // Iniciar cliente
    await client.start();
}

// Ejecutar si es el archivo principal
if (import.meta.url === `file://${process.argv[1]}`) {
    main().catch(error => {
        console.error('❌ Error fatal:', error);
        process.exit(1);
    });
}

export default LP3Client;
