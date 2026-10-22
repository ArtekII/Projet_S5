<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<dl class="row mb-0">
 <dt class="col-sm-5">Salaire</dt><dd class="col-sm-7"><c:out value="${affichee.salaire}"/> Ar</dd>
 <dt class="col-sm-5">Durée hebdomadaire</dt><dd class="col-sm-7"><c:out value="${affichee.heuresParSemaine}"/> h</dd>
 <dt class="col-sm-5">Horaires</dt><dd class="col-sm-7"><c:out value="${affichee.horaires}" default="Non précisés"/></dd>
 <dt class="col-sm-5">Lieu de travail</dt><dd class="col-sm-7"><c:out value="${affichee.lieuTravail}"/></dd>
 <c:if test="${not empty affichee.remarque}"><dt class="col-sm-5">Remarque</dt><dd class="col-sm-7 text-break"><c:out value="${affichee.remarque}"/></dd></c:if>
</dl>
