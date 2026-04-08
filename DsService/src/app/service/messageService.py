from app.utils.messagesUtil import MessagesUtil
from app.service.llmService import LLMService
import logging

logger = logging.getLogger(__name__)

class MessageService:
    def __init__(self):
        self.messageUtil = MessagesUtil()
        self.llmService = LLMService()
    
    def process_message(self, message):
        logger.debug("Received message for processing.")
        if self.messageUtil.isBankSms(message):
            logger.info("Message identified as a bank SMS. Invoking LLM service.")
            return self.llmService.runLLM(message)
        else:
            logger.info("Message is not a bank SMS. Ignored.")
            return None