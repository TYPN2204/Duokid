// Grade dropdown click handler - Enhanced version
(function() {
    'use strict';
    
    function initGradeDropdown() {
        console.log('Initializing grade dropdown...');
        
        // Add click handler to trigger
        document.querySelectorAll('.grade-dropdown-trigger').forEach(function(trigger) {
            // Check if already has listener
            if (trigger.dataset.listenerAdded === 'true') {
                return; // Already initialized
            }
            trigger.dataset.listenerAdded = 'true';
            
            trigger.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                console.log('Dropdown trigger clicked');
                
                const dropdown = trigger.closest('.grade-dropdown');
                if (dropdown) {
                    // Close other dropdowns
                    document.querySelectorAll('.grade-dropdown').forEach(function(d) {
                        if (d !== dropdown) {
                            d.classList.remove('open');
                        }
                    });
                    
                    // Toggle current dropdown
                    const isOpen = dropdown.classList.contains('open');
                    if (isOpen) {
                        dropdown.classList.remove('open');
                        console.log('Dropdown closed');
                    } else {
                        dropdown.classList.add('open');
                        console.log('Dropdown opened');
                    }
                }
            });
        });
        
        // Add click handlers to menu links - ensure they work
        document.querySelectorAll('.grade-dropdown-menu a').forEach(function(link) {
            link.addEventListener('click', function(e) {
                console.log('Menu link clicked:', this.href);
                // Don't prevent default - allow navigation
                // Just close dropdown after a short delay
                setTimeout(function() {
                    document.querySelectorAll('.grade-dropdown').forEach(function(d) {
                        d.classList.remove('open');
                    });
                }, 100);
            });
        });
        
        // Close dropdown when clicking outside
        document.addEventListener('click', function(e) {
            const dropdown = e.target.closest('.grade-dropdown');
            if (!dropdown) {
                document.querySelectorAll('.grade-dropdown').forEach(function(d) {
                    d.classList.remove('open');
                });
            }
        });
        
        // Also close on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                document.querySelectorAll('.grade-dropdown').forEach(function(d) {
                    d.classList.remove('open');
                });
            }
        });
        
        console.log('Grade dropdown initialized');
    }
    
    // Initialize multiple times to ensure it works
    function init() {
        try {
            initGradeDropdown();
        } catch (error) {
            console.error('Error initializing grade dropdown:', error);
        }
    }
    
    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        // DOM already loaded
        init();
    }
    
    // Also try after a short delay in case scripts load out of order
    setTimeout(init, 100);
})();

