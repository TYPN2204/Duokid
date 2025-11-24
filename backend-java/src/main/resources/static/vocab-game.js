// Monster Vocab Game JavaScript - Image Game

// Game state
let gameState = {
    currentQuestion: 0,
    totalQuestions: 10,
    score: 0,
    correctCount: 0,
    gameData: [],
    timer: null,
    timeLeft: 10,
    isPlaying: false,
    currentWord: null,
    questionStartTime: null
};

// Initialize game
document.addEventListener('DOMContentLoaded', function() {
    if (typeof vocabItems !== 'undefined' && vocabItems.length > 0) {
        // Convert vocab items to game format if needed
        console.log('Vocab items loaded:', vocabItems);
    }
    
    // Initialize current category
    if (typeof currentCategory !== 'undefined') {
        window.currentCategory = currentCategory;
    } else {
        // Fallback: get from page
        const categoryTitle = document.querySelector('.category-title');
        if (categoryTitle) {
            window.currentCategory = categoryTitle.textContent.trim();
        } else {
            window.currentCategory = 'Personal';
        }
    }
    
    // Add event listeners for vocab items (using event delegation)
    const vocabContainer = document.querySelector('.vocab-items-container');
    if (vocabContainer) {
        vocabContainer.addEventListener('click', function(e) {
            // Check if clicked on vocab-image-frame or vocab-word-btn
            const vocabFrame = e.target.closest('.vocab-image-frame');
            const vocabBtn = e.target.closest('.vocab-word-btn');
            
            if (vocabFrame) {
                const word = vocabFrame.getAttribute('data-word');
                if (word) {
                    playWordSound(word);
                }
            } else if (vocabBtn) {
                const word = vocabBtn.getAttribute('data-word');
                if (word) {
                    playWordSound(word);
                }
            }
        });
    }
});

// Play word sound
function playWordSound(word) {
    if (!word) return;
    
    // Use TTS API
    fetch(`/tts?text=${encodeURIComponent(word)}`)
        .then(res => res.json())
        .then(data => {
            if (data.audioUrl) {
                const audio = new Audio(data.audioUrl);
                audio.play();
            }
        })
        .catch(err => {
            console.error('Error playing sound:', err);
            // Fallback: use Web Speech API
            if ('speechSynthesis' in window) {
                const utterance = new SpeechSynthesisUtterance(word);
                utterance.lang = 'en-US';
                speechSynthesis.speak(utterance);
            }
        });
}

function playCategorySound() {
    const categoryTitle = document.querySelector('.category-title');
    if (categoryTitle) {
        playWordSound(categoryTitle.textContent.trim());
    }
}

function playCurrentWordSound() {
    if (gameState.currentWord) {
        playWordSound(gameState.currentWord);
    }
}

// Navigation
function previousCategory() {
    const vocabCategories = ['Personal', 'Animals', 'Colors', 'Food', 'Family', 'Numbers'];
    const cat = window.currentCategory || 'Personal';
    const currentIndex = vocabCategories.indexOf(cat);
    const prevIndex = currentIndex > 0 ? currentIndex - 1 : vocabCategories.length - 1;
    window.location.href = `/minigame?category=${vocabCategories[prevIndex]}`;
}

function nextCategory() {
    const vocabCategories = ['Personal', 'Animals', 'Colors', 'Food', 'Family', 'Numbers'];
    const cat = window.currentCategory || 'Personal';
    const currentIndex = vocabCategories.indexOf(cat);
    const nextIndex = currentIndex < vocabCategories.length - 1 ? currentIndex + 1 : 0;
    window.location.href = `/minigame?category=${vocabCategories[nextIndex]}`;
}

// Start playing game
function startPlayingGame() {
    if (typeof vocabItems === 'undefined' || vocabItems.length < 4) {
        alert('Không đủ từ vựng để chơi!');
        return;
    }
    
    // Generate game data (10 questions)
    gameState.gameData = [];
    for (let i = 0; i < gameState.totalQuestions; i++) {
        // Randomly select a word from vocabItems
        const correctWord = vocabItems[Math.floor(Math.random() * vocabItems.length)];
        
        // Get 2 wrong options
        const wrongOptions = vocabItems.filter(item => item.word !== correctWord.word);
        const shuffled = wrongOptions.sort(() => Math.random() - 0.5);
        const wrong1 = shuffled[0];
        const wrong2 = shuffled[1];
        
        // Shuffle options
        const options = [correctWord, wrong1, wrong2].sort(() => Math.random() - 0.5);
        
        gameState.gameData.push({
            correctWord: correctWord.word,
            correctEmoji: correctWord.emoji,
            options: options
        });
    }
    
    // Reset game state
    gameState.currentQuestion = 0;
    gameState.score = 0;
    gameState.correctCount = 0;
    gameState.isPlaying = true;
    
    // Show playing screen
    document.getElementById('learningScreen').classList.add('hidden');
    document.getElementById('playingScreen').classList.remove('hidden');
    document.getElementById('resultScreen').classList.add('hidden');
    
    // Start first question
    loadQuestion();
}

// Load question
function loadQuestion() {
    if (gameState.currentQuestion >= gameState.gameData.length) {
        endGame();
        return;
    }
    
    const question = gameState.gameData[gameState.currentQuestion];
    gameState.currentWord = question.correctWord;
    gameState.questionStartTime = Date.now();
    gameState.timeLeft = 10;
    
    // Update UI
    document.getElementById('questionNumber').textContent = gameState.currentQuestion + 1;
    document.getElementById('totalQuestions').textContent = gameState.totalQuestions;
    document.getElementById('targetWord').textContent = question.correctWord;
    document.getElementById('currentScore').textContent = gameState.score;
    document.getElementById('gameFeedback').textContent = '';
    document.getElementById('gameFeedback').className = 'game-feedback';
    
    // Generate images
    const container = document.getElementById('gameImagesContainer');
    container.innerHTML = '';
    
    question.options.forEach((option, index) => {
        const card = document.createElement('div');
        card.className = 'game-image-card';
        card.dataset.word = option.word;
        card.onclick = () => selectImage(card, option.word);
        
        const emoji = document.createElement('div');
        emoji.className = 'game-emoji-large';
        emoji.textContent = option.emoji;
        
        card.appendChild(emoji);
        container.appendChild(card);
    });
    
    // Start timer
    startTimer();
}

// Timer
function startTimer() {
    clearInterval(gameState.timer);
    
    const timerElement = document.getElementById('timer');
    timerElement.textContent = gameState.timeLeft;
    
    gameState.timer = setInterval(() => {
        gameState.timeLeft--;
        timerElement.textContent = gameState.timeLeft;
        
        if (gameState.timeLeft <= 0) {
            clearInterval(gameState.timer);
            // Time's up - treat as wrong answer
            handleAnswer(null, false);
        } else if (gameState.timeLeft <= 3) {
            timerElement.style.color = '#EF4444';
            timerElement.style.fontWeight = 'bold';
        }
    }, 1000);
}

// Select image
function selectImage(card, selectedWord) {
    if (!gameState.isPlaying) return;
    
    clearInterval(gameState.timer);
    gameState.isPlaying = false;
    
    const isCorrect = selectedWord === gameState.currentWord;
    const timeTaken = (Date.now() - gameState.questionStartTime) / 1000; // seconds
    
    // Calculate points based on time (faster = more points)
    let points = 0;
    if (isCorrect) {
        if (timeTaken <= 2) points = 30;
        else if (timeTaken <= 4) points = 20;
        else if (timeTaken <= 6) points = 15;
        else if (timeTaken <= 8) points = 10;
        else points = 5;
        
        gameState.correctCount++;
    }
    
    gameState.score += points;
    
    // Show feedback
    const feedback = document.getElementById('gameFeedback');
    if (isCorrect) {
        feedback.textContent = `✅ Đúng! +${points} điểm`;
        feedback.className = 'game-feedback correct';
        card.classList.add('correct-answer');
    } else {
        feedback.textContent = '❌ Sai!';
        feedback.className = 'game-feedback wrong';
        card.classList.add('wrong-answer');
        
        // Highlight correct answer
        document.querySelectorAll('.game-image-card').forEach(c => {
            if (c.dataset.word === gameState.currentWord) {
                c.classList.add('correct-answer');
            }
        });
    }
    
    // Disable all cards
    document.querySelectorAll('.game-image-card').forEach(c => {
        c.style.pointerEvents = 'none';
    });
    
    // Move to next question after 1.5 seconds
    setTimeout(() => {
        gameState.currentQuestion++;
        gameState.isPlaying = true;
        loadQuestion();
    }, 1500);
}

function handleAnswer(selectedWord, isCorrect) {
    // This is called when time runs out
    if (selectedWord === null) {
        const feedback = document.getElementById('gameFeedback');
        feedback.textContent = '⏱️ Hết thời gian!';
        feedback.className = 'game-feedback wrong';
        
        // Highlight correct answer
        document.querySelectorAll('.game-image-card').forEach(c => {
            if (c.dataset.word === gameState.currentWord) {
                c.classList.add('correct-answer');
            }
            c.style.pointerEvents = 'none';
        });
    }
    
    setTimeout(() => {
        gameState.currentQuestion++;
        gameState.isPlaying = true;
        loadQuestion();
    }, 1500);
}

// End game
function endGame() {
    clearInterval(gameState.timer);
    gameState.isPlaying = false;
    
    // Calculate rewards
    const ratio = gameState.correctCount / gameState.totalQuestions;
    let heartsLost = 0;
    let gemsEarned = 0;
    
    if (ratio < 0.5) {
        // Low score - lose hearts
        heartsLost = Math.ceil((0.5 - ratio) * 2); // Lose 1-2 hearts
    } else if (ratio >= 0.8) {
        // High score - earn gems
        gemsEarned = Math.floor(ratio * 10); // 8-10 gems
    }
    
    // Submit results to server
    submitGameResults(gameState.score, gameState.correctCount, gameState.totalQuestions, heartsLost, gemsEarned);
    
    // Show result screen
    document.getElementById('playingScreen').classList.add('hidden');
    document.getElementById('resultScreen').classList.remove('hidden');
    
    document.getElementById('finalScore').textContent = gameState.score;
    document.getElementById('correctCount').textContent = gameState.correctCount;
    document.getElementById('totalCount').textContent = gameState.totalQuestions;
    
    // Show rewards
    const rewardsDiv = document.getElementById('resultRewards');
    rewardsDiv.innerHTML = '';
    
    if (heartsLost > 0) {
        const rewardItem = document.createElement('div');
        rewardItem.className = 'reward-item negative';
        rewardItem.innerHTML = `<span>❤️ Mất ${heartsLost} tim</span>`;
        rewardsDiv.appendChild(rewardItem);
    }
    
    if (gemsEarned > 0) {
        const rewardItem = document.createElement('div');
        rewardItem.className = 'reward-item positive';
        rewardItem.innerHTML = `<span>💎 Nhận ${gemsEarned} gem</span>`;
        rewardsDiv.appendChild(rewardItem);
    }
    
    if (heartsLost === 0 && gemsEarned === 0) {
        const rewardItem = document.createElement('div');
        rewardItem.className = 'reward-item neutral';
        rewardItem.innerHTML = `<span>🎯 Giữ nguyên</span>`;
        rewardsDiv.appendChild(rewardItem);
    }
}

// Submit game results
function submitGameResults(score, correct, total, heartsLost, gemsEarned) {
    fetch('/minigame/image-game-result', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            score: score,
            correct: correct,
            total: total,
            heartsLost: heartsLost,
            gemsEarned: gemsEarned
        })
    })
    .then(res => res.json())
    .then(data => {
        console.log('Game results submitted:', data);
        // Update user stats if needed
        if (data.hearts !== undefined) {
            // Could update UI here if needed
        }
    })
    .catch(err => {
        console.error('Error submitting results:', err);
    });
}

// Restart game
function restartGame() {
    startPlayingGame();
}

// Back to learning screen
function backToLearning() {
    document.getElementById('resultScreen').classList.add('hidden');
    document.getElementById('playingScreen').classList.add('hidden');
    document.getElementById('learningScreen').classList.remove('hidden');
}
