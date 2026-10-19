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
