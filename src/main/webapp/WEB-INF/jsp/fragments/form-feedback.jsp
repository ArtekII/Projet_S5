<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not empty saisieFormulaire}">
 <div hidden id="app-form-restore" data-action="<c:out value='${actionFormulaire}'/>">
  <c:forEach var="champ" items="${saisieFormulaire}"><c:forEach var="valeur" items="${champ.value}"><input type="hidden" data-restore-name="<c:out value='${champ.key}'/>" value="<c:out value='${valeur}'/>"></c:forEach></c:forEach>
 </div>
</c:if>
