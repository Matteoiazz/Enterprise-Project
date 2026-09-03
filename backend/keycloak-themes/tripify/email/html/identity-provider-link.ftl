<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
</head>
<body style="margin: 0; padding: 40px 20px; background-color: #F7F5EF; font-family: 'Segoe UI', Helvetica, Arial, sans-serif;">
<div style="max-width: 500px; margin: 0 auto; background-color: #FFFFFF; border-radius: 24px; padding: 40px 28px; text-align: center; box-shadow: 0 10px 25px rgba(11,61,46,0.1); word-break: break-word; overflow-wrap: anywhere;">
    <div style="font-size: 40px; margin-bottom: 20px;">🔗</div>
    <h2 style="color: #0B3D2E; font-size: 23px; font-weight: 900; margin-top: 0;">Collega il tuo account Google</h2>
    <p style="color: #8A8F8C; font-size: 15px; line-height: 1.6; margin-bottom: 30px; word-break: break-word; overflow-wrap: anywhere;">
        Hai chiesto di collegare l'accesso con <strong>${identityProviderDisplayName}</strong> (${identityProviderContext.username}) al tuo account Tripify. Clicca sul pulsante qui sotto per confermare il collegamento.
    </p>
    <a href="${link}" style="display: inline-block; background-color: #1B8A5A; color: #FFFFFF; text-decoration: none; padding: 16px 32px; border-radius: 14px; font-weight: bold; font-size: 16px;">Collega account</a>
    <p style="color: #B7BBB2; font-size: 12px; margin-top: 30px;">
        Questo link ha una validita limitata (${linkExpirationFormatter(linkExpiration)}).<br>Se non hai richiesto tu il collegamento, ignora pure questa email: il tuo account resta invariato.
    </p>
</div>
</body>
</html>
