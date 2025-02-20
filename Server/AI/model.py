# model.py
import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from config import get_config

Config = get_config()

class SentenceSimilarity:
    def __init__(self):
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        self.tokenizer = AutoTokenizer.from_pretrained(Config.MODEL_NAME)
        self.model = AutoModelForSequenceClassification.from_pretrained(
            Config.MODEL_PATH,
            num_labels=1,
            problem_type="regression",
            torch_dtype=torch.float16
        ).to(self.device)

        self.model.eval()  # Set to evaluation mode
   
    def compute_similarity(self, sent1: str, sent2: str):
        sent1 = sent1.rstrip() + '.' if sent1[-1] not in {'!', '.', '?'} else sent1
        sent2 = sent2.rstrip() + '.' if sent2[-1] not in {'!', '.', '?'} else sent2

        print(f"Input sentences: '{sent1}', '{sent2}'")

        inputs = self.tokenizer(sent1, sent2, 
                            padding=True,
                            truncation=True,
                            max_length=128,
                            return_tensors='pt')
        print(f"Tokenized IDs: {inputs['input_ids']}")
        
        inputs = {k: v.to(self.device) for k, v in inputs.items()}

        with torch.no_grad():
            outputs = self.model(**inputs)
            prediction = outputs.logits.squeeze().item()

        return prediction