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
            max-width:600px;
            background:var(--tf-white);
            border-radius:28px;
            display:flex;
            flex-direction: column;
            overflow:hidden;
            box-shadow:0 30px 60px -20px rgba(11,61,46,0.25);
        }
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

        .tf-field{ margin-bottom:16px; }
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

        @media (max-width: 860px){
            .tf-form-panel{ padding:32px 26px; }
        }

        a:focus-visible, button:focus-visible, input:focus-visible, label:focus-within{
            outline:2px solid var(--tf-green);
            outline-offset:2px;
        }
    </style>
</head>
<body>
<div class="tf-shell">
    <div class="tf-form-panel">
        <h2>Imposta la tua Password</h2>
        <p class="tf-sub">L'email è stata verificata! Ora crea una password sicura per il tuo account.</p>

        <!-- Nascondiamo il warning arancione di default, mostriamo solo i veri errori! -->
        <#if message?has_content && message.type == 'error'>
            <div class="tf-alert">⚠️ ${kcSanitize(message.summary)?no_esc}</div>
        </#if>

        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">

            <input type="text" id="username" name="username" value="${username}" autocomplete="username" readonly="readonly" style="display:none;"/>
            <input type="password" id="password" name="password" autocomplete="current-password" style="display:none;"/>

            <div class="tf-field">
                <label for="password-new">Nuova Password</label>
                <div class="tf-input-wrap">
                    <input type="password" id="password-new" name="password-new" class="<#if messagesPerField.existsError('password','password-confirm')>tf-input-error</#if>" placeholder="••••••••" autocomplete="new-password" required />
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
                    <input type="password" id="password-confirm" name="password-confirm" class="<#if messagesPerField.existsError('password-confirm')>tf-input-error</#if>" placeholder="••••••••" autocomplete="new-password" required />
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

            // Mostra la checklist non appena l'utente digita qualcosa
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
</script>
</body>
</html>