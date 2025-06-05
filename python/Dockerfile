FROM python:3.11-slim

WORKDIR /app

# Install system dependencies that might be needed by some Python packages
# RUN apt-get update && apt-get install -y --no-install-recommends default-libmysqlclient-dev gcc && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY ./src /app/src

ENV PYTHONPATH="/app/src:${PYTHONPATH}"

# Ensure the main FastAPI app object is correctly referenced.
# If your main file is main.py (now in the WORKDIR /app/src) and app object is 'app', then main:app is correct.
CMD ["uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8080"]

EXPOSE 8080 