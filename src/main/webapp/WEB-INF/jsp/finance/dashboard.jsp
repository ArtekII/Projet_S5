<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Finance</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head>
<body class="bg-light app-shell">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main id="contenu" class="container" tabindex="-1">
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <header class="app-page-head"><div><span class="app-eyebrow">Finance</span><h1>Salaires à valider</h1><p>Vérifiez le salaire proposé par les RH, puis transmettez votre décision à la Direction.</p></div></header>
 <ol class="app-workflow" aria-label="Parcours de publication"><li><span>Manager</span></li><li><span>RH</span></li><li aria-current="step"><span>Finance</span></li><li><span>Direction</span></li><li><span>Publication</span></li></ol>
 <div class="card shadow-sm"><div class="card-header bg-warning">Offres en attente Finance</div>
  <div class="card-body">
   <c:if test="${empty offresAttente}"><div class="text-muted small">Aucune offre en attente Finance.</div></c:if>
   <c:forEach var="o" items="${offresAttente}">
    <div class="border rounded p-3 mb-2">
     <strong>${o.poste.intitule}</strong> — <span>${o.departement.nom}</span> — Manager: <span>${o.manager.email}</span><br>
     <span class="small">Exp: <span>${o.anneesExperience}</span> ans — Places: <span>${o.nbPlaces}</span> — Salaire RH: <strong><c:choose><c:when test="${empty o.salaire}">-</c:when><c:otherwise>${o.salaire} Ar</c:otherwise></c:choose></strong></span>
     <form method="post" action="${pageContext.request.contextPath}/finance/offre/${o.id}/valider" class="mt-2">
      <div class="input-group input-group-sm mb-1"><span class="input-group-text">Salaire (modifier si besoin)</span><input name="salaire" type="number" step="0.01" min="0" class="form-control" value="${o.salaire}" placeholder="laisser vide = garder salaire RH"><button class="btn btn-success">Valider &rarr; Direction</button></div>
      <input name="commentaire" class="form-control form-control-sm" placeholder="commentaire (optionnel)">
     </form>
     <form method="post" action="${pageContext.request.contextPath}/finance/offre/${o.id}/refuser" class="mt-1 d-flex gap-1"><input name="commentaire" placeholder="motif refus" class="form-control form-control-sm"><button class="btn btn-sm btn-outline-danger">Refuser</button></form>
    </div>
   </c:forEach>
  </div>
 </div>
 <div class="card shadow-sm mt-4"><div class="card-header">Historique toutes offres</div><div class="card-body" data-collection="offres finance" data-search-placeholder="Intitulé du poste"><div class="table-responsive"><table class="table table-sm mb-0">
  <thead><tr><th>Poste</th><th>Statut</th><th>Salaire</th></tr></thead>
  <tbody>
   <c:forEach var="o" items="${toutesOffres}"><tr data-item data-state="<c:out value='${o.statut.libelle}'/>"><td>${o.poste.intitule}</td><td><c:set var="offreSuivie" value="${o}"/><%@ include file="../fragments/offre-status.jspf" %></td><td>${o.salaire}</td></tr></c:forEach>
  </tbody>
 </table></div></div></div>
</main>
</body></html>
