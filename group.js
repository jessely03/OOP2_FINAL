function chat() {
  return head('Group chat', 'Local demo conversation and trip updates.') +
    `<section class="panel"><div class="messages" aria-label="Trip messages">${state.messages.map(message => `<article class="message" ${message.page ? 'data-update' : ''}><strong>${message.author}</strong><p>${escapeHtml(message.text)}</p>${message.page ? `<a class="quiet" href="#${message.page}">View update ↗</a>` : ''}</article>`).join('')}</div>
    <form class="message-form"><input name="message" aria-label="Message" maxlength="500" required placeholder="Write to the group…"><button class="primary">Send</button></form></section>`;
}

function showMembers() {
  editorAction = null;
  form.innerHTML = `<h2 id="editor-title">Trip members</h2><p>This demo has four sample travelers. Shared invites and live sync need a backend.</p>
    ${members.map(member => `<div class="line-item">${member}</div>`).join('')}<button type="button" class="primary" data-action="close">Done</button>`;
  editor.setAttribute('aria-labelledby', 'editor-title');
  editor.showModal();
}

view.addEventListener('submit', event => {
  if (!event.target.matches('.message-form')) return;
  event.preventDefault();
  const text = new FormData(event.target).get('message').trim();
  if (!text) return;
  state.messages.push({ author: 'You', text });
  save('Message added to the demo chat');
  view.querySelector('[name="message"]').focus();
});
