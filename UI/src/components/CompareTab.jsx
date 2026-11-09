import { useState, useEffect } from 'react'
import { findOverlap, getCompanies } from '../api/client'

export default function CompareTab() {
  const [companies, setCompanies] = useState([])
  const [selected, setSelected] = useState([])
  const [search, setSearch] = useState('')
  const [results, setResults] = useState(null)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)

  useEffect(() => {
    getCompanies().then(setCompanies).catch(() => {})
  }, [])

  const filtered = search
    ? companies.filter(c => c.name.toLowerCase().includes(search.toLowerCase()) && !selected.includes(c.name)).slice(0, 8)
    : []

  const addCompany = (name) => {
    setSelected(s => [...s, name])
    setSearch('')
  }

  const doCompare = async (p = 0) => {
    if (selected.length < 2) return
    setLoading(true)
    try {
      const data = await findOverlap({ companies: selected, page: p, size: 20 })
      setResults(data)
      setPage(p)
    } catch (e) { console.error(e) }
    setLoading(false)
  }

  return (
    <div className="compare-container">
      <h3 style={{ marginBottom: 12 }}>Compare companies — find shared problems</h3>
      <div className="compare-select">
        {selected.map(c => (
          <span key={c} className="chip">{c}
            <button onClick={() => setSelected(s => s.filter(x => x !== c))}>×</button>
          </span>
        ))}
        <div style={{ position: 'relative' }}>
          <input className="search-input" placeholder="+ Add company..." value={search}
            onChange={e => setSearch(e.target.value)} style={{ width: 180, fontSize: 12 }} />
          {filtered.length > 0 && (
            <div style={{
              position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 10,
              background: 'var(--bg-tertiary)', border: '1px solid var(--border)',
              borderRadius: 6, maxHeight: 160, overflowY: 'auto',
            }}>
              {filtered.map(c => (
                <div key={c.id} onClick={() => addCompany(c.name)}
                  style={{ padding: '6px 10px', fontSize: 12, cursor: 'pointer' }}
                  onMouseEnter={e => e.target.style.background = 'var(--bg-secondary)'}
                  onMouseLeave={e => e.target.style.background = 'transparent'}
                >{c.name}</div>
              ))}
            </div>
          )}
        </div>
        <button className="btn" onClick={() => doCompare(0)}
          disabled={selected.length < 2}>Find Overlap</button>
      </div>

      {loading && <div className="loading">Loading...</div>}

      {results && !loading && (
        <>
          <div className="compare-info">
            <strong>{results.totalElements}</strong> shared problems across {selected.join(', ')}
          </div>
          <table className="results-table">
            <thead>
              <tr>
                <th>Title</th>
                <th style={{ width: 80 }}>Difficulty</th>
                {selected.map(c => (
                  <th key={c} style={{ width: 100 }}>{c} Freq</th>
                ))}
                <th>Topics</th>
              </tr>
            </thead>
            <tbody>
              {results.content.map(p => (
                <tr key={p.id}>
                  <td><a href={p.link} target="_blank" rel="noopener noreferrer">{p.title}</a></td>
                  <td><span className={`difficulty-badge ${p.difficulty}`}>{p.difficulty}</span></td>
                  {selected.map(companyName => {
                    const match = p.companies.filter(c => c.name === companyName)
                    const maxFreq = match.length > 0 ? Math.max(...match.map(m => m.frequency || 0)) : 0
                    return (
                      <td key={companyName}>
                        <div className="freq-bar"><div className="freq-bar-fill" style={{ width: `${maxFreq}%` }} /></div>
                        <span style={{ fontSize: 10, color: 'var(--text-secondary)' }}>{maxFreq.toFixed(1)}</span>
                      </td>
                    )
                  })}
                  <td className="topic-text">{p.topics.slice(0, 3).join(', ')}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pagination">
            <button className="page-btn" disabled={page === 0} onClick={() => doCompare(page - 1)}>←</button>
            <span style={{ fontSize: 12, color: 'var(--text-secondary)', padding: '4px 8px' }}>
              Page {page + 1} of {results.totalPages}
            </span>
            <button className="page-btn" disabled={page >= results.totalPages - 1} onClick={() => doCompare(page + 1)}>→</button>
          </div>
        </>
      )}
    </div>
  )
}
