<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Dossiers administratifs · RH</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <link href="${pageContext.request.contextPath}/assets/conditions.css" rel="stylesheet">
</head><body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main class="container conditions-page">
 <header class="conditions-hero"><h1>Dossiers administratifs</h1><p>Renseignements, pièces justificatives et parcours des utilisateurs et employés.</p></header>
 <c:if test="${empty dossiers}"><p>Aucun dossier à afficher.</p></c:if>
 <div class="table-responsive"><table class="table table-striped bg-white align-middle">
  <thead><tr><th>Utilisateur</th><th>Identité</th><th>Employé</th><th>Documents à suivre</th><th>Dossier</th></tr></thead>
  <tbody><c:forEach var="d" items="${dossiers}"><tr>
   <td><c:out value="${d.email}"/></td><td><c:out value="${d.nom}"/> <c:out value="${d.prenom}"/><c:if test="${empty d.nom}">À compléter</c:if></td>
   <td><c:out value="${empty d.employeId ? 'Non embauché' : d.employeId}"/></td>
   <td><span class="badge bg-danger">${d.expires} expiré(s)</span> <span class="badge bg-warning text-dark">${d.aRenouveler} à renouveler sous 30 jours</span></td>
   <td><a href="${pageContext.request.contextPath}/rh/dossiers/${d.id}">Ouvrir le dossier</a></td>
  </tr></c:forEach></tbody>
 </table></div>
</main></body></html>
