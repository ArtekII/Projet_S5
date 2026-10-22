<!DOCTYPE html>
<html lang="fr">

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <title>Connexion – JobListe</title>
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

        h1 {
            font-family: "Bricolage Grotesque", "Source Sans 3", system-ui, sans-serif;
            margin: 0;
            line-height: 1.1
        }

        .wrap {
            max-width: 520px;
            margin: 0 auto;
            padding: 0 20px 60px
        }

        header.top {
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

        .intro {
            text-align: center;
            padding: 20px 0 24px
        }

        .intro h1 {
            font-size: clamp(2rem, 6vw, 2.8rem);
            font-weight: 800;
            letter-spacing: -.02em
        }

        .intro p {
            color: var(--muted);
            margin: 10px 0 0
        }

        .rd {
            position: absolute;
            opacity: 0;
            pointer-events: none
        }

        .card {
            background: var(--surface);
            border: 1px solid var(--line);
            border-radius: 14px;
            padding: 22px
        }

        .tabs {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 6px;
            background: var(--bg);
            border: 1px solid var(--line);
            border-radius: 10px;
            padding: 4px;
            margin-bottom: 22px
        }

        .tabs label {
            text-align: center;
            padding: 8px 10px;
            border-radius: 7px;
            font-weight: 600;
            cursor: pointer
        }

        #t-login:checked~.card label[for="t-login"],
        #t-reg:checked~.card label[for="t-reg"] {
            background: var(--accent);
            color: var(--accent-ink)
        }

        #t-login:checked~.card .f-reg,
        #t-reg:checked~.card .f-login {
            display: none
        }

        #t-login:focus-visible~.card label[for="t-login"],
        #t-reg:focus-visible~.card label[for="t-reg"] {
            outline: 3px solid var(--hl);
            outline-offset: 2px
        }

        form {
            display: flex;
            flex-direction: column;
            gap: 16px
        }

        .field {
            display: flex;
            flex-direction: column;
            gap: 6px
        }

        .field label {
            font-weight: 600;
            font-size: .95rem
        }

        .field input {
            width: 100%;
            background: var(--bg);
            color: var(--ink);
            border: 2px solid var(--line);
            border-radius: 10px;
            font: inherit;
            padding: 12px 14px;
            outline: none
        }

        .field input:focus {
            border-color: var(--ink);
            box-shadow: 0 0 0 3px var(--hl)
        }

        .field input:user-invalid {
            border-color: #c0392b
        }

        .hint {
            color: var(--muted);
            font-size: .85rem
        }

        .row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 10px;
            flex-wrap: wrap;
            font-size: .95rem
        }

        .row a,
        .alt a {
            color: var(--accent);
            font-weight: 600
        }

        .check {
            display: flex;
            gap: 10px;
            align-items: flex-start;
            font-size: .95rem
        }

        .check input {
            margin-top: 5px;
            accent-color: var(--accent)
        }

        .btn {
            border: 0;
            background: var(--accent);
            color: var(--accent-ink);
            font: 600 1rem "Source Sans 3", sans-serif;
            padding: 13px 22px;
            border-radius: 10px;
            cursor: pointer
        }

        .btn:hover {
            filter: brightness(1.08)
        }

        .alt {
            text-align: center;
            color: var(--muted);
            font-size: .95rem;
            margin: 0
        }

        .alt label {
            color: var(--accent);
            font-weight: 600;
            cursor: pointer;
            text-decoration: underline
        }

        :focus-visible {
            outline: 3px solid var(--hl);
            outline-offset: 2px
        }
    </style>
</head>

<body>
    <div class="wrap">
        <header class="top">
            <div class="logo"><span>Job</span>Liste</div>
        </header>
        <section class="intro">
            <h1>Bienvenue</h1>
            <p>Connectez-vous ou créez un compte pour postuler aux offres.</p>
        </section>
        <main>
            <input class="rd" type="radio" name="tab" id="t-login" checked aria-label="Se connecter">
            <input class="rd" type="radio" name="tab" id="t-reg" aria-label="Créer un compte">
            <div class="card">
                <div class="tabs"><label for="t-login">Se connecter</label><label for="t-reg">Créer un compte</label>
                </div>

                <form class="f-login" method="post" action="/login">
                    <div class="field"><label for="l-email">Adresse e-mail</label><input id="l-email" name="email"
                            type="email" autocomplete="email" placeholder="vous@exemple.com" required></div>
                    <div class="field"><label for="l-pass">Mot de passe</label><input id="l-pass" name="mot_de_passe"
                            type="password" autocomplete="current-password" required></div>
                    <div class="row"><span class="check"><input type="checkbox" id="l-rem" name="se_souvenir"><label
                                for="l-rem">Rester connecté</label></span><a href="#">Mot de passe oublié ?</a></div>
                    <button class="btn" type="submit">Se connecter</button>
                    <p class="alt">Pas encore de compte ? <label for="t-reg">Créer un compte</label></p>
                </form>

                <form class="f-reg" method="post" action="/register">
                    <input type="hidden" name="role" value="CANDIDAT">
                    <div class="field"><label for="r-email">Adresse e-mail</label><input id="r-email" name="email"
                            type="email" autocomplete="email" placeholder="vous@exemple.com" required></div>
                    <div class="field"><label for="r-pass">Mot de passe</label><input id="r-pass" name="mot_de_passe"
                            type="password" autocomplete="new-password" minlength="8" required><span class="hint">8
                            caractères minimum.</span></div>
                    <div class="field"><label for="r-conf">Confirmer le mot de passe</label><input id="r-conf"
                            name="confirmation" type="password" autocomplete="new-password" minlength="8" required>
                    </div>
                    <div class="check"><input type="checkbox" id="r-cgu" name="cgu" required><label
                            for="r-cgu">J’accepte les conditions d’utilisation.</label></div>
                    <button class="btn" type="submit">Créer mon compte</button>
                    <p class="alt">Déjà inscrit ? <label for="t-login">Se connecter</label></p>
                </form>
            </div>
        </main>
    </div>
</body>

</html>