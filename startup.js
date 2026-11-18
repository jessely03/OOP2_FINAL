const pages = { overview, itinerary, calendar, explore, expenses, chat };
window.addEventListener('hashchange', () => render(true));
window.addEventListener('resize', drawMap);
render();
