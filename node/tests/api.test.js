import { jest } from '@jest/globals';
import request from 'supertest';
import app from '../src/index.js';

describe('API Tests', () => {
  describe('GET /api/health', () => {
    it('should return health status', async () => {
      const response = await request(app).get('/api/health');
      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('status', 'ok');
      expect(response.body).toHaveProperty('service', 'registration-client');
    });
  });

  describe('POST /api/register', () => {
    it('should validate required fields', async () => {
      const response = await request(app)
        .post('/api/register')
        .send({});
      
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('success', false);
      expect(response.body).toHaveProperty('errors');
    });

    it('should validate email format', async () => {
      const response = await request(app)
        .post('/api/register')
        .send({
          nombre: 'Test User',
          correo: 'invalid-email',
          clave: 123456,
          dni: 12345678,
          telefono: 987654321,
        });
      
      expect(response.status).toBe(400);
      expect(response.body.errors[0].path).toBe('correo');
    });
  });
}); 