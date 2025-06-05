"""Database configuration and SQLAlchemy setup."""

from sqlalchemy import create_engine, text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import logging

logger = logging.getLogger(__name__)

Base = declarative_base()


class DatabaseConnection:
    """Manages database connection and sessions."""

    def __init__(self, config):
        """Initialize database connection."""
        self.config = config
        self.engine = None
        self.SessionLocal = None

    def connect(self):
        """Create database engine and session factory."""
        try:
            self.engine = create_engine(
                self.config.SQLALCHEMY_DATABASE_URI,
                pool_size=self.config.SQLALCHEMY_POOL_SIZE,
                pool_timeout=self.config.SQLALCHEMY_POOL_TIMEOUT,
                pool_recycle=self.config.SQLALCHEMY_POOL_RECYCLE,
                echo=self.config.DEBUG,  # Log SQL queries in debug mode
            )

            # Test connection
            with self.engine.connect() as conn:
                conn.execute(text("SELECT 1"))

            self.SessionLocal = sessionmaker(
                autocommit=False, autoflush=False, bind=self.engine
            )

            logger.info("Successfully connected to database")

        except Exception as e:
            logger.error(f"Failed to connect to database: {e}")
            raise

    def get_session(self):
        """Get a database session."""
        if not self.SessionLocal:
            raise RuntimeError("Database not connected")
        return self.SessionLocal()

    def disconnect(self):
        """Close database connection."""
        if self.engine:
            self.engine.dispose()
            logger.info("Disconnected from database")
