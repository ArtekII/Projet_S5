<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Mes contrats</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <link href="${pageContext.request.contextPath}/assets/conditions.css" rel="stylesheet">
</head><body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main class="container conditions-page">
 <a href="${pageContext.request.contextPath}/candidat/conditions">← Mes conditions de travail</a>
 <header class="conditions-hero"><h1>Mes contrats</h1><p>Retrouvez les contrats créés après votre acceptation des conditions de travail.</p></header>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${empty contrats}"><div class="conditions-empty">Aucun contrat pour le moment.</div></c:if>
 <c:forEach var="contrat" items="${contrats}">
  <article class="card shadow-sm mb-3">
   <div class="card-header d-flex justify-content-between"><h2 class="h5 mb-0"><c:out value="${contrat.typeContrat.libelle}"/></h2><span class="badge bg-success"><c:out value="${contrat.typeContrat.code}"/></span></div>
   <div class="card-body">
    <p class="small text-muted">Contrat n° ${contrat.id} · Candidature n° ${contrat.idCandidature}</p>
    <p><c:choose><c:when test="${contrat.typeContrat.code == 'CDI'}">Le CDI a été attribué grâce à l’accréditation RH de votre candidature.</c:when><c:otherwise>Un contrat d’essai a été attribué à votre candidature.</c:otherwise></c:choose></p>
    <h3 class="h6">Conditions acceptées</h3>
    <c:set var="affichee" value="${contrat.condition}"/><%@ include file="../fragments/condition-summary.jsp" %>
    <p class="small text-muted mb-0">Les dates et les modalités de signature seront précisées ultérieurement.</p>
   </div>
  </article>
 </c:forEach>
</main></body></html>
