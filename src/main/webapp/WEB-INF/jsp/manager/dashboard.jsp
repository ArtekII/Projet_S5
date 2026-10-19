<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Manager</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <h4>Manager — <span>${user.departement.nom}</span></h4>
 <p class="text-muted small">Deux actions : (1) Demander une offre sur un poste existant (dropdown) — (2) Demander un nouveau poste au RH.</p>
 <div class="row g-4">
  <div class="col-lg-6">
   <div class="card shadow-sm"><div class="card-header bg-primary text-white">1) Demande d'offre — poste existant</div>
    <div class="card-body">
     <form method="post" action="${pageContext.request.contextPath}/manager/offre">
      <div class="mb-3"><label class="form-label">Poste (dropdown)</label>
       <select name="posteId" class="form-select" required>
        <option value="">-- choisir --</option>
        <c:forEach var="p" items="${postes}">
         <option value="${p.id}">${p.intitule}</option>
        </c:forEach>
       </select>
       <div class="form-text">Si le poste n'est pas dans la liste, utilisez le formulaire 2 ci-contre.</div>
      </div>
      <div class="mb-3"><label class="form-label">Années d'expérience requises</label><input name="anneesExperience" type="number" min="0" class="form-control" required value="2"></div>
      <div class="mb-3"><label class="form-label">Nombre de places</label><input name="nbPlaces" type="number" min="1" class="form-control" required value="5" placeholder="ex: 5 dev junior"></div>
      <button class="btn btn-primary w-100">Envoyer au RH</button>
     </form>
    </div>
   </div>
  </div>
  <div class="col-lg-6">
   <div class="card shadow-sm"><div class="card-header bg-secondary text-white">2) Demande de nouveau poste (pas dans le dropdown)</div>
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
  <div class="card-body p-0"><div class="table-responsive"><table class="table table-sm mb-0">
   <thead><tr><th>Poste</th><th>Exp</th><th>Places</th><th>Salaire</th><th>Statut</th><th>Date</th></tr></thead>
   <tbody>
    <c:forEach var="o" items="${mesOffres}">
     <tr>
      <td>${o.poste.intitule}</td>
      <td>${o.anneesExperience}</td>
      <td>${o.nbPlaces}</td>
      <td><c:choose><c:when test="${empty o.salaire}">-</c:when><c:otherwise>${o.salaire} Ar</c:otherwise></c:choose></td>
      <td><span class="badge bg-info">${o.statut.libelle}</span></td>
      <td>${f:dateTime(o.createdAt)}</td>
     </tr>
    </c:forEach>
    <c:if test="${empty mesOffres}"><tr><td colspan="6" class="text-center text-muted py-3">Aucune offre</td></tr></c:if>
   </tbody>
  </table></div></div>
 </div>
</div>
</body></html>
