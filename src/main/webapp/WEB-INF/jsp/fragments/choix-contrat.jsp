<%@ page pageEncoding="UTF-8" %>
<c:choose>
 <c:when test="${d.cdi_autorise}">
  <label class="form-label d-block mb-3">Votre type de contrat
   <select name="typeContrat" class="form-select" required>
    <option value="">Choisissez votre contrat</option>
    <option value="CE">Contrat d’essai (CE)</option>
    <option value="CDI">Contrat à durée indéterminée (CDI)</option>
   </select>
  </label>
 </c:when>
 <c:otherwise><input type="hidden" name="typeContrat" value="CE"></c:otherwise>
</c:choose>
