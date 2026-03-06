/**
 * Voice Agent - Real-time voice interaction with Spring AI Alibaba
 * Uses WebSocket for streaming: /voice/ws/audio (audio input) or /voice/ws (text input)
 */

(function () {
  'use strict';

  const SAMPLE_RATE = 16000;
  const CHUNK_SIZE = 1600;

  // State
  let ws = null;
  let audioContext = null;
  let workletNode = null;
  let mediaStream = null;
  let ttsFinishTimeout = null;
  let sessionStartTime = null;
  let isEnding = false;
  let stopTimeoutId = null;
  let agentEndCloseId = null;
  let timerInterval = null;
  let currentTranscript = '';
  let currentResponse = '';

  // DOM refs
  const el = {
    btnStart: document.getElementById('btn-start'),
    btnStop: document.getElementById('btn-stop'),
    statusDot: document.getElementById('status-dot'),
    statusDotInline: document.getElementById('status-dot-inline'),
    statusText: document.getElementById('status-text'),
    elapsed: document.getElementById('elapsed'),
    sessionHint: document.getElementById('session-hint'),
    turnBadge: document.getElementById('turn-badge'),
    transcript: document.getElementById('transcript'),
    response: document.getElementById('response'),
    activityList: document.getElementById('activity-list'),
    logList: document.getElementById('log-list'),
    btnClearActivity: document.getElementById('btn-clear-activity'),
    textInput: document.getElementById('text-input'),
    btnSendText: document.getElementById('btn-send-text'),
  };

  // AudioWorklet code for PCM capture (16kHz, 16-bit, mono)
  const workletCode = `
    class PCMProcessor extends AudioWorkletProcessor {
      constructor() {
        super();
        this.buffer = [];
        this.targetSampleRate = 16000;
        this.resampleRatio = sampleRate / this.targetSampleRate;
        this.resampleIndex = 0;
      }
      process(inputs) {
        const input = inputs[0];
        if (!input || !input[0]) return true;
        const channelData = input[0];
        for (let i = 0; i < channelData.length; i++) {
          this.resampleIndex += 1;
          if (this.resampleIndex >= this.resampleRatio) {
            this.resampleIndex -= this.resampleRatio;
            let sample = Math.max(-1, Math.min(1, channelData[i]));
            const int16 = sample < 0 ? sample * 0x8000 : sample * 0x7FFF;
            this.buffer.push(int16);
          }
        }
        const CHUNK_SIZE = 1600;
        while (this.buffer.length >= CHUNK_SIZE) {
          const chunk = this.buffer.splice(0, CHUNK_SIZE);
          const int16Array = new Int16Array(chunk);
          this.port.postMessage(int16Array.buffer, [int16Array.buffer]);
        }
        return true;
      }
    }
    registerProcessor('pcm-processor', PCMProcessor);
  `;

  function setStatus(status) {
    el.statusDot.className = 'status-dot status-' + status;
    el.statusDotInline.className = 'status-dot status-' + status;
    const texts = {
      ready: 'Ready',
      connecting: 'Connecting...',
      listening: 'Listening...',
      error: 'Error',
      disconnected: 'Disconnected',
    };
    el.statusText.textContent = texts[status] || status;
  }

  function formatTime(date) {
    return date.toLocaleTimeString();
  }

  function addActivity(type, label, text, args) {
    const empty = el.activityList.querySelector('.activity-empty');
    if (empty) empty.remove();

    const icons = { stt: '🎤', agent: '🤖', tts: '🔊', tool: '🔧' };
    const iconClass = 'activity-icon-' + type;
    const labelClass = 'activity-label-' + type;

    const div = document.createElement('div');
    div.className = 'activity-item';
    div.innerHTML = `
      <div class="activity-icon ${iconClass}">${icons[type] || '📋'}</div>
      <div class="activity-body">
        <div class="activity-label ${labelClass}">${escapeHtml(label)}</div>
        <div class="activity-text">${escapeHtml(text)}</div>
        ${args ? `<pre class="activity-args" style="margin-top:0.5rem;padding:0.5rem;background:#fff;border-radius:0.25rem;font-size:0.6875rem;overflow-x:auto;">${escapeHtml(JSON.stringify(args, null, 2))}</pre>` : ''}
        <div class="activity-time">${formatTime(new Date())}</div>
      </div>
    `;
    el.activityList.insertBefore(div, el.activityList.firstChild);

    // Keep max 50 items
    while (el.activityList.children.length > 50) {
      el.activityList.removeChild(el.activityList.lastChild);
    }
  }

  function escapeHtml(s) {
    const div = document.createElement('div');
    div.textContent = s;
    return div.innerHTML;
  }

  function addLog(message) {
    const empty = el.logList.querySelector('.log-empty');
    if (empty) empty.remove();

    const div = document.createElement('div');
    div.className = 'log-entry';
    div.innerHTML = `<span class="log-time">${formatTime(new Date())}</span>${escapeHtml(message)}`;
    el.logList.appendChild(div);

    while (el.logList.children.length > 100) {
      el.logList.removeChild(el.logList.firstChild);
    }
    el.logList.scrollTop = el.logList.scrollHeight;
  }

  function clearActivity() {
    el.activityList.innerHTML = '<div class="activity-empty">No activity yet...</div>';
  }

  function clearLogs() {
    el.logList.innerHTML = '<div class="log-empty">Logs will appear here...</div>';
  }

  function resetPipeline() {
    currentTranscript = '';
    currentResponse = '';
    el.transcript.textContent = '—';
    el.response.textContent = '—';
    el.turnBadge.textContent = 'Waiting...';
    el.turnBadge.className = 'turn-badge turn-waiting';
  }

  function startTurn() {
    el.turnBadge.textContent = 'Turn Active';
    el.turnBadge.className = 'turn-badge turn-active';
  }

  function finishTurn() {
    el.turnBadge.textContent = 'Waiting...';
    el.turnBadge.className = 'turn-badge turn-waiting';
  }

  // TTS playback: buffer MP3 chunks, flush to blob and queue; play queue in order (no overlap)
  let ttsBuffer = [];
  let ttsPlayTimeout = null;
  let ttsPlayQueue = [];
  let ttsPlaying = false;
  /** One-shot callback when TTS queue has finished playing (so we can close session after playback). */
  let onTtsQueueDrained = null;

  function pushTtsChunk(base64Audio) {
    ttsBuffer.push(base64Audio);
  }

  function flushTtsToQueue() {
    if (ttsBuffer.length === 0) return;
    const combined = ttsBuffer.map((b) => atob(b)).join('');
    const bytes = new Uint8Array(combined.length);
    for (let i = 0; i < combined.length; i++) bytes[i] = combined.charCodeAt(i);
    ttsBuffer = [];
    ttsPlayQueue.push(new Blob([bytes], { type: 'audio/mpeg' }));
    playNextTtsInQueue();
  }

  function playNextTtsInQueue() {
    if (ttsPlaying || ttsPlayQueue.length === 0) {
      if (!ttsPlaying && ttsPlayQueue.length === 0 && onTtsQueueDrained) {
        const fn = onTtsQueueDrained;
        onTtsQueueDrained = null;
        fn();
      }
      return;
    }
    ttsPlaying = true;
    const blob = ttsPlayQueue.shift();
    const url = URL.createObjectURL(blob);
    const audio = new Audio(url);
    audio.onended = () => {
      URL.revokeObjectURL(url);
      ttsPlaying = false;
      playNextTtsInQueue();
    };
    audio.onerror = () => {
      URL.revokeObjectURL(url);
      ttsPlaying = false;
      playNextTtsInQueue();
    };
    audio.play();
  }

  function stopTtsPlayback() {
    onTtsQueueDrained = null;
    ttsBuffer = [];
    ttsPlayQueue = [];
    if (ttsPlayTimeout) {
      clearTimeout(ttsPlayTimeout);
      ttsPlayTimeout = null;
    }
    if (ttsFinishTimeout) {
      clearTimeout(ttsFinishTimeout);
      ttsFinishTimeout = null;
    }
  }

  function handleEvent(ev) {
    switch (ev.type) {
      case 'stt_chunk':
        // Realtime partial transcript (live recognition)
        el.transcript.textContent = (ev.transcript || '') + '…';
        break;

      case 'stt_output':
        currentTranscript = ev.transcript || '';
        el.transcript.textContent = currentTranscript || '—';
        addActivity('stt', 'Transcription', currentTranscript);
        break;

      case 'agent_chunk':
        currentResponse += ev.text || '';
        el.response.textContent = currentResponse || '—';
        break;

      case 'tool_call':
        addActivity('tool', `Tool: ${ev.name}`, 'Called with arguments:', ev.args);
        addLog(`Tool call: ${ev.name}`);
        break;

      case 'tool_result':
        addActivity('tool', `Tool Result: ${ev.name}`, ev.result || '');
        addLog(`Tool result: ${ev.result}`);
        break;

      case 'agent_end':
        if (currentResponse) {
          addActivity('agent', 'Agent Response', currentResponse);
        }
        if (isEnding) {
          // Fallback: close only if tts_end never arrives (e.g. error). Don't close after 2s
          // or TTS stream gets cut off; we close on tts_end instead.
          agentEndCloseId = setTimeout(() => {
            if (ws) {
              ws.close();
              ws = null;
            }
            if (stopTimeoutId) clearTimeout(stopTimeoutId);
            agentEndCloseId = null;
            finalizeStop();
          }, 30000);
        }
        break;

      case 'tts_chunk':
        if (ev.audio) {
          pushTtsChunk(ev.audio);
        }
        if (ttsFinishTimeout) clearTimeout(ttsFinishTimeout);
        ttsFinishTimeout = setTimeout(() => {
          flushTtsToQueue();
          ttsFinishTimeout = null;
          finishTurn();
          // Do not close WebSocket here when isEnding: wait for tts_end so all TTS
          // chunks are received; otherwise playback is cut off and "WebSocket disconnected"
          // appears before stream finishes.
        }, 300);
        break;

      case 'tts_end':
        if (ttsFinishTimeout) {
          clearTimeout(ttsFinishTimeout);
          ttsFinishTimeout = null;
        }
        flushTtsToQueue();
        finishTurn();
        if (isEnding && ws) {
          if (agentEndCloseId) {
            clearTimeout(agentEndCloseId);
            agentEndCloseId = null;
          }
          // Close WebSocket and show "session ended" only after TTS queue has finished playing
          onTtsQueueDrained = () => {
            onTtsQueueDrained = null;
            if (ws) {
              ws.close();
              ws = null;
            }
            if (stopTimeoutId) clearTimeout(stopTimeoutId);
            finalizeStop();
          };
          playNextTtsInQueue();
        }
        break;

      case 'error':
        addLog('Error: ' + (ev.message || 'Unknown error'));
        setStatus('error');
        break;
    }
  }

  async function startAudioCapture(onChunk) {
    if (!audioContext) {
      audioContext = new AudioContext();
      const blob = new Blob([workletCode], { type: 'application/javascript' });
      const url = URL.createObjectURL(blob);
      await audioContext.audioWorklet.addModule(url);
      URL.revokeObjectURL(url);
    }
    if (audioContext.state === 'suspended') {
      await audioContext.resume();
    }
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
    });
    const source = audioContext.createMediaStreamSource(mediaStream);
    workletNode = new AudioWorkletNode(audioContext, 'pcm-processor');
    workletNode.port.onmessage = (e) => onChunk(e.data);
    source.connect(workletNode);
  }

  function stopAudioCapture() {
    if (workletNode) {
      workletNode.disconnect();
      workletNode = null;
    }
    if (mediaStream) {
      mediaStream.getTracks().forEach((t) => t.stop());
      mediaStream = null;
    }
    if (audioContext) {
      audioContext.close().catch(() => {});
      audioContext = null;
    }
  }

  async function start() {
    resetPipeline();
    clearActivity();
    clearLogs();
    stopTtsPlayback();
    setStatus('connecting');

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/voice/ws/audio`;
    ws = new WebSocket(wsUrl);
    ws.binaryType = 'arraybuffer';

    ws.onopen = async () => {
      setStatus('listening');
      sessionStartTime = Date.now();
      timerInterval = setInterval(() => {
        const s = Math.floor((Date.now() - sessionStartTime) / 1000);
        const m = Math.floor(s / 60);
        el.elapsed.textContent = `${m}:${(s % 60).toString().padStart(2, '0')}`;
      }, 1000);
      el.btnStart.disabled = true;
      el.btnStop.disabled = false;
      el.sessionHint.classList.remove('hidden');
      addLog('Session started');

      try {
        await startAudioCapture((chunk) => {
          if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(chunk);
          }
        });
        addLog('Microphone access granted');
        addLog('Streaming PCM audio (16kHz, 16-bit, mono)');
      } catch (err) {
        addLog('Error: ' + (err.message || 'Unknown error'));
        setStatus('error');
        stop();
      }
    };

    ws.onmessage = (event) => {
      const ev = JSON.parse(event.data);
      startTurn();
      handleEvent(ev);
    };

    ws.onclose = () => {
      setStatus('disconnected');
      addLog('WebSocket disconnected');
    };

    ws.onerror = () => {
      addLog('WebSocket error');
      setStatus('error');
    };
  }

  function stop() {
    isEnding = true;

    if (ttsFinishTimeout) {
      clearTimeout(ttsFinishTimeout);
      ttsFinishTimeout = null;
    }

    stopTtsPlayback();
    stopAudioCapture();

    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'end' }));
      addLog('Sent end signal, waiting for response...');
      stopTimeoutId = setTimeout(() => {
        if (ws) {
          ws.close();
          ws = null;
        }
        finalizeStop();
      }, 60000);
    } else {
      if (ws) {
        ws.close();
        ws = null;
      }
      finalizeStop();
    }
  }

  function finalizeStop() {
    isEnding = false;
    if (stopTimeoutId) {
      clearTimeout(stopTimeoutId);
      stopTimeoutId = null;
    }
    if (agentEndCloseId) {
      clearTimeout(agentEndCloseId);
      agentEndCloseId = null;
    }
    addLog('Session ended');

    if (timerInterval) {
      clearInterval(timerInterval);
      timerInterval = null;
    }

    el.btnStart.disabled = false;
    el.btnStop.disabled = true;
    el.sessionHint.classList.add('hidden');
    setStatus('ready');
    finishTurn();
  }

  async function sendText() {
    const text = (el.textInput.value || '').trim();
    if (!text) return;

    el.textInput.value = '';
    resetPipeline();
    startTurn();
    addActivity('stt', 'User (text)', text);
    currentTranscript = text;
    el.transcript.textContent = text;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/voice/ws`;
    const textWs = new WebSocket(wsUrl);

    textWs.onopen = () => {
      textWs.send(JSON.stringify({ text }));
    };

    textWs.onmessage = (event) => {
      const ev = JSON.parse(event.data);
      handleEvent(ev);
    };

    textWs.onclose = () => {
      finishTurn();
    };

    textWs.onerror = () => {
      addLog('WebSocket error (text)');
      setStatus('error');
      finishTurn();
    };
  }

  el.btnStart.addEventListener('click', start);
  el.btnStop.addEventListener('click', stop);
  el.btnClearActivity.addEventListener('click', clearActivity);
  el.btnSendText.addEventListener('click', sendText);
  el.textInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') sendText();
  });
})();
