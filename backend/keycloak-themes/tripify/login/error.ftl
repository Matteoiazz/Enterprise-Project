<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Qualcosa è andato storto | Tripify</title>
    <style>
        :root{
            --tf-green:#1B8A5A;
            --tf-dark-green:#0B3D2E;
            --tf-bg:#F7F5EF;
            --tf-white:#FFFFFF;
            --tf-gray:#8A8F8C;
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
        p{ font-size:14.5px; color:var(--tf-gray); line-height:1.6; overflow-wrap:anywhere; word-break:break-word; }
        .tf-notice{
            margin-top:24px;
            padding:14px 16px;
            background:var(--tf-bg);
            border-radius:var(--tf-radius-sm);
            font-size:13.5px;
            color:var(--tf-dark-green);
            font-weight:600;
        }
        .tf-btn{
            display:inline-block;
            width:100%;
            margin-top:24px;
            padding:14px;
            border-radius:var(--tf-radius-sm);
            background:var(--tf-green);
            color:var(--tf-white);
            font-size:15px;
            font-weight:700;
            text-decoration:none;
            transition:background .2s ease;
        }
        .tf-btn:hover{ background:var(--tf-dark-green); }
    </style>
</head>
<body>
<div class="tf-card">
    <div class="tf-icon">⚠️</div>
    <h2>Qualcosa è andato storto</h2>
    <p>${kcSanitize(message.summary)?no_esc}</p>

    <#if client?? && client.baseUrl?has_content>
        <p><a href="${client.baseUrl}" class="tf-btn">Torna a Tripify</a></p>
    <#else>
        <div class="tf-notice">📱 Puoi chiudere questa pagina e riprovare dall'app.</div>
    </#if>
</div>
</body>
</html>
