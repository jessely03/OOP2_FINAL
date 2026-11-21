const days = ['Fri, Nov 20', 'Sat, Nov 21', 'Sun, Nov 22'];
const members = ['You', 'Jessie', 'Vivi', 'Jason'];

const seed = {
  activities: [
    { id: 'a1', day: 0, time: '14:00', place: '101', title: 'Taipei 101 observatory' },
    { id: 'a2', day: 0, time: '16:30', place: 'elephant', title: 'Sunset at Elephant Mountain' },
    { id: 'a3', day: 0, time: '19:00', place: 'raohe', title: 'Street food at Raohe' },
    { id: 'a4', day: 1, time: '10:00', place: 'longshan', title: 'Visit Longshan Temple' },
    { id: 'a5', day: 1, time: '13:00', place: 'dihua', title: 'Explore Dihua Street' },
    { id: 'a6', day: 2, time: '09:30', place: 'daan', title: 'Morning at Daan Forest Park' }
  ],
  saved: ['elephant', 'raohe'],
  expenses: [{ title: 'Airport taxi', payer: 'Jessie', cents: 120000 }, { title: 'Dinner deposit', payer: 'Vivi', cents: 80000 }],
  messages: [{ author: 'Jessie', text: 'Sunset hike on Friday? Let’s meet at Taipei 101 first.' }, { author: 'Vivi', text: 'I saved Raohe for dinner afterwards!' }]
};
const storageKey = 'navigo-taipei-v1';

let state = structuredClone(seed);
let day = 0;

try {
  const saved = JSON.parse(localStorage.getItem(storageKey));
  if (saved && Object.keys(seed).every(key => Array.isArray(saved[key]))) state = saved;
} catch {
  // The starter data still works when browser storage is unavailable.
}

const today = (index = day) => state.activities.filter(item => item.day === index).sort((a, b) => a.time.localeCompare(b.time));
