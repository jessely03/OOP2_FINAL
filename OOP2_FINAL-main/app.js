/* Shared UI and navigation. Feature scripts load before startup.js. */
const view = document.querySelector('#view');
const pageLabel = document.querySelector('#page-label');
const workspace = document.querySelector('.workspace');
const editor = document.querySelector('#editor');
const form = document.querySelector('#editor-form');
const notice = document.querySelector('#notice');

let editorAction;
let noticeTimer;

const escapeHtml = value => String(value).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
const money = cents => new Intl.NumberFormat('en-TW', { style: 'currency', currency: 'TWD', minimumFractionDigits: cents % 100 ? 2 : 0 }).format(cents / 100);

const external = (url, label, style = 'outline') => `<a class="${style}" href="${escapeHtml(url)}" target="_blank" rel="noopener">${label}</a>`;
const button = (action, label, style = 'outline', id = '') => `<button class="${style}" data-action="${action}" data-id="${id}">${label}</button>`;
const head = (title, subtitle, action = '') => `<div class="section-head"><div><h2>${title}</h2><p>${subtitle}</p></div>${action}</div>`;

function toast(message) {
  notice.textContent = message;
  clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => { notice.textContent = ''; }, 3100);
}

function save(message) {
  try {
    localStorage.setItem(storageKey, JSON.stringify(state));
    toast(message);
  } catch {
    toast('Updated for this session. Browser storage is unavailable.');
  }
  render();
}

function overview() {
  const cards = [
    [state.activities.length, 'Scheduled stops', 'Open itinerary', 'itinerary'],
    [state.saved.length, 'Saved places', 'Explore places', 'explore'],
    [money(state.expenses.reduce((sum, expense) => sum + expense.cents, 0)), 'Shared expenses', 'Open split bill', 'expenses'],
    [members.length, 'Travelers', 'Open group chat', 'chat']
  ];
  return head('Trip overview', 'Jump into a day or pick up where your group left off.', '<a class="primary" href="#itinerary">Open itinerary →</a>') +
    `<section class="overview-days" aria-label="Your three trip days">
       ${days.map((date, index) => {
         const items = today(index);
         const firstStop = items[0] ? escapeHtml(items[0].title) : 'Plan your first stop';
         return `<a class="day-card" href="#itinerary/${index + 1}"><span class="day-card-number" aria-hidden="true">0${index + 1}</span><div><small>Day ${index + 1} · ${items.length} ${items.length === 1 ? 'stop' : 'stops'}</small><h3>${date}</h3><p>${firstStop}</p></div><span class="day-arrow" aria-hidden="true">↗</span></a>`;
       }).join('')}
     </section>
     <div class="tiles overview-tools">${cards.map(([value, title, label, page]) => `<a class="panel tile" href="#${page}"><div class="stat">${value}</div><h3>${title}</h3><span class="tile-link">${label} <span aria-hidden="true">↗</span></span></a>`).join('')}</div>`;
}


function render(focusContent = false) {
  const [route, selectedDay] = location.hash.slice(1).split('/');
  const page = Object.hasOwn(pages, route) ? route : 'overview';
  const dayIndex = Number(selectedDay) - 1;
  if (page === 'itinerary' && Number.isInteger(dayIndex) && days[dayIndex]) day = dayIndex;
  workspace.dataset.page = page;
  document.querySelectorAll('.sidebar a[data-page]').forEach(link => {
    if (link.dataset.page === page) {
      link.setAttribute('aria-current', 'page');
      pageLabel.textContent = link.textContent.slice(1).trim();
    } else {
      link.removeAttribute('aria-current');
    }
  });
  document.title = `${pageLabel.textContent} · NaviGo`;
  view.innerHTML = pages[page]();
  drawMap();
  const messages = view.querySelector('.messages');
  if (messages) messages.scrollTop = messages.scrollHeight;
  if (focusContent) {
    const target = view.querySelector('.day[aria-current]') || view.querySelector('h2');
    target.tabIndex = target.matches('a') ? 0 : -1;
    target.focus();
  }
}

function showEditor(title, fields, onSubmit, deleteButton = '') {
  editorAction = onSubmit;
  form.innerHTML = `<h2 id="editor-title">${title}</h2>${fields}<div class="row">${deleteButton}<button type="button" class="outline" data-action="close">Cancel</button><button class="primary">Save</button></div>`;
  editor.setAttribute('aria-labelledby', 'editor-title');
  editor.showModal();
}

document.addEventListener('click', event => {
  const control = event.target.closest('[data-action]');
  if (!control) return;
  const { action, id } = control.dataset;
  if (action === 'skip') view.focus();
  if (action === 'activity') editActivity(id);
  if (action === 'plan-place') editActivity('', id);
  if (action === 'close') editor.close();

  if (action === 'delete-activity') deleteActivity(id);
  if (action === 'save-place') toggleSavedPlace(id);
  if (action === 'expense') editExpense();
  if (action === 'share') showMembers();
});

form.addEventListener('submit', event => {
  event.preventDefault();
  const action = editorAction;
  if (action && action(new FormData(form)) === false) return;
  editor.close();
  view.querySelector('.section-head button')?.focus();
});
