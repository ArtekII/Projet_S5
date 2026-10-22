<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>Espace candidat – JobListe</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:wght@600;800&family=Source+Sans+3:wght@400;600&display=swap" rel="stylesheet">
<style>
:root{--bg:#f2f6f7;--surface:#fff;--ink:#12242d;--muted:#5a6d76;--line:#d5e0e4;--accent:#0b7a75;--accent-ink:#fff;--hl:#f4b63f;--hl-ink:#3a2a00;--bad:#b3261e;
color-scheme:light;box-sizing:border-box;padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px)}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#0f1b21;--surface:#16262e;--ink:#e8f1f3;--muted:#9bb0b8;--line:#2a3f49;--accent:#3cc4bb;--accent-ink:#06211f;--bad:#ff8a7d;color-scheme:dark}}
:root[data-theme="dark"]{--bg:#0f1b21;--surface:#16262e;--ink:#e8f1f3;--muted:#9bb0b8;--line:#2a3f49;--accent:#3cc4bb;--accent-ink:#06211f;--bad:#ff8a7d;color-scheme:dark}
html{scroll-padding-top:env(safe-area-inset-top,0px)}
*,*::before,*::after{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--ink);font:400 17px/1.55 "Source Sans 3",system-ui,sans-serif}
h1,h2,h3{font-family:"Bricolage Grotesque","Source Sans 3",system-ui,sans-serif;margin:0;line-height:1.15}
.app{display:grid;grid-template-columns:260px 1fr;min-height:100vh}
.rd{position:absolute;opacity:0;pointer-events:none}
/* Sidebar */
.side{background:var(--surface);border-right:1px solid var(--line);padding:22px 16px;display:flex;flex-direction:column;gap:24px;position:sticky;top:0;align-self:start;height:100vh}
.logo{font:800 1.25rem "Bricolage Grotesque",sans-serif;padding:0 8px}
.logo span{background:var(--hl);color:var(--hl-ink);padding:0 6px;border-radius:4px;margin-right:4px}
.nav{display:flex;flex-direction:column;gap:4px}
.nav label{display:flex;justify-content:space-between;align-items:center;gap:8px;padding:10px 12px;border-radius:10px;font-weight:600;cursor:pointer;border:1px solid transparent}
.nav label:hover{border-color:var(--line)}
.nav small{background:var(--bg);border:1px solid var(--line);border-radius:99px;padding:0 9px;font-size:.8rem;color:var(--muted)}
#v-offres:checked~.side label[for="v-offres"],#v-cand:checked~.side label[for="v-cand"],#v-etapes:checked~.side label[for="v-etapes"],#v-cv:checked~.side label[for="v-cv"]{background:var(--accent);color:var(--accent-ink)}
#v-offres:checked~.side label[for="v-offres"] small,#v-cand:checked~.side label[for="v-cand"] small{background:transparent;border-color:currentColor;color:inherit}
#v-offres:focus-visible~.side label[for="v-offres"],#v-cand:focus-visible~.side label[for="v-cand"],#v-etapes:focus-visible~.side label[for="v-etapes"],#v-cv:focus-visible~.side label[for="v-cv"]{outline:3px solid var(--hl);outline-offset:2px}
.me{margin-top:auto;display:flex;align-items:center;gap:10px;border-top:1px solid var(--line);padding:14px 8px 0}
.av{width:38px;height:38px;border-radius:50%;background:var(--hl);color:var(--hl-ink);display:grid;place-items:center;font-weight:800;flex:none}
.me div{min-width:0;font-size:.9rem;line-height:1.3}
.me b{display:block;overflow:hidden;text-overflow:ellipsis}
.me a{color:var(--accent);font-weight:600}
/* Contenu */
.main{padding:32px clamp(18px,4vw,44px) 60px;min-width:0}
.panel{display:none}
#v-offres:checked~.main .p-offres,#v-cand:checked~.main .p-cand,#v-etapes:checked~.main .p-etapes,#v-cv:checked~.main .p-cv{display:block}
.head{margin-bottom:24px}
.head h1{font-size:clamp(1.8rem,4vw,2.6rem);font-weight:800;letter-spacing:-.02em}
.head p{color:var(--muted);margin:8px 0 0;max-width:60ch}
.meta{color:var(--muted);font-size:.95rem}
.tag,.st{display:inline-block;font-weight:600;font-size:.85rem;padding:2px 10px;border-radius:99px;white-space:nowrap}
.tag{background:var(--hl);color:var(--hl-ink)}
.st{border:1px solid var(--line)}
.st-run{background:var(--hl);color:var(--hl-ink);border-color:transparent}
.st-wait{background:var(--bg);color:var(--ink)}
.st-ko{color:var(--bad);border-color:var(--bad)}
.st-ok{background:var(--accent);color:var(--accent-ink);border-color:transparent}
.go{display:block;margin-top:auto;text-align:center;background:var(--accent);color:var(--accent-ink);text-decoration:none;font-weight:600;padding:10px 20px;border-radius:8px}
.go.off{background:var(--bg);color:var(--muted);border:1px solid var(--line);cursor:default}
/* Offres */
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(250px,1fr));gap:18px}
.card{display:flex;flex-direction:column;gap:10px;background:var(--surface);border:1px solid var(--line);border-radius:14px;padding:18px}
.card:hover{border-color:var(--accent)}
.card h3{font-size:1.15rem;font-weight:600}
.facts{display:flex;flex-wrap:wrap;gap:6px}
.facts span{background:var(--bg);border:1px solid var(--line);border-radius:6px;padding:2px 8px;font-size:.85rem}
summary{cursor:pointer;font-weight:600}
/* Candidatures */
.apps{display:flex;flex-direction:column;gap:16px}
.app-row{background:var(--surface);border:1px solid var(--line);border-radius:14px;padding:18px;display:flex;flex-direction:column;gap:14px}
.top{display:flex;justify-content:space-between;align-items:flex-start;gap:12px;flex-wrap:wrap}
.top h3{font-size:1.15rem;font-weight:600}
.mini{list-style:none;margin:0;padding:0;display:grid;grid-template-columns:repeat(4,1fr);gap:8px}
.mini.auto{grid-template-columns:repeat(auto-fit,minmax(150px,1fr))}
.mini li{border-top:4px solid var(--line);padding-top:6px;font-size:.85rem;color:var(--muted)}
.mini li b{display:block;color:var(--ink);font-size:.95rem}
.mini .done{border-color:var(--accent);color:var(--ink)}
.mini .cur{border-color:var(--hl);color:var(--ink);font-weight:600}
.mini .ko{border-color:var(--bad);color:var(--bad);font-weight:600}
.lnk{display:block;cursor:pointer;color:var(--accent);text-decoration:underline}
.lnk span{display:block;font-size:.8rem;font-weight:400}
.etapes-list{display:flex;flex-direction:column;gap:10px;border-top:1px solid var(--line);padding-top:14px}
.test{display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;background:var(--bg);border:1px solid var(--line);border-radius:10px;padding:12px 14px}
.test b{display:block}
/* Étapes */
.steps{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:16px;max-width:760px;position:relative}
.step{display:grid;grid-template-columns:48px 1fr;gap:16px;position:relative}
.step:not(:last-child)::before{content:"";position:absolute;left:23px;top:50px;bottom:-18px;width:2px;background:var(--line)}
.num{width:48px;height:48px;border-radius:50%;background:var(--accent);color:var(--accent-ink);display:grid;place-items:center;font:800 1.2rem "Bricolage Grotesque",sans-serif}
.box{background:var(--surface);border:1px solid var(--line);border-radius:14px;padding:16px 18px}
.box h3{font-size:1.15rem;font-weight:600;margin-bottom:6px}
.box p{margin:0 0 10px}
.val{display:inline-block;background:var(--hl);color:var(--hl-ink);font-weight:600;font-size:.85rem;padding:2px 10px;border-radius:99px}
:focus-visible{outline:3px solid var(--hl);outline-offset:2px}
label.go{cursor:pointer}
.field{display:flex;flex-direction:column;gap:6px}
.field label{font-weight:600;font-size:.95rem}
.field input,.field select,.field textarea{width:100%;background:var(--bg);color:var(--ink);border:2px solid var(--line);border-radius:10px;font:inherit;padding:12px 14px;outline:none}
 .field :focus{border-color:var(--ink);box-shadow:0 0 0 3px var(--hl)}
 .field input:user-invalid{border-color:var(--bad)}
 .hint{color:var(--muted);font-size:.85rem}
 form.f{display:flex;flex-direction:column;gap:16px;max-width:640px}
 .two{display:grid;grid-template-columns:1fr 1fr;gap:16px}
 .btn{border:0;background:var(--accent);color:var(--accent-ink);font:600 1rem "Source Sans 3",sans-serif;padding:13px 22px;border-radius:10px;cursor:pointer;align-self:flex-start}
 .btn:hover{filter:brightness(1.08)}
 .empty{background:var(--surface);border:1px dashed var(--line);border-radius:14px;padding:22px;color:var(--muted)}
 .kv{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:10px;margin:0}
 .kv div{background:var(--bg);border:1px solid var(--line);border-radius:10px;padding:10px 14px}
 .kv dt{color:var(--muted);font-size:.85rem}
 .kv dd{margin:0;font-weight:600}
 .banner{border-radius:12px;padding:12px 16px;margin-bottom:18px;font-weight:600}
 .banner.ok{background:var(--accent);color:var(--accent-ink)}
 .banner.ko{background:var(--bad);color:#fff}
 .quiz{background:var(--bg);border:1px solid var(--line);border-radius:12px;padding:14px 16px}
 .quiz label.opt{display:flex;align-items:flex-start;gap:10px;font-weight:400;margin-top:8px}
 .quiz textarea{width:100%;background:var(--surface);color:var(--ink);border:2px solid var(--line);border-radius:10px;font:inherit;padding:12px 14px;outline:none}
 .quiz textarea:focus{border-color:var(--ink);box-shadow:0 0 0 3px var(--hl)}
 @media (max-width:620px){.two{grid-template-columns:1fr}}
@media (max-width:820px){
.app{grid-template-columns:1fr}
.side{position:static;height:auto;flex-direction:row;flex-wrap:wrap;align-items:center;border-right:0;border-bottom:1px solid var(--line);padding:14px 16px;gap:12px}
.nav{flex-direction:row;flex-wrap:wrap;width:100%;order:3}
.me{margin:0 0 0 auto;border:0;padding:0}
.me div{display:none}
.mini{grid-template-columns:repeat(2,1fr)}
}
</style>
</head>
<body>
<div class="app">
<input class="rd" type="radio" name="v" id="v-offres" ${onglet eq 'offres' ? 'checked' : ''} aria-label="Offres disponibles">
<input class="rd" type="radio" name="v" id="v-cv" ${onglet eq 'cv' ? 'checked' : ''} aria-label="Déposer mon CV">
<input class="rd" type="radio" name="v" id="v-cand" ${onglet eq 'cand' ? 'checked' : ''} aria-label="Mes candidatures">
<input class="rd" type="radio" name="v" id="v-etapes" ${onglet eq 'etapes' ? 'checked' : ''} aria-label="Étapes de postulation">

<aside class="side">
<div class="logo"><span>Job</span>Liste</div>
<nav class="nav" aria-label="Navigation">
<label for="v-offres">Offres disponibles <small>${nbOffres}</small></label>
<label for="v-cv">Déposer mon CV</label>
<label for="v-cand">Mes candidatures <small>${nbCandidatures}</small></label>
<label for="v-etapes">Étapes de postulation</label>
</nav>
<div class="me"><div class="av">${userEmail.substring(0,1).toUpperCase()}</div><div><b>${userEmail}</b><a href="<c:url value='/logout'/>">Se déconnecter</a></div></div>
</aside>

<main class="main">
<c:if test="${not empty success}"><div class="banner ok">${success}</div></c:if>
<c:if test="${not empty error}"><div class="banner ko">${error}</div></c:if>
<!-- Offres disponibles -->
<section class="panel p-offres">
<div class="head"><h1>Offres disponibles</h1><p>Choisissez une offre et postulez. Les offres auxquelles vous avez déjà postulé sont signalées.</p></div>
<c:if test="${empty offres}">
<div class="empty">Aucune offre d'emploi disponible pour le moment.</div>
</c:if>
<div class="grid">
<c:forEach var="offre" items="${offres}">
<article class="card job" data-d="info">

<span class="tag">
${offre.statut.libelle}
</span>

<h3>
${offre.poste.intitule}
</h3>

<div class="meta">
${offre.departement.nom}
·
<c:choose>
<c:when test="${empty offre.salaire}">Salaire non précisé</c:when>
<c:otherwise>${offre.salaire} Ar</c:otherwise>
</c:choose>
</div>

<div class="facts">
<span>
${offre.nbPlaces} places
</span>

<span>
${offre.anneesExperience} an(s) d’expérience
</span>
</div>

<details>
<summary>Voir la description</summary>
<p>
${offre.poste.description}
</p>
</details>

<c:choose>
<c:when test="${offresPostulees.contains(offre.id)}">
<span class="go off">Déjà postulé</span>
</c:when>
<c:otherwise>
<a class="go" href="<c:url value='/espaceCandidat'/>?offre=${offre.id}">Postuler</a>
</c:otherwise>
</c:choose>

</article>
</c:forEach>
</div>
</section>

<!-- Déposer mon CV -->
<section class="panel p-cv">
<div class="head"><h1>Déposer mon CV</h1><p>Renseignez vos informations pour postuler. Votre CV passe ensuite à l’étape 1 : étude des CV par le RH.</p></div>
<form class="f box" method="post" action="<c:url value='/candidatures/cv'/>">
<div class="field"><label for="cv-offre">Offre</label><select id="cv-offre" name="id_offre" required><option value="" selected disabled>Choisir une offre</option><c:forEach var="offre" items="${offres}"><option value="${offre.id}" ${(offreSelectionnee eq offre.id) ? 'selected' : ''}>${offre.poste.intitule} — ${offre.departement.nom} (publiée le ${f:date(offre.datePublication)})</option></c:forEach></select></div>
<div class="two"><div class="field"><label for="cv-nom">Nom</label><input id="cv-nom" name="nom" type="text" autocomplete="family-name" required></div><div class="field"><label for="cv-prenom">Prénom</label><input id="cv-prenom" name="prenom" type="text" autocomplete="given-name" required></div></div>
<div class="two"><div class="field"><label for="cv-naiss">Date de naissance</label><input id="cv-naiss" name="date_naissance" type="date" autocomplete="bday"></div><div class="field"><label for="cv-exp">Années d’expérience</label><input id="cv-exp" name="annee_experience" type="number" min="0" max="60" step="1" required></div></div>
<div class="field"><label for="cv-mail">Adresse e-mail</label><input id="cv-mail" name="email" type="email" value="${userEmail}" autocomplete="email"></div>
<div class="field"><label for="cv-diplome">Diplôme</label><input id="cv-diplome" name="diplome" type="text" placeholder="Ex. Master en informatique"></div>
<div class="field"><label for="cv-comp">Compétences</label><textarea id="cv-comp" name="competences" rows="3" placeholder="Ex. Java, Spring, SQL"></textarea></div>
<p class="hint">Votre compte et la date d’envoi sont enregistrés automatiquement. Après l’envoi, votre candidature passe à l’étape 1 : elle est en attente de validation par le RH.</p>
<button class="btn" type="submit">Envoyer ma candidature</button>
</form>
</section>

<!-- Mes candidatures -->
<section class="panel p-cand">
<div class="head"><h1>Mes candidatures</h1><p>Suivez l’avancement de chacune de vos candidatures, étape par étape.</p></div>
<c:if test="${empty candidatureVues}">
<div class="empty">Vous n’avez encore postulé à aucune offre. Rendez-vous dans « Offres disponibles » pour postuler.</div>
</c:if>
<div class="apps">
<c:forEach var="vue" items="${candidatureVues}">
<article class="app-row">
<div class="top">
<div>
<h3>${vue.intitulePoste}</h3>
<div class="meta">${vue.departement} · Postulé le ${f:date(vue.datePostulation)}</div>
</div>
<span class="st ${vue.statutClasse}">${vue.statutLibelle}</span>
</div>

<c:if test="${not empty vue.candidature}">
<ol class="mini auto">
<c:forEach var="e" items="${etapesRecrutement}">
<li class="${e.ordre lt vue.ordreEtape ? 'done' : ''} ${e.code eq vue.codeEtape ? 'cur' : ''}">
<b>${e.libelle}</b>
<span>Étape ${e.ordre}</span>
</li>
</c:forEach>
</ol>
</c:if>

<c:if test="${not empty vue.etapes}">
<ol class="mini auto">
<c:forEach var="etape" items="${vue.etapes}" varStatus="st">
<li class="done ${st.last ? 'cur' : ''}">
<b>${etape.nom}</b>
<span>
<c:if test="${not empty etape.note}">Note ${etape.note}</c:if>
<c:if test="${not empty etape.rang}"> · Rang ${etape.rang}</c:if>
</span>
</li>
</c:forEach>
</ol>
</c:if>
<p class="meta">${vue.resumeEtapes}</p>

<c:if test="${vue.codeEtape eq 'ENTRETIEN' and not empty vue.entretien}">
<div class="box">
<h3>Convocation à l'entretien</h3>
<p class="meta"><b>Date et heure :</b> ${vue.entretien.dateEntretien}</p>
<p class="meta"><b>Lieu :</b> ${vue.entretien.lieu}</p>
<p class="meta">Le RH a enregistré votre rendez-vous. Consultez ces informations avant l'entretien.</p>
</div>
</c:if>

<c:if test="${vue.questionnaireARemplir}">
<div class="box">
<h3>${vue.questionnaire.titre}</h3>
<p class="meta">Étape en cours : questionnaire. Répondez aux ${vue.questionnaire.questions.size()} question(s) ci-dessous puis validez : vos réponses seront transmises au RH.</p>
<form class="f" method="post" action="<c:url value='/candidatures/questionnaire'/>">
<input type="hidden" name="id_candidature" value="${vue.candidature.id}">
<c:forEach var="q" items="${vue.questionnaire.questions}" varStatus="qs">
<div class="quiz">
<div class="field"><label>${qs.count}. ${q.enonce} <span class="hint">(${q.points} point(s))</span></label></div>
<c:choose>
<c:when test="${q.type.code eq 'QCM'}">
<c:forEach var="ch" items="${q.choix}">
<label class="opt"><input type="radio" name="choix_${q.id}" value="${ch.id}"> <span>${ch.libelle}</span></label>
</c:forEach>
</c:when>
<c:otherwise>
<textarea name="texte_${q.id}" rows="3" placeholder="Votre réponse"></textarea>
</c:otherwise>
</c:choose>
</div>
</c:forEach>
<button class="btn" type="submit">Envoyer mes réponses</button>
</form>
</div>
</c:if>

<c:if test="${vue.enCours and vue.etapeQuestionnaire and vue.questionnaire.repondu}">
<div class="box">
<h3>${vue.questionnaire.titre}</h3>
<p class="meta">Vos réponses ont été envoyées. Elles sont en attente de validation par le RH.</p>
<details>
<summary>Revoir mes réponses</summary>
<div class="etapes-list">
<c:forEach var="q" items="${vue.questionnaire.questions}">
<c:set var="rep" value="${vue.questionnaire.getReponseDe(q)}"/>
<div class="test">
<div><b>${q.enonce}</b></div>
<div class="facts">
<c:if test="${not empty rep}">
<c:if test="${not empty rep.choix}"><span>${rep.choix.libelle}</span></c:if>
<c:if test="${not empty rep.reponseTexte}"><span>${rep.reponseTexte}</span></c:if>
</c:if>
<c:if test="${empty rep}"><span>Sans réponse</span></c:if>
</div>
</div>
</c:forEach>
</div>
</details>
</div>
</c:if>

<c:if test="${not empty vue.etapes}">
<details>
<summary>Détail des étapes franchies</summary>
<div class="etapes-list">
<c:forEach var="etape" items="${vue.etapes}">
<div class="test">
<div><b>${etape.nom}</b><span class="meta">Sélectionné le ${f:dateTime(etape.dateSelection)}</span></div>
<div class="facts">
<c:if test="${not empty etape.note}"><span>Note : ${etape.note}</span></c:if>
<c:if test="${not empty etape.rang}"><span>Rang : ${etape.rang}</span></c:if>
</div>
</div>
</c:forEach>
</div>
</details>
</c:if>

<c:if test="${not empty vue.cvCandidat}">
<details>
<summary>Voir mon CV</summary>
<dl class="kv">
<div><dt>Nom</dt><dd>${vue.cvCandidat.nom}</dd></div>
<div><dt>Prénom</dt><dd>${vue.cvCandidat.prenom}</dd></div>
<div><dt>Date de naissance</dt><dd>${vue.cvCandidat.dateNaissance}</dd></div>
<div><dt>Années d’expérience</dt><dd>${vue.cvCandidat.anneeExperience}</dd></div>
<div><dt>E-mail</dt><dd>${vue.cvCandidat.email}</dd></div>
<div><dt>Déposé le</dt><dd>${f:dateTime(vue.cvCandidat.createdAt)}</dd></div>
</dl>
</details>
</c:if>
</article>
</c:forEach>
</div>
</section>

<!-- Étapes de postulation -->
<section class="panel p-etapes">
<div class="head"><h1>Étapes de postulation</h1><p>Chaque candidature passe par les étapes du processus de recrutement. À chaque étape, le RH valide le résultat avant de passer à la suivante.</p></div>
<ol class="steps">
<c:forEach var="etape" items="${etapes}" varStatus="st">
<li class="step">
<div class="num">${st.count}</div>
<div class="box">
<h3>Étape ${st.count} — ${etape.nom}</h3>
<c:choose>
<c:when test="${etape.nom.equalsIgnoreCase('selection')}">
<p>Le RH examine les CV et décide quels candidats sont retenus.</p>
<p class="meta"><b>À fournir : votre CV (nom, prénom, date de naissance, années d’expérience, e-mail).</b></p>
</c:when>
<c:when test="${etape.nom.equalsIgnoreCase('Test')}">
<p>Le système enregistre les notes et peut calculer les résultats. Le RH vérifie les résultats et valide la liste des candidats retenus.</p>
<p class="meta"><b>À fournir : vos réponses aux questions du test.</b></p>
</c:when>
<c:when test="${etape.nom.equalsIgnoreCase('Entretien')}">
<p>Les évaluateurs saisissent les notes et leurs commentaires. Le RH valide les résultats des entretiens.</p>
<p class="meta"><b>Rien à saisir : vous consultez les critères et le résultat de votre entretien.</b></p>
</c:when>
<c:otherwise>
<p>Le RH traite cette étape et valide la liste des candidats retenus.</p>
</c:otherwise>
</c:choose>
<span class="val">Validation RH</span>
</div>
</li>
</c:forEach>
</ol>
<p class="meta">Les étapes que vous avez franchies pour chaque offre sont détaillées dans « Mes candidatures ».</p>
</section>
</main>
</div>
</body>
</html>
