const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");
const showLogin = document.getElementById("show-login");
const showRegister = document.getElementById("show-register");
const authHeading = document.getElementById("auth-heading");
const authMessage = document.getElementById("auth-message");

function setMode(mode) {
    const isLogin = mode === "login";
    loginForm.hidden = !isLogin;
    registerForm.hidden = isLogin;
    showLogin.classList.toggle("active", isLogin);
    showRegister.classList.toggle("active", !isLogin);
    authHeading.textContent = isLogin ? "Sign in" : "Create account";
    authMessage.textContent = "";
    authMessage.className = "form-message";
}

async function sendAuthRequest(path, data) {
    const response = await fetch(`api/auth/${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    });

    const result = await response.json();
    if (!response.ok) throw new Error(result.error || "Authentication failed.");
    window.location.href = "index.html";
}

loginForm.addEventListener("submit", async function(event) {
    event.preventDefault();
    authMessage.textContent = "";
    authMessage.className = "form-message";

    try {
        await sendAuthRequest("login", {
            login: loginForm.login.value.trim(),
            password: loginForm.password.value
        });
    } catch (error) {
        authMessage.textContent = error.message;
        authMessage.classList.add("error");
    }
});

registerForm.addEventListener("submit", async function(event) {
    event.preventDefault();
    authMessage.textContent = "";
    authMessage.className = "form-message";

    try {
        await sendAuthRequest("register", {
            username: registerForm.username.value.trim(),
            email: registerForm.email.value.trim(),
            password: registerForm.password.value
        });
    } catch (error) {
        authMessage.textContent = error.message;
        authMessage.classList.add("error");
    }
});

showLogin.addEventListener("click", function() {
    setMode("login");
});

showRegister.addEventListener("click", function() {
    setMode("register");
});

setMode("login");
