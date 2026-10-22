<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>RH</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <h4>RH — Validation</h4>
  <p class="small text-muted">Vous voyez : (A) demandes de nouveaux postes à valider/insérer — (B) offres en attente RH (même formulaire que le manager + champ salaire à définir) — (C) candidatures en cours et validation des étapes.</p>
 <div class="row g-4">
  <div class="col-lg-6">
   <div class="card shadow-sm"><div class="card-header bg-info">A) Demandes de nouveau poste (EN_ATTENTE)</div>
    <div class="card-body">
     <c:if test="${empty demandesAttente}"><div class="text-muted small">Aucune demande en attente.</div></c:if>
     <c:forEach var="d" items="${demandesAttente}">
      <div class="border rounded p-2 mb-2">
       <strong>${d.intitulePropose}</strong> — <span>${d.departement.nom}</span> — par <span>${d.manager.email}</span>
       <div class="small text-muted">${d.justification}</div>
       <div class="d-flex gap-2 mt-2">
        <form method="post" action="${pageContext.request.contextPath}/rh/demande/${d.id}/valider"><button class="btn btn-sm btn-success">Valider &amp; insérer</button></form>
        <form method="post" action="${pageContext.request.contextPath}/rh/demande/${d.id}/refuser" class="d-flex gap-1"><input name="motif" placeholder="motif refus" class="form-control form-control-sm"><button class="btn btn-sm btn-danger">Refuser</button></form>
       </div>
      </div>
     </c:forEach>
    </div>
   </div>
  </div>
  <div class="col-lg-6">
   <div class="card shadow-sm"><div class="card-header bg-warning">B) Offres en attente RH</div>
    <div class="card-body">
     <c:if test="${empty offresAttente}"><div class="text-muted small">Aucune offre en attente RH.</div></c:if>
     <c:forEach var="o" items="${offresAttente}">
      <div class="border rounded p-2 mb-2">
       <strong>${o.poste.intitule}</strong> — <span>${o.departement.nom}</span> — Manager: <span>${o.manager.email}</span>
       <form method="post" action="${pageContext.request.contextPath}/rh/offre/${o.id}/valider" class="mt-2">
        <div class="mb-2">
         <label class="form-label small mb-1">Poste (dropdown)</label>
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
  </div>
 </div>
  <div class="card shadow-sm mt-4"><div class="card-header">C) Candidatures en cours — validation des étapes</div>
   <div class="card-body">
    <c:if test="${empty candidaturesRh}"><div class="text-muted small">Aucune candidature en cours.</div></c:if>
    <c:forEach var="c" items="${candidaturesRh}">
     <div class="border rounded p-2 mb-2">
      <strong>${c.offre}</strong> — <span>${c.departement}</span> — candidat <span>${c.candidat}</span>
      <div class="small">Étape en cours : <b>${c.etape}</b> → <span class="text-muted">${c.etapeSuivante}</span> · Statut : ${c.statut}</div>
      <c:if test="${c.afficheQuestionnaire}">
       <details class="mt-2"><summary>Voir les réponses au questionnaire</summary>
        <table class="table table-sm mb-2">
         <thead><tr><th>Question</th><th>Réponse</th><th>Points</th></tr></thead>
         <tbody>
          <c:forEach var="q" items="${c.questionnaire.questions}">
           <c:set var="rep" value="${c.questionnaire.getReponseDe(q)}"/>
           <tr>
            <td>${q.enonce}</td>
            <td><c:choose><c:when test="${not empty rep and not empty rep.choix}">${rep.choix.libelle}</c:when><c:when test="${not empty rep and not empty rep.reponseTexte}">${rep.reponseTexte}</c:when><c:otherwise>-</c:otherwise></c:choose></td>
            <td>${rep.pointsObtenus}</td>
           </tr>
          </c:forEach>
         </tbody>
        </table>
       </details>
      </c:if>
      <div class="d-flex gap-2 mt-2">
       <form method="post" action="${pageContext.request.contextPath}/rh/candidature/${c.candidature.id}/valider"><button class="btn btn-sm btn-success">Valider l'étape → ${c.etapeSuivante}</button></form>
       <form method="post" action="${pageContext.request.contextPath}/rh/candidature/${c.candidature.id}/eliminer"><button class="btn btn-sm btn-outline-danger">Éliminer</button></form>
      </div>
     </div>
    </c:forEach>
   </div>
  </div>
  <div class="card shadow-sm mt-4"><div class="card-header">Historique toutes offres</div>
  <div class="card-body p-0"><div class="table-responsive"><table class="table table-sm mb-0">
   <thead><tr><th>Poste</th><th>Dept</th><th>Exp</th><th>Places</th><th>Salaire</th><th>Statut</th><th>Recrutement</th></tr></thead>
   <tbody>
    <c:forEach var="o" items="${toutesOffres}">
     <tr><td>${o.poste.intitule}</td><td>${o.departement.nom}</td><td>${o.anneesExperience}</td><td>${o.nbPlaces}</td><td>${empty o.salaire ? '-' : o.salaire}</td><td><span class="badge bg-secondary">${o.statut.libelle}</span></td><td><a class="btn btn-sm btn-outline-primary" href="${pageContext.request.contextPath}/rh/offre/${o.id}/recrutement">Ouvrir</a></td></tr>
    </c:forEach>
   </tbody>
  </table></div></div>
 </div>
</div>
</body></html>
