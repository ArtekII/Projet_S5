<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
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
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <c:if test="${empty contrats}"><div class="conditions-empty">Aucun contrat pour le moment.</div></c:if>
 <c:forEach var="contrat" items="${contrats}">
  <article class="card shadow-sm mb-3">
   <div class="card-header d-flex justify-content-between"><h2 class="h5 mb-0"><c:out value="${contrat.typeContrat.libelle}"/></h2><span class="badge bg-success"><c:out value="${contrat.typeContrat.code}"/></span></div>
   <div class="card-body">
    <p class="small text-muted">Contrat n° ${contrat.id} · Candidature n° ${contrat.idCandidature}</p>
    <p><c:choose><c:when test="${contrat.typeContrat.code == 'CDI'}">Le CDI a été attribué grâce à l’accréditation RH de votre candidature.</c:when><c:otherwise>Un contrat d’essai a été attribué à votre candidature.</c:otherwise></c:choose></p>
    <h3 class="h6">Conditions acceptées</h3>
    <c:set var="affichee" value="${contrat.condition}"/><%@ include file="../fragments/condition-summary.jsp" %>
    <c:choose>
     <c:when test="${contrat.statut == 'BROUILLON'}"><div class="alert alert-info">Le RH prépare votre contrat et ses dates. Vous pourrez ensuite confirmer votre accord.</div></c:when>
     <c:otherwise>
      <p><strong>Début :</strong> <c:out value="${contrat.dateDebut}"/> <c:if test="${not empty contrat.dateFin}"> · <strong>Fin :</strong> <c:out value="${contrat.dateFin}"/></c:if></p>
      <c:choose>
       <c:when test="${contrat.statut == 'SIGNE'}"><div class="alert alert-success">Contrat signé le ${f:dateTime(contrat.signeLe)} (heure de Madagascar). Votre candidature est à l’étape Embauche.</div></c:when>
       <c:otherwise>
        <form method="post" action="${pageContext.request.contextPath}/candidat/contrats/${contrat.id}/signer">
         <input type="hidden" name="revision" value="${contrat.revision}">
         <div class="form-check mb-3"><input class="form-check-input" type="checkbox" id="accord-${contrat.id}" name="confirmation" value="true" required><label class="form-check-label" for="accord-${contrat.id}">J’ai lu les conditions et les dates ci-dessus et je confirme mon accord.</label></div>
         <p class="small text-muted">Cette confirmation enregistre votre accord dans l’application avec votre identité et la date.</p>
         <button class="btn btn-success">Confirmer et signer le contrat</button>
        </form>
       </c:otherwise>
      </c:choose>
     </c:otherwise>
    </c:choose>
   </div>
  </article>
 </c:forEach>
</main></body></html>
