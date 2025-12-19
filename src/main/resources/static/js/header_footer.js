// Create header.html content as a string
const headerHTML = `
            <header>
                <div class="logo">
                    <img src="/web_images/Icon.png" alt="Schneaggchat Icon" style="height:1em; vertical-align:middle;">
                    <span>Schneaggchat</span>
                </div>
                <nav class="nav-links">
                    <a href="/">Home</a>
                    <a href="/stats.html">Stats</a>
                    <a href="/privacypolicy.html">Datenschutz</a>
                    <a href="/donations.html">Spenden</a>
                </nav>
                <button class="mobile-toggle">
                    <i class="fas fa-bars"></i>
                </button>
            </header>
        `;

// Create footer.html content as a string
const footerHTML = `
            <footer>
                <div class="footer-columns">
                    <div class="footer-column">
                        <h4>Navigation</h4>
                        <a href="/">Home</a>
                        <a href="/stats.html">Stats</a>
                    </div>
                    <div class="footer-column">
                        <h4>Rechtliches</h4>
                        <a href="/privacypolicy.html">Datenschutz</a>
                    </div>
                    <div class="footer-column">
                        <h4>Support</h4>
                        <a href="/donations.html">Spenden</a>
                        <a href="/delete_account.html">Account löschen</a>
                    </div>
                </div>
            </footer>
        `;

// Inject header and footer into containers
document.getElementById('header-container').innerHTML = headerHTML;
document.getElementById('footer-container').innerHTML = footerHTML;

// Initialize functionality after components are injected
// Initialize functionality
function initPage() {
    // Logo click handler
    document.querySelector('.logo')?.addEventListener('click', () => {
        window.location.href = '/';
    });

    // Mobile menu toggle - UNIVERSAL VERSION
    const mobileToggle = document.querySelector('.mobile-toggle');
    const navLinks = document.querySelector('.nav-links');

    if (mobileToggle && navLinks) {
        mobileToggle.addEventListener('click', function (e) {
            e.stopPropagation(); // Prevent click from bubbling to document
            navLinks.classList.toggle('active');
        });

        // Close menu when clicking outside
        document.addEventListener('click', function (e) {
            if (navLinks.classList.contains('active') && !navLinks.contains(e.target)) {
                navLinks.classList.remove('active');
            }
        });
    }

    // Universal scroll handler
    window.addEventListener('scroll', function () {
        const header = document.querySelector('header');
        if (header) {
            header.classList.toggle('scrolled', window.scrollY > 50);
        }
    });
}

// Initialize when elements are ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initPage);
} else {
    initPage(); // In case DOM is already ready
}