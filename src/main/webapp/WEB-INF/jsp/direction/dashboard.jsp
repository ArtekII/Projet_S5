<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Direction</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <h4>Direction — Validation finale</h4>
 <p class="small text-muted">Dès que vous validez, l'offre est officiellement publiée et visible par les candidats.</p>
 <div class="card shadow-sm"><div class="card-header bg-dark text-white">Offres en attente Direction</div>
  <div class="card-body">
   <c:if test="${empty offresAttente}"><div class="text-muted small">Aucune offre en attente Direction.</div></c:if>
   <c:forEach var="o" items="${offresAttente}">
    <div class="border rounded p-3 mb-2">
     <strong>${o.poste.intitule}</strong> — <span>${o.departement.nom}</span> — Salaire: <strong>${o.salaire} Ar</strong> — Places: <span>${o.nbPlaces}</span> — Exp: <span>${o.anneesExperience}</span> ans<br>
     <span class="small text-muted">Manager: <span>${o.manager.email}</span></span>
     <div class="d-flex gap-2 mt-2">
      <form method="post" action="${pageContext.request.contextPath}/direction/offre/${o.id}/valider"><input type="hidden" name="commentaire" value=""><button class="btn btn-sm btn-success">Publier l'offre</button></form>
      <form method="post" action="${pageContext.request.contextPath}/direction/offre/${o.id}/refuser" class="d-flex gap-1"><input name="commentaire" placeholder="motif refus" class="form-control form-control-sm"><button class="btn btn-sm btn-danger">Refuser</button></form>
     </div>
    </div>
   </c:forEach>
  </div>
 </div>
 <div class="card shadow-sm mt-4"><div class="card-header">Offres publiées (visibles candidats)</div><div class="card-body p-0"><div class="table-responsive"><table class="table table-sm mb-0">
  <thead><tr><th>Poste</th><th>Dept</th><th>Salaire</th><th>Places</th><th>Date publication</th></tr></thead>
  <tbody>
   <c:forEach var="o" items="${offresPubliees}"><tr><td>${o.poste.intitule}</td><td>${o.departement.nom}</td><td>${o.salaire}</td><td>${o.nbPlaces}</td><td>${f:dateTime(o.datePublication)}</td></tr></c:forEach>
   <c:if test="${empty offresPubliees}"><tr><td colspan="5" class="text-center text-muted py-2">Aucune offre publiée</td></tr></c:if>
  </tbody>
 </table></div></div></div>
</div>
</body></html>
