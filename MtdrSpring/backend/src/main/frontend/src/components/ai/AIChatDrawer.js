import React, { useEffect, useRef, useState } from 'react';
import Drawer from '@mui/material/Drawer';
import IconButton from '@mui/material/IconButton';
import TextField from '@mui/material/TextField';
import CircularProgress from '@mui/material/CircularProgress';
import useAIChat from './useAIChat';
import './AIChatDrawer.css';

function AIChatDrawer({ open, onClose }) {
  const { messages, loading, error, send, reset } = useAIChat();
  const [input, setInput] = useState('');
  const scrollRef = useRef(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, loading]);

  const handleSend = () => {
    if (!input.trim() || loading) return;
    send(input);
    setInput('');
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{ className: 'ai-drawer__paper' }}
    >
      <div className="ai-drawer__header">
        <span className="ai-drawer__title">Asistente IA</span>
        <div>
          <IconButton size="small" onClick={reset} title="Limpiar conversación">
            <span className="material-icons" style={{ fontSize: 20 }}>refresh</span>
          </IconButton>
          <IconButton size="small" onClick={onClose} title="Cerrar">
            <span className="material-icons" style={{ fontSize: 20 }}>close</span>
          </IconButton>
        </div>
      </div>

      <div className="ai-drawer__messages" ref={scrollRef}>
        {messages.map((m, idx) => (
          <div
            key={idx}
            className={`ai-bubble ai-bubble--${m.role === 'user' ? 'user' : 'assistant'}`}
          >
            {m.content}
            {m.citations && m.citations.length > 0 && (
              <div className="ai-citations">
                {m.citations.map((c, i) => (
                  <span key={i} className="ai-citation">
                    [{c.sourceType}#{c.sourceId}]
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}
        {loading && (
          <div className="ai-bubble ai-bubble--assistant">
            <CircularProgress size={16} />
            <span style={{ marginLeft: 8 }}>Pensando...</span>
          </div>
        )}
      </div>

      {error && <div className="ai-drawer__error">{error}</div>}

      <div className="ai-drawer__input-row">
        <TextField
          fullWidth
          size="small"
          multiline
          maxRows={4}
          placeholder="Pregunta algo sobre el proyecto..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={loading}
        />
        <IconButton
          color="primary"
          onClick={handleSend}
          disabled={loading || !input.trim()}
          title="Enviar"
        >
          <span className="material-icons">send</span>
        </IconButton>
      </div>
    </Drawer>
  );
}

export default AIChatDrawer;
