const recordBtn = document.getElementById('record-btn');
const player = document.getElementById('player');
const transcript = document.getElementById('transcript');
const nluResult = document.getElementById('nlu-result');

let recorder;
let audioChunks = [];
let recording = false;

const initRecorder = async () => {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

    recorder = new MediaRecorder(stream, { mimeType: 'audio/ogg' });

    recorder.addEventListener('dataavailable', (event) => {
        audioChunks.push(event.data);
    });

    recorder.addEventListener('stop', async () => {
        const audioBlob = new Blob(audioChunks, { type: 'audio/ogg' });
        const base64Audio = await blobToBase64(audioBlob);
        const text = await sendAudioToSTT(base64Audio);
        console.log(text)
        const nlu_response = await sendTextToNLU(text);
        console.log(nlu_response.message.split('\\n').join('<br>').split('\n').join('<br>'))
        nluResult.innerHTML = nlu_response.message.split('\\n').join('<br>').split('\n').join('<br>');
        
        
        const audio_base64 = await sendTextToTTS(nlu_response.for_tts);
        player.src = 'data:audio/ogg;base64,' + audio_base64;
        player.play(); // Автоматическое воспроизведение аудио
    });
};

const blobToBase64 = (blob) => {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(blob);
        reader.onloadend = () => {
            console.log(reader.result)
            const base64data = reader.result.split(',')[1];
            resolve(base64data);
        };
        reader.onerror = reject;
    });
};

const sendAudioToSTT = async (base64Audio) => {
    const response = await fetch('http://localhost:5002/transcribe', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ audio: base64Audio })
    });

    const data = await response.json();
    return data.transcription;
};


const sendTextToNLU = async (text) => {
    const response = await fetch('http://localhost:5000/parse', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ text: text })
    });

    const data = await response.json();
    return data;
};

const sendTextToTTS = async (text) => {
    const response = await fetch('http://localhost:5001/tts', {
        method: 'POST',
        headers: { 
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ text: text })
    });

    const data = await response.json();
    return data.audio_base64;
};

recordBtn.addEventListener('click', () => {
    nluResult.innerHTML = ""
    if (!recording) {
        recording = true;
        recordBtn.textContent = 'Stop Recording';
        audioChunks = [];
        recorder.start();
    } else {
        recording = false;
        recordBtn.textContent = 'Record';
        recorder.stop();
    }
});

initRecorder();
