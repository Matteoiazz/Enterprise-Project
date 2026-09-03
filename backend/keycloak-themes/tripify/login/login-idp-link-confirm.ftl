<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Collega il tuo account | Tripify</title>
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
        h2{ font-size:24px; font-weight:900; color:var(--tf-dark-green); margin-bottom:12px; }
        p{ font-size:14.5px; color:var(--tf-gray); line-height:1.6; margin-bottom:24px; }
        .tf-btn{
            display:block;
            width:100%;
            margin-top:12px;
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
    </style>
</head>
<body>
<div class="tf-card">
    <div class="tf-icon">🔗</div>
    <h2>Hai già un account Tripify</h2>
    <p>Esiste già un account registrato con questa email. Collegalo a ${idpDisplayName} per accedere più velocemente la prossima volta.</p>

    <form action="${url.loginAction}" method="post">
        <button type="submit" class="tf-btn" name="submitAction" id="linkAccount" value="linkAccount">Collega a ${idpDisplayName}</button>
    </form>
</div>
</body>
</html>
