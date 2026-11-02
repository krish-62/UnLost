package com.unlost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

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
@MultipartConfig(
        maxFileSize = 5242880,
        maxRequestSize = 6291456,
        fileSizeThreshold = 1048576
)
public class ItemServlet extends HttpServlet {
    private static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo();

        if (path != null && path.matches("/\\d+/image")) {
            getImage(request, response, path);
            return;
        }

        if ("/mine".equals(path)) {
            Integer userId = AuthServlet.currentUserId(request);
            if (userId == null) {
                sendError(response, 401, "Please sign in to view your reports.");
                return;
            }
            getItems(request, response, true, userId);
            return;
        }

        getItems(request, response, false, null);
    }

    private void getItems(HttpServletRequest request, HttpServletResponse response,
                          boolean mine, Integer userId) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String search = request.getParameter("search");
        String type = request.getParameter("type");
        String category = request.getParameter("category");

        if (search == null) search = "";
        if (type == null || type.isBlank()) type = "All";
        if (category == null || category.isBlank()) category = "All";

        StringBuilder sql = new StringBuilder(
                "SELECT i.id, i.type, i.name, i.category, i.description, i.location, " +
                "DATE_FORMAT(i.item_date, '%Y-%m-%d') AS item_date, i.contact, i.status, " +
                "i.image_data IS NOT NULL AS has_image, u.username AS reporter " +
                "FROM items i LEFT JOIN users u ON i.user_id = u.id WHERE ");

        List<Object> values = new ArrayList<>();
        if (mine) {
            sql.append("i.user_id = ?");
            values.add(userId);
        } else {
            sql.append("i.status = 'Open'");
        }

        if (!type.equals("All")) {
            sql.append(" AND i.type = ?");
            values.add(type);
        }

        if (!category.equals("All")) {
            sql.append(" AND i.category = ?");
            values.add(category);
        }

        if (!search.isBlank()) {
            sql.append(" AND (i.name LIKE ? OR i.location LIKE ? OR i.description LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            values.add(pattern);
            values.add(pattern);
            values.add(pattern);
        }

        sql.append(" ORDER BY i.id DESC");

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
                    item.put("has_image", result.getBoolean("has_image"));
                    item.put("reporter", result.getString("reporter"));
                    items.add(item);
                }
            }

            mapper.writeValue(response.getWriter(), items);
        } catch (SQLException e) {
            getServletContext().log("Could not load item reports", e);
            sendError(response, 500, "Could not load reports.");
        }
    }

    private void getImage(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        int id = Integer.parseInt(path.split("/")[1]);
        Integer userId = AuthServlet.currentUserId(request);

        String sql = "SELECT image_data, image_type FROM items " +
                "WHERE id = ? AND (status = 'Open' OR user_id = ?)";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, userId == null ? -1 : userId);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getBytes("image_data") == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                byte[] image = result.getBytes("image_data");
                response.setContentType(result.getString("image_type"));
                response.setContentLength(image.length);
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("Cache-Control", "private, max-age=300");
                response.getOutputStream().write(image);
            }
        } catch (SQLException e) {
            getServletContext().log("Could not load item image", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Integer userId = AuthServlet.currentUserId(request);
        if (userId == null) {
            sendError(response, 401, "Please sign in before submitting a report.");
            return;
        }

        if (!AuthServlet.hasValidCsrfToken(request)) {
            sendError(response, 403, "Your session has expired. Refresh the page and try again.");
            return;
        }

        request.setCharacterEncoding("UTF-8");

        String type = parameter(request, "type");
        String name = parameter(request, "name");
        String category = parameter(request, "category");
        String description = parameter(request, "description");
        String location = parameter(request, "location");
        String itemDate = parameter(request, "item_date");
        String contact = parameter(request, "contact");

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

        byte[] imageData = null;
        String imageType = null;

        try {
            Part imagePart = request.getPart("image");
            if (imagePart != null && imagePart.getSize() > 0) {
                if (imagePart.getSize() > MAX_IMAGE_SIZE) {
                    sendError(response, 413, "Image must be 5 MB or smaller.");
                    return;
                }

                imageData = imagePart.getInputStream().readNBytes(MAX_IMAGE_SIZE + 1);
                if (imageData.length > MAX_IMAGE_SIZE) {
                    sendError(response, 413, "Image must be 5 MB or smaller.");
                    return;
                }

                imageType = detectImageType(imageData);
                if (imageType == null) {
                    sendError(response, 400, "Upload a valid JPG, PNG or WEBP image.");
                    return;
                }
            }
        } catch (IllegalStateException e) {
            sendError(response, 413, "Image must be 5 MB or smaller.");
            return;
        }

        String sql = "INSERT INTO items " +
                "(user_id, type, name, category, description, location, item_date, contact, image_data, image_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setString(2, type);
            statement.setString(3, name.trim());
            statement.setString(4, category);
            statement.setString(5, description.trim());
            statement.setString(6, location.trim());
            statement.setDate(7, Date.valueOf(itemDate));
            statement.setString(8, contact.trim());

            if (imageData == null) {
                statement.setNull(9, java.sql.Types.BLOB);
                statement.setNull(10, java.sql.Types.VARCHAR);
            } else {
                statement.setBytes(9, imageData);
                statement.setString(10, imageType);
            }

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    mapper.writeValue(response.getWriter(), Map.of(
                            "id", keys.getInt(1),
                            "message", "Report added successfully."
                    ));
                } else {
                    sendError(response, 500, "Report was saved but its ID could not be read.");
                }
            }
        } catch (IllegalArgumentException e) {
            sendError(response, 400, "Enter a valid date.");
        } catch (SQLException e) {
            getServletContext().log("Could not save item report", e);
            sendError(response, 500, "Could not save report.");
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod())) {
            doPatch(request, response);
        } else {
            super.service(request, response);
        }
    }

    @Override
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Integer userId = AuthServlet.currentUserId(request);
        if (userId == null) {
            sendError(response, 401, "Please sign in.");
            return;
        }

        if (!AuthServlet.hasValidCsrfToken(request)) {
            sendError(response, 403, "Your session has expired. Refresh the page and try again.");
            return;
        }

        String path = request.getPathInfo();
        if (path == null || !path.matches("/\\d+/resolve")) {
            sendError(response, 400, "Invalid report address.");
            return;
        }

        int id = Integer.parseInt(path.split("/")[1]);

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE items SET status = 'Resolved' WHERE id = ? AND user_id = ? AND status = 'Open'")) {
            statement.setInt(1, id);
            statement.setInt(2, userId);

            if (statement.executeUpdate() == 0) {
                sendError(response, 404, "Report not found or you do not own this report.");
                return;
            }

            mapper.writeValue(response.getWriter(),
                    Map.of("message", "Report marked as resolved."));
        } catch (SQLException e) {
            getServletContext().log("Could not resolve item report", e);
            sendError(response, 500, "Could not update report.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Integer userId = AuthServlet.currentUserId(request);
        if (userId == null) {
            sendError(response, 401, "Please sign in.");
            return;
        }

        if (!AuthServlet.hasValidCsrfToken(request)) {
            sendError(response, 403, "Your session has expired. Refresh the page and try again.");
            return;
        }

        String path = request.getPathInfo();
        if (path == null || !path.matches("/\\d+")) {
            sendError(response, 400, "Invalid report address.");
            return;
        }

        int id = Integer.parseInt(path.substring(1));

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM items WHERE id = ? AND user_id = ?")) {
            statement.setInt(1, id);
            statement.setInt(2, userId);

            if (statement.executeUpdate() == 0) {
                sendError(response, 404, "Report not found or you do not own this report.");
                return;
            }

            mapper.writeValue(response.getWriter(),
                    Map.of("message", "Report deleted successfully."));
        } catch (SQLException e) {
            getServletContext().log("Could not delete item report", e);
            sendError(response, 500, "Could not delete report.");
        }
    }

    private String parameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private String detectImageType(byte[] data) {
        if (data.length >= 8
                && (data[0] & 0xff) == 0x89
                && data[1] == 0x50
                && data[2] == 0x4e
                && data[3] == 0x47
                && data[4] == 0x0d
                && data[5] == 0x0a
                && data[6] == 0x1a
                && data[7] == 0x0a) {
            return "image/png";
        }

        if (data.length >= 3
                && (data[0] & 0xff) == 0xff
                && (data[1] & 0xff) == 0xd8
                && (data[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }

        if (data.length >= 12
                && data[0] == 'R'
                && data[1] == 'I'
                && data[2] == 'F'
                && data[3] == 'F'
                && data[8] == 'W'
                && data[9] == 'E'
                && data[10] == 'B'
                && data[11] == 'P') {
            return "image/webp";
        }

        return null;
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        mapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}
