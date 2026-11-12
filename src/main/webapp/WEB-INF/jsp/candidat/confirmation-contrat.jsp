<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Confirmer mon contrat</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head><body class="bg-light app-shell">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main id="contenu" class="container py-4" tabindex="-1">
 <a href="${pageContext.request.contextPath}/candidat/conditions">Retour à mes conditions</a>
 <h1 class="h3 mt-3">Étape 2 : confirmer mon contrat</h1>
 <p>Vous avez accepté les conditions de travail. Confirmez maintenant le contrat pour finaliser votre embauche.</p>
 <c:if test="${not empty success}"><div class="alert alert-success"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <section class="card mb-3"><div class="card-body">
  <h2 class="h5">Conditions acceptées</h2>
  <c:set var="affichee" value="${d.condition}"/>
  <%@ include file="../fragments/condition-summary.jsp" %>
 </div></section>
 <form method="post" action="${pageContext.request.contextPath}/candidat/contrats/candidature/${d.id}/confirmer">
  <c:choose>
   <c:when test="${d.cdi_autorise}"><p>L’accréditation RH vous permet de choisir entre CE et CDI.</p></c:when>
   <c:otherwise><p>Votre contrat proposé est un contrat d’essai (CE).</p></c:otherwise>
  </c:choose>
  <%@ include file="../fragments/choix-contrat.jsp" %>
  <button class="btn btn-success">Confirmer mon contrat et finaliser mon embauche</button>
 </form>
</main></body></html>
