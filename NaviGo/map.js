/* map.js - Connected to NaviGo Java Backend (http://localhost:8080) */
const JAVA_API_BASE = 'http://localhost:8080';

const categories = { city: '◈', culture: '✦', food: '♨', nature: '▲' };

const defaultPlaces = [
  { id: '101', name: 'Taipei 101', area: 'Xinyi · Skyline', type: 'city', lat: 25.034, lng: 121.565 },
  { id: 'elephant', name: 'Elephant Mountain', area: 'Xinyi · Sunset hike', type: 'nature', lat: 25.027, lng: 121.577 },
  { id: 'raohe', name: 'Raohe Night Market', area: 'Songshan · Street food', type: 'food', lat: 25.051, lng: 121.577 },
  { id: 'longshan', name: 'Longshan Temple', area: 'Wanhua · Heritage', type: 'culture', lat: 25.037, lng: 121.5 },
  { id: 'dihua', name: 'Dihua Street', area: 'Datong · Historic district', type: 'culture', lat: 25.056, lng: 121.51 },
  { id: 'daan', name: 'Daan Forest Park', area: 'Daan · Green space', type: 'nature', lat: 25.03, lng: 121.536 }
];

// Persist discovered Google Maps places so itinerary.js can always resolve getPlace(id)
const placeCacheKey = 'navigo-places-cache-v1';
let places = [...defaultPlaces];
try {
  const cached = JSON.parse(localStorage.getItem(placeCacheKey));
  if (Array.isArray(cached) && cached.length) places = cached;
} catch {}

let searchResults = [...defaultPlaces];
let placeQuery = '';
let activeCategory = 'all';
let searchDebounce;

const getPlace = id => places.find(place => place.id === id) || defaultPlaces[0];

const coords = place => `${place.lat},${place.lng}`;
const mapSearch = place => `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(place.name + ' ' + coords(place))}`;
const directions = (from, to, between = []) => {
  const params = new URLSearchParams({ api: '1', origin: coords(from), destination: coords(to), travelmode: 'transit' });
  if (between.length) params.set('waypoints', between.map(coords).join('|'));
  return `https://www.google.com/maps/dir/?${params}`;
};

function cacheDiscoveredPlaces(newItems) {
  newItems.forEach(item => {
    if (!places.some(p => p.id === item.id)) {
      places.push(item);
    }
  });
  try {
    localStorage.setItem(placeCacheKey, JSON.stringify(places));
  } catch {}
}

/**
 * Renders the Itinerary Day Map using your Java Google Static Maps endpoint,
 * with automatic fallback to the local canvas renderer if the Java server isn't running.
 */
function drawMap() {
  const mapContainer = view.querySelector('.map');
  if (!mapContainer) return;

  const stops = today().map(item => getPlace(item.place)).filter(Boolean);
  if (stops.length > 0) {
    const stopsQuery = stops.map(coords).join('|');
    const javaMapUrl = `${JAVA_API_BASE}/api/map/static?stops=${encodeURIComponent(stopsQuery)}`;

    let img = mapContainer.querySelector('img.java-map-img');
    if (!img) {
      img = document.createElement('img');
      img.className = 'java-map-img';
      img.alt = 'Google Map of Taipei stops';
      img.style.cssText = 'width:100%;height:100%;object-fit:cover;display:block;';
      img.onerror = () => {
        img.remove();
        drawFallbackCanvas();
      };
      mapContainer.prepend(img);
    }
    img.src = javaMapUrl;
  } else {
    drawFallbackCanvas();
  }
}

function drawFallbackCanvas() {
  const canvas = view.querySelector('canvas');
  if (!canvas) return;
  const { width, height } = canvas.getBoundingClientRect();
  const ctx = canvas.getContext('2d');
  if (!ctx || !width || !height) return;
  const density = devicePixelRatio || 1;
  canvas.width = width * density;
  canvas.height = height * density;
  ctx.scale(density, density);
  ctx.fillStyle = '#e5eff5';
  ctx.fillRect(0, 0, width, height);
  ctx.fillStyle = '#dceee6';
  ctx.beginPath();
  ctx.ellipse(width * .82, height * .85, width * .43, 43, -.35, 0, Math.PI * 2);
  ctx.fill();
}

function placeCards() {
  const filtered = activeCategory === 'all'
    ? searchResults
    : searchResults.filter(p => p.type === activeCategory);

  return filtered.map(place => `
    <article class="panel tile">
      <span class="category ${place.type}" aria-hidden="true">${categories[place.type] || '◈'}</span>
      <h3>${escapeHtml(place.name)}</h3>
      <p>${escapeHtml(place.area)}</p>
      <div class="row">
        ${button('save-place', state.saved.includes(place.id) ? '★ Saved' : '☆ Save', 'outline', place.id)}
        ${button('plan-place', '+ Plan stop', 'quiet', place.id)}
      </div>
      ${external(mapSearch(place), 'Google Maps ↗', 'quiet')}
    </article>
  `).join('') || '<p class="empty">No Taipei places match your search. Try another keyword!</p>';
}

function explore() {
  const filterButtons = [
    ['all', 'All spots'],
    ['food', '♨ Food & Night Markets'],
    ['culture', '✦ Culture'],
    ['nature', '▲ Nature'],
    ['city', '◈ City']
  ].map(([key, label]) =>
    `<button type="button" class="${activeCategory === key ? 'primary' : 'outline'}" data-filter="${key}">${label}</button>`
  ).join('');

  return head('Explore places', 'Search real Taipei locations via Google Maps and add stops to your trip.') +
    `<div class="row" style="margin-bottom:12px; flex-wrap:wrap;">
       <input class="search" style="margin-bottom:0; flex:1; min-width:240px;" type="search" aria-label="Search Taipei places" placeholder="Search Google Maps (e.g., Shilin Night Market, cocktail bar, ramen)…" value="${escapeHtml(placeQuery)}">
       <div class="row" style="gap:6px; flex-wrap:wrap;">${filterButtons}</div>
     </div>
     <div id="place-results" class="tiles">${placeCards()}</div>`;
}

async function queryJavaPlacesBackend(query) {
  const resultsEl = view.querySelector('#place-results');
  if (!query.trim()) {
    searchResults = [...defaultPlaces];
    if (resultsEl) resultsEl.innerHTML = placeCards();
    return;
  }

  if (resultsEl) resultsEl.innerHTML = '<p class="empty">Researching Taipei places on Google Maps…</p>';

  try {
    const res = await fetch(`${JAVA_API_BASE}/api/places/search?query=${encodeURIComponent(query + ' Taipei')}`);
    if (!res.ok) throw new Error('Backend error');
    const data = await res.json();
    if (Array.isArray(data) && data.length > 0) {
      cacheDiscoveredPlaces(data);
      searchResults = data;
    } else {
      searchResults = [];
    }
  } catch {
    // Fallback to local filtering if Java server is not running yet
    searchResults = places.filter(p => `${p.name} ${p.area}`.toLowerCase().includes(query.trim().toLowerCase()));
  }

  if (view.querySelector('#place-results')) {
    view.querySelector('#place-results').innerHTML = placeCards();
  }
}

function toggleSavedPlace(id) {
  const wasSaved = state.saved.includes(id);
  state.saved = wasSaved ? state.saved.filter(place => place !== id) : [...state.saved, id];
  save(wasSaved ? 'Place removed from saved' : 'Place saved');
  view.querySelector(`[data-action="save-place"][data-id="${id}"]`)?.focus();
}

view.addEventListener('input', event => {
  if (!event.target.matches('.search')) return;
  placeQuery = event.target.value;
  clearTimeout(searchDebounce);
  searchDebounce = setTimeout(() => queryJavaPlacesBackend(placeQuery), 350);
});

view.addEventListener('click', event => {
  const filterBtn = event.target.closest('[data-filter]');
  if (!filterBtn) return;
  activeCategory = filterBtn.dataset.filter;
  render();
});
