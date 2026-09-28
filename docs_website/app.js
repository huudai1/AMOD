// --- Tab Switching Logic ---
const tabButtons = document.querySelectorAll('.nav-tab-btn');
const tabSections = document.querySelectorAll('.content-tab-section');

tabButtons.forEach(button => {
    button.addEventListener('click', () => {
        const targetTabId = button.getAttribute('data-tab');
        
        // Remove active class from all buttons and sections
        tabButtons.forEach(btn => btn.classList.remove('active'));
        tabSections.forEach(section => section.classList.remove('active'));
        
        // Add active class to clicked button and target section
        button.classList.add('active');
        const targetSection = document.getElementById(targetTabId);
        if (targetSection) {
            targetSection.classList.add('active');
        }
    });
});

// --- Code Copying Logic ---
const copyButtons = document.querySelectorAll('.copy-btn');
copyButtons.forEach(button => {
    button.addEventListener('click', () => {
        const targetId = button.getAttribute('data-target');
        const codeElement = document.getElementById(targetId);
        if (codeElement) {
            navigator.clipboard.writeText(codeElement.textContent)
                .then(() => {
                    const originalText = button.textContent;
                    button.textContent = 'Copied!';
                    button.style.background = 'hsl(190, 100%, 45%)';
                    button.style.color = 'hsl(222, 24%, 6%)';
                    
                    setTimeout(() => {
                        button.textContent = originalText;
                        button.style.background = '';
                        button.style.color = '';
                    }, 2000);
                })
                .catch(err => {
                    console.error('Could not copy text: ', err);
                });
        }
    });
});

// --- Accordion FAQ Logic ---
const accordionHeaders = document.querySelectorAll('.accordion-header');
accordionHeaders.forEach(header => {
    header.addEventListener('click', () => {
        const item = header.parentElement;
        const isActive = item.classList.contains('active');
        
        // Close all items
        document.querySelectorAll('.accordion-item').forEach(accItem => {
            accItem.classList.remove('active');
        });
        
        // If it wasn't active, open it
        if (!isActive) {
            item.classList.add('active');
        }
    });
});

// --- Contact Form Handling ---
const contactForm = document.getElementById('contact-form');
const formStatusMsg = document.getElementById('form-status-msg');

if (contactForm) {
    contactForm.addEventListener('submit', (e) => {
        e.preventDefault();
        
        // Simulate sending process
        const submitBtn = document.getElementById('btn-form-submit');
        submitBtn.disabled = true;
        submitBtn.textContent = 'Sending...';
        
        setTimeout(() => {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Send Message';
            contactForm.reset();
            
            formStatusMsg.classList.add('success');
            formStatusMsg.textContent = 'Thank you! Your message has been sent successfully.';
            
            setTimeout(() => {
                formStatusMsg.classList.remove('success');
                formStatusMsg.textContent = '';
            }, 5000);
        }, 1200);
    });
}

// --- Live Selection Playground (Simulator) ---

// Simulated Upgrade State
const state = {
    level: 1,
    experience: 0,
    rolls: 3,
    stats: {
        max_health: 0,
        attack_damage: 0,
        attack_speed: 0,
        movement_speed: 0,
        bonus_xp: 0,
        crit_chance: 0
    }
};

// Available Stats configuration
const statsConfig = {
    max_health: { name: 'Max Health', icon: '❤️', unit: 'HP', step: 1.0 },
    attack_damage: { name: 'Attack Damage', icon: '🗡️', unit: 'AD', step: 0.25 },
    attack_speed: { name: 'Attack Speed', icon: '⚡', unit: '%', step: 0.10 },
    movement_speed: { name: 'Movement Speed', icon: '👟', unit: '%', step: 0.02 },
    bonus_xp: { name: 'Bonus XP', icon: '🧪', unit: '%', step: 0.05 },
    crit_chance: { name: 'Crit Chance', icon: '💥', unit: '%', step: 0.01 }
};

// Rarity configurations
const rarities = [
    { name: 'common', weight: 50.0, display: 'Common', color: 'hsl(0, 0%, 65%)' },
    { name: 'uncommon', weight: 25.0, display: 'Uncommon', color: 'hsl(120, 50%, 45%)' },
    { name: 'rare', weight: 13.0, display: 'Rare', color: 'hsl(210, 85%, 55%)' },
    { name: 'epic', weight: 7.0, display: 'Epic', color: 'hsl(290, 70%, 55%)' },
    { name: 'legendary', weight: 3.5, display: 'Legendary', color: 'hsl(35, 95%, 55%)' },
    { name: 'mythic', weight: 1.0, display: 'Mythic', color: 'hsl(360, 80%, 55%)' }
];

// Calculate XP required using the exact formula:
// level 1: 50 XP, increments by 10 each level, capped at 500 XP.
function getXpNeeded(level) {
    const needed = 50.0 + (level - 1) * 10.0;
    return Math.min(500.0, needed);
}

// Select random rarity based on weights
function getRandomRarity() {
    const totalWeight = rarities.reduce((sum, r) => sum + r.weight, 0);
    let rand = Math.random() * totalWeight;
    for (let r of rarities) {
        if (rand <= r.weight) return r;
        rand -= r.weight;
    }
    return rarities[0];
}

// Generate a random card definition
function generateRandomCard(excludeKeys = []) {
    const availableKeys = Object.keys(statsConfig).filter(k => !excludeKeys.includes(k));
    const statKey = availableKeys[Math.floor(Math.random() * availableKeys.length)];
    const rarity = getRandomRarity();
    const config = statsConfig[statKey];
    
    // Value computation
    let levelIndex = rarities.findIndex(r => r.name === rarity.name);
    let rIdx = levelIndex;
    let val = 0;
    
    if (statKey === 'bonus_xp') {
        if (rIdx === 4) val = 1.00;
        else if (rIdx === 5) val = 2.00;
        else val = 0.25 + 0.05 * rIdx;
    } else {
        val = config.step * (rIdx + 1);
    }

    // Tooltip / Description formatting
    let valDisplay = '';
    if (statKey === 'bonus_xp' || statKey === 'movement_speed' || statKey === 'crit_chance') {
        valDisplay = `+${Math.round(val * 100)}%`;
    } else if (statKey === 'attack_speed') {
        valDisplay = `+${Math.round(val * 100)}%`;
    } else {
        valDisplay = `+${val.toFixed(2).replace(/\.00$/, '').replace(/(\.\d)0$/, '$1')}`;
    }

    return {
        id: `${statKey}_${rarity.name}`,
        statKey: statKey,
        name: config.name,
        icon: config.icon,
        rarity: rarity.name,
        rarityDisplay: rarity.display,
        value: val,
        valDisplay: valDisplay
    };
}

// Draw 3 unique cards
function drawThreeCards() {
    const cards = [];
    const chosenStats = [];
    for (let i = 0; i < 3; i++) {
        const card = generateRandomCard(chosenStats);
        cards.push(card);
        chosenStats.push(card.statKey);
    }
    return cards;
}

// DOM Elements for simulator
const domLevel = document.getElementById('sim-level');
const domRolls = document.getElementById('sim-rolls');
const domXpFill = document.getElementById('sim-xp-fill');
const domXpText = document.getElementById('sim-xp-text');
const domStatsDisplay = document.getElementById('sim-stats-display');
const btnDraw = document.getElementById('btn-sim-draw');

const domModal = document.getElementById('sim-modal');
const domModalRolls = document.getElementById('sim-modal-rolls-count');
const domCardsRow = document.getElementById('sim-cards-row');
const btnReroll = document.getElementById('btn-sim-reroll');

let currentDrawnCards = [];

// Update Simulated HUD view
function updateHUD() {
    domLevel.textContent = state.level;
    domRolls.textContent = state.rolls;
    
    const needed = getXpNeeded(state.level);
    const progressPct = (state.experience / needed) * 100;
    domXpFill.style.width = `${progressPct}%`;
    domXpText.textContent = `${Math.round(state.experience)} / ${needed} XP`;

    // Render Stats list values
    Object.keys(state.stats).forEach(key => {
        const config = statsConfig[key];
        const val = state.stats[key];
        const entry = domStatsDisplay.querySelector(`[data-stat="${key}"]`);
        if (entry) {
            let valStr = '';
            if (key === 'bonus_xp' || key === 'movement_speed' || key === 'crit_chance' || key === 'attack_speed') {
                valStr = `+${Math.round(val * 100)}%`;
            } else {
                valStr = `+${val.toFixed(2).replace(/\.00$/, '').replace(/(\.\d)0$/, '$1')}`;
            }
            entry.innerHTML = `${config.icon} ${config.name}: <span class="text-green">${valStr}</span>`;
        }
    });
}

// Open modal drawer
function triggerCardSelection() {
    currentDrawnCards = drawThreeCards();
    domModalRolls.textContent = state.rolls;
    
    // Build card buttons
    domCardsRow.innerHTML = '';
    currentDrawnCards.forEach((card, idx) => {
        const cardEl = document.createElement('div');
        cardEl.className = `sim-card rarity-${card.rarity}`;
        cardEl.innerHTML = `
            <div>
                <div class="card-rarity-banner">${card.rarityDisplay}</div>
                <div class="card-icon">${card.icon}</div>
                <div class="card-title">${card.name}</div>
            </div>
            <div class="card-desc">Increases your total ${card.name} by ${card.valDisplay}.</div>
        `;
        
        cardEl.addEventListener('click', () => {
            // Select Card logic
            state.stats[card.statKey] += card.value;
            domModal.classList.remove('active');
            updateHUD();
        });
        
        domCardsRow.appendChild(cardEl);
    });

    // Disable reroll button if rolls = 0
    if (state.rolls <= 0) {
        btnReroll.disabled = true;
    } else {
        btnReroll.disabled = false;
    }

    domModal.classList.add('active');
}

// Handle Gain XP button click
btnDraw.addEventListener('click', () => {
    // 15 base XP * (1 + bonus_xp multiplier)
    const multiplier = 1 + state.stats.bonus_xp;
    const gained = 15 * multiplier;
    
    state.experience += gained;
    const needed = getXpNeeded(state.level);
    
    if (state.experience >= needed) {
        state.experience -= needed;
        state.level++;
        state.rolls = 3; // reset roll count on level up
        triggerCardSelection();
    }
    
    updateHUD();
});

// Handle Reroll button click
btnReroll.addEventListener('click', () => {
    if (state.rolls > 0) {
        state.rolls--;
        domModalRolls.textContent = state.rolls;
        triggerCardSelection();
        updateHUD();
    }
});

// Initialize HUD display
updateHUD();
