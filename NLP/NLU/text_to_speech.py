import base64
import io
import torch
import wave
import numpy as np
#import soundfile as sf
from pydub import AudioSegment
from flask import Flask, request, jsonify
from flask_cors import CORS


class SileroTTS:
    def __init__(self, language='ru', model_id='v3_1_ru', device='cpu'):
        self.language = language
        self.model_id = model_id
        self.device = torch.device(device)

        torch.hub.download_url_to_file('https://raw.githubusercontent.com/snakers4/silero-models/master/models.yml',
                                       'latest_silero_models.yml',
                                       progress=False)

        self.model_tts, self.example_text = torch.hub.load(repo_or_dir='snakers4/silero-models',
                                     model='silero_tts',
                                     language=language,
                                     speaker=model_id)
        self.model_tts.to(self.device)
        self.sample_rate = 48000

    def text_to_sound_base64(self, text, speaker='eugene', put_accent=True, put_yo=True):
        print(text)
        audio = self.model_tts.apply_tts(text=text,
                                         speaker=speaker,
                                         sample_rate=self.sample_rate,
                                         #voice_path='voices/drunk_panda',
                                         put_accent=put_accent,
                                         put_yo=put_yo)
        print(audio)
        return self.create_ogg_vorbis_base64(audio, self.sample_rate)
    
    def create_wav_base64(self, audio, sample_rate):
        # Convert the tensor to NumPy array and scale it to int16
        audio_numpy = audio.cpu().numpy()
        audio_int16 = (audio_numpy * (2 ** 15 - 1)).astype(np.int16)
        audio_bytes = audio_int16.tobytes()
        
        # Create a BytesIO buffer to store the WAV data
        wav_buffer = io.BytesIO()
        
        # Write the WAV data to the buffer
        with wave.open(wav_buffer, 'wb') as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(2)  # 16-bit PCM
            wav_file.setframerate(int(sample_rate))
            wav_file.writeframes(audio_bytes)
        
        # Get the bytes from the buffer
        wav_data = wav_buffer.getvalue()
        
        # Encode the bytes to URL-safe base64
        base64_data = base64.b64encode(wav_data).decode('utf-8')
        
        return base64_data

    # def create_ogg_vorbis_base64(self, audio, sample_rate):
    #     # Convert the tensor to NumPy array and scale it to float32
    #     audio_numpy = audio.cpu().numpy().astype(np.float32)
        
    #     # Replace NaNs and Infs with zeros
    #     audio_numpy = np.nan_to_num(audio_numpy)

    #     threshold = 1e-10  # Set a threshold value according to your needs
    #     audio_numpy = np.where(np.abs(audio_numpy) < threshold, 0, audio_numpy)
    #     # Create a BytesIO buffer to store the Ogg Vorbis data
    #     ogg_buffer = io.BytesIO()

    #     # Write the Ogg Vorbis data to the buffer
    #     with sf.SoundFile(ogg_buffer, mode='w', samplerate=int(sample_rate), channels=1, format='OGG', subtype='VORBIS') as f:
    #         f.write(audio_numpy)

    #     # Get the bytes from the buffer
    #     ogg_data = ogg_buffer.getvalue()

    #     # Encode the bytes to URL-safe base64
    #     base64_data = base64.b64encode(ogg_data).decode('utf-8')

    #     return base64_data

    from pydub import AudioSegment

    def create_ogg_vorbis_base64(self, audio, sample_rate):
        # Convert the tensor to NumPy array and scale it to int16
        audio_numpy = audio.cpu().numpy()
        audio_int16 = (audio_numpy * (2 ** 15 - 1)).astype(np.int16)

        # Create audio segment
        audio_segment = AudioSegment(
            audio_int16.tobytes(),
            frame_rate=int(sample_rate),
            sample_width=audio_int16.dtype.itemsize,
            channels=1
        )

        # Export audio segment to ogg vorbis
        ogg_buffer = io.BytesIO()
        audio_segment.export(ogg_buffer, format="ogg")

        # Encode the bytes to URL-safe base64
        base64_data = base64.b64encode(ogg_buffer.getvalue()).decode('utf-8')

        return base64_data


    
app = Flask(__name__)
CORS(app)
silero_tts = SileroTTS()

import re

def convert_to_ssml(text):
    # Заменяем знаки препинания на соответствующие теги SSML
    text = re.sub(r'\. ', '.</s> <s>', text)
    text = re.sub(r'\? ', '?</s> <s>', text)
    text = re.sub(r'! ', '!</s> <s>', text)
    
    # Добавляем теги для начала и конца каждого предложения
    text = '<s>' + text + '</s>'
    
    # Добавляем теги для начала и конца документа
    ssml = '<speak>' + text + '</speak>'

    return ssml



@app.route('/tts', methods=['POST'])
def text_to_speech():
    text = request.json.get('text')
    print(text)
    if not text:
        return jsonify({'error': 'Text is required'}), 400
    
    text = text.replace('Йенкинс', 'Дженкинс').replace(":", "")

    

    text = convert_to_ssml(text)
    print(text)

    audio_base64 = silero_tts.text_to_sound_base64(text)
    return jsonify({'audio_base64': audio_base64})

app.run(port=5001, debug=False)


# def save_audio_to_wav(audio_bytes, sample_rate, filename='test.wav'):
#     with wave.open(filename, 'wb') as wav_file:
#         wav_file.setnchannels(1)
#         wav_file.setsampwidth(2)  # 16-bit PCM
#         wav_file.setframerate(int(sample_rate))
#         wav_file.writeframes(audio_bytes)

# example_text = '<prosody rate="x-slow">Здравствуйте! меня зовут бух+ая панда из варкрафта и я ваш голосовой асссистент</prosody>'
# audio_base64 = silero_tts.text_to_sound_base64(example_text)
# with open('test.txt', 'w') as f:
#     f.write(audio_base64)

# save_audio_to_wav(base64.b64decode(audio_base64), 48000)
