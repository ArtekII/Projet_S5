<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr"><head><meta charset="UTF-8"><title>Recrutement</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light"><jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <a href="${pageContext.request.contextPath}/rh/dashboard" class="btn btn-sm btn-secondary mb-3">Retour RH</a>
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <h4>Recrutement — ${offre.poste.intitule}</h4>
 <p class="text-muted">Offre #${offre.id} — ${offre.nbPlaces} place(s)</p>

 <div class="card shadow-sm mb-4"><div class="card-header">1. Première sélection des CV</div><div class="card-body">
  <form method="post" action="${pageContext.request.contextPath}/rh/offre/${offre.id}/selection-cv">
   <table class="table table-sm"><thead><tr><th></th><th>Nom</th><th>Prénom</th><th>Expérience</th><th>Email</th></tr></thead><tbody>
    <c:forEach var="cv" items="${candidatures}">
     <tr><td><input type="checkbox" name="cvIds" value="${cv.id}"></td><td>${cv.nom}</td><td>${cv.prenom}</td><td>${cv.anneeExperience} an(s)</td><td>${cv.email}</td></tr>
    </c:forEach>
    <c:if test="${empty candidatures}"><tr><td colspan="5" class="text-muted">Aucune candidature.</td></tr></c:if>
   </tbody></table>
   <button class="btn btn-primary" ${empty candidatures ? 'disabled' : ''}>Valider la sélection CV</button>
  </form>
  <div class="small text-muted mt-2">La liste est triée par années d'expérience décroissantes.</div>
 </div></div>

 <div class="card shadow-sm mb-4"><div class="card-header">2. Questionnaire</div><div class="card-body">
  <form method="post" action="${pageContext.request.contextPath}/rh/offre/${offre.id}/question" class="row g-2 mb-3">
   <div class="col-md-8"><input name="texte" class="form-control" required placeholder="Question"></div>
   <div class="col-md-2"><input name="pointsMax" type="number" min="1" class="form-control" required value="10" placeholder="Points"></div>
   <div class="col-md-2"><button class="btn btn-outline-primary w-100">Ajouter</button></div>
  </form>
  <ul><c:forEach var="q" items="${questions}"><li>${q.question} (${q.pointsMax} points)</li></c:forEach></ul>
  <c:if test="${not empty selection1}">
   <form method="post" action="${pageContext.request.contextPath}/rh/offre/${offre.id}/notes-questionnaire">
    <table class="table table-sm"><thead><tr><th>Candidat sélectionné</th><th>Note</th></tr></thead><tbody>
     <c:forEach var="s" items="${selection1}"><tr><td>${s.cvCandidat.prenom} ${s.cvCandidat.nom}</td><td><input name="points_${s.id}" type="number" min="0" step="0.01" class="form-control form-control-sm"></td></tr></c:forEach>
    </tbody></table><button class="btn btn-success">Enregistrer les notes / Top 5</button>
   </form>
  </c:if>
 </div></div>

 <div class="card shadow-sm mb-4"><div class="card-header">Top 5 questionnaire</div><div class="card-body">
  <ol>  <c:forEach var="s" items="${top5}"><li>${s.cvCandidat.prenom} ${s.cvCandidat.nom} — ${s.note} points</li></c:forEach></ol>
 </div></div>

 <div class="card shadow-sm mb-4"><div class="card-header">3. Entretiens et classement final</div><div class="card-body">
  <form method="post" action="${pageContext.request.contextPath}/rh/offre/${offre.id}/entretien" class="input-group mb-3">
   <input name="nom" class="form-control" required placeholder="Ex: Entretien technique"><button class="btn btn-outline-primary">Ajouter une grille</button>
  </form>
  <c:forEach var="entretien" items="${entretiens}">
   <h6>${entretien.nom}</h6>
   <form method="post" action="${pageContext.request.contextPath}/rh/offre/${offre.id}/notes-entretien">
    <input type="hidden" name="entretienId" value="${entretien.id}">
    <table class="table table-sm"><tbody><c:forEach var="s" items="${top5}"><tr><td>${s.cvCandidat.prenom} ${s.cvCandidat.nom}</td><td><input name="points_${s.id}" type="number" min="0" step="0.01" class="form-control form-control-sm"></td></tr></c:forEach></tbody></table>
    <button class="btn btn-success btn-sm">Enregistrer les notes et recalculer</button>
   </form>
  </c:forEach>
  <table class="table table-sm mt-3"><thead><tr><th>Rang</th><th>Candidat</th><th>Note finale</th></tr></thead><tbody>
   <c:forEach var="r" items="${classement}"><tr><td>${r.rang}</td><td>${r.cvCandidat.prenom} ${r.cvCandidat.nom}</td><td>${r.note}</td></tr></c:forEach>
  </tbody></table>
 </div></div>
</div></body></html>
