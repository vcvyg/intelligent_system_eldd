function initFamilyMobileLayout(activeNav) {
    highlightBottomNav(activeNav);
}

function highlightBottomNav(activeNav) {
    if (!activeNav) {
        return;
    }
    const items = document.querySelectorAll('.bottom-nav-item[data-family-nav]');
    items.forEach(item => {
        const navKey = item.dataset.familyNav;
        if (!navKey) {
            return;
        }
        item.classList.toggle('active', navKey === activeNav);
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const activeNav = document.body ? document.body.dataset.familyNav : '';
    initFamilyMobileLayout(activeNav);
});

