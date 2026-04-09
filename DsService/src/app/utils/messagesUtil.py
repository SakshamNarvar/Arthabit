import re

class MessagesUtil:
    _words_to_search = ["spent", "bank", "card", "transaction", "withdrawal", "deposit", "balance", "payment", "transfer"]
    _pattern = re.compile(r'\b(?:' + '|'.join(re.escape(word) for word in _words_to_search) + r')\b', flags=re.IGNORECASE)

    def isBankSms(self, message):
        return bool(self._pattern.search(message))