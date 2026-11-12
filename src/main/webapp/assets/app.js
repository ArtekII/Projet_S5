/* Progressive enhancements: all navigation and existing submissions work without JS. */
(() => {
 'use strict';
 const all = (selector, root = document) => Array.from(root.querySelectorAll(selector));
 let sequence = 0;
 const uid = prefix => `${prefix}-${++sequence}`;
 const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('fr');
 const element = (tag, className, text) => {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
 };

 // Keep location, history and the selected menu entry in agreement.
 const locationUrl = new URL(window.location.href);
 const currentView = document.body.dataset.currentView ?? (locationUrl.searchParams.get('vue') || '');
 const offerDetail = /\/rh\/offre\/\d+\/candidats$/.test(locationUrl.pathname);
 all('[data-nav-link]').forEach(link => {
  const url = new URL(link.href);
  const samePage = url.pathname === locationUrl.pathname || locationUrl.pathname.startsWith(url.pathname + '/')
   || (offerDetail && url.pathname.endsWith('/rh/dashboard'));
  const sameView = (url.searchParams.get('vue') || '') === (offerDetail ? 'historique' : currentView);
  const sameTab = (url.searchParams.get('onglet') || 'offres') === (locationUrl.searchParams.get('onglet') || 'offres');
  if (samePage && sameView && sameTab) link.setAttribute('aria-current', 'page');
 });
 const navigation = document.querySelector('.app-navigation');
 if (navigation) {
  const mobile = window.matchMedia('(max-width: 980px)');
  const adapt = () => { navigation.open = !mobile.matches; };
  adapt();
  mobile.addEventListener('change', adapt);
 }
 all('.app-space-switch').forEach(menu => {
  document.addEventListener('click', event => { if (!menu.contains(event.target)) menu.open = false; });
  menu.addEventListener('keydown', event => {
   if (event.key === 'Escape') { menu.open = false; menu.querySelector('summary').focus(); }
  });
 });

 // Real links update the URL; the candidate's radio panels remain a no-script fallback.
 all('[data-candidate-tab]').forEach(link => {
  const active = document.getElementById(`v-${link.dataset.candidateTab}`);
  if (active?.checked) link.setAttribute('aria-current', 'page');
 });

 // Search, status filtering, sorting and pagination operate only on rendered records.
 all('[data-collection]').forEach(collection => {
  const items = all('[data-item]', collection);
  if (!items.length) return;
  if (items.some(item => item.hasAttribute('data-priority'))) items.sort((a,b) => Number(a.dataset.priority || 0)-Number(b.dataset.priority || 0));
  const toolbar = element('div', 'app-toolbar');
  const field = (title, control) => {
   const group = element('div');
   control.id = uid('filtre');
   const label = element('label', 'app-field-label', title);
   label.htmlFor = control.id;
   group.append(label, control);
   toolbar.append(group);
  };
  const search = element('input', 'form-control');
  search.type = 'search'; search.placeholder = collection.dataset.searchPlaceholder || 'Rechercher…';
  field('Rechercher', search);
  const state = element('select', 'form-select');
  state.add(new Option(collection.dataset.stateLabel === 'Département' ? 'Tous les départements' : 'Tous les statuts', ''));
  [...new Set(items.map(item => item.dataset.state).filter(Boolean))].sort((a,b) => a.localeCompare(b,'fr')).forEach(value => state.add(new Option(value,value)));
  if (state.options.length > 1) field(collection.dataset.stateLabel || 'Statut', state);
  const sort = element('select', 'form-select');
  sort.add(new Option(collection.dataset.orderLabel || 'Ordre initial', 'initial'));
  sort.add(new Option('Ordre alphabétique', 'alpha'));
  field('Trier', sort);
  const count = element('p', 'app-filter-count');
  count.setAttribute('role','status'); count.setAttribute('aria-live','polite');
  toolbar.append(count);
  collection.prepend(toolbar);
  const pagination = element('nav', 'app-pagination');
  pagination.setAttribute('aria-label', 'Pagination : ' + (collection.dataset.collection || 'résultats'));
  const previous = element('button','btn btn-outline-primary','Précédent'); previous.type = 'button';
  const next = element('button','btn btn-outline-primary','Suivant'); next.type = 'button';
  const pageInfo = element('span');
  const pageSize = element('select','form-select');
  pageSize.setAttribute('aria-label','Résultats par page'); pageSize.style.width = 'auto';
  [10,25,50].forEach(size => pageSize.add(new Option(`${size} par page`,String(size))));
  pagination.append(previous,pageInfo,next,pageSize);
  const empty = element('p','app-empty','Aucun résultat. Essayez un autre mot-clé ou un autre statut.');
  empty.hidden = true;
  collection.append(empty,pagination);
  const reset = element('button','btn btn-secondary','Réinitialiser'); reset.type = 'button'; toolbar.append(reset);
  let page = 1;
  const render = () => {
   const query = normalize(search.value.trim());
   const filtered = items.filter(item => normalize(item.dataset.search || item.textContent).includes(query) && (!state.value || item.dataset.state === state.value));
   const sorted = sort.value === 'alpha' ? [...filtered].sort((a,b) => (a.dataset.sort || a.textContent.trim()).localeCompare(b.dataset.sort || b.textContent.trim(),'fr')) : filtered;
   // Every item in one collection has the same parent, preserving valid table markup.
   const ordered = sort.value === 'alpha' ? [...items].sort((a,b) => (a.dataset.sort || a.textContent.trim()).localeCompare(b.dataset.sort || b.textContent.trim(),'fr')) : items;
   ordered.forEach(item => item.parentElement.append(item));
   const size = Number(pageSize.value);
   const pages = Math.max(1,Math.ceil(sorted.length / size));
   page = Math.min(page,pages);
   const visible = new Set(sorted.slice((page-1)*size,page*size));
   items.forEach(item => { item.hidden = !visible.has(item); });
   count.textContent = `${filtered.length} résultat${filtered.length > 1 ? 's' : ''} sur ${items.length}`;
   empty.hidden = filtered.length > 0;
   pageInfo.textContent = `Page ${page} sur ${pages}`;
   previous.disabled = page === 1; next.disabled = page === pages;
   pagination.hidden = filtered.length <= 10;
  };
  [search,state,sort,pageSize].forEach(control => control.addEventListener(control === search ? 'input' : 'change',() => { page=1; render(); }));
  previous.addEventListener('click',() => { page--; render(); });
  next.addEventListener('click',() => { page++; render(); });
  reset.addEventListener('click',() => { search.value=''; state.value=''; sort.value='initial'; page=1; render(); search.focus(); });
  render();
 });

 // Restore only the submitted form after a server-side validation error.
 const savedForm = document.getElementById('app-form-restore');
 if (savedForm) {
  const values = new Map();
  all('[data-restore-name]', savedForm).forEach(input => {
   const name = input.dataset.restoreName;
   if (!values.has(name)) values.set(name, []);
   values.get(name).push(input.value);
  });
  const form = all('form').find(candidate => new URL(candidate.action).pathname === savedForm.dataset.action
   && (!values.has('idOffre') || candidate.elements.namedItem('idOffre')?.value === values.get('idOffre')[0])
   && (!values.has('id_candidature') || candidate.elements.namedItem('id_candidature')?.value === values.get('id_candidature')[0]));
  if (form) values.forEach((entries,name) => {
   all('input,select,textarea',form).filter(control => control.name === name && control.type !== 'file').forEach((control,index) => {
    if (control.type === 'checkbox' || control.type === 'radio') control.checked=entries.includes(control.value);
    else if (entries[index] !== undefined) control.value=entries[index];
   });
  });
 }

 // Replace technical QCM serialization with named answers and a correct-answer selector.
 all('[data-question]').forEach(question => {
  const choices = question.querySelector('[name="choix"]');
  const correct = question.querySelector('[name="bonneReponse"]');
  const type = question.querySelector('[name="type"]');
  const prompt = question.querySelector('[name="enonce"]');
  if (!choices || !correct || !type) return;
  const editor = element('fieldset','app-choices');
  editor.append(element('legend','app-field-label','Réponses proposées'));
  const hint = element('p','small text-muted','Renseignez au moins deux réponses et cochez la bonne réponse.');
  editor.append(hint);
  const rawValues = choices.value ? choices.value.split('|') : [];
  const answerCount = Math.max(3,rawValues.length);
  const radioName = uid('bonne-reponse');
  const answers = [], radios = [];
  const appendAnswer = (value='') => {
   const index=answers.length;
   const row = element('div','app-choice-row');
   const answer = element('input','form-control'); answer.type='text'; answer.value=value;
   answer.id=uid('reponse'); answer.placeholder=`Réponse ${index+1}`; answer.setAttribute('aria-label',`Réponse ${index+1}`);
   const label = element('label');
   const radio = element('input'); radio.type='radio'; radio.name=radioName; radio.value=String(index);
   radio.checked=correct.value !== '' && Number(correct.value) === index;
   label.append(radio,document.createTextNode('Bonne réponse'));
   row.append(answer,label); editor.append(row); answers.push(answer); radios.push(radio);
  };
  for (let index=0;index<answerCount;index++) appendAnswer(rawValues[index] || '');
  // Hide fallback controls, not their submitted values or their array positions.
  choices.closest('[data-choice-fallback]').hidden=true;
  correct.closest('[data-correct-fallback]').hidden=true;
  question.append(editor);
  const sync = () => {
   const isQcm=type.value === 'QCM';
   editor.hidden=!isQcm; editor.disabled=!isQcm;
   choices.value=answers.map(answer => answer.value.trim()).join('|');
   correct.value=radios.find(radio => radio.checked)?.value || '';
   const configured=isQcm && !!prompt?.value.trim();
   const filled=answers.filter(answer => answer.value.trim()).length;
   answers.forEach(answer => answer.setCustomValidity(''));
   if (configured && filled < 2) answers[0].setCustomValidity('Ajoutez au moins deux réponses.');
   answers.forEach((answer,index) => {
    if (isQcm && answer.value.includes('|')) answer.setCustomValidity('Utilisez une réponse par champ, sans le caractère |.');
    if (configured && radios[index].checked && !answer.value.trim()) answer.setCustomValidity('Renseignez la réponse sélectionnée.');
   });
   radios.forEach(radio => { radio.required=configured; });
  };
  question.addEventListener('input',sync); question.addEventListener('change',sync); sync();
  const addAnswer=element('button','btn btn-outline-primary','Ajouter une réponse'); addAnswer.type='button';
  editor.append(addAnswer);
  addAnswer.addEventListener('click',() => { appendAnswer(); editor.append(addAnswer); sync(); answers.at(-1).focus(); });
 });

 // Associate existing labels, and give older placeholder-only controls a visible label.
 const names={titre:'Titre du questionnaire',seuilReussite:'Score minimum de réussite',enonce:'Question',points:'Points',type:'Type de réponse',choix:'Réponses (séparées par |)',bonneReponse:'Numéro de la bonne réponse (à partir de 0)',commentaire:'Commentaire',motif:'Motif du refus',dateEntretien:'Date et heure de l’entretien',lieu:'Lieu ou lien de visioconférence',noteHonneteteCv:'Conformité du CV / 20',notePersonnalite:'Personnalité / 20',noteCompetences:'Compétences / 20',salaire:'Salaire (Ar)',posteId:'Poste à pourvoir',anneesExperience:'Années d’expérience',nbPlaces:'Nombre de places'};
 all('input,select,textarea').filter(control => !['hidden','radio','checkbox','submit','button'].includes(control.type)).forEach(control => {
  if (control.closest('[hidden]') || control.labels?.length || control.hasAttribute('aria-label') || control.hasAttribute('aria-labelledby')) return;
  control.id ||= uid('champ');
  let label=control.previousElementSibling?.matches('label') ? control.previousElementSibling : null;
  const inputGroup=control.closest('.input-group');
  if (inputGroup) {
   const title=inputGroup.querySelector('.input-group-text');
   if (title) { title.id ||= uid('libelle'); control.setAttribute('aria-labelledby',title.id); return; }
  }
  if (!label) {
   const text=names[control.name] || control.placeholder;
   if (!text) return;
   label=element('label','app-field-label',text);
   control.before(label);
  }
  label.htmlFor=control.id;
 });
 all('.alert-success,.banner.ok').forEach(message => message.setAttribute('role','status'));
 all('.alert-danger,.banner.ko').forEach(message => message.setAttribute('role','alert'));
 const errorSummary = document.querySelector('.alert-danger,.banner.ko');
 if (errorSummary) { errorSummary.tabIndex=-1; errorSummary.focus(); }
 const pointageDialog=document.querySelector('[data-pointage-confirm]');
 if (pointageDialog && typeof pointageDialog.showModal === 'function') {
  all('a').filter(link => !link.hasAttribute('data-confirm-logout') && new URL(link.href).pathname.endsWith('/logout')).forEach(link => {
   link.addEventListener('click',event => {
    if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    pointageDialog.querySelector('[data-confirm-logout]').href=link.href;
    pointageDialog.showModal();
   });
  });
  pointageDialog.querySelector('[data-cancel-logout]').addEventListener('click',() => pointageDialog.close());
 }

 // Inline browser validation keeps the entered values and explains each correction.
 all('form').forEach(form => {
  form.addEventListener('invalid',event => {
   const field=event.target;
   if (!field.id) field.id=uid('champ');
   const id=`${field.id}-erreur`;
   let error=document.getElementById(id);
   if (!error) { error=element('span','app-field-error'); error.id=id; field.after(error); }
   error.textContent=field.validationMessage; field.setAttribute('aria-invalid','true');
   const description=new Set((field.getAttribute('aria-describedby') || '').split(' ').filter(Boolean));
   description.add(id); field.setAttribute('aria-describedby',[...description].join(' '));
  },true);
  form.addEventListener('input',event => {
   const field=event.target;
   if (field.validity?.valid) {
    field.removeAttribute('aria-invalid');
    const errorId=`${field.id}-erreur`;
    document.getElementById(errorId)?.remove();
    const descriptions=(field.getAttribute('aria-describedby') || '').split(' ').filter(id => id && id !== errorId);
    if (descriptions.length) field.setAttribute('aria-describedby',descriptions.join(' '));
    else field.removeAttribute('aria-describedby');
   }
  });
 });
})();
