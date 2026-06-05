package com.borneflow.mbo;

import com.borneflow.exception.BorneflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe de base pour tous les MBO du projet BorneFlow.
 *
 * <p>Simule le pattern {@code psdi.mbo.Mbo} de Maximo : un MBO est un objet métier
 * qui encapsule les données ET le comportement d'une entité, avec des hooks de cycle
 * de vie.</p>
 *
 * <p>Cette classe est volontairement simplifiée par rapport au vrai Mbo Maximo
 * (qui dépend de jars propriétaires IBM non redistribuables). Elle expose les
 * mêmes hooks principaux pour rester fidèle au pattern.</p>
 *
 * <h2>Hooks de cycle de vie</h2>
 * <ul>
 *   <li>{@link #add()} — appelé à la création, pour initialiser les valeurs par défaut</li>
 *   <li>{@link #validate()} — appelé avant {@link #save()}, pour vérifier les règles métier</li>
 *   <li>{@link #save()} — appelé lors de la persistance</li>
 *   <li>{@link #delete()} — appelé lors de la suppression</li>
 * </ul>
 */
public abstract class BfMbo {

    private static final Logger log = LoggerFactory.getLogger(BfMbo.class);

    /** Attributs de l'objet, stockés génériquement comme dans Maximo. */
    protected final Map<String, Object> attributes = new HashMap<>();

    /** Indique si l'objet est nouveau (jamais persisté). */
    protected boolean isNew = true;

    /** Indique si l'objet a été modifié depuis le dernier save. */
    protected boolean isModified = false;

    /**
     * Hook appelé à la création de l'objet. Les sous-classes peuvent l'overrider
     * pour initialiser des valeurs par défaut.
     *
     * @throws BorneflowException si l'initialisation échoue
     */
    public void add() throws BorneflowException {
        log.debug("MBO add() — initialisation par défaut, type={}", getClass().getSimpleName());
        setAttribute("createdAt", Instant.now());
    }

    /**
     * Hook de validation appelé avant la persistance.
     * Les sous-classes doivent l'overrider pour appliquer leurs règles métier.
     *
     * @throws BorneflowException si la validation échoue
     */
    public void validate() throws BorneflowException {
        // Implémentation par défaut : ne fait rien.
        // Les sous-classes doivent override.
    }

    /**
     * Hook de persistance. Appelle {@link #validate()} puis simule la persistance.
     *
     * @throws BorneflowException si la sauvegarde échoue
     */
    public void save() throws BorneflowException {
        validate();
        setAttribute("updatedAt", Instant.now());
        isNew = false;
        isModified = false;
        log.info("MBO save() — type={}, id={}", getClass().getSimpleName(), getAttribute("id"));
    }

    /**
     * Hook de suppression.
     *
     * @throws BorneflowException si la suppression échoue
     */
    public void delete() throws BorneflowException {
        log.info("MBO delete() — type={}, id={}", getClass().getSimpleName(), getAttribute("id"));
    }

    /** Lit un attribut générique. */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /** Écrit un attribut générique et marque l'objet comme modifié. */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
        isModified = true;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isModified() {
        return isModified;
    }
}
