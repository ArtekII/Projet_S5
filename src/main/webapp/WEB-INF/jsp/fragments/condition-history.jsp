<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<c:if test="${not empty d.historique}">
 <details class="mt-3 border-top pt-3">
  <summary>Historique des propositions (${d.historique.size()})</summary>
  <c:forEach var="h" items="${d.historique}">
   <div class="border rounded p-3 mt-3">
    <p class="small text-muted">Version ${h.version} · <c:out value="${h.roleProposant.libelle}"/> · ${f:dateTime(h.createdAt)}
     <c:if test="${h.roleProposant.code == 'CANDIDAT'}"> · <c:out value="${h.decisionRh.libelle}"/></c:if>
    </p>
    <c:set var="affichee" value="${h}"/>
    <%@ include file="condition-summary.jsp" %>
   </div>
  </c:forEach>
 </details>
</c:if>
