# UnLost

UnLost is a campus Lost & Found web application built with HTML, CSS, JavaScript, Java Servlets, JDBC, MariaDB/MySQL, Maven and Apache Tomcat 10.1.

## Features

- Register and sign in with a username or email and password.
- Store passwords as salted PBKDF2 hashes.
- Browse, search and filter open Lost and Found reports.
- Upload a JPG, PNG or WEBP photo with a report (up to 5 MB).
- View reports submitted by the signed-in account.
- Only the account that created a report can mark it as resolved or delete it.
- Resolved reports are removed from the public reports page.
- Keep resolved reports in the owner's report history.

## Requirements

- Linux operating system
- Java JDK 17
- Apache Maven
- MariaDB Server
- Git and curl
- Apache Tomcat 10.1
- A modern web browser

## 1. Install the required packages

### Debian, Ubuntu, Linux Mint and other APT-based distributions

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven mariadb-server git curl
```

### Fedora, RHEL, Rocky Linux and AlmaLinux

```bash
sudo dnf install -y java-17-openjdk-devel maven mariadb-server git curl
```

### Arch Linux and Arch-based distributions

```bash
sudo pacman -Syu --needed jdk17-openjdk maven mariadb git curl
```

On a brand-new Arch Linux installation, initialize MariaDB once before starting it:

```bash
sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
```

Do not run the initialization command again on an existing database installation.

### openSUSE

```bash
sudo zypper refresh
sudo zypper install -y java-17-openjdk-devel maven mariadb git curl
```

### Alpine Linux

```bash
sudo apk update
sudo apk add openjdk17 maven mariadb mariadb-client git curl
sudo rc-service mariadb setup
sudo rc-update add mariadb default
sudo rc-service mariadb start
```

On systemd-based distributions, start MariaDB with:

```bash
sudo systemctl enable --now mariadb
```

On distributions that use a different service manager, start and enable MariaDB using that distribution's service commands.

Verify the tools:

```bash
java -version
mvn -version
mariadb --version
git --version
curl --version
```

Make sure Java reports version 17.

## 2. Download the project

```bash
cd "$HOME"
git clone https://github.com/krish-62/UnLost.git
cd "$HOME/UnLost"
```

## 3. Set up MariaDB for a new installation

Run the schema:

```bash
sudo mariadb < database.sql
```

Create the local application account and grant only the permissions used by the application:

```bash
sudo mariadb <<'SQL'
CREATE USER IF NOT EXISTS 'unlost_user'@'localhost' IDENTIFIED BY '';
ALTER USER 'unlost_user'@'localhost' IDENTIFIED BY '';
GRANT SELECT, INSERT, UPDATE ON unlost.users TO 'unlost_user'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON unlost.items TO 'unlost_user'@'localhost';
FLUSH PRIVILEGES;
SQL
```

Test the application account:

```bash
mariadb -u unlost_user unlost -e "SHOW TABLES;"
```

The local application account has an empty database password to keep this local learning setup from prompting for a database password. Keep MariaDB bound to localhost and do not expose port 3306 to other computers or the internet. Do not use this passwordless account on a shared or production server.

## 4. Upgrade an existing UnLost database

Use this section only when upgrading the earlier UnLost database that already has an `items` table.

First make a backup:

```bash
sudo mariadb-dump unlost > "$HOME/unlost-before-account-upgrade.sql"
```

Download the migration file:

```bash
curl -fL https://raw.githubusercontent.com/krish-62/UnLost/main/migration.sql -o migration.sql
```

Apply the migration once:

```bash
sudo mariadb < migration.sql
```

Grant the additional permissions required by the new account and report features:

```bash
sudo mariadb -e "GRANT SELECT, INSERT, UPDATE ON unlost.users TO 'unlost_user'@'localhost'; GRANT SELECT, INSERT, UPDATE, DELETE ON unlost.items TO 'unlost_user'@'localhost'; FLUSH PRIVILEGES;"
```

The migration preserves old reports and changes the earlier `Returned` status to `Resolved`. Reports created before accounts existed have no owner, so they remain visible while open but cannot be resolved or deleted through the account interface. After creating your account, you can optionally assign those older reports to it from MariaDB:

```sql
UPDATE unlost.items
SET user_id = (SELECT id FROM unlost.users WHERE username = 'YOUR_USERNAME')
WHERE user_id IS NULL;
```

Replace `YOUR_USERNAME` with the account that should own those earlier reports. Run this only if you are sure those unassigned reports belong to that account.

Do not run `migration.sql` more than once on the same database. Fresh installations should use `database.sql`, not `migration.sql`.

## 5. Install Apache Tomcat 10.1

The project uses Jakarta Servlet 6 and requires Tomcat 10.1. These commands install Tomcat in your home directory, independently of the Linux distribution:

```bash
TOMCAT_VERSION=10.1.61
cd "$HOME"
curl -fLO "https://downloads.apache.org/tomcat/tomcat-10/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz"
tar -xzf "apache-tomcat-${TOMCAT_VERSION}.tar.gz"
mv "apache-tomcat-${TOMCAT_VERSION}" "$HOME/tomcat10"
export CATALINA_HOME="$HOME/tomcat10"
```

If you open a new terminal later, set the Tomcat location again:

```bash
export CATALINA_HOME="$HOME/tomcat10"
```

## 6. Build the application

From the project directory:

```bash
mvn clean package
```

Maven creates:

```text
target/unlost.war
```

## 7. Deploy and start UnLost

```bash
cp target/unlost.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/startup.sh"
```

Open the website:

```text
http://localhost:8080/unlost/
```

Open the sign-in and registration page:

```text
http://localhost:8080/unlost/auth.html
```

Open your report history after signing in:

```text
http://localhost:8080/unlost/my-reports.html
```

Test the public API:

```bash
curl -i http://localhost:8080/unlost/api/items
```

## 8. Stop Tomcat

```bash
"$CATALINA_HOME/bin/shutdown.sh"
```

## 9. Update the project

```bash
cd "$HOME/UnLost"
git pull
mvn clean package
"$CATALINA_HOME/bin/shutdown.sh"
rm -rf "$CATALINA_HOME/webapps/unlost"
cp target/unlost.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/startup.sh"
```

## Troubleshooting

Check MariaDB:

```bash
sudo systemctl status mariadb
```

Check the database tables:

```bash
mariadb -u unlost_user unlost -e "SHOW TABLES;"
```

Check the public reports API:

```bash
curl -i http://localhost:8080/unlost/api/items
```

Check whether port 8080 is in use:

```bash
ss -ltnp | grep ':8080'
```

View Tomcat logs:

```bash
tail -n 100 "$CATALINA_HOME/logs/catalina.out"
```

## Project structure

```text
UnLost/
├── database.sql
├── migration.sql
├── pom.xml
└── src/main/
    ├── java/com/unlost/
    │   ├── AuthServlet.java
    │   ├── Database.java
    │   ├── ItemServlet.java
    │   └── PasswordUtil.java
    └── webapp/
        ├── app.js
        ├── auth.html
        ├── auth.js
        ├── index.html
        ├── my-reports.html
        ├── my-reports.js
        └── style.css
```

## Security and project scope

Passwords are stored as salted PBKDF2 hashes and are never stored as plain text. State-changing requests use a session CSRF token, and the backend checks the signed-in account's ID before resolving or deleting a report. Image uploads are limited to 5 MB and checked for JPG, PNG or WEBP file signatures.

This is still a learning project. It does not yet include email verification, password reset, login rate limiting, account recovery or administrator moderation. The passwordless database account is intended only for a local machine. Use HTTPS, secure database credentials, rate limiting and a broader security review before deploying this application for public or campus-wide use.
