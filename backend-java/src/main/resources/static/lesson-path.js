// Lesson Path Interactive JavaScript
(function() {
    'use strict';

    // Initialize when DOM is ready
    document.addEventListener('DOMContentLoaded', function() {
        initLessonPath();
        initProgressBars();
        initTooltips();
        initPathNavigation();
    });

    /**
     * Initialize lesson path interactions
     */
    function initLessonPath() {
        const lessonCircles = document.querySelectorAll('.lesson-circle');
        
        lessonCircles.forEach(circle => {
            // Hover effects
            circle.addEventListener('mouseenter', function() {
                if (!this.classList.contains('locked')) {
                    this.style.transform = 'scale(1.15)';
                    this.style.transition = 'transform 0.2s ease';
                }
            });

            circle.addEventListener('mouseleave', function() {
                this.style.transform = 'scale(1)';
            });

            // Click handler for locked lessons
            circle.addEventListener('click', function(e) {
                if (this.classList.contains('locked')) {
                    e.preventDefault();
                    showLockedMessage();
                } else if (this.classList.contains('completed')) {
                    // Show completion animation
                    showCompletionAnimation(this);
                }
            });

            // Add pulse animation for active lesson
            if (circle.classList.contains('active') && !circle.classList.contains('completed')) {
                addPulseAnimation(circle);
            }
        });
    }

    /**
     * Show locked lesson message
     */
    function showLockedMessage() {
        const message = document.createElement('div');
        message.className = 'lock-message';
        message.innerHTML = `
            <div style="
                position: fixed;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                background: white;
                padding: 24px 32px;
                border-radius: 16px;
                box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                z-index: 1000;
                text-align: center;
                max-width: 400px;
            ">
                <div style="font-size: 48px; margin-bottom: 16px;">🔒</div>
                <h3 style="margin: 0 0 12px 0; color: #1F2937;">Bài học chưa được mở khóa</h3>
                <p style="margin: 0 0 20px 0; color: #6B7280;">
                    Hãy hoàn thành bài học trước đó để mở khóa bài học này!
                </p>
                <button onclick="this.parentElement.parentElement.remove()" 
                        class="btn-main" 
                        style="width: 100%;">
                    Đã hiểu
                </button>
            </div>
        `;
        document.body.appendChild(message);

        // Auto remove after 5 seconds
        setTimeout(() => {
            if (message.parentElement) {
                message.remove();
            }
        }, 5000);
    }

    /**
     * Show completion animation
     */
    function showCompletionAnimation(element) {
        const originalContent = element.innerHTML;
        element.innerHTML = '✓';
        element.style.transform = 'scale(1.3)';
        element.style.background = '#10B981';
        
        setTimeout(() => {
            element.style.transform = 'scale(1)';
            element.innerHTML = originalContent;
        }, 500);
    }

    /**
     * Add pulse animation to active lesson
     */
    function addPulseAnimation(element) {
        element.style.animation = 'pulse 2s ease-in-out infinite';
        
        // Add CSS animation if not exists
        if (!document.getElementById('pulse-animation-style')) {
            const style = document.createElement('style');
            style.id = 'pulse-animation-style';
            style.textContent = `
                @keyframes pulse {
                    0%, 100% {
                        box-shadow: 0 0 0 0 rgba(88, 204, 2, 0.4);
                    }
                    50% {
                        box-shadow: 0 0 0 8px rgba(88, 204, 2, 0);
                    }
                }
            `;
            document.head.appendChild(style);
        }
    }

    /**
     * Initialize progress bars animation
     */
    function initProgressBars() {
        const progressBars = document.querySelectorAll('.quest-progress-fill');
        
        progressBars.forEach(bar => {
            const width = bar.style.width || getComputedStyle(bar).width;
            const targetWidth = width.replace('%', '');
            
            // Reset to 0
            bar.style.width = '0%';
            bar.style.transition = 'width 0.8s ease-out';
            
            // Animate to target width
            setTimeout(() => {
                bar.style.width = width;
            }, 100);
        });
    }

    /**
     * Initialize tooltips for lesson nodes
     */
    function initTooltips() {
        const lessonNodes = document.querySelectorAll('.lesson-node');
        
        lessonNodes.forEach(node => {
            const circle = node.querySelector('.lesson-circle');
            const label = node.querySelector('.lesson-label');
            
            if (circle && label) {
                circle.addEventListener('mouseenter', function() {
                    showTooltip(this, label.textContent);
                });
                
                circle.addEventListener('mouseleave', function() {
                    hideTooltip();
                });
            }
        });
    }

    /**
     * Show tooltip
     */
    function showTooltip(element, text) {
        const tooltip = document.createElement('div');
        tooltip.className = 'lesson-tooltip';
        tooltip.textContent = text;
        tooltip.style.cssText = `
            position: absolute;
            background: #1F2937;
            color: white;
            padding: 8px 12px;
            border-radius: 8px;
            font-size: 14px;
            pointer-events: none;
            z-index: 1000;
            white-space: nowrap;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        `;
        
        document.body.appendChild(tooltip);
        
        const rect = element.getBoundingClientRect();
        tooltip.style.left = (rect.left + rect.width / 2 - tooltip.offsetWidth / 2) + 'px';
        tooltip.style.top = (rect.top - tooltip.offsetHeight - 8) + 'px';
    }

    /**
     * Hide tooltip
     */
    function hideTooltip() {
        const tooltip = document.querySelector('.lesson-tooltip');
        if (tooltip) {
            tooltip.remove();
        }
    }

    /**
     * Initialize path navigation (arrow buttons)
     */
    function initPathNavigation() {
        const pathArrow = document.querySelector('.path-arrow');
        if (pathArrow) {
            pathArrow.addEventListener('click', function() {
                // Scroll to previous section or navigate
                const lessonPath = document.querySelector('.lesson-path');
                if (lessonPath) {
                    lessonPath.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            });
        }

        const guideBtn = document.querySelector('.path-guide-btn');
        if (guideBtn) {
            guideBtn.addEventListener('click', function() {
                showGuideModal();
            });
        }
    }

    /**
     * Show guide modal
     */
    function showGuideModal() {
        const modal = document.createElement('div');
        modal.className = 'guide-modal';
        modal.innerHTML = `
            <div style="
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0,0,0,0.5);
                display: flex;
                align-items: center;
                justify-content: center;
                z-index: 2000;
            ">
                <div style="
                    background: white;
                    padding: 32px;
                    border-radius: 20px;
                    max-width: 500px;
                    max-height: 80vh;
                    overflow-y: auto;
                ">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                        <h2 style="margin: 0; color: #1F2937;">Hướng dẫn</h2>
                        <button onclick="this.closest('.guide-modal').remove()" 
                                style="
                                    background: none;
                                    border: none;
                                    font-size: 24px;
                                    cursor: pointer;
                                    color: #6B7280;
                                ">✕</button>
                    </div>
                    <div style="color: #4B5563; line-height: 1.6;">
                        <p><strong>⭐ Bài học đang hoạt động:</strong> Bài học bạn có thể bắt đầu ngay.</p>
                        <p><strong>✓ Bài học đã hoàn thành:</strong> Bài học bạn đã hoàn thành.</p>
                        <p><strong>🔒 Bài học bị khóa:</strong> Hoàn thành bài học trước đó để mở khóa.</p>
                        <p><strong>🎁 Hộp quà:</strong> Nhận phần thưởng đặc biệt khi hoàn thành nhiều bài học.</p>
                        <p style="margin-top: 20px;">
                            <strong>Mẹo:</strong> Hoàn thành 10 bài học để mở khóa Bảng xếp hạng và thi đua với bạn bè!
                        </p>
                    </div>
                    <button onclick="this.closest('.guide-modal').remove()" 
                            class="btn-main" 
                            style="width: 100%; margin-top: 20px;">
                        Đã hiểu
                    </button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);

        // Close on background click
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                modal.remove();
            }
        });
    }

    /**
     * Smooth scroll to lesson
     */
    function scrollToLesson(lessonId) {
        const lessonNode = document.querySelector(`[data-lesson-id="${lessonId}"]`);
        if (lessonNode) {
            lessonNode.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }

    // Export functions for global use
    window.LessonPath = {
        scrollToLesson: scrollToLesson
    };

})();

