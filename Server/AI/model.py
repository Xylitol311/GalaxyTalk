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
        print(f"Input sentences: '{sent1}', '{sent2}'")
        sent1 = sent1.rstrip() + '.' if sent1[-1] not in {'!', '.', '?'} else sent1
        sent2 = sent2.rstrip() + '.' if sent2[-1] not in {'!', '.', '?'} else sent2

        encoded = self.tokenizer(sent1, sent2, 
                            padding=True,
                            truncation=True,
                            max_length=128,
                            return_tensors='pt')
        print(f"Tokenized IDs: {encoded['input_ids']}")
        
        input_ids = encoded['input_ids'].to(self.device)
        attention_mask = encoded['attention_mask'].to(self.device)
         
        with torch.amp.autocast(device_type='cuda'):
            outputs = self.model(input_ids, attention_mask=attention_mask)
            similarity = torch.sigmoid(outputs.logits)
    
        return similarity.item()