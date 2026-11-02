const itemsList = document.getElementById("items-list");
const itemCount = document.getElementById("item-count");
const searchInput = document.getElementById("search");
const typeFilter = document.getElementById("type-filter");
const categoryFilter = document.getElementById("category-filter");
const searchButton = document.getElementById("search-button");
const reportForm = document.getElementById("report-form");
const formMessage = document.getElementById("form-message");
const imageInput = document.getElementById("image");
const imagePreview = document.getElementById("image-preview");
const loginRequired = document.getElementById("login-required");
const authLink = document.getElementById("auth-link");
const myReportsLink = document.getElementById("my-reports-link");
const logoutButton = document.getElementById("logout-button");

let csrfToken = null;
let previewUrl = null;

async function loadSession() {
    try {
        const response = await fetch("api/auth/me");
        const result = await response.json();

        if (result.authenticated) {
            csrfToken = result.csrfToken;
            authLink.textContent = result.user.username;
            authLink.href = "my-reports.html";
            myReportsLink.hidden = false;
            logoutButton.hidden = false;
            loginRequired.hidden = true;
            reportForm.hidden = false;
        } else {
            loginRequired.hidden = false;
            reportForm.hidden = true;
        }
    } catch (error) {
        loginRequired.hidden = false;
        reportForm.hidden = true;
    }
}

async function loadItems() {
    itemsList.innerHTML = '<p class="message">Loading reports...</p>';

    const params = new URLSearchParams({
        search: searchInput.value.trim(),
        type: typeFilter.value,
        category: categoryFilter.value
    });

    try {
        const response = await fetch(`api/items?${params.toString()}`);
        if (!response.ok) throw new Error("Could not load reports.");

        const items = await response.json();
        showItems(items);
    } catch (error) {
        itemsList.innerHTML = '<p class="message">Could not load reports. Please try again.</p>';
        itemCount.textContent = "";
    }
}

function showItems(items) {
    itemCount.textContent = `${items.length} report${items.length === 1 ? "" : "s"}`;

    if (items.length === 0) {
        itemsList.innerHTML = '<p class="message">No open reports found.</p>';
        return;
    }

    itemsList.innerHTML = "";

    items.forEach(function(item) {
        const card = document.createElement("article");
        card.className = "item-card";

        const top = document.createElement("div");
        top.className = "card-top";

        const tag = document.createElement("span");
        tag.className = `tag ${item.type.toLowerCase()}`;
        tag.textContent = item.type;

        const category = document.createElement("span");
        category.className = "category";
        category.textContent = item.category;
        top.append(tag, category);

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

        const location = document.createElement("p");
        location.textContent = `Location: ${item.location}`;

        const date = document.createElement("p");
        date.textContent = `Date: ${item.item_date}`;

        const contact = document.createElement("p");
        contact.textContent = `Contact: ${item.contact}`;

        const reporter = document.createElement("p");
        reporter.textContent = `Reported by: ${item.reporter || "Campus member"}`;

        details.append(location, date, contact, reporter);
        card.appendChild(details);
        itemsList.appendChild(card);
    });
}

reportForm.addEventListener("submit", async function(event) {
    event.preventDefault();

    formMessage.textContent = "";
    formMessage.className = "form-message";

    const formData = new FormData(reportForm);

    try {
        const response = await fetch("api/items", {
            method: "POST",
            headers: { "X-CSRF-Token": csrfToken },
            body: formData
        });

        const result = await response.json();
        if (!response.ok) throw new Error(result.error || "Could not save report.");

        formMessage.textContent = "Your report has been added.";
        formMessage.classList.add("success");
        reportForm.reset();
        clearPreview();

        searchInput.value = "";
        typeFilter.value = "All";
        categoryFilter.value = "All";
        await loadItems();

        document.getElementById("items").scrollIntoView({ behavior: "smooth" });
    } catch (error) {
        formMessage.textContent = error.message;
        formMessage.classList.add("error");
    }
});

function clearPreview() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    previewUrl = null;
    imagePreview.removeAttribute("src");
    imagePreview.hidden = true;
}

imageInput.addEventListener("change", function() {
    clearPreview();
    const file = imageInput.files[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
        formMessage.textContent = "Image must be 5 MB or smaller.";
        formMessage.className = "form-message error";
        imageInput.value = "";
        return;
    }

    previewUrl = URL.createObjectURL(file);
    imagePreview.src = previewUrl;
    imagePreview.hidden = false;
});

logoutButton.addEventListener("click", async function() {
    try {
        await fetch("api/auth/logout", {
            method: "POST",
            headers: { "X-CSRF-Token": csrfToken }
        });
        window.location.href = "index.html";
    } catch (error) {
        window.location.href = "index.html";
    }
});

searchButton.addEventListener("click", loadItems);
typeFilter.addEventListener("change", loadItems);
categoryFilter.addEventListener("change", loadItems);

searchInput.addEventListener("keydown", function(event) {
    if (event.key === "Enter") {
        event.preventDefault();
        loadItems();
    }
});

loadSession();
loadItems();
