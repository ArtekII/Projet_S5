<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Connexion - Recrutement</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<style>.login-card{max-width:460px;margin:60px auto}.role-badge{font-size:.75rem}</style>
</head>
<body class="bg-light">
<div class="container">
 <div class="card shadow login-card">
  <div class="card-body p-4">
   <h4 class="text-center mb-1">Portail Recrutement</h4>
   <p class="text-center text-muted small">Chaque rôle se connecte ici</p>
   <c:if test="${not empty error}">
    <div class="alert alert-danger py-2 small">${error}</div>
   </c:if>
   <form method="post" action="${pageContext.request.contextPath}/login">
    <div class="mb-3"><label class="form-label">Email</label><input name="email" type="email" class="form-control" required placeholder="ex: manager@demo.com"></div>
    <div class="mb-3"><label class="form-label">Mot de passe</label><input name="password" type="password" class="form-control" required value="password"></div>
    <button class="btn btn-primary w-100">Se connecter</button>
   </form>
   <hr>
   <p class="small text-muted mb-1">Comptes de test (mot de passe = <code>password</code>) :</p>
   <div class="d-flex flex-wrap gap-1">
    <span class="badge bg-secondary">manager@demo.com (MANAGER)</span>
    <span class="badge bg-info">rh@demo.com (RH)</span>
    <span class="badge bg-warning text-dark">finance@demo.com (FINANCE)</span>
    <span class="badge bg-dark">direction@demo.com (DIRECTION)</span>
    <span class="badge bg-success">candidat@demo.com (CANDIDAT)</span>
   </div>
   <p class="small text-muted mt-3 mb-0">Workflow : Manager &rarr; RH (salaire) &rarr; Finance (valide/modifie salaire) &rarr; Direction (publication) &rarr; Candidat voit l'offre.</p>
  </div>
 </div>
</div>
</body>
</html>
