<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="fr"><head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <title>Connexion · JobListe</title>
 <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
 <jsp:include page="/WEB-INF/jsp/fragments/app-head.jsp"/>
</head><body class="app-login">
<main class="app-login-wrap" id="contenu">
 <a class="app-brand" href="${pageContext.request.contextPath}/jobOffre"><span>Job</span>Liste</a>
 <div class="app-login-card">
  <span class="app-eyebrow">Votre espace professionnel</span>
  <h1>Bienvenue</h1><p>Connectez-vous pour retrouver vos démarches et vos actions à suivre.</p>
  <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
  <form method="post" action="${pageContext.request.contextPath}/login">
   <c:if test="${not empty offreSouhaitee}"><input type="hidden" name="offre" value="${offreSouhaitee}"><p class="small">Après connexion, vous pourrez postuler à l'offre sélectionnée.</p></c:if>
   <div class="mb-3"><label class="form-label" for="email">Adresse e-mail</label><input id="email" name="email" type="email" class="form-control" autocomplete="username" value="<c:out value='${email}'/>" required placeholder="vous@entreprise.com"></div>
   <div class="mb-4"><label class="form-label" for="password">Mot de passe</label><input id="password" name="password" type="password" class="form-control" autocomplete="current-password" required></div>
   <button class="btn btn-primary w-100" type="submit">Se connecter</button>
  </form>
  <p class="small mt-4 mb-0">Vous cherchez un poste ? <a href="${pageContext.request.contextPath}/jobOffre">Consulter les offres</a></p>
 </div>
 <details class="app-login-demo"><summary>Comptes de démonstration</summary>
  <p class="mt-2 mb-1">Mot de passe commun : <code>password</code></p>
  <ul><li>manager@demo.com — Manager</li><li>rh@demo.com — Ressources humaines</li><li>finance@demo.com — Finance</li><li>direction@demo.com — Direction</li><li>candidat@demo.com — Candidat</li></ul>
 </details>
</main></body></html>
