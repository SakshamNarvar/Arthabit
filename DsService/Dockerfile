FROM python:3.11-slim

WORKDIR /app

# 1. Copy requirements first to leverage Docker cache
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 2. Copy the rest of the source code
COPY . .

# 3. Set env vars and run
ENV FLASK_APP=src/app/__init__.py
EXPOSE 8010

# Use '0.0.0.0' so it is accessible outside the container
CMD ["flask", "run", "--host=0.0.0.0", "--port=8010"]