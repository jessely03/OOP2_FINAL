const screens = [
  ['home', 'Overview', '⌂'],
  ['calendar', 'Trip Calendar', '▦'],
  ['explore', 'Explore Map', '⌖'],
  ['expenses', 'Expenses', '▤'],
  ['group', 'Group Chat', '◫']
];
const seed = {
  id: 'taiwan', name: 'Taiwan Trip', destination: 'Taipei, Taiwan',
  start: '2025-12-20', end: '2025-12-25', members: ['You', 'Alice', 'Daniel', 'Emma'],
  activities: [
    { id: 'flight', name: 'Flight to Taipei', location: 'Taoyuan International Airport', date: '2025-12-20', time: '09:00', category: 'Transit' },
    { id: 'temple', name: 'Visit Longshan Temple', location: 'Wanhua District', date: '2025-12-20', time: '13:00', category: 'Explore' },
    { id: 'dinner', name: 'Dinner at Taipei 101', location: 'Xinyi District', date: '2025-12-20', time: '19:00', category: 'Food' }
  ]
};
const $ = selector => document.querySelector(selector);
const storageKey = 'navigo.member1.v1';
let storageAvailable = true;
let saved;
try {
  saved = JSON.parse(localStorage.getItem(storageKey));
} catch {
  storageAvailable = false;
}
const trips = Array.isArray(saved?.trips) && saved.trips.length ? saved.trips : [seed];
let trip = trips.find(item => item.id === saved?.active) || trips[0];
let selectedDate = trip.start;
let month = trip.start.slice(0, 7);
let editingId = null;
let statusTimer;
const main = $('#main');
const tripForm = $('#trip-form');
const activityForm = $('#activity-form');
const tripDialog = $('#trip-dialog');
const activityDialog = $('#activity-dialog');

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[character]);
}

function dateLabel(value, options = { month: 'short', day: 'numeric' }) {
  return new Date(value + 'T12:00:00').toLocaleDateString('en-US', options);
}

function dateRange(item) {
  return dateLabel(item.start) + ' – ' + dateLabel(item.end, { month: 'short', day: 'numeric', year: 'numeric' });
}

function notify(message) {
  $('#status').textContent = message;
  clearTimeout(statusTimer);
  statusTimer = setTimeout(() => { $('#status').textContent = ''; }, 3500);
}

function save(message = '') {
  try {
    localStorage.setItem(storageKey, JSON.stringify({ trips, active: trip.id }));
    storageAvailable = true;
  } catch {
    storageAvailable = false;
  }
  if (!storageAvailable) notify('Browser storage is unavailable. Changes last until you close or reload this page.');
  else if (message) notify(message);
}

function setTrip(item) {
  trip = item;
  selectedDate = item.start;
  month = item.start.slice(0, 7);
}

function route() {
  const hash = location.hash.slice(1);
  return hash === 'welcome' || screens.some(([key]) => key === hash) ? hash : 'home';
}

function render() {
  const page = route();
  $('#navigation').innerHTML = '<p class="nav-label">Workspace</p>' + screens.map(([key, label, icon]) =>
    '<a href="#' + key + '"' + (page === key ? ' aria-current="page"' : '') +
    '><span class="nav-icon" aria-hidden="true">' + icon + '</span>' + label + '</a>'
  ).join('');
  const title = screens.find(([key]) => key === page)?.[1] || 'Welcome';
  $('#breadcrumb').textContent = 'Workspace / ' + title;
  document.title = title + ' | NaviGo';
  $('#trip-select').replaceChildren(...trips.map(item => new Option(item.name, item.id, false, item.id === trip.id)));
  const views = { welcome, home, calendar, explore, expenses, group };
  main.innerHTML = views[page]();
}

function welcome() {
  return '<section class="hero welcome-cover"><div class="hero-content"><p class="eyebrow light">NaviGo trip planner</p>' +
    '<h1>Plan together.<br>Explore together.</h1><p>Organize your itinerary, stops, and group plans in one trip workspace.</p>' +
    '<a class="button lime" href="#home">Get started</a></div></section>';
}

function home() {
  const firstDate = trip.activities.map(item => item.date).sort()[0] || trip.start;
  const memberCount = trip.members.length;
  return '<div class="page-heading"><div><p class="eyebrow">Trip workspace</p><h1>My Trips</h1>' +
    '<p>Pick a trip or start planning your next one.</p></div><button class="button primary" data-create>+ New trip</button></div>' +
    '<section class="hero ' + (trip.id === 'taiwan' ? 'taipei' : 'trip-cover') + '"><div class="hero-content">' +
    '<span class="hero-tag">⌖ ' + escapeHtml(trip.destination) + '</span><h2>' + escapeHtml(trip.name) + '</h2>' +
    '<p>' + dateRange(trip) + ' <span class="separator">•</span> ' + memberCount + (memberCount === 1 ? ' traveler' : ' travelers') + '</p>' +
    '<a class="button lime" href="#calendar">Open itinerary</a></div></section>' +
    '<div class="dashboard-grid"><section class="panel"><div class="section-head"><div><p class="eyebrow">Itinerary</p><h2>' +
    dateLabel(firstDate, { weekday: 'long', month: 'short', day: 'numeric' }) +
    '</h2></div><a class="text-link" href="#calendar">View calendar</a></div>' + activityList(firstDate) +
    '</section><section class="panel tools-panel"><div class="section-head"><div><p class="eyebrow">Trip tools</p><h2>Plan with your group</h2></div></div>' +
    toolLink('explore', 'Find and save places', 'mint') + toolLink('expenses', 'Review shared costs', 'peach') +
    toolLink('group', 'See the trip conversation', 'purple') + '</section></div>';
}

function toolLink(key, description, color) {
  const [, label, icon] = screens.find(screen => screen[0] === key);
  return '<a class="tool-link" href="#' + key + '"><span class="tile ' + color + '" aria-hidden="true">' +
    icon + '</span><span><strong>' + label + '</strong><small>' + description + '</small></span></a>';
}

function activityList(date) {
  const items = trip.activities.filter(item => item.date === date).sort((a, b) => a.time.localeCompare(b.time));
  if (!items.length) return '<p class="empty">No activities scheduled for this day.</p>';
  const styles = { Transit: ['✈', 'purple'], Explore: ['⌖', 'mint'], Food: ['♨', 'peach'] };
  return '<ul class="activities">' + items.map(item => {
    const [icon, color] = styles[item.category];
    return '<li><button class="activity" data-edit="' + item.id + '"><time>' + item.time + '</time>' +
      '<span class="tile ' + color + '" aria-hidden="true">' + icon + '</span><span><strong>' + escapeHtml(item.name) +
      '</strong><small>' + escapeHtml(item.location) + '</small></span><span class="edit-mark" aria-label="Edit">✎</span></button></li>';
  }).join('') + '</ul>';
}

function calendar() {
  const [year, number] = month.split('-').map(Number);
  const offset = (new Date(year, number - 1, 1).getDay() + 6) % 7;
  const count = new Date(year, number, 0).getDate();
  let days = '<span aria-hidden="true"></span>'.repeat(offset);
  for (let day = 1; day <= count; day++) {
    const date = month + '-' + String(day).padStart(2, '0');
    const active = date === selectedDate;
    const inTrip = date >= trip.start && date <= trip.end;
    const hasActivity = trip.activities.some(item => item.date === date);
    days += '<button data-date="' + date + '" class="day' + (inTrip ? ' in-trip' : '') +
      (hasActivity ? ' has-activity' : '') + '" aria-pressed="' + active + '" aria-label="' +
      dateLabel(date, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' }) +
      (hasActivity ? ', has activities' : '') + '">' + day + '</button>';
  }
  return '<div class="page-heading"><div><p class="eyebrow">' + escapeHtml(trip.name) +
    '</p><h1>Trip Calendar</h1><p>' + dateRange(trip) + '</p></div><button class="button primary" data-add>+ Add activity</button></div>' +
    '<div class="dashboard-grid calendar-grid"><section class="panel"><div class="section-head"><h2>' +
    dateLabel(month + '-01', { month: 'long', year: 'numeric' }) + '</h2><div class="month-controls">' +
    '<button class="icon-button" data-month="-1" aria-label="Previous month">‹</button><button class="icon-button" data-month="1" aria-label="Next month">›</button>' +
    '</div></div><div class="calendar">' + ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(day =>
      '<span class="weekday">' + day + '</span>').join('') + days +
    '</div><p class="calendar-key"><span></span> Trip dates</p></section>' +
    '<section class="panel"><div class="section-head"><div><p class="eyebrow">Daily itinerary</p><h2>' +
    dateLabel(selectedDate, { weekday: 'long', month: 'short', day: 'numeric' }) +
    '</h2></div></div>' + activityList(selectedDate) + '</section></div>';
}

function demoPage(key, description, contents) {
  const [, title] = screens.find(screen => screen[0] === key);
  return '<div class="page-heading"><div><p class="eyebrow">' + escapeHtml(trip.name) + '</p><h1>' +
    title + '</h1><p>' + description + '</p></div></div>' +
    '<section class="panel module-preview"><p class="eyebrow">Layout preview</p>' + contents + '</section>';
}

function explore() {
  const places = trip.id === 'taiwan'
    ? '<div class="map-preview" role="img" aria-label="Illustrative map with two saved places in Taipei">' +
      '<span class="map-marker temple">1</span><span class="map-marker tower">2</span></div>' +
      '<ul class="preview-list"><li><strong>1. Longshan Temple</strong><span>Wanhua</span></li>' +
      '<li><strong>2. Taipei 101</strong><span>Xinyi</span></li></ul>'
    : '<p class="empty">No saved places in this trip.</p>';
  return demoPage('explore', 'Saved destinations and routes for your trip.', '<h2>Saved places</h2>' + places);
}

function expenses() {
  const costs = trip.id === 'taiwan'
    ? '<div class="amount">NT$240</div><ul class="preview-list">' + trip.members.map(name =>
      '<li><strong>' + escapeHtml(name) + '</strong><span>NT$60</span></li>').join('') + '</ul>'
    : '<p class="empty">No shared expenses in this trip.</p>';
  return demoPage('expenses', 'A preview of shared costs and each traveler’s share.', '<h2>Split bill</h2>' + costs);
}

function group() {
  const messages = trip.id === 'taiwan'
    ? '<div class="messages"><p><strong>Alice</strong>Let’s meet at 9am!</p><p><strong>Daniel</strong>I added a new cafe!</p></div>'
    : '<p class="empty">No messages in this trip.</p>';
  return demoPage('group', trip.members.map(escapeHtml).join(' · '), '<h2>Trip conversation</h2>' + messages);
}

function openActivity(id) {
  editingId = id || null;
  const item = trip.activities.find(activity => activity.id === id);
  activityForm.reset();
  for (const input of activityForm.querySelectorAll('input')) input.setCustomValidity('');
  const fields = activityForm.elements;
  fields.date.min = trip.start;
  fields.date.max = trip.end;
  fields.date.value = item?.date || (selectedDate >= trip.start && selectedDate <= trip.end ? selectedDate : trip.start);
  fields.time.value = item?.time || '09:00';
  fields.name.value = item?.name || '';
  fields.location.value = item?.location || '';
  fields.category.value = item?.category || 'Explore';
  $('#activity-heading').textContent = item ? 'Edit activity' : 'Add activity';
  $('#delete-activity').hidden = !item;
  activityDialog.showModal();
}

function finishActivity(message) {
  activityDialog.close();
  save(message);
  render();
  main.focus();
}

$('#trip-select').addEventListener('change', event => {
  setTrip(trips.find(item => item.id === event.target.value));
  save();
  render();
});

document.addEventListener('click', event => {
  const button = event.target.closest('button');
  if (!button) return;
  const action = button.dataset;
  if ('close' in action) button.closest('dialog').close();
  if ('create' in action) {
    tripForm.reset();
    for (const input of tripForm.querySelectorAll('input')) input.setCustomValidity('');
    tripDialog.showModal();
  }
  if ('add' in action || action.edit) openActivity(action.edit);
  if (action.date) {
    selectedDate = action.date;
    render();
    document.querySelector('[data-date="' + selectedDate + '"]').focus();
  }
  if (action.month) {
    const [year, number] = month.split('-').map(Number);
    const nextMonth = new Date(year, number - 1 + Number(action.month), 1);
    month = nextMonth.getFullYear() + '-' + String(nextMonth.getMonth() + 1).padStart(2, '0');
    selectedDate = month === trip.start.slice(0, 7) ? trip.start : month + '-01';
    render();
    document.querySelector('[data-month="' + action.month + '"]').focus();
  }
});

function readName(form, key) {
  const field = form.elements[key];
  const value = field.value.trim();
  field.setCustomValidity(value ? '' : 'Please enter a value.');
  return value;
}

for (const form of [tripForm, activityForm]) {
  form.addEventListener('input', event => {
    event.target.setCustomValidity('');
    if (form === tripForm) tripForm.elements.end.setCustomValidity('');
  });
}

tripForm.addEventListener('submit', event => {
  event.preventDefault();
  const name = readName(tripForm, 'name');
  const destination = readName(tripForm, 'destination');
  const { start, end } = Object.fromEntries(new FormData(tripForm));
  tripForm.elements.end.setCustomValidity(end >= start ? '' : 'End date must be on or after the start date.');
  if (!tripForm.reportValidity()) return;
  const item = { id: String(Date.now()), name, destination, start, end, members: ['You'], activities: [] };
  trips.push(item);
  setTrip(item);
  tripDialog.close();
  save('Trip created.');
  render();
  main.focus();
});

activityForm.addEventListener('submit', event => {
  event.preventDefault();
  const name = readName(activityForm, 'name');
  const location = readName(activityForm, 'location');
  if (!activityForm.reportValidity()) return;
  const { date, time, category } = Object.fromEntries(new FormData(activityForm));
  const item = { id: editingId || String(Date.now()), name, location, date, time, category };
  const index = trip.activities.findIndex(activity => activity.id === editingId);
  if (index < 0) trip.activities.push(item);
  else trip.activities[index] = item;
  selectedDate = date;
  month = date.slice(0, 7);
  finishActivity('Activity saved.');
});

$('#delete-activity').addEventListener('click', () => {
  trip.activities = trip.activities.filter(item => item.id !== editingId);
  finishActivity('Activity removed.');
});

$('.skip').addEventListener('click', event => {
  event.preventDefault();
  main.focus();
});

window.addEventListener('hashchange', () => {
  render();
  main.focus();
  window.scrollTo(0, 0);
});

render();
