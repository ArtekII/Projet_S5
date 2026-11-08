<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
 <title>Espace employé – JobListe</title>
 <link rel="preconnect" href="https://fonts.googleapis.com">
 <link href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:wght@600;800&family=Source+Sans+3:wght@400;600&display=swap" rel="stylesheet">
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <link href="${pageContext.request.contextPath}/assets/espace-employe.css" rel="stylesheet">
</head><body>
<a class="employe-skip" href="#contenu">Aller au contenu</a>
<div class="employe-app">
 <aside class="employe-side">
  <a class="employe-logo" href="${pageContext.request.contextPath}/espaceEmploye"><span>Job</span>Liste</a>
  <div class="employe-caption">Espace employé</div>
  <nav class="employe-nav" aria-label="Navigation de l'espace employé">
   <a href="${pageContext.request.contextPath}/espaceEmploye?onglet=pointages" aria-current="${ongletEmploye == 'pointages' ? 'page' : 'false'}">Mon planning et mes pointages</a>
   <a href="${pageContext.request.contextPath}/espaceEmploye?onglet=renseignements" aria-current="${ongletEmploye == 'renseignements' ? 'page' : 'false'}">Mes renseignements</a>
   <a href="${pageContext.request.contextPath}/espaceEmploye?onglet=documents" aria-current="${ongletEmploye == 'documents' ? 'page' : 'false'}">Mes documents <small>${fn:length(documents)}</small></a>
   <a href="${pageContext.request.contextPath}/espaceEmploye?onglet=cv" aria-current="${ongletEmploye == 'cv' ? 'page' : 'false'}">Mes CV <small>${fn:length(cvs) + fn:length(cvsCandidats)}</small></a>
   <a href="${pageContext.request.contextPath}/espaceEmploye?onglet=contrats" aria-current="${ongletEmploye == 'contrats' ? 'page' : 'false'}">Mes contrats <small>${fn:length(contrats)}</small></a>
   <a href="${pageContext.request.contextPath}/espaceEmploye?onglet=parcours" aria-current="${ongletEmploye == 'parcours' ? 'page' : 'false'}">Mon parcours professionnel</a>
   <c:if test="${not empty employe}"><a href="${pageContext.request.contextPath}/espaceEmploye?onglet=integration" aria-current="${ongletEmploye == 'integration' ? 'page' : 'false'}">Mon intégration</a></c:if>
  </nav>
  <nav class="employe-retour" aria-label="Autres espaces">
   <c:choose>
    <c:when test="${sessionScope.userRole == 'CANDIDAT'}"><a href="${pageContext.request.contextPath}/espaceCandidat">← Espace candidat</a></c:when>
    <c:when test="${sessionScope.userRole == 'RH'}"><a href="${pageContext.request.contextPath}/rh/dashboard">← Espace RH</a></c:when>
    <c:when test="${sessionScope.userRole == 'MANAGER'}"><a href="${pageContext.request.contextPath}/manager/dashboard">← Espace manager</a></c:when>
    <c:when test="${sessionScope.userRole == 'FINANCE'}"><a href="${pageContext.request.contextPath}/finance/dashboard">← Espace finance</a></c:when>
    <c:when test="${sessionScope.userRole == 'DIRECTION'}"><a href="${pageContext.request.contextPath}/direction/dashboard">← Espace direction</a></c:when>
   </c:choose>
  </nav>
  <div class="employe-me">
   <div class="employe-avatar" aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(titulaire.email, 0, 1))}"/></div>
   <div><b><c:out value="${titulaire.email}"/></b><a href="${pageContext.request.contextPath}/logout">Se déconnecter</a></div>
  </div>
 </aside>
 <main id="contenu" class="employe-main" tabindex="-1">
  <header class="employe-head">
   <h1>Espace employé</h1><p>Retrouvez vos renseignements, vos documents et votre parcours professionnel.</p>
   <c:if test="${not empty employe}"><div class="employe-identite">Employé n° ${employe.id} · Embauche : ${employe.dateEmbauche}<c:if test="${not empty titulaire.departement}"> · <c:out value="${titulaire.departement.nom}"/></c:if></div></c:if>
  </header>
  <c:if test="${not empty success}"><div class="alert alert-success" role="status"><c:out value="${success}"/></div></c:if>
  <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
  <c:set var="documentsASuivre" value="0"/>
  <c:forEach var="piece" items="${documents}"><c:if test="${piece.validite == 'EXPIRE' or piece.validite == 'A_RENOUVELER'}"><c:set var="documentsASuivre" value="${documentsASuivre + 1}"/></c:if></c:forEach>
  <div class="employe-apercu" aria-label="Aperçu du dossier">
   <a href="${pageContext.request.contextPath}/espaceEmploye?onglet=renseignements"><span>Fiche de renseignements</span><strong>${empty fiche.id ? 'À compléter' : 'Enregistrée'}</strong></a>
   <a href="${pageContext.request.contextPath}/espaceEmploye?onglet=documents"><span>Documents à suivre</span><strong>${documentsASuivre}</strong><small>Expirés ou à renouveler sous 30 jours</small></a>
   <a href="${pageContext.request.contextPath}/espaceEmploye?onglet=contrats"><span>Contrats conservés</span><strong>${fn:length(contrats)}</strong></a>
  </div>
  <%@ include file="../fragments/dossier-contenu.jspf" %>
  <c:if test="${ongletEmploye == 'pointages'}"><%@ include file="../fragments/pointage-employe.jspf" %></c:if>
  <c:if test="${ongletEmploye == 'integration' and empty employe}"><div class="employe-empty">Votre suivi d'intégration sera disponible après votre embauche.</div></c:if>
 </main>
</div>
</body></html>
