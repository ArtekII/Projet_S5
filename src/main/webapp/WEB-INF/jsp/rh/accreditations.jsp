<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Accréditations · RH</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <link href="${pageContext.request.contextPath}/assets/conditions.css" rel="stylesheet">
</head><body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main class="container conditions-page">
 <a href="${pageContext.request.contextPath}/rh/dashboard">Retour au tableau de bord RH</a>
 <header class="conditions-hero"><div class="conditions-kicker">Espace ressources humaines</div><h1>Accréditations</h1><p>Accordez une accréditation CDI à un candidat avant son acceptation des conditions. Sans accréditation, il obtient un contrat d’essai.</p></header>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <c:if test="${empty dossiers}"><div class="conditions-empty">Aucun candidat en attente d’accréditation.</div></c:if>
 <c:forEach var="d" items="${dossiers}">
  <article class="card shadow-sm mb-3">
   <div class="card-header"><h2 class="h5 mb-1"><c:out value="${d.email}"/></h2><span class="small text-muted"><c:out value="${d.intitule}"/> · Candidature n° ${d.id}</span></div>
   <div class="card-body">
    <c:choose>
     <c:when test="${d.cdi_autorise}"><span class="badge bg-success">Accréditation CDI accordée</span></c:when>
     <c:otherwise>
      <p>Contrat prévu : contrat d’essai (CE).</p>
      <form method="post" action="${pageContext.request.contextPath}/rh/candidatures/${d.id}/autoriser-cdi">
       <button class="btn btn-primary">Accorder l’accréditation CDI</button>
      </form>
     </c:otherwise>
    </c:choose>
   </div>
  </article>
 </c:forEach>
</main></body></html>
