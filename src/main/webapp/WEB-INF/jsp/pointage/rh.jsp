<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Pointages et anomalies · RH</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head><body class="bg-light"><jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main class="container-fluid px-lg-5 pb-5">
 <h1 class="h2">Pointages et anomalies</h1><p><a href="${pageContext.request.contextPath}/rh/plannings">Définir les plannings par poste</a></p>
 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
 <form method="get" action="${pageContext.request.contextPath}/rh/pointages" class="row g-3 align-items-end mb-4">
  <div class="col-sm-4 col-lg-3"><label for="du" class="form-label">Du</label><input id="du" type="date" name="du" value="${du}" class="form-control" required></div>
  <div class="col-sm-4 col-lg-3"><label for="au" class="form-label">Au (31 jours maximum)</label><input id="au" type="date" name="au" value="${au}" class="form-control" required></div>
  <div class="col-sm-4"><button class="btn btn-primary">Afficher les pointages</button></div>
 </form>
 <p class="small text-muted">Durées en HH:MM:SS. Le temps connecté est cumulé sans doublons, hors pause prévue. Les heures supplémentaires correspondent au dépassement de la durée journalière prévue. Les valeurs restent provisoires pendant une session ouverte.</p>
 <c:if test="${empty bilans}"><p>Aucun pointage ou employé actif pour cette période.</p></c:if>
 <c:forEach var="b" items="${bilans}"><article class="card mb-4">
  <div class="card-header d-flex flex-wrap justify-content-between gap-2"><strong>${b.date} · Employé ${b.employe.id} · <c:out value="${b.email}"/></strong><c:if test="${b.enCours}"><span class="badge bg-info text-dark">En cours · provisoire</span></c:if><c:if test="${not empty b.transmission}"><span class="badge bg-success">Transmis à Finance</span></c:if></div>
  <div class="card-body">
   <p><c:choose><c:when test="${empty b.planning}">Planning non défini</c:when><c:when test="${not b.planning.jourTravaille}">Jour de repos · <c:out value="${b.planning.poste.intitule}"/></c:when><c:otherwise><c:out value="${b.planning.poste.intitule}"/> · ${b.planning.heureDebut} → ${b.planning.heureFin}<c:if test="${not empty b.planning.pauseDebut}"> · Pause ${b.planning.pauseDebut} → ${b.planning.pauseFin}</c:if></c:otherwise></c:choose></p>
   <div class="table-responsive"><table class="table table-sm"><thead><tr><th>Première connexion</th><th>Dernière déconnexion</th><th>Prévu</th><th>Travaillé</th><th>Retard</th><th>Heures sup.</th><th>Temps prévu non travaillé</th></tr></thead><tbody><tr><td><c:out value="${b.premiereEntree}" default="—"/></td><td><c:out value="${b.derniereSortie}" default="—"/></td><td>${b.prevuFormate}</td><td><strong>${b.travailFormate}</strong></td><td>${b.retardFormate}</td><td>${b.supplementairesFormate}</td><td>${b.absenceFormate}</td></tr></tbody></table></div>
   <c:forEach var="a" items="${b.anomalies}"><div class="alert ${a.bloquante ? 'alert-danger' : 'alert-warning'} py-2 mb-2" role="status"><c:out value="${a.message}"/></div></c:forEach>
   <c:if test="${b.transmissible}"><form method="post" action="${pageContext.request.contextPath}/rh/pointages/transmettre" class="my-3"><input type="hidden" name="pointageCsrf" value="${pointageCsrf}"><input type="hidden" name="employeId" value="${b.employe.id}"><input type="hidden" name="date" value="${b.date}"><button class="btn btn-success">Valider cette journée et transmettre à Finance</button></form></c:if>
   <c:if test="${empty b.transmission and not b.transmissible}"><p class="small text-muted">Transmission disponible pour une journée passée, terminée, avec planning et sans anomalie bloquante.</p></c:if>
   <details><summary>Connexions, déconnexions et régularisations</summary>
    <c:if test="${empty b.sessions}"><p class="mt-2">Aucune connexion enregistrée.</p></c:if>
    <c:forEach var="p" items="${b.sessions}"><div class="border rounded p-3 mt-3">
     <p class="mb-1"><strong>Pointage ${p.id}</strong> · Connexion d'origine : ${p.connexion} · Déconnexion d'origine : <c:out value="${p.deconnexion}" default="Manquante"/><c:if test="${p.sessionExpiree}"> · Session expirée</c:if></p>
     <c:if test="${p.regularise}"><p class="small">Horaires retenus après régularisation : ${p.entreeEffective} → ${p.sortieEffective}</p></c:if>
     <c:if test="${empty b.transmission}"><form method="post" action="${pageContext.request.contextPath}/rh/pointages/${p.id}/corriger" class="row g-2 mt-1">
      <input type="hidden" name="pointageCsrf" value="${pointageCsrf}"><input type="hidden" name="revision" value="${p.revision}">
      <div class="col-md-4"><label for="entree-${p.id}" class="form-label">Entrée retenue</label><input id="entree-${p.id}" name="entree" type="datetime-local" step="1" class="form-control" value="${p.entreeEffective}" required></div>
      <div class="col-md-4"><label for="sortie-${p.id}" class="form-label">Sortie retenue</label><input id="sortie-${p.id}" name="sortie" type="datetime-local" step="1" class="form-control" value="${p.sortieEffective}" required></div>
      <div class="col-md-4"><label for="motif-${p.id}" class="form-label">Motif de régularisation</label><input id="motif-${p.id}" name="motif" maxlength="1000" class="form-control" required></div>
      <div class="col-12"><button class="btn btn-outline-primary">Régulariser ce pointage</button></div>
     </form></c:if>
    </div></c:forEach>
    <c:forEach var="c" items="${b.corrections}"><p class="small mt-2 mb-0">Correction du ${c.corrigeLe} par <c:out value="${c.rh.email}"/> : <c:out value="${c.motif}"/> · ${c.ancienneEntree} → <c:out value="${c.ancienneSortie}" default="Manquante"/> remplacé par ${c.nouvelleEntree} → ${c.nouvelleSortie}.</p></c:forEach>
   </details>
  </div>
 </article></c:forEach>
</main></body></html>
