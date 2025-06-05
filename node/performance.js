/**
 * Analizador de Desempeño para LP3
 * Maneja pruebas de carga y recolección de métricas
 */

class PerformanceAnalyzer {
    constructor() {
        this.reset();
    }

    reset() {
        this.metrics = {
            totalRequests: 0,
            successfulRequests: 0,
            failedRequests: 0,
            latencies: [],
            startTime: null,
            endTime: null,
            requestTimes: []
        };
    }

    recordRequest(latency, success = true) {
        this.metrics.totalRequests++;
        this.metrics.latencies.push(latency);
        this.metrics.requestTimes.push(Date.now());

        if (success) {
            this.metrics.successfulRequests++;
        } else {
            this.metrics.failedRequests++;
        }
    }

    getStats() {
        const {
            totalRequests,
            successfulRequests,
            failedRequests,
            latencies,
            startTime,
            endTime,
            requestTimes
        } = this.metrics;

        // Calcular estadísticas básicas
        const successRate = totalRequests > 0 ? 
            Math.round((successfulRequests / totalRequests) * 100) : 0;

        // Estadísticas de latencia
        let averageLatency = 0;
        let minLatency = 0;
        let maxLatency = 0;

        if (latencies.length > 0) {
            averageLatency = Math.round(
                latencies.reduce((sum, lat) => sum + lat, 0) / latencies.length
            );
            minLatency = Math.min(...latencies);
            maxLatency = Math.max(...latencies);
        }

        // Calcular throughput (solicitudes por segundo)
        let throughput = 0;
        let totalTime = 0;

        if (startTime && endTime) {
            totalTime = endTime - startTime;
            throughput = totalTime > 0 ? 
                Math.round((totalRequests * 1000) / totalTime) : 0;
        } else if (requestTimes.length > 1) {
            // Calcular basado en tiempos de solicitudes individuales
            const minTime = Math.min(...requestTimes);
            const maxTime = Math.max(...requestTimes);
            totalTime = maxTime - minTime;
            throughput = totalTime > 0 ? 
                Math.round((totalRequests * 1000) / totalTime) : 0;
        }

        return {
            totalRequests,
            successfulRequests,
            failedRequests,
            successRate,
            averageLatency,
            minLatency,
            maxLatency,
            throughput,
            totalTime,
            
            // Estadísticas adicionales
            p50Latency: this.calculatePercentile(latencies, 50),
            p95Latency: this.calculatePercentile(latencies, 95),
            p99Latency: this.calculatePercentile(latencies, 99)
        };
    }

    calculatePercentile(values, percentile) {
        if (values.length === 0) return 0;
        
        const sorted = [...values].sort((a, b) => a - b);
        const index = Math.ceil((percentile / 100) * sorted.length) - 1;
        return sorted[Math.max(0, index)] || 0;
    }

    async runLoadTest(totalRequests, concurrentLimit, requestFunction, progressCallback) {
        console.log(`🚀 Iniciando prueba de carga: ${totalRequests} solicitudes con ${concurrentLimit} concurrentes`);
        
        this.reset();
        this.metrics.startTime = Date.now();

        const tasks = [];
        let completed = 0;
        let successCount = 0;
        let errorCount = 0;

        // Crear semáforo para limitar concurrencia
        const semaphore = new Semaphore(concurrentLimit);

        for (let i = 0; i < totalRequests; i++) {
            const task = this.executeRequest(
                semaphore,
                requestFunction,
                this.generateRandomUserData()
            ).then((result) => {
                completed++;
                if (result.success) {
                    successCount++;
                } else {
                    errorCount++;
                }

                // Llamar callback de progreso si existe
                if (progressCallback) {
                    progressCallback(completed, successCount, errorCount);
                }

                return result;
            }).catch((error) => {
                completed++;
                errorCount++;
                
                if (progressCallback) {
                    progressCallback(completed, successCount, errorCount);
                }

                return { success: false, error: error.message };
            });

            tasks.push(task);
        }

        // Esperar a que todas las solicitudes terminen
        const results = await Promise.all(tasks);
        
        this.metrics.endTime = Date.now();

        console.log(`✅ Prueba de carga completada en ${this.metrics.endTime - this.metrics.startTime}ms`);
        
        return results;
    }

    async executeRequest(semaphore, requestFunction, userData) {
        // Adquirir permiso del semáforo
        await semaphore.acquire();

        try {
            const startTime = Date.now();
            
            // Ejecutar la función de solicitud
            const requestId = await requestFunction(userData);
            
            // Por ahora, simulamos que todas las solicitudes son exitosas
            // En un escenario real, esperaríamos la respuesta ACK
            const endTime = Date.now();
            const latency = endTime - startTime;

            // Simular tiempo de respuesta variable (100ms - 2000ms)
            const simulatedLatency = Math.random() * 1900 + 100;
            await new Promise(resolve => setTimeout(resolve, Math.min(simulatedLatency, 100)));

            this.recordRequest(latency, true);

            return {
                success: true,
                requestId: requestId,
                latency: latency
            };

        } catch (error) {
            console.error('❌ Error en solicitud:', error.message);
            this.recordRequest(0, false);
            
            return {
                success: false,
                error: error.message
            };

        } finally {
            // Liberar permiso del semáforo
            semaphore.release();
        }
    }

    generateRandomUserData() {
        const nombres = [
            'Juan Carlos', 'María Elena', 'José Luis', 'Ana Sofía', 'Carlos Alberto',
            'Patricia Isabel', 'Miguel Ángel', 'Laura Patricia', 'Roberto Carlos', 'Carmen Rosa',
            'Fernando José', 'Sandra María', 'Alejandro', 'Mónica Elizabeth', 'Ricardo Manuel',
            'Claudia Esperanza', 'Daniel Eduardo', 'Gabriela', 'Andrés Felipe', 'Verónica'
        ];

        const apellidos = [
            'García López', 'Rodríguez Martínez', 'Hernández Silva', 'López Vargas', 'Martínez Cruz',
            'González Ruiz', 'Pérez Morales', 'Sánchez Torres', 'Ramírez Flores', 'Flores Herrera',
            'Rivera Campos', 'Torres Mendoza', 'Vargas Castillo', 'Castro Jiménez', 'Morales Ramos'
        ];

        const dominios = ['gmail.com', 'hotmail.com', 'yahoo.com', 'outlook.com', 'company.com'];

        // Generar datos aleatorios
        const nombre = nombres[Math.floor(Math.random() * nombres.length)];
        const apellido = apellidos[Math.floor(Math.random() * apellidos.length)];
        const nombreCompleto = `${nombre} ${apellido}`;
        
        // Email basado en el nombre
        const emailBase = nombre.toLowerCase().replace(/\s+/g, '.') + Math.floor(Math.random() * 1000);
        const dominio = dominios[Math.floor(Math.random() * dominios.length)];
        const correo = `${emailBase}@${dominio}`;

        // DNI aleatorio de 8 dígitos
        const dni = String(Math.floor(Math.random() * 90000000) + 10000000);

        // Teléfono aleatorio de 9 dígitos (empezando por 9)
        const telefono = '9' + String(Math.floor(Math.random() * 100000000)).padStart(8, '0');

        // Clave aleatoria de 4 dígitos
        const clave = String(Math.floor(Math.random() * 9000) + 1000);

        // Amigos frecuentes aleatorios (0-3 amigos)
        const numAmigos = Math.floor(Math.random() * 4);
        const amigosFrecuentes = [];
        for (let i = 0; i < numAmigos; i++) {
            // IDs de amigos entre 1 y 100
            const amigoId = Math.floor(Math.random() * 100) + 1;
            if (!amigosFrecuentes.includes(amigoId)) {
                amigosFrecuentes.push(amigoId);
            }
        }

        return {
            nombre: nombreCompleto,
            correo: correo,
            clave: clave,
            dni: dni,
            telefono: telefono,
            amigosFrecuentes: amigosFrecuentes
        };
    }

    exportMetrics() {
        const stats = this.getStats();
        const timestamp = new Date().toISOString();
        
        return {
            timestamp: timestamp,
            test_config: {
                total_requests: stats.totalRequests,
                concurrent_limit: 'variable'
            },
            results: stats,
            raw_data: {
                latencies: this.metrics.latencies.slice(), // Copia del array
                request_times: this.metrics.requestTimes.slice()
            }
        };
    }
}

/**
 * Clase Semáforo para controlar concurrencia
 */
class Semaphore {
    constructor(permits) {
        this.permits = permits;
        this.waiting = [];
    }

    async acquire() {
        if (this.permits > 0) {
            this.permits--;
            return Promise.resolve();
        }

        return new Promise(resolve => {
            this.waiting.push(resolve);
        });
    }

    release() {
        this.permits++;
        
        if (this.waiting.length > 0) {
            const resolve = this.waiting.shift();
            this.permits--;
            resolve();
        }
    }
}

export default PerformanceAnalyzer; 