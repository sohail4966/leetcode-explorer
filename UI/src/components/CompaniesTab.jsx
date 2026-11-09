import { useState, useEffect } from 'react'
import { getCompanies, getCompanyStats } from '../api/client'

export default function CompaniesTab({ onViewProblems }) {
  const [companies, setCompanies] = useState([])
  const [search, setSearch] = useState('')
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getCompanies().then(data => { setCompanies(data); setLoading(false) }).catch(() => setLoading(false))
  }, [])

  const filtered = search
    ? companies.filter(c => c.name.toLowerCase().includes(search.toLowerCase()))
    : companies

  const openStats = async (name) => {
    try {
      const data = await getCompanyStats(name)
      setStats(data)
    } catch (e) { console.error(e) }
  }

  const maxTopicCount = stats ? Math.max(...stats.topTopics.map(t => t.count)) : 1

  return (
    <div className="companies-container">
      <input className="search-input" placeholder="🔍 Search companies..."
        value={search} onChange={e => setSearch(e.target.value)}
        style={{ maxWidth: 300, marginBottom: 14 }} />

      {loading && <div className="loading">Loading...</div>}

      <div className="company-grid">
        {filtered.map(c => (
          <div key={c.id} className="company-card" onClick={() => openStats(c.name)}>
            <h3>{c.name}</h3>
            <div className="count">{c.problemCount} problems</div>
          </div>
        ))}
      </div>

      {stats && (
        <div className="stats-overlay" onClick={() => setStats(null)}>
          <div className="stats-modal" onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h2>{stats.name}</h2>
              <button className="btn-outline btn-sm" onClick={() => setStats(null)}
                style={{ background: 'transparent', border: '1px solid var(--border)', color: 'var(--text-secondary)', padding: '4px 12px', borderRadius: 6, cursor: 'pointer' }}>
                ✕
              </button>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
              <p style={{ color: 'var(--text-secondary)', margin: 0 }}>{stats.totalProblems} total problems</p>
              <button className="btn btn-sm" onClick={() => { setStats(null); onViewProblems(stats.name) }}>
                View Problems →
              </button>
            </div>

            <div className="stats-section">
              <h4>Difficulty Breakdown</h4>
              {Object.entries(stats.difficultyBreakdown).map(([diff, count]) => (
                <div key={diff} className="stat-bar-row">
                  <span className="stat-bar-label">
                    <span className={`difficulty-badge ${diff}`}>{diff}</span>
                  </span>
                  <div className="stat-bar">
                    <div className="stat-bar-fill" style={{
                      width: `${(count / stats.totalProblems) * 100}%`,
                      background: diff === 'EASY' ? 'var(--easy)' : diff === 'MEDIUM' ? 'var(--medium)' : 'var(--hard)'
                    }} />
                  </div>
                  <span className="stat-bar-count">{count}</span>
                </div>
              ))}
            </div>

            <div className="stats-section">
              <h4>Top Topics</h4>
              {stats.topTopics.map(t => (
                <div key={t.name} className="stat-bar-row">
                  <span className="stat-bar-label">{t.name}</span>
                  <div className="stat-bar">
                    <div className="stat-bar-fill" style={{ width: `${(t.count / maxTopicCount) * 100}%` }} />
                  </div>
                  <span className="stat-bar-count">{t.count}</span>
                </div>
              ))}
            </div>

            <div className="stats-section">
              <h4>Recency Breakdown</h4>
              {Object.entries(stats.recencyBreakdown).map(([rec, count]) => (
                <div key={rec} className="stat-bar-row">
                  <span className="stat-bar-label">{rec.replace(/_/g, ' ')}</span>
                  <div className="stat-bar">
                    <div className="stat-bar-fill" style={{ width: `${(count / stats.totalProblems) * 100}%` }} />
                  </div>
                  <span className="stat-bar-count">{count}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
