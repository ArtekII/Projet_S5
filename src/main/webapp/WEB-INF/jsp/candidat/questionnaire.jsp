<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr"><head><meta charset="UTF-8"><title>Questionnaire</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light"><jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container"><a href="${pageContext.request.contextPath}/candidat/offre/${offre.id}" class="btn btn-sm btn-secondary mb-3">Retour à l'offre</a>
 <div class="card shadow-sm"><div class="card-header">Questionnaire — ${offre.poste.intitule}</div><div class="card-body">
  <p>Vous avez été retenu(e) pour la première sélection. Voici le questionnaire à préparer :</p>
  <ol><c:forEach var="q" items="${questions}"><li class="mb-2">${q.question} <span class="text-muted">(${q.pointsMax} points)</span></li></c:forEach></ol>
  <c:if test="${empty questions}"><p class="text-muted">Le questionnaire n'est pas encore disponible.</p></c:if>
 </div></div>
</div></body></html>
