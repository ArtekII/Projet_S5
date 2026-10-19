<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><title>Mes demandes</title></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <h5>Mes demandes de postes</h5>
 <table class="table table-sm">
  <thead><tr><th>Intitulé</th><th>Décision</th><th>Date</th></tr></thead>
  <tbody>
   <c:forEach var="d" items="${demandes}">
    <tr><td>${d.intitulePropose}</td><td>${d.decision.libelle}</td><td>${f:dateTime(d.createdAt)}</td></tr>
   </c:forEach>
  </tbody>
 </table>
 <a href="${pageContext.request.contextPath}/manager/dashboard" class="btn btn-sm btn-secondary">Retour</a>
</div>
</body></html>
