// Word Puzzle Game JavaScript

// Game state
let gameState = {
    currentQuestion: 0,
    totalQuestions: 10,
    score: 0,
    correctCount: 0,
    gameData: [],
    timer: null,
    timeLeft: 30,
    isPlaying: false,
    currentWord: null,
    currentLetters: [],
    usedLetters: [],
    wordSlots: [],
    questionStartTime: null
};

// Initialize game
document.addEventListener('DOMContentLoaded', function() {
    if (typeof vocabItems === 'undefined' || vocabItems.length < 4) {
        alert('Không đủ từ vựng để chơi!');
        return;
    }
    
    // Generate game data
    generateGameData();
    
    // Start first question
    loadQuestion();
});

// Generate game data
function generateGameData() {
    gameState.gameData = [];
    for (let i = 0; i < gameState.totalQuestions; i++) {
        // Randomly select a word from vocabItems
        const wordData = vocabItems[Math.floor(Math.random() * vocabItems.length)];
        const word = wordData.word.toUpperCase();
        
        // Shuffle letters
        const letters = word.split('').sort(() => Math.random() - 0.5);
        
        gameState.gameData.push({
            word: word,
            wordLower: wordData.word.toLowerCase(),
            emoji: wordData.emoji,
            letters: letters
        });
    }
}

// Load question
function loadQuestion() {
    if (gameState.currentQuestion >= gameState.gameData.length) {
        endGame();
        return;
    }
    
    const question = gameState.gameData[gameState.currentQuestion];
    gameState.currentWord = question.word;
    gameState.currentLetters = [...question.letters];
    gameState.usedLetters = [];
    gameState.wordSlots = [];
    gameState.questionStartTime = Date.now();
    gameState.timeLeft = 30;
    
    // Update UI
    document.getElementById('questionNumber').textContent = gameState.currentQuestion + 1;
    document.getElementById('totalQuestions').textContent = gameState.totalQuestions;
    document.getElementById('wordImage').textContent = question.emoji;
    document.getElementById('currentScore').textContent = gameState.score;
    document.getElementById('gameFeedback').textContent = '';
    document.getElementById('gameFeedback').className = 'game-feedback';
    
    // Generate word slots
    const slotsContainer = document.getElementById('wordSlots');
    slotsContainer.innerHTML = '';
    gameState.wordSlots = [];
    
    for (let i = 0; i < question.word.length; i++) {
        const slot = document.createElement('div');
        slot.className = 'word-slot';
        slot.dataset.index = i;
        slot.textContent = '';
        slotsContainer.appendChild(slot);
        gameState.wordSlots.push(slot);
    }
    
    // Generate letter tiles
    const lettersContainer = document.getElementById('lettersContainer');
    lettersContainer.innerHTML = '';
    
    question.letters.forEach((letter, index) => {
        const tile = document.createElement('div');
        tile.className = 'letter-tile';
        tile.textContent = letter;
        tile.dataset.letter = letter;
        tile.dataset.index = index;
        tile.onclick = () => selectLetter(tile);
        lettersContainer.appendChild(tile);
    });
    
    // Start timer
    startTimer();
    gameState.isPlaying = true;
}

// Timer
function startTimer() {
    clearInterval(gameState.timer);
    
    const timerElement = document.getElementById('timer');
    timerElement.textContent = gameState.timeLeft;
    timerElement.style.color = '#1F2937';
    timerElement.style.fontWeight = 'normal';
    
    gameState.timer = setInterval(() => {
        gameState.timeLeft--;
        timerElement.textContent = gameState.timeLeft;
        
        if (gameState.timeLeft <= 0) {
            clearInterval(gameState.timer);
            // Time's up
            handleTimeUp();
        } else if (gameState.timeLeft <= 5) {
            timerElement.style.color = '#EF4444';
            timerElement.style.fontWeight = 'bold';
        }
    }, 1000);
}

// Select letter
function selectLetter(tile) {
    if (!gameState.isPlaying || tile.classList.contains('used')) {
        return;
    }
    
    // Find first empty slot
    const emptySlot = gameState.wordSlots.find(slot => !slot.textContent);
    if (!emptySlot) {
        return; // All slots filled
    }
    
    // Move letter to slot
    emptySlot.textContent = tile.textContent;
    emptySlot.dataset.letter = tile.dataset.letter;
    emptySlot.classList.add('filled');
    
    // Mark tile as used
    tile.classList.add('used');
    gameState.usedLetters.push({
        tile: tile,
        slot: emptySlot
    });
}

// Reset word
function resetWord() {
    if (!gameState.isPlaying) return;
    
    // Clear all slots
    gameState.wordSlots.forEach(slot => {
        slot.textContent = '';
        slot.classList.remove('filled', 'correct', 'wrong');
        delete slot.dataset.letter;
    });
    
    // Restore all tiles
    gameState.usedLetters.forEach(({tile}) => {
        tile.classList.remove('used');
    });
    
    gameState.usedLetters = [];
    
    // Clear feedback
    document.getElementById('gameFeedback').textContent = '';
    document.getElementById('gameFeedback').className = 'game-feedback';
}

// Check answer
function checkAnswer() {
    if (!gameState.isPlaying) return;
    
    // Get current word from slots
    const currentWord = gameState.wordSlots
        .map(slot => slot.textContent || '')
        .join('');
    
    if (currentWord.length !== gameState.currentWord.length) {
        document.getElementById('gameFeedback').textContent = '⚠️ Vui lòng điền đầy đủ các chữ cái!';
        document.getElementById('gameFeedback').className = 'game-feedback info';
        return;
    }
    
    clearInterval(gameState.timer);
    gameState.isPlaying = false;
    
    const isCorrect = currentWord === gameState.currentWord;
    const timeTaken = (Date.now() - gameState.questionStartTime) / 1000;
    
    // Calculate points based on time
    let points = 0;
    if (isCorrect) {
        if (timeTaken <= 5) points = 50;
        else if (timeTaken <= 10) points = 40;
        else if (timeTaken <= 15) points = 30;
        else if (timeTaken <= 20) points = 20;
        else if (timeTaken <= 25) points = 15;
        else points = 10;
        
        gameState.correctCount++;
        
        // Mark slots as correct
        gameState.wordSlots.forEach(slot => {
            slot.classList.add('correct');
            slot.classList.remove('filled');
        });
        
        document.getElementById('gameFeedback').textContent = `✅ Đúng! +${points} điểm`;
        document.getElementById('gameFeedback').className = 'game-feedback correct';
    } else {
        // Mark wrong slots
        for (let i = 0; i < gameState.wordSlots.length; i++) {
            const slot = gameState.wordSlots[i];
            const slotLetter = slot.textContent;
            const correctLetter = gameState.currentWord[i];
            
            if (slotLetter === correctLetter) {
                slot.classList.add('correct');
            } else {
                slot.classList.add('wrong');
            }
        }
        
        document.getElementById('gameFeedback').textContent = '❌ Sai! Thử lại nhé!';
        document.getElementById('gameFeedback').className = 'game-feedback wrong';
    }
    
    gameState.score += points;
    
    // Move to next question after 2 seconds
    setTimeout(() => {
        gameState.currentQuestion++;
        loadQuestion();
    }, 2000);
}

// Handle time up
function handleTimeUp() {
    if (!gameState.isPlaying) return;
    
    gameState.isPlaying = false;
    
    // Show correct answer
    for (let i = 0; i < gameState.wordSlots.length; i++) {
        const slot = gameState.wordSlots[i];
        if (!slot.textContent) {
            slot.textContent = gameState.currentWord[i];
            slot.classList.add('correct');
        }
    }
    
    document.getElementById('gameFeedback').textContent = '⏱️ Hết thời gian!';
    document.getElementById('gameFeedback').className = 'game-feedback wrong';
    
    setTimeout(() => {
        gameState.currentQuestion++;
        loadQuestion();
    }, 2000);
}

// Play current word sound
function playCurrentWordSound() {
    if (gameState.currentWord) {
        const question = gameState.gameData[gameState.currentQuestion];
        if (question) {
            playWordSound(question.wordLower);
        }
    }
}

// Play word sound
function playWordSound(word) {
    if (!word) return;
    
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
        heartsLost = Math.ceil((0.5 - ratio) * 2);
    } else if (ratio >= 0.8) {
        // High score - earn gems
        gemsEarned = Math.floor(ratio * 10);
    }
    
    // Submit results to server
    submitGameResults(gameState.score, gameState.correctCount, gameState.totalQuestions, heartsLost, gemsEarned);
    
    // Show result screen
    document.querySelector('.game-panel:not(.result-panel)').classList.add('hidden');
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
    })
    .catch(err => {
        console.error('Error submitting results:', err);
    });
}

// Restart game
function restartGame() {
    gameState.currentQuestion = 0;
    gameState.score = 0;
    gameState.correctCount = 0;
    generateGameData();
    
    document.getElementById('resultScreen').classList.add('hidden');
    document.querySelector('.game-panel:not(.result-panel)').classList.remove('hidden');
    
    loadQuestion();
}

