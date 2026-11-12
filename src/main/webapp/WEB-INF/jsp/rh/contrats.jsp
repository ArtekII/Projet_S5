<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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
 <header class="conditions-hero">
  <h1>Contrats des employés</h1>
  <p>Modifiez le contrat d'un employé ou programmez son contrat successif. Le nouveau contrat devient le contrat courant à sa date de commencement.</p>
 </header>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>

 <section class="card shadow-sm mb-4" aria-labelledby="formulaire-contrat">
  <div class="card-header"><h2 id="formulaire-contrat" class="h5 mb-0">Modifier ou programmer un contrat</h2></div>
  <div class="card-body">
   <c:if test="${empty employes}">
    <div class="conditions-empty">Aucun employé enregistré. Les employés sont créés automatiquement à la confirmation d'un contrat de candidature.</div>
   </c:if>
   <form method="post" action="${pageContext.request.contextPath}/rh/contrats" class="row g-3">
     <div class="col-lg-3 col-md-6">
      <label for="idEmp" class="form-label">Employé (idEmp)</label>
      <select id="idEmp" name="idEmp" class="form-select" required>
       <option value="">Choisir un employé…</option>
       <c:forEach var="e" items="${employes}">
        <option value="${e.id}">Emp ${e.id} — <c:out value="${e.email}"/> (<c:out value="${e.typeCode}"/>)</option>
       </c:forEach>
      </select>
     </div>
     <div class="col-lg-3 col-md-6">
      <label for="typeContrat" class="form-label">Type de contrat</label>
      <select id="typeContrat" name="typeContrat" class="form-select" required>
       <option value="">Choisir un type…</option>
       <c:forEach var="type" items="${typesContrat}">
        <option value="${type.code}"><c:out value="${type.code}"/> — <c:out value="${type.libelle}"/></option>
       </c:forEach>
      </select>
     </div>
     <div class="col-lg-2 col-md-6">
      <label for="dateDebut" class="form-label">Date de commencement</label>
      <input id="dateDebut" type="date" name="dateDebut" class="form-control" required>
     </div>
     <div class="col-lg-2 col-md-6">
      <label for="dureeMois" class="form-label">Durée (mois)</label>
      <input id="dureeMois" type="number" name="dureeMois" class="form-control" min="1" step="1" placeholder="ex. 3">
     </div>
     <div class="col-lg-2 d-flex align-items-end">
      <button class="btn btn-primary w-100">Enregistrer</button>
     </div>
    </form>
    <p class="small text-muted mt-3 mb-0">
     Une date aujourd'hui ou passée applique immédiatement le nouveau contrat (par exemple CE → CDI).
     Une date future le programme : il sera activé automatiquement ce jour-là. La durée est obligatoire pour un CE et ignorée pour un CDI.
    </p>
   </div>
  </section>

 <section aria-labelledby="employes-title" class="mb-5">
  <h2 id="employes-title" class="h4 mb-3">Employés et contrat actuel</h2>
  <c:if test="${empty employes}"><div class="conditions-empty">Aucun contrat d'employé à afficher.</div></c:if>
  <c:if test="${not empty employes}">
   <div class="table-responsive">
    <table class="table table-striped table-hover align-middle bg-white">
     <thead>
      <tr>
       <th>Employé</th><th>Embauche</th><th>Contrat actuel</th><th>Commencement</th><th>Fin</th><th>Statut</th><th>Historique</th>
      </tr>
     </thead>
     <tbody>
      <c:forEach var="e" items="${employes}">
       <tr>
        <td><strong>Emp ${e.id}</strong><br><span class="small text-muted"><c:out value="${e.email}"/></span><br><a class="small" href="${pageContext.request.contextPath}/rh/dossiers/${e.utilisateurId}">Dossier et intégration</a></td>
        <td>${e.dateEmbauche}</td>
        <td><span class="badge bg-primary"><c:out value="${e.typeCode}"/></span><br><span class="small text-muted"><c:out value="${e.typeLibelle}"/></span></td>
        <td>${empty e.dateDebut ? '—' : e.dateDebut}</td>
        <td>
         <c:choose>
          <c:when test="${e.typeCode == 'CDI'}">Indéterminée</c:when>
          <c:when test="${empty e.dateFin}">—</c:when>
          <c:otherwise>${e.dateFin} <span class="small text-muted">(${e.dureeMois} mois)</span></c:otherwise>
         </c:choose>
        </td>
        <td><span class="badge ${e.statut == 'PROGRAMME' ? 'bg-warning text-dark' : 'bg-success'}"><c:out value="${e.statut}"/></span></td>
        <td>
         <details>
          <summary class="small">Voir les contrats</summary>
          <c:if test="${empty historiques[e.id]}"><p class="small text-muted mb-0">Aucun historique.</p></c:if>
          <ul class="small mb-0 ps-3">
           <c:forEach var="h" items="${historiques[e.id]}">
            <li>
             Contrat n° ${h.id} — <strong><c:out value="${h.typeCode}"/></strong>
             · ${empty h.dateDebut ? 'date non renseignée' : h.dateDebut}
             <c:choose>
              <c:when test="${h.typeCode == 'CDI'}">→ indéterminée</c:when>
              <c:when test="${not empty h.dateFin}">→ ${h.dateFin} (${h.dureeMois} mois)</c:when>
             </c:choose>
             · <c:out value="${h.statut}"/>
            </li>
           </c:forEach>
          </ul>
         </details>
        </td>
       </tr>
      </c:forEach>
     </tbody>
    </table>
   </div>
  </c:if>
 </section>

 <section aria-labelledby="contrats-candidatures-title">
  <h2 id="contrats-candidatures-title" class="h4 mb-3">Contrats issus des candidatures</h2>
  <c:if test="${empty contrats}"><div class="conditions-empty">Aucun contrat pour le moment.</div></c:if>
  <c:forEach var="contrat" items="${contrats}">
   <article class="card mb-3 shadow-sm"><div class="card-header">
    Contrat n° ${contrat.id}
    <c:if test="${not empty contrat.idCandidature}"> · Candidature n° ${contrat.idCandidature}</c:if>
    <c:if test="${not empty contrat.idEmploye}"> · Employé n° ${contrat.idEmploye}</c:if>
    <span class="badge bg-primary"><c:out value="${contrat.typeContrat.code}"/></span>
    <c:if test="${not empty contrat.statut}"><span class="badge bg-secondary"><c:out value="${contrat.statut}"/></span></c:if>
   </div><div class="card-body">
    <c:if test="${not empty contrat.dateDebut}"><p class="small mb-2">Du ${contrat.dateDebut}
     <c:choose><c:when test="${contrat.typeContrat.code == 'CDI'}">au indéterminé</c:when><c:when test="${not empty contrat.dateFin}">au ${contrat.dateFin}</c:when></c:choose>
    </p></c:if>
    <c:if test="${not empty contrat.condition}"><c:set var="affichee" value="${contrat.condition}"/><%@ include file="../fragments/condition-summary.jsp" %></c:if>
   </div></article>
  </c:forEach>
 </section>
</main></body></html>
