<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Controlla la tua email | Tripify</title>
    <style>
        :root{
            --tf-green:#1B8A5A;
            --tf-dark-green:#0B3D2E;
            --tf-bg:#F7F5EF;
            --tf-white:#FFFFFF;
            --tf-gray:#8A8F8C;
            --tf-light-gray:#E7E4DC;
            --tf-radius-sm:14px;
            font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
        }
        *{ box-sizing:border-box; margin:0; padding:0; }
        html,body{ max-width:100%; overflow-x:hidden; }
        body{
            min-height:100vh;
            display:flex;
            align-items:center;
            justify-content:center;
            background:var(--tf-bg);
            padding:24px;
        }
        .tf-card{
            width:100%;
            max-width:480px;
            background:var(--tf-white);
            border-radius:24px;
            padding:40px 28px;
            text-align:center;
            box-shadow:0 25px 50px -12px rgba(11,61,46,0.2);
            overflow-wrap:anywhere;
            word-break:break-word;
        }
        .tf-icon{ font-size:48px; margin-bottom:16px; display:inline-block; }
        h2{ font-size:23px; font-weight:900; color:var(--tf-dark-green); margin-bottom:12px; }
        p{ font-size:14.5px; color:var(--tf-gray); line-height:1.6; margin-bottom:10px; }
        .tf-mail{
            display:block;
            margin:4px 0 2px;
            color:var(--tf-dark-green);
            font-weight:700;
            overflow-wrap:anywhere;
            word-break:break-all;
        }
        .tf-btn{
            display:block;
            width:100%;
            margin-top:22px;
            padding:14px;
            border:none;
            border-radius:var(--tf-radius-sm);
            background:var(--tf-green);
            color:var(--tf-white);
            font-size:15px;
            font-weight:700;
            font-family:inherit;
            cursor:pointer;
            text-decoration:none;
            transition:background .2s ease;
        }
        .tf-btn:hover{ background:var(--tf-dark-green); }
        .tf-resend{
            display:inline-block;
            margin-top:18px;
            color:var(--tf-green);
            font-size:13px;
            font-weight:700;
            text-decoration:underline;
        }
    </style>
</head>
<body>
<div class="tf-card">
    <div class="tf-icon">✉️</div>
    <h2>Controlla la tua posta</h2>
    <p>Abbiamo inviato a
        <span class="tf-mail">${brokerContext.username}</span>
        un'email con il link per collegare l'accesso con ${idpDisplayName} al tuo account Tripify.</p>
    <p>Apri il link dall'email, poi torna al login e accedi di nuovo con ${idpDisplayName} per completare il collegamento.</p>
    <a href="${url.loginRestartFlowUrl}" class="tf-btn">Torna al login</a>
    <a href="${url.loginAction}" class="tf-resend">Non hai ricevuto l'email? Invia di nuovo</a>
</div>
</body>
</html>
