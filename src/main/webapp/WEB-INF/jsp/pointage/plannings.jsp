<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html><html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Plannings par poste · RH</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head><body class="bg-light app-shell"><jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main id="contenu" class="container pb-5" tabindex="-1">
 <h1 class="h2">Plannings par poste</h1>
 <p>Définissez les jours travaillés, les horaires et les pauses. Une nouvelle date d'effet conserve les versions précédentes.</p>
 <p><a href="${pageContext.request.contextPath}/rh/pointages">Consulter les pointages et préparer les variables de paie →</a></p>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <section class="card mb-4"><div class="card-body">
  <h2 class="h5">Définir un horaire</h2>
  <form:form modelAttribute="planningForm" method="post" action="${pageContext.request.contextPath}/rh/plannings" cssClass="row g-3">
   <input type="hidden" name="pointageCsrf" value="${pointageCsrf}">
   <form:errors path="*" element="div" cssClass="alert alert-danger" htmlEscape="true"/>
   <div class="col-md-5"><label for="posteId" class="form-label">Poste</label><form:select path="posteId" id="posteId" cssClass="form-select" required="required"><form:option value="" label="Choisir un poste…"/><c:forEach var="poste" items="${postes}"><form:option value="${poste.id}" htmlEscape="true"><c:out value="${poste.intitule}"/></form:option></c:forEach></form:select></div>
   <div class="col-md-3"><label for="jourSemaine" class="form-label">Jour de la semaine</label><form:select path="jourSemaine" id="jourSemaine" cssClass="form-select"><form:option value="1" label="Lundi"/><form:option value="2" label="Mardi"/><form:option value="3" label="Mercredi"/><form:option value="4" label="Jeudi"/><form:option value="5" label="Vendredi"/><form:option value="6" label="Samedi"/><form:option value="7" label="Dimanche"/></form:select></div>
   <div class="col-md-4"><label for="dateEffet" class="form-label">Applicable à partir du</label><form:input path="dateEffet" id="dateEffet" type="date" cssClass="form-control" required="required"/></div>
   <div class="col-12"><div class="form-check"><form:checkbox path="jourTravaille" id="jourTravaille" cssClass="form-check-input"/><label for="jourTravaille" class="form-check-label">Jour travaillé (décochez pour définir un jour de repos)</label></div></div>
   <div class="col-md-3"><label for="heureDebut" class="form-label">Heure de début</label><form:input path="heureDebut" id="heureDebut" type="time" cssClass="form-control"/></div>
   <div class="col-md-3"><label for="heureFin" class="form-label">Heure de fin</label><form:input path="heureFin" id="heureFin" type="time" cssClass="form-control"/></div>
   <div class="col-md-3"><label for="pauseDebut" class="form-label">Début de pause</label><form:input path="pauseDebut" id="pauseDebut" type="time" cssClass="form-control"/></div>
   <div class="col-md-3"><label for="pauseFin" class="form-label">Fin de pause</label><form:input path="pauseFin" id="pauseFin" type="time" cssClass="form-control"/></div>
   <div class="col-12"><p class="small text-muted">Une fin antérieure au début indique un horaire de nuit jusqu'au lendemain. Les deux heures de pause sont facultatives. Les horaires sont exprimés dans le fuseau Indian/Antananarivo.</p><button class="btn btn-primary">Enregistrer le planning</button></div>
  </form:form>
 </div></section>
 <h2 class="h4">Historique des horaires</h2>
 <c:if test="${empty plannings}"><p class="text-muted">Aucun planning défini.</p></c:if>
 <div class="table-responsive"><table class="table table-striped bg-white align-middle"><thead><tr><th>Poste</th><th>Jour</th><th>Date d'effet</th><th>Horaires</th><th>Pause</th><th>Défini par</th></tr></thead><tbody>
  <c:forEach var="p" items="${plannings}"><tr><td><c:out value="${p.poste.intitule}"/></td><td><c:out value="${p.jourLibelle}"/></td><td>${p.dateEffet}</td><td><c:choose><c:when test="${p.jourTravaille}">${p.heureDebut} → ${p.heureFin}</c:when><c:otherwise>Repos</c:otherwise></c:choose></td><td><c:choose><c:when test="${not empty p.pauseDebut}">${p.pauseDebut} → ${p.pauseFin}</c:when><c:otherwise>Sans pause</c:otherwise></c:choose></td><td><c:out value="${p.rh.email}"/></td></tr></c:forEach>
 </tbody></table></div>
</main></body></html>
