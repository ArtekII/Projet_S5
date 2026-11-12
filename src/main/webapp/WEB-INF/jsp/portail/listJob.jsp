<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Offres d'emploi · JobListe</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head><body class="app-public">
<a class="app-skip" href="#contenu">Aller au contenu</a>
<div class="app-public-wrap">
 <header class="app-public-header"><a class="app-brand" href="${pageContext.request.contextPath}/jobOffre"><span>Job</span>Liste</a><a href="${pageContext.request.contextPath}/login">${empty sessionScope.userId ? 'Se connecter' : 'Mon espace'}</a></header>
 <section class="app-public-hero"><span class="app-eyebrow">Votre prochaine étape</span><h1>Trouvez votre prochain poste.</h1><p>Explorez les opportunités et retrouvez une offre qui correspond à votre expérience.</p></section>
 <main id="contenu" tabindex="-1" data-collection="offres publiques" data-state-label="Département" data-search-placeholder="Intitulé, département ou compétence">
  <c:if test="${empty offres}"><div class="app-empty"><strong>Aucune offre disponible pour le moment</strong>Revenez prochainement pour découvrir les nouvelles opportunités.</div></c:if>
  <div class="app-public-jobs">
   <c:forEach var="offre" items="${offres}">
    <article class="app-job-card" data-item data-state="<c:out value='${offre.departement.nom}'/>" data-sort="<c:out value='${offre.poste.intitule}'/>">
     <span class="app-eyebrow"><c:out value="${offre.departement.nom}"/></span>
     <h2><c:out value="${offre.poste.intitule}"/></h2>
     <p><c:choose><c:when test="${empty offre.salaire}">Salaire non précisé</c:when><c:otherwise>${offre.salaire} Ar</c:otherwise></c:choose></p>
     <div class="app-job-facts"><span>${offre.nbPlaces} place(s)</span><span>${offre.anneesExperience} an(s) d'expérience</span></div>
     <details><summary>Description du poste</summary><p class="mt-2"><c:out value="${offre.poste.description}"/></p></details>
     <a class="btn" href="${pageContext.request.contextPath}/espaceCandidat?offre=${offre.id}">Postuler à cette offre</a>
    </article>
   </c:forEach>
  </div>
 </main>
</div></body></html>
