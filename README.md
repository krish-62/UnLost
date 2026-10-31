# CampusFind — Java Lost & Found Mini Project

CampusFind is a beginner-friendly full-stack campus Lost & Found application built with HTML, CSS, JavaScript, Java Servlets, JDBC, MySQL/MariaDB, Maven and Apache Tomcat 10.1.

## Features

- Submit Lost and Found reports.
- Store reports in MySQL/MariaDB.
- Search by item name, location or description.
- Filter by report type and category.
- Display item date, location and contact details.
- Mark an item as returned.
- Responsive desktop and mobile layout.

## Requirements

- Java JDK 17 or newer
- Maven
- MySQL or MariaDB
- Apache Tomcat 10.1 (Jakarta Servlet 6)
- A modern web browser

## 1. Set up the database

Run the schema from the project root:

```bash
mysql -u root -p < database.sql
```

On MariaDB installations where root authentication uses the local socket, you may instead use:

```bash
sudo mariadb < database.sql
```

Create a dedicated application database user with only the privileges required by the application (SELECT, INSERT and UPDATE on the `campusfind` database). Avoid using the database root account for the web application.

## 2. Configure database credentials safely

The application reads these environment variables in the Tomcat service process:

- `CAMPUSFIND_DB_URL` — optional; defaults to `jdbc:mysql://localhost:3306/campusfind?useSSL=false&serverTimezone=UTC`.
- `CAMPUSFIND_DB_USER` — required.
- `CAMPUSFIND_DB_PASSWORD` — required.

Configure them for the Tomcat service using your operating system's service configuration. For systemd-based installations, a service drop-in can define environment variables. Do not paste real credentials into source code, commit them, or place them in a tracked file. The `.env.example` file contains placeholders only; Tomcat does not automatically load it.

## 3. Build

From the directory containing `pom.xml`:

```bash
mvn clean package
```

Maven creates `target/campusfind.war` when the build succeeds.

## 4. Deploy to Tomcat 10.1

Copy `target/campusfind.war` into Tomcat's `webapps` directory and start/restart Tomcat using your installation's service or scripts. For a manual Tomcat installation, the deployment directory is typically:

```text
$CATALINA_HOME/webapps/
```

Open the application at:

```text
http://localhost:8080/campusfind/
```

This project uses Jakarta Servlet 6 and is intended for Tomcat 10.1; it is not directly compatible with Tomcat 9's older javax.servlet API.

## 5. How it works

1. The browser loads `index.html`, `style.css` and `app.js`.
2. JavaScript sends requests to the `/api/items` servlet endpoint.
3. `ItemServlet` validates requests and uses JDBC prepared statements to query or update the database.
4. The servlet returns JSON for the frontend to render.
5. Submitting a report sends a POST request; marking an item returned sends a PATCH request.

## Security and project scope

This is a learning/classroom prototype. It does not implement user login or role-based authorization: anyone who can reach the application can submit reports and change an item's returned status. Before any public deployment, add authentication, authorization, abuse/rate limiting, privacy protections for contact details, and a security review. Keep credentials outside source control and use a least-privilege database account.

## Repository layout

```text
CampusFInd/
├── database.sql
├── pom.xml
├── src/main/java/com/campusfind/
│   ├── Database.java
│   └── ItemServlet.java
└── src/main/webapp/
    ├── app.js
    ├── index.html
    └── style.css
```
