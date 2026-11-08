<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="fr"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Variables de pointage · Finance</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head><body class="bg-light"><jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main class="container pb-5"><h1 class="h2">Variables de pointage transmises par les RH</h1>
 <p>Durées validées pour la préparation de la paie. Les montants et majorations restent à calculer selon les règles de paie applicables.</p>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <form method="get" action="${pageContext.request.contextPath}/finance/pointages" class="row g-3 mb-4 align-items-end">
  <div class="col-sm-4"><label for="du" class="form-label">Du</label><input id="du" type="date" name="du" value="${du}" class="form-control" required></div>
  <div class="col-sm-4"><label for="au" class="form-label">Au (31 jours maximum)</label><input id="au" type="date" name="au" value="${au}" class="form-control" required></div>
  <div class="col-sm-4"><button class="btn btn-primary">Afficher</button></div>
 </form>
 <c:url var="exportCsv" value="/finance/pointages.csv"><c:param name="du" value="${du}"/><c:param name="au" value="${au}"/></c:url>
 <c:if test="${not empty variables}"><p><a href="<c:out value='${exportCsv}'/>" class="btn btn-outline-primary">Exporter les variables en CSV</a></p></c:if>
 <c:if test="${empty variables}"><p>Aucune variable transmise pour cette période.</p></c:if>
 <div class="table-responsive"><table class="table table-striped bg-white"><thead><tr><th>Employé</th><th>Date</th><th>Prévu</th><th>Travaillé</th><th>Retard</th><th>Heures sup.</th><th>Temps prévu non travaillé</th><th>Validé par</th><th>Transmission</th></tr></thead><tbody>
  <c:forEach var="v" items="${variables}"><tr><td>${v.employe.id}</td><td>${v.date}</td><td>${v.prevuFormate}</td><td>${v.travailFormate}</td><td>${v.retardFormate}</td><td>${v.supplementairesFormate}</td><td>${v.absenceFormate}</td><td><c:out value="${v.rh.email}"/></td><td>${v.transmisLe}</td></tr></c:forEach>
 </tbody></table></div>
</main></body></html>
