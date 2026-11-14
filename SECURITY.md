# Sécurité et divulgation responsable

**Ne publiez pas de vulnérabilité exploitable avec des données sensibles dans une issue publique.**
Contactez le mainteneur depuis son profil GitHub pour convenir d'un canal privé si nécessaire, ou utilisez les signalements privés GitHub s'ils sont activés sur ce dépôt.

N'ajoutez jamais : tokens, clés API, mots de passe, certificats, keystores, fichiers `local.properties`, données utilisateur, journaux complets avec chemins ou noms personnels.

Le serveur Python doit rester lié à **127.0.0.1**, et la passerelle WebView ↔ Android doit valider les données entrantes. Les flux radio et métadonnées proviennent de sources externes non fiables.

La permission `RECORD_AUDIO` est utilisée pour la visualisation via l'API Android `Visualizer`, pas pour enregistrer le microphone ; tout changement à cet usage devra être expliqué et revu.
