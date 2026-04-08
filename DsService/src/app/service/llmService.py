import os
import logging
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

    def runLLM(self, message):
        if not self.runnable:
            logger.error("LLM pipeline is not active. Attempted to process message.")
            return None
            
        try:
            logger.info("Invoking LLM for message extraction.")
            return self.runnable.invoke({"text":message})
        except Exception as e:
            logger.error(f"Error during LLM invocation: {e}")
            return None