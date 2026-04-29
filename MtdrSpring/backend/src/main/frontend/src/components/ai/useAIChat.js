import { useState, useCallback } from 'react';
import { API_AI } from '../../API';

const ENDPOINT = process.env.NODE_ENV === 'production'
  ? API_AI
  : `http://localhost:8080${API_AI}`;

const initialMessages = [
  { role: 'assistant', content: '¡Hola! Soy tu asistente de proyecto. Pregúntame sobre proyectos, sprints, tareas, KPIs, despliegues o incidentes.' }
];

export default function useAIChat() {
  const [messages, setMessages] = useState(initialMessages);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const send = useCallback(async (question) => {
    const trimmed = (question || '').trim();
    if (!trimmed) return;

    setError(null);
    setLoading(true);

    const userTurn = { role: 'user', content: trimmed };
    const newMessages = [...messages, userTurn];
    setMessages(newMessages);

    const history = newMessages
      .filter(m => m.role === 'user' || m.role === 'assistant')
      .slice(0, -1)
      .map(m => ({ role: m.role, content: m.content }));

    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`${ENDPOINT}/ask`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ question: trimmed, history })
      });

      if (res.status === 401) {
        setError('Sesión expirada. Inicia sesión de nuevo.');
        setLoading(false);
        return;
      }
      if (!res.ok) {
        const txt = await res.text();
        throw new Error(`HTTP ${res.status}: ${txt}`);
      }
      const data = await res.json();
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: data.answer,
        citations: data.citations || []
      }]);
    } catch (e) {
      setError(e.message || 'Error consultando al asistente');
    } finally {
      setLoading(false);
    }
  }, [messages]);

  const reset = useCallback(() => {
    setMessages(initialMessages);
    setError(null);
  }, []);

  return { messages, loading, error, send, reset };
}
