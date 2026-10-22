<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Conditions de travail · RH</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <link href="${pageContext.request.contextPath}/assets/conditions.css" rel="stylesheet">
</head><body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main class="container conditions-page">
 <a href="${pageContext.request.contextPath}/rh/dashboard" class="small text-decoration-none">← Tableau de bord RH</a>
 <header class="conditions-hero"><div class="conditions-kicker">Espace ressources humaines</div><h1>Conditions de travail</h1><p>Définissez une proposition par poste, puis examinez les ajustements demandés par chaque candidat.</p></header>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <div class="row g-3 mb-4">
 </div>
 <section aria-labelledby="postes-title" class="mb-5">
  <h2 id="postes-title" class="h4 mb-3">Propositions par poste</h2>
  <p class="text-muted small">Une nouvelle version ne modifie pas les négociations déjà ouvertes avec les candidats.</p>
  <c:if test="${empty postesConditions}"><div class="conditions-empty">Aucun poste actif. Ajoutez d’abord un poste au catalogue.</div></c:if>
  <div class="row g-3">
   <c:forEach var="p" items="${postesConditions}">
    <div class="col-lg-6"><article class="card h-100 shadow-sm">
     <div class="card-header d-flex justify-content-between align-items-center gap-2"><h3 class="h6 mb-0"><c:out value="${p.intitule}"/></h3><span class="badge ${empty p.conditions ? 'bg-secondary' : 'bg-primary'}">${empty p.conditions ? 'À définir' : 'Proposition disponible'}</span></div>
     <div class="card-body">
      <c:choose><c:when test="${not empty p.conditions}"><p class="small text-muted">Version ${p.conditions.version}</p><c:set var="affichee" value="${p.conditions}"/><%@ include file="../fragments/condition-summary.jsp" %></c:when><c:otherwise><p class="text-muted">Les candidats ne disposent pas encore de conditions pour ce poste.</p></c:otherwise></c:choose>
      <details class="mt-3" <c:if test="${empty p.conditions or posteErreur == p.id}">open</c:if>>
       <summary>${empty p.conditions ? 'Définir les conditions' : 'Proposer une nouvelle version'}</summary>
       <form class="mt-3" method="post" action="${pageContext.request.contextPath}/rh/conditions/poste/${p.id}">
        <c:set var="formPrefix" value="poste-${p.id}"/><c:set var="valeurs" value="${posteErreur == p.id ? condition : p.conditions}"/>
        <%@ include file="../fragments/condition-fields.jsp" %>
        <button class="btn btn-primary mt-3">Enregistrer les conditions</button>
       </form>
      </details>
     </div>
    </article></div>
   </c:forEach>
  </div>
 </section>
 <section aria-labelledby="demandes-title"><h2 id="demandes-title" class="h4 mb-3">Négociations avec les candidats</h2>
  <c:if test="${empty dossiers}"><div class="conditions-empty">Aucune candidature à l’étape « Conditions » pour le moment. Les demandes des candidats apparaîtront ici.</div></c:if>
  <c:forEach var="d" items="${dossiers}">
   <article class="card shadow-sm mb-3">
    <div class="card-header"><h3 class="h6 mb-1"><c:out value="${d.intitule}"/></h3><span class="small text-muted"><c:out value="${d.email}"/> · Candidature n° ${d.id}</span></div>
    <div class="card-body">
     <p><strong>Type prévu :</strong> ${d.cdi_autorise ? 'CDI autorisé' : "Contrat d’essai"}</p>
     <c:if test="${not empty d.actuelle.repondreAvant}">
      <p class="${d.expire ? 'text-danger' : 'text-muted'}">Réponse attendue avant le ${f:dateTime(d.actuelle.repondreAvant)} (heure de Madagascar). ${d.expire ? 'Délai expiré.' : ''}</p>
      <form method="post" action="${pageContext.request.contextPath}/rh/conditions/candidature/${d.id}/prolonger" class="mb-3">
       <button class="btn btn-outline-secondary">Prolonger le délai de réponse</button>
      </form>
     </c:if>
     <c:choose>
      <c:when test="${empty d.actuelle}"><p class="text-muted mb-0">Définissez les conditions du poste pour permettre au candidat de les consulter.</p></c:when>
      <c:when test="${d.actuelle.roleProposant.code == 'RH'}"><span class="badge bg-secondary mb-3">En attente du candidat</span><c:set var="affichee" value="${d.actuelle}"/><%@ include file="../fragments/condition-summary.jsp" %></c:when>
      <c:otherwise>
       <c:choose><c:when test="${d.actuelle.decisionRh.code == 'EN_ATTENTE'}"><span class="badge bg-warning text-dark mb-3">À examiner</span></c:when><c:when test="${d.actuelle.decisionRh.code == 'VALIDEE'}"><span class="badge bg-success mb-3">Changements validés</span></c:when><c:otherwise><span class="badge bg-danger mb-3">Changements refusés</span></c:otherwise></c:choose>
       <div class="table-responsive"><table class="table table-bordered"><caption>Comparaison avec la proposition RH initiale</caption><thead class="table-light"><tr><th scope="col">Condition</th><th scope="col">Proposition RH</th><th scope="col">Demande du candidat</th></tr></thead><tbody>
        <tr><th scope="row">Salaire</th><td><c:out value="${d.initiale.salaire}"/> Ar</td><td><strong><c:out value="${d.actuelle.salaire}"/> Ar</strong></td></tr>
        <tr><th scope="row">Heures / semaine</th><td><c:out value="${d.initiale.heuresParSemaine}"/> h</td><td><c:out value="${d.actuelle.heuresParSemaine}"/> h</td></tr>
        <tr><th scope="row">Horaires</th><td><c:out value="${d.initiale.horaires}" default="Non précisés"/></td><td><c:out value="${d.actuelle.horaires}" default="Non précisés"/></td></tr>
        <tr><th scope="row">Lieu</th><td><c:out value="${d.initiale.lieuTravail}"/></td><td><c:out value="${d.actuelle.lieuTravail}"/></td></tr>
       </tbody></table></div>
       <c:if test="${not empty d.actuelle.remarque}"><p class="text-break"><strong>Remarque du candidat :</strong> <c:out value="${d.actuelle.remarque}"/></p></c:if>
       <c:if test="${d.actuelle.decisionRh.code == 'EN_ATTENTE'}">
        <form method="post" action="${pageContext.request.contextPath}/rh/conditions/${d.actuelle.id}/decision" class="d-flex flex-wrap gap-2">
         <button name="valider" value="true" class="btn btn-success">Valider les changements</button>
         <button name="valider" value="false" class="btn btn-outline-danger">Refuser les changements</button>
        </form>
       </c:if>
      </c:otherwise>
     </c:choose>
     <%@ include file="../fragments/condition-history.jsp" %>
    </div>
   </article>
  </c:forEach>
 </section>
</main></body></html>
