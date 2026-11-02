# UnLost

UnLost is a campus Lost & Found website made with HTML, CSS, JavaScript, Java Servlets, JDBC, MariaDB/MySQL, Maven and Apache Tomcat.

## Features

- Create an account and sign in with a username or email.
- Submit Lost and Found reports with an optional photo.
- Search and filter open reports.
- View your own reports, including resolved reports.
- Only the owner of a report can resolve or delete it.
- Resolved reports no longer appear on the public page.

## Requirements

Install these tools using your Linux distribution's package manager:

- Java JDK 17
- Apache Maven
- MariaDB Server
- Git and curl
- Apache Tomcat 10.1

Example package commands:

Ubuntu or Debian:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven mariadb-server git curl
```

Fedora:

```bash
sudo dnf install -y java-17-openjdk-devel maven mariadb-server git curl
```

Arch Linux:

```bash
sudo pacman -Syu --needed jdk17-openjdk maven mariadb git curl
```

For other distributions, install the same tools using their package manager. Start MariaDB using your system's service manager. On systemd systems:

```bash
sudo systemctl enable --now mariadb
```

Check the installations:

```bash
java -version
mvn -version
mariadb --version
```

Java should report version 17.

## 1. Download UnLost

```bash
git clone https://github.com/krish-62/UnLost.git
cd UnLost
```

## 2. Set up the database

For a new installation, create the database tables:

```bash
sudo mariadb < database.sql
```

Create the local database account used by the application:

```bash
sudo mariadb <<'SQL'
CREATE USER IF NOT EXISTS 'unlost_user'@'localhost' IDENTIFIED BY '';
ALTER USER 'unlost_user'@'localhost' IDENTIFIED BY '';
GRANT SELECT, INSERT, UPDATE ON unlost.users TO 'unlost_user'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON unlost.items TO 'unlost_user'@'localhost';
FLUSH PRIVILEGES;
SQL
```

Check the database:

```bash
mariadb -u unlost_user unlost -e "SHOW TABLES;"
```

The application uses an empty database password for this local classroom setup. Keep MariaDB accessible only from your own machine. Do not expose this database account or port 3306 to a network.

### Existing UnLost database

When upgrading an older UnLost database that already contains the `items` table, do not run `database.sql` over it. First make a backup:

```bash
sudo mariadb-dump unlost > "$HOME/unlost-backup.sql"
```

Download and apply the migration once:

```bash
curl -fL https://raw.githubusercontent.com/krish-62/UnLost/main/migration.sql -o migration.sql
sudo mariadb < migration.sql
```

Then grant the required permissions:

```bash
sudo mariadb -e "GRANT SELECT, INSERT, UPDATE ON unlost.users TO 'unlost_user'@'localhost'; GRANT SELECT, INSERT, UPDATE, DELETE ON unlost.items TO 'unlost_user'@'localhost'; FLUSH PRIVILEGES;"
```

The migration keeps old reports, but reports created before accounts existed have no owner. They stay public while open and cannot be managed from an account until an owner is assigned. Do not run the migration more than once.

## 3. Install Apache Tomcat 10.1

The application uses Jakarta Servlet 6.0, so use Tomcat 10.1.

Download and extract Tomcat into your home directory:

```bash
cd "$HOME"
curl -fLO https://downloads.apache.org/tomcat/tomcat-10/v10.1.61/bin/apache-tomcat-10.1.61.tar.gz
tar -xzf apache-tomcat-10.1.61.tar.gz
mv apache-tomcat-10.1.61 tomcat10
export CATALINA_HOME="$HOME/tomcat10"
```

The `CATALINA_HOME` command sets the Tomcat folder for the current terminal. Run it again when you open a new terminal.

## 4. Build the project

From the UnLost project folder:

```bash
mvn clean package
```

When Maven finishes successfully, it creates:

```text
target/unlost.war
```

## 5. Deploy and run

Copy the WAR file to Tomcat and start the server:

```bash
cp target/unlost.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/startup.sh"
```

Open the application:

- Website: http://localhost:8080/unlost/
- Sign in or register: http://localhost:8080/unlost/auth.html
- My reports: http://localhost:8080/unlost/my-reports.html

To stop Tomcat:

```bash
"$CATALINA_HOME/bin/shutdown.sh"
```

## 6. Updating the project

After pulling new changes:

```bash
git pull origin main
mvn clean package
"$CATALINA_HOME/bin/shutdown.sh"
rm -rf "$CATALINA_HOME/webapps/unlost"
cp target/unlost.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/startup.sh"
```

## Project structure

```text
UnLost/
├── database.sql
├── migration.sql
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/unlost/
    │   ├── AuthServlet.java
    │   ├── Database.java
    │   ├── ItemServlet.java
    │   └── PasswordUtil.java
    └── webapp/
        ├── index.html
        ├── auth.html
        ├── my-reports.html
        ├── style.css
        ├── app.js
        ├── auth.js
        └── my-reports.js
```

## What each file does

- `AuthServlet.java`: registration, login, logout and session handling.
- `ItemServlet.java`: listing, creating, resolving, deleting reports and serving item photos.
- `PasswordUtil.java`: password hashing and checking.
- `Database.java`: opens the MariaDB/MySQL connection.
- `database.sql`: creates tables for a fresh installation.
- `migration.sql`: upgrades the earlier database structure.
- `index.html` and `app.js`: public reports and the report form.
- `auth.html` and `auth.js`: sign-in and registration page.
- `my-reports.html` and `my-reports.js`: the signed-in user's report history.
- `style.css`: styling shared by all pages.
- `pom.xml`: Maven dependencies and WAR build settings.

## Project limitations

This is a student learning project. It does not include email verification, password reset, login rate limiting or administrator moderation. The passwordless database account is only for local demonstration, not public hosting.
