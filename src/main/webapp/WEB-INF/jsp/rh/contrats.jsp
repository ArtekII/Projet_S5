<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
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
 <header class="conditions-hero"><h1>Préparation des contrats</h1><p>Renseignez les dates pour permettre au candidat de signer son contrat.</p></header>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <c:if test="${empty contrats}"><div class="conditions-empty">Aucun contrat pour le moment.</div></c:if>
 <c:forEach var="contrat" items="${contrats}">
  <article class="card mb-3 shadow-sm"><div class="card-header">
   Contrat n° ${contrat.id} · Candidature n° ${contrat.idCandidature}
   <span class="badge bg-primary"><c:out value="${contrat.typeContrat.code}"/></span>
  </div><div class="card-body">
   <c:set var="affichee" value="${contrat.condition}"/><%@ include file="../fragments/condition-summary.jsp" %>
   <c:choose>
    <c:when test="${contrat.statut == 'SIGNE'}">
     <p class="text-success">Signé le ${f:dateTime(contrat.signeLe)} par le candidat n° ${contrat.idSignataire} (heure de Madagascar).</p>
     <p>Début : <c:out value="${contrat.dateDebut}"/> <c:if test="${not empty contrat.dateFin}"> · Fin : <c:out value="${contrat.dateFin}"/></c:if></p>
    </c:when>
    <c:otherwise>
     <p>${contrat.statut == 'A_SIGNER' ? 'En attente de signature du candidat' : 'Brouillon à préparer'}</p>
     <form method="post" action="${pageContext.request.contextPath}/rh/contrats/${contrat.id}/preparer">
      <label class="form-label" for="debut-${contrat.id}">Date de début</label>
      <input class="form-control mb-3" id="debut-${contrat.id}" name="dateDebut" type="date" value="${contrat.dateDebut}" required>
      <c:if test="${contrat.typeContrat.code != 'CDI'}">
       <label class="form-label" for="fin-${contrat.id}">Date de fin du contrat d’essai</label>
       <input class="form-control mb-3" id="fin-${contrat.id}" name="dateFin" type="date" value="${contrat.dateFin}" required>
      </c:if>
      <button class="btn btn-primary">Mettre à disposition pour signature</button>
     </form>
    </c:otherwise>
   </c:choose>
  </div></article>
 </c:forEach>
</main></body></html>
