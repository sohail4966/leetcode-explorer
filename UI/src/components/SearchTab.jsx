import { useState, useEffect, useCallback, useRef } from 'react'
import { searchProblems, getCompanies, getTopics } from '../api/client'

const RECENCY_OPTIONS = [
  { value: '', label: 'All' },
  { value: 'THIRTY_DAYS', label: '30d' },
  { value: 'THREE_MONTHS', label: '3mo' },
  { value: 'SIX_MONTHS', label: '6mo' },
  { value: 'MORE_THAN_SIX_MONTHS', label: '6mo+' },
]

export default function SearchTab({ initialCompany, initialTopic }) {
  const [filters, setFilters] = useState({
    search: '', company: initialCompany ? [initialCompany] : [],
    difficulty: [], topic: initialTopic ? [initialTopic] : [],
    recency: '', minFrequency: '', maxFrequency: '',
    sortBy: 'title', sortOrder: 'asc', page: 0, size: 20,
  })
  const [results, setResults] = useState(null)
  const [loading, setLoading] = useState(false)
  const [companyOptions, setCompanyOptions] = useState([])
  const [topicOptions, setTopicOptions] = useState([])
  const [companySearch, setCompanySearch] = useState('')
  const [topicSearch, setTopicSearch] = useState('')
  const isFirstRender = useRef(true)
  const isFirstPage = useRef(true)

  useEffect(() => {
    getCompanies().then(setCompanyOptions).catch(() => {})
    getTopics().then(setTopicOptions).catch(() => {})
  }, [])

  const doSearch = useCallback(async (f) => {
    setLoading(true)
    try {
      const params = { ...f }
      if (!params.minFrequency) delete params.minFrequency
      if (!params.maxFrequency) delete params.maxFrequency
      if (!params.recency) delete params.recency
      const data = await searchProblems(params)
      setResults(data)
    } catch (e) { console.error(e) }
    setLoading(false)
  }, [])

  const filterKey = JSON.stringify({
    s: filters.search, c: filters.company, d: filters.difficulty,
    t: filters.topic, r: filters.recency, mn: filters.minFrequency,
    mx: filters.maxFrequency, sb: filters.sortBy, so: filters.sortOrder,
  })

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false
      doSearch(filters)
      return
    }
    const timeout = setTimeout(() => {
      setFilters(f => ({ ...f, page: 0 }))
      doSearch({ ...filters, page: 0 })
    }, 300)
    return () => clearTimeout(timeout)
  }, [filterKey])

  useEffect(() => {
    if (isFirstPage.current) {
      isFirstPage.current = false
      return
    }
    doSearch(filters)
  }, [filters.page])

  const clearFilters = () => {
    const cleared = {
      search: '', company: [], difficulty: [], topic: [],
      recency: '', minFrequency: '', maxFrequency: '',
      sortBy: 'title', sortOrder: 'asc', page: 0, size: 20,
    }
    setFilters(cleared)
  }

  const toggleDifficulty = (d) => {
    setFilters(f => ({
      ...f,
      difficulty: f.difficulty.includes(d)
        ? f.difficulty.filter(x => x !== d)
        : [...f.difficulty, d]
    }))
  }

  const addCompany = (name) => {
    if (!filters.company.includes(name)) {
      setFilters(f => ({ ...f, company: [...f.company, name] }))
    }
    setCompanySearch('')
  }

  const removeCompany = (name) => {
    setFilters(f => ({ ...f, company: f.company.filter(c => c !== name) }))
  }

  const addTopic = (name) => {
    if (!filters.topic.includes(name)) {
      setFilters(f => ({ ...f, topic: [...f.topic, name] }))
    }
    setTopicSearch('')
  }

  const removeTopic = (name) => {
    setFilters(f => ({ ...f, topic: f.topic.filter(t => t !== name) }))
  }

  const toggleSort = (col) => {
    setFilters(f => {
      if (f.sortBy !== col) return { ...f, sortBy: col, sortOrder: 'asc' }
      if (f.sortOrder === 'asc') return { ...f, sortBy: col, sortOrder: 'desc' }
      return { ...f, sortBy: 'title', sortOrder: 'asc' }
    })
  }

  const sortIndicator = (col) => {
    const active = filters.sortBy === col
    return (
      <span className="sort-arrows">
        <span className={`sort-arrow${active && filters.sortOrder === 'asc' ? ' active' : ''}`}>▲</span>
        <span className={`sort-arrow${active && filters.sortOrder === 'desc' ? ' active' : ''}`}>▼</span>
      </span>
    )
  }

  const filteredCompanies = companySearch
    ? companyOptions.filter(c => c.name.toLowerCase().includes(companySearch.toLowerCase())).slice(0, 8)
    : []

  const filteredTopics = topicSearch
    ? topicOptions.filter(t => t.name.toLowerCase().includes(topicSearch.toLowerCase())).slice(0, 8)
    : []

  return (
    <div className="search-layout">
      <div className="sidebar">
        <div className="sidebar-section">
          <input
            className="search-input"
            placeholder="🔍 Search problems..."
            value={filters.search}
            onChange={e => setFilters(f => ({ ...f, search: e.target.value }))}
            onKeyDown={e => e.key === 'Enter' && e.preventDefault()}
          />
        </div>

        <div className="sidebar-section">
          <div className="sidebar-label">Company</div>
          <div style={{ position: 'relative' }}>
            <input
              className="search-input"
              placeholder="Select companies..."
              value={companySearch}
              onChange={e => setCompanySearch(e.target.value)}
              style={{ fontSize: 12 }}
            />
            {filteredCompanies.length > 0 && (
              <div style={{
                position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 10,
                background: 'var(--bg-tertiary)', border: '1px solid var(--border)',
                borderRadius: 6, maxHeight: 160, overflowY: 'auto',
              }}>
                {filteredCompanies.map(c => (
                  <div key={c.id} onClick={() => addCompany(c.name)}
                    style={{ padding: '6px 10px', fontSize: 12, cursor: 'pointer' }}
                    onMouseEnter={e => e.target.style.background = 'var(--bg-secondary)'}
                    onMouseLeave={e => e.target.style.background = 'transparent'}
                  >
                    {c.name} <span style={{ color: 'var(--text-secondary)' }}>({c.problemCount})</span>
                  </div>
                ))}
              </div>
            )}
          </div>
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap', marginTop: 6 }}>
            {filters.company.map(c => (
              <span key={c} className="chip">{c} <button onClick={() => removeCompany(c)}>×</button></span>
            ))}
          </div>
        </div>

        <div className="sidebar-section">
          <div className="sidebar-label">Difficulty</div>
          <div className="difficulty-toggles">
            {['EASY', 'MEDIUM', 'HARD'].map(d => (
              <button key={d}
                className={`diff-toggle ${d.toLowerCase()} ${filters.difficulty.includes(d) ? 'active' : ''}`}
                onClick={() => toggleDifficulty(d)}
              >{d.charAt(0) + d.slice(1).toLowerCase()}</button>
            ))}
          </div>
        </div>

        <div className="sidebar-section">
          <div className="sidebar-label">Topic</div>
          <div style={{ position: 'relative' }}>
            <input
              className="search-input"
              placeholder="Select topics..."
              value={topicSearch}
              onChange={e => setTopicSearch(e.target.value)}
              style={{ fontSize: 12 }}
            />
            {filteredTopics.length > 0 && (
              <div style={{
                position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 10,
                background: 'var(--bg-tertiary)', border: '1px solid var(--border)',
                borderRadius: 6, maxHeight: 160, overflowY: 'auto',
              }}>
                {filteredTopics.map(t => (
                  <div key={t.id} onClick={() => addTopic(t.name)}
                    style={{ padding: '6px 10px', fontSize: 12, cursor: 'pointer' }}
                    onMouseEnter={e => e.target.style.background = 'var(--bg-secondary)'}
                    onMouseLeave={e => e.target.style.background = 'transparent'}
                  >
                    {t.name} <span style={{ color: 'var(--text-secondary)' }}>({t.problemCount})</span>
                  </div>
                ))}
              </div>
            )}
          </div>
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap', marginTop: 6 }}>
            {filters.topic.map(t => (
              <span key={t} className="chip">{t} <button onClick={() => removeTopic(t)}>×</button></span>
            ))}
          </div>
        </div>

        <div className="sidebar-section">
          <div className="sidebar-label">Recency</div>
          <div className="recency-group">
            {RECENCY_OPTIONS.map(r => (
              <button key={r.value}
                className={`recency-btn ${filters.recency === r.value ? 'active' : ''}`}
                onClick={() => setFilters(f => ({ ...f, recency: r.value }))}
              >{r.label}</button>
            ))}
          </div>
        </div>

        <div className="sidebar-section">
          <div className="sidebar-label">Frequency</div>
          <div className="freq-range">
            <input className="freq-input" placeholder="0" value={filters.minFrequency}
              onChange={e => setFilters(f => ({ ...f, minFrequency: e.target.value }))} />
            <span style={{ color: 'var(--text-secondary)' }}>—</span>
            <input className="freq-input" placeholder="100" value={filters.maxFrequency}
              onChange={e => setFilters(f => ({ ...f, maxFrequency: e.target.value }))} />
          </div>
        </div>

        <button className="btn-reset" onClick={clearFilters}>Reset Filters</button>
      </div>

      <div className="results-area">
        {loading && <div className="loading">Loading...</div>}
        {results && !loading && (
          <>
            <div className="results-header">
              <span>Showing {results.page * results.size + 1}–{Math.min((results.page + 1) * results.size, results.totalElements)} of {results.totalElements} results</span>
              <span className="results-hint">Click on a title to open the problem on LeetCode</span>
            </div>

            <table className="results-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th style={{ width: 90 }}>Difficulty</th>
                  <th className="sortable" style={{ width: 100 }} onClick={() => toggleSort('frequency')}>Freq {sortIndicator('frequency')}</th>
                  <th>Topics</th>
                  <th>Companies</th>
                </tr>
              </thead>
              <tbody>
                {results.content.map(p => (
                  <tr key={p.id}>
                    <td><a href={p.link} target="_blank" rel="noopener noreferrer">{p.title}</a></td>
                    <td><span className={`difficulty-badge ${p.difficulty}`}>{p.difficulty}</span></td>
                    <td>{(() => {
                      const maxFreq = p.companies.length > 0
                        ? Math.max(...p.companies.map(c => c.frequency || 0))
                        : null
                      if (maxFreq == null) return '—'
                      return (
                        <div className="freq-cell">
                          <div className="freq-bar-inline">
                            <div className="freq-bar-inline-fill" style={{ width: `${maxFreq}%` }} />
                          </div>
                          <span className="freq-value">{maxFreq.toFixed(1)}</span>
                        </div>
                      )
                    })()}</td>
                    <td className="topic-text">{p.topics.slice(0, 3).join(', ')}{p.topics.length > 3 ? '...' : ''}</td>
                    <td>
                      {p.companies.slice(0, 2).map((c, i) => (
                        <span key={i} className="company-badge">{c.name}</span>
                      ))}
                      {p.companies.length > 2 && <span className="more-count"> +{p.companies.length - 2}</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="pagination">
              <button className="page-btn" disabled={results.page === 0}
                onClick={() => setFilters(f => ({ ...f, page: f.page - 1 }))}>←</button>
              {Array.from({ length: Math.min(results.totalPages, 7) }, (_, i) => {
                let pageNum
                if (results.totalPages <= 7) pageNum = i
                else if (results.page < 4) pageNum = i
                else if (results.page > results.totalPages - 5) pageNum = results.totalPages - 7 + i
                else pageNum = results.page - 3 + i
                return (
                  <button key={pageNum} className={`page-btn ${results.page === pageNum ? 'active' : ''}`}
                    onClick={() => setFilters(f => ({ ...f, page: pageNum }))}>{pageNum + 1}</button>
                )
              })}
              <button className="page-btn" disabled={results.page >= results.totalPages - 1}
                onClick={() => setFilters(f => ({ ...f, page: f.page + 1 }))}>→</button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
