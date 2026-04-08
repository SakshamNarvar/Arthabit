from flask import Flask
from flask import request, jsonify
from app.service.messageService import MessageService
from kafka import KafkaProducer
from kafka.errors import KafkaError
import json
import os
import time
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

messageService = MessageService()
kafka_host = os.getenv('KAFKA_HOST', 'localhost')
kafka_port = os.getenv('KAFKA_PORT', '9092')
kafka_bootstrap_servers = f"{kafka_host}:{kafka_port}"
print("Kafka server is "+kafka_bootstrap_servers)
print("\n")

def init_kafka_producer(retries=5, delay=5):
    for attempt in range(retries):
        try:
            producer = KafkaProducer(
                bootstrap_servers=kafka_bootstrap_servers,
                value_serializer=lambda v: json.dumps(v).encode('utf-8')
            )
            logger.info("Successfully connected to Kafka.")
            return producer
        except Exception as e:
            logger.warning(f"Failed to connect to Kafka (attempt {attempt + 1}/{retries}): {e}")
            if attempt < retries - 1:
                time.sleep(delay)
    
    logger.error("Could not connect to Kafka after multiple attempts.")
    return None

producer = init_kafka_producer()

@app.route('/v1/ds/message', methods=['POST'])
def handle_message():
    user_id = request.headers.get('x-user-id')
    if not user_id:
        return jsonify({'error': 'x-user-id header is required'}), 400

    message = request.json.get('message')
    result = messageService.process_message(message)

    if result is not None:
        serialized_result = result.serialize()
        serialized_result['user_id'] = user_id
        
        if producer is None:
            logger.error("Kafka producer is not initialized.")
            return jsonify({'error': 'Message queue unavailable'}), 503

        try:
            future = producer.send('expense_service', serialized_result)
            # block for up to 10 seconds to ensure the message was published
            future.get(timeout=10)
            return jsonify(serialized_result)
        except KafkaError as e:
            logger.error(f"Failed to send message to Kafka: {e}")
            return jsonify({'error': 'Failed to process message internally'}), 500
        except Exception as e:
            logger.error(f"Unexpected error when sending to Kafka: {e}")
            return jsonify({'error': 'Failed to process message internally'}), 500
    else:
        return jsonify({'error': 'Invalid message format'}), 400


@app.route('/health', methods=['GET'])
def health_check():
    return 'OK'


if __name__ == "__main__":
    app.run(host="localhost", port= 8010 ,debug=True)