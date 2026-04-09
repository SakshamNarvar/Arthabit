import os
import logging
from tenacity import retry, wait_exponential, stop_after_attempt, retry_if_exception_type
from langchain_core.prompts import ChatPromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI
from app.service.Expense import Expense
from dotenv import load_dotenv

logger = logging.getLogger(__name__)

class LLMService:
    def __init__(self):
        load_dotenv()
        self.prompt = ChatPromptTemplate.from_messages(
        [
            (
                "system",
                "You are an expert extraction algorithm. "
                "Only extract relevant information from the text. "
                "If you do not know the value of an attribute asked to extract, "
                "return null for the attribute's value.",
            ),
            ("human", "{text}")
        ]
        )
        self.apiKey = os.getenv('GOOGLE_API_KEY')
        if not self.apiKey:
            logger.warning("GOOGLE_API_KEY environment variable is not set.")

        try:
            self.llm = ChatGoogleGenerativeAI(api_key=self.apiKey, model="gemini-flash-lite-latest", temperature=0)
            self.runnable = self.prompt | self.llm.with_structured_output(schema=Expense)
            logger.info("LLM initialized successfully.")
        except Exception as e:
            logger.error(f"Failed to initialize LLM: {e}")
            self.runnable = None

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10), reraise=True)
    def _invoke_llm(self, message):
        logger.info("Invoking LLM for message extraction.")
        return self.runnable.invoke({"text":message})

    def runLLM(self, message):
        if not self.runnable:
            logger.error("LLM pipeline is not active. Attempted to process message.")
            return None
            
        try:
            return self._invoke_llm(message)
        except Exception as e:
            logger.error(f"Error during LLM invocation after retries: {e}")
            return None