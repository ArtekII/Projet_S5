<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><title>Détail offre</title></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <a href="${pageContext.request.contextPath}/candidat/offres" class="btn btn-sm btn-secondary mb-3">&larr; Retour aux offres</a>
 <div class="card shadow-sm">
  <div class="card-header">${offre.poste.intitule}</div>
  <div class="card-body">
   <p>${offre.poste.description}</p>
   <ul>
    <li>Département: <span>${offre.departement.nom}</span></li>
    <li>Exp: <span>${offre.anneesExperience}</span> ans</li>
    <li>Places: <span>${offre.nbPlaces}</span></li>
    <li>Salaire: <strong>${offre.salaire}</strong></li>
    <li>Statut: <span class="badge bg-success">${offre.statut.libelle}</span></li>
   </ul>
   <div class="small text-muted">Validations :</div>
   <ul class="small">
    <c:forEach var="v" items="${validations}">
     <li>${v.roleValideur.code} - ${v.decision.libelle} le ${f:dateTime(v.decidedAt)}<c:if test="${not empty v.commentaire}"> (${v.commentaire})</c:if></li>
    </c:forEach>
   </ul>
   <button class="btn btn-primary" disabled>Postuler (à brancher sur candidature)</button>
  </div>
 </div>
</div>
</body></html>
