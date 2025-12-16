// Donations Data Configuration
// To add a new donation, simply add a new object to the 'donations' array below
// Format:
// {
//     name: "Donor Name",
//     amount: 25.00,
//     date: "15. Dezember 2025"
// }
// Note: Icons are randomly selected automatically!

// Available icons for random selection
const iconOptions = [
    "fas fa-heart",
    "fas fa-star",
    "fas fa-rocket",
    "fas fa-gift",
    "fas fa-coffee",
    "fas fa-thumbs-up",
    "fas fa-hand-holding-heart",
    "fas fa-trophy",
    "fas fa-medal",
    "fas fa-crown",
    "fas fa-gem",
    "fas fa-fire"
];

const donationsConfig = {
    // PayPal donation link
    paypalLink: "https://paypal.me/florianlerchenmuelle",

    // All donations (newest first)
    // No need to specify icons - they're assigned randomly!
    donations: [
        {
            name: "FLo",
            amount: 125.00,
            date: "15. Dezember 2025"
        },

    ]
};

// Get a random icon from the available options
function getRandomIcon() {
    return iconOptions[Math.floor(Math.random() * iconOptions.length)];
}

// Calculate total donations automatically
function calculateTotal() {
    return donationsConfig.donations.reduce((sum, donation) => sum + donation.amount, 0);
}

// Render donations to the page
function renderDonations() {
    const donationsGrid = document.querySelector('.donations-grid');
    const totalAmountElement = document.querySelector('.total-amount');

    // Clear existing content
    donationsGrid.innerHTML = '';

    // Render each donation
    donationsConfig.donations.forEach(donation => {
        const donationCard = document.createElement('div');
        donationCard.className = 'donation-card';

        // Use random icon
        const randomIcon = getRandomIcon();

        donationCard.innerHTML = `
            <div class="donation-icon">
                <i class="${randomIcon}"></i>
            </div>
            <h4 class="donation-name">${donation.name}</h4>
            <p class="donation-amount">€${donation.amount.toFixed(2)}</p>
            <p class="donation-date">${donation.date}</p>
        `;

        donationsGrid.appendChild(donationCard);
    });

    // Update total
    const total = calculateTotal();
    totalAmountElement.textContent = `€${total.toFixed(2)}`;
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', renderDonations);
} else {
    renderDonations();
}
