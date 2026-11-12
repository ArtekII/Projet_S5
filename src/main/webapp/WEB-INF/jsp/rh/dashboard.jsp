<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html><html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>${titreRh} · Gestion RH</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head><body class="app-shell" data-current-view="${vueRh}">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main id="contenu" tabindex="-1">
 <c:if test="${not empty vueRh}"><nav class="app-breadcrumb" aria-label="Fil d’Ariane"><a href="${pageContext.request.contextPath}/rh/dashboard">Vue d'ensemble</a><span aria-hidden="true">/</span><span aria-current="page">${titreRh}</span></nav></c:if>
 <header class="app-page-head"><div><span class="app-eyebrow">Ressources humaines</span><h1>${titreRh}</h1><p>${descriptionRh}</p></div></header>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <c:if test="${empty vueRh}">
  <div class="app-stats" aria-label="Activité du recrutement">
   <a class="app-stat" href="${pageContext.request.contextPath}/rh/dashboard?vue=demandes"><span>Demandes de postes</span><strong>${fn:length(demandesAttente)}</strong><small>À examiner →</small></a>
   <a class="app-stat" href="${pageContext.request.contextPath}/rh/dashboard?vue=offres"><span>Offres à valider</span><strong>${fn:length(offresAttente)}</strong><small>À transmettre à Finance →</small></a>
   <a class="app-stat" href="${pageContext.request.contextPath}/rh/dashboard?vue=candidatures"><span>Candidatures en cours</span><strong>${fn:length(candidaturesRh)}</strong><small>Suivre les étapes →</small></a>
   <a class="app-stat" href="${pageContext.request.contextPath}/rh/dashboard?vue=questionnaires"><span>Questionnaires à préparer</span><strong>${questionnairesAPreparer}</strong><small>Configurer les questions →</small></a>
  </div>
  <div class="app-grid">
   <section class="card"><div class="card-header"><h2 class="h5 mb-0">À traiter</h2></div><div class="card-body">
    <c:if test="${empty demandesAttente and empty offresAttente and empty candidaturesRh}"><div class="app-empty"><strong>Vous êtes à jour</strong>Aucune demande, offre ou candidature en attente.</div></c:if>
    <c:if test="${not empty demandesAttente}"><a class="app-task" href="${pageContext.request.contextPath}/rh/dashboard?vue=demandes"><span><strong>Examiner les demandes de postes</strong><small>Approuver les nouveaux besoins des managers.</small></span><span>${fn:length(demandesAttente)} →</span></a></c:if>
    <c:if test="${not empty offresAttente}"><a class="app-task" href="${pageContext.request.contextPath}/rh/dashboard?vue=offres"><span><strong>Définir les salaires proposés</strong><small>Vérifier les offres avant leur transmission à Finance.</small></span><span>${fn:length(offresAttente)} →</span></a></c:if>
    <c:if test="${not empty candidaturesRh}"><a class="app-task" href="${pageContext.request.contextPath}/rh/dashboard?vue=candidatures"><span><strong>Faire avancer les candidatures</strong><small>Questionnaires, entretiens et prochaines étapes.</small></span><span>${fn:length(candidaturesRh)} →</span></a></c:if>
   </div></section>
   <section class="card"><div class="card-header"><h2 class="h5 mb-0">Accès rapides</h2></div><div class="card-body">
    <a class="app-task" href="${pageContext.request.contextPath}/rh/dossiers"><span><strong>Documents et dossiers</strong><small>Repérer les pièces expirées ou à renouveler.</small></span><span>→</span></a>
    <a class="app-task" href="${pageContext.request.contextPath}/rh/pointages"><span><strong>Pointages et anomalies</strong><small>Vérifier les journées avant transmission à Finance.</small></span><span>→</span></a>
    <a class="app-task" href="${pageContext.request.contextPath}/rh/plannings"><span><strong>Plannings</strong><small>Préparer les horaires de travail par poste.</small></span><span>→</span></a>
   </div></section>
  </div>
  <section class="card mt-4"><div class="card-header"><h2 class="h5 mb-0">Échéances à venir</h2></div><div class="card-body">
   <p class="small text-muted">Entretiens des 7 prochains jours et documents à renouveler sous 30 jours. Les 5 échéances les plus proches sont affichées.</p>
   <c:if test="${empty echeancesRh}"><p class="text-muted mb-0">Aucun entretien ni renouvellement prévu sur ces périodes.</p></c:if>
   <c:forEach var="echeance" items="${echeancesRh}"><a class="app-task" href="${pageContext.request.contextPath}<c:out value='${echeance.lien}'/>"><span><strong><c:out value="${echeance.titre}"/></strong><small><c:out value="${echeance.description}"/></small></span><span><c:out value="${echeance.date}"/> →</span></a></c:forEach>
  </div></section>
 </c:if>
<c:if test="${vueRh == 'demandes'}">
<div class="card shadow-sm"><div class="card-header bg-info">Demandes à examiner</div>
    <div class="card-body">
     <c:if test="${empty demandesAttente}"><div class="text-muted small">Aucune demande en attente.</div></c:if>
     <c:forEach var="d" items="${demandesAttente}">
      <div class="border rounded p-2 mb-2">
       <strong>${d.intitulePropose}</strong> — <span>${d.departement.nom}</span> — par <span>${d.manager.email}</span>
       <div class="small text-muted">${d.justification}</div>
       <div class="d-flex gap-2 mt-2">
        <form method="post" action="${pageContext.request.contextPath}/rh/demande/${d.id}/valider"><button class="btn btn-sm btn-success">Approuver la création du poste</button></form>
        <form method="post" action="${pageContext.request.contextPath}/rh/demande/${d.id}/refuser" class="d-flex gap-1"><input name="motif" placeholder="motif refus" class="form-control form-control-sm"><button class="btn btn-sm btn-danger">Refuser</button></form>
       </div>
      </div>
     </c:forEach>
    </div>
   </div>
</c:if>
<c:if test="${vueRh == 'offres'}">
<div class="card shadow-sm"><div class="card-header bg-warning">Offres à transmettre à Finance</div>
    <div class="card-body">
     <c:if test="${empty offresAttente}"><div class="text-muted small">Aucune offre en attente RH.</div></c:if>
     <c:forEach var="o" items="${offresAttente}">
      <div class="border rounded p-2 mb-2">
       <strong>${o.poste.intitule}</strong> — <span>${o.departement.nom}</span> — Manager: <span>${o.manager.email}</span>
       <form method="post" action="${pageContext.request.contextPath}/rh/offre/${o.id}/valider" class="mt-2">
        <div class="mb-2">
         <label class="form-label small mb-1">Poste à pourvoir</label>
         <select name="posteId" class="form-select form-select-sm" required>
          <c:forEach var="p" items="${postes}">
           <option value="${p.id}" <c:if test="${p.id == o.poste.id}">selected</c:if>>${p.intitule}</option>
          </c:forEach>
         </select>
        </div>
        <div class="row g-2 mb-2">
         <div class="col-6"><label class="form-label small mb-1">Années d'expérience</label>
          <input name="anneesExperience" type="number" min="0" class="form-control form-control-sm" required value="${o.anneesExperience}"></div>
         <div class="col-6"><label class="form-label small mb-1">Nombre de places</label>
          <input name="nbPlaces" type="number" min="1" class="form-control form-control-sm" required value="${o.nbPlaces}"></div>
        </div>
        <div class="input-group input-group-sm mb-1"><span class="input-group-text">Salaire (Ar)</span><input name="salaire" type="number" step="0.01" min="0" class="form-control" required placeholder="ex: 1500000"><button class="btn btn-success">Valider &rarr; Finance</button></div>
        <input name="commentaire" class="form-control form-control-sm" placeholder="commentaire (optionnel)">
       </form>
       <form method="post" action="${pageContext.request.contextPath}/rh/offre/${o.id}/refuser" class="mt-1 d-flex gap-1"><input name="commentaire" placeholder="motif refus" class="form-control form-control-sm"><button class="btn btn-sm btn-outline-danger">Refuser</button></form>
      </div>
     </c:forEach>
    </div>
   </div>
</c:if>
<c:if test="${vueRh == 'questionnaires'}">
 <section class="card"><div class="card-header"><h2 class="h5 mb-0">Questionnaires par offre</h2></div>
  <div class="card-body" data-collection="questionnaires" data-search-placeholder="Poste ou département">
   <c:if test="${empty toutesOffres}"><div class="app-empty"><strong>Aucune offre à configurer</strong>Créez une offre pour préparer son questionnaire.</div></c:if>
   <div>
    <c:forEach var="o" items="${toutesOffres}">
     <c:set var="questionnaire" value="${questionnairesParOffre[o.id]}"/>
     <article class="app-task" data-item data-state="${empty questionnaire ? 'À préparer' : 'Configuré'}">
      <span><strong><c:out value="${o.poste.intitule}"/></strong><small><c:out value="${o.departement.nom}"/> · <c:choose><c:when test="${empty questionnaire}">Questionnaire à préparer</c:when><c:otherwise><c:out value="${questionnaire.titre}"/></c:otherwise></c:choose></small></span>
      <a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/rh/offre/${o.id}/candidats#questionnaire">${empty questionnaire ? 'Configurer' : 'Modifier'}</a>
     </article>
    </c:forEach>
   </div>
  </div>
 </section>
</c:if>
<c:if test="${vueRh == 'candidatures'}">
<div class="card shadow-sm mt-4"><div class="card-header">Candidatures à suivre</div>
   <div class="card-body" data-collection="candidatures" data-search-placeholder="Candidat, poste ou département">
    <c:if test="${empty candidaturesRh}"><div class="text-muted small">Aucune candidature en cours.</div></c:if>
    <c:forEach var="c" items="${candidaturesRh}">
     <div class="border rounded p-3 mb-3" data-item data-state="<c:out value='${c.etape}'/>">
      <strong>${c.offre}</strong> — <span>${c.departement}</span> — candidat <span>${c.candidat}</span>
      <div class="small mt-2">Étape en cours : <b>${c.etape}</b> → <span class="text-muted">${c.etapeSuivante}</span> · Statut : ${c.statut}</div>
      <c:if test="${c.afficheQuestionnaire}">
       <details class="mt-2"><summary>Voir les réponses au questionnaire</summary>
        <div class="small mb-2">Score actuel : <strong>${c.candidature.scoreQuestionnaire}</strong> / ${c.questionnaire.questionnaire.seuilReussite} requis</div>
        <form method="post" action="${pageContext.request.contextPath}/rh/candidature/${c.candidature.id}/questionnaire/noter">
        <table class="table table-sm mb-2">
         <thead><tr><th>Question</th><th>Réponse</th><th>Points</th></tr></thead>
         <tbody>
          <c:forEach var="q" items="${c.questionnaire.questions}">
           <c:set var="rep" value="${c.questionnaire.getReponseDe(q)}"/>
           <tr>
            <td>${q.enonce}</td>
            <td><c:choose><c:when test="${not empty rep and not empty rep.choix}">${rep.choix.libelle}</c:when><c:when test="${not empty rep and not empty rep.reponseTexte}">${rep.reponseTexte}</c:when><c:otherwise>-</c:otherwise></c:choose></td>
            <td>
             ${rep.pointsObtenus}
             <c:if test="${not empty rep and not empty rep.reponseTexte}">
              <input name="note_${q.id}" type="number" min="0" max="${q.points}" step="0.01" class="form-control form-control-sm mt-1" placeholder="Note / ${q.points}">
             </c:if>
            </td>
           </tr>
          </c:forEach>
         </tbody>
        </table>
        <button class="btn btn-sm btn-outline-primary" type="submit">Enregistrer les points</button>
        </form>
       </details>
      </c:if>
      <c:if test="${c.codeEtape eq 'ENTRETIEN'}">
       <form method="post" action="${pageContext.request.contextPath}/rh/candidature/${c.candidature.id}/entretien" class="row g-2 mt-2">
        <div class="col-md-4"><input name="dateEntretien" type="datetime-local" class="form-control form-control-sm" required></div>
        <div class="col-md-4"><input name="lieu" class="form-control form-control-sm" placeholder="Lieu ou lien visioconférence" required></div>
        <div class="col-md-4"><input name="noteHonneteteCv" type="number" min="0" max="20" step="0.01" class="form-control form-control-sm" placeholder="Honnêteté CV /20" required></div>
        <div class="col-md-4"><input name="notePersonnalite" type="number" min="0" max="20" step="0.01" class="form-control form-control-sm" placeholder="Personnalité /20" required></div>
        <div class="col-md-4"><input name="noteCompetences" type="number" min="0" max="20" step="0.01" class="form-control form-control-sm" placeholder="Compétences /20" required></div>
        <div class="col-12"><button class="btn btn-sm btn-primary">Enregistrer l'entretien</button></div>
       </form>
      </c:if>
      <div class="d-flex gap-2 mt-2">
       <c:choose>
        <c:when test="${c.codeEtape eq 'QUESTIONNAIRE' and not c.questionnaire.repondu}">
         <p class="small text-muted mb-0">En attente du candidat : le questionnaire doit être envoyé avant de valider cette étape.</p>
        </c:when>
        <c:otherwise>
         <form method="post" action="${pageContext.request.contextPath}/rh/candidature/${c.candidature.id}/valider"><button class="btn btn-sm btn-success">Valider l'étape → ${c.etapeSuivante}</button></form>
        </c:otherwise>
       </c:choose>
       <form method="post" action="${pageContext.request.contextPath}/rh/candidature/${c.candidature.id}/eliminer"><button class="btn btn-sm btn-outline-danger">Éliminer</button></form>
      </div>
     </div>
    </c:forEach>
   </div>
  </div>
</c:if>
<c:if test="${vueRh == 'historique'}">
<div class="card shadow-sm mt-4"><div class="card-header">Historique toutes offres</div>
  <div class="card-body" data-collection="offres" data-search-placeholder="Poste ou département"><div class="table-responsive"><table class="table table-sm mb-0">
   <thead><tr><th>Poste</th><th>Département</th><th>Expérience</th><th>Places</th><th>Salaire</th><th>Statut</th><th>Actions</th></tr></thead>
   <tbody>
    <c:forEach var="o" items="${toutesOffres}">
     <tr data-item data-state="<c:out value='${o.statut.libelle}'/>" data-sort="<c:out value='${o.poste.intitule}'/>"><td>${o.poste.intitule}</td><td>${o.departement.nom}</td><td>${o.anneesExperience}</td><td>${o.nbPlaces}</td><td>${empty o.salaire ? '-' : o.salaire}</td><td><c:set var="offreSuivie" value="${o}"/><%@ include file="../fragments/offre-status.jspf" %></td><td><a class="btn btn-sm btn-outline-primary" href="${pageContext.request.contextPath}/rh/offre/${o.id}/candidats">Suivre les candidats</a></td></tr>
    </c:forEach>
   <c:if test="${empty toutesOffres}"><tr><td colspan="7" class="text-center text-muted py-4">Aucune offre pour le moment.</td></tr></c:if></tbody>
  </table></div></div>
 </div>
</c:if>
</main></body></html>
