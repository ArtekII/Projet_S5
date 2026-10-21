<%@ page import="java.util.List" %>
    <%@ page import="com.project.entity.OffreEmploi" %>

        <% List<OffreEmploi> offres =
            (List<OffreEmploi>) request.getAttribute("offres");
                %>
                <!DOCTYPE html>
                <html lang="fr">

                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
                    <title>Offres d’emploi</title>
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link
                        href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:wght@600;800&family=Source+Sans+3:wght@400;600&display=swap"
                        rel="stylesheet">
                    <style>
                        :root {
                            --bg: #f2f6f7;
                            --surface: #fff;
                            --ink: #12242d;
                            --muted: #5a6d76;
                            --line: #d5e0e4;
                            --accent: #0b7a75;
                            --accent-ink: #fff;
                            --hl: #f4b63f;
                            --hl-ink: #3a2a00;
                            color-scheme: light;
                            box-sizing: border-box;
                            padding-top: env(safe-area-inset-top, 0px);
                            padding-bottom: env(safe-area-inset-bottom, 0px)
                        }

                        @media (prefers-color-scheme:dark) {
                            :root:not([data-theme="light"]) {
                                --bg: #0f1b21;
                                --surface: #16262e;
                                --ink: #e8f1f3;
                                --muted: #9bb0b8;
                                --line: #2a3f49;
                                --accent: #3cc4bb;
                                --accent-ink: #06211f;
                                color-scheme: dark
                            }
                        }

                        :root[data-theme="dark"] {
                            --bg: #0f1b21;
                            --surface: #16262e;
                            --ink: #e8f1f3;
                            --muted: #9bb0b8;
                            --line: #2a3f49;
                            --accent: #3cc4bb;
                            --accent-ink: #06211f;
                            color-scheme: dark
                        }

                        html {
                            scroll-padding-top: env(safe-area-inset-top, 0px)
                        }

                        *,
                        *::before,
                        *::after {
                            box-sizing: border-box
                        }

                        body {
                            margin: 0;
                            background: var(--bg);
                            color: var(--ink);
                            font: 400 17px/1.55 "Source Sans 3", system-ui, sans-serif
                        }

                        h1,
                        h2,
                        h3 {
                            font-family: "Bricolage Grotesque", "Source Sans 3", system-ui, sans-serif;
                            margin: 0;
                            line-height: 1.15
                        }

                        .wrap {
                            max-width: 1240px;
                            margin: 0 auto;
                            padding: 0 20px 60px
                        }

                        header.top {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            padding: 18px 0
                        }

                        .logo {
                            font: 800 1.25rem "Bricolage Grotesque", sans-serif
                        }

                        .logo span {
                            background: var(--hl);
                            color: var(--hl-ink);
                            padding: 0 6px;
                            border-radius: 4px;
                            margin-right: 4px
                        }

                        .login {
                            background: none;
                            border: 2px solid var(--ink);
                            color: var(--ink);
                            font: 600 1rem "Source Sans 3", sans-serif;
                            padding: 8px 18px;
                            border-radius: 8px;
                            cursor: pointer
                        }

                        .login:hover {
                            background: var(--ink);
                            color: var(--bg)
                        }

                        .hero {
                            padding: 30px 0 24px;
                            text-align: center
                        }

                        .hero h1 {
                            font-size: clamp(2.2rem, 6vw, 3.8rem);
                            font-weight: 800;
                            max-width: 16ch;
                            margin: 0 auto;
                            letter-spacing: -.02em
                        }

                        .hero p {
                            color: var(--muted);
                            max-width: 52ch;
                            margin: 14px auto 24px
                        }

                        .search {
                            display: flex;
                            background: var(--surface);
                            border: 2px solid var(--ink);
                            border-radius: 10px;
                            overflow: hidden;
                            max-width: 640px;
                            margin: 0 auto;
                            text-align: left
                        }

                        .search input {
                            flex: 1;
                            min-width: 0;
                            border: 0;
                            background: transparent;
                            color: var(--ink);
                            font: inherit;
                            padding: 14px 16px;
                            outline: none
                        }

                        .search button {
                            border: 0;
                            background: var(--accent);
                            color: var(--accent-ink);
                            font: 600 1rem "Source Sans 3", sans-serif;
                            padding: 0 22px;
                            cursor: pointer
                        }

                        .search:focus-within {
                            box-shadow: 0 0 0 3px var(--hl)
                        }

                        /* Filtres par domaine (CSS seul) */
                        .rd {
                            position: absolute;
                            opacity: 0;
                            pointer-events: none
                        }

                        .chips {
                            display: flex;
                            flex-wrap: wrap;
                            justify-content: center;
                            gap: 10px;
                            margin: 8px 0 26px
                        }

                        .chips label {
                            border: 1px solid var(--line);
                            background: var(--surface);
                            border-radius: 99px;
                            padding: 6px 18px;
                            cursor: pointer;
                            font-weight: 600
                        }

                        .chips label:hover {
                            border-color: var(--accent)
                        }

                        #d-all:checked~.chips label[for="d-all"],
                        #d-info:checked~.chips label[for="d-info"],
                        #d-mkt:checked~.chips label[for="d-mkt"],
                        #d-fin:checked~.chips label[for="d-fin"],
                        #d-san:checked~.chips label[for="d-san"],
                        #d-log:checked~.chips label[for="d-log"] {
                            background: var(--accent);
                            border-color: var(--accent);
                            color: var(--accent-ink)
                        }

                        #d-info:checked~#list .job:not([data-d="info"]),
                        #d-mkt:checked~#list .job:not([data-d="mkt"]),
                        #d-fin:checked~#list .job:not([data-d="fin"]),
                        #d-san:checked~#list .job:not([data-d="san"]),
                        #d-log:checked~#list .job:not([data-d="log"]) {
                            display: none
                        }

                        /* Cartes : 4 par ligne */
                        #list {
                            display: grid;
                            grid-template-columns: repeat(4, 1fr);
                            gap: 18px
                        }

                        .job {
                            display: flex;
                            flex-direction: column;
                            gap: 10px;
                            background: var(--surface);
                            border: 1px solid var(--line);
                            border-radius: 14px;
                            padding: 18px
                        }

                        .job:hover {
                            border-color: var(--accent)
                        }

                        .tag {
                            align-self: flex-start;
                            background: var(--hl);
                            color: var(--hl-ink);
                            font-weight: 600;
                            font-size: .85rem;
                            padding: 2px 10px;
                            border-radius: 99px
                        }

                        .job h3 {
                            font-size: 1.15rem;
                            font-weight: 600
                        }

                        .meta {
                            color: var(--muted);
                            font-size: .95rem
                        }

                        .facts {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 6px
                        }

                        .facts span {
                            background: var(--bg);
                            border: 1px solid var(--line);
                            border-radius: 6px;
                            padding: 2px 8px;
                            font-size: .85rem
                        }

                        details summary {
                            cursor: pointer;
                            color: var(--accent);
                            font-weight: 600
                        }

                        details p {
                            margin: 8px 0 0;
                            font-size: .95rem
                        }

                        .go {
                            display: block;
                            margin-top: auto;
                            text-align: center;
                            background: var(--accent);
                            color: var(--accent-ink);
                            text-decoration: none;
                            font-weight: 600;
                            padding: 10px 20px;
                            border-radius: 8px
                        }

                        :focus-visible {
                            outline: 3px solid var(--hl);
                            outline-offset: 2px
                        }

                        @media (max-width:1100px) {
                            #list {
                                grid-template-columns: repeat(2, 1fr)
                            }
                        }

                        @media (max-width:620px) {
                            #list {
                                grid-template-columns: 1fr
                            }
                        }
                    </style>
                </head>

                <body>
                    <div class="wrap">
                        <header class="top">
                            <div class="logo"><span>Job</span>Liste</div><button class="login" type="button">Se
                                connecter</button>
                        </header>
                        <section class="hero">
                            <h1>Trouvez votre prochain poste</h1>
                            <p>Parcourez les offres du moment, filtrez par domaine, puis postulez en un clic.</p>
                            <form class="search" role="search" onsubmit="return false">
                                <input type="search" placeholder="Métier, entreprise ou mot-clé"
                                    aria-label="Rechercher une offre">
                                <button type="button">Rechercher</button>
                            </form>
                        </section>
                        <main>
                            <input class="rd" type="radio" name="d" id="d-all" checked aria-label="Tous">
                            <input class="rd" type="radio" name="d" id="d-info" aria-label="Informatique">
                            <input class="rd" type="radio" name="d" id="d-mkt" aria-label="Marketing">
                            <input class="rd" type="radio" name="d" id="d-fin" aria-label="Finance">
                            <input class="rd" type="radio" name="d" id="d-san" aria-label="Santé">
                            <input class="rd" type="radio" name="d" id="d-log" aria-label="Logistique">
                            <div class="chips">
                                <label for="d-all">Tous</label><label for="d-info">Informatique</label><label
                                    for="d-mkt">Marketing</label><label for="d-fin">Finance</label><label
                                    for="d-san">Santé</label><label for="d-log">Logistique</label>
                            </div>
                            <div id="list">

                                <% if (offres !=null && !offres.isEmpty()) { for (OffreEmploi offre : offres) { %>

                                    <article class="job" data-d="info">

                                        <span class="tag">
                                            <%= offre.getStatut().getLibelle() %>
                                        </span>

                                        <h3>
                                            <%= offre.getPoste().getIntitule() %>
                                        </h3>

                                        <div class="meta">
                                            <%= offre.getDepartement().getNom() %>
                                                ·
                                                <%= offre.getSalaire() !=null ? offre.getSalaire() + " Ar"
                                                    : "Salaire non précisé" %>
                                        </div>

                                        <div class="facts">
                                            <span>
                                                <%= offre.getNbPlaces() %> places
                                            </span>

                                            <span>
                                                <%= offre.getAnneesExperience() %>
                                                    an(s) d’expérience
                                            </span>
                                        </div>

                                        <details>
                                            <summary>Voir la description</summary>
                                            <p>
                                                <%= offre.getPoste().getDescription() %>
                                            </p>
                                        </details>

                                        <a class="go" href="#">Postuler</a>

                                    </article>

                                    <% } } else { %>

                                        <p>Aucune offre d'emploi disponible.</p>

                                        <% } %>

                            </div>
                            ```
                        </main>
                    </div>
                </body>
                </html>