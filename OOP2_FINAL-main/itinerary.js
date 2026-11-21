function itinerary() {
  const items = today();
  const stops = items.map(item => getPlace(item.place));
  const route = stops.length > 1 ? directions(stops[0], stops.at(-1), stops.slice(1, -1)) : stops.length ? mapSearch(stops[0]) : '';
  return head('Itinerary', 'Your day-by-day plan, all in one place.', button('activity', '+ Add activity', 'primary')) +
    `<nav class="days" aria-label="Trip days">${days.map((label, index) => `<a class="day" href="#itinerary/${index + 1}" ${index === day ? 'aria-current="true"' : ''}>Day ${index + 1}<small>${label}</small></a>`).join('')}</nav>
    <div class="columns">
      <section class="panel"><header class="panel-head"><div><h3>${days[day]}</h3><p>${items.length} scheduled ${items.length === 1 ? 'stop' : 'stops'}</p></div><a class="quiet" href="#calendar">Calendar ↗</a></header>
        ${items.map(item => {
          const place = getPlace(item.place);
          return `<div class="activity"><time>${item.time}</time><span class="category ${place.type}" aria-hidden="true">${categories[place.type]}</span><div><strong>${escapeHtml(item.title)}</strong><small>${place.area}</small></div>${button('activity', 'Edit', 'quiet', item.id)}</div>`;
        }).join('') || '<p class="empty">No stops yet. Add an activity to plan this day.</p>'}
      </section>
      <section class="panel"><header class="panel-head"><div><h3>Day route</h3><p>Stops in itinerary order</p></div>${route ? external(route, 'Google Maps ↗') : ''}</header>
        <div class="map"><canvas aria-label="Schematic preview of scheduled stops" role="img"></canvas><span class="map-note">Schematic preview</span></div>
        <div class="route-links">${stops.slice(1).map((stop, index) => external(directions(stops[index], stop), `Transit ${index + 1} → ${index + 2} ↗`, 'quiet')).join('')}</div>
      </section>
    </div>`;
}


function calendar() {
  return head('Trip calendar', 'See the schedule across all three days.') +
    `<div class="tiles calendar">${days.map((date, index) => `<section class="panel"><div class="panel-head"><div><h3>${date}</h3><p>Day ${index + 1}</p></div><a class="quiet" href="#itinerary/${index + 1}" aria-label="Open itinerary for ${date}">Open ↗</a></div>${today(index).map(item => `<div class="activity"><time>${item.time}</time><strong>${escapeHtml(item.title)}</strong></div>`).join('') || '<p class="empty">No activities yet.</p>'}</section>`).join('')}</div>`;
}

function editActivity(id, selectedPlace) {
  const existing = state.activities.find(item => item.id === id);
  const item = existing || { day, time: '10:00', place: selectedPlace || places[0].id, title: selectedPlace ? getPlace(selectedPlace).name : '' };
  showEditor(existing ? 'Edit activity' : 'Add activity', `
    <label>Activity name<input name="title" required maxlength="80" value="${escapeHtml(item.title)}"></label>
    <label>Day<select name="day">${days.map((date, index) => `<option value="${index}" ${index === item.day ? 'selected' : ''}>${date}</option>`).join('')}</select></label>
    <label>Time<input type="time" name="time" required value="${item.time}"></label>
    <label>Place<select name="place">${places.map(place => `<option value="${place.id}" ${place.id === item.place ? 'selected' : ''}>${place.name}</option>`).join('')}</select></label>`, data => {
    const title = data.get('title').trim();
    if (!title) { toast('Enter an activity name.'); return false; }
    const updated = { id: existing?.id || `a-${Date.now()}`, title, day: Number(data.get('day')), time: data.get('time'), place: data.get('place') };
    if (existing) state.activities[state.activities.indexOf(existing)] = updated;
    else state.activities.push(updated);
    day = updated.day;
    state.messages.push({ author: 'Itinerary update', text: `${existing ? 'Updated' : 'Added'} ${title} · ${days[day]} at ${updated.time}`, page: 'itinerary' });
    location.hash = `itinerary/${day + 1}`;
    save(existing ? 'Activity updated' : 'Activity added');
  }, existing ? `<button type="button" class="quiet" data-action="delete-activity" data-id="${id}">Delete</button>` : '');
}

function deleteActivity(id) {
  const item = state.activities.find(activity => activity.id === id);
  state.activities = state.activities.filter(activity => activity.id !== id);
  state.messages.push({ author: 'Itinerary update', text: `Removed ${item.title} from ${days[item.day]}.`, page: 'itinerary' });
  editor.close();
  save('Activity removed');
  view.querySelector('[data-action="activity"]')?.focus();
}
