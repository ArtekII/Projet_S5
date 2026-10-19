<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Offres - Candidat</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <h4>Offres publiées</h4>
 <p class="small text-muted">Seules les offres validées par RH &rarr; Finance &rarr; Direction apparaissent ici.</p>
 <c:if test="${empty offres}"><div class="alert alert-info">Aucune offre publiée pour le moment.</div></c:if>
 <div class="row g-3">
  <c:forEach var="o" items="${offres}">
   <div class="col-md-6 col-lg-4">
    <div class="card shadow-sm h-100">
     <div class="card-body">
      <h6>${o.poste.intitule}</h6>
      <p class="small text-muted mb-1">${o.poste.description}</p>
      <ul class="small mb-2">
       <li>Département: <span>${o.departement.nom}</span></li>
       <li>Exp: <span>${o.anneesExperience}</span> ans</li>
       <li>Places: <span>${o.nbPlaces}</span></li>
       <li>Salaire: <strong><c:choose><c:when test="${empty o.salaire}">-</c:when><c:otherwise>${o.salaire} Ar</c:otherwise></c:choose></strong></li>
      </ul>
      <a href="${pageContext.request.contextPath}/candidat/offre/${o.id}" class="btn btn-sm btn-primary">Voir détails</a>
     </div>
     <div class="card-footer small text-muted">Publiée le ${f:dateTime(o.datePublication)}</div>
    </div>
   </div>
  </c:forEach>
 </div>
</div>
</body></html>
