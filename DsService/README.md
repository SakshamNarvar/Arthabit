# DS Service (Data Science Service)

The **DsService** is a Python Flask microservice that acts as the "smart parser" for out Expense Tracker App. It receives unstructured bank SMS messages, uses an LLM (Google Gemini) to extract structured expense details (amount, merchant, currency), and publishes the data to a Kafka topic for further processing. The service includes robust error handling, Kafka connection retries, and comprehensive structured logging for observability.

## Architecture & Data Flow

1. **API Gateway / Client** sends an SMS payload to `POST /v1/ds/message`.
2. **Filter (Regex)**: The service first checks if the message is a bank SMS using keyword matching. If not, it skips LLM processing to save costs.
3. **LLM Extraction**: If valid, the SMS is sent to Google Gemini via LangChain, which extracts the data into a defined `Expense` schema.
4. **Message Broker**: The structured expense data (with the attached `user_id`) is published to the `expense_service` Kafka topic. The producer includes automatic retries and publication timeouts for reliability.
5. Downstream services consume this Kafka topic to save the expense to a database.

## Prerequisites

- Python 3.11+
- Kafka running locally or remotely (default: localhost:9092)
- Google Gemini API Key

## Setup & Execution

### 1. Environment Setup
Create a virtual environment and install dependencies:
```bash
python3 -m venv dsenv
source dsenv/bin/activate
pip install -r requirements.txt
pip install -e .
```

### 2. Configuration
Create a `.env` file in the root directory and add your keys/config:
```env
GOOGLE_API_KEY=your_gemini_api_key_here
KAFKA_HOST=localhost
KAFKA_PORT=9092
```

### 3. Run the Service
Start the Flask development server:
```bash
python src/app/__init__.py
```

### 4. Run with Docker
Alternatively, you can build and run the service using Docker:
```bash
docker build -t ds-service .
docker run -p 8010:8010 --env-file .env ds-service
```

## API Endpoints

### `POST /v1/ds/message`
Processes an SMS, extracts expense details, and publishes to Kafka.
**Headers:** `x-user-id: <string>` (Required)
**Body:**
```json
{
  "message": "Rs 500 spent on Amazon using HDFC card"
}
```
**Response (200 OK):**
```json
{
  "amount": "500",
  "merchant": "Amazon",
  "currency": "Rs",
  "user_id": "user123"
}
```

**Error Responses:**
- `400 Bad Request`: Missing `x-user-id` header or invalid message format.
- `500 Internal Server Error`: Failed to process message internally (e.g., LLM pipeline failure or Kafka exceptions).
- `503 Service Unavailable`: Message queue (Kafka) is unavailable or failed to initialize.

### Other Endpoints
- `GET /`: Sanity check (Returns "Hello world")
- `GET /health`: Health probe (Returns "OK")

## Project Structure

- `src/app/__init__.py`: Flask app entry point, routes, and Kafka producer.
- `src/app/service/messageService.py`: Orchestrator linking the filter and LLM service.
- `src/app/utils/messagesUtil.py`: Regex-based filter to validate bank SMS messages.
- `src/app/service/llmService.py`: LangChain Google Gemini integration for data extraction.
- `src/app/service/Expense.py`: Pydantic schema for the expense extraction model.
