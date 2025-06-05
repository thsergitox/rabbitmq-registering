"""Database configuration and SQLAlchemy setup."""

from sqlalchemy import create_engine, text, event
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, scoped_session
from sqlalchemy.pool import QueuePool
import logging
import threading

logger = logging.getLogger(__name__)

Base = declarative_base()


class DatabaseConnection:
    """Manages database connection and sessions with thread safety."""

    def __init__(self, config):
        """Initialize database connection."""
        self.config = config
        self.engine = None
        self.SessionLocal = None
        self._lock = threading.Lock()

    def connect(self):
        """Create database engine and session factory with enhanced pooling."""
        try:
            # Enhanced connection pooling for high concurrency
            self.engine = create_engine(
                self.config.SQLALCHEMY_DATABASE_URI,
                poolclass=QueuePool,
                pool_size=20,  # Increased for high concurrency
                max_overflow=40,  # Allow more overflow connections
                pool_timeout=30,  # Timeout for getting connection
                pool_recycle=3600,  # Recycle connections after 1 hour
                pool_pre_ping=True,  # Test connections before using
                echo=self.config.DEBUG,  # Log SQL queries in debug mode
                connect_args={
                    "connect_timeout": 10,
                    "read_timeout": 30,
                    "write_timeout": 30,
                    "charset": "utf8mb4",
                },
            )

            # Add pool event listeners for monitoring
            @event.listens_for(self.engine, "connect")
            def receive_connect(dbapi_connection, connection_record):
                connection_record.info["pid"] = threading.get_ident()
                logger.debug(
                    f"New connection created by thread {threading.get_ident()}"
                )

            @event.listens_for(self.engine, "checkout")
            def receive_checkout(dbapi_connection, connection_record, connection_proxy):
                pid = threading.get_ident()
                logger.debug(f"Connection checked out by thread {pid}")

            # Test connection
            with self.engine.connect() as conn:
                conn.execute(text("SELECT 1"))

            # Create thread-safe session factory
            session_factory = sessionmaker(
                autocommit=False,
                autoflush=False,
                bind=self.engine,
                expire_on_commit=False,  # Prevent lazy loading issues
            )

            # Use scoped session for thread safety
            self.SessionLocal = scoped_session(session_factory)

            logger.info(
                f"Successfully connected to database with pool size {self.engine.pool.size()}"
            )

        except Exception as e:
            logger.error(f"Failed to connect to database: {e}")
            raise

    def get_session(self):
        """Get a thread-safe database session."""
        if not self.SessionLocal:
            raise RuntimeError("Database not connected")
        return self.SessionLocal()

    def remove_session(self):
        """Remove the current thread's session."""
        if self.SessionLocal:
            self.SessionLocal.remove()

    def disconnect(self):
        """Close database connection."""
        with self._lock:
            if self.SessionLocal:
                self.SessionLocal.remove()
            if self.engine:
                self.engine.dispose()
                logger.info("Disconnected from database")

    def get_pool_status(self):
        """Get connection pool status for monitoring."""
        if self.engine and hasattr(self.engine.pool, "size"):
            pool = self.engine.pool
            return {
                "size": pool.size(),
                "checked_in": pool.checkedin(),
                "checked_out": pool.checkedout(),
                "overflow": pool.overflow(),
            }
        return None
