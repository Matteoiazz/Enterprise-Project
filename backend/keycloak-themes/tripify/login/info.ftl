<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Avviso | Tripify</title>
    <style>
        :root{
            --tf-green:#1B8A5A;
            --tf-dark-green:#0B3D2E;
            --tf-bg:#F7F5EF;
            --tf-white:#FFFFFF;
            --tf-gray:#8A8F8C;
            --tf-light-gray:#E7E4DC;
            --tf-error:#D14343;
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
            max-width:1040px;
            background:var(--tf-white);
            border-radius:28px;
            display:flex;
            overflow:hidden;
            box-shadow:0 30px 60px -20px rgba(11,61,46,0.25);
        }
        .tf-hero{
            flex:0 0 300px;
            position:relative;
            background:linear-gradient(160deg, var(--tf-green) 0%, var(--tf-dark-green) 100%);
            color:var(--tf-white);
            padding:44px 34px;
            display:flex;
            flex-direction:column;
            justify-content:space-between;
        }
        .tf-hero::before{
            content:"";
            position:absolute; inset:0;
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
            font-size:27px; font-weight:900; line-height:1.3; margin-bottom:14px;
        }
        .tf-hero-copy p{ font-size:14.5px; opacity:0.85; line-height:1.6; }
        .tf-hero-list{ list-style:none; margin-top:22px; position:relative; z-index:1; }
        .tf-hero-list li{
            font-size:13.5px; opacity:0.9; margin-bottom:10px;
            padding-left:22px; position:relative;
        }
        .tf-hero-list li::before{
            content:"✓"; position:absolute; left:0; font-weight:900;
        }
        .tf-route{
            position:relative; z-index:1;
            display:flex; align-items:center; gap:8px;
            font-size:12.5px; opacity:0.75; letter-spacing:1px; text-transform:uppercase;
        }
        .tf-route .dot{ width:6px; height:6px; border-radius:50%; background:var(--tf-white); }
        .tf-route .line{ flex:1; height:1px; background:rgba(255,255,255,0.35); max-width:40px; }

        .tf-form-panel{
            flex:1;
            padding:44px 48px;
            min-width:0;
        }
        .tf-form-panel h2{ font-size:25px; font-weight:900; color:var(--tf-dark-green); margin-bottom:4px; }
        .tf-form-panel .tf-sub{ color:var(--tf-gray); font-size:14px; margin-bottom:22px; }

        .tf-alert{
            background:#FDECEC; color:var(--tf-error);
            border-radius:var(--tf-radius-sm);
            padding:12px 14px; font-size:13.5px; font-weight:600;
            margin-bottom:18px;
        }

        .tf-toggle{
            display:flex;
            background:var(--tf-bg);
            border-radius:var(--tf-radius-sm);
            padding:5px;
            margin-bottom:26px;
            gap:5px;
        }
        .tf-toggle input{ display:none; }
        .tf-toggle label{
            flex:1;
            text-align:center;
            padding:12px 10px;
            border-radius:10px;
            font-size:14px;
            font-weight:800;
            color:var(--tf-gray);
            cursor:pointer;
            transition:all .18s ease;
            display:flex; align-items:center; justify-content:center; gap:8px;
        }
        .tf-toggle input:checked + label{
            background:var(--tf-green);
            color:var(--tf-white);
            box-shadow:0 8px 18px -8px rgba(27,138,90,0.6);
        }

        .tf-grid{
            display:grid;
            grid-template-columns:1fr 1fr;
            gap:16px 16px;
        }
        .tf-field{ margin-bottom:16px; }
        .tf-field.full{ grid-column:1 / -1; }
        .tf-field label{
            display:block; font-size:12.5px; font-weight:700;
            color:var(--tf-dark-green); margin-bottom:6px; letter-spacing:0.3px;
        }
        .tf-input-wrap input{
            width:100%;
            padding:13px 15px;
            border:1.5px solid var(--tf-light-gray);
            border-radius:var(--tf-radius-sm);
            font-size:14.5px;
            color:var(--tf-dark-green);
            background:var(--tf-bg);
            outline:none;
            transition:border-color .15s ease, background .15s ease;
        }
        .tf-input-wrap input:focus{ border-color:var(--tf-green); background:var(--tf-white); }
        .tf-input-wrap input::placeholder{ color:#B7BBB2; }
        .tf-input-error{ border-color:var(--tf-error) !important; }
        .tf-field-error-text{ color:var(--tf-error); font-size:12px; margin-top:5px; font-weight:600; display:none; }

        #organizer-fields{
            grid-column:1 / -1;
            display:none;
            background:var(--tf-bg);
            border:1.5px dashed var(--tf-light-gray);
            border-radius:var(--tf-radius-sm);
            padding:16px 16px 2px 16px;
            margin-bottom:6px;
        }
        #organizer-fields .tf-org-title{
            grid-column:1/-1;
            font-size:12.5px; font-weight:800; color:var(--tf-green);
            text-transform:uppercase; letter-spacing:0.8px; margin-bottom:12px;
            display:flex; align-items:center; gap:6px;
        }
        #organizer-fields .tf-grid{ margin:0; }

        .tf-terms{
            display:flex; align-items:flex-start; gap:10px;
            font-size:13px; color:var(--tf-gray); margin:6px 0 22px 0;
        }
        .tf-terms input{ margin-top:2px; accent-color:var(--tf-green); width:16px; height:16px; flex-shrink:0; }
        .tf-terms a{ color:var(--tf-green); font-weight:700; text-decoration:none; }
        .tf-terms a:hover{ text-decoration:underline; }

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
            transition:all .2s ease;
        }
        .tf-submit:hover:not(:disabled){ background:var(--tf-dark-green); }
        .tf-submit:active:not(:disabled){ transform:scale(0.99); }
        .tf-submit:disabled{ background:#B7BBB2; cursor:not-allowed; opacity: 0.7;}

        .tf-footer-text{ text-align:center; margin-top:22px; font-size:13.5px; color:var(--tf-gray); }
        .tf-link{ color:var(--tf-green); text-decoration:none; font-weight:700; }
        .tf-link:hover{ text-decoration:underline; }

        #password-checklist { display: none; font-size: 12.5px; margin-top: 8px; color: var(--tf-gray); font-weight: 600; padding-left: 5px;}
        .req-item { display: flex; align-items: center; margin-bottom: 4px; transition: color 0.2s ease;}
        .req-icon { width: 16px; display: inline-block; text-align: center; margin-right: 6px; }

        @media (max-width: 860px){
            .tf-shell{ flex-direction:column; max-width:520px; }
            .tf-hero{ flex:none; }
            .tf-hero-copy p, .tf-hero-list{ display:none; }
            .tf-form-panel{ padding:32px 26px; }
            .tf-grid{ grid-template-columns:1fr; }
        }

        a:focus-visible, button:focus-visible, input:focus-visible, label:focus-within{
            outline:2px solid var(--tf-green);
            outline-offset:2px;
        }
    </style>
</head>
<body>
<div class="tf-shell" style="max-width: 500px; text-align: center; padding: 40px;">
    <div style="font-size: 50px; margin-bottom: 20px;">✅</div>
    <h2>${message.summary}</h2>

    <#if skipLink??>
    <#else>
        <#if pageRedirectUri?has_content>
            <p><a href="${pageRedirectUri}" class="tf-link">Clicca qui per continuare</a></p>
        <#elseif actionUri?has_content>
            <p><a href="${actionUri}" class="tf-link">Clicca qui per procedere</a></p>
        <#else>
            <p><a href="${url.loginUrl}" class="tf-btn" style="margin-top:20px;">Torna al Login</a></p>
        </#if>
    </#if>
</div>
</body>
</html>