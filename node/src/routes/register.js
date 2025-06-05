import { Router } from 'express';
import { body, validationResult } from 'express-validator';
import { rabbitmqService } from '../services/rabbitmq.js';
import { apiLogger as logger } from '../utils/logger.js';

const router = Router();

// Validation middleware
const validateRegistration = [
  body('nombre').notEmpty().trim().withMessage('Name is required'),
  body('correo').isEmail().normalizeEmail().withMessage('Valid email is required'),
  body('clave').isInt({ min: 100000, max: 999999 }).withMessage('Password must be 6 digits'),
  body('dni').isInt({ min: 10000000, max: 99999999 }).withMessage('DNI must be 8 digits'),
  body('telefono').isInt({ min: 100000000, max: 999999999 }).withMessage('Phone must be 9 digits'),
  body('friendsDni').optional().isArray().withMessage('Friends DNI must be an array'),
  body('friendsDni.*').optional().isInt({ min: 10000000, max: 99999999 }).withMessage('Friend DNI must be 8 digits'),
];

// POST /api/register - Register a new user
router.post('/register', validateRegistration, async (req, res) => {
  try {
    // Check validation errors
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        success: false,
        errors: errors.array(),
      });
    }

    const { nombre, correo, clave, dni, telefono, friendsDni = [] } = req.body;

    logger.info(`Processing registration for DNI: ${dni}`);

    // Check if RabbitMQ is connected
    if (!rabbitmqService.isConnected()) {
      return res.status(503).json({
        success: false,
        error: 'Service temporarily unavailable',
      });
    }

    // Publish registration request and wait for response
    const registrationData = {
      nombre,
      correo,
      clave: parseInt(clave),
      dni: parseInt(dni),
      telefono: parseInt(telefono),
      friendsDni: friendsDni.map(d => parseInt(d)),
    };

    const response = await rabbitmqService.publishRegistration(registrationData);

    // Determine success based on response status
    const isSuccess = response.status === 'SUCCESS';
    const statusCode = isSuccess ? 201 : 400;

    logger.info(`Registration ${isSuccess ? 'successful' : 'failed'} for DNI: ${dni}`, response);

    res.status(statusCode).json({
      success: isSuccess,
      data: response,
      timestamp: new Date().toISOString(),
    });

  } catch (error) {
    logger.error('Registration error:', error);

    if (error.message === 'Response timeout') {
      return res.status(504).json({
        success: false,
        error: 'Request timeout - please try again',
      });
    }

    res.status(500).json({
      success: false,
      error: 'Internal server error',
    });
  }
});

// GET /api/register/status - Check registration service status
router.get('/register/status', (req, res) => {
  const isConnected = rabbitmqService.isConnected();
  
  res.json({
    service: 'registration',
    status: isConnected ? 'operational' : 'degraded',
    rabbitmq: isConnected ? 'connected' : 'disconnected',
    timestamp: new Date().toISOString(),
  });
});

export default router; 