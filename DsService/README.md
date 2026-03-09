# DS Service — Complete Workflow Explanation

> **Read this top-to-bottom.** It is written so you know exactly where to look first, what each piece does, and how data flows through the entire service.

---

## 1. What Is This Service? (The Big Picture)

The **DsService** (Data Science Service) is a **Python Flask microservice** that sits inside a larger **Expense Tracker App**. Its single job is:

> **Receive an SMS text → Decide if it's a bank transaction SMS → If yes, use an LLM (Google Gemini) to extract expense details (amount, merchant, currency) → Publish the structured data to a Kafka topic so another service can save it.**

Think of it as the "smart parser" in the system — it turns messy, unstructured bank SMS messages into clean, structured expense data.

---

## 2. Where to Start Reading (Recommended Order)

| Order | File | Why Read It |
|-------|------|-------------|
| 1 | `src/app/__init__.py` | **Entry point.** The Flask app, routes, Kafka producer, and the request/response lifecycle all live here. Start here. |
| 2 | `src/app/service/messageService.py` | **Orchestrator.** The route calls this. It decides whether to process the message and delegates to sub-components. |
| 3 | `src/app/utils/messagesUtil.py` | **Filter/Guard.** Checks if a message looks like a bank SMS before wasting an LLM call. |
| 4 | `src/app/service/llmService.py` | **AI Brain.** Sends the bank SMS text to Google Gemini and gets back structured expense data. |
| 5 | `src/app/service/Expense.py` | **Data Model.** Defines what an "expense" looks like (amount, merchant, currency). |
| 6 | `setup.py` / `requirements.txt` | **Dependencies.** What libraries this service needs. |
| 7 | `Dockerfile` | **Deployment.** How this gets containerized. |
| 8 | `src/app/config.py` | Currently empty — placeholder for future Flask config. |

---

## 3. Full Data Flow (Step by Step)

Here is the exact journey of a single request, from the moment it arrives to the moment it leaves:

```
┌──────────────────────────────────────────────────────────────────────────┐
│  EXTERNAL CALLER (e.g., Android app / another microservice)             │
│  POST /v1/ds/message                                                    │
│  Headers: { "x-user-id": "user123" }                                   │
│  Body:    { "message": "Rs 500 spent on Amazon using HDFC card" }       │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  STEP 1: __init__.py → handle_message()                                 │
│                                                                         │
│  • Reads x-user-id from request headers                                 │
│  • Reads "message" from JSON body                                       │
│  • Calls messageService.process_message(message)                        │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  STEP 2: messageService.py → process_message(message)                   │
│                                                                         │
│  • Calls messagesUtil.isBankSms(message)                                │
│  • If FALSE → returns None (not a bank SMS, stop here)                  │
│  • If TRUE  → calls llmService.runLLM(message)                         │
└────────────┬─────────────────────────────────┬───────────────────────────┘
             │ (is bank SMS)                   │ (not bank SMS)
             ▼                                 ▼
┌────────────────────────────┐    ┌────────────────────────────────────────┐
│  STEP 3: messagesUtil.py   │    │  Back to __init__.py                   │
│  isBankSms() returned True │    │  result is None → return 400 error     │
│                             │    │  {"error": "Invalid message format"}   │
└────────────┬───────────────┘    └────────────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  STEP 4: llmService.py → runLLM(message)                               │
│                                                                         │
│  • Sends the SMS text to Google Gemini (gemini-2.5-flash-lite)          │
│  • Uses a system prompt telling the LLM to extract fields               │
│  • LLM returns structured output matching the Expense schema            │
│  • Returns an Expense object: { amount, merchant, currency }            │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  STEP 5: Back in __init__.py                                            │
│                                                                         │
│  • Calls result.serialize() to get a plain dict                         │
│  • Attaches user_id to the dict                                         │
│  • Publishes to Kafka topic "expense_service"                           │
│  • Returns the serialized result as JSON to the caller                  │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  KAFKA TOPIC: "expense_service"                                         │
│                                                                         │
│  Message: { "amount": "500", "merchant": "Amazon",                      │
│             "currency": "Rs", "user_id": "user123" }                    │
│                                                                         │
│  → Another microservice (likely a Java/Spring "Expense Service")        │
│    consumes this and saves it to a database.                            │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 4. File-by-File Deep Dive

### 4.1 `src/app/__init__.py` — The Entry Point & API Layer

**What it does:**
- Creates the Flask application
- Initializes a `KafkaProducer` (connects to Kafka on startup)
- Instantiates `MessageService` (which in turn initializes the LLM)
- Defines three HTTP routes:
  - `POST /v1/ds/message` — The main endpoint. Receives SMS text, processes it, publishes to Kafka.
  - `GET /` — Simple "Hello World" for quick verification.
  - `GET /health` — Health check (used by Docker/Kubernetes probes).

**Key decisions made here:**
- The `x-user-id` header is required — this identifies which user the expense belongs to. The DS Service doesn't manage users; it trusts the caller to provide this.
- Kafka serialization happens via `json.dumps().encode('utf-8')` — all messages are published as JSON bytes.
- If `messageService.process_message()` returns `None`, a 400 error is returned.

**What happens if this file didn't exist?**
Nothing works. There is no API, no server, no entry point. This IS the application.

---

### 4.2 `src/app/service/messageService.py` — The Orchestrator

**What it does:**
- Acts as a **coordinator** between the utility layer (is this a bank SMS?) and the AI layer (extract expense data).
- Has one method: `process_message(message)`.
- Logic: `if isBankSms → call LLM`, else → `return None`.

**Why it exists / Why it's necessary:**
- **Separation of concerns.** The Flask route shouldn't know about SMS validation or LLM invocation. The `MessageService` encapsulates that workflow.
- **Cost savings.** LLM API calls cost money. By checking `isBankSms()` first, it avoids sending irrelevant texts (like "Hey, dinner tonight?") to the LLM.

**What happens if this file didn't exist?**
You'd have to put SMS filtering logic AND LLM calling logic directly in the Flask route handler (`__init__.py`). For a single endpoint that's technically possible, but it violates separation of concerns and makes the code harder to test and maintain.

---

### 4.3 `src/app/utils/messagesUtil.py` — The SMS Filter / Guard

**What it does:**
- Has one method: `isBankSms(message)`.
- Uses a simple **regex keyword search** for the words: `"spent"`, `"bank"`, `"card"`.
- If ANY of these words appear in the message (case-insensitive), it returns `True`.

**Example:**
```
"Rs 500 spent on Amazon using HDFC card" → True  (contains "spent" and "card")
"Hey, are you free tonight?"              → False (no matching keywords)
"Your bank account is debited"            → True  (contains "bank")
```

**Why it exists / Why it's necessary:**
- **Gatekeeper.** Prevents non-financial messages from reaching the LLM.
- **Cost control.** Every LLM call costs tokens/money. This simple regex check is essentially free.
- **Accuracy.** The LLM is prompted to extract expense data — if you send it "dinner at 8pm?", it might hallucinate amounts or merchants. Better to not call it at all.

**What happens if this file didn't exist?**
Every single message — including personal texts, OTP messages, promotional spam — would be sent to the LLM. This means:
1. Higher API costs (unnecessary LLM calls).
2. Garbage output (the LLM might "extract" fake expenses from non-financial texts).
3. Bad data polluting the Kafka topic and eventually the database.

---

### 4.4 `src/app/service/llmService.py` — The AI Brain

**What it does:**
- Initializes a connection to **Google Gemini** (`gemini-2.5-flash-lite`) using LangChain.
- Constructs a **prompt template** with a system message telling the LLM: _"You are an expert extraction algorithm. Only extract relevant information. Return null if unknown."_
- Uses LangChain's `with_structured_output(schema=Expense)` to force the LLM to return data matching the `Expense` Pydantic model.
- Has one method: `runLLM(message)` — sends the SMS text through the prompt → LLM → structured output pipeline.

**The LangChain pipeline (`self.runnable`):**
```
ChatPromptTemplate  →  ChatGoogleGenerativeAI.with_structured_output(Expense)
       ↓                                ↓
 Formats the text             Calls Google Gemini API,
 into system + human           forces output to match
 message pair                  Expense(amount, merchant, currency)
```

**Why it exists / Why it's necessary:**
- This is the **core intelligence** of the service. Without it, you'd have to write complex regex or NLP rules to parse wildly varying bank SMS formats.
- The LLM handles all the variability — different banks, different currencies, different formats — in a single call.
- `with_structured_output` guarantees the response is a valid `Expense` object, not free-form text.

**What happens if this file didn't exist?**
You'd have no way to extract structured expense data from free-text SMS messages. You'd need to write manual parsers for every bank's SMS format, which is brittle and unmaintainable.

---

### 4.5 `src/app/service/Expense.py` — The Data Model

**What it does:**
- Defines the `Expense` Pydantic model with three fields:
  - `amount` — The transaction amount (e.g., "500")
  - `merchant` — Who was paid (e.g., "Amazon")
  - `currency` — The currency (e.g., "Rs", "USD")
- All fields are `Optional[str]` — the LLM might not find all values.
- Has a `serialize()` method that converts the model to a plain Python dict for Kafka publishing.

**Why it exists / Why it's necessary:**
- **Schema definition for the LLM.** LangChain's `with_structured_output(schema=Expense)` reads this model's fields and descriptions to tell Gemini WHAT to extract. Without it, the LLM wouldn't know what fields you want.
- **Data contract.** It defines the shape of data flowing into Kafka. Downstream consumers (the Expense Service) expect this exact structure.
- **Validation.** Being a Pydantic model, it automatically validates types.

**What happens if this file didn't exist?**
- The LLM would have no schema to extract into — `with_structured_output()` would fail.
- There'd be no agreed-upon data format between this service and downstream consumers.
- Serialization to Kafka would be ad-hoc and error-prone.

---

### 4.6 `src/app/config.py` — Flask Configuration (Currently Empty)

**What it does:**
- Currently nothing. It's loaded by `app.config.from_pyfile('config.py')` in `__init__.py`.
- Placeholder for future Flask configuration (e.g., debug mode, secret keys, database URIs).

**What happens if this file didn't exist?**
- The `app.config.from_pyfile('config.py')` call would throw a `FileNotFoundError` and crash the app on startup.
- Even though it's empty, it must exist for the Flask config loading to succeed.

---

### 4.7 `setup.py` — Package Definition

**What it does:**
- Defines the Python package metadata and its dependencies.
- Allows installation via `pip install -e .` (editable/dev mode).
- Key dependencies: Flask, kafka-python, langchain-core, langchain-google-genai, pydantic, python-dotenv, gunicorn.

**What happens if this file didn't exist?**
- You couldn't install the project as a Python package.
- The `from app.service.xxx import xxx` imports could fail depending on how you run the code.
- No standardized way to declare or install dependencies.

---

### 4.8 `Dockerfile` — Container Deployment

**What it does:**
- Builds a Docker image from `python:3.11-slim`.
- Installs dependencies from `requirements.txt`.
- Sets `FLASK_APP=src/app/__init__.py` so Flask knows where the app is.
- Exposes port 8010 and runs the Flask dev server.

**What happens if this file didn't exist?**
- You couldn't containerize or deploy this service in Docker/Kubernetes.
- You'd have to run it manually with `python -m flask run` or `gunicorn`.

---

## 5. Dependency Map (Who Uses What)

```
__init__.py (Flask App)
    ├── MessageService (from messageService.py)
    │       ├── MessagesUtil (from messagesUtil.py)
    │       │       └── [regex matching — no external deps]
    │       └── LLMService (from llmService.py)
    │               ├── ChatGoogleGenerativeAI (Google Gemini via LangChain)
    │               ├── ChatPromptTemplate (LangChain prompt builder)
    │               └── Expense (from Expense.py — Pydantic model)
    │                       └── [defines schema: amount, merchant, currency]
    └── KafkaProducer (from kafka-python)
            └── [publishes to "expense_service" topic]
```

---

## 6. Environment Variables

| Variable | Where Used | Purpose | Default |
|----------|-----------|---------|---------|
| `KAFKA_HOST` | `__init__.py` | Kafka broker hostname | `localhost` |
| `KAFKA_PORT` | `__init__.py` | Kafka broker port | `9092` |
| `GOOGLE_API_KEY` | `llmService.py` (via `.env` file) | API key to call Google Gemini | None (required) |

---

## 7. API Endpoints

| Method | Path | Purpose | Request | Response |
|--------|------|---------|---------|----------|
| `POST` | `/v1/ds/message` | Process an SMS, extract expense, publish to Kafka | Header: `x-user-id`, Body: `{"message": "..."}` | `{"amount": "...", "merchant": "...", "currency": "...", "user_id": "..."}` |
| `GET` | `/` | Sanity check | — | `"Hello world"` |
| `GET` | `/health` | Health probe | — | `"OK"` |

---

## 8. How This Service Fits in the Larger System

```
┌──────────────────┐     POST /v1/ds/message     ┌──────────────────┐
│                  │ ──────────────────────────▶  │                  │
│  Mobile App /    │                              │   DS Service     │
│  API Gateway     │  ◀──────────────────────────  │   (this service) │
│                  │     JSON response             │                  │
└──────────────────┘                              └────────┬─────────┘
                                                           │
                                                  Kafka publish to
                                                  "expense_service"
                                                           │
                                                           ▼
                                                  ┌──────────────────┐
                                                  │  Expense Service  │
                                                  │  (Java/Spring?)   │
                                                  │                  │
                                                  │  Consumes from   │
                                                  │  Kafka, saves    │
                                                  │  to database     │
                                                  └──────────────────┘
```

---

## 9. Key Concepts to Understand

### Why Kafka and not a direct HTTP call to the Expense Service?
- **Decoupling.** The DS Service doesn't need to know where or how the Expense Service runs. It just publishes to a topic.
- **Resilience.** If the Expense Service is down, messages queue in Kafka and get processed when it comes back.
- **Scalability.** Multiple consumers can read from the same topic.

### Why use an LLM instead of regex for parsing?
- Bank SMS formats vary wildly across banks and countries. Writing regex for every format is nearly impossible to maintain.
- The LLM generalizes — it understands natural language and can extract fields from formats it has never seen before.
- `with_structured_output` ensures the response is always a clean `Expense` object, not free-form text.

### Why Pydantic?
- Pydantic provides runtime type validation. If the LLM returns garbage, Pydantic will catch it.
- LangChain uses Pydantic models to generate the schema it sends to the LLM, telling it exactly what fields to extract.

---

## 10. Quick Start (How to Run Locally)

```bash
# 1. Activate virtual environment
source dsenv/bin/activate

# 2. Install in dev mode
pip install -e .

# 3. Create a .env file with your Google API key
echo "GOOGLE_API_KEY=your-key-here" > .env

# 4. Make sure Kafka is running on localhost:9092

# 5. Run the Flask app
flask --app src/app run --port 8010

# 6. Test it
curl -X POST http://localhost:8010/v1/ds/message \
  -H "Content-Type: application/json" \
  -H "x-user-id: user123" \
  -d '{"message": "Rs 500 spent at Amazon using HDFC credit card"}'
```

---

## 11. Summary (TL;DR)

| Component | One-Line Summary |
|-----------|-----------------|
| `__init__.py` | Flask app + routes + Kafka producer — the glue that holds everything together |
| `messageService.py` | Orchestrates: "Is it a bank SMS? If yes, call the LLM." |
| `messagesUtil.py` | Simple keyword regex to filter bank SMS from non-bank messages |
| `llmService.py` | Sends text to Google Gemini, gets back structured expense data |
| `Expense.py` | Pydantic model defining the shape of an expense (amount, merchant, currency) |
| `config.py` | Empty placeholder for Flask config (must exist or app crashes) |
| `setup.py` | Python package definition + dependency list |
| `Dockerfile` | Container packaging for deployment |
