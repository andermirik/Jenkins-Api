import torch
from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor
import soundfile as sf
import io, base64
from flask import Flask, request, jsonify
import torchaudio
from flask_cors import CORS

import difflib

RUSSIAN_NUMBERS = {
    "ноль": 0,
    "один": 1,
    "два": 2,
    "три": 3,
    "четыре": 4,
    "пять": 5,
    "шесть": 6,
    "семь": 7,
    "восемь": 8,
    "девять": 9,
    "десять": 10,
    "одиннадцать": 11,
    "двенадцать": 12,
    "тринадцать": 13,
    "четырнадцать": 14,
    "пятнадцать": 15,
    "шестнадцать": 16,
    "семнадцать": 17,
    "восемнадцать": 18,
    "девятнадцать": 19,
    "двадцать": 20,
}

# Добавляем числа от 21 до 99
for i in range(21, 100):
    tens = i // 10
    ones = i % 10
    tens_word = ""
    ones_word = ""

    if tens == 2:
        tens_word = "двадцать"
    elif tens == 3:
        tens_word = "тридцать"
    elif tens == 4:
        tens_word = "сорок"
    elif tens == 5:
        tens_word = "пятьдесят"
    elif tens == 6:
        tens_word = "шестьдесят"
    elif tens == 7:
        tens_word = "семьдесят"
    elif tens == 8:
        tens_word = "восемьдесят"
    elif tens == 9:
        tens_word = "девяносто"

    if ones == 1:
        ones_word = "один"
    elif ones == 2:
        ones_word = "два"
    elif ones == 3:
        ones_word = "три"
    elif ones == 4:
        ones_word = "четыре"
    elif ones == 5:
        ones_word = "пять"
    elif ones == 6:
        ones_word = "шесть"
    elif ones == 7:
        ones_word = "семь"
    elif ones == 8:
        ones_word = "восемь"
    elif ones == 9:
        ones_word = "девять"

    word = f"{tens_word} {ones_word}"
    RUSSIAN_NUMBERS[word.strip()] = i

app = Flask(__name__)
CORS(app)

def decode_base64_to_ogg_file(base64_data):
    return io.BytesIO(base64.b64decode(base64_data))

def load_audio(wav_audio):
    waveform, sample_rate = torchaudio.load(wav_audio, format="wav")
    if waveform.ndim == 2:  # Если волновая форма имеет две размерности, уберите размерность канала, если каналов больше одного
        waveform = waveform.mean(axis=0, keepdim=True)
    else:
        waveform = torch.unsqueeze(waveform, 0)
    return waveform, sample_rate

class SpeechToText:
    def __init__(self, model_name="jonatasgrosman/wav2vec2-large-xlsr-53-russian", device="cpu"):
        self.device = torch.device(device)
        self.processor = Wav2Vec2Processor.from_pretrained(model_name)
        self.model = Wav2Vec2ForCTC.from_pretrained(model_name).to(self.device)

    def transcribe(self, waveform, sample_rate):
        if sample_rate != 16000:
            waveform = self.resample_audio(waveform, sample_rate, 16000)
            sample_rate = 16000

        input_values = self.processor(waveform, sampling_rate=sample_rate, return_tensors="pt").input_values.squeeze(1).to(self.device)
        logits = self.model(input_values).logits
        predicted_ids = torch.argmax(logits, dim=-1)
        transcription = self.processor.decode(predicted_ids[0])

        return transcription

    def resample_audio(self, waveform, input_sample_rate, output_sample_rate):
        waveform = waveform.to(self.device)  # Преобразование в torch.Tensor
        resampler = torchaudio.transforms.Resample(input_sample_rate, output_sample_rate)
        return resampler(waveform)


stt = SpeechToText()

import ffmpeg

def opus_to_wav(opus_audio):
    with open("input_audio.opus", "wb") as input_file:
        input_file.write(opus_audio)

    process = (
        ffmpeg
        .input("input_audio.opus")
        .output("output_audio.wav")
        .overwrite_output()
        .run()
    )

    with open("output_audio.wav", "rb") as output_file:
        wav_audio = io.BytesIO(output_file.read())

    return wav_audio

def post_process_numbers(text):
    words = text.split()
    result = []
    i = 0

    while i < len(words):
        word = words[i]

        if word.isdigit():
            number = int(word)
            i += 1
            while i < len(words) and words[i].isdigit():
                number += int(words[i])
                i += 1
            result.append(str(number))
        else:
            result.append(word)
            i += 1

    return " ".join(result)

def contains_all_words(phrase, match):
    phrase_words = set(phrase.split())
    match_words = set(match.split())
    return match_words.issubset(phrase_words)

def is_number_word(word):
    return word in RUSSIAN_NUMBERS

def replace_numbers_with_digits(text):
    words = text.split()
    result = []
    i = 0

    while i < len(words):
        word = words[i]
        matches = difflib.get_close_matches(word, RUSSIAN_NUMBERS.keys(), n=1, cutoff=0.75)
        if matches and is_number_word(matches[0]):
            num_word = matches[0]
            if contains_all_words(" ".join(words[i:i+num_word.count(" ")+1]), num_word):
                result.append(str(RUSSIAN_NUMBERS[num_word]))
                i += num_word.count(" ") + 1
            else:
                result.append(word)
                i += 1
        else:
            result.append(word)
            i += 1

    return post_process_numbers(" ".join(result))

text = "остаромисборку задача задача номер сорок два"
print(replace_numbers_with_digits(text))


#text = "останови сбурку задачи задача номер срогдва"
#result = replace_numbers_with_digits(text)
#print(result)  # "останови сбурку задачи задача номер 42"

@app.route('/transcribe', methods=['POST'])
def transcribe_audio():
    data = request.get_json()
    base64_audio = data.get('audio')

    if not base64_audio:
        return jsonify({'error': 'No audio data provided'}), 400

    opus_audio = base64.b64decode(base64_audio)
    wav_audio = opus_to_wav(opus_audio)

    waveform, sample_rate = load_audio(wav_audio)

    transcription = stt.transcribe(waveform, sample_rate)

    print(transcription)

    return jsonify({'transcription': replace_numbers_with_digits(transcription)}), 200


app.run(port=5002)
