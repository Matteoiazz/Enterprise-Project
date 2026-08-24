
        <!DOCTYPE html>
        <html lang="it">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Accedi | Tripify</title>
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
                    min-height:600px;
                    background:var(--tf-white);
                    border-radius:28px;
                    display:flex;
                    overflow:hidden;
                    box-shadow:0 30px 60px -20px rgba(11,61,46,0.25);
                }
                /* ---- Left hero panel ---- */
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

                /* ---- Right form panel ---- */
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
                    color:var(--tf-gray); font-size:14px; margin-bottom:28px;
                }
                .tf-alert{
                    background:#FDECEC; color:var(--tf-error);
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
                .tf-row-between{
                    display:flex; align-items:center; justify-content:space-between;
                    margin-bottom:26px; font-size:13.5px;
                }
                .tf-checkbox{ display:flex; align-items:center; gap:8px; color:var(--tf-gray); }
                .tf-checkbox input{ accent-color:var(--tf-green); width:16px; height:16px; }
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

                .tf-divider{
                    display:flex; align-items:center; gap:14px;
                    margin:28px 0 22px 0;
                    color:var(--tf-gray); font-size:12.5px; font-weight:700;
                    text-transform:uppercase; letter-spacing:1px;
                }
                .tf-divider::before, .tf-divider::after{
                    content:""; flex:1; height:1px; background:var(--tf-light-gray);
                }

                .tf-social-row{
                    display:flex; gap:12px;
                }
                .tf-social-btn{
                    flex:1;
                    display:flex; align-items:center; justify-content:center; gap:8px;
                    padding:12px;
                    border-radius:var(--tf-radius-sm);
                    border:1.5px solid var(--tf-light-gray);
                    background:var(--tf-white);
                    color:var(--tf-dark-green);
                    font-size:13.5px;
                    font-weight:700;
                    text-decoration:none;
                    transition:border-color .15s ease, box-shadow .15s ease;
                }
                .tf-social-btn:hover{
                    border-color:var(--tf-green);
                    box-shadow:0 6px 14px -6px rgba(27,138,90,0.35);
                }
                .tf-social-btn svg{ width:18px; height:18px; flex-shrink:0; }

                .tf-footer-text{
                    text-align:center; margin-top:30px;
                    font-size:13.5px; color:var(--tf-gray);
                }

                @media (max-width: 760px){
                    .tf-shell{ flex-direction:column; max-width:440px; }
                    .tf-hero{ padding:32px 28px; min-height:180px; }
                    .tf-hero-copy h1{ font-size:24px; }
                    .tf-hero-copy p{ display:none; }
                    .tf-form-panel{ padding:36px 28px; }
                }

                /* keyboard focus visibility */
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
                    <h1>Bentornato.<br/>Il tuo prossimo<br/>viaggio ti aspetta.</h1>
                    <p>Gestisci documenti, compagni di viaggio e pagamenti in un unico posto, ovunque tu sia diretto.</p>
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
                <h2>Accedi</h2>
                <p class="tf-sub">Inserisci le tue credenziali per continuare</p>

                <#if message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                    <#if message.summary != 'Your login attempt timed out. Login will start from the beginning.'
                    && message.summary != 'Il tuo tentativo di accesso è scaduto. L\'accesso ripartirà dall\'inizio.'
                    && message.summary != 'Action expired. Please continue with login now.'
                    && message.summary != 'Page has expired'>
                        <div class="tf-alert">
                            ⚠️ ${kcSanitize(message.summary)?no_esc}
                        </div>
                    </#if>
                </#if>

                <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                    <div class="tf-field">
                        <label for="username"><#if !realm.loginWithEmailAllowed>Nome utente<#elseif !realm.registrationEmailAsUsername>Nome utente o Email<#else>Email</#if></label>
                        <div class="tf-input-wrap">
                            <input tabindex="1"
                                   id="username"
                                   class="${properties.kcInputClass!} <#if messagesPerField.existsError('username','password')>tf-input-error</#if>"
                                   name="username"
                                   value="${(login.username!'')}"
                                   type="text"
                                   autofocus
                                   autocomplete="username"
                                   placeholder="nome@esempio.com"
                                   aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                            />
                        </div>
                        <#if messagesPerField.existsError('username','password')>
                            <div class="tf-field-error-text">
                                ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                            </div>
                        </#if>
                    </div>

                    <div class="tf-field">
                        <label for="password">Password</label>
                        <div class="tf-input-wrap">
                            <input tabindex="2"
                                   id="password"
                                   class="${properties.kcInputClass!} <#if messagesPerField.existsError('username','password')>tf-input-error</#if>"
                                   name="password"
                                   type="password"
                                   autocomplete="current-password"
                                   placeholder="••••••••"
                            />
                        </div>
                    </div>

                    <div class="tf-row-between">
                        <#if realm.rememberMe && !usernameEditDisabled??>
                            <label class="tf-checkbox">
                                <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" <#if login.rememberMe??>checked</#if>>
                                Ricordami
                            </label>
                        </#if>
                        <#if realm.resetPasswordAllowed>
                            <a tabindex="4" class="tf-link" href="${url.loginResetCredentialsUrl}">Password dimenticata?</a>
                        </#if>
                    </div>

                    <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                    <button tabindex="5" class="tf-submit" name="login" id="kc-login" type="submit">Accedi</button>
                </form>

                <#if realm.password && social.providers?? && social.providers?has_content>
                    <div class="tf-divider">oppure continua con</div>
                    <div class="tf-social-row">
                        <#list social.providers as p>
                            <a href="${p.loginUrl}" id="social-${p.alias}" class="tf-social-btn">
                                <#if p.alias == 'google'>
                                    <svg viewBox="0 0 48 48"><path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.3 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34.5 6.1 29.5 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.7-.4-3.5z"/><path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.6 16 19 13 24 13c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34.5 6.1 29.5 4 24 4c-7.6 0-14.1 4.3-17.4 10.7z"/><path fill="#4CAF50" d="M24 44c5.3 0 10.1-2 13.7-5.4l-6.3-5.3C29.3 35 26.8 36 24 36c-5.2 0-9.6-3.3-11.3-7.9l-6.6 5.1C9.8 39.6 16.3 44 24 44z"/><path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-1.1 3-3.1 5.4-5.9 6.9l6.3 5.3C39.6 37.4 44 31.7 44 24c0-1.3-.1-2.7-.4-3.5z"/></svg>
                                    Google
                                <#elseif p.alias == 'facebook'>
                                    <svg viewBox="0 0 24 24" fill="#1877F2"><path d="M22 12.06C22 6.5 17.52 2 12 2S2 6.5 2 12.06c0 5 3.66 9.15 8.44 9.94v-7.03H7.9v-2.91h2.54V9.84c0-2.51 1.49-3.9 3.77-3.9 1.09 0 2.23.2 2.23.2v2.46h-1.26c-1.24 0-1.63.77-1.63 1.56v1.88h2.78l-.44 2.91h-2.34V22c4.78-.79 8.44-4.94 8.44-9.94z"/></svg>
                                    Facebook
                                <#elseif p.alias == 'apple'>
                                    <svg viewBox="0 0 24 24" fill="#0B3D2E"><path d="M16.365 1.43c0 1.14-.462 2.242-1.213 3.058-.827.902-2.19 1.6-3.303 1.51-.14-1.09.44-2.24 1.202-3.02.85-.9 2.32-1.58 3.314-1.548zM20.6 17.13c-.53 1.24-.79 1.8-1.48 2.9-.96 1.53-2.31 3.44-3.98 3.46-1.49.02-1.87-.98-3.89-.97-2.02.01-2.44.99-3.93.97-1.67-.02-2.95-1.75-3.91-3.28C.79 17.03.16 12.9 1.7 10.13c1.02-1.87 2.85-3.06 4.83-3.09 1.55-.03 3.02 1.05 3.97 1.05.94 0 2.72-1.3 4.59-1.11.78.03 2.97.32 4.38 2.4-.11.07-2.61 1.53-2.58 4.56.03 3.61 3.16 4.81 3.19 4.83-.03.09-.5 1.71-1.5 3.36z"/></svg>
                                    Apple
                                <#else>
                                    ${p.displayName}
                                </#if>
                            </a>
                        </#list>
                    </div>
                </#if>

                <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
                    <p class="tf-footer-text">
                        Non hai un account?
                        <a class="tf-link" href="${url.registrationUrl}">Registrati</a>
                    </p>
                </#if>
            </div>
        </div>
        </body>
        </html>

