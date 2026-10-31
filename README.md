# CampusFind

CampusFind is a campus Lost & Found web application built with HTML, CSS, JavaScript, Java Servlets, JDBC, MariaDB/MySQL, Maven and Apache Tomcat 10.1.

## Features

- Submit Lost and Found reports.
- Browse and search item reports.
- Filter reports by type and category.
- View item descriptions, locations, dates and contact details.
- Mark an item as returned.
- Responsive frontend for desktop and mobile browsers.

## Requirements

- Linux operating system
- Java JDK 17
- Apache Maven
- MariaDB Server
- Git and curl
- Apache Tomcat 10.1
- A modern web browser

The commands below cover common Linux distributions. Choose the section matching your distribution, then continue with the shared installation steps. For another distribution, install the equivalent Java 17 JDK, Maven, MariaDB Server, Git and curl packages using its package manager.

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

Clone the repository and enter the project directory:

```bash
git clone https://github.com/krish-62/CampusFInd.git
cd CampusFInd
```

## 3. Set up MariaDB

Start the MariaDB service if it is not already running. Then create the database and table from the project SQL file:

```bash
sudo mariadb < database.sql
```

Create the application account:

```bash
sudo mariadb -e "CREATE USER IF NOT EXISTS 'campus_user'@'localhost' IDENTIFIED BY ''; ALTER USER 'campus_user'@'localhost' IDENTIFIED BY ''; GRANT SELECT, INSERT, UPDATE ON campusfind.* TO 'campus_user'@'localhost'; FLUSH PRIVILEGES;"
```

Test the account:

```bash
mariadb -u campus_user campusfind -e "SELECT 1; SHOW TABLES;"
```

The application uses the local MariaDB account `campus_user` with an empty password. This means the application does not prompt for a database password.

**Security:** This passwordless account is only for a local learning environment. Keep MariaDB accessible only from the same computer. Do not expose port 3306 to a network or the internet, and do not use this configuration on a shared or production server.

## 4. Install Apache Tomcat 10.1

The project uses Jakarta Servlet 6 and requires Tomcat 10.1. The following commands install Tomcat in your home directory, so they work independently of the Linux distribution and do not require a system Tomcat package.

```bash
TOMCAT_VERSION=10.1.61
cd "$HOME"
curl -fLO "https://downloads.apache.org/tomcat/tomcat-10/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz"
tar -xzf "apache-tomcat-${TOMCAT_VERSION}.tar.gz"
mv "apache-tomcat-${TOMCAT_VERSION}" "$HOME/tomcat10"
export CATALINA_HOME="$HOME/tomcat10"
```

Keep using the same terminal for the remaining commands in this guide. If you open a new terminal later, set the Tomcat location again:

```bash
export CATALINA_HOME="$HOME/tomcat10"
```

## 5. Build the application

From the project directory, run:

```bash
mvn clean package
```

When the build succeeds, Maven creates:

```text
target/campusfind.war
```

## 6. Deploy and start CampusFind

Copy the generated WAR file into Tomcat's web application directory:

```bash
cp target/campusfind.war "$CATALINA_HOME/webapps/"
```

Start Tomcat:

```bash
"$CATALINA_HOME/bin/startup.sh"
```

Tomcat automatically deploys the WAR file. Open the application in your browser:

```text
http://localhost:8080/campusfind/
```

Test the backend API:

```bash
curl -i http://localhost:8080/campusfind/api/items
```

A successful API request returns `HTTP 200` and a JSON array. If there are no reports yet, the response is an empty array: `[]`.

## 7. Stop Tomcat

To stop the local server:

```bash
"$CATALINA_HOME/bin/shutdown.sh"
```

If you open a new terminal, set `CATALINA_HOME` again before using the Tomcat scripts.

## 8. Updating the project

When you want to download the latest repository changes:

```bash
cd "$HOME/CampusFInd"
git pull
mvn clean package
cp target/campusfind.war "$CATALINA_HOME/webapps/"
```

If Tomcat is already running, stop it before replacing the WAR, remove the previously extracted application directory, copy the new WAR and start Tomcat again:

```bash
"$CATALINA_HOME/bin/shutdown.sh"
rm -rf "$CATALINA_HOME/webapps/campusfind"
cp target/campusfind.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/startup.sh"
```

## Troubleshooting

### MariaDB is not running

On systemd-based distributions:

```bash
sudo systemctl status mariadb
sudo systemctl start mariadb
```

### Check the database account and table

```bash
mariadb -u campus_user campusfind -e "SELECT COUNT(*) FROM items;"
```

### Check the API

```bash
curl -i http://localhost:8080/campusfind/api/items
```

### Check whether port 8080 is already in use

```bash
ss -ltnp | grep ':8080'
```

### View Tomcat logs

```bash
tail -n 100 "$CATALINA_HOME/logs/catalina.out"
```

## Project structure

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

## Project limitations

This is a learning project. It does not currently include user login or role-based access control. Anyone who can access the application can submit reports and change an item's status. Do not expose this instance to the public internet. Add authentication, authorization, input-abuse controls and appropriate privacy protections before deploying it for real campus-wide use.
