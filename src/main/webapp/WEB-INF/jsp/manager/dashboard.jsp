<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Manager</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head>
<body class="bg-light app-shell">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main id="contenu" class="container" tabindex="-1">
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <header class="app-page-head"><div><span class="app-eyebrow">Mon équipe · <c:out value="${user.departement.nom}"/></span><h1>Mes recrutements</h1><p>Demandez une offre et suivez son avancement jusqu'à sa publication.</p></div><a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/manager/demandes">Suivre mes demandes de postes</a></header>
 <ol class="app-workflow" aria-label="Parcours de publication"><li><span>Manager</span></li><li><span>Ressources humaines</span></li><li><span>Finance</span></li><li><span>Direction</span></li><li><span>Publication</span></li></ol>
 <div class="row g-4">
  <div class="col-lg-6">
   <div class="card shadow-sm"><div class="card-header bg-primary text-white">Demander une offre</div>
    <div class="card-body">
     <form method="post" action="${pageContext.request.contextPath}/manager/offre">
      <div class="mb-3"><label class="form-label">Poste à pourvoir</label>
       <select name="posteId" class="form-select" required>
        <option value="">-- choisir --</option>
        <c:forEach var="p" items="${postes}">
         <option value="${p.id}">${p.intitule}</option>
        </c:forEach>
       </select>
       <div class="form-text">Votre poste manque dans la liste ? Proposez sa création avec le formulaire « Nouveau poste ».</div>
      </div>
      <div class="mb-3"><label class="form-label">Années d'expérience requises</label><input name="anneesExperience" type="number" min="0" class="form-control" required value="0"></div>
      <div class="mb-3"><label class="form-label">Nombre de places</label><input name="nbPlaces" type="number" min="1" class="form-control" required value="1"></div>
      <button class="btn btn-primary w-100">Envoyer au RH</button>
     </form>
    </div>
   </div>
  </div>
  <div class="col-lg-6">
   <div class="card shadow-sm"><div class="card-header bg-secondary text-white">Proposer un nouveau poste</div>
    <div class="card-body">
     <form method="post" action="${pageContext.request.contextPath}/manager/demande-poste">
      <div class="mb-3"><label class="form-label">Intitulé proposé</label><input name="intitule" class="form-control" required placeholder="ex: DevOps Junior"></div>
      <div class="mb-3"><label class="form-label">Justification</label><textarea name="justification" class="form-control" rows="3" placeholder="Pourquoi ce poste ?"></textarea></div>
      <button class="btn btn-secondary w-100">Demander au RH</button>
     </form>
    </div>
   </div>
  </div>
 </div>
 <div class="card shadow-sm mt-4"><div class="card-header">Mes offres</div>
  <div class="card-body" data-collection="mes offres" data-search-placeholder="Poste ou département"><div class="table-responsive"><table class="table table-sm mb-0">
   <thead><tr><th>Poste</th><th>Expérience</th><th>Places</th><th>Salaire</th><th>Statut</th><th>Date</th></tr></thead>
   <tbody>
    <c:forEach var="o" items="${mesOffres}">
     <tr data-item data-state="<c:out value='${o.statut.libelle}'/>">
      <td>${o.poste.intitule}</td>
      <td>${o.anneesExperience}</td>
      <td>${o.nbPlaces}</td>
      <td><c:choose><c:when test="${empty o.salaire}">-</c:when><c:otherwise>${o.salaire} Ar</c:otherwise></c:choose></td>
      <td><c:set var="offreSuivie" value="${o}"/><%@ include file="../fragments/offre-status.jspf" %></td>
      <td>${f:dateTime(o.createdAt)}</td>
     </tr>
    </c:forEach>
    <c:if test="${empty mesOffres}"><tr><td colspan="6" class="text-center text-muted py-3">Aucune offre</td></tr></c:if>
   </tbody>
  </table></div></div>
 </div>
</main>
</body></html>
