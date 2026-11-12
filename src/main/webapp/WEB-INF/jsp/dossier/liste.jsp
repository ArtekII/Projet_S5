<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Dossiers administratifs · RH</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <link href="${pageContext.request.contextPath}/assets/conditions.css" rel="stylesheet">
<jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head><body class="bg-light app-shell">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main id="contenu" class="container conditions-page" tabindex="-1">
 <header class="conditions-hero"><h1>Dossiers administratifs</h1><p>Renseignements, pièces justificatives et parcours des utilisateurs et employés.</p></header>
 <c:if test="${empty dossiers}"><p>Aucun dossier à afficher.</p></c:if>
 <div data-collection="dossiers" data-order-label="Documents à traiter en premier" data-search-placeholder="Nom, prénom ou adresse e-mail"><div class="table-responsive"><table class="table table-striped bg-white align-middle">
  <thead><tr><th>Utilisateur</th><th>Identité</th><th>Employé</th><th>Documents à suivre</th><th>Dossier</th></tr></thead>
  <tbody><c:forEach var="d" items="${dossiers}"><tr data-item data-priority="${d.expires > 0 ? 0 : (d.aRenouveler > 0 ? 1 : 2)}" data-state="${d.expires > 0 ? 'Documents expirés' : (d.aRenouveler > 0 ? 'À renouveler' : 'Aucun document à renouveler')}">
   <td><c:out value="${d.email}"/></td><td><c:out value="${d.nom}"/> <c:out value="${d.prenom}"/><c:if test="${empty d.nom}">À compléter</c:if></td>
   <td><c:out value="${empty d.employeId ? 'Non embauché' : d.employeId}"/></td>
   <td><c:if test="${d.expires > 0}"><span class="badge bg-danger">${d.expires} expiré(s)</span> </c:if><c:if test="${d.aRenouveler > 0}"><span class="badge bg-warning text-dark">${d.aRenouveler} à renouveler sous 30 jours</span></c:if><c:if test="${d.expires == 0 and d.aRenouveler == 0}"><span class="badge bg-secondary">Aucun document à renouveler</span></c:if></td>
   <td><a href="${pageContext.request.contextPath}/rh/dossiers/${d.id}">Ouvrir le dossier</a></td>
  </tr></c:forEach></tbody>
 </table></div></div>
</main></body></html>
