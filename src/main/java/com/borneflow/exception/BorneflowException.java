package com.borneflow.exception;

/**
 * Exception de base pour toutes les erreurs métier du projet BorneFlow.
 *
 * <p>Toute exception métier doit étendre cette classe ou être de ce type.
 * Cette convention permet :</p>
 * <ul>
 *   <li>Une gestion uniforme des erreurs au niveau des couches supérieures</li>
 *   <li>Une distinction claire entre les erreurs métier et les erreurs techniques</li>
 *   <li>L'application d'une politique de logging cohérente</li>
 * </ul>
 *
 * <p>Inspirée du pattern {@code psdi.util.MXException} de Maximo, sans la dépendance
 * propriétaire IBM.</p>
 */
public class BorneflowException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Code d'erreur fonctionnel (ex: "BF_STATION_OFFLINE", "BF_TECH_NOT_FOUND") */
    private final String errorCode;

    /** Identifiant éventuel de l'entité concernée (sans donnée sensible) */
    private final String entityId;

    public BorneflowException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.entityId = null;
    }

    public BorneflowException(String errorCode, String message, String entityId) {
        super(message);
        this.errorCode = errorCode;
        this.entityId = entityId;
    }

    public BorneflowException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.entityId = null;
    }

    public BorneflowException(String errorCode, String message, String entityId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.entityId = entityId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getEntityId() {
        return entityId;
    }

    @Override
    public String toString() {
        return "BorneflowException[code=" + errorCode
                + ", entityId=" + entityId
                + ", message=" + getMessage() + "]";
    }
}
