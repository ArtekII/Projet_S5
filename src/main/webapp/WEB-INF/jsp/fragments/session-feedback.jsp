<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not empty pointageSessionVue}">
 <dialog class="app-confirm" data-pointage-confirm aria-labelledby="pointage-fin-title" aria-describedby="pointage-fin-description">
  <h2 id="pointage-fin-title">Terminer votre pointage ?</h2>
  <p id="pointage-fin-description">Votre pointage est en cours depuis ${pointageSessionVue}. Vous déconnecter enregistrera votre sortie.</p>
  <div class="d-flex gap-2 flex-wrap"><button type="button" class="btn btn-secondary" data-cancel-logout autofocus>Rester connecté</button><a class="btn btn-primary" href="${pageContext.request.contextPath}/logout" data-confirm-logout>Terminer et se déconnecter</a></div>
 </dialog>
</c:if>
