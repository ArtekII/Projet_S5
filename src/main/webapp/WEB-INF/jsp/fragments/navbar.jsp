<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<a class="app-skip" href="#contenu">Aller au contenu</a>
<aside class="app-sidebar">
 <a class="app-brand" href="${pageContext.request.contextPath}/"><c:choose><c:when test="${sessionScope.userRole == 'RH'}"><span>Gestion</span> RH</c:when><c:otherwise><span>Job</span>Liste</c:otherwise></c:choose></a>
 <p class="app-sidebar-caption">${sessionScope.userRole == 'RH' ? 'Ressources humaines' : 'Recrutement et vie professionnelle'}</p>
 <details class="app-navigation" open>
  <summary>Menu principal</summary>
  <nav class="app-nav" aria-label="Navigation principale">
   <c:choose>
    <c:when test="${sessionScope.userRole == 'RH'}">
     <span class="app-nav-caption">Recrutement</span>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/dashboard">Vue d'ensemble</a>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/dashboard?vue=demandes">Demandes de postes</a>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/dashboard?vue=offres">Offres à valider</a>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/dashboard?vue=candidatures">Candidatures</a>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/dashboard?vue=questionnaires">Questionnaires</a>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/dashboard?vue=historique">Toutes les offres</a>
     <span class="app-nav-caption">Administration</span>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/conditions">Conditions de travail</a>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/contrats">Contrats</a>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/conges">Congés</a>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/dossiers">Dossiers administratifs</a>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/accreditations">Accréditations</a>
     <span class="app-nav-caption">Temps de travail</span>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/plannings">Plannings</a>
     <a data-nav-link href="${pageContext.request.contextPath}/rh/pointages">Pointages et anomalies</a>
    </c:when>
    <c:when test="${sessionScope.userRole == 'MANAGER'}">
     <span class="app-nav-caption">Mon équipe</span>
     <a data-nav-link href="${pageContext.request.contextPath}/manager/dashboard">Mes recrutements</a>
     <a data-nav-link href="${pageContext.request.contextPath}/manager/demandes">Mes demandes de postes</a>
    </c:when>
    <c:when test="${sessionScope.userRole == 'FINANCE'}">
     <span class="app-nav-caption">Finance</span>
     <a data-nav-link href="${pageContext.request.contextPath}/finance/dashboard">Salaires à valider</a>
     <a data-nav-link href="${pageContext.request.contextPath}/finance/pointages">Variables de pointage</a>
    </c:when>
    <c:when test="${sessionScope.userRole == 'DIRECTION'}">
     <span class="app-nav-caption">Direction</span>
     <a data-nav-link href="${pageContext.request.contextPath}/direction/dashboard">Publications à valider</a>
    </c:when>
    <c:when test="${sessionScope.userRole == 'CANDIDAT'}">
     <span class="app-nav-caption">Ma candidature</span>
     <a data-nav-link href="${pageContext.request.contextPath}/espaceCandidat?onglet=offres">Offres disponibles</a>
     <a data-nav-link href="${pageContext.request.contextPath}/espaceCandidat?onglet=cand">Mes candidatures</a>
     <a data-nav-link href="${pageContext.request.contextPath}/espaceCandidat?onglet=cv">Déposer mon CV</a>
     <a data-nav-link href="${pageContext.request.contextPath}/candidat/conditions">Conditions et contrats</a>
    </c:when>
   </c:choose>
   <c:if test="${requestScope.accesEspaceEmploye}"><span class="app-nav-caption">Mon dossier</span><a href="${pageContext.request.contextPath}/espaceEmploye">Espace employé</a></c:if>
  </nav>
 </details>
 <div class="app-account">
  <strong><c:out value="${sessionScope.userEmail}"/></strong>
  <small><c:out value="${empty sessionScope.userRoleLibelle ? sessionScope.userRole : sessionScope.userRoleLibelle}"/></small>
  <a href="${pageContext.request.contextPath}/logout" data-logout>Se déconnecter</a>
 </div>
</aside>
<header class="app-topbar">
 <span class="app-topbar-title">Espace <c:out value="${empty sessionScope.userRoleLibelle ? sessionScope.userRole : sessionScope.userRoleLibelle}"/></span>
 <div class="app-topbar-tools">
  <c:if test="${not empty pointageSessionVue}"><a class="app-pointage-link" href="${pageContext.request.contextPath}/espaceEmploye?onglet=pointages">Pointage en cours depuis ${pointageSessionVue}</a></c:if>
  <details class="app-space-switch">
   <summary>Mon espace</summary>
   <div class="app-space-links">
    <c:if test="${requestScope.accesEspaceEmploye}"><a href="${pageContext.request.contextPath}/espaceEmploye">Espace employé</a></c:if>
    <c:if test="${sessionScope.userRole == 'CANDIDAT'}"><a href="${pageContext.request.contextPath}/espaceCandidat">Espace candidat</a></c:if>
    <a href="${pageContext.request.contextPath}/logout" data-logout>Se déconnecter</a>
   </div>
  </details>
 </div>
</header>
<jsp:include page="/WEB-INF/jsp/fragments/session-feedback.jsp"/>
<jsp:include page="/WEB-INF/jsp/fragments/form-feedback.jsp"/>
