import { useState, useEffect } from 'react'
import { getTopics } from '../api/client'

export default function TopicsTab({ onViewTopic }) {
  const [topics, setTopics] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getTopics().then(data => { setTopics(data); setLoading(false) }).catch(() => setLoading(false))
  }, [])

  const filtered = search
    ? topics.filter(t => t.name.toLowerCase().includes(search.toLowerCase()))
    : topics

  return (
    <div className="topics-container">
      <input className="search-input" placeholder="🔍 Search topics..."
        value={search} onChange={e => setSearch(e.target.value)}
        style={{ maxWidth: 300, marginBottom: 14 }} />

      {loading && <div className="loading">Loading...</div>}

      <div className="topics-grid">
        {filtered.map(t => (
          <div key={t.id} className="topic-chip" onClick={() => onViewTopic(t.name)}>
            <strong>{t.name}</strong>
            <span className="count">{t.problemCount}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
