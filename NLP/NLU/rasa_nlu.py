import asyncio
from rasa.core.agent import Agent
from rasa.shared.utils.io import json_to_string

class Load_Rasa_NLU:
    def __init__(self, model_path:str) -> None:
        self.agent = Agent.load(model_path)
        print("NLU model loaded")

    def nlu_processing(self, message: str) -> str:
        message = message.strip()
        results = asyncio.run(self.agent.parse_message(message))
        return json_to_string(results)