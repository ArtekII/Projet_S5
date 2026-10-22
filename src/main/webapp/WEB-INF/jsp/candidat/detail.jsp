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
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
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
   <c:choose>
    <c:when test="${aDejaCandidate}">
     <div class="alert alert-info mb-0">Vous avez déjà candidaté à cette offre.</div>
    </c:when>
    <c:otherwise>
     <hr>
     <h5>Formulaire de candidature</h5>
     <form method="post" action="${pageContext.request.contextPath}/candidat/offre/${offre.id}/candidature">
      <div class="row g-3">
       <div class="col-md-6"><label class="form-label">Nom</label><input name="nom" class="form-control" required></div>
       <div class="col-md-6"><label class="form-label">Prénom</label><input name="prenom" class="form-control" required></div>
       <div class="col-md-6"><label class="form-label">Date de naissance</label><input name="dateNaissance" type="date" class="form-control" required></div>
       <div class="col-md-6"><label class="form-label">Années d'expérience</label><input name="anneeExperience" type="number" min="0" class="form-control" required value="0"></div>
      </div>
      <button class="btn btn-primary mt-3">Envoyer ma candidature</button>
     </form>
    </c:otherwise>
   </c:choose>
   <c:if test="${selectionnePourQuestionnaire}">
    <a class="btn btn-outline-primary mt-2" href="${pageContext.request.contextPath}/candidat/offre/${offre.id}/questionnaire">Voir le questionnaire</a>
   </c:if>
   <c:if test="${aDejaCandidate and not selectionnePourQuestionnaire}">
    <div class="alert alert-secondary mt-2 mb-0">Votre candidature est en attente de sélection par le RH.</div>
   </c:if>
  </div>
 </div>
</div>
</body></html>
