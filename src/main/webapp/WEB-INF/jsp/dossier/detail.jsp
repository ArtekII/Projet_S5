<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Dossier administratif</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <link href="${pageContext.request.contextPath}/assets/conditions.css" rel="stylesheet">
<jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head><body class="bg-light app-shell">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main id="contenu" class="container conditions-page" tabindex="-1">
 <c:if test="${gestionRh}"><a href="${pageContext.request.contextPath}/rh/dossiers">← Dossiers administratifs</a></c:if>
 <header class="conditions-hero"><h1>Dossier administratif</h1><p><c:out value="${titulaire.email}"/>
  <c:if test="${not empty employe}"> · Employé n° ${employe.id} · Embauche : ${employe.dateEmbauche}</c:if>
  <c:if test="${not empty titulaire.departement}"> · Département : <c:out value="${titulaire.departement.nom}"/></c:if>
 </p></header>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <nav aria-label="Rubriques du dossier" class="d-flex flex-wrap gap-3 mb-4">
  <a href="#renseignements">Renseignements</a><a href="#documents">Documents</a><a href="#cv">CV</a>
  <a href="#contrats">Contrats</a><a href="#parcours">Parcours professionnel</a>
  <c:if test="${not empty employe}"><a href="#integration">Intégration</a></c:if>
  <c:if test="${gestionRh}"><a href="#journal">Journal d'activité</a></c:if>
 </nav>

<%@ include file="../fragments/dossier-contenu.jspf" %>
</main></body></html>
