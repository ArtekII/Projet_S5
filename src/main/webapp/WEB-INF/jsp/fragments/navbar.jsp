<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
 <div class="container">
  <a class="navbar-brand" href="${pageContext.request.contextPath}/">Recrutement</a>
  <div class="ms-auto d-flex align-items-center gap-3">
   <c:if test="${sessionScope.userRole == 'RH'}"><a class="text-light small" href="${pageContext.request.contextPath}/rh/conditions">Conditions de travail</a></c:if>
   <c:if test="${sessionScope.userRole == 'RH'}"><a class="text-light small" href="${pageContext.request.contextPath}/rh/contrats">Contrats</a></c:if>
   <c:if test="${sessionScope.userRole == 'CANDIDAT'}"><a class="text-light small" href="${pageContext.request.contextPath}/candidat/conditions">Mes conditions</a></c:if>
   <c:if test="${sessionScope.userRole == 'RH'}"><a class="text-light small" href="${pageContext.request.contextPath}/rh/accreditations">Accr&eacute;ditations</a></c:if>
   <span class="text-light small">${sessionScope.userEmail}</span>
   <span class="badge bg-light text-dark">${sessionScope.userRole}</span>
   <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-light btn-sm">Déconnexion</a>
  </div>
 </div>
</nav>
