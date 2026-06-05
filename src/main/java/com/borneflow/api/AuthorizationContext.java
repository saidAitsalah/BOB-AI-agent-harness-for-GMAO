package com.borneflow.api;

/**
 * Interface d'autorisation pour les ressources REST.
 *
 * <p>Toute méthode publique exposée par l'API doit vérifier les droits
 * d'accès via cette interface avant d'exécuter son traitement.</p>
 *
 * <p>L'implémentation concrète est injectée selon le contexte (Maximo Security
 * en production, mock en test).</p>
 */
public interface AuthorizationContext {

    /** Vérifie si l'utilisateur courant peut lire une ressource. */
    boolean canRead(String resourceType, String resourceId);

    /** Vérifie si l'utilisateur courant peut créer une ressource du type donné. */
    boolean canCreate(String resourceType);

    /** Vérifie si l'utilisateur courant peut modifier une ressource. */
    boolean canUpdate(String resourceType, String resourceId);

    /** Vérifie si l'utilisateur courant peut supprimer une ressource. */
    boolean canDelete(String resourceType, String resourceId);
}
