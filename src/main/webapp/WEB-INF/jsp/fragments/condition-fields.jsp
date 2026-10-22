<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div class="row g-3">
 <div class="col-sm-6">
  <label class="form-label" for="${formPrefix}-salaire">Salaire (Ar)</label>
  <input id="${formPrefix}-salaire" name="salaire" type="number" min="0" max="9999999999.99" step="0.01" required class="form-control" value="<c:out value='${valeurs.salaire}'/>">
 </div>
 <div class="col-sm-6">
  <label class="form-label" for="${formPrefix}-heures">Heures par semaine</label>
  <input id="${formPrefix}-heures" name="heuresParSemaine" type="number" min="0.1" max="999.9" step="0.1" required class="form-control" value="<c:out value='${valeurs.heuresParSemaine}'/>">
 </div>
 <div class="col-sm-6">
  <label class="form-label" for="${formPrefix}-horaires">Horaires</label>
  <input id="${formPrefix}-horaires" name="horaires" maxlength="100" class="form-control" placeholder="Ex. : lundi au vendredi, 8 h à 17 h" value="<c:out value='${valeurs.horaires}'/>">
 </div>
 <div class="col-sm-6">
  <label class="form-label" for="${formPrefix}-lieu">Lieu de travail</label>
  <input id="${formPrefix}-lieu" name="lieuTravail" maxlength="255" required class="form-control" value="<c:out value='${valeurs.lieuTravail}'/>">
 </div>
 <div class="col-12">
  <label class="form-label" for="${formPrefix}-remarque">Remarque <span class="text-muted fw-normal">(facultatif)</span></label>
  <textarea id="${formPrefix}-remarque" name="remarque" rows="3" class="form-control" placeholder="Précisez les raisons ou les détails de votre proposition."><c:out value="${valeurs.remarque}"/></textarea>
 </div>
</div>
