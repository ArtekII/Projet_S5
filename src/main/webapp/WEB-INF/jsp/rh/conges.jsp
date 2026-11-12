<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Congés · Gestion RH</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <link href="${pageContext.request.contextPath}/assets/conditions.css" rel="stylesheet">
<jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head><body class="bg-light app-shell">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<main id="contenu" class="container py-4" tabindex="-1">
 <header class="mb-4">
  <h1 class="h3 mb-1">Demandes de congés</h1>
  <p class="text-muted mb-0">Validez ou refusez les demandes. Le solde n'est décompté qu'à la validation, après contrôle du solde disponible.</p>
 </header>

 <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>

 <section class="card shadow-sm mb-4" aria-labelledby="demandes-rh">
  <div class="card-header d-flex flex-wrap justify-content-between align-items-center gap-2">
   <h2 id="demandes-rh" class="h5 mb-0">Demandes</h2>
   <span class="badge bg-light text-dark">Les demandes en attente apparaissent en premier</span>
  </div>
  <div class="card-body">
   <c:choose>
    <c:when test="${empty demandes}">
     <p class="mb-0">Aucune demande de congé enregistrée.</p>
    </c:when>
    <c:otherwise>
     <div class="table-responsive">
      <table class="table table-sm align-middle mb-0">
       <thead><tr><th>Employé</th><th>Type</th><th>Du</th><th>Au</th><th class="text-end">Jours</th><th class="text-end">Solde</th><th>Statut</th><th>Décision</th></tr></thead>
       <tbody>
        <c:forEach var="d" items="${demandes}">
         <tr>
          <td><c:out value="${d.email}"/><div class="small text-muted">Employé n° <c:out value="${d.employeId}"/></div></td>
          <td><strong><c:out value="${d.typeCode}"/></strong><div class="small text-muted"><c:out value="${d.typeLibelle}"/></div></td>
          <td><c:out value="${d.dateDebut}"/></td>
          <td><c:out value="${d.dateFin}"/></td>
          <td class="text-end"><c:out value="${d.nbJours}"/></td>
          <td class="text-end">
           <c:choose>
            <c:when test="${empty d.soldeDisponible}"><span class="text-muted">—</span></c:when>
            <c:otherwise><c:out value="${d.soldeDisponible}"/></c:otherwise>
           </c:choose>
          </td>
          <td>
           <c:choose>
            <c:when test="${d.statut == 'EN_ATTENTE'}"><span class="badge bg-warning text-dark">En attente</span></c:when>
            <c:when test="${d.statut == 'VALIDE'}"><span class="badge bg-success">Validé</span></c:when>
            <c:when test="${d.statut == 'REFUSE'}"><span class="badge bg-danger">Refusé</span></c:when>
            <c:otherwise><span class="badge bg-secondary">Annulé</span></c:otherwise>
           </c:choose>
           <c:if test="${not empty d.commentaire}"><div class="small text-muted"><c:out value="${d.commentaire}"/></div></c:if>
          </td>
          <td style="min-width: 18rem">
           <c:choose>
            <c:when test="${d.statut == 'EN_ATTENTE'}">
             <form method="post" action="${pageContext.request.contextPath}/rh/conges/${d.id}/decision" class="row g-1">
              <input type="hidden" name="dossierCsrf" value="${dossierCsrf}">
              <div class="col-12"><input name="commentaire" class="form-control form-control-sm" maxlength="500" placeholder="Motif de la décision (facultatif)"></div>
              <div class="col-6"><button class="btn btn-success btn-sm w-100" name="action" value="VALIDER" type="submit">Valider</button></div>
              <div class="col-6"><button class="btn btn-outline-danger btn-sm w-100" name="action" value="REFUSER" type="submit">Refuser</button></div>
             </form>
            </c:when>
            <c:otherwise>
             <span class="small text-muted"><c:out value="${d.dateDecision}"/></span>
            </c:otherwise>
           </c:choose>
          </td>
         </tr>
        </c:forEach>
       </tbody>
      </table>
     </div>
    </c:otherwise>
   </c:choose>
  </div>
 </section>

 <section class="card shadow-sm" aria-labelledby="soldes-rh">
  <div class="card-header"><h2 id="soldes-rh" class="h5 mb-0">Soldes des employés</h2></div>
  <div class="card-body">
   <c:choose>
    <c:when test="${empty soldes}">
     <p class="mb-0">Aucun solde calculé. Les soldes apparaissent dès qu'un employé a une date d'embauche et un type de congé avec acquisition mensuelle.</p>
    </c:when>
    <c:otherwise>
     <div class="table-responsive">
      <table class="table table-sm align-middle mb-0">
       <thead><tr><th>Employé</th><th>Type</th><th>Période</th><th class="text-end">Acquis</th><th class="text-end">Pris</th><th class="text-end">En attente</th><th class="text-end">Disponible après attente</th></tr></thead>
       <tbody>
        <c:forEach var="s" items="${soldes}">
         <tr>
          <td><c:out value="${s.email}"/><div class="small text-muted">Employé n° <c:out value="${s.employeId}"/></div></td>
          <td><strong><c:out value="${s.typeCode}"/></strong><div class="small text-muted"><c:out value="${s.typeLibelle}"/></div></td>
          <td><c:out value="${s.periodeDebut}"/> → <c:out value="${s.periodeFin}"/></td>
          <td class="text-end"><c:out value="${s.joursAcquis}"/></td>
          <td class="text-end"><c:out value="${s.joursPris}"/></td>
          <td class="text-end"><c:out value="${s.joursEnAttente}"/></td>
          <td class="text-end"><strong><c:out value="${s.soldeReserve}"/></strong></td>
         </tr>
        </c:forEach>
       </tbody>
      </table>
     </div>
    </c:otherwise>
   </c:choose>
  </div>
 </section>
</main>
</body></html>
