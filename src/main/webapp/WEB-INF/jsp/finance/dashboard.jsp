<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Finance</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <h4>Finance — Validation salaire</h4>
 <p class="small text-muted">Vous revoyez l'offre validée par le RH, vous pouvez modifier le salaire ou valider tel quel &rarr; passe à la Direction.</p>
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
 <div class="card shadow-sm mt-4"><div class="card-header">Historique toutes offres</div><div class="card-body p-0"><div class="table-responsive"><table class="table table-sm mb-0">
  <thead><tr><th>Poste</th><th>Statut</th><th>Salaire</th></tr></thead>
  <tbody>
   <c:forEach var="o" items="${toutesOffres}"><tr><td>${o.poste.intitule}</td><td><span class="badge bg-secondary">${o.statut.libelle}</span></td><td>${o.salaire}</td></tr></c:forEach>
  </tbody>
 </table></div></div></div>
</div>
</body></html>
