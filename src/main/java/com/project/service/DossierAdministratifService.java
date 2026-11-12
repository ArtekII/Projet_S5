package com.project.service;

import com.project.entity.*;
import com.project.repository.*;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DossierAdministratifService {
    public static final long TAILLE_MAX = 10 * 1024 * 1024;
    public static final List<String> TYPES_DOCUMENT = List.of("CV", "IDENTITE", "CONTRAT", "DIPLOME", "ATTESTATION", "AUTRE");
    private final UtilisateurRepository utilisateurs;
    private final EmployeRepository employes;
    private final DossierAdministratifRepository dossiers;
    private final DocumentAdministratifRepository documents;
    private final ParcoursProfessionnelRepository parcours;
    private final JournalDossierRepository journal;
    private final CvRepository cvs;
    private final CvCandidatRepository cvsCandidats;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Validator validator;

    public DossierAdministratifService(UtilisateurRepository utilisateurs, EmployeRepository employes,
            DossierAdministratifRepository dossiers, DocumentAdministratifRepository documents,
            ParcoursProfessionnelRepository parcours, JournalDossierRepository journal,
            CvRepository cvs, CvCandidatRepository cvsCandidats, JdbcTemplate jdbc, Clock clock, Validator validator) {
        this.utilisateurs = utilisateurs; this.employes = employes; this.dossiers = dossiers;
        this.documents = documents; this.parcours = parcours; this.journal = journal;
        this.cvs = cvs; this.cvsCandidats = cvsCandidats; this.jdbc = jdbc; this.clock = clock; this.validator = validator;
    }

    private Utilisateur acteur(Integer id) {
        Utilisateur acteur = utilisateurs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!Boolean.TRUE.equals(acteur.getActif())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return acteur;
    }

    private boolean estRh(Utilisateur acteur) {
        return acteur.getRole() != null && "RH".equals(acteur.getRole().getCode());
    }

    private Utilisateur autoriser(Integer cible, Utilisateur acteur, boolean rhSeulement) {
        if (!estRh(acteur) && (rhSeulement || !acteur.getId().equals(cible))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return utilisateurs.findById(cible).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void verrouiller(Integer cible) {
        // Serialize aussi la premiere creation du dossier et les mises a jour de la checklist.
        jdbc.queryForObject("select id from utilisateur where id = ? for update", Integer.class, cible);
    }

    private void tracer(Utilisateur cible, Utilisateur acteur, String action, String detail) {
        JournalDossier entree = new JournalDossier();
        entree.setUtilisateur(cible); entree.setActeur(acteur); entree.setAction(action);
        entree.setDetail(detail); entree.setDateAction(LocalDateTime.now(clock)); journal.save(entree);
    }

    public List<Map<String, Object>> lister(Integer acteurId) {
        Utilisateur acteur = acteur(acteurId);
        autoriser(acteurId, acteur, true);
        LocalDate aujourdHui = LocalDate.now(clock);
        tracer(acteur, acteur, "LISTE_DOSSIERS", "Consultation de la liste des dossiers administratifs");
        return jdbc.queryForList("""
                select u.id, u.email, d.nom, d.prenom, e.id_employe as employeId,
                    (select count(*) from document_administratif doc where doc.id_utilisateur = u.id
                        and doc.date_expiration < ?) as expires,
                    (select count(*) from document_administratif doc where doc.id_utilisateur = u.id
                        and doc.date_expiration between ? and ?) as aRenouveler
                from utilisateur u
                left join dossier_administratif d on d.id_utilisateur = u.id
                left join employe e on e.id_utilisateur = u.id
                order by u.email, u.id
                """, aujourdHui, aujourdHui, aujourdHui.plusDays(30));
    }

    public Map<String, Object> consulter(Integer cibleId, Integer acteurId) {
        Utilisateur acteur = acteur(acteurId);
        Utilisateur cible = autoriser(cibleId, acteur, false);
        tracer(cible, acteur, "CONSULTATION", "Consultation du dossier administratif");
        Map<String, Object> vue = new HashMap<>();
        vue.put("titulaire", cible);
        vue.put("gestionRh", estRh(acteur));
        vue.put("fiche", dossiers.findByUtilisateur_Id(cibleId).orElseGet(() -> preRemplir(cible)));
        vue.put("employe", employes.findByUtilisateurId(cibleId).orElse(null));
        vue.put("cvs", cvs.findByCandidat_Id(cibleId));
        vue.put("cvsCandidats", cvsCandidats.findByCandidat_Id(cibleId));
        vue.put("parcours", parcours.findByUtilisateur_IdOrderByDateDebutDescIdDesc(cibleId));
        // Le contenu binaire n'est jamais charge lors du simple affichage de la liste.
        List<Map<String, Object>> pieces = jdbc.queryForList("""
                select id, type, nom_fichier as nomFichier, date_delivrance as dateDelivrance,
                    date_expiration as dateExpiration, depose_le as deposeLe
                from document_administratif where id_utilisateur = ? order by depose_le desc, id desc
                """, cibleId);
        for (Map<String, Object> piece : pieces) {
            java.sql.Date expiration = (java.sql.Date) piece.get("dateExpiration");
            piece.put("validite", validite(expiration == null ? null : expiration.toLocalDate()));
        }
        vue.put("documents", pieces);
        vue.put("contrats", jdbc.queryForList("""
                select c.id, tc.code as typeCode, c.date_debut as dateDebut, c.date_fin as dateFin,
                    c.statut, ct.salaire, ct.lieu_travail as lieuTravail,
                    p.intitule as poste, (c.id = e.id_contrat_actuel) as actuel
                from employe e join contrat c on c.id_employe = e.id_employe or c.id = e.id_contrat_actuel
                join type_contrat tc on tc.id = c.id_type_contrat
                join condition_travail ct on ct.id = c.id_condition
                left join candidature ca on ca.id = ct.id_candidature
                left join offre_emploi o on o.id = ca.id_offre
                left join poste p on p.id = coalesce(ct.id_poste, o.id_poste)
                where e.id_utilisateur = ? order by c.date_debut desc nulls last, c.id desc
                """, cibleId));
        if (estRh(acteur)) vue.put("journal", journal.findTop100ByUtilisateur_IdOrderByIdDesc(cibleId));
        return vue;
    }

    private DossierAdministratif preRemplir(Utilisateur cible) {
        DossierAdministratif fiche = new DossierAdministratif();
        fiche.setEmailPersonnel(cible.getEmail());
        var versions = cvs.findByCandidat_Id(cible.getId());
        if (!versions.isEmpty()) {
            Cv cv = versions.stream().max(Comparator.comparing(Cv::getId)).orElseThrow();
            fiche.setNom(cv.getNom()); fiche.setPrenom(cv.getPrenom()); fiche.setDateNaissance(cv.getDateNaissance());
        } else {
            var candidatures = cvsCandidats.findByCandidat_Id(cible.getId());
            if (!candidatures.isEmpty()) {
                CvCandidat cv = candidatures.stream().max(Comparator.comparing(CvCandidat::getId)).orElseThrow();
                fiche.setNom(cv.getNom()); fiche.setPrenom(cv.getPrenom()); fiche.setDateNaissance(cv.getDateNaissance());
            }
        }
        return fiche;
    }

    public String validite(LocalDate expiration) {
        if (expiration == null) return "SANS_ECHEANCE";
        LocalDate aujourdHui = LocalDate.now(clock);
        if (expiration.isBefore(aujourdHui)) return "EXPIRE";
        return expiration.isAfter(aujourdHui.plusDays(30)) ? "VALIDE" : "A_RENOUVELER";
    }

    private void valider(Object saisie) {
        if (!validator.validate(saisie).isEmpty()) throw new IllegalArgumentException("Les renseignements sont incomplets ou invalides.");
    }

    public void enregistrerFiche(Integer cibleId, Integer acteurId, DossierAdministratif saisie) {
        Utilisateur acteur = acteur(acteurId);
        Utilisateur cible = autoriser(cibleId, acteur, false);
        valider(saisie); verrouiller(cibleId);
        DossierAdministratif fiche = dossiers.findByUtilisateur_Id(cibleId).orElseGet(DossierAdministratif::new);
        if (!Objects.equals(fiche.getRevision(), saisie.getRevision())) {
            throw new IllegalArgumentException("Le dossier a été modifié. Rechargez la page avant de réessayer.");
        }
        List<String> modifies = new ArrayList<>();
        var destination = new org.springframework.beans.BeanWrapperImpl(fiche);
        var source = new org.springframework.beans.BeanWrapperImpl(saisie);
        // Liste explicite : aucun champ de rattachement ou de checklist ne peut etre modifie par ce formulaire.
        for (String champ : List.of("nom", "prenom", "dateNaissance", "lieuNaissance", "nationalite", "numeroIdentite",
                "adresse", "telephone", "emailPersonnel", "situationMatrimoniale", "nombreEnfants", "contactUrgence", "telephoneUrgence")) {
            Object valeur = source.getPropertyValue(champ);
            if (!Objects.equals(destination.getPropertyValue(champ), valeur)) modifies.add(champ);
            destination.setPropertyValue(champ, valeur);
        }
        fiche.setUtilisateur(cible); fiche.setModifieLe(LocalDateTime.now(clock));
        dossiers.saveAndFlush(fiche);
        tracer(cible, acteur, "MODIFICATION_FICHE", "Champs modifiés : " + String.join(", ", modifies));
    }

    public Integer deposer(Integer cibleId, Integer acteurId, String type, MultipartFile fichier,
            LocalDate delivrance, LocalDate expiration) throws IOException {
        Utilisateur acteur = acteur(acteurId);
        Utilisateur cible = autoriser(cibleId, acteur, false);
        if (!TYPES_DOCUMENT.contains(type)) throw new IllegalArgumentException("Type de document invalide.");
        if (fichier == null || fichier.isEmpty() || fichier.getSize() > TAILLE_MAX) {
            throw new IllegalArgumentException("Choisissez un fichier PDF, JPEG ou PNG de 10 Mo maximum.");
        }
        verifierDates(delivrance, expiration);
        String nom = Optional.ofNullable(fichier.getOriginalFilename()).orElse("").replace('\\', '/');
        nom = nom.substring(nom.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        if (nom.isBlank() || nom.length() > 255) throw new IllegalArgumentException("Nom de fichier invalide.");
        byte[] contenu = fichier.getBytes();
        if (contenu.length == 0 || contenu.length > TAILLE_MAX) throw new IllegalArgumentException("Taille de fichier invalide.");
        String mime = detecterType(contenu, nom);
        DocumentAdministratif document = new DocumentAdministratif();
        document.setUtilisateur(cible); document.setDeposant(acteur); document.setType(type);
        document.setNomFichier(nom); document.setTypeMime(mime); document.setContenu(contenu);
        document.setDateDelivrance(delivrance); document.setDateExpiration(expiration);
        document.setDeposeLe(LocalDateTime.now(clock)); documents.saveAndFlush(document);
        tracer(cible, acteur, "DEPOT_DOCUMENT", "Document n° " + document.getId() + " (" + type + ") : " + nom);
        return document.getId();
    }

    private String detecterType(byte[] contenu, String nom) {
        String extension = nom.toLowerCase(Locale.ROOT);
        if (extension.endsWith(".pdf") && contenu.length >= 5
                && "%PDF-".equals(new String(contenu, 0, 5, StandardCharsets.US_ASCII))) return "application/pdf";
        if ((extension.endsWith(".jpg") || extension.endsWith(".jpeg")) && contenu.length >= 3
                && (contenu[0] & 255) == 255 && (contenu[1] & 255) == 216 && (contenu[2] & 255) == 255) return "image/jpeg";
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        if (extension.endsWith(".png") && contenu.length >= 8 && Arrays.equals(png, Arrays.copyOf(contenu, 8))) return "image/png";
        throw new IllegalArgumentException("Seuls les fichiers PDF, JPEG et PNG correspondant à leur extension sont acceptés.");
    }

    private void verifierDates(LocalDate delivrance, LocalDate expiration) {
        if (delivrance != null && delivrance.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("La date de délivrance ne peut pas être future.");
        }
        if (delivrance != null && expiration != null && expiration.isBefore(delivrance)) {
            throw new IllegalArgumentException("L'expiration doit suivre la délivrance.");
        }
    }

    public DocumentAdministratif telecharger(Integer cibleId, Integer acteurId, Integer documentId) {
        Utilisateur acteur = acteur(acteurId);
        Utilisateur cible = autoriser(cibleId, acteur, false);
        DocumentAdministratif document = documents.findById(documentId)
                .filter(d -> d.getUtilisateur().getId().equals(cibleId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        tracer(cible, acteur, "TELECHARGEMENT", "Document n° " + documentId + " : " + document.getNomFichier());
        return document;
    }

    public void modifierValidite(Integer cibleId, Integer acteurId, Integer documentId, LocalDate delivrance, LocalDate expiration) {
        Utilisateur acteur = acteur(acteurId);
        Utilisateur cible = autoriser(cibleId, acteur, true);
        verifierDates(delivrance, expiration); verrouiller(cibleId);
        DocumentAdministratif document = documents.findById(documentId)
                .filter(d -> d.getUtilisateur().getId().equals(cibleId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String avant = "délivrance=" + document.getDateDelivrance() + ", expiration=" + document.getDateExpiration();
        document.setDateDelivrance(delivrance); document.setDateExpiration(expiration);
        tracer(cible, acteur, "VALIDITE_DOCUMENT", "Document n° " + documentId + " : " + avant
                + " → délivrance=" + delivrance + ", expiration=" + expiration);
    }

    public void ajouterParcours(Integer cibleId, Integer acteurId, ParcoursProfessionnel saisie) {
        Utilisateur acteur = acteur(acteurId);
        Utilisateur cible = autoriser(cibleId, acteur, true);
        valider(saisie);
        if (employes.findByUtilisateurId(cibleId).isEmpty()) throw new IllegalArgumentException("Ce dossier n'est pas encore rattaché à un employé.");
        if (saisie.getDateFin() != null && saisie.getDateFin().isBefore(saisie.getDateDebut())) {
            throw new IllegalArgumentException("La fin doit suivre le début de la période.");
        }
        if ("SALAIRE".equals(saisie.getType()) && saisie.getMontant() == null) {
            throw new IllegalArgumentException("Le montant du salaire est obligatoire.");
        }
        if (!"SALAIRE".equals(saisie.getType()) && saisie.getMontant() != null) {
            throw new IllegalArgumentException("Le montant concerne uniquement les salaires.");
        }
        ParcoursProfessionnel entree = new ParcoursProfessionnel();
        entree.setUtilisateur(cible); entree.setType(saisie.getType()); entree.setLibelle(saisie.getLibelle());
        entree.setDateDebut(saisie.getDateDebut()); entree.setDateFin(saisie.getDateFin());
        entree.setMontant(saisie.getMontant()); entree.setDescription(saisie.getDescription());
        entree.setCreeLe(LocalDateTime.now(clock)); parcours.saveAndFlush(entree);
        tracer(cible, acteur, "AJOUT_PARCOURS", "Entrée n° " + entree.getId() + " (" + entree.getType() + ") : " + entree.getLibelle());
    }

    public void enregistrerIntegration(Integer cibleId, Integer acteurId, Long revision, Set<String> etapes) {
        Utilisateur acteur = acteur(acteurId);
        Utilisateur cible = autoriser(cibleId, acteur, true);
        if (employes.findByUtilisateurId(cibleId).isEmpty()) throw new IllegalArgumentException("Employé introuvable.");
        if (!Set.of("signature", "badge", "compte", "materiel", "formation", "paie").containsAll(etapes)) {
            throw new IllegalArgumentException("Étape d'intégration invalide.");
        }
        verrouiller(cibleId);
        DossierAdministratif fiche = dossiers.findByUtilisateur_Id(cibleId)
                .orElseThrow(() -> new IllegalArgumentException("Complétez d'abord la fiche de renseignements."));
        if (!Objects.equals(revision, fiche.getRevision())) throw new IllegalArgumentException("Le dossier a été modifié. Rechargez la page.");
        List<String> modifications = new ArrayList<>();
        var bean = new org.springframework.beans.BeanWrapperImpl(fiche);
        for (String etape : List.of("signature", "badge", "compte", "materiel", "formation", "paie")) {
            boolean valeur = etapes.contains(etape);
            if (!Objects.equals(bean.getPropertyValue(etape), valeur)) modifications.add(etape + "=" + valeur);
            bean.setPropertyValue(etape, valeur);
        }
        fiche.setModifieLe(LocalDateTime.now(clock)); dossiers.saveAndFlush(fiche);
        tracer(cible, acteur, "CHECKLIST_INTEGRATION", String.join(", ", modifications));
    }
}
