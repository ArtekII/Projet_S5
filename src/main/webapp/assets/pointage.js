(() => {
    if (!document.querySelector('[data-pointage-en-cours]')) return;
    // L'onglet ne contient aucun formulaire : le rechargement actualise aussi les pauses et les alertes.
    window.setInterval(() => {
        if (document.visibilityState === 'visible') window.location.reload();
    }, 30000);
})();
