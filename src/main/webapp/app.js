const itemsList = document.getElementById("items-list");
const itemCount = document.getElementById("item-count");
const searchInput = document.getElementById("search");
const typeFilter = document.getElementById("type-filter");
const categoryFilter = document.getElementById("category-filter");
const searchButton = document.getElementById("search-button");
const reportForm = document.getElementById("report-form");
const formMessage = document.getElementById("form-message");

async function loadItems() {
    itemsList.innerHTML = '<p class="message">Loading reports...</p>';

    const params = new URLSearchParams({
        search: searchInput.value.trim(),
        type: typeFilter.value,
        category: categoryFilter.value,
        status: "Open"
    });

    try {
        const response = await fetch(`api/items?${params.toString()}`);
        if (!response.ok) throw new Error("Could not load reports.");

        const items = await response.json();
        showItems(items);
    } catch (error) {
        itemsList.innerHTML =
            '<p class="message">Could not load reports. Check that Tomcat and MySQL are running.</p>';
        itemCount.textContent = "";
    }
}

function showItems(items) {
    itemCount.textContent = `${items.length} report${items.length === 1 ? "" : "s"}`;

    if (items.length === 0) {
        itemsList.innerHTML =
            '<p class="message">No reports found. You can be the first to add one.</p>';
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

        const details = document.createElement("div");
        details.className = "item-details";

        const location = document.createElement("p");
        location.textContent = `Location: ${item.location}`;

        const date = document.createElement("p");
        date.textContent = `Date: ${item.item_date}`;

        const contact = document.createElement("p");
        contact.textContent = `Contact: ${item.contact}`;

        details.append(location, date, contact);
        card.append(top, title, description, details);

        const returnedButton = document.createElement("button");
        returnedButton.className = "return-button";
        returnedButton.type = "button";
        returnedButton.textContent = "Mark as returned";
        returnedButton.addEventListener("click", function() {
            markReturned(item.id);
        });

        card.appendChild(returnedButton);
        itemsList.appendChild(card);
    });
}

reportForm.addEventListener("submit", async function(event) {
    event.preventDefault();

    formMessage.textContent = "";
    formMessage.className = "form-message";

    const formData = new FormData(reportForm);
    const item = Object.fromEntries(formData.entries());

    try {
        const response = await fetch("api/items", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(item)
        });

        const result = await response.json();
        if (!response.ok) throw new Error(result.error || "Could not save report.");

        formMessage.textContent = "Your report has been added.";
        formMessage.classList.add("success");
        reportForm.reset();

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

async function markReturned(id) {
    if (!confirm("Has this item been returned to its owner?")) return;

    try {
        const response = await fetch(`api/items/${id}/returned`, {
            method: "PATCH"
        });

        const result = await response.json();
        if (!response.ok) throw new Error(result.error || "Could not update item.");

        await loadItems();
    } catch (error) {
        alert(error.message);
    }
}

searchButton.addEventListener("click", loadItems);
typeFilter.addEventListener("change", loadItems);
categoryFilter.addEventListener("change", loadItems);

searchInput.addEventListener("keydown", function(event) {
    if (event.key === "Enter") {
        event.preventDefault();
        loadItems();
    }
});

loadItems();
