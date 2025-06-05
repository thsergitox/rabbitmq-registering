/**
 * Cargador de datos CSV para usuarios
 */

import fs from 'fs/promises';
import path from 'path';

class CSVDataLoader {
    constructor() {
        this.userData = [];
        this.csvPath = path.join(process.cwd(), 'user.csv');
        this.currentIndex = 0;
    }

    async loadCSVData() {
        try {
            console.log('📁 Cargando datos de usuarios desde user.csv...');
            
            const csvContent = await fs.readFile(this.csvPath, 'utf-8');
            const lines = csvContent.trim().split('\n');
            
            this.userData = lines
                .filter(line => line.trim() !== '') // Filtrar líneas vacías
                .map(line => {
                    const [id, nombre, correo, clave, dni, telefono, timestamp] = line.split(',');
                    
                    // Formatear DNI para asegurar 8 dígitos
                    const formattedDNI = dni.padStart(8, '0');
                    
                    return {
                        id: parseInt(id),
                        nombre: nombre.trim(),
                        correo: correo.trim(),
                        clave: clave.trim(),
                        dni: formattedDNI,
                        telefono: telefono.trim(),
                        timestamp: timestamp ? timestamp.trim() : null,
                        amigosFrecuentes: [] // Campo vacío por defecto
                    };
                })
                .filter(user => user.id && user.nombre && user.correo && user.dni); // Filtrar registros inválidos

            console.log(`✅ Cargados ${this.userData.length} usuarios desde CSV`);
            return true;

        } catch (error) {
            console.error('❌ Error cargando datos CSV:', error.message);
            console.log('💡 Asegúrate de que existe el archivo user.csv en el directorio node/');
            return false;
        }
    }

    /**
     * Obtiene el siguiente usuario de la lista
     */
    getNextUser() {
        if (this.userData.length === 0) {
            return null;
        }

        const user = this.userData[this.currentIndex];
        this.currentIndex = (this.currentIndex + 1) % this.userData.length;
        
        return user;
    }

    /**
     * Obtiene un usuario específico por índice
     */
    getUser(index) {
        if (index >= 0 && index < this.userData.length) {
            return this.userData[index];
        }
        return null;
    }

    /**
     * Obtiene un conjunto de usuarios para simulación
     */
    getUsersForSimulation(count) {
        if (this.userData.length === 0) {
            return [];
        }

        const users = [];
        for (let i = 0; i < count; i++) {
            users.push(this.getNextUser());
        }
        
        return users;
    }

    /**
     * Obtiene usuario aleatorio
     */
    getRandomUser() {
        if (this.userData.length === 0) {
            return null;
        }

        const randomIndex = Math.floor(Math.random() * this.userData.length);
        return this.userData[randomIndex];
    }

    /**
     * Genera datos de amigos frecuentes aleatorios
     */
    generateRandomFriends(excludeId) {
        const friendsCount = Math.floor(Math.random() * 4); // 0-3 amigos
        const friends = [];
        
        for (let i = 0; i < friendsCount; i++) {
            const randomUser = this.getRandomUser();
            if (randomUser && randomUser.id !== excludeId && !friends.includes(randomUser.id)) {
                friends.push(randomUser.id);
            }
        }
        
        return friends;
    }

    /**
     * Obtiene estadísticas de los datos cargados
     */
    getStats() {
        return {
            totalUsers: this.userData.length,
            currentIndex: this.currentIndex,
            csvPath: this.csvPath
        };
    }

    /**
     * Reinicia el índice de usuarios
     */
    reset() {
        this.currentIndex = 0;
    }

    /**
     * Verifica si hay datos cargados
     */
    hasData() {
        return this.userData.length > 0;
    }
}

export default CSVDataLoader; 