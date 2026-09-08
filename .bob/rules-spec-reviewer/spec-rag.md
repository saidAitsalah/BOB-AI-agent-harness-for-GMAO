# Consultation des spécifications fonctionnelles

## Quand consulter la spécification

Appelle `rechercher_regles_bornflow` AVANT de te prononcer sur :
- le comportement attendu d'un composant (que DOIT-il faire ?)
- les droits d'accès et habilitations
- les règles métier, les états, les transitions
- la validation des flux d'intégration

N'appelle PAS cet outil pour : la syntaxe d'un langage, une question
d'architecture technique, ou lorsque la question porte uniquement sur ce que
le code fait actuellement.

## Comment citer

Cite systématiquement l'identifiant exact de la règle sur laquelle tu
t'appuies, sous la forme : « Selon BF100-RDG-011, … ».

N'invente jamais d'identifiant. Si aucune règle ne couvre le point examiné,
écris explicitement : « Aucune règle de gestion ne couvre ce point dans les
SFD indexées. » L'absence de spécification est une information utile.

## En cas d'écart entre le code et la spécification

Si le comportement observé dans le code diverge de la règle de gestion :

1. Cite la règle et son identifiant.
2. Cite l'extrait de code concerné (fichier, méthode).
3. Formule l'écart comme un CONSTAT, pas comme un verdict.

Ne tranche jamais silencieusement en faveur de l'un ou de l'autre. La
spécification peut être périmée autant que le code peut être fautif — vérifie
le champ `version` retourné par l'outil et mentionne-le si le document est
ancien.

Formulation attendue :
> Divergence : BF100-RDG-006 (v1.0) exige que toute erreur technique soit
> remontée à l'utilisateur. Or `MaintenanceOrderService.tryFindOrCreate()`
> retourne `null` dans le bloc catch, sans journalisation ni propagation.