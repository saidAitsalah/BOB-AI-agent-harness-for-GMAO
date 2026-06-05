package com.borneflow.mbo;

import com.borneflow.exception.BorneflowException;

/**
 * MBO représentant un technicien de maintenance (équivalent {@code Person/Labor} en Maximo).
 *
 * <p>Note : le matricule technique ({@code matricule}) est une donnée à traiter avec
 * précaution. Il <strong>ne doit jamais être logué en clair</strong> (cf.
 * {@code .bob/rules/security-rules.md}).</p>
 */
public class BfTechnicianMbo extends BfMbo {

    public enum Skill {
        ELECTRICAL,    // habilitation électrique
        MECHANICAL,    // maintenance mécanique
        SOFTWARE,      // mise à jour firmware
        NETWORK        // diagnostic réseau / télémétrie
    }

    @Override
    public void add() throws BorneflowException {
        super.add();
        setAttribute("active", true);
    }

    @Override
    public void validate() throws BorneflowException {
        var matricule = (String) getAttribute("matricule");
        if (matricule == null || matricule.isBlank()) {
            throw new BorneflowException(
                "BF_TECH_INVALID",
                "Le matricule technicien est obligatoire");
        }

        var displayName = (String) getAttribute("displayName");
        if (displayName == null || displayName.isBlank()) {
            throw new BorneflowException(
                "BF_TECH_INVALID",
                "Le nom d'affichage est obligatoire");
        }
    }

    public boolean hasSkill(Skill skill) {
        var skills = getAttribute("skills");
        if (skills instanceof java.util.Set<?> set) {
            return set.contains(skill);
        }
        return false;
    }
}
