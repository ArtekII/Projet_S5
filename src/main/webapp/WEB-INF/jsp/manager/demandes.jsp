<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta name="viewport" content="width=device-width, initial-scale=1"><meta charset="UTF-8"><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><title>Mes demandes</title><jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head>
<body class="bg-light app-shell">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main id="contenu" class="container" tabindex="-1">
 <header class="app-page-head"><div><span class="app-eyebrow">Mon équipe</span><h1>Mes demandes de postes</h1><p>Retrouvez les demandes transmises aux ressources humaines et leur décision.</p></div></header>
 <div data-collection="demandes"><div class="table-responsive"><table class="table table-sm">
  <thead><tr><th>Intitulé</th><th>Décision</th><th>Date</th></tr></thead>
  <tbody>
   <c:forEach var="d" items="${demandes}">
    <tr data-item data-state="<c:out value='${d.decision.libelle}'/>"><td>${d.intitulePropose}</td><td>${d.decision.libelle}</td><td>${f:dateTime(d.createdAt)}</td></tr>
   </c:forEach>
  <c:if test="${empty demandes}"><tr><td colspan="3" class="text-center text-muted py-4">Aucune demande envoyée pour le moment.</td></tr></c:if></tbody>
 </table></div></div>
 <a href="${pageContext.request.contextPath}/manager/dashboard" class="btn btn-sm btn-secondary">Retour</a>
</main>
</body></html>
