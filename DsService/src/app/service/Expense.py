from typing import Optional
from pydantic import BaseModel, Field

class Expense(BaseModel):
        """Information about a transaction made on any Card"""
        amount: Optional[str] = Field(title="expense", description="Expense made on the transaction")
        merchant: Optional[str] = Field(title="merchant", description="Merchant name to whom the transaction has been made")
        currency: Optional[str] = Field(title="currency", description="Currency of the transaction (only allowed values = INR, USD, otherwise null)")
        fund_source: Optional[str] = Field(title="fund_source", description="Fund source of the transaction like SBI Card, ICICI Bank, otherwise null)")

        def serialize(self):
                return {
                        "amount": self.amount,
                        "merchant": self.merchant,
                        "currency": self.currency,
                        "fund_source": self.fund_source
                }