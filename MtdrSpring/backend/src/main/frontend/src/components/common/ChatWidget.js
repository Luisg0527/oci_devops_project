import React, { useEffect, useRef, useState } from 'react';
import { API_BASE } from '../../config/apiBase';
import './ChatWidget.css';

const STORAGE_KEY = 'sprintly_chat_history';
const MAX_HISTORY = 10;

const WELCOME_MESSAGE = {
  role: 'assistant',
  content:
    'Hola, soy Sprintly. Pregúntame por el estado de un sprint, riesgos en tu equipo, tareas atrasadas o pídeme un resumen rápido para tomar decisiones.',
};

function loadHistory() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return [WELCOME_MESSAGE];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : [WELCOME_MESSAGE];
  } catch (e) {
    return [WELCOME_MESSAGE];
  }
}

function saveHistory(messages) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages));
  } catch (e) {
    // sessionStorage lleno o inaccesible: ignorar
  }
}

function ChatWidget({ open, onClose }) {
  const [messages, setMessages] = useState(loadHistory);
  const [input, setInput] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState('');
  const scrollRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    saveHistory(messages);
  }, [messages]);

  useEffect(() => {
    if (open && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
    if (open && inputRef.current) {
      inputRef.current.focus();
    }
  }, [open, messages]);

  const handleSend = async (event) => {
    if (event) event.preventDefault();
    const text = input.trim();
    if (!text || isSending) return;

    const token = localStorage.getItem('authToken');
    if (!token) {
      setError('Sesión expirada. Inicia sesión de nuevo.');
      return;
    }

    setError('');
    const userMessage = { role: 'user', content: text };
    const nextMessages = [...messages, userMessage];
    setMessages(nextMessages);
    setInput('');
    setIsSending(true);

    try {
      const history = nextMessages
        .filter((m) => m !== WELCOME_MESSAGE)
        .slice(-MAX_HISTORY - 1, -1);

      const response = await fetch(`${API_BASE}/ai/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          message: text,
          history,
        }),
      });

      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.error || `Error ${response.status}`);
      }

      const assistantMessage = {
        role: 'assistant',
        content: payload.reply || '(respuesta vacía)',
      };
      setMessages((prev) => [...prev, assistantMessage]);
    } catch (err) {
      setError(err.message || 'No se pudo obtener respuesta del asistente.');
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: `⚠️ ${err.message || 'Error al procesar la solicitud.'}` },
      ]);
    } finally {
      setIsSending(false);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSend();
    }
  };

  const handleClear = () => {
    setMessages([WELCOME_MESSAGE]);
    setError('');
  };

  if (!open) return null;

  return (
    <div className="chat-widget" role="dialog" aria-label="Asistente Sprintly">
      <header className="chat-widget__header">
        <div className="chat-widget__title">
          <span className="material-icons chat-widget__title-icon">smart_toy</span>
          <div>
            <strong>Sprintly</strong>
            <span className="chat-widget__subtitle">Asistente de proyectos</span>
          </div>
        </div>
        <div className="chat-widget__actions">
          <button
            type="button"
            className="chat-widget__icon-btn"
            onClick={handleClear}
            aria-label="Limpiar conversación"
            title="Limpiar conversación"
          >
            <span className="material-icons">refresh</span>
          </button>
          <button
            type="button"
            className="chat-widget__icon-btn"
            onClick={onClose}
            aria-label="Cerrar chat"
            title="Cerrar"
          >
            <span className="material-icons">close</span>
          </button>
        </div>
      </header>

      <div className="chat-widget__messages" ref={scrollRef}>
        {messages.map((msg, idx) => (
          <div
            key={idx}
            className={`chat-message chat-message--${msg.role}`}
          >
            <div className="chat-message__bubble">{msg.content}</div>
          </div>
        ))}
        {isSending && (
          <div className="chat-message chat-message--assistant">
            <div className="chat-message__bubble chat-message__bubble--typing">
              <span className="chat-typing-dot" />
              <span className="chat-typing-dot" />
              <span className="chat-typing-dot" />
            </div>
          </div>
        )}
      </div>

      {error && <div className="chat-widget__error">{error}</div>}

      <form className="chat-widget__input-row" onSubmit={handleSend}>
        <textarea
          ref={inputRef}
          className="chat-widget__input"
          placeholder="Pregunta sobre sprints, tareas, equipo…"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          rows={2}
          disabled={isSending}
        />
        <button
          type="submit"
          className="chat-widget__send"
          disabled={isSending || !input.trim()}
          aria-label="Enviar"
        >
          <span className="material-icons">send</span>
        </button>
      </form>
    </div>
  );
}

export default ChatWidget;
