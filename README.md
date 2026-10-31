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

Create the database and a dedicated local application account. On the Arch Linux setup used for development, run:

```bash
sudo mariadb
```

At the MariaDB prompt, run:

```sql
CREATE DATABASE IF NOT EXISTS campusfind;
CREATE USER IF NOT EXISTS 'campus_user'@'localhost' IDENTIFIED BY '';
ALTER USER 'campus_user'@'localhost' IDENTIFIED BY '';
GRANT SELECT, INSERT, UPDATE ON campusfind.* TO 'campus_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

The application account intentionally has an empty password so the local development app does not ask for a database password. It is restricted to connections from `localhost` and has only the privileges needed by this application.

Import the table schema from the project root:

```bash
sudo mariadb campusfind < database.sql
```

You can test the application account without a password prompt:

```bash
mariadb -u campus_user campusfind
```

## 2. Database connection settings

By default, `Database.java` connects using:

- URL: `jdbc:mysql://localhost:3306/campusfind?useSSL=false&serverTimezone=UTC`
- Username: `campus_user`
- Password: empty

You can optionally override the URL or username with `CAMPUSFIND_DB_URL` or `CAMPUSFIND_DB_USER`. No database password environment variable is used by this local-development configuration.

**Security warning:** An account with an empty password is acceptable only for a local learning environment. Keep MariaDB bound to localhost, do not expose port 3306 to your network or the internet, and do not reuse this setup on a production server. For deployment on a shared or public server, use a strong password, secret management, and a restricted database account.

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

This is a learning/classroom prototype. It does not implement user login or role-based authorization: anyone who can reach the application can submit reports and change an item's returned status. Before any public deployment, add authentication, authorization, abuse/rate limiting, privacy protections for contact details, and a security review.

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
