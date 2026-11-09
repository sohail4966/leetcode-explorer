const API_HOST = import.meta.env.VITE_API_URL || '';
const BASE_URL = `${API_HOST}/api/v1`;

async function fetchJson(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return res.json();
}

export async function searchProblems(params) {
  const qs = new URLSearchParams();
  if (params.company?.length) params.company.forEach(c => qs.append('company', c));
  if (params.difficulty?.length) params.difficulty.forEach(d => qs.append('difficulty', d));
  if (params.topic?.length) params.topic.forEach(t => qs.append('topic', t));
  if (params.recency) qs.set('recency', params.recency);
  if (params.search) qs.set('search', params.search);
  if (params.minFrequency != null) qs.set('minFrequency', params.minFrequency);
  if (params.maxFrequency != null) qs.set('maxFrequency', params.maxFrequency);
  if (params.sortBy) qs.set('sortBy', params.sortBy);
  if (params.sortOrder) qs.set('sortOrder', params.sortOrder);
  qs.set('page', params.page ?? 0);
  qs.set('size', params.size ?? 20);
  return fetchJson(`${BASE_URL}/problems?${qs}`);
}

export async function findOverlap(params) {
  const qs = new URLSearchParams();
  params.companies.forEach(c => qs.append('companies', c));
  if (params.difficulty?.length) params.difficulty.forEach(d => qs.append('difficulty', d));
  if (params.topic?.length) params.topic.forEach(t => qs.append('topic', t));
  if (params.recency) qs.set('recency', params.recency);
  qs.set('page', params.page ?? 0);
  qs.set('size', params.size ?? 20);
  return fetchJson(`${BASE_URL}/problems/overlap?${qs}`);
}

export async function getCompanies(search) {
  const qs = search ? `?search=${encodeURIComponent(search)}` : '';
  return fetchJson(`${BASE_URL}/companies${qs}`);
}

export async function getCompanyStats(name) {
  return fetchJson(`${BASE_URL}/companies/${encodeURIComponent(name)}/stats`);
}

export async function getTopics(search) {
  const qs = search ? `?search=${encodeURIComponent(search)}` : '';
  return fetchJson(`${BASE_URL}/topics${qs}`);
}

export async function getSyncStatus() {
  return fetchJson(`${BASE_URL}/sync/status`);
}

export async function triggerSync() {
  const res = await fetch(`${BASE_URL}/sync/trigger`, { method: 'POST' });
  return res.json();
}
