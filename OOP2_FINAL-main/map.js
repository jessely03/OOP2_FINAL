const categories = { city: '◈', culture: '✦', food: '♨', nature: '▲' };
const places = [
  { id: '101', name: 'Taipei 101', area: 'Xinyi · Skyline', type: 'city', lat: 25.034, lng: 121.565 },
  { id: 'elephant', name: 'Elephant Mountain', area: 'Xinyi · Sunset hike', type: 'nature', lat: 25.027, lng: 121.577 },
  { id: 'raohe', name: 'Raohe Night Market', area: 'Songshan · Street food', type: 'food', lat: 25.051, lng: 121.577 },
  { id: 'longshan', name: 'Longshan Temple', area: 'Wanhua · Heritage', type: 'culture', lat: 25.037, lng: 121.5 },
  { id: 'dihua', name: 'Dihua Street', area: 'Datong · Historic district', type: 'culture', lat: 25.056, lng: 121.51 },
  { id: 'daan', name: 'Daan Forest Park', area: 'Daan · Green space', type: 'nature', lat: 25.03, lng: 121.536 }
];

let placeQuery = '';

const getPlace = id => places.find(place => place.id === id);

const coords = place => `${place.lat},${place.lng}`;
const mapSearch = place => `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(coords(place))}`;
const directions = (from, to, between = []) => {
  const params = new URLSearchParams({ api: '1', origin: coords(from), destination: coords(to), travelmode: 'transit' });
  if (between.length) params.set('waypoints', between.map(coords).join('|'));
  return `https://www.google.com/maps/dir/?${params}`;
};

function drawMap() {
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
  ctx.strokeStyle = '#ffffff';
  ctx.lineWidth = 3;
  ctx.beginPath();
  for (let x = -80; x < width; x += 29) {
    ctx.moveTo(x, 0);
    ctx.lineTo(x + 84, height);
  }
  for (let y = 0; y < height + 65; y += 24) {
    ctx.moveTo(0, y);
    ctx.lineTo(width, y - 65);
  }
  ctx.stroke();

  const stops = today().map(item => getPlace(item.place));
  const points = stops.map(stop => [
    width * (.14 + .72 * (stop.lng - 121.49) / .10),
    height * (.14 + .65 * (25.06 - stop.lat) / .04)
  ]);
  ctx.strokeStyle = '#547e9f';
  ctx.lineWidth = 3;
  ctx.setLineDash([5, 5]);
  ctx.beginPath();
  points.forEach(([x, y], index) => { if (index) ctx.lineTo(x, y); else ctx.moveTo(x, y); });
  ctx.stroke();
  ctx.setLineDash([]);
  const seen = new Map();
  points.forEach(([x, y], index) => {
    const key = stops[index].id;
    const offset = seen.get(key) || 0;
    seen.set(key, offset + 1);
    x += offset * 14;
    ctx.beginPath();
    ctx.arc(x, y, 12, 0, Math.PI * 2);
    ctx.fillStyle = '#305e85';
    ctx.fill();
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 11px system-ui';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(index + 1, x, y);
  });
}

function placeCards() {
  const matches = places.filter(place => `${place.name} ${place.area}`.toLowerCase().includes(placeQuery.trim().toLowerCase()));
  return matches.map(place => `<article class="panel tile"><span class="category ${place.type}" aria-hidden="true">${categories[place.type]}</span><h3>${place.name}</h3><p>${place.area}</p><div class="row">${button('save-place', state.saved.includes(place.id) ? '★ Saved' : '☆ Save', 'outline', place.id)}${button('plan-place', '+ Plan stop', 'quiet', place.id)}</div>${external(mapSearch(place), 'Google Maps ↗', 'quiet')}</article>`).join('') || '<p class="empty">No places match your search.</p>';
}

function explore() {
  return head('Explore places', 'Search the Taipei demo collection and add a stop to your trip.') +
    `<input class="search" type="search" aria-label="Search Taipei places" placeholder="Search by place or neighborhood…" value="${escapeHtml(placeQuery)}"><div id="place-results" class="tiles">${placeCards()}</div>`;
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
  view.querySelector('#place-results').innerHTML = placeCards();
});
