function expenses() {
  const balances = Object.fromEntries(members.map(member => [member, 0]));
  for (const item of state.expenses) {
    balances[item.payer] += item.cents;
    const share = Math.floor(item.cents / members.length);
    const remainder = item.cents % members.length;
    members.forEach((member, index) => { balances[member] -= share + Number(index < remainder); });
  }
  const creditors = members.filter(member => balances[member] > 0);
  const debtors = members.filter(member => balances[member] < 0);
  const payments = [];
  for (const debtor of debtors) {
    for (const creditor of creditors) {
      const amount = Math.min(-balances[debtor], balances[creditor]);
      if (amount <= 0) continue;
      payments.push(`<div class="line-item row"><span>${debtor} → ${creditor}</span><strong>${money(amount)}</strong></div>`);
      balances[debtor] += amount;
      balances[creditor] -= amount;
    }
  }
  return head('Split bill', 'Shared expenses are divided equally among four travelers.', button('expense', '+ Add expense', 'primary')) +
    `<div class="columns"><section class="panel"><div class="panel-head"><h3>Expenses</h3><strong>${money(state.expenses.reduce((sum, item) => sum + item.cents, 0))}</strong></div>${state.expenses.map(item => `<div class="line-item row"><div><strong>${escapeHtml(item.title)}</strong><small>Paid by ${item.payer}</small></div><strong>${money(item.cents)}</strong></div>`).join('')}</section><section class="panel"><div class="panel-head"><h3>Who pays whom</h3></div>${payments.join('') || '<p class="empty">Everyone is settled up.</p>'}</section></div>`;
}

function editExpense() {
  showEditor('Add expense', `<label>Description<input required name="title" maxlength="80"></label>
    <label>Amount (TWD)<input required type="number" name="amount" min="0.01" max="10000000" step="0.01"></label>
    <label>Paid by<select name="payer">${members.map(member => `<option>${member}</option>`).join('')}</select></label>`, data => {
    const title = data.get('title').trim();
    if (!title) { toast('Enter an expense description.'); return false; }
    const cents = Math.round(Number(data.get('amount')) * 100);
    state.expenses.push({ title, cents, payer: data.get('payer') });
    state.messages.push({ author: 'Expense update', text: `Added ${title} · ${money(cents)}`, page: 'expenses' });
    save('Expense added');
  });
}
