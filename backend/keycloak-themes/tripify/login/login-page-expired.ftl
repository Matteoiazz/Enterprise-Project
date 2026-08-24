<!DOCTYPE html>
<html lang="it">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Aggiornamento pagina...</title>
    <style>
        body { background: #F7F5EF; margin: 0; display: flex; justify-content: center; align-items: center; height: 100vh; font-family: sans-serif; }
        .loader { border: 4px solid #E7E4DC; border-top: 4px solid #1B8A5A; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; }
        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
    </style>
    <script>
        window.location.href = "${url.loginRestartFlowUrl}";
    </script>
</head>
<body>
<div class="loader"></div>
</body>
</html>