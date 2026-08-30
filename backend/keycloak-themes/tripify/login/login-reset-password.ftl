<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Password dimenticata | Tripify</title>
    <style>
        :root{
            --tf-green:#1B8A5A;
            --tf-dark-green:#0B3D2E;
            --tf-bg:#F7F5EF;
            --tf-white:#FFFFFF;
            --tf-gray:#8A8F8C;
            --tf-light-gray:#E7E4DC;
            --tf-error:#D14343;
            --tf-success:#1B8A5A;
            --tf-radius:20px;
            --tf-radius-sm:14px;
            font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
        }
        *{ box-sizing:border-box; margin:0; padding:0; }
        body{
            min-height:100vh;
            display:flex;
            align-items:center;
            justify-content:center;
            background:var(--tf-bg);
            padding:24px;
        }
        .tf-shell{
            width:100%;
            max-width:960px;
            min-height:560px;
            background:var(--tf-white);
            border-radius:28px;
            display:flex;
            overflow:hidden;
            box-shadow:0 30px 60px -20px rgba(11,61,46,0.25);
        }
        .tf-hero{
            flex:1;
            position:relative;
            background:linear-gradient(160deg, var(--tf-green) 0%, var(--tf-dark-green) 100%);
            color:var(--tf-white);
            padding:48px 40px;
            display:flex;
            flex-direction:column;
            justify-content:space-between;
            min-width:0;
        }
        .tf-hero::before{
            content:"";
            position:absolute;
            inset:0;
            background-image:
                    radial-gradient(circle at 85% 15%, rgba(255,255,255,0.10) 0%, transparent 45%),
                    radial-gradient(circle at 10% 90%, rgba(255,255,255,0.08) 0%, transparent 40%);
            pointer-events:none;
        }
        .tf-logo{
            display:flex; align-items:center; gap:10px;
            font-weight:900; font-size:20px; letter-spacing:2px;
            position:relative; z-index:1;
        }
        .tf-logo-mark{
            width:34px; height:34px; border-radius:10px;
            background:rgba(255,255,255,0.16);
            display:flex; align-items:center; justify-content:center;
        }
        .tf-hero-copy{ position:relative; z-index:1; }
        .tf-hero-copy h1{
            font-size:32px; font-weight:900; line-height:1.25;
            margin-bottom:14px; letter-spacing:0.2px;
        }
        .tf-hero-copy p{
            font-size:15px; opacity:0.85; line-height:1.6; max-width:340px;
        }
        .tf-route{
            position:relative; z-index:1;
            display:flex; align-items:center; gap:8px;
            font-size:13px; opacity:0.75; letter-spacing:1px;
            text-transform:uppercase;
        }
        .tf-route .dot{ width:6px; height:6px; border-radius:50%; background:var(--tf-white); }
        .tf-route .line{ flex:1; height:1px; background:rgba(255,255,255,0.35); max-width:60px; }

        .tf-form-panel{
            flex:1;
            padding:48px 44px;
            display:flex;
            flex-direction:column;
            justify-content:center;
            min-width:0;
        }
        .tf-form-panel h2{
            font-size:26px; font-weight:900; color:var(--tf-dark-green);
            margin-bottom:6px;
        }
        .tf-form-panel .tf-sub{
            color:var(--tf-gray); font-size:14px; margin-bottom:28px; line-height:1.6;
        }
        .tf-alert{
            background:#FDECEC; color:var(--tf-error);
            border-radius:var(--tf-radius-sm);
            padding:12px 14px; font-size:13.5px; font-weight:600;
            margin-bottom:20px;
            display:flex; align-items:center; gap:8px;
        }
        .tf-alert-success{
            background:#E9F6EF; color:var(--tf-success);
            border-radius:var(--tf-radius-sm);
            padding:12px 14px; font-size:13.5px; font-weight:600;
            margin-bottom:20px;
            display:flex; align-items:center; gap:8px;
        }
        .tf-field{ margin-bottom:18px; }
        .tf-field label{
            display:block; font-size:13px; font-weight:700;
            color:var(--tf-dark-green); margin-bottom:6px; letter-spacing:0.3px;
        }
        .tf-input-wrap{ position:relative; }
        .tf-input-wrap input{
            width:100%;
            padding:14px 16px;
            border:1.5px solid var(--tf-light-gray);
            border-radius:var(--tf-radius-sm);
            font-size:15px;
            color:var(--tf-dark-green);
            background:var(--tf-bg);
            outline:none;
            transition:border-color .15s ease, background .15s ease;
        }
        .tf-input-wrap input:focus{
            border-color:var(--tf-green);
            background:var(--tf-white);
        }
        .tf-input-wrap input::placeholder{ color:#B7BBB2; }
        .tf-input-error{ border-color:var(--tf-error) !important; }
        .tf-field-error-text{
            color:var(--tf-error); font-size:12.5px; margin-top:6px; font-weight:600;
        }
        .tf-link{
            color:var(--tf-green); text-decoration:none; font-weight:700;
        }
        .tf-link:hover{ text-decoration:underline; }
        .tf-submit{
            width:100%;
            padding:15px;
            border:none;
            border-radius:var(--tf-radius-sm);
            background:var(--tf-green);
            color:var(--tf-white);
            font-size:15.5px;
            font-weight:800;
            letter-spacing:0.3px;
            cursor:pointer;
            transition:background .15s ease, transform .05s ease;
        }
        .tf-submit:hover{ background:var(--tf-dark-green); }
        .tf-submit:active{ transform:scale(0.99); }

        .tf-footer-text{
            text-align:center; margin-top:26px;
            font-size:13.5px; color:var(--tf-gray);
        }

        @media (max-width: 760px){
            .tf-shell{ flex-direction:column; max-width:440px; min-height:0; }
            .tf-hero{ padding:32px 28px; min-height:160px; }
            .tf-hero-copy h1{ font-size:24px; }
            .tf-hero-copy p{ display:none; }
            .tf-form-panel{ padding:36px 28px; }
        }

        a:focus-visible, button:focus-visible, input:focus-visible{
            outline:2px solid var(--tf-green);
            outline-offset:2px;
        }
    </style>
</head>
<body>
<div class="tf-shell">

    <!-- HERO -->
    <div class="tf-hero">
        <div class="tf-logo">
            <span class="tf-logo-mark">✈️</span>
            TRIPIFY
        </div>
        <div class="tf-hero-copy">
            <h1>Capita a tutti.<br/>Ti aiutiamo a<br/>rientrare.</h1>
            <p>Inserisci l'email del tuo account: se corrisponde a un profilo Tripify, ti mandiamo subito il link per crearne una nuova.</p>
        </div>
        <div class="tf-route">
            <span class="dot"></span>
            <span class="line"></span>
            <span>Recupero password</span>
            <span class="line"></span>
            <span class="dot"></span>
        </div>
    </div>

    <!-- FORM -->
    <div class="tf-form-panel">
        <h2>Password dimenticata?</h2>
        <p class="tf-sub">Nessun problema, ti mandiamo un link per reimpostarla.</p>

        <#if message?has_content>
            <#if message.type == 'success'>
                <div class="tf-alert-success">✅ ${kcSanitize(message.summary)?no_esc}</div>
            <#elseif message.type == 'error'>
                <div class="tf-alert">⚠️ ${kcSanitize(message.summary)?no_esc}</div>
            </#if>
        </#if>

        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
            <div class="tf-field">
                <label for="username"><#if !realm.loginWithEmailAllowed>Nome utente<#elseif !realm.registrationEmailAsUsername>Nome utente o Email<#else>Email</#if></label>
                <div class="tf-input-wrap">
                    <input tabindex="1"
                           id="username"
                           name="username"
                           class="<#if messagesPerField.existsError('username')>tf-input-error</#if>"
                           type="text"
                           autofocus
                           autocomplete="username"
                           value="${auth.attemptedUsername!''}"
                           placeholder="nome@esempio.com"
                           aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                    />
                </div>
                <#if messagesPerField.existsError('username')>
                    <div class="tf-field-error-text">
                        ${kcSanitize(messagesPerField.getFirstError('username'))?no_esc}
                    </div>
                </#if>
            </div>

            <button tabindex="2" class="tf-submit" type="submit">Invia il link di recupero</button>
        </form>

        <p class="tf-footer-text">
            Ricordi la password? <a class="tf-link" href="${url.loginUrl}">Torna al Login</a>
        </p>
    </div>
</div>
</body>
</html>
