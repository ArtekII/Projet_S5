<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
 <meta charset="UTF-8">
 <meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Candidats — ${offre.poste.intitule}</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main class="container py-4">
 <a href="${pageContext.request.contextPath}/rh/dashboard" class="btn btn-sm btn-secondary mb-3">&larr; Retour au dashboard</a>
 <h3>Candidats pour : ${offre.poste.intitule}</h3>
 <p class="text-muted">${offre.departement.nom} — ${offre.statut.libelle}</p>

 <div class="card shadow-sm mb-4">
  <div class="card-header">Questionnaire de l'offre</div>
  <div class="card-body">
   <c:choose>
    <c:when test="${empty questionnaire}">
     <div class="alert alert-warning">Aucun questionnaire n'est encore configuré pour cette offre.</div>
     <form method="post" action="${pageContext.request.contextPath}/rh/questionnaire">
      <input type="hidden" name="idOffre" value="${offre.id}">
      <div class="row g-2 mb-3">
       <div class="col-md-8"><input name="titre" class="form-control" required placeholder="Titre du questionnaire"></div>
       <div class="col-md-4"><input name="seuilReussite" type="number" min="0" step="0.01" class="form-control" required placeholder="Seuil de réussite"></div>
      </div>
      <c:forEach begin="0" end="4" varStatus="q">
       <div class="row g-2 mb-2">
        <div class="col-md-5"><input name="enonce" class="form-control form-control-sm" placeholder="Question ${q.count}"></div>
        <div class="col-md-2"><input name="points" type="number" min="0" step="0.01" value="1" class="form-control form-control-sm"></div>
        <div class="col-md-2"><select name="type" class="form-select form-select-sm"><option value="QCM">QCM</option><option value="TEXTE">Réponse libre</option></select></div>
        <div class="col-md-3"><input name="choix" class="form-control form-control-sm" placeholder="A|B|C"></div>
        <div class="col-md-2 offset-md-10"><input name="bonneReponse" type="number" min="0" class="form-control form-control-sm" placeholder="Bonne réponse (0...)"></div>
       </div>
      </c:forEach>
      <button class="btn btn-primary" type="submit">Créer et envoyer aux candidats</button>
     </form>
    </c:when>
    <c:otherwise>
      <form method="post" action="${pageContext.request.contextPath}/rh/offre/${offre.id}/questionnaire">
      <div class="row g-2 mb-3">
       <div class="col-md-8"><label class="form-label">Titre</label><input name="titre" value="${questionnaire.titre}" class="form-control" required></div>
       <div class="col-md-4"><label class="form-label">Seuil de réussite</label><input name="seuilReussite" value="${questionnaire.seuilReussite}" type="number" min="0" step="0.01" class="form-control" required></div>
      </div>
      <p class="small text-muted">Modifiez les questions puis enregistrez. Les changements seront utilisés par les candidats qui n'ont pas encore envoyé leurs réponses.</p>
      <ol class="mb-3">
       <c:forEach var="q" items="${questions}">
        <li class="mb-2">
         <div class="row g-2">
          <div class="col-md-5"><input name="enonce" value="${q.enonce}" class="form-control form-control-sm" required></div>
          <div class="col-md-2"><input name="points" value="${q.points}" type="number" min="0" step="0.01" class="form-control form-control-sm" required></div>
          <div class="col-md-2"><select name="type" class="form-select form-select-sm"><option value="QCM" ${q.type.code eq 'QCM' ? 'selected' : ''}>QCM</option><option value="TEXTE" ${q.type.code eq 'TEXTE' ? 'selected' : ''}>Réponse libre</option></select></div>
          <div class="col-md-3"><input name="choix" value="${q.choixTexte}" class="form-control form-control-sm" placeholder="A|B|C"></div>
          <div class="col-md-2 offset-md-10"><input name="bonneReponse" value="${q.bonneReponseIndex}" type="number" min="0" class="form-control form-control-sm" placeholder="Bonne réponse (0...)"></div>
         </div>
        </li>
       </c:forEach>
      </ol>
      <button class="btn btn-primary" type="submit">Modifier et renvoyer aux candidats</button>
      </form>
    </c:otherwise>
   </c:choose>
  </div>
 </div>

 <div class="card shadow-sm">
  <div class="card-header">Liste des candidats (${candidaturesRh.size()})</div>
  <div class="card-body">
   <c:if test="${empty candidaturesRh}"><div class="text-muted">Aucune candidature pour cette offre.</div></c:if>
   <c:forEach var="candidat" items="${candidaturesRh}">
    <div class="border rounded p-3 mb-3">
     <div class="d-flex justify-content-between">
      <div><strong>${candidat.candidat}</strong><div class="small text-muted">Candidature #${candidat.candidature.id}</div></div>
      <span class="badge bg-secondary">${candidat.statut}</span>
     </div>
     <div class="small mt-2">Étape : <b>${candidat.etape}</b> → ${candidat.etapeSuivante}</div>
     <c:if test="${not empty questionnaire and candidat.questionnaire.repondu}">
      <details class="mt-2" open>
       <summary>Réponses et points — score ${candidat.candidature.scoreQuestionnaire} / ${questionnaire.seuilReussite}</summary>
       <form method="post" action="${pageContext.request.contextPath}/rh/candidature/${candidat.candidature.id}/questionnaire/noter">
        <table class="table table-sm mt-2">
         <thead><tr><th>Question</th><th>Réponse</th><th>Points</th></tr></thead>
         <tbody>
          <c:forEach var="q" items="${candidat.questionnaire.questions}">
           <c:set var="rep" value="${candidat.questionnaire.getReponseDe(q)}"/>
           <tr>
            <td>${q.enonce}</td>
            <td><c:choose><c:when test="${not empty rep.choix}">${rep.choix.libelle}</c:when><c:when test="${not empty rep.reponseTexte}">${rep.reponseTexte}</c:when><c:otherwise>-</c:otherwise></c:choose></td>
            <td>${rep.pointsObtenus}<c:if test="${not empty rep.reponseTexte}"><input name="note_${q.id}" type="number" min="0" max="${q.points}" step="0.01" class="form-control form-control-sm mt-1" placeholder="/ ${q.points}"></c:if></td>
           </tr>
          </c:forEach>
         </tbody>
        </table>
        <button class="btn btn-sm btn-outline-primary" type="submit">Enregistrer les points</button>
       </form>
      </details>
     </c:if>
     <c:if test="${candidat.codeEtape eq 'ENTRETIEN'}">
      <form method="post" action="${pageContext.request.contextPath}/rh/candidature/${candidat.candidature.id}/entretien" class="row g-2 mt-3">
       <div class="col-md-4"><label class="form-label small">Date et heure</label><input name="dateEntretien" type="datetime-local" class="form-control form-control-sm" required></div>
       <div class="col-md-4"><label class="form-label small">Lieu</label><input name="lieu" class="form-control form-control-sm" placeholder="Salle ou lien visioconférence" required></div>
       <div class="col-md-4"><label class="form-label small">Honnêteté CV /20</label><input name="noteHonneteteCv" type="number" min="0" max="20" step="0.01" class="form-control form-control-sm" required></div>
       <div class="col-md-4"><input name="notePersonnalite" type="number" min="0" max="20" step="0.01" class="form-control form-control-sm" placeholder="Personnalité /20" required></div>
       <div class="col-md-4"><input name="noteCompetences" type="number" min="0" max="20" step="0.01" class="form-control form-control-sm" placeholder="Compétences /20" required></div>
       <div class="col-12"><button class="btn btn-sm btn-primary">Enregistrer l'entretien</button></div>
      </form>
     </c:if>
     <c:if test="${candidat.statut eq 'En cours'}">
      <div class="d-flex gap-2 mt-3">
       <form method="post" action="${pageContext.request.contextPath}/rh/candidature/${candidat.candidature.id}/valider"><button class="btn btn-sm btn-success">Valider l'étape</button></form>
       <form method="post" action="${pageContext.request.contextPath}/rh/candidature/${candidat.candidature.id}/eliminer"><button class="btn btn-sm btn-outline-danger">Éliminer</button></form>
      </div>
     </c:if>
    </div>
   </c:forEach>
  </div>
 </div>
</main>
</body>
</html>
