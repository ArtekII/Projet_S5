document.querySelectorAll('.conditions-page article').forEach((carte) => {
    const zone = carte.querySelector('[data-modifications]');
    if (!zone) return;
    const champs = zone.querySelector('fieldset');
    const message = carte.querySelector('[data-reponse-message]');
    const boutons = carte.querySelectorAll('[data-reponse]');

    function choisir(code) {
        const modifier = code === 'AUTRE';
        zone.hidden = !modifier;
        champs.disabled = !modifier;
        boutons.forEach((bouton) => {
            const selectionne = bouton.dataset.reponse === code;
            bouton.setAttribute('aria-pressed', String(selectionne));
            bouton.classList.toggle('btn-primary', selectionne);
            bouton.classList.toggle('btn-outline-primary', !selectionne);
        });
        if (modifier) {
            message.textContent = 'Ajustez les conditions ci-dessous, puis envoyez votre proposition au RH.';
        } else if (code === 'OUI') {
            message.textContent = formulaire.dataset.apercu === 'true'
                ? 'Simulation : les conditions seraient acceptées et un contrat CE serait créé (CDI avec accréditation RH). Aucune donnée enregistrée.'
                : 'Acceptation des conditions et création de votre contrat en cours…';
        } else {
            message.textContent = 'Vous avez choisi « Non ». La suite de cette action sera disponible plus tard. Aucune réponse enregistrée.';
        }
    }

    boutons.forEach((bouton) => bouton.addEventListener('click', () => choisir(bouton.dataset.reponse)));
    if (!zone.hidden) choisir('AUTRE');

    const formulaire = zone.querySelector('form');
    if (formulaire.dataset.apercu === 'true') {
        formulaire.addEventListener('submit', (event) => {
            event.preventDefault();
            message.textContent = 'Simulation : votre proposition serait envoyée au RH pour validation. Aucune donnée enregistrée dans cet aperçu.';
        });
    }
});
