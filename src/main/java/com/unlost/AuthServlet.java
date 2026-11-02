package com.unlost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,30}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        if (!"/me".equals(request.getPathInfo())) {
            sendError(response, 404, "Endpoint not found.");
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            mapper.writeValue(response.getWriter(), Map.of("authenticated", false));
            return;
        }

        String csrfToken = (String) session.getAttribute("csrfToken");
        if (csrfToken == null) {
            csrfToken = createToken();
            session.setAttribute("csrfToken", csrfToken);
        }

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", session.getAttribute("userId"));
        user.put("username", session.getAttribute("username"));
        user.put("email", session.getAttribute("email"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authenticated", true);
        result.put("user", user);
        result.put("csrfToken", csrfToken);
        mapper.writeValue(response.getWriter(), result);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        request.setCharacterEncoding("UTF-8");

        String path = request.getPathInfo();
        if ("/register".equals(path)) {
            register(request, response);
        } else if ("/login".equals(path)) {
            login(request, response);
        } else if ("/logout".equals(path)) {
            logout(request, response);
        } else {
            sendError(response, 404, "Endpoint not found.");
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        JsonNode body = readBody(request, response);
        if (body == null) return;

        String username = value(body, "username");
        String email = value(body, "email").toLowerCase(Locale.ROOT);
        String password = rawValue(body, "password");

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            sendError(response, 400, "Username must be 3 to 30 characters and use only letters, numbers or underscores.");
            return;
        }

        if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            sendError(response, 400, "Enter a valid email address.");
            return;
        }

        if (password.length() < 8 || password.length() > 128) {
            sendError(response, 400, "Password must contain between 8 and 128 characters.");
            return;
        }

        String passwordHash = PasswordUtil.hash(password);

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)")) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.executeUpdate();

            try (PreparedStatement lookup = connection.prepareStatement(
                    "SELECT id, username, email FROM users WHERE username = ?")) {
                lookup.setString(1, username);
                try (ResultSet result = lookup.executeQuery()) {
                    if (result.next()) {
                        startSession(request, result.getInt("id"),
                                result.getString("username"), result.getString("email"));
                        writeSession(response, request);
                    } else {
                        sendError(response, 500, "Account was created but could not be loaded.");
                    }
                }
            }
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                sendError(response, 409, "That username or email is already registered.");
            } else {
                getServletContext().log("Could not register account", e);
                sendError(response, 500, "Could not create account.");
            }
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        JsonNode body = readBody(request, response);
        if (body == null) return;

        String login = value(body, "login");
        String password = rawValue(body, "password");

        if (login.isBlank() || password.isBlank()
                || login.length() > 254 || password.length() > 128) {
            sendError(response, 400, "Enter a valid username or email and password.");
            return;
        }

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, username, email, password_hash FROM users " +
                     "WHERE username = ? OR email = ? LIMIT 1")) {
            statement.setString(1, login);
            statement.setString(2, login.toLowerCase(Locale.ROOT));

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !PasswordUtil.verify(password, result.getString("password_hash"))) {
                    sendError(response, 401, "Incorrect username/email or password.");
                    return;
                }

                startSession(request, result.getInt("id"),
                        result.getString("username"), result.getString("email"));
                writeSession(response, request);
            }
        } catch (SQLException e) {
            getServletContext().log("Could not log in", e);
            sendError(response, 500, "Could not log in.");
        }
    }

    private void logout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !hasValidCsrfToken(request)) {
            sendError(response, 403, "Your session has expired. Refresh the page and try again.");
            return;
        }

        session.invalidate();
        mapper.writeValue(response.getWriter(), Map.of("message", "You have been logged out."));
    }

    private void startSession(HttpServletRequest request, int userId, String username, String email) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) oldSession.invalidate();

        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(30 * 60);
        session.setAttribute("userId", userId);
        session.setAttribute("username", username);
        session.setAttribute("email", email);
        session.setAttribute("csrfToken", createToken());
    }

    private void writeSession(HttpServletResponse response, HttpServletRequest request)
            throws IOException {
        HttpSession session = request.getSession(false);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", session.getAttribute("userId"));
        user.put("username", session.getAttribute("username"));
        user.put("email", session.getAttribute("email"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authenticated", true);
        result.put("user", user);
        result.put("csrfToken", session.getAttribute("csrfToken"));
        mapper.writeValue(response.getWriter(), result);
    }

    public static Integer currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object userId = session.getAttribute("userId");
        return userId instanceof Integer ? (Integer) userId : null;
    }

    public static boolean hasValidCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        Object savedToken = session.getAttribute("csrfToken");
        String sentToken = request.getHeader("X-CSRF-Token");
        return savedToken instanceof String
                && sentToken != null
                && MessageDigest.isEqual(
                        ((String) savedToken).getBytes(StandardCharsets.UTF_8),
                        sentToken.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode readBody(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            return mapper.readTree(request.getReader());
        } catch (Exception e) {
            sendError(response, 400, "Invalid request.");
            return null;
        }
    }

    private String value(JsonNode body, String field) {
        JsonNode node = body == null ? null : body.get(field);
        return node == null || node.isNull() ? "" : node.asText().trim();
    }

    private String rawValue(JsonNode body, String field) {
        JsonNode node = body == null ? null : body.get(field);
        return node == null || node.isNull() ? "" : node.asText();
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setStatus(status);
        mapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}
