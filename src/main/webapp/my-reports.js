const reportsList = document.getElementById("my-reports-list");
const reportsMessage = document.getElementById("my-reports-message");
const logoutButton = document.getElementById("logout-button");

let csrfToken = null;

async function loadMyReports() {
    try {
        const sessionResponse = await fetch("api/auth/me");
        const session = await sessionResponse.json();

        if (!session.authenticated) {
            window.location.href = "auth.html";
            return;
        }

        csrfToken = session.csrfToken;

        const response = await fetch("api/items/mine");
        const items = await response.json();

        if (!response.ok) throw new Error(items.error || "Could not load your reports.");

        renderReports(items);
    } catch (error) {
        reportsList.innerHTML = "";
        reportsMessage.textContent = error.message;
        reportsMessage.className = "form-message error";
    }
}

function renderReports(items) {
    reportsList.innerHTML = "";

    if (items.length === 0) {
        reportsList.innerHTML = '<p class="message">You have not submitted any reports yet.</p>';
        return;
    }

    items.forEach(function(item) {
        const card = document.createElement("article");
        card.className = "item-card";

        const top = document.createElement("div");
        top.className = "card-top";

        const tag = document.createElement("span");
        tag.className = `tag ${item.type.toLowerCase()}`;
        tag.textContent = item.type;

        const status = document.createElement("span");
        status.className = item.status === "Open" ? "status-open" : "status-resolved";
        status.textContent = item.status;
        top.append(tag, status);

        const title = document.createElement("h3");
        title.textContent = item.name;

        const description = document.createElement("p");
        description.className = "description";
        description.textContent = item.description;

        card.append(top, title, description);

        if (item.has_image) {
            const image = document.createElement("img");
            image.className = "item-image";
            image.src = `api/items/${item.id}/image`;
            image.alt = `Photo of ${item.name}`;
            image.loading = "lazy";
            card.appendChild(image);
        }

        const details = document.createElement("div");
        details.className = "item-details";
        [
            `Category: ${item.category}`,
            `Location: ${item.location}`,
            `Date: ${item.item_date}`,
            `Contact: ${item.contact}`
        ].forEach(function(text) {
            const line = document.createElement("p");
            line.textContent = text;
            details.appendChild(line);
        });

        card.appendChild(details);

        if (item.status === "Open") {
            const resolveButton = document.createElement("button");
            resolveButton.className = "return-button";
            resolveButton.type = "button";
            resolveButton.textContent = item.type === "Lost" ? "Mark as found" : "Mark as returned";
            resolveButton.addEventListener("click", function() {
                resolveReport(item.id);
            });
            card.appendChild(resolveButton);
        }

        const deleteButton = document.createElement("button");
        deleteButton.className = "delete-button";
        deleteButton.type = "button";
        deleteButton.textContent = "Delete report";
        deleteButton.addEventListener("click", function() {
            deleteReport(item.id);
        });
        card.appendChild(deleteButton);

        reportsList.appendChild(card);
    });
}

async function resolveReport(id) {
    if (!confirm("Mark this report as resolved? It will disappear from the public reports page.")) return;

    try {
        const response = await fetch(`api/items/${id}/resolve`, {
            method: "PATCH",
            headers: { "X-CSRF-Token": csrfToken }
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.error || "Could not update report.");

        reportsMessage.textContent = result.message;
        reportsMessage.className = "form-message success";
        await loadMyReports();
    } catch (error) {
        reportsMessage.textContent = error.message;
        reportsMessage.className = "form-message error";
    }
}

async function deleteReport(id) {
    if (!confirm("Delete this report permanently? This cannot be undone.")) return;

    try {
        const response = await fetch(`api/items/${id}`, {
            method: "DELETE",
            headers: { "X-CSRF-Token": csrfToken }
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.error || "Could not delete report.");

        reportsMessage.textContent = result.message;
        reportsMessage.className = "form-message success";
        await loadMyReports();
    } catch (error) {
        reportsMessage.textContent = error.message;
        reportsMessage.className = "form-message error";
    }
}

logoutButton.addEventListener("click", async function() {
    await fetch("api/auth/logout", {
        method: "POST",
        headers: { "X-CSRF-Token": csrfToken }
    });
    window.location.href = "index.html";
});

loadMyReports();
