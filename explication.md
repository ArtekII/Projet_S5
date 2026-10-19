# Explication détaillée du projet « Recrutement »

> Document généré pour comprendre **tout** le fonctionnement du projet : architecture,
> base de données, workflow métier, et **le contenu intégral de chaque fichier source**.
>
> ⚠️ **Version à jour** : les vues sont en **JSP + JSTL** (et non Thymeleaf), l'application
> est packagée en **WAR**.

---

## Table des matières

1. [But du projet](#1-but-du-projet)
2. [Stack technique](#2-stack-technique)
3. [Compiler et lancer (deploy.sh)](#3-compiler-et-lancer)
4. [Architecture en couches](#4-architecture-en-couches)
5. [Arborescence des fichiers](#5-arborescence-des-fichiers)
6. [Le workflow métier](#6-le-workflow-métier)
7. [La base de données](#7-la-base-de-données)
8. [Comptes de test](#8-comptes-de-test)
9. [Détail fichier par fichier](#9-détail-fichier-par-fichier)
10. [Cycle de vie d'une requête HTTP](#10-cycle-de-vie-dune-requête-http)
11. [Parcours complet du workflow (URL par URL)](#11-parcours-complet-du-workflow)
12. [Sécurité](#12-sécurité)
13. [Problèmes fréquents](#13-problèmes-fréquents)
14. [Pistes d'évolution](#14-pistes-dévolution)

---

## 1. But du projet

Application web de **gestion du recrutement** avec un workflow de validation à plusieurs
niveaux. Chaque acteur se connecte avec un compte et voit une interface adaptée à son rôle :

| Rôle | Ce qu'il fait |
|------|----------------|
| **MANAGER** | Demande une offre sur un poste existant (dropdown), ou demande la **création d'un nouveau poste** au RH |
| **RH** | Valide/insère les nouveaux postes ; revoit chaque offre (même formulaire que le manager) **+ fixe le salaire** |
| **FINANCE** | Revoit l'offre, **modifie ou valide le prix (salaire)** |
| **DIRECTION** | **Validation finale** → publie officiellement l'offre |
| **CANDIDAT** | Voit les offres publiées et leur détail |

Le principe clé : une offre ne devient **visible par les candidats** que si elle a traversé
**toute** la chaîne de validation `Manager → RH → Finance → Direction`.

---

## 2. Stack technique

| Élément | Choix | Rôle |
|---------|-------|------|
| Langage | **Java 21** | Utilise les *switch expressions* (`case X -> ...`) |
| Framework | **Spring Boot 3.3.5** | Application web autonome (Tomcat embarqué) |
| Web MVC | **Spring Web MVC** | Contrôleurs + résolution des vues |
| Vues | **JSP + JSTL 3.0** | Pages rendues côté serveur |
| Moteur JSP | **Apache Jasper** (`tomcat-embed-jasper`) | Compile les `.jsp` |
| Packaging | **WAR** | Compatible Tomcat embarqué *et* conteneur externe |
| Persistance | **Spring Data JPA** + **Hibernate** | Entités + repositories |
| Base (dev) | **H2** en mémoire | Aucune installation, données rechargées à chaque démarrage |
| Base (prod) | **PostgreSQL** | Driver présent, schéma dans `sql/schema.sql` |
| Sécurité mdp | **spring-security-crypto** | `BCryptPasswordEncoder` |
| Build | **Maven** (+ wrapper `mvnw`) | Compilation, packaging, exécution |
| Tests | **spring-boot-starter-test** | JUnit 5 |

> ⚠️ **Point critique** : le projet a besoin de **Java 21**. Sur cette machine `JAVA_HOME`
> pointe vers Java 8, ce qui provoque des erreurs de compilation (`switch` expressions
> non supportées). Le script `deploy.sh` force Java 21 automatiquement.

> ℹ️ **Pourquoi le WAR ?** Les JSP ne fonctionnent pas de manière fiable dans un **jar
> exécutable** avec Tomcat embarqué. Le passage en `war` (avec `ServletInitializer`)
> règle le problème et permet aussi un déploiement dans un Tomcat externe.

---

## 3. Compiler et lancer

Le script `deploy.sh` (voir son contenu complet en [9.2](#92-deploysh)) :

```bash
./deploy.sh          # compile + lance en dev (spring-boot:run, H2 en mémoire)
./deploy.sh build    # compile + construit le war exécutable (tests ignorés)
./deploy.sh war      # construit puis lance le war packagé
./deploy.sh test     # lance les tests
./deploy.sh clean    # nettoie target/
./deploy.sh help     # aide
```

Sans le script (à la main) :

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # OBLIGATOIRE
./mvnw spring-boot:run                                # dev
./mvnw clean package -DskipTests                      # war
java -jar target/project-0.0.1-SNAPSHOT.war           # exécuter le war
```

L'application écoute sur **http://localhost:8080**.
La console H2 est disponible sur **http://localhost:8080/h2-console**.

---

## 4. Architecture en couches

L'application suit le schéma classique **Controller → Service → Repository → Entity**,
avec des **vues JSP** en sortie.

```
Navigateur  ──HTTP──▶  Controller  ──▶  Service ──▶  Repository ──▶  Entity ──▶  BDD (H2/Postgres)
     ▲                     │
     └──── HTML (JSP + JSTL)◀┘   (compilé par Jasper)
```

- **Controller** : reçoit la requête, lit la session, appelle le service, renvoie un **nom
  de vue** (`return "manager/dashboard"`) ou une redirection (`return "redirect:/..."`).
- **Résolution de vue** : `spring.mvc.view.prefix=/WEB-INF/jsp/` +
  `spring.mvc.view.suffix=.jsp` transforment `"manager/dashboard"` en
  `/WEB-INF/jsp/manager/dashboard.jsp`.
- **Service** : contient la **logique métier** et les transitions d'état. Annoté
  `@Transactional` pour les écritures.
- **Repository** : interfaces Spring Data JPA ; les méthodes sont **générées** à partir
  de leur nom.
- **Entity** : classes JPA mappées aux tables SQL.
- **Config** : un interceptor qui protège les URL par rôle.
- **Vues JSP** : HTML + balises JSTL (`c:forEach`, `c:if`…) + EL (`${...}`).

---

## 5. Arborescence des fichiers

```
Projet_S5/
├── deploy.sh                        # Script de build/lancement (forcer Java 21)
├── pom.xml                          # Dépendances + config Maven (packaging war)
├── mvnw / mvnw.cmd                  # Maven Wrapper
├── .mvn/wrapper/maven-wrapper.properties
├── log.txt                          # Note perso (comptes de test) — non utilisé par le code
├── sql/
│   ├── schema.sql                   # Schéma de référence PostgreSQL
│   ├── 19-10-26.sql                 # Copie datée de schema.sql (392 lignes)
│   └── 19/10/26.sql                 # Autre copie datée (392 lignes)
└── src/
    ├── main/
    │   ├── java/com/project/
    │   │   ├── ProjectApplication.java          # Point d'entrée Spring Boot
    │   │   ├── ServletInitializer.java          # Déploiement WAR (conteneur externe)
    │   │   ├── config/
    │   │   │   ├── AuthInterceptor.java         # Contrôle d'accès par rôle
    │   │   │   └── WebConfig.java               # Enregistre l'interceptor
    │   │   ├── web/
    │   │   │   └── FormatUtils.java             # Fonctions JSP (formatage des dates)
    │   │   ├── controller/                      # 6 contrôleurs
    │   │   │   ├── AuthController.java          # /login /logout
    │   │   │   ├── ManagerController.java       # /manager/**
    │   │   │   ├── RhController.java            # /rh/**
    │   │   │   ├── FinanceController.java       # /finance/**
    │   │   │   ├── DirectionController.java     # /direction/**
    │   │   │   └── CandidatController.java      # /candidat/**
    │   │   ├── entity/                          # 9 entités JPA
    │   │   ├── repository/                      # 9 repositories Spring Data
    │   │   └── service/
    │   │       ├── AuthService.java             # Authentification + session
    │   │       └── OffreService.java            # Toute la logique du workflow
    │   ├── resources/
    │   │   ├── application.properties           # Config Spring Boot
    │   │   ├── schema-h2.sql                    # Schéma + données de référence (H2)
    │   │   └── data.sql                         # Comptes de test + postes/départements
    │   └── webapp/WEB-INF/
    │       ├── jsp/
    │       │   ├── login.jsp
    │       │   ├── fragments/navbar.jsp
    │       │   ├── manager/{dashboard,demandes}.jsp
    │       │   ├── rh/dashboard.jsp
    │       │   ├── finance/dashboard.jsp
    │       │   ├── direction/dashboard.jsp
    │       │   └── candidat/{offres,detail}.jsp
    │       └── tld/functions.tld                # Fonctions EL (f:dateTime)
    └── test/java/com/project/ProjectApplicationTests.java
```

> Les fichiers `sql/19-10-26.sql` et `sql/19/10/26.sql` sont des **copies datées** de
> `sql/schema.sql` (même contenu, en-tête différent). Ils ne sont **pas** utilisés au
> démarrage (seul `src/main/resources/schema-h2.sql` l'est). Ils ne sont donc pas
> reproduits intégralement ici pour éviter 800 lignes de doublon.

---

## 6. Le workflow métier

### 6.1 Vue d'ensemble

```
        ┌────────────┐
        │  MANAGER   │  (1) choisit un poste dans le dropdown
        │            │      OU demande un nouveau poste
        └─────┬──────┘
              │  offre : statut EN_ATTENTE_RH
              ▼
        ┌────────────┐
        │     RH     │  (2) revoit l'offre (poste/expérience/places)
        │            │      + DÉFINIT LE SALAIRE
        └─────┬──────┘
              │  statut EN_ATTENTE_FINANCE
              ▼
        ┌────────────┐
        │  FINANCE   │  (3) modifie ou valide le PRIX (salaire)
        └─────┬──────┘
              │  statut EN_ATTENTE_DIRECTION
              ▼
        ┌────────────┐
        │ DIRECTION  │  (4) VALIDATION FINALE → publication
        └─────┬──────┘
              │  statut PUBLIEE
              ▼
        ┌────────────┐
        │  CANDIDAT  │  (5) voit l'offre publiée
        └────────────┘
```

### 6.2 Les deux entrées possibles du Manager

1. **Poste déjà existant** (`/manager/offre`) : le manager choisit dans un `<select>`
   le poste, saisit les *années d'expérience* et le *nombre de places*, puis envoie.
   → crée une **`OffreEmploi`** au statut `EN_ATTENTE_RH`.
2. **Nouveau poste** (`/manager/demande-poste`) : le manager propose un **intitulé** +
   une justification. → crée une **`DemandePoste`** au statut `EN_ATTENTE`.
   Le RH la valide et le poste est alors ajouté au **catalogue** (donc au dropdown).

### 6.3 Les statuts d'une offre (`statut_offre`)

| Ordre | Code | Libellé | Qui agit ensuite |
|-------|------|---------|------------------|
| 1 | `EN_ATTENTE_RH` | En attente de validation RH | RH |
| — | `REFUSEE_RH` | Refusée par le RH | (fin) |
| 2 | `EN_ATTENTE_FINANCE` | En attente de validation Finance | Finance |
| — | `REFUSEE_FINANCE` | Refusée par la Finance | (fin) |
| 3 | `EN_ATTENTE_DIRECTION` | En attente de validation Direction | Direction |
| — | `REFUSEE_DIRECTION` | Refusée par la Direction | (fin) |
| 4 | `PUBLIEE` | Publiée (visible candidats) | Candidat |
| — | `CLOTUREE` | Clôturée | (fin) |

Chaque transition **écrit une ligne dans `offre_validation`** : c'est l'**historique**
(qui a validé, quand, avec quel salaire avant/après, quel commentaire). Le candidat
peut voir cet historique sur la page de détail d'une offre.

---

## 7. La base de données

### 7.1 Deux schémas

- **`src/main/resources/schema-h2.sql`** → utilisé en **dev** (H2, base en mémoire).
  Les clés auto-incrémentées y sont en `GENERATED BY DEFAULT AS IDENTITY`
  (compatible H2 en mode PostgreSQL).
- **`sql/schema.sql`** → schéma de **référence PostgreSQL** (identités en
  `GENERATED ALWAYS AS IDENTITY`), pour un déploiement en production.

Les deux contiennent **le même modèle** ; `schema-h2.sql` ajoute en plus les `INSERT`
des données de référence (rôles, décisions, statuts, etc.).

### 7.2 Catégories de tables

**A. Tables de référence** (remplies dans `schema-h2.sql`) :
`role`, `decision`, `statut_offre`, `etape_recrutement`, `statut_candidature`,
`resultat`, `type_question`, `reponse_condition`, `type_contrat`, `type_notification`.

**B. Tables cœur du workflow** (utilisées par le code Java) :

| Table | Entité Java | Contenu |
|-------|-------------|---------|
| `utilisateur` | `Utilisateur` | comptes (email, mot de passe BCrypt, rôle, département) |
| `departement` | `Departement` | services de l'entreprise |
| `poste` | `Poste` | catalogue des intitulés de postes |
| `demande_poste` | `DemandePoste` | demandes de nouveaux postes faites par le manager |
| `offre_emploi` | `OffreEmploi` | les offres (poste, places, salaire, statut) |
| `offre_validation` | `OffreValidation` | historique des validations |

**C. Tables « futures » / non encore exploitées par le code Java** :
`historique_poste`, `cv`, `candidature`, `candidature_etape`, `questionnaire`,
`question`, `choix_reponse`, `reponse_candidat`, `entretien`, `condition_travail`,
`contrat`, `notification`.
Le schéma est volontairement **plus large** que l'implémentation actuelle : ces tables
préparent les étapes suivantes du recrutement (dépôt de CV, questionnaire, entretiens,
conditions de travail, embauche).

### 7.3 Relations principales (workflow)

```
departement 1───* utilisateur *───1 role
manager (utilisateur) 1───* offre_emploi
poste 1───* offre_emploi
offre_emploi 1───* offre_validation
utilisateur(manager) 1───* demande_poste
demande_poste *───1 decision   (EN_ATTENTE / VALIDEE / REFUSEE)
```

---

## 8. Comptes de test

Mot de passe **unique** pour tous : `password` (hash BCrypt dans `data.sql`).

| Email | Rôle | Département | Page d'accueil après login |
|-------|------|-------------|-----------------------------|
| `manager@demo.com` | MANAGER | Informatique | `/manager/dashboard` |
| `rh@demo.com` | RH | Ressources Humaines | `/rh/dashboard` |
| `finance@demo.com` | FINANCE | Finance | `/finance/dashboard` |
| `direction@demo.com` | DIRECTION | Direction | `/direction/dashboard` |
| `candidat@demo.com` | CANDIDAT | — | `/candidat/offres` |

---

## 9. Détail fichier par fichier

> Pour chaque fichier : une explication, puis **son contenu intégral**.

### 9.1 `pom.xml`

**Rôle** : décrit le projet Maven.
- Parent `spring-boot-starter-parent` **3.3.5** ; Java **21**.
- **`<packaging>war</packaging>`** : nécessaire pour les JSP.
- Dépendances web/JPA/validation, `spring-security-crypto` (BCrypt).
- **JSP** : `tomcat-embed-jasper` (moteur de compilation des JSP), `jakarta.servlet-api`
  (`provided`), **JSTL 3.0** (`jakarta.servlet.jsp.jstl-api` + `org.glassfish.web:…jstl`).
- `spring-boot-starter-tomcat` en `provided` : Tomcat présent en dev, exclu du war pour
  un conteneur externe.
- Drivers **H2** (dev) et **PostgreSQL** (prod), starter de test.
- Plugin `spring-boot-maven-plugin` → war exécutable `project-0.0.1-SNAPSHOT.war`.

**Contenu du fichier :**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.3.5</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>com</groupId>
	<artifactId>project</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<packaging>war</packaging>
	<name>Recrutement</name>
	<description>Workflow recrutement Manager - RH - Finance - Direction - Candidat</description>
	<properties>
		<java.version>21</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-crypto</artifactId>
		</dependency>

		<!-- Vues JSP : Jasper (compilateur JSP de Tomcat) -->
		<dependency>
			<groupId>org.apache.tomcat.embed</groupId>
			<artifactId>tomcat-embed-jasper</artifactId>
		</dependency>
		<!-- Servlet API : fournie par le conteneur -->
		<dependency>
			<groupId>jakarta.servlet</groupId>
			<artifactId>jakarta.servlet-api</artifactId>
			<scope>provided</scope>
		</dependency>
		<!-- JSTL 3.0 (Jakarta EE 10) : API + implémentation -->
		<dependency>
			<groupId>jakarta.servlet.jsp.jstl</groupId>
			<artifactId>jakarta.servlet.jsp.jstl-api</artifactId>
			<version>3.0.0</version>
		</dependency>
		<dependency>
			<groupId>org.glassfish.web</groupId>
			<artifactId>jakarta.servlet.jsp.jstl</artifactId>
			<version>3.0.1</version>
		</dependency>
		<!-- Tomcat embarqué pour le dev ; exclu du war (fourni par le conteneur en prod) -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-tomcat</artifactId>
			<scope>provided</scope>
		</dependency>

		<!-- DB -->
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>
```

---

### 9.2 `deploy.sh`

**Rôle** : script tout-en-un de build/lancement.

Points importants :
- `set -euo pipefail` : arrêt immédiat en cas d'erreur.
- `cd "$(dirname "$0")"` : fonctionne quel que soit le dossier d'appel.
- **Forçage de Java 21** : `is_java21()` vérifie si `JAVA_HOME` pointe bien vers un JDK 21 ;
  sinon `find_java21()` cherche un JDK 21 et le définit.
- Variables/texte adaptés au **WAR** (`target/project-0.0.1-SNAPSHOT.war`).
- Modes : `run` (défaut, `spring-boot:run`), `build` (`clean package -DskipTests`),
  `war` (build puis `java -jar`), `test`, `clean`, `help`.

**Contenu du fichier :**
```bash
#!/usr/bin/env bash
#
# deploy.sh - Compiler et lancer le projet Recrutement
#
# Usage:
#   ./deploy.sh            -> compile + lance en dev (spring-boot:run, H2 en memoire)
#   ./deploy.sh build      -> compile + construit le war executable (skip tests)
#   ./deploy.sh war        -> build puis lance le war package
#   ./deploy.sh test       -> lance les tests
#   ./deploy.sh clean      -> nettoie target/
#   ./deploy.sh help       -> affiche cette aide
#
set -euo pipefail
cd "$(dirname "$0")"

APP="target/project-0.0.1-SNAPSHOT.war"

# ---------------------------------------------------------------------------
# Le projet a besoin de Java 21 (switch expressions). Or JAVA_HOME peut pointer
# sur Java 8 sur cette machine : on force donc un JDK 21.
# ---------------------------------------------------------------------------
find_java21() {
  local candidates=("/usr/lib/jvm/java-21-openjdk-amd64" /usr/lib/jvm/java-21-* \
                     /usr/lib/jvm/temurin-21* /opt/java-21* /opt/jdk-21*)
  local c
  for c in "${candidates[@]}"; do
    if [ -x "$c/bin/java" ]; then echo "$c"; return 0; fi
  done
  return 1
}

is_java21() {
  [ -n "${1:-}" ] && [ -x "$1/bin/java" ] && "$1/bin/java" -version 2>&1 | grep -q 'version "21'
}

if ! is_java21 "${JAVA_HOME:-}"; then
  if JDK21="$(find_java21)"; then
    export JAVA_HOME="$JDK21"
  else
    echo "ERREUR: JDK 21 introuvable. Installe-le ou definis JAVA_HOME." >&2
    exit 1
  fi
fi
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> Java : $(java -version 2>&1 | head -1)"
echo "==> JAVA_HOME : ${JAVA_HOME:-'(defaut)'}"

run_war() {
  build
  echo "==> Lancement du war : $APP"
  exec java -jar "$APP"
}

build() {
  echo "==> Compilation + packaging (tests ignores)"
  ./mvnw -q clean package -DskipTests
  echo "==> War genere : $APP"
}

case "${1:-run}" in
  run)
    echo "==> Compilation + lancement en dev (spring-boot:run)"
    exec ./mvnw spring-boot:run
    ;;
  build)
    build
    ;;
  war)
    run_war
    ;;
  test)
    echo "==> Tests"
    ./mvnw test
    ;;
  clean)
    echo "==> Nettoyage"
    ./mvnw clean
    ;;
  help|-h|--help)
    sed -n '2,12p' "$0"
    ;;
  *)
    echo "Commande inconnue : $1" >&2
    sed -n '2,12p' "$0" >&2
    exit 1
    ;;
esac
```

---

### 9.3 `src/main/resources/application.properties`

**Rôle** : configuration Spring Boot.
- `spring.mvc.view.prefix=/WEB-INF/jsp/` + `spring.mvc.view.suffix=.jsp` : résolution
  des vues JSP.
- `spring.datasource.*` : **H2 en mémoire** (`jdbc:h2:mem:recrutement`) en `MODE=PostgreSQL`.
- `spring.jpa.hibernate.ddl-auto=none` : Hibernate ne crée pas les tables ; le script SQL
  s'en charge.
- `spring.sql.init.*` : exécute `schema-h2.sql` puis `data.sql` au démarrage.
- `spring.h2.console.enabled=true` : console web H2 sur `/h2-console`.

**Contenu du fichier :**
```properties
spring.application.name=Recrutement
server.port=8080

# Vues JSP (prefixe + suffixe -> le controleur renvoie "manager/dashboard")
# => /WEB-INF/jsp/manager/dashboard.jsp
spring.mvc.view.prefix=/WEB-INF/jsp/
spring.mvc.view.suffix=.jsp

# H2 en memoire (dev) - facile a lancer sans Postgres
spring.datasource.url=jdbc:h2:mem:recrutement;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# initialisation: schema.sql + data.sql
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema-h2.sql
spring.sql.init.data-locations=classpath:data.sql
spring.sql.init.continue-on-error=false

# H2 console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

---

### 9.4 `src/main/resources/schema-h2.sql`

**Rôle** : crée **toutes les tables** et insère les **données de référence** (rôles,
décisions, statuts…). C'est le schéma effectivement utilisé en dev.

À noter :
- `GENERATED BY DEFAULT AS IDENTITY` (auto-incrément H2).
- Les tables `candidature`, `cv`, `questionnaire`, `entretien`, `contrat`… existent déjà
  mais ne sont pas encore utilisées par le code Java.

**Contenu du fichier :**
```sql
CREATE TABLE departement (
    id   INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    nom  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE role (
    id       INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE decision (
    id       INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE statut_offre (
    id       INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

CREATE TABLE etape_recrutement (
    id       INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL,
    ordre    INT NOT NULL UNIQUE
);

CREATE TABLE statut_candidature (
    id       INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

CREATE TABLE resultat (
    id       INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE type_question (
    id       INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE reponse_condition (
    id       INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE type_contrat (
    id       INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

CREATE TABLE type_notification (
    id       INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

INSERT INTO role (code, libelle) VALUES
    ('MANAGER',   'Manager'),
    ('RH',        'Ressources humaines'),
    ('FINANCE',   'Bureau de finances'),
    ('DIRECTION', 'Direction'),
    ('CANDIDAT',  'Candidat');

INSERT INTO decision (code, libelle) VALUES
    ('EN_ATTENTE', 'En attente'),
    ('VALIDEE',    'Validee'),
    ('REFUSEE',    'Refusee');

INSERT INTO statut_offre (code, libelle) VALUES
    ('EN_ATTENTE_RH',        'En attente de validation RH'),
    ('REFUSEE_RH',           'Refusee par le RH'),
    ('EN_ATTENTE_FINANCE',   'En attente de validation Finance'),
    ('REFUSEE_FINANCE',      'Refusee par la Finance'),
    ('EN_ATTENTE_DIRECTION', 'En attente de validation Direction'),
    ('REFUSEE_DIRECTION',    'Refusee par la Direction'),
    ('PUBLIEE',              'Publiee'),
    ('CLOTUREE',             'Cloturee');

INSERT INTO etape_recrutement (code, libelle, ordre) VALUES
    ('CV',            'Selection des CV',  1),
    ('QUESTIONNAIRE', 'Questionnaire',     2),
    ('ENTRETIEN',     'Entretien',         3),
    ('CONDITIONS',    'Conditions de travail', 4),
    ('EMBAUCHE',      'Embauche',          5);

INSERT INTO statut_candidature (code, libelle) VALUES
    ('EN_COURS',            'En cours'),
    ('ELIMINE',             'Elimine'),
    ('REFUSE_PAR_CANDIDAT', 'Refuse par le candidat'),
    ('EMBAUCHE',            'Embauche');

INSERT INTO resultat (code, libelle) VALUES
    ('REUSSI', 'Reussi'),
    ('ECHOUE', 'Echoue');

INSERT INTO type_question (code, libelle) VALUES
    ('QCM',   'Choix multiple'),
    ('TEXTE', 'Reponse libre');

INSERT INTO reponse_condition (code, libelle) VALUES
    ('OUI',   'Accepte'),
    ('NON',   'Refuse'),
    ('AUTRE', 'Autre remarque / modification');

INSERT INTO type_contrat (code, libelle) VALUES
    ('CE',  'Contrat d''essai'),
    ('CDI', 'Contrat a duree indeterminee');

INSERT INTO type_notification (code, libelle) VALUES
    ('SELECTION_CV', 'Selection du CV'),
    ('QUESTIONNAIRE', 'Questionnaire'),
    ('ENTRETIEN',    'Entretien'),
    ('CLASSEMENT',   'Classement'),
    ('CONDITIONS',   'Conditions de travail'),
    ('OFFRE',        'Offre d''emploi'),
    ('INFO',         'Information');

CREATE TABLE utilisateur (
    id              INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe    VARCHAR(255) NOT NULL,
    id_role         INT NOT NULL REFERENCES role(id),
    id_departement  INT REFERENCES departement(id),
    actif           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE poste (
    id          INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    intitule    VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    actif       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE historique_poste (
    id               INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_poste         INT NOT NULL REFERENCES poste(id),
    id_utilisateur   INT NOT NULL REFERENCES utilisateur(id),
    actif            BOOLEAN NOT NULL,
    date_modification TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE demande_poste (
    id               INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_manager       INT NOT NULL REFERENCES utilisateur(id),
    id_departement   INT NOT NULL REFERENCES departement(id),
    intitule_propose VARCHAR(150) NOT NULL,
    justification    TEXT,
    id_decision      INT NOT NULL DEFAULT 1 REFERENCES decision(id),
    id_rh            INT REFERENCES utilisateur(id),
    motif_refus      TEXT,
    id_poste         INT REFERENCES poste(id),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at       TIMESTAMP
);

CREATE TABLE offre_emploi (
    id                  INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_poste            INT NOT NULL REFERENCES poste(id),
    id_departement      INT NOT NULL REFERENCES departement(id),
    id_manager          INT NOT NULL REFERENCES utilisateur(id),
    annees_experience   INT NOT NULL DEFAULT 0 CHECK (annees_experience >= 0),
    nb_places           INT NOT NULL CHECK (nb_places > 0),
    salaire             NUMERIC(12,2) CHECK (salaire IS NULL OR salaire >= 0),
    id_statut           INT NOT NULL DEFAULT 1 REFERENCES statut_offre(id),
    date_publication    TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE offre_validation (
    id                 INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_offre           INT NOT NULL REFERENCES offre_emploi(id) ON DELETE CASCADE,
    id_role_valideur   INT NOT NULL REFERENCES role(id),
    id_valideur        INT NOT NULL REFERENCES utilisateur(id),
    id_decision        INT NOT NULL REFERENCES decision(id),
    salaire_avant      NUMERIC(12,2),
    salaire_apres      NUMERIC(12,2),
    commentaire        TEXT,
    decided_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tables complementaires (candidature etc) simplifiees pour H2
CREATE TABLE cv (
    id              INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_candidat     INT NOT NULL REFERENCES utilisateur(id),
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    date_naissance  DATE NOT NULL,
    diplome         TEXT NOT NULL,
    competences     TEXT NOT NULL,
    hobby           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE candidature (
    id                    INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_offre              INT NOT NULL REFERENCES offre_emploi(id),
    id_candidat           INT NOT NULL REFERENCES utilisateur(id),
    id_cv                 INT NOT NULL REFERENCES cv(id),
    id_etape              INT NOT NULL DEFAULT 1 REFERENCES etape_recrutement(id),
    id_statut             INT NOT NULL DEFAULT 1 REFERENCES statut_candidature(id),
    score_questionnaire   NUMERIC(6,2),
    cdi_autorise          BOOLEAN NOT NULL DEFAULT FALSE,
    id_rh_cdi             INT REFERENCES utilisateur(id),
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_offre, id_candidat)
);

CREATE TABLE candidature_etape (
    id               INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_candidature   INT NOT NULL REFERENCES candidature(id) ON DELETE CASCADE,
    id_etape         INT NOT NULL REFERENCES etape_recrutement(id),
    id_resultat      INT NOT NULL REFERENCES resultat(id),
    id_decideur      INT REFERENCES utilisateur(id),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE questionnaire (
    id              INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_offre        INT NOT NULL UNIQUE REFERENCES offre_emploi(id) ON DELETE CASCADE,
    titre           VARCHAR(150) NOT NULL,
    seuil_reussite  NUMERIC(6,2) NOT NULL,
    id_createur     INT NOT NULL REFERENCES utilisateur(id)
);

CREATE TABLE question (
    id                INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_questionnaire  INT NOT NULL REFERENCES questionnaire(id) ON DELETE CASCADE,
    id_type           INT NOT NULL DEFAULT 1 REFERENCES type_question(id),
    enonce            TEXT NOT NULL,
    points            NUMERIC(5,2) NOT NULL DEFAULT 1,
    ordre             INT NOT NULL DEFAULT 1
);

CREATE TABLE choix_reponse (
    id           INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_question  INT NOT NULL REFERENCES question(id) ON DELETE CASCADE,
    libelle      TEXT NOT NULL,
    est_correct  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE reponse_candidat (
    id               INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_candidature   INT NOT NULL REFERENCES candidature(id) ON DELETE CASCADE,
    id_question      INT NOT NULL REFERENCES question(id),
    id_choix         INT REFERENCES choix_reponse(id),
    reponse_texte    TEXT,
    points_obtenus   NUMERIC(5,2),
    UNIQUE (id_candidature, id_question)
);

CREATE TABLE entretien (
    id                  INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_candidature      INT NOT NULL UNIQUE REFERENCES candidature(id) ON DELETE CASCADE,
    id_rh               INT NOT NULL REFERENCES utilisateur(id),
    date_entretien      TIMESTAMP,
    note_honnetete_cv   NUMERIC(4,2) NOT NULL CHECK (note_honnetete_cv BETWEEN 0 AND 20),
    note_personnalite   NUMERIC(4,2) NOT NULL CHECK (note_personnalite BETWEEN 0 AND 20),
    note_competences    NUMERIC(4,2) NOT NULL CHECK (note_competences BETWEEN 0 AND 20),
    note_totale         NUMERIC(5,2)
);

CREATE TABLE condition_travail (
    id                   INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_candidature       INT NOT NULL REFERENCES candidature(id) ON DELETE CASCADE,
    version              INT NOT NULL DEFAULT 1,
    id_role_proposant    INT NOT NULL REFERENCES role(id),
    salaire              NUMERIC(12,2) NOT NULL CHECK (salaire >= 0),
    heures_par_semaine   NUMERIC(4,1) NOT NULL CHECK (heures_par_semaine > 0),
    horaires             VARCHAR(100),
    lieu_travail         VARCHAR(255) NOT NULL,
    id_reponse           INT REFERENCES reponse_condition(id),
    remarque             TEXT,
    id_decision_rh       INT NOT NULL DEFAULT 1 REFERENCES decision(id),
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_candidature, version)
);

CREATE TABLE contrat (
    id                INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_candidature    INT NOT NULL UNIQUE REFERENCES candidature(id),
    id_condition      INT NOT NULL REFERENCES condition_travail(id),
    id_type_contrat   INT NOT NULL DEFAULT 1 REFERENCES type_contrat(id),
    date_debut        DATE,
    date_fin          DATE,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification (
    id                    INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_destinataire       INT NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    id_type_notification  INT NOT NULL REFERENCES type_notification(id),
    titre                 VARCHAR(150) NOT NULL,
    message               TEXT NOT NULL,
    id_candidature        INT REFERENCES candidature(id) ON DELETE SET NULL,
    id_offre              INT REFERENCES offre_emploi(id) ON DELETE SET NULL,
    lue                   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

### 9.5 `src/main/resources/data.sql`

**Rôle** : insère les **données de test** : départements, postes du catalogue, et les
5 comptes utilisateurs (un par rôle).

Le mot de passe des comptes est le hash BCrypt de `password` :
`$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG`.
Les `id_role`/`id_departement` sont résolus par des sous-requêtes sur le code/le nom.

**Contenu du fichier :**
```sql
INSERT INTO departement (nom) VALUES ('Informatique'), ('Ressources Humaines'), ('Finance'), ('Direction'), ('Marketing');
INSERT INTO poste (intitule, description, actif) VALUES
 ('Developpeur Junior', 'Developpeur debutant Java/Spring', true),
 ('Developpeur Senior', 'Developpeur experimente Java/Spring', true),
 ('Chef de projet', 'Gestion de projets IT', true),
 ('Comptable', 'Gestion comptable et finance', true),
 ('Designer UI/UX', 'Conception interfaces utilisateur', true);

-- mot de passe = "password"  BCrypt $2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG
INSERT INTO utilisateur (email, mot_de_passe, id_role, id_departement) VALUES
 ('manager@demo.com',   '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='MANAGER'), (SELECT id FROM departement WHERE nom='Informatique')),
 ('rh@demo.com',        '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='RH'), (SELECT id FROM departement WHERE nom='Ressources Humaines')),
 ('finance@demo.com',   '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='FINANCE'), (SELECT id FROM departement WHERE nom='Finance')),
 ('direction@demo.com', '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='DIRECTION'), (SELECT id FROM departement WHERE nom='Direction')),
 ('candidat@demo.com',  '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='CANDIDAT'), NULL);
```

---

### 9.6 `sql/schema.sql`

**Rôle** : schéma de **référence pour PostgreSQL** (production), identique au modèle de
`schema-h2.sql` mais avec `GENERATED ALWAYS AS IDENTITY`.

> `sql/19-10-26.sql` et `sql/19/10/26.sql` sont des **copies** de ce fichier ; non
> utilisés par l'application.

**Contenu du fichier :**
```sql
CREATE TABLE departement (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE role (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

-- Decision d'un valideur (demande de poste, offre, conditions de travail)
CREATE TABLE decision (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE statut_offre (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

CREATE TABLE etape_recrutement (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL,
    ordre    INT NOT NULL UNIQUE
);

CREATE TABLE statut_candidature (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

CREATE TABLE resultat (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE type_question (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

-- Reponse du candidat a la proposition de conditions de travail
CREATE TABLE reponse_condition (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(50) NOT NULL
);

CREATE TABLE type_contrat (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(20) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

CREATE TABLE type_notification (
    id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code     VARCHAR(30) NOT NULL UNIQUE,
    libelle  VARCHAR(80) NOT NULL
);

-- ---- Donnees de reference ----
INSERT INTO role (code, libelle) VALUES
    ('MANAGER',   'Manager'),
    ('RH',        'Ressources humaines'),
    ('FINANCE',   'Bureau de finances'),
    ('DIRECTION', 'Direction'),
    ('CANDIDAT',  'Candidat');

INSERT INTO decision (code, libelle) VALUES
    ('EN_ATTENTE', 'En attente'),
    ('VALIDEE',    'Validee'),
    ('REFUSEE',    'Refusee');

INSERT INTO statut_offre (code, libelle) VALUES
    ('EN_ATTENTE_RH',        'En attente de validation RH'),
    ('REFUSEE_RH',           'Refusee par le RH'),
    ('EN_ATTENTE_FINANCE',   'En attente de validation Finance'),
    ('REFUSEE_FINANCE',      'Refusee par la Finance'),
    ('EN_ATTENTE_DIRECTION', 'En attente de validation Direction'),
    ('REFUSEE_DIRECTION',    'Refusee par la Direction'),
    ('PUBLIEE',              'Publiee'),
    ('CLOTUREE',             'Cloturee');

INSERT INTO etape_recrutement (code, libelle, ordre) VALUES
    ('CV',            'Selection des CV',  1),
    ('QUESTIONNAIRE', 'Questionnaire',     2),
    ('ENTRETIEN',     'Entretien',         3),
    ('CONDITIONS',    'Conditions de travail', 4),
    ('EMBAUCHE',      'Embauche',          5);

INSERT INTO statut_candidature (code, libelle) VALUES
    ('EN_COURS',            'En cours'),
    ('ELIMINE',             'Elimine'),
    ('REFUSE_PAR_CANDIDAT', 'Refuse par le candidat'),
    ('EMBAUCHE',            'Embauche');

INSERT INTO resultat (code, libelle) VALUES
    ('REUSSI', 'Reussi'),
    ('ECHOUE', 'Echoue');

INSERT INTO type_question (code, libelle) VALUES
    ('QCM',   'Choix multiple'),
    ('TEXTE', 'Reponse libre');

INSERT INTO reponse_condition (code, libelle) VALUES
    ('OUI',   'Accepte'),
    ('NON',   'Refuse'),
    ('AUTRE', 'Autre remarque / modification');

INSERT INTO type_contrat (code, libelle) VALUES
    ('CE',  'Contrat d''essai'),
    ('CDI', 'Contrat a duree indeterminee');

INSERT INTO type_notification (code, libelle) VALUES
    ('SELECTION_CV', 'Selection du CV'),
    ('QUESTIONNAIRE', 'Questionnaire'),
    ('ENTRETIEN',    'Entretien'),
    ('CLASSEMENT',   'Classement'),
    ('CONDITIONS',   'Conditions de travail'),
    ('OFFRE',        'Offre d''emploi'),
    ('INFO',         'Information');

-- ---------------------------------------------------------------------
-- UTILISATEURS
-- ---------------------------------------------------------------------
CREATE TABLE utilisateur (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe    VARCHAR(255) NOT NULL,              -- hash (bcrypt/argon2)
    id_role         INT NOT NULL REFERENCES role(id),
    id_departement  INT REFERENCES departement(id),
    actif           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- POSTES ET DEMANDES DE NOUVEAUX POSTES
-- ---------------------------------------------------------------------

-- Catalogue des postes (contenu du dropdown du manager)
CREATE TABLE poste (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    intitule    VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    actif       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE TABLE historique_poste (
    id               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_poste         INT NOT NULL REFERENCES poste(id),
    id_utilisateur   INT NOT NULL REFERENCES utilisateur(id),
    actif            BOOLEAN NOT NULL,
    date_modification TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Demande du manager pour un poste pas présent encore
CREATE TABLE demande_poste (
    id               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_manager       INT NOT NULL REFERENCES utilisateur(id),
    id_departement   INT NOT NULL REFERENCES departement(id),
    intitule_propose VARCHAR(150) NOT NULL,
    justification    TEXT,
    id_decision      INT NOT NULL DEFAULT 1 REFERENCES decision(id),   -- 1 = EN_ATTENTE
    id_rh            INT REFERENCES utilisateur(id),
    motif_refus      TEXT,
    id_poste         INT REFERENCES poste(id),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    decided_at       TIMESTAMP
);

-- ---------------------------------------------------------------------
--  OFFRES D'EMPLOI ET CIRCUIT DE VALIDATION
-- ---------------------------------------------------------------------

CREATE TABLE offre_emploi (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_poste            INT NOT NULL REFERENCES poste(id),
    id_departement      INT NOT NULL REFERENCES departement(id),
    id_manager          INT NOT NULL REFERENCES utilisateur(id),
    annees_experience   INT NOT NULL DEFAULT 0 CHECK (annees_experience >= 0),
    nb_places           INT NOT NULL CHECK (nb_places > 0),
    salaire             NUMERIC(12,2) CHECK (salaire IS NULL OR salaire >= 0),  -- NULL tant que le RH ne l'a pas fixe
    id_statut           INT NOT NULL DEFAULT 1 REFERENCES statut_offre(id),     -- 1 = EN_ATTENTE_RH
    date_publication    TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Historique de chaque decision. id_role_valideur = RH, FINANCE ou DIRECTION.
CREATE TABLE offre_validation (
    id                 INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_offre           INT NOT NULL REFERENCES offre_emploi(id) ON DELETE CASCADE,
    id_role_valideur   INT NOT NULL REFERENCES role(id),
    id_valideur        INT NOT NULL REFERENCES utilisateur(id),
    id_decision        INT NOT NULL REFERENCES decision(id),
    salaire_avant      NUMERIC(12,2),
    salaire_apres      NUMERIC(12,2),                   -- different si la Finance modifie le salaire
    commentaire        TEXT,
    decided_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- CANDIDATURES
-- ---------------------------------------------------------------------

CREATE TABLE cv (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidat     INT NOT NULL REFERENCES utilisateur(id),
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    date_naissance  DATE NOT NULL,
    diplome         TEXT NOT NULL,
    competences     TEXT NOT NULL,
    hobby           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE candidature (
    id                    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_offre              INT NOT NULL REFERENCES offre_emploi(id),
    id_candidat           INT NOT NULL REFERENCES utilisateur(id),
    id_cv                 INT NOT NULL REFERENCES cv(id),
    id_etape              INT NOT NULL DEFAULT 1 REFERENCES etape_recrutement(id),   -- 1 = CV
    id_statut             INT NOT NULL DEFAULT 1 REFERENCES statut_candidature(id),  -- 1 = EN_COURS
    score_questionnaire   NUMERIC(6,2),
    cdi_autorise          BOOLEAN NOT NULL DEFAULT FALSE,   -- accreditation donnee par le RH
    id_rh_cdi             INT REFERENCES utilisateur(id),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (id_offre, id_candidat)
);

-- Historique du passage de chaque etape
CREATE TABLE candidature_etape (
    id               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidature   INT NOT NULL REFERENCES candidature(id) ON DELETE CASCADE,
    id_etape         INT NOT NULL REFERENCES etape_recrutement(id),
    id_resultat      INT NOT NULL REFERENCES resultat(id),
    id_decideur      INT REFERENCES utilisateur(id),        -- NULL si automatique
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- QUESTIONNAIRES
-- ---------------------------------------------------------------------

CREATE TABLE questionnaire (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_offre        INT NOT NULL UNIQUE REFERENCES offre_emploi(id) ON DELETE CASCADE,
    titre           VARCHAR(150) NOT NULL,
    seuil_reussite  NUMERIC(6,2) NOT NULL,
    id_createur     INT NOT NULL REFERENCES utilisateur(id)
);

CREATE TABLE question (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_questionnaire  INT NOT NULL REFERENCES questionnaire(id) ON DELETE CASCADE,
    id_type           INT NOT NULL DEFAULT 1 REFERENCES type_question(id),   -- 1 = QCM
    enonce            TEXT NOT NULL,
    points            NUMERIC(5,2) NOT NULL DEFAULT 1,
    ordre             INT NOT NULL DEFAULT 1
);

CREATE TABLE choix_reponse (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_question  INT NOT NULL REFERENCES question(id) ON DELETE CASCADE,
    libelle      TEXT NOT NULL,
    est_correct  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE reponse_candidat (
    id               INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidature   INT NOT NULL REFERENCES candidature(id) ON DELETE CASCADE,
    id_question      INT NOT NULL REFERENCES question(id),
    id_choix         INT REFERENCES choix_reponse(id),
    reponse_texte    TEXT,
    points_obtenus   NUMERIC(5,2),
    UNIQUE (id_candidature, id_question)
);

-- ---------------------------------------------------------------------
-- ENTRETIEN ET CLASSEMENT
-- ---------------------------------------------------------------------

CREATE TABLE entretien (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidature      INT NOT NULL UNIQUE REFERENCES candidature(id) ON DELETE CASCADE,
    id_rh               INT NOT NULL REFERENCES utilisateur(id),
    date_entretien      TIMESTAMP,
    note_honnetete_cv   NUMERIC(4,2) NOT NULL CHECK (note_honnetete_cv BETWEEN 0 AND 20),
    note_personnalite   NUMERIC(4,2) NOT NULL CHECK (note_personnalite BETWEEN 0 AND 20),
    note_competences    NUMERIC(4,2) NOT NULL CHECK (note_competences BETWEEN 0 AND 20),
    note_totale         NUMERIC(5,2) GENERATED ALWAYS AS
                        (note_honnetete_cv + note_personnalite + note_competences) STORED
);

-- Rang durant anle entretien
CREATE VIEW v_classement AS
SELECT t.*,
       (t.rang <= t.nb_places) AS retenu
FROM (
    SELECT c.id_offre,
           c.id AS id_candidature,
           e.note_totale,
           o.nb_places,
           RANK() OVER (PARTITION BY c.id_offre ORDER BY e.note_totale DESC) AS rang
    FROM candidature c
    JOIN statut_candidature s ON s.id = c.id_statut AND s.code = 'EN_COURS'
    JOIN entretien e          ON e.id_candidature = c.id
    JOIN offre_emploi o       ON o.id = c.id_offre
) t;

-- ---------------------------------------------------------------------
-- CONDITIONS DE TRAVAIL ET CONTRAT
-- ---------------------------------------------------------------------

CREATE TABLE condition_travail (
    id                   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidature       INT NOT NULL REFERENCES candidature(id) ON DELETE CASCADE,
    version              INT NOT NULL DEFAULT 1,
    id_role_proposant    INT NOT NULL REFERENCES role(id),          -- RH ou CANDIDAT
    salaire              NUMERIC(12,2) NOT NULL CHECK (salaire >= 0),
    heures_par_semaine   NUMERIC(4,1) NOT NULL CHECK (heures_par_semaine > 0),
    horaires             VARCHAR(100),
    lieu_travail         VARCHAR(255) NOT NULL,
    id_reponse           INT REFERENCES reponse_condition(id),      -- NULL tant que le candidat n'a pas repondu
    remarque             TEXT,
    id_decision_rh       INT NOT NULL DEFAULT 1 REFERENCES decision(id),
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (id_candidature, version)
);

CREATE TABLE contrat (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_candidature    INT NOT NULL UNIQUE REFERENCES candidature(id),
    id_condition      INT NOT NULL REFERENCES condition_travail(id),
    id_type_contrat   INT NOT NULL DEFAULT 1 REFERENCES type_contrat(id),   -- 1 = CE
    date_debut        DATE,
    date_fin          DATE,                              -- NULL pour un CDI
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- NOTIFICATIONS (boite de reception)
-- ---------------------------------------------------------------------

CREATE TABLE notification (
    id                    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_destinataire       INT NOT NULL REFERENCES utilisateur(id) ON DELETE CASCADE,
    id_type_notification  INT NOT NULL REFERENCES type_notification(id),
    titre                 VARCHAR(150) NOT NULL,
    message               TEXT NOT NULL,
    id_candidature        INT REFERENCES candidature(id) ON DELETE SET NULL,
    id_offre              INT REFERENCES offre_emploi(id) ON DELETE SET NULL,
    lue                   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW()
);
-- ===========================================
-- Donnees ajoutees le 19/10/26 (run du projet)
-- ===========================================
INSERT INTO departement (nom) VALUES ('Informatique'), ('Ressources Humaines'), ('Finance'), ('Direction'), ('Marketing');
INSERT INTO poste (intitule, description, actif) VALUES
 ('Developpeur Junior', 'Developpeur debutant Java/Spring', true),
 ('Developpeur Senior', 'Developpeur experimente Java/Spring', true),
 ('Chef de projet', 'Gestion de projets IT', true),
 ('Comptable', 'Gestion comptable et finance', true),
 ('Designer UI/UX', 'Conception interfaces utilisateur', true);

-- mot de passe = "password"  BCrypt $2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG
INSERT INTO utilisateur (email, mot_de_passe, id_role, id_departement) VALUES
 ('manager@demo.com',   '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='MANAGER'), (SELECT id FROM departement WHERE nom='Informatique')),
 ('rh@demo.com',        '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='RH'), (SELECT id FROM departement WHERE nom='Ressources Humaines')),
 ('finance@demo.com',   '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='FINANCE'), (SELECT id FROM departement WHERE nom='Finance')),
 ('direction@demo.com', '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='DIRECTION'), (SELECT id FROM departement WHERE nom='Direction')),
 ('candidat@demo.com',  '$2a$10$gq35vcShpyR0dTQy2UiLkOynCfOPFfkfGTxsoOdMsOWHn0SMuBuKG', (SELECT id FROM role WHERE code='CANDIDAT'), NULL);
```

---

### 9.7 `ProjectApplication.java`

**Rôle** : **point d'entrée**. `@SpringBootApplication` active l'auto-configuration,
le scan des composants et la config. `main()` lance l'application.

**Contenu du fichier :**
```java
package com.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectApplication.class, args);
	}

}
```

---

### 9.8 `ServletInitializer.java`

**Rôle** : requis pour le **packaging WAR**. Étend `SpringBootServletInitializer` et
pointe vers `ProjectApplication`. Permet de déployer le war dans un Tomcat externe
(en plus du `java -jar` qui utilise le Tomcat embarqué).

**Contenu du fichier :**
```java
package com.project;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Necessaire pour le packaging WAR : permet de deployer l'application dans un
 * conteneur de servlets externe (Tomcat, etc.) en plus du lancement embarque.
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ProjectApplication.class);
    }
}
```

---

### 9.9 `config/AuthInterceptor.java`

**Rôle** : **garde d'accès**. Avant chaque requête protégée :
1. session + `userId` présents, sinon → `/login` ;
2. rôle cohérent avec le préfixe d'URL (`/manager/**`, `/rh/**`, …), sinon → `/login?error=role`.

**Contenu du fichier :**
```java
package com.project.config;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component
public class AuthInterceptor implements HandlerInterceptor {
  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
    HttpSession session = req.getSession(false);
    if(session==null || session.getAttribute("userId")==null){
      res.sendRedirect("/login"); return false;
    }
    String role = (String) session.getAttribute("userRole");
    String path = req.getRequestURI();
    // controle role par prefixe
    if(path.startsWith("/manager") && !"MANAGER".equals(role)) { res.sendRedirect("/login?error=role"); return false; }
    if(path.startsWith("/rh") && !"RH".equals(role)) { res.sendRedirect("/login?error=role"); return false; }
    if(path.startsWith("/finance") && !"FINANCE".equals(role)) { res.sendRedirect("/login?error=role"); return false; }
    if(path.startsWith("/direction") && !"DIRECTION".equals(role)) { res.sendRedirect("/login?error=role"); return false; }
    if(path.startsWith("/candidat") && !"CANDIDAT".equals(role)) { res.sendRedirect("/login?error=role"); return false; }
    return true;
  }
}
```

---

### 9.10 `config/WebConfig.java`

**Rôle** : **enregistre l'interceptor** sur les préfixes de chaque rôle, en excluant
`/login`, `/logout`, `/css/**` et `/h2-console/**`.

**Contenu du fichier :**
```java
package com.project.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AuthInterceptor authInterceptor;
  public WebConfig(AuthInterceptor a){this.authInterceptor=a;}
  @Override
  public void addInterceptors(InterceptorRegistry registry){
    registry.addInterceptor(authInterceptor)
      .addPathPatterns("/manager/**","/rh/**","/finance/**","/direction/**","/candidat/**")
      .excludePathPatterns("/login","/logout","/css/**","/h2-console/**");
  }
}
```

---

### 9.11 `web/FormatUtils.java` + `WEB-INF/tld/functions.tld`

**Rôle** : **formatage des dates** dans les JSP. JSTL `<fmt:formatDate>` ne gère pas les
`LocalDateTime` ; on expose donc une **fonction EL** `f:dateTime(...)` (et `f:date(...)`)
via une taglib personnalisée.

- `FormatUtils` : méthodes statiques `dateTime(LocalDateTime)` → `dd/MM/yyyy HH:mm` et
  `date(LocalDateTime)` → `dd/MM/yyyy` (renvoient `-` si null).
- `functions.tld` : déclare ces fonctions, référencées dans les JSP par
  `<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>` puis `${f:dateTime(o.createdAt)}`.

**Contenu de `FormatUtils.java` :**
```java
package com.project.web;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Fonctions utilitaires exposees aux vues JSP via la taglib
 * {@code /WEB-INF/tld/functions.tld} (prefixe {@code f} dans les JSP).
 *
 * Exemple d'utilisation : ${f:dateTime(offre.createdAt)}
 */
public final class FormatUtils {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FormatUtils() {
        // classe utilitaire : pas d'instanciation
    }

    /** Formate un LocalDateTime en "dd/MM/yyyy HH:mm" (ou "-" si null). */
    public static String dateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME.format(value);
    }

    /** Formate un LocalDateTime en "dd/MM/yyyy" (ou "-" si null). */
    public static String date(LocalDateTime value) {
        return value == null ? "-" : DATE.format(value);
    }
}
```

**Contenu de `functions.tld` :**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<taglib xmlns="https://jakarta.ee/xml/ns/jakartaee"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-jsptaglibrary_3_0.xsd"
        version="3.0">
  <tlib-version>1.0</tlib-version>
  <short-name>f</short-name>
  <uri>http://project.com/functions</uri>

  <function>
    <name>dateTime</name>
    <function-class>com.project.web.FormatUtils</function-class>
    <function-signature>java.lang.String dateTime(java.time.LocalDateTime)</function-signature>
  </function>

  <function>
    <name>date</name>
    <function-class>com.project.web.FormatUtils</function-class>
    <function-signature>java.lang.String date(java.time.LocalDateTime)</function-signature>
  </function>
</taglib>
```

---

### 9.12 Les entités JPA (`entity/`)

Une **entité** = une classe Java mappée à une table SQL (`@Entity`, `@Table`, `@Id`,
`@GeneratedValue(IDENTITY)`, `@ManyToOne`/`@JoinColumn`). Les colonnes en snake_case sont
mappées via `@Column(name=...)`.

#### `entity/Role.java`
Table `role` : `MANAGER`, `RH`, `FINANCE`, `DIRECTION`, `CANDIDAT`.

**Contenu du fichier :**
```java
package com.project.entity;
import jakarta.persistence.*;
@Entity @Table(name="role")
public class Role {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 private String code; private String libelle;
 public Role(){} public Role(String code,String libelle){this.code=code;this.libelle=libelle;}
 public Integer getId(){return id;} public void setId(Integer id){this.id=id;}
 public String getCode(){return code;} public void setCode(String c){this.code=c;}
 public String getLibelle(){return libelle;} public void setLibelle(String l){this.libelle=l;}
}
```

#### `entity/Decision.java`
Table `decision` : `EN_ATTENTE`, `VALIDEE`, `REFUSEE`.

**Contenu du fichier :**
```java
package com.project.entity;
import jakarta.persistence.*;
@Entity @Table(name="decision")
public class Decision {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 private String code; private String libelle;
 public Decision(){} public Integer getId(){return id;} public void setId(Integer id){this.id=id;}
 public String getCode(){return code;} public void setCode(String c){this.code=c;}
 public String getLibelle(){return libelle;} public void setLibelle(String l){this.libelle=l;}
}
```

#### `entity/StatutOffre.java`
Table `statut_offre` : le statut d'une offre dans le workflow — **cœur du workflow**.

**Contenu du fichier :**
```java
package com.project.entity;
import jakarta.persistence.*;
@Entity @Table(name="statut_offre")
public class StatutOffre {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 private String code; private String libelle;
 public StatutOffre(){} public Integer getId(){return id;} public void setId(Integer id){this.id=id;}
 public String getCode(){return code;} public void setCode(String c){this.code=c;}
 public String getLibelle(){return libelle;} public void setLibelle(String l){this.libelle=l;}
}
```

#### `entity/Departement.java`
Table `departement` : service d'un utilisateur / d'une offre.

**Contenu du fichier :**
```java
package com.project.entity;
import jakarta.persistence.*;
@Entity @Table(name="departement")
public class Departement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 private String nom;
 public Departement(){} public Integer getId(){return id;} public void setId(Integer id){this.id=id;}
 public String getNom(){return nom;} public void setNom(String nom){this.nom=nom;}
}
```

#### `entity/Poste.java`
Table `poste` : **catalogue** des intitulés (alimente le dropdown manager).

**Contenu du fichier :**
```java
package com.project.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="poste")
public class Poste {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 private String intitule; private String description; private Boolean actif=true;
 @Column(name="created_at") private LocalDateTime createdAt;
 public Poste(){} 
 public Integer getId(){return id;} public void setId(Integer id){this.id=id;}
 public String getIntitule(){return intitule;} public void setIntitule(String s){this.intitule=s;}
 public String getDescription(){return description;} public void setDescription(String d){this.description=d;}
 public Boolean getActif(){return actif;} public void setActif(Boolean a){this.actif=a;}
 public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime c){this.createdAt=c;}
}
```

#### `entity/Utilisateur.java`
Table `utilisateur` : compte (email, mot de passe BCrypt, rôle, département, actif).

**Contenu du fichier :**
```java
package com.project.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="utilisateur")
public class Utilisateur {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 private String email;
 @Column(name="mot_de_passe") private String motDePasse;
 @ManyToOne @JoinColumn(name="id_role") private Role role;
 @ManyToOne @JoinColumn(name="id_departement") private Departement departement;
 private Boolean actif = true;
 @Column(name="created_at") private LocalDateTime createdAt;
 public Utilisateur(){} 
 public Integer getId(){return id;} public void setId(Integer id){this.id=id;}
 public String getEmail(){return email;} public void setEmail(String e){this.email=e;}
 public String getMotDePasse(){return motDePasse;} public void setMotDePasse(String m){this.motDePasse=m;}
 public Role getRole(){return role;} public void setRole(Role r){this.role=r;}
 public Departement getDepartement(){return departement;} public void setDepartement(Departement d){this.departement=d;}
 public Boolean getActif(){return actif;} public void setActif(Boolean a){this.actif=a;}
 public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime c){this.createdAt=c;}
}
```

#### `entity/DemandePoste.java`
Table `demande_poste` : demande de nouveau poste (intitulé, justification, décision RH,
poste créé éventuel).

**Contenu du fichier :**
```java
package com.project.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="demande_poste")
public class DemandePoste {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @ManyToOne @JoinColumn(name="id_manager") private Utilisateur manager;
 @ManyToOne @JoinColumn(name="id_departement") private Departement departement;
 @Column(name="intitule_propose") private String intitulePropose;
 private String justification;
 @ManyToOne @JoinColumn(name="id_decision") private Decision decision;
 @ManyToOne @JoinColumn(name="id_rh") private Utilisateur rh;
 @Column(name="motif_refus") private String motifRefus;
 @ManyToOne @JoinColumn(name="id_poste") private Poste poste;
 @Column(name="created_at") private LocalDateTime createdAt;
 @Column(name="decided_at") private LocalDateTime decidedAt;
 public DemandePoste(){} 
 public Integer getId(){return id;} public void setId(Integer id){this.id=id;}
 public Utilisateur getManager(){return manager;} public void setManager(Utilisateur u){this.manager=u;}
 public Departement getDepartement(){return departement;} public void setDepartement(Departement d){this.departement=d;}
 public String getIntitulePropose(){return intitulePropose;} public void setIntitulePropose(String s){this.intitulePropose=s;}
 public String getJustification(){return justification;} public void setJustification(String s){this.justification=s;}
 public Decision getDecision(){return decision;} public void setDecision(Decision d){this.decision=d;}
 public Utilisateur getRh(){return rh;} public void setRh(Utilisateur u){this.rh=u;}
 public String getMotifRefus(){return motifRefus;} public void setMotifRefus(String s){this.motifRefus=s;}
 public Poste getPoste(){return poste;} public void setPoste(Poste p){this.poste=p;}
 public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime c){this.createdAt=c;}
 public LocalDateTime getDecidedAt(){return decidedAt;} public void setDecidedAt(LocalDateTime d){this.decidedAt=d;}
}
```

#### `entity/OffreEmploi.java`
Table `offre_emploi` : l'offre (poste, département, manager, expérience, places, salaire,
statut, date de publication).

**Contenu du fichier :**
```java
package com.project.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="offre_emploi")
public class OffreEmploi {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @ManyToOne @JoinColumn(name="id_poste") private Poste poste;
 @ManyToOne @JoinColumn(name="id_departement") private Departement departement;
 @ManyToOne @JoinColumn(name="id_manager") private Utilisateur manager;
 @Column(name="annees_experience") private Integer anneesExperience;
 @Column(name="nb_places") private Integer nbPlaces;
 private BigDecimal salaire;
 @ManyToOne @JoinColumn(name="id_statut") private StatutOffre statut;
 @Column(name="date_publication") private LocalDateTime datePublication;
 @Column(name="created_at") private LocalDateTime createdAt;
 public OffreEmploi(){} 
 public Integer getId(){return id;} public void setId(Integer id){this.id=id;}
 public Poste getPoste(){return poste;} public void setPoste(Poste p){this.poste=p;}
 public Departement getDepartement(){return departement;} public void setDepartement(Departement d){this.departement=d;}
 public Utilisateur getManager(){return manager;} public void setManager(Utilisateur u){this.manager=u;}
 public Integer getAnneesExperience(){return anneesExperience;} public void setAnneesExperience(Integer a){this.anneesExperience=a;}
 public Integer getNbPlaces(){return nbPlaces;} public void setNbPlaces(Integer n){this.nbPlaces=n;}
 public BigDecimal getSalaire(){return salaire;} public void setSalaire(BigDecimal s){this.salaire=s;}
 public StatutOffre getStatut(){return statut;} public void setStatut(StatutOffre s){this.statut=s;}
 public LocalDateTime getDatePublication(){return datePublication;} public void setDatePublication(LocalDateTime d){this.datePublication=d;}
 public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime c){this.createdAt=c;}
}
```

#### `entity/OffreValidation.java`
Table `offre_validation` : **journal des validations** (rôle, valideur, décision,
salaire avant/après, commentaire, date).

**Contenu du fichier :**
```java
package com.project.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="offre_validation")
public class OffreValidation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @ManyToOne @JoinColumn(name="id_offre") private OffreEmploi offre;
 @ManyToOne @JoinColumn(name="id_role_valideur") private Role roleValideur;
 @ManyToOne @JoinColumn(name="id_valideur") private Utilisateur valideur;
 @ManyToOne @JoinColumn(name="id_decision") private Decision decision;
 @Column(name="salaire_avant") private BigDecimal salaireAvant;
 @Column(name="salaire_apres") private BigDecimal salaireApres;
 private String commentaire;
 @Column(name="decided_at") private LocalDateTime decidedAt;
 public OffreValidation(){} 
 public Integer getId(){return id;} public void setId(Integer id){this.id=id;}
 public OffreEmploi getOffre(){return offre;} public void setOffre(OffreEmploi o){this.offre=o;}
 public Role getRoleValideur(){return roleValideur;} public void setRoleValideur(Role r){this.roleValideur=r;}
 public Utilisateur getValideur(){return valideur;} public void setValideur(Utilisateur u){this.valideur=u;}
 public Decision getDecision(){return decision;} public void setDecision(Decision d){this.decision=d;}
 public BigDecimal getSalaireAvant(){return salaireAvant;} public void setSalaireAvant(BigDecimal s){this.salaireAvant=s;}
 public BigDecimal getSalaireApres(){return salaireApres;} public void setSalaireApres(BigDecimal s){this.salaireApres=s;}
 public String getCommentaire(){return commentaire;} public void setCommentaire(String c){this.commentaire=c;}
 public LocalDateTime getDecidedAt(){return decidedAt;} public void setDecidedAt(LocalDateTime d){this.decidedAt=d;}
}
```

---

### 9.13 Les repositories (`repository/`)

Un **repository** Spring Data JPA est une **interface** héritant de
`JpaRepository<Entité, TypeId>`. Les méthodes suivant la convention de nommage
(`findByX`, `OrderByY`, `_` pour traverser une relation) sont traduites en SQL
automatiquement.

Exemples :
- `findByStatut_CodeOrderByCreatedAtDesc("EN_ATTENTE_RH")` → `WHERE statut.code = ? ORDER BY created_at DESC`.
- `findByManager_IdOrderByCreatedAtDesc(id)` → offres d'un manager, récentes d'abord.

**Contenu des fichiers :**
```java
package com.project.repository;
import com.project.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface RoleRepository extends JpaRepository<Role,Integer> {
  Optional<Role> findByCode(String code);
}
```

```java
package com.project.repository;
import com.project.entity.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface DecisionRepository extends JpaRepository<Decision,Integer> {
  Optional<Decision> findByCode(String code);
}
```

```java
package com.project.repository;
import com.project.entity.StatutOffre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface StatutOffreRepository extends JpaRepository<StatutOffre,Integer> {
  Optional<StatutOffre> findByCode(String code);
}
```

```java
package com.project.repository;
import com.project.entity.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DepartementRepository extends JpaRepository<Departement,Integer> {}
```

```java
package com.project.repository;
import com.project.entity.Poste;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PosteRepository extends JpaRepository<Poste,Integer> {
  List<Poste> findByActifTrueOrderByIntituleAsc();
  List<Poste> findAllByOrderByIntituleAsc();
}
```

```java
package com.project.repository;
import com.project.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UtilisateurRepository extends JpaRepository<Utilisateur,Integer> {
  Optional<Utilisateur> findByEmail(String email);
}
```

```java
package com.project.repository;
import com.project.entity.DemandePoste;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DemandePosteRepository extends JpaRepository<DemandePoste,Integer> {
  List<DemandePoste> findByDecision_CodeOrderByCreatedAtDesc(String code);
  List<DemandePoste> findByManager_IdOrderByCreatedAtDesc(Integer managerId);
  List<DemandePoste> findAllByOrderByCreatedAtDesc();
}
```

```java
package com.project.repository;
import com.project.entity.OffreEmploi;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface OffreEmploiRepository extends JpaRepository<OffreEmploi,Integer> {
  List<OffreEmploi> findByStatut_CodeOrderByCreatedAtDesc(String code);
  List<OffreEmploi> findByStatut_CodeInOrderByCreatedAtDesc(List<String> codes);
  List<OffreEmploi> findByManager_IdOrderByCreatedAtDesc(Integer managerId);
  List<OffreEmploi> findAllByOrderByCreatedAtDesc();
}
```

```java
package com.project.repository;
import com.project.entity.OffreValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface OffreValidationRepository extends JpaRepository<OffreValidation,Integer> {
  List<OffreValidation> findByOffre_IdOrderByDecidedAtAsc(Integer offreId);
}
```

---

### 9.14 `service/AuthService.java`

**Rôle** : authentification et session.
- `authenticate` : compare le mot de passe (BCrypt) et vérifie le compte actif.
- `login` : stocke dans la session `userId`, `userEmail`, `userRole`, `userRoleLibelle`.
- `logout` : invalide la session.
- `currentUser` : recharge l'utilisateur depuis `userId`.
- `redirectByRole` : redirige vers la vue adaptée au rôle.

**Contenu du fichier :**
```java
package com.project.service;
import com.project.entity.Utilisateur;
import com.project.repository.UtilisateurRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
@Service
public class AuthService {
  private final UtilisateurRepository utilisateurRepository;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  public AuthService(UtilisateurRepository ur){this.utilisateurRepository=ur;}
  public Optional<Utilisateur> authenticate(String email,String rawPassword){
    Optional<Utilisateur> opt = utilisateurRepository.findByEmail(email);
    if(opt.isEmpty()) return Optional.empty();
    Utilisateur u = opt.get();
    if(!encoder.matches(rawPassword, u.getMotDePasse())) return Optional.empty();
    if(!Boolean.TRUE.equals(u.getActif())) return Optional.empty();
    return Optional.of(u);
  }
  public void login(HttpSession session, Utilisateur u){
    session.setAttribute("userId", u.getId());
    session.setAttribute("userEmail", u.getEmail());
    session.setAttribute("userRole", u.getRole().getCode());
    session.setAttribute("userRoleLibelle", u.getRole().getLibelle());
  }
  public void logout(HttpSession session){ session.invalidate(); }
  public Optional<Utilisateur> currentUser(HttpSession session){
    Object id = session.getAttribute("userId");
    if(id==null) return Optional.empty();
    return utilisateurRepository.findById((Integer)id);
  }
  public String redirectByRole(String roleCode){
    return switch(roleCode){
      case "MANAGER" -> "redirect:/manager/dashboard";
      case "RH" -> "redirect:/rh/dashboard";
      case "FINANCE" -> "redirect:/finance/dashboard";
      case "DIRECTION" -> "redirect:/direction/dashboard";
      case "CANDIDAT" -> "redirect:/candidat/offres";
      default -> "redirect:/login";
    };
  }
}
```

---

### 9.15 `service/OffreService.java`

**Rôle** : **toute la logique métier du workflow**. Fichier le plus important.

| Méthode | Ce qu'elle fait |
|---------|-----------------|
| `postesActifs()` | liste des postes actifs (dropdown) |
| `creerOffre(...)` | crée une `OffreEmploi` en `EN_ATTENTE_RH` |
| `demanderNouveauPoste(...)` | crée une `DemandePoste` en `EN_ATTENTE` |
| `traiterDemandePoste(...)` | RH accepte (crée le `Poste`) ou refuse |
| `rhValiderOffre(...)` | RH revoit/modifie poste, expérience, places + **fixe le salaire** → `EN_ATTENTE_FINANCE` |
| `financeValiderOffre(...)` | Finance modifie/valide le salaire → `EN_ATTENTE_DIRECTION` |
| `directionValiderOffre(...)` | Direction valide → `PUBLIEE` (+ `datePublication`) ou refuse |
| `offresEnAttenteRh/Finance/Direction()` | listes filtrées par statut |
| `offresPubliees()` | offres visibles candidats |
| `validations(offreId)` | historique des validations |

Chaque transition crée une `OffreValidation` (traçabilité). Méthodes d'écriture
`@Transactional`.

**Contenu du fichier :**
```java
package com.project.service;
import com.project.entity.*;
import com.project.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service
public class OffreService {
  private final OffreEmploiRepository offreRepo;
  private final PosteRepository posteRepo;
  private final StatutOffreRepository statutRepo;
  private final DecisionRepository decisionRepo;
  private final RoleRepository roleRepo;
  private final OffreValidationRepository validationRepo;
  private final DemandePosteRepository demandeRepo;
  private final DepartementRepository departementRepo;
  public OffreService(OffreEmploiRepository o, PosteRepository p, StatutOffreRepository s, DecisionRepository d,
                      RoleRepository r, OffreValidationRepository v, DemandePosteRepository dp, DepartementRepository dept){
    this.offreRepo=o; this.posteRepo=p; this.statutRepo=s; this.decisionRepo=d; this.roleRepo=r; this.validationRepo=v; this.demandeRepo=dp; this.departementRepo=dept;
  }
  public List<Poste> postesActifs(){ return posteRepo.findByActifTrueOrderByIntituleAsc(); }
  public List<Departement> departements(){ return departementRepo.findAll(); }

  // Manager cree une offre (poste existant)
  @Transactional
  public OffreEmploi creerOffre(Integer posteId,int anneesExp,int nbPlaces, Utilisateur manager){
    Poste poste = posteRepo.findById(posteId).orElseThrow();
    Departement dept = manager.getDepartement();
    StatutOffre statut = statutRepo.findByCode("EN_ATTENTE_RH").orElseThrow();
    OffreEmploi o = new OffreEmploi();
    o.setPoste(poste); o.setDepartement(dept); o.setManager(manager);
    o.setAnneesExperience(anneesExp); o.setNbPlaces(nbPlaces);
    o.setStatut(statut); o.setCreatedAt(LocalDateTime.now());
    return offreRepo.save(o);
  }

  // Manager demande nouveau poste
  @Transactional
  public DemandePoste demanderNouveauPoste(String intitule, String justification, Utilisateur manager){
    Decision enAttente = decisionRepo.findByCode("EN_ATTENTE").orElseThrow();
    DemandePoste d = new DemandePoste();
    d.setManager(manager); d.setDepartement(manager.getDepartement());
    d.setIntitulePropose(intitule); d.setJustification(justification);
    d.setDecision(enAttente); d.setCreatedAt(LocalDateTime.now());
    return demandeRepo.save(d);
  }

  // RH valide/refuse demande de poste
  @Transactional
  public DemandePoste traiterDemandePoste(Integer demandeId, boolean valider, String motif, Utilisateur rh){
    DemandePoste d = demandeRepo.findById(demandeId).orElseThrow();
    Decision dec = decisionRepo.findByCode(valider?"VALIDEE":"REFUSEE").orElseThrow();
    d.setDecision(dec); d.setRh(rh); d.setDecidedAt(LocalDateTime.now());
    if(!valider){ d.setMotifRefus(motif); }
    else {
      Poste p = new Poste(); p.setIntitule(d.getIntitulePropose()); p.setDescription(d.getJustification()); p.setActif(true); p.setCreatedAt(LocalDateTime.now());
      p = posteRepo.save(p); d.setPoste(p);
    }
    return demandeRepo.save(d);
  }

  // RH valide une offre : il revoit le meme formulaire que le manager (poste/exp/places)
  // + un champ salaire a definir -> passe en EN_ATTENTE_FINANCE
  @Transactional
  public OffreEmploi rhValiderOffre(Integer offreId, Integer posteId, Integer anneesExp, Integer nbPlaces,
                                    BigDecimal salaire, Utilisateur rh, boolean valider, String commentaire){
    OffreEmploi o = offreRepo.findById(offreId).orElseThrow();
    if(!"EN_ATTENTE_RH".equals(o.getStatut().getCode())) throw new IllegalStateException("Offre pas en attente RH");
    Decision dec = decisionRepo.findByCode(valider?"VALIDEE":"REFUSEE").orElseThrow();
    Role roleRh = roleRepo.findByCode("RH").orElseThrow();
    BigDecimal avant = o.getSalaire();
    if(valider){
      if(posteId!=null) o.setPoste(posteRepo.findById(posteId).orElseThrow());
      if(anneesExp!=null && anneesExp>=0) o.setAnneesExperience(anneesExp);
      if(nbPlaces!=null && nbPlaces>0) o.setNbPlaces(nbPlaces);
      o.setSalaire(salaire);
      o.setStatut(statutRepo.findByCode("EN_ATTENTE_FINANCE").orElseThrow());
    } else {
      o.setStatut(statutRepo.findByCode("REFUSEE_RH").orElseThrow());
    }
    offreRepo.save(o);
    OffreValidation v = new OffreValidation();
    v.setOffre(o); v.setRoleValideur(roleRh); v.setValideur(rh); v.setDecision(dec);
    v.setSalaireAvant(avant); v.setSalaireApres(valider?salaire:avant);
    v.setCommentaire(commentaire); v.setDecidedAt(LocalDateTime.now());
    validationRepo.save(v);
    return o;
  }

  // Finance valide/modifie salaire -> passe en EN_ATTENTE_DIRECTION
  @Transactional
  public OffreEmploi financeValiderOffre(Integer offreId, BigDecimal nouveauSalaire, Utilisateur finance, boolean valider, String commentaire){
    OffreEmploi o = offreRepo.findById(offreId).orElseThrow();
    if(!"EN_ATTENTE_FINANCE".equals(o.getStatut().getCode())) throw new IllegalStateException("Offre pas en attente Finance");
    Decision dec = decisionRepo.findByCode(valider?"VALIDEE":"REFUSEE").orElseThrow();
    Role roleF = roleRepo.findByCode("FINANCE").orElseThrow();
    BigDecimal avant = o.getSalaire();
    if(valider){
      if(nouveauSalaire!=null) o.setSalaire(nouveauSalaire);
      o.setStatut(statutRepo.findByCode("EN_ATTENTE_DIRECTION").orElseThrow());
    } else {
      o.setStatut(statutRepo.findByCode("REFUSEE_FINANCE").orElseThrow());
    }
    offreRepo.save(o);
    OffreValidation v = new OffreValidation();
    v.setOffre(o); v.setRoleValideur(roleF); v.setValideur(finance); v.setDecision(dec);
    v.setSalaireAvant(avant); v.setSalaireApres(o.getSalaire());
    v.setCommentaire(commentaire); v.setDecidedAt(LocalDateTime.now());
    validationRepo.save(v);
    return o;
  }

  // Direction validation finale -> PUBLIE ou REFUSEE_DIRECTION
  @Transactional
  public OffreEmploi directionValiderOffre(Integer offreId, Utilisateur direction, boolean valider, String commentaire){
    OffreEmploi o = offreRepo.findById(offreId).orElseThrow();
    if(!"EN_ATTENTE_DIRECTION".equals(o.getStatut().getCode())) throw new IllegalStateException("Offre pas en attente Direction");
    Decision dec = decisionRepo.findByCode(valider?"VALIDEE":"REFUSEE").orElseThrow();
    Role roleD = roleRepo.findByCode("DIRECTION").orElseThrow();
    if(valider){
      o.setStatut(statutRepo.findByCode("PUBLIEE").orElseThrow());
      o.setDatePublication(LocalDateTime.now());
    } else {
      o.setStatut(statutRepo.findByCode("REFUSEE_DIRECTION").orElseThrow());
    }
    offreRepo.save(o);
    OffreValidation v = new OffreValidation();
    v.setOffre(o); v.setRoleValideur(roleD); v.setValideur(direction); v.setDecision(dec);
    v.setSalaireAvant(o.getSalaire()); v.setSalaireApres(o.getSalaire());
    v.setCommentaire(commentaire); v.setDecidedAt(LocalDateTime.now());
    validationRepo.save(v);
    return o;
  }

  public List<OffreEmploi> offresEnAttenteRh(){ return offreRepo.findByStatut_CodeOrderByCreatedAtDesc("EN_ATTENTE_RH"); }
  public List<OffreEmploi> offresEnAttenteFinance(){ return offreRepo.findByStatut_CodeOrderByCreatedAtDesc("EN_ATTENTE_FINANCE"); }
  public List<OffreEmploi> offresEnAttenteDirection(){ return offreRepo.findByStatut_CodeOrderByCreatedAtDesc("EN_ATTENTE_DIRECTION"); }
  public List<OffreEmploi> offresPubliees(){ return offreRepo.findByStatut_CodeOrderByCreatedAtDesc("PUBLIEE"); }
  public List<DemandePoste> demandesEnAttente(){ return demandeRepo.findByDecision_CodeOrderByCreatedAtDesc("EN_ATTENTE"); }
  public List<DemandePoste> toutesDemandes(){ return demandeRepo.findAllByOrderByCreatedAtDesc(); }
  public List<OffreEmploi> toutesOffres(){ return offreRepo.findAllByOrderByCreatedAtDesc(); }
  public List<OffreEmploi> offresDuManager(Integer managerId){ return offreRepo.findByManager_IdOrderByCreatedAtDesc(managerId); }
  public List<OffreValidation> validations(Integer offreId){ return validationRepo.findByOffre_IdOrderByDecidedAtAsc(offreId); }
  public Optional<OffreEmploi> findOffre(Integer id){ return offreRepo.findById(id); }
}
```

---

### 9.16 Les contrôleurs (`controller/`)

Un **contrôleur** mappe une URL à une action (`@Controller`, `@RequestMapping`).
`@GetMapping` = afficher, `@PostMapping` = traiter un formulaire. Il récupère
l'utilisateur courant via `authService.currentUser(session)`, appelle le service, renvoie
une **vue** (`"manager/dashboard"` → `/WEB-INF/jsp/manager/dashboard.jsp`) ou une
redirection. `RedirectAttributes` transporte un message `success`/`error` après
redirection (pattern *flash*).

#### `controller/AuthController.java`
`/`, `/login` (GET affiche, POST authentifie) et `/logout`. Redirige si session existante.

**Contenu du fichier :**
```java
package com.project.controller;
import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
@Controller
public class AuthController {
  private final AuthService authService;
  public AuthController(AuthService a){this.authService=a;}
  @GetMapping({"/","/login"})
  public String loginPage(@RequestParam(value="error",required=false) String error, Model model, HttpSession session){
    if(session!=null && session.getAttribute("userId")!=null){
      String role=(String)session.getAttribute("userRole");
      return authService.redirectByRole(role);
    }
    if(error!=null) model.addAttribute("error", error.equals("role")?"Accès non autorisé pour votre rôle.":"Email ou mot de passe incorrect.");
    return "login";
  }
  @PostMapping("/login")
  public String doLogin(@RequestParam String email, @RequestParam String password, HttpSession session, Model model){
    Optional<Utilisateur> opt = authService.authenticate(email,password);
    if(opt.isEmpty()){ model.addAttribute("error","Email ou mot de passe incorrect."); return "login"; }
    authService.login(session, opt.get());
    return authService.redirectByRole(opt.get().getRole().getCode());
  }
  @GetMapping("/logout")
  public String logout(HttpSession session){ if(session!=null) authService.logout(session); return "redirect:/login"; }
}
```

#### `controller/ManagerController.java`
`/manager/dashboard`, `POST /manager/offre`, `POST /manager/demande-poste`,
`/manager/demandes`.

**Contenu du fichier :**
```java
package com.project.controller;
import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/manager")
public class ManagerController {
  private final OffreService offreService;
  private final AuthService authService;
  public ManagerController(OffreService o, AuthService a){this.offreService=o; this.authService=a;}
  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("postes", offreService.postesActifs());
    model.addAttribute("mesOffres", offreService.offresDuManager(u.getId()));
    model.addAttribute("toutesOffres", offreService.toutesOffres());
    return "manager/dashboard";
  }
  @PostMapping("/offre")
  public String creerOffre(@RequestParam Integer posteId, @RequestParam int anneesExperience, @RequestParam int nbPlaces, HttpSession session, RedirectAttributes ra){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    if(nbPlaces<=0 || anneesExperience<0){ ra.addFlashAttribute("error","Valeurs invalides."); return "redirect:/manager/dashboard"; }
    offreService.creerOffre(posteId, anneesExperience, nbPlaces, u);
    ra.addFlashAttribute("success","Demande d\u0027offre envoy\u00e9e au RH (en attente RH).");
    return "redirect:/manager/dashboard";
  }
  @PostMapping("/demande-poste")
  public String demanderPoste(@RequestParam String intitule, @RequestParam(required=false) String justification, HttpSession session, RedirectAttributes ra){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    if(intitule==null || intitule.isBlank()){ ra.addFlashAttribute("error","Intitule requis."); return "redirect:/manager/dashboard"; }
    offreService.demanderNouveauPoste(intitule.trim(), justification, u);
    ra.addFlashAttribute("success","Demande de nouveau poste envoy\u00e9e au RH.");
    return "redirect:/manager/dashboard";
  }
  @GetMapping("/demandes")
  public String mesDemandes(HttpSession session, Model model){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("demandes", offreService.toutesDemandes().stream().filter(d->d.getManager().getId().equals(u.getId())).toList());
    return "manager/demandes";
  }
}
```

#### `controller/RhController.java`
`/rh/dashboard`, `POST /rh/offre/{id}/valider` (poste/expérience/places + salaire),
`POST /rh/offre/{id}/refuser`, `POST /rh/demande/{id}/valider`, `POST /rh/demande/{id}/refuser`.

**Contenu du fichier :**
```java
package com.project.controller;
import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
@Controller @RequestMapping("/rh")
public class RhController {
  private final OffreService offreService;
  private final AuthService authService;
  public RhController(OffreService o, AuthService a){this.offreService=o; this.authService=a;}
  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("postes", offreService.postesActifs());
    model.addAttribute("offresAttente", offreService.offresEnAttenteRh());
    model.addAttribute("demandesAttente", offreService.demandesEnAttente());
    model.addAttribute("toutesOffres", offreService.toutesOffres());
    model.addAttribute("toutesDemandes", offreService.toutesDemandes());
    return "rh/dashboard";
  }
  @PostMapping("/offre/{id}/valider")
  public String validerOffre(@PathVariable Integer id,
                             @RequestParam(required=false) Integer posteId,
                             @RequestParam(required=false) Integer anneesExperience,
                             @RequestParam(required=false) Integer nbPlaces,
                             @RequestParam BigDecimal salaire,
                             @RequestParam(required=false) String commentaire,
                             HttpSession session, RedirectAttributes ra){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try{ offreService.rhValiderOffre(id, posteId, anneesExperience, nbPlaces, salaire, u, true, commentaire); ra.addFlashAttribute("success","Offre validee -> transmise a la Finance."); }
    catch(Exception e){ ra.addFlashAttribute("error", e.getMessage()); }
    return "redirect:/rh/dashboard";
  }
  @PostMapping("/offre/{id}/refuser")
  public String refuserOffre(@PathVariable Integer id, @RequestParam(required=false) String commentaire, HttpSession session, RedirectAttributes ra){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try{ offreService.rhValiderOffre(id, null, null, null, null, u, false, commentaire); ra.addFlashAttribute("success","Offre refusee par RH."); }
    catch(Exception e){ ra.addFlashAttribute("error", e.getMessage()); }
    return "redirect:/rh/dashboard";
  }
  @PostMapping("/demande/{id}/valider")
  public String validerDemande(@PathVariable Integer id, HttpSession session, RedirectAttributes ra){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    offreService.traiterDemandePoste(id, true, null, u);
    ra.addFlashAttribute("success","Poste ajoute au catalogue (dropdown manager)." );
    return "redirect:/rh/dashboard";
  }
  @PostMapping("/demande/{id}/refuser")
  public String refuserDemande(@PathVariable Integer id, @RequestParam(required=false) String motif, HttpSession session, RedirectAttributes ra){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    offreService.traiterDemandePoste(id, false, motif, u);
    ra.addFlashAttribute("success","Demande de poste refusee.");
    return "redirect:/rh/dashboard";
  }
}
```

#### `controller/FinanceController.java`
`/finance/dashboard`, `POST /finance/offre/{id}/valider` (salaire modifiable),
`POST .../refuser`.

**Contenu du fichier :**
```java
package com.project.controller;
import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
@Controller @RequestMapping("/finance")
public class FinanceController {
  private final OffreService offreService;
  private final AuthService authService;
  public FinanceController(OffreService o, AuthService a){this.offreService=o; this.authService=a;}
  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("offresAttente", offreService.offresEnAttenteFinance());
    model.addAttribute("toutesOffres", offreService.toutesOffres());
    return "finance/dashboard";
  }
  @PostMapping("/offre/{id}/valider")
  public String valider(@PathVariable Integer id, @RequestParam(required=false) BigDecimal salaire, @RequestParam(required=false) String commentaire, HttpSession session, RedirectAttributes ra){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try{ offreService.financeValiderOffre(id, salaire, u, true, commentaire); ra.addFlashAttribute("success","Offre validee -> transmise a la Direction."); }
    catch(Exception e){ ra.addFlashAttribute("error", e.getMessage()); }
    return "redirect:/finance/dashboard";
  }
  @PostMapping("/offre/{id}/refuser")
  public String refuser(@PathVariable Integer id, @RequestParam(required=false) String commentaire, HttpSession session, RedirectAttributes ra){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try{ offreService.financeValiderOffre(id, null, u, false, commentaire); ra.addFlashAttribute("success","Offre refusee par Finance."); }
    catch(Exception e){ ra.addFlashAttribute("error", e.getMessage()); }
    return "redirect:/finance/dashboard";
  }
}
```

#### `controller/DirectionController.java`
`/direction/dashboard`, `POST /direction/offre/{id}/valider` (publication),
`POST .../refuser`.

**Contenu du fichier :**
```java
package com.project.controller;
import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/direction")
public class DirectionController {
  private final OffreService offreService;
  private final AuthService authService;
  public DirectionController(OffreService o, AuthService a){this.offreService=o; this.authService=a;}
  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("offresAttente", offreService.offresEnAttenteDirection());
    model.addAttribute("toutesOffres", offreService.toutesOffres());
    model.addAttribute("offresPubliees", offreService.offresPubliees());
    return "direction/dashboard";
  }
  @PostMapping("/offre/{id}/valider")
  public String valider(@PathVariable Integer id, @RequestParam(required=false) String commentaire, HttpSession session, RedirectAttributes ra){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try{ offreService.directionValiderOffre(id, u, true, commentaire); ra.addFlashAttribute("success","Offre PUBLIEe ! Visible par les candidats."); }
    catch(Exception e){ ra.addFlashAttribute("error", e.getMessage()); }
    return "redirect:/direction/dashboard";
  }
  @PostMapping("/offre/{id}/refuser")
  public String refuser(@PathVariable Integer id, @RequestParam(required=false) String commentaire, HttpSession session, RedirectAttributes ra){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    try{ offreService.directionValiderOffre(id, u, false, commentaire); ra.addFlashAttribute("success","Offre refusee par Direction."); }
    catch(Exception e){ ra.addFlashAttribute("error", e.getMessage()); }
    return "redirect:/direction/dashboard";
  }
}
```

#### `controller/CandidatController.java`
`/candidat/offres` (offres publiées), `/candidat/offre/{id}` (détail + historique).

**Contenu du fichier :**
```java
package com.project.controller;
import com.project.entity.Utilisateur;
import com.project.service.AuthService;
import com.project.service.OffreService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller @RequestMapping("/candidat")
public class CandidatController {
  private final OffreService offreService;
  private final AuthService authService;
  public CandidatController(OffreService o, AuthService a){this.offreService=o; this.authService=a;}
  @GetMapping("/offres")
  public String offres(HttpSession session, Model model){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("offres", offreService.offresPubliees());
    return "candidat/offres";
  }
  @GetMapping("/offre/{id}")
  public String detail(@PathVariable Integer id, HttpSession session, Model model){
    Utilisateur u = authService.currentUser(session).orElseThrow();
    model.addAttribute("user", u);
    model.addAttribute("offre", offreService.findOffre(id).orElseThrow());
    model.addAttribute("validations", offreService.validations(id));
    return "candidat/detail";
  }
}
```

---

### 9.17 Les vues JSP (`WEB-INF/jsp/`)

Les JSP mélangent HTML et balises/EL. Syntaxe utile :
- `<%@ taglib prefix="c" uri="jakarta.tags.core" %>` : bibliothèque JSTL *core*.
- `<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>` : fonctions maison.
- `${...}` : Expression Language (EL). Ex. `${offres}`, `${sessionScope.userEmail}`.
- `<c:forEach var="o" items="${offres}">` : boucle.
- `<c:if test="${...}">` : condition ; `<c:choose>/<c:when>/<c:otherwise>` : alternative.
- `${pageContext.request.contextPath}` : racine de l'application (pour les URL).
- `${empty x}` : vrai si `x` est null ou vide ; `<c:out>` : échappement HTML.
- `<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>` : inclusion du fragment menu.
- `${f:dateTime(o.createdAt)}` : dates formatées `dd/MM/yyyy HH:mm`.

#### `jsp/fragments/navbar.jsp`
Fragment **navbar** inclus dans toutes les pages (utilisateur, rôle, déconnexion).

**Contenu du fichier :**
```jsp
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
 <div class="container">
  <a class="navbar-brand" href="${pageContext.request.contextPath}/">Recrutement</a>
  <div class="ms-auto d-flex align-items-center gap-3">
   <span class="text-light small">${sessionScope.userEmail}</span>
   <span class="badge bg-light text-dark">${sessionScope.userRole}</span>
   <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-light btn-sm">Déconnexion</a>
  </div>
 </div>
</nav>
```

#### `jsp/login.jsp`
Page de connexion (formulaire email/mot de passe, message d'erreur, comptes de test).

**Contenu du fichier :**
```jsp
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
```

#### `jsp/manager/dashboard.jsp`
Les **deux formulaires** du manager (offre sur poste existant, demande de nouveau poste)
+ tableau de ses offres.

**Contenu du fichier :**
```jsp
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Manager</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <h4>Manager — <span>${user.departement.nom}</span></h4>
 <p class="text-muted small">Deux actions : (1) Demander une offre sur un poste existant (dropdown) — (2) Demander un nouveau poste au RH.</p>
 <div class="row g-4">
  <div class="col-lg-6">
   <div class="card shadow-sm"><div class="card-header bg-primary text-white">1) Demande d'offre — poste existant</div>
    <div class="card-body">
     <form method="post" action="${pageContext.request.contextPath}/manager/offre">
      <div class="mb-3"><label class="form-label">Poste (dropdown)</label>
       <select name="posteId" class="form-select" required>
        <option value="">-- choisir --</option>
        <c:forEach var="p" items="${postes}">
         <option value="${p.id}">${p.intitule}</option>
        </c:forEach>
       </select>
       <div class="form-text">Si le poste n'est pas dans la liste, utilisez le formulaire 2 ci-contre.</div>
      </div>
      <div class="mb-3"><label class="form-label">Années d'expérience requises</label><input name="anneesExperience" type="number" min="0" class="form-control" required value="2"></div>
      <div class="mb-3"><label class="form-label">Nombre de places</label><input name="nbPlaces" type="number" min="1" class="form-control" required value="5" placeholder="ex: 5 dev junior"></div>
      <button class="btn btn-primary w-100">Envoyer au RH</button>
     </form>
    </div>
   </div>
  </div>
  <div class="col-lg-6">
   <div class="card shadow-sm"><div class="card-header bg-secondary text-white">2) Demande de nouveau poste (pas dans le dropdown)</div>
    <div class="card-body">
     <form method="post" action="${pageContext.request.contextPath}/manager/demande-poste">
      <div class="mb-3"><label class="form-label">Intitulé proposé</label><input name="intitule" class="form-control" required placeholder="ex: DevOps Junior"></div>
      <div class="mb-3"><label class="form-label">Justification</label><textarea name="justification" class="form-control" rows="3" placeholder="Pourquoi ce poste ?"></textarea></div>
      <button class="btn btn-secondary w-100">Demander au RH</button>
     </form>
    </div>
   </div>
  </div>
 </div>
 <div class="card shadow-sm mt-4"><div class="card-header">Mes offres</div>
  <div class="card-body p-0"><div class="table-responsive"><table class="table table-sm mb-0">
   <thead><tr><th>Poste</th><th>Exp</th><th>Places</th><th>Salaire</th><th>Statut</th><th>Date</th></tr></thead>
   <tbody>
    <c:forEach var="o" items="${mesOffres}">
     <tr>
      <td>${o.poste.intitule}</td>
      <td>${o.anneesExperience}</td>
      <td>${o.nbPlaces}</td>
      <td><c:choose><c:when test="${empty o.salaire}">-</c:when><c:otherwise>${o.salaire} Ar</c:otherwise></c:choose></td>
      <td><span class="badge bg-info">${o.statut.libelle}</span></td>
      <td>${f:dateTime(o.createdAt)}</td>
     </tr>
    </c:forEach>
    <c:if test="${empty mesOffres}"><tr><td colspan="6" class="text-center text-muted py-3">Aucune offre</td></tr></c:if>
   </tbody>
  </table></div></div>
 </div>
</div>
</body></html>
```

#### `jsp/manager/demandes.jsp`
Liste des demandes de nouveaux postes du manager.

**Contenu du fichier :**
```jsp
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><title>Mes demandes</title></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <h5>Mes demandes de postes</h5>
 <table class="table table-sm">
  <thead><tr><th>Intitulé</th><th>Décision</th><th>Date</th></tr></thead>
  <tbody>
   <c:forEach var="d" items="${demandes}">
    <tr><td>${d.intitulePropose}</td><td>${d.decision.libelle}</td><td>${f:dateTime(d.createdAt)}</td></tr>
   </c:forEach>
  </tbody>
 </table>
 <a href="${pageContext.request.contextPath}/manager/dashboard" class="btn btn-sm btn-secondary">Retour</a>
</div>
</body></html>
```

#### `jsp/rh/dashboard.jsp`
Demandes de postes à valider/insérer + offres en attente RH (**même formulaire que le
manager + champ salaire**).

**Contenu du fichier :**
```jsp
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>RH</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <h4>RH — Validation</h4>
 <p class="small text-muted">Vous voyez : (A) demandes de nouveaux postes à valider/insérer — (B) offres en attente RH (même formulaire que le manager + champ salaire à définir).</p>
 <div class="row g-4">
  <div class="col-lg-6">
   <div class="card shadow-sm"><div class="card-header bg-info">A) Demandes de nouveau poste (EN_ATTENTE)</div>
    <div class="card-body">
     <c:if test="${empty demandesAttente}"><div class="text-muted small">Aucune demande en attente.</div></c:if>
     <c:forEach var="d" items="${demandesAttente}">
      <div class="border rounded p-2 mb-2">
       <strong>${d.intitulePropose}</strong> — <span>${d.departement.nom}</span> — par <span>${d.manager.email}</span>
       <div class="small text-muted">${d.justification}</div>
       <div class="d-flex gap-2 mt-2">
        <form method="post" action="${pageContext.request.contextPath}/rh/demande/${d.id}/valider"><button class="btn btn-sm btn-success">Valider &amp; insérer</button></form>
        <form method="post" action="${pageContext.request.contextPath}/rh/demande/${d.id}/refuser" class="d-flex gap-1"><input name="motif" placeholder="motif refus" class="form-control form-control-sm"><button class="btn btn-sm btn-danger">Refuser</button></form>
       </div>
      </div>
     </c:forEach>
    </div>
   </div>
  </div>
  <div class="col-lg-6">
   <div class="card shadow-sm"><div class="card-header bg-warning">B) Offres en attente RH</div>
    <div class="card-body">
     <c:if test="${empty offresAttente}"><div class="text-muted small">Aucune offre en attente RH.</div></c:if>
     <c:forEach var="o" items="${offresAttente}">
      <div class="border rounded p-2 mb-2">
       <strong>${o.poste.intitule}</strong> — <span>${o.departement.nom}</span> — Manager: <span>${o.manager.email}</span>
       <form method="post" action="${pageContext.request.contextPath}/rh/offre/${o.id}/valider" class="mt-2">
        <div class="mb-2">
         <label class="form-label small mb-1">Poste (dropdown)</label>
         <select name="posteId" class="form-select form-select-sm" required>
          <c:forEach var="p" items="${postes}">
           <option value="${p.id}" <c:if test="${p.id == o.poste.id}">selected</c:if>>${p.intitule}</option>
          </c:forEach>
         </select>
        </div>
        <div class="row g-2 mb-2">
         <div class="col-6"><label class="form-label small mb-1">Années d'expérience</label>
          <input name="anneesExperience" type="number" min="0" class="form-control form-control-sm" required value="${o.anneesExperience}"></div>
         <div class="col-6"><label class="form-label small mb-1">Nombre de places</label>
          <input name="nbPlaces" type="number" min="1" class="form-control form-control-sm" required value="${o.nbPlaces}"></div>
        </div>
        <div class="input-group input-group-sm mb-1"><span class="input-group-text">Salaire (Ar)</span><input name="salaire" type="number" step="0.01" min="0" class="form-control" required placeholder="ex: 1500000"><button class="btn btn-success">Valider &rarr; Finance</button></div>
        <input name="commentaire" class="form-control form-control-sm" placeholder="commentaire (optionnel)">
       </form>
       <form method="post" action="${pageContext.request.contextPath}/rh/offre/${o.id}/refuser" class="mt-1 d-flex gap-1"><input name="commentaire" placeholder="motif refus" class="form-control form-control-sm"><button class="btn btn-sm btn-outline-danger">Refuser</button></form>
      </div>
     </c:forEach>
    </div>
   </div>
  </div>
 </div>
 <div class="card shadow-sm mt-4"><div class="card-header">Historique toutes offres</div>
  <div class="card-body p-0"><div class="table-responsive"><table class="table table-sm mb-0">
   <thead><tr><th>Poste</th><th>Dept</th><th>Exp</th><th>Places</th><th>Salaire</th><th>Statut</th></tr></thead>
   <tbody>
    <c:forEach var="o" items="${toutesOffres}">
     <tr><td>${o.poste.intitule}</td><td>${o.departement.nom}</td><td>${o.anneesExperience}</td><td>${o.nbPlaces}</td><td>${empty o.salaire ? '-' : o.salaire}</td><td><span class="badge bg-secondary">${o.statut.libelle}</span></td></tr>
    </c:forEach>
   </tbody>
  </table></div></div>
 </div>
</div>
</body></html>
```

#### `jsp/finance/dashboard.jsp`
Offres en attente Finance : **modifier/valider le salaire**.

**Contenu du fichier :**
```jsp
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Finance</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <h4>Finance — Validation salaire</h4>
 <p class="small text-muted">Vous revoyez l'offre validée par le RH, vous pouvez modifier le salaire ou valider tel quel &rarr; passe à la Direction.</p>
 <div class="card shadow-sm"><div class="card-header bg-warning">Offres en attente Finance</div>
  <div class="card-body">
   <c:if test="${empty offresAttente}"><div class="text-muted small">Aucune offre en attente Finance.</div></c:if>
   <c:forEach var="o" items="${offresAttente}">
    <div class="border rounded p-3 mb-2">
     <strong>${o.poste.intitule}</strong> — <span>${o.departement.nom}</span> — Manager: <span>${o.manager.email}</span><br>
     <span class="small">Exp: <span>${o.anneesExperience}</span> ans — Places: <span>${o.nbPlaces}</span> — Salaire RH: <strong><c:choose><c:when test="${empty o.salaire}">-</c:when><c:otherwise>${o.salaire} Ar</c:otherwise></c:choose></strong></span>
     <form method="post" action="${pageContext.request.contextPath}/finance/offre/${o.id}/valider" class="mt-2">
      <div class="input-group input-group-sm mb-1"><span class="input-group-text">Salaire (modifier si besoin)</span><input name="salaire" type="number" step="0.01" min="0" class="form-control" value="${o.salaire}" placeholder="laisser vide = garder salaire RH"><button class="btn btn-success">Valider &rarr; Direction</button></div>
      <input name="commentaire" class="form-control form-control-sm" placeholder="commentaire (optionnel)">
     </form>
     <form method="post" action="${pageContext.request.contextPath}/finance/offre/${o.id}/refuser" class="mt-1 d-flex gap-1"><input name="commentaire" placeholder="motif refus" class="form-control form-control-sm"><button class="btn btn-sm btn-outline-danger">Refuser</button></form>
    </div>
   </c:forEach>
  </div>
 </div>
 <div class="card shadow-sm mt-4"><div class="card-header">Historique toutes offres</div><div class="card-body p-0"><div class="table-responsive"><table class="table table-sm mb-0">
  <thead><tr><th>Poste</th><th>Statut</th><th>Salaire</th></tr></thead>
  <tbody>
   <c:forEach var="o" items="${toutesOffres}"><tr><td>${o.poste.intitule}</td><td><span class="badge bg-secondary">${o.statut.libelle}</span></td><td>${o.salaire}</td></tr></c:forEach>
  </tbody>
 </table></div></div></div>
</div>
</body></html>
```

#### `jsp/direction/dashboard.jsp`
Validation finale (publier/refuser) + offres publiées.

**Contenu du fichier :**
```jsp
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Direction</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <c:if test="${not empty success}"><div class="alert alert-success">${success}</div></c:if>
 <c:if test="${not empty error}"><div class="alert alert-danger">${error}</div></c:if>
 <h4>Direction — Validation finale</h4>
 <p class="small text-muted">Dès que vous validez, l'offre est officiellement publiée et visible par les candidats.</p>
 <div class="card shadow-sm"><div class="card-header bg-dark text-white">Offres en attente Direction</div>
  <div class="card-body">
   <c:if test="${empty offresAttente}"><div class="text-muted small">Aucune offre en attente Direction.</div></c:if>
   <c:forEach var="o" items="${offresAttente}">
    <div class="border rounded p-3 mb-2">
     <strong>${o.poste.intitule}</strong> — <span>${o.departement.nom}</span> — Salaire: <strong>${o.salaire} Ar</strong> — Places: <span>${o.nbPlaces}</span> — Exp: <span>${o.anneesExperience}</span> ans<br>
     <span class="small text-muted">Manager: <span>${o.manager.email}</span></span>
     <div class="d-flex gap-2 mt-2">
      <form method="post" action="${pageContext.request.contextPath}/direction/offre/${o.id}/valider"><input type="hidden" name="commentaire" value=""><button class="btn btn-sm btn-success">Publier l'offre</button></form>
      <form method="post" action="${pageContext.request.contextPath}/direction/offre/${o.id}/refuser" class="d-flex gap-1"><input name="commentaire" placeholder="motif refus" class="form-control form-control-sm"><button class="btn btn-sm btn-danger">Refuser</button></form>
     </div>
    </div>
   </c:forEach>
  </div>
 </div>
 <div class="card shadow-sm mt-4"><div class="card-header">Offres publiées (visibles candidats)</div><div class="card-body p-0"><div class="table-responsive"><table class="table table-sm mb-0">
  <thead><tr><th>Poste</th><th>Dept</th><th>Salaire</th><th>Places</th><th>Date publication</th></tr></thead>
  <tbody>
   <c:forEach var="o" items="${offresPubliees}"><tr><td>${o.poste.intitule}</td><td>${o.departement.nom}</td><td>${o.salaire}</td><td>${o.nbPlaces}</td><td>${f:dateTime(o.datePublication)}</td></tr></c:forEach>
   <c:if test="${empty offresPubliees}"><tr><td colspan="5" class="text-center text-muted py-2">Aucune offre publiée</td></tr></c:if>
  </tbody>
 </table></div></div></div>
</div>
</body></html>
```

#### `jsp/candidat/offres.jsp`
Offres **publiées** vues par le candidat.

**Contenu du fichier :**
```jsp
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Offres - Candidat</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <h4>Offres publiées</h4>
 <p class="small text-muted">Seules les offres validées par RH &rarr; Finance &rarr; Direction apparaissent ici.</p>
 <c:if test="${empty offres}"><div class="alert alert-info">Aucune offre publiée pour le moment.</div></c:if>
 <div class="row g-3">
  <c:forEach var="o" items="${offres}">
   <div class="col-md-6 col-lg-4">
    <div class="card shadow-sm h-100">
     <div class="card-body">
      <h6>${o.poste.intitule}</h6>
      <p class="small text-muted mb-1">${o.poste.description}</p>
      <ul class="small mb-2">
       <li>Département: <span>${o.departement.nom}</span></li>
       <li>Exp: <span>${o.anneesExperience}</span> ans</li>
       <li>Places: <span>${o.nbPlaces}</span></li>
       <li>Salaire: <strong><c:choose><c:when test="${empty o.salaire}">-</c:when><c:otherwise>${o.salaire} Ar</c:otherwise></c:choose></strong></li>
      </ul>
      <a href="${pageContext.request.contextPath}/candidat/offre/${o.id}" class="btn btn-sm btn-primary">Voir détails</a>
     </div>
     <div class="card-footer small text-muted">Publiée le ${f:dateTime(o.datePublication)}</div>
    </div>
   </div>
  </c:forEach>
 </div>
</div>
</body></html>
```

#### `jsp/candidat/detail.jsp`
Détail d'une offre + historique des validations.

**Contenu du fichier :**
```jsp
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="f" uri="/WEB-INF/tld/functions.tld" %>
<!DOCTYPE html>
<html lang="fr">
<head><meta charset="UTF-8"><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><title>Détail offre</title></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/jsp/fragments/navbar.jsp"/>
<div class="container">
 <a href="${pageContext.request.contextPath}/candidat/offres" class="btn btn-sm btn-secondary mb-3">&larr; Retour aux offres</a>
 <div class="card shadow-sm">
  <div class="card-header">${offre.poste.intitule}</div>
  <div class="card-body">
   <p>${offre.poste.description}</p>
   <ul>
    <li>Département: <span>${offre.departement.nom}</span></li>
    <li>Exp: <span>${offre.anneesExperience}</span> ans</li>
    <li>Places: <span>${offre.nbPlaces}</span></li>
    <li>Salaire: <strong>${offre.salaire}</strong></li>
    <li>Statut: <span class="badge bg-success">${offre.statut.libelle}</span></li>
   </ul>
   <div class="small text-muted">Validations :</div>
   <ul class="small">
    <c:forEach var="v" items="${validations}">
     <li>${v.roleValideur.code} - ${v.decision.libelle} le ${f:dateTime(v.decidedAt)}<c:if test="${not empty v.commentaire}"> (${v.commentaire})</c:if></li>
    </c:forEach>
   </ul>
   <button class="btn btn-primary" disabled>Postuler (à brancher sur candidature)</button>
  </div>
 </div>
</div>
</body></html>
```

---

### 9.18 `test/ProjectApplicationTests.java`

**Rôle** : test de fumée. `@SpringBootTest` démarre le contexte ; `contextLoads()` vérifie
que l'application se charge sans erreur.

**Contenu du fichier :**
```java
package com.project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
```

---

## 10. Cycle de vie d'une requête HTTP

Exemple : **connexion** puis **accès au dashboard manager**.

1. Le navigateur `POST /login` avec `email` + `password`.
2. `AuthController.doLogin` appelle `AuthService.authenticate(...)`.
3. `AuthService` charge l'utilisateur (`UtilisateurRepository.findByEmail`) et compare le
   mot de passe (BCrypt) → succès.
4. `AuthService.login` remplit la **session** (`userId`, `userRole`, …).
5. `redirectByRole("MANAGER")` renvoie `redirect:/manager/dashboard`.
6. Le navigateur fait `GET /manager/dashboard`.
7. `AuthInterceptor.preHandle` s'exécute **avant** le contrôleur : session + rôle OK
   (sinon redirection).
8. `ManagerController.dashboard` place les données dans le `Model` et retourne
   `"manager/dashboard"`.
9. Le **ViewResolver** combine `prefix` + nom + `suffix` →
   `/WEB-INF/jsp/manager/dashboard.jsp`.
10. **Jasper** compile la JSP, JSTL/EL produisent le HTML, renvoyé au navigateur.

---

## 11. Parcours complet du workflow

Enchaînement des URLs (toutes en `POST` sauf les pages) :

| Étape | Rôle | Requête | Effet |
|-------|------|---------|-------|
| 1 | Manager | `POST /manager/offre` (`posteId`, `anneesExperience`, `nbPlaces`) | crée l'offre, statut `EN_ATTENTE_RH` |
| 1bis | Manager | `POST /manager/demande-poste` (`intitule`, `justification`) | crée une demande de poste `EN_ATTENTE` |
| 2a | RH | `POST /rh/demande/{id}/valider` | crée le `Poste` (ajouté au dropdown) |
| 2b | RH | `POST /rh/offre/{id}/valider` (`posteId`, `anneesExperience`, `nbPlaces`, `salaire`) | fixe le salaire, statut `EN_ATTENTE_FINANCE` |
| 3 | Finance | `POST /finance/offre/{id}/valider` (`salaire`) | modifie/valide le prix, statut `EN_ATTENTE_DIRECTION` |
| 4 | Direction | `POST /direction/offre/{id}/valider` | statut `PUBLIEE` + `datePublication` |
| 5 | Candidat | `GET /candidat/offres` | voit l'offre publiée |

À chaque étape, une ligne est ajoutée dans `offre_validation` (historique) ; à chaque
refus, le statut passe à `REFUSEE_RH` / `REFUSEE_FINANCE` / `REFUSEE_DIRECTION`.

---

## 12. Sécurité

- **Mots de passe** : hachés en **BCrypt** (`spring-security-crypto`).
- **Session** : authentification via la session HTTP (`JSESSIONID`) ; attributs
  `userId` / `userRole`.
- **Autorisation** : `AuthInterceptor` vérifie le rôle par préfixe d'URL. Un candidat qui
  tente `/manager/dashboard` est renvoyé vers `/login?error=role`.
- **Portée** : ce n'est **pas** Spring Security complet (pas de CSRF, pas de
  `@PreAuthorize`). Contrôle d'accès pragmatique, suffisant pour ce projet.

---

## 13. Problèmes fréquents

| Symptôme | Cause | Solution |
|----------|-------|----------|
| `switch` : `';' expected` / `illegal start of expression` | Compilation avec **Java 8** | Utiliser Java 21 (ou `./deploy.sh`) |
| `Port 8080 was already in use` / `Process terminated with exit code: 1` | Un processus occupe le port | `ss -ltnp \| grep :8080` puis `kill <PID>` |
| Page **500** avec `JasperException` / `Unable to compile class for JSP` | Erreur de syntaxe JSP/EL ou taglib | Lire la stack dans la console, corriger la JSP |
| `The absolute uri: jakarta.tags.core cannot be resolved` | JSTL absent du classpath | Vérifier les dépendances JSTL dans `pom.xml` |
| `java.lang.NoClassDefFoundError … jasper` | Moteur JSP manquant | `tomcat-embed-jasper` présent ; relancer `./deploy.sh build` |
| JSP non trouvée (`404` sur la vue) | Nom/emplacement du `.jsp` | Respecter `prefix`/`suffix` et l'arborescence `WEB-INF/jsp/...` |
| `AUTO_INCREMENT` : erreur de syntaxe au démarrage | Non supporté par H2 en mode PostgreSQL | `GENERATED BY DEFAULT AS IDENTITY` |
| Les modifications de JSP ne s'affichent pas | JSP déjà compilée en cache | Redémarrer l'application (ou recompiler) |

---

## 14. Pistes d'évolution

1. **Candidature** : brancher le bouton « Postuler » (tables `cv`, `candidature` prêtes).
2. **Questionnaire / entretien / conditions de travail / contrat** : tables présentes,
   reste à créer entités/repositories/services/contrôleurs + JSP.
3. **Notifications** : table prête ; notifier chaque acteur.
4. **Profil production** : `application-prod.properties` (Postgres + `sql/schema.sql`) et
   migrations (Flyway/Liquibase).
5. **Spring Security complet** : CSRF, `@PreAuthorize`.
6. **Suivi temps réel côté manager** : afficher l'étape courante de chaque offre.

---

*Fin du document.*
