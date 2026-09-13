# Format de sortie (obligatoire)

Structurez votre réponse finale en exactement ces sections :

1. **Plan** - une liste ordonnée des étapes de migration.
2. **Risques** - une entrée par risque, chacune associée à la sous-tâche qui l'a identifiée (par exemple « selon dependency-analyzer : », « selon security-reviewer : »).
3. **Points de contrôle humains** - liste explicite des étapes nécessitant une validation humaine avant exécution, et pourquoi.
4. **Traçabilité** - pour chaque étape du Plan, indiquer de quelle sous-tâche (ou de quel ticket) elle est issue. Une étape sans source nommée n'est pas acceptable.
5. **Réversibilité** - pour chaque étape, préciser si elle est réversible (par exemple via `git revert`) et comment.

Ne pas omettre les sections 4 et 5, même lorsque la réponse est courte (par exemple « toutes les étapes sont réversibles via git revert »).
