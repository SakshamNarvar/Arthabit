import os
from langchain_core.prompts import ChatPromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI
from app.service.Expense import Expense
from dotenv import load_dotenv

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

        self.llm = ChatGoogleGenerativeAI(api_key=self.apiKey, model="gemini-flash-lite-latest", temperature=0)
        
        self.runnable = self.prompt | self.llm.with_structured_output(schema=Expense)

    def runLLM(self, message):
        return self.runnable.invoke({"text":message})