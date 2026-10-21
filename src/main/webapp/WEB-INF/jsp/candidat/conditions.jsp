<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Mes conditions de travail · Candidat</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <link href="${pageContext.request.contextPath}/assets/conditions.css" rel="stylesheet">
</head><body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main class="container conditions-page"><c:if test="${apercu}"><div class="alert alert-info">Aperçu statique : candidature Design UI/UX ayant passé la troisième sélection. Les montants sont des exemples et aucune réponse ne sera enregistrée.</div></c:if>
 <a href="${pageContext.request.contextPath}/candidat/offres" class="small text-decoration-none">← Voir les offres</a>
 <header class="conditions-hero"><div class="conditions-kicker">Mon espace candidat</div><h1>Mes conditions de travail</h1><p>Consultez la proposition du RH. Vous pouvez demander des ajustements et suivre sa décision depuis cette page.</p></header>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <div class="row g-3 mb-4">
  
 </div>
 <c:forEach var="d" items="${dossiers}">
  <article class="card shadow-sm mb-4">
   <div class="card-header"><h2 class="h5 mb-1"><c:out value="${d.intitule}"/></h2><span class="small text-muted" <c:if test="${apercu}">hidden</c:if>>Candidature n° ${d.id}</span></div>
   <div class="card-body">
    <c:choose>
     <c:when test="${empty d.actuelle}"><div class="alert alert-info mb-0">Le RH prépare les conditions de ce poste. Revenez sur cette page pour les consulter.</div></c:when>
     <c:otherwise>
      <c:if test="${not empty d.actuelle.repondreAvant}">
       <p class="small">Réponse attendue avant le <strong>${f:dateTime(d.actuelle.repondreAvant)}</strong> (heure de Madagascar).</p>
      </c:if>
      <c:choose>
       <c:when test="${d.actuelle.roleProposant.code == 'RH'}"><span class="badge bg-primary mb-3">Proposition du RH</span></c:when>
       <c:when test="${d.actuelle.decisionRh.code == 'EN_ATTENTE'}"><div class="alert alert-warning"><strong>Votre proposition est en attente de validation.</strong> Le RH examine vos changements. Ils ne sont pas encore acceptés.</div></c:when>
       <c:when test="${d.actuelle.decisionRh.code == 'VALIDEE'}"><div class="alert alert-success"><strong>Vos changements ont été validés par le RH.</strong></div></c:when>
       <c:otherwise><div class="alert alert-danger"><strong>Vos changements ont été refusés par le RH.</strong> La proposition RH initiale reste consultable ci-dessous.</div></c:otherwise>
      </c:choose>
      <div class="row g-4">
       <div class="col-lg-5">
        <h3 class="h6 mb-3">Proposition RH initiale</h3>
        <c:set var="affichee" value="${d.initiale}"/><%@ include file="../fragments/condition-summary.jsp" %>
       </div>
       <div class="col-lg-7">
        <c:choose>
         <c:when test="${d.expire}"><div class="alert alert-danger">Le délai de réponse est expiré. Contactez le RH pour demander une prolongation.</div></c:when>
         <c:when test="${d.actuelle.roleProposant.code == 'RH' and empty d.actuelle.reponse}">
          <h3 class="h6 mb-2">Votre réponse</h3>
          <div class="d-flex flex-wrap gap-2 mb-3" role="group" aria-label="Réponse aux conditions">
           <c:forEach var="r" items="${reponsesConditions}">
            <button type="${(r.code == 'OUI' or r.code == 'NON') and not apercu ? 'submit' : 'button'}" form="${r.code == 'NON' ? 'refuser' : 'accepter'}-${d.id}" class="btn btn-outline-primary" data-reponse="<c:out value='${r.code}'/>" aria-pressed="false" aria-controls="modifications-${d.id}"><c:choose><c:when test="${r.code == 'OUI'}">Oui</c:when><c:when test="${r.code == 'NON'}">Non</c:when><c:otherwise>Autre remarque</c:otherwise></c:choose></button>
           </c:forEach>
          </div>
          <c:if test="${not apercu}"><form id="refuser-${d.id}" method="post" action="${pageContext.request.contextPath}/candidat/conditions/candidature/${d.id}/refuser"></form><form id="accepter-${d.id}" method="post" action="${pageContext.request.contextPath}/candidat/conditions/candidature/${d.id}/accepter"></form></c:if><p class="small text-muted" data-reponse-message role="status" aria-live="polite">Choisissez une réponse. « Autre remarque » permet de proposer des changements.</p>
          <div id="modifications-${d.id}" data-modifications <c:if test="${candidatureErreur != d.id}">hidden</c:if>>
          <p class="small text-muted">Les champs reprennent les conditions du RH. Ajustez ceux que vous souhaitez négocier.</p>
          <form method="post" data-apercu="${apercu}" action="<c:choose><c:when test="${apercu}">${pageContext.request.contextPath}/candidat/conditions/apercu</c:when><c:otherwise>${pageContext.request.contextPath}/candidat/conditions/candidature/${d.id}/proposer</c:otherwise></c:choose>"><fieldset data-modification-fields <c:if test="${candidatureErreur != d.id}">disabled</c:if>>
           <c:set var="formPrefix" value="candidature-${d.id}"/><c:set var="valeurs" value="${candidatureErreur == d.id ? condition : d.actuelle}"/>
           <%@ include file="../fragments/condition-fields.jsp" %>
           <button class="btn btn-primary mt-3">Envoyer ma proposition au RH</button>
           <p class="small text-muted mt-2 mb-0">Cette demande concerne uniquement votre candidature.</p>
           </fieldset>
          </form>
          </div>
         </c:when>
         <c:when test="${d.actuelle.roleProposant.code == 'CANDIDAT' and d.actuelle.decisionRh.code == 'REFUSEE'}">
          <form method="post" action="${pageContext.request.contextPath}/candidat/conditions/candidature/${d.id}/accepter-initiales" class="mb-3">
           <button class="btn btn-success">Accepter les conditions RH initiales</button>
          </form>
          <h3 class="h6">Envoyer une nouvelle contre-proposition</h3>
          <form method="post" action="${pageContext.request.contextPath}/candidat/conditions/candidature/${d.id}/proposer" class="mb-3">
           <c:set var="formPrefix" value="candidature-${d.id}"/><c:set var="valeurs" value="${candidatureErreur == d.id ? condition : d.actuelle}"/>
           <%@ include file="../fragments/condition-fields.jsp" %>
           <button class="btn btn-primary mt-3">Envoyer ma proposition au RH</button>
          </form>
          <form method="post" action="${pageContext.request.contextPath}/candidat/conditions/candidature/${d.id}/refuser">
           <button class="btn btn-outline-danger">Refuser les conditions et clôturer ma candidature</button>
          </form>
         </c:when>
         <c:otherwise><c:if test="${d.actuelle.roleProposant.code == 'CANDIDAT' and d.actuelle.decisionRh.code == 'VALIDEE'}"><form method="post" action="${pageContext.request.contextPath}/candidat/conditions/candidature/${d.id}/accepter" class="mb-3"><button class="btn btn-success">Oui, accepter les conditions et passer au contrat</button></form></c:if><h3 class="h6 mb-3">Votre dernière proposition</h3><c:set var="affichee" value="${d.actuelle}"/><%@ include file="../fragments/condition-summary.jsp" %></c:otherwise>
        </c:choose>
       </div>
      </div>
      <%@ include file="../fragments/condition-history.jsp" %>
     </c:otherwise>
    </c:choose>
   </div>
  </article>
 </c:forEach>
<noscript><div class="alert alert-info">Activez JavaScript pour choisir « Autre remarque » et modifier les conditions.</div></noscript></main><script src="${pageContext.request.contextPath}/assets/conditions-candidat.js" defer></script></body></html>
