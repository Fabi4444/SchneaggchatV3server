// Donations Data Configuration
// To add a new donation, simply add a new object to the 'donations' array below
// Format:
// {
//     name: "Donor Name",
//     amount: 25.00,
//     date: "15. Dezember 2025",
//     message: "Optional message from the donor" // Optional field
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
            name: "Petra",
            amount: 50.00,
            date: "27. Jänner 2026",
            message: "Gratuliere"
        },
        {
            name: "Jonny",
            amount: 5.00,
            date: "14. Jänner 2026",
            message: "Liebe Grüße"
        },
        {
            name: "Daffith",
            amount: 10.00,
            date: "24. Dezember 2025",
            message: "Weihnachtsspende"
        },
        {
            name: "Herr Ess",
            amount: 20.00,
            date: "14. Oktober 2025",
            message: "Schneaggchat"
        },
        {
            name: "Norbert Konrad",
            amount: 20.00,
            date: "28. Mai 2025",
            message: "Dra bliba buaba..."
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

// Animate value function (copied/adapted from stats.html logic)
function animateValue(element, target) {
    let current = 0;
    const increment = target / 50; // 50 steps
    const timer = setInterval(() => {
        current += increment;
        if (current >= target) {
            element.textContent = `€${target.toFixed(2)}`;
            clearInterval(timer);
        } else {
            element.textContent = `€${current.toFixed(2)}`;
        }
    }, 20); // 20ms interval -> ~1 second total duration
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
            <p class="donation-amount">€0.00</p>
            <p class="donation-date">${donation.date}</p>
            ${donation.message ? `<p class="donation-message">"${donation.message}"</p>` : ''}
        `;

        donationsGrid.appendChild(donationCard);

        // Animate the individual donation amount
        const amountElement = donationCard.querySelector('.donation-amount');
        animateValue(amountElement, donation.amount);
    });

    // Update and animate total
    const total = calculateTotal();
    animateValue(totalAmountElement, total);
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', renderDonations);
} else {
    renderDonations();
}
