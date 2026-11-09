import { useState, useEffect } from 'react'
import './App.css'
import { getSyncStatus } from './api/client'
import SearchTab from './components/SearchTab'
import CompareTab from './components/CompareTab'
import CompaniesTab from './components/CompaniesTab'
import TopicsTab from './components/TopicsTab'

const TABS = [
  { id: 'search', label: 'Search', icon: '🔍' },
  { id: 'compare', label: 'Compare', icon: '🔄' },
  { id: 'companies', label: 'Companies', icon: '🏢' },
  { id: 'topics', label: 'Topics', icon: '🏷️' },
]

export default function App() {
  const [activeTab, setActiveTab] = useState('search')
  const [syncStatus, setSyncStatus] = useState(null)
  const [searchCompany, setSearchCompany] = useState(null)
  const [searchTopic, setSearchTopic] = useState(null)
  const [theme, setTheme] = useState(() => localStorage.getItem('theme') || 'dark')

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('theme', theme)
  }, [theme])

  useEffect(() => {
    getSyncStatus().then(setSyncStatus).catch(() => {})
  }, [])

  const statusClass = syncStatus?.status?.toLowerCase() || 'unknown'
  const lastSync = syncStatus?.lastSyncedAt
    ? new Date(syncStatus.lastSyncedAt).toLocaleString()
    : 'Never'

  return (
    <div className="app">
      <div className="top-bar">
        <div className="top-bar-stats">
          <span style={{ fontWeight: 600 }}>LeetCode Explorer</span>
          <span className="disclaimer">Data sourced from <a href="https://github.com/liquidslr/leetcode-company-wise-problems" target="_blank" rel="noopener noreferrer">liquidslr/leetcode-company-wise-problems</a></span>
        </div>
        <div className="top-bar-right">
          <div className="sync-status">
            <span className={`sync-dot ${statusClass}`} />
            <span>Last sync: {lastSync}</span>
          </div>
          <button className="theme-toggle" onClick={() => setTheme(t => t === 'dark' ? 'light' : 'dark')}>
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>
        </div>
      </div>

      <div className="tabs">
        {TABS.map(tab => (
          <div
            key={tab.id}
            className={`tab ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => { setSearchCompany(null); setSearchTopic(null); setActiveTab(tab.id) }}
          >
            {tab.icon} {tab.label}
          </div>
        ))}
      </div>

      {activeTab === 'search' && <SearchTab initialCompany={searchCompany} initialTopic={searchTopic} />}
      {activeTab === 'compare' && <CompareTab />}
      {activeTab === 'companies' && <CompaniesTab onViewProblems={(name) => { setSearchCompany(name); setActiveTab('search') }} />}
      {activeTab === 'topics' && <TopicsTab onViewTopic={(name) => { setSearchTopic(name); setActiveTab('search') }} />}
    </div>
  )
}
