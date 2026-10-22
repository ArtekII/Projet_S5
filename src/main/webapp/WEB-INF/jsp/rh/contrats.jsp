<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Contrats · RH</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <link href="${pageContext.request.contextPath}/assets/conditions.css" rel="stylesheet">
</head><body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main class="container conditions-page">
 <a href="${pageContext.request.contextPath}/rh/conditions">← Conditions de travail</a>
 <header class="conditions-hero"><h1>Contrats attribués</h1><p>Les candidats obtiennent leur CE ou leur CDI dès l’acceptation des conditions de travail.</p></header>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <c:if test="${empty contrats}"><div class="conditions-empty">Aucun contrat pour le moment.</div></c:if>
 <c:forEach var="contrat" items="${contrats}">
  <article class="card mb-3 shadow-sm"><div class="card-header">
   Contrat n° ${contrat.id} · Candidature n° ${contrat.idCandidature}
   <span class="badge bg-primary"><c:out value="${contrat.typeContrat.code}"/></span>
  </div><div class="card-body">
   <c:set var="affichee" value="${contrat.condition}"/><%@ include file="../fragments/condition-summary.jsp" %>

  </div></article>
 </c:forEach>
</main></body></html>
