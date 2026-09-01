<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Crea Password | Tripify</title>
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
        .tf-form-panel h2{ font-size:26px; font-weight:900; color:var(--tf-dark-green); margin-bottom:6px; }
        .tf-form-panel .tf-sub{ color:var(--tf-gray); font-size:14px; margin-bottom:28px; line-height:1.6; }

        .tf-alert{
            background:#FDECEC; color:var(--tf-error);
            border-radius:var(--tf-radius-sm);
            padding:12px 14px; font-size:13.5px; font-weight:600;
            margin-bottom:20px;
        }

        .tf-field{ margin-bottom:16px; }
        .tf-field label{
            display:block; font-size:13px; font-weight:700;
            color:var(--tf-dark-green); margin-bottom:6px; letter-spacing:0.3px;
        }
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
        .tf-input-wrap input:focus{ border-color:var(--tf-green); background:var(--tf-white); }
        .tf-input-wrap input::placeholder{ color:#B7BBB2; }
        .tf-input-wrap{ position:relative; }
        .tf-input-wrap input.tf-has-eye{ padding-right:48px; }
        .tf-eye{
            position:absolute; top:0; right:6px; height:100%;
            display:flex; align-items:center; justify-content:center;
            width:34px; padding:0; border:none; background:transparent;
            color:var(--tf-gray); cursor:pointer; border-radius:10px;
            transition:color .15s ease, background .15s ease;
        }
        .tf-eye:hover{ color:var(--tf-dark-green); background:rgba(11,61,46,0.06); }
        .tf-eye svg{ width:20px; height:20px; display:block; }
        .tf-eye .tf-eye-off{ display:none; }
        .tf-eye.is-showing .tf-eye-open{ display:none; }
        .tf-eye.is-showing .tf-eye-off{ display:block; }
        .tf-input-error{ border-color:var(--tf-error) !important; }
        .tf-field-error-text{ color:var(--tf-error); font-size:12px; margin-top:5px; font-weight:600; display:none; }

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
            margin-top: 10px;
        }
        .tf-submit:hover:not(:disabled){ background:var(--tf-dark-green); }
        .tf-submit:active:not(:disabled){ transform:scale(0.99); }
        .tf-submit:disabled{ background:#B7BBB2; cursor:not-allowed; opacity: 0.7;}

        /* CSS PER LA CHECKLIST LIVE */
        #password-checklist { display: none; font-size: 12.5px; margin-top: 8px; color: var(--tf-gray); font-weight: 600; padding-left: 5px;}
        .req-item { display: flex; align-items: center; margin-bottom: 4px; transition: color 0.2s ease;}
        .req-icon { width: 16px; display: inline-block; text-align: center; margin-right: 6px; }

        @media (max-width: 760px){
            .tf-shell{ flex-direction:column; max-width:440px; min-height:0; }
            .tf-hero{ padding:32px 28px; min-height:160px; }
            .tf-hero-copy h1{ font-size:24px; }
            .tf-hero-copy p{ display:none; }
            .tf-form-panel{ padding:36px 28px; }
        }

        a:focus-visible, button:focus-visible, input:focus-visible, label:focus-within{
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
            <h1>Quasi fatto.<br/>Un'ultima cosa.</h1>
            <p>La tua password protegge prenotazioni, documenti e pagamenti: scegline una robusta e tienila solo per te.</p>
        </div>
        <div class="tf-route">
            <span class="dot"></span>
            <span class="line"></span>
            <span>Accesso sicuro</span>
            <span class="line"></span>
            <span class="dot"></span>
        </div>
    </div>

    <!-- FORM -->
    <div class="tf-form-panel">
        <h2>Crea la tua Password</h2>
        <p class="tf-sub">Scegli una password sicura per il tuo account Tripify.</p>

        <#if message?has_content && message.type == 'error'>
            <div class="tf-alert">⚠️ ${kcSanitize(message.summary)?no_esc}</div>
        </#if>

        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">

            <input type="text" id="username" name="username" value="${username}" autocomplete="username" readonly="readonly" style="display:none;"/>
            <input type="password" id="password" name="password" autocomplete="current-password" style="display:none;"/>

            <div class="tf-field">
                <label for="password-new">Nuova Password</label>
                <div class="tf-input-wrap">
                    <input type="password" id="password-new" name="password-new" class="tf-has-eye <#if messagesPerField.existsError('password','password-confirm')>tf-input-error</#if>" placeholder="••••••••" autocomplete="new-password" required />
                    <button type="button" class="tf-eye" tabindex="-1" aria-pressed="false" aria-label="Mostra o nascondi la password" onclick="tfTogglePwd(this, 'password-new')">
                        <svg class="tf-eye-open" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                        <svg class="tf-eye-off" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                    </button>
                </div>
                <!-- CHECKLIST LIVE PASSWORD -->
                <div id="password-checklist">
                    <div id="req-length" class="req-item"><span class="req-icon">❌</span>Almeno 8 caratteri</div>
                    <div id="req-upper" class="req-item"><span class="req-icon">❌</span>Almeno una lettera maiuscola</div>
                    <div id="req-number" class="req-item"><span class="req-icon">❌</span>Almeno un numero</div>
                    <div id="req-special" class="req-item"><span class="req-icon">❌</span>Almeno un carattere speciale</div>
                </div>
            </div>

            <div class="tf-field">
                <label for="password-confirm">Conferma Password</label>
                <div class="tf-input-wrap">
                    <input type="password" id="password-confirm" name="password-confirm" class="tf-has-eye <#if messagesPerField.existsError('password-confirm')>tf-input-error</#if>" placeholder="••••••••" autocomplete="new-password" required />
                    <button type="button" class="tf-eye" tabindex="-1" aria-pressed="false" aria-label="Mostra o nascondi la password" onclick="tfTogglePwd(this, 'password-confirm')">
                        <svg class="tf-eye-open" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                        <svg class="tf-eye-off" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                    </button>
                </div>
                <!-- ERRORE LIVE PASSWORD UGUALI -->
                <div id="pwd-match-error" class="tf-field-error-text">Le password non coincidono</div>
            </div>

            <button type="submit" id="kc-submit-btn" class="tf-submit" disabled>Salva e Accedi</button>
        </form>
    </div>
</div>

<script>
    // --- LOGICA DI VALIDAZIONE LIVE PASSWORD ---
    const pwdInput = document.getElementById('password-new');
    const pwdConfirmInput = document.getElementById('password-confirm');
    const submitBtn = document.getElementById('kc-submit-btn');
    const registerForm = document.getElementById('kc-passwd-update-form');

    const pwdMatchErrorUI = document.getElementById('pwd-match-error');
    const pwdChecklistUI = document.getElementById('password-checklist');

    const reqLength = document.getElementById('req-length');
    const reqUpper = document.getElementById('req-upper');
    const reqNumber = document.getElementById('req-number');
    const reqSpecial = document.getElementById('req-special');

    function updateReq(element, isMet) {
        const icon = element.querySelector('.req-icon');
        if (isMet) {
            icon.textContent = "✅";
            element.style.color = "var(--tf-green)";
        } else {
            icon.textContent = "❌";
            element.style.color = "var(--tf-gray)";
        }
    }

    function validateForm() {
        let isFormValid = true;

        if (pwdInput) {
            const pwd = pwdInput.value;

            if (pwd.length > 0) {
                pwdChecklistUI.style.display = 'block';
            } else {
                pwdChecklistUI.style.display = 'none';
                isFormValid = false;
            }

            const hasLen = pwd.length >= 8;
            const hasUpper = /[A-Z]/.test(pwd);
            const hasNum = /[0-9]/.test(pwd);
            const hasSpec = /[^A-Za-z0-9]/.test(pwd);

            updateReq(reqLength, hasLen);
            updateReq(reqUpper, hasUpper);
            updateReq(reqNumber, hasNum);
            updateReq(reqSpecial, hasSpec);

            if (!(hasLen && hasUpper && hasNum && hasSpec)) {
                isFormValid = false;
            }

            // Controllo che le password coincidano
            if (pwdConfirmInput.value.length > 0) {
                if (pwd !== pwdConfirmInput.value) {
                    pwdConfirmInput.classList.add('tf-input-error');
                    pwdMatchErrorUI.style.display = 'block';
                    isFormValid = false;
                } else {
                    pwdConfirmInput.classList.remove('tf-input-error');
                    pwdMatchErrorUI.style.display = 'none';
                }
            } else {
                isFormValid = false;
            }
        }

        submitBtn.disabled = !isFormValid;
    }

    document.addEventListener('DOMContentLoaded', function() {
        if(pwdInput) pwdInput.addEventListener('input', validateForm);
        if(pwdConfirmInput) pwdConfirmInput.addEventListener('input', validateForm);

        registerForm.addEventListener('submit', function() {
            submitBtn.disabled = true;
            submitBtn.textContent = "Salvataggio in corso...";
        });

        validateForm();
    });

    function tfTogglePwd(btn, inputId){
        var input = document.getElementById(inputId);
        if(!input) return;
        var reveal = input.getAttribute('type') === 'password';
        input.setAttribute('type', reveal ? 'text' : 'password');
        btn.classList.toggle('is-showing', reveal);
        btn.setAttribute('aria-pressed', reveal ? 'true' : 'false');
    }
</script>
</body>
</html>
