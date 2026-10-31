package com.campusfind;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/items/*")
public class ItemServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();

    // GET /api/items - list open reports, with optional filters.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String search = request.getParameter("search");
        String type = request.getParameter("type");
        String category = request.getParameter("category");
        String status = request.getParameter("status");

        if (search == null) search = "";
        if (type == null || type.isBlank()) type = "All";
        if (category == null || category.isBlank()) category = "All";
        if (status == null || status.isBlank()) status = "Open";

        StringBuilder sql = new StringBuilder(
                "SELECT id, type, name, category, description, location, " +
                "DATE_FORMAT(item_date, '%Y-%m-%d') AS item_date, contact, status " +
                "FROM items WHERE status = ?");
        List<Object> values = new ArrayList<>();
        values.add(status);

        if (!type.equals("All")) {
            sql.append(" AND type = ?");
            values.add(type);
        }

        if (!category.equals("All")) {
            sql.append(" AND category = ?");
            values.add(category);
        }

        if (!search.isBlank()) {
            sql.append(" AND (name LIKE ? OR location LIKE ? OR description LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            values.add(pattern);
            values.add(pattern);
            values.add(pattern);
        }

        sql.append(" ORDER BY id DESC");

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < values.size(); i++) {
                statement.setObject(i + 1, values.get(i));
            }

            List<Map<String, Object>> items = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", result.getInt("id"));
                    item.put("type", result.getString("type"));
                    item.put("name", result.getString("name"));
                    item.put("category", result.getString("category"));
                    item.put("description", result.getString("description"));
                    item.put("location", result.getString("location"));
                    item.put("item_date", result.getString("item_date"));
                    item.put("contact", result.getString("contact"));
                    item.put("status", result.getString("status"));
                    items.add(item);
                }
            }

            mapper.writeValue(response.getWriter(), items);
        } catch (SQLException e) {
            getServletContext().log("Could not load item reports", e);
            sendError(response, 500, "Could not load reports. Check the database settings.");
        }
    }

    // POST /api/items - create a report.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonNode body;
        try {
            body = mapper.readTree(request.getReader());
        } catch (Exception e) {
            sendError(response, 400, "Invalid JSON request.");
            return;
        }

        String type = value(body, "type");
        String name = value(body, "name");
        String category = value(body, "category");
        String description = value(body, "description");
        String location = value(body, "location");
        String itemDate = value(body, "item_date");
        String contact = value(body, "contact");

        if (type.isBlank() || name.isBlank() || category.isBlank()
                || description.isBlank() || location.isBlank()
                || itemDate.isBlank() || contact.isBlank()) {
            sendError(response, 400, "Please fill in all fields.");
            return;
        }

        if (!type.equals("Lost") && !type.equals("Found")) {
            sendError(response, 400, "Choose Lost or Found.");
            return;
        }

        if (name.length() > 80 || category.length() > 50
                || description.length() > 500 || location.length() > 120
                || contact.length() > 120) {
            sendError(response, 400, "One or more fields are too long.");
            return;
        }

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO items (type, name, category, description, location, item_date, contact) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, type);
            statement.setString(2, name.trim());
            statement.setString(3, category);
            statement.setString(4, description.trim());
            statement.setString(5, location.trim());
            statement.setDate(6, Date.valueOf(itemDate));
            statement.setString(7, contact.trim());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    mapper.writeValue(response.getWriter(), Map.of(
                            "id", id,
                            "message", "Report added successfully."
                    ));
                } else {
                    sendError(response, 500, "Report saved, but its ID could not be read.");
                }
            }
        } catch (IllegalArgumentException e) {
            sendError(response, 400, "Enter a valid date.");
        } catch (SQLException e) {
            getServletContext().log("Could not save item report", e);
            sendError(response, 500, "Could not save report. Check the database settings.");
        }
    }

    // PATCH /api/items/{id}/returned - mark an item as returned.
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod())) {
            doPatch(request, response);
        } else {
            super.service(request, response);
        }
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+/returned")) {
            sendError(response, 400, "Use /api/items/{id}/returned.");
            return;
        }

        String[] parts = pathInfo.split("/");
        int id = Integer.parseInt(parts[1]);

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE items SET status = 'Returned' WHERE id = ?")) {
            statement.setInt(1, id);
            int updated = statement.executeUpdate();

            if (updated == 0) {
                sendError(response, 404, "Item not found.");
            } else {
                mapper.writeValue(response.getWriter(),
                        Map.of("message", "Item marked as returned."));
            }
        } catch (SQLException e) {
            getServletContext().log("Could not update item status", e);
            sendError(response, 500, "Could not update item status.");
        }
    }

    private String value(JsonNode body, String field) {
        JsonNode node = body == null ? null : body.get(field);
        return node == null || node.isNull() ? "" : node.asText().trim();
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        mapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}
