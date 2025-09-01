// utils/gameValidation.ts
import fetch from "node-fetch";
import {ApiError} from "../bootstrap/errors";

/**
 * Check if a cell is empty before placing character
 */
export function validateEmptyCell(liveGame: any, row: number, col: number) {
  if (liveGame.cellData[row][col] != null && liveGame.cellData[row][col] !== "") {
    throw new ApiError("Cell is already filled", 400);
  }
}

/**
 * Check if selected cells form a straight line (horizontal, vertical, or diagonal)
 */
export function validateLinearSelection(cells: { row: number; col: number }[]) {
  if (cells.length < 2) return; // Single cell is always valid

  const allRows = cells.map(c => c.row);
  const allCols = cells.map(c => c.col);

  const isHorizontal = allRows.every(r => r === allRows[0]) &&
    isConsecutive(allCols);

  const isVertical = allCols.every(c => c === allCols[0]) &&
    isConsecutive(allRows);

  const isDiagonal = isDiagonalLine(cells);

  if (!(isHorizontal || isVertical || isDiagonal)) {
    throw new ApiError("Cells must be in a straight line (horizontal, vertical, or diagonal)", 400);
  }
}

/**
 * Ensure values are consecutive (either strictly increasing or decreasing by 1)
 */
function isConsecutive(values: number[]) {
  const sorted = [...values].sort((a, b) => a - b);
  for (let i = 1; i < sorted.length; i++) {
    if (sorted[i] - sorted[i - 1] !== 1) return false;
  }
  return true;
}

/**
 * Check diagonal alignment
 */
function isDiagonalLine(cells: { row: number; col: number }[]) {
  // sort cells by row for consistency
  const sorted = [...cells].sort((a, b) => a.row - b.row);

  for (let i = 1; i < sorted.length; i++) {
    const prev = sorted[i - 1];
    const curr = sorted[i];
    if (Math.abs(curr.row - prev.row) !== 1 || Math.abs(curr.col - prev.col) !== 1) {
      return false;
    }
  }
  return true;
}

/**
 * Check all cells are filled with characters
 */
export function validateCellsFilled(liveGame: any, cells: { row: number; col: number }[]) {
  for (const { row, col } of cells) {
    if (!liveGame.cellData[row][col] || liveGame.cellData[row][col] === "") {
      throw new ApiError("Selected cells must all be filled", 400);
    }
  }
}

/**
 * Build word from selected cells
 */
export function buildWordFromCells(liveGame: any, cells: { row: number; col: number }[]) {
  return cells.map(({ row, col }) => liveGame.cellData[row][col]).join("");
}

/**
 * Check dictionary API for validity
 */
export async function validateWord(word: string) {
  const url = `https://api.dictionaryapi.dev/api/v2/entries/en/${word}`;
  const res = await fetch(url);

  if (!res.ok) {
    throw new ApiError(`Invalid word: ${word}`, 400);
  }

  const data = await res.json();
  if (!Array.isArray(data) || data.length === 0) {
    throw new ApiError(`Invalid word: ${word}`, 400);
  }
  return true;
}

/**
 * Check if word already claimed
 * Now includes singular/plural and verb form checking
 * Examples:
 * - If "play" is claimed, "plays", "playing", "played" will be blocked
 * - If "plays" is claimed, "play", "playing", "played" will be blocked
 * - If "playing" is claimed, "play", "plays", "played" will be blocked
 */
export function validateWordNotClaimed(word: string, dbGame: any) {
  const claimed = dbGame.claimedWords || {};
  const allWords = Object.values(claimed).flat().map((w: any) => w.toLowerCase());
  
  // Generate word variations to check for singular/plural conflicts
  const wordVariations = generateWordVariations(word);
  
  // Check if any variation of the word is already claimed
  for (const variation of wordVariations) {
    if (allWords.includes(variation)) {
      throw new ApiError(`Word variation '${variation}' already claimed by another player`, 400);
    }
  }
}

/**
 * Generate singular/plural variations of a word
 * This handles common English word patterns including:
 * - Singular/plural forms (cat/cats, play/plays, baby/babies)
 * - Verb conjugations (play/plays/playing/played)
 * - Special cases (words ending in s, sh, ch, x, z)
 * - Irregular forms (child/children, go/goes/went)
 * 
 * Test cases:
 * - "play" -> ["play", "plays", "playing", "played"]
 * - "plays" -> ["plays", "play"]
 * - "playing" -> ["playing", "play"]
 * - "cat" -> ["cat", "cats"]
 * - "cats" -> ["cats", "cat"]
 * - "baby" -> ["baby", "babies"]
 * - "babies" -> ["babies", "baby"]
 * - "watch" -> ["watch", "watches", "watching", "watched"]
 * - "child" -> ["child", "children"]
 * - "go" -> ["go", "goes", "going", "went", "gone"]
 */
function generateWordVariations(word: string): string[] {
  const variations = new Set<string>([word.toLowerCase()]);
  const wordLower = word.toLowerCase();
  
  // Handle irregular plurals
  const irregularPlurals: { [key: string]: string } = {
    'child': 'children',
    'foot': 'feet',
    'tooth': 'teeth',
    'man': 'men',
    'woman': 'women',
    'person': 'people',
    'goose': 'geese',
    'mouse': 'mice',
    'louse': 'lice',
    'ox': 'oxen',
    'fish': 'fish', // Same in singular and plural
    'sheep': 'sheep', // Same in singular and plural
    'deer': 'deer' // Same in singular and plural
  };
  
  // Handle irregular plurals (singular -> plural)
  if (irregularPlurals[wordLower]) {
    variations.add(irregularPlurals[wordLower]);
  }
  
  // Handle irregular plurals (plural -> singular)
  for (const [singular, plural] of Object.entries(irregularPlurals)) {
    if (wordLower === plural) {
      variations.add(singular);
    }
  }
  
  // Handle irregular verbs
  const irregularVerbs: { [key: string]: string[] } = {
    'go': ['goes', 'going', 'went', 'gone'],
    'be': ['am', 'is', 'are', 'being', 'was', 'were', 'been'],
    'have': ['has', 'having', 'had'],
    'do': ['does', 'doing', 'did', 'done'],
    'see': ['sees', 'seeing', 'saw', 'seen'],
    'take': ['takes', 'taking', 'took', 'taken'],
    'make': ['makes', 'making', 'made'],
    'come': ['comes', 'coming', 'came'],
    'get': ['gets', 'getting', 'got', 'gotten'],
    'give': ['gives', 'giving', 'gave', 'given'],
    'know': ['knows', 'knowing', 'knew', 'known'],
    'think': ['thinks', 'thinking', 'thought'],
    'say': ['says', 'saying', 'said'],
    'tell': ['tells', 'telling', 'told'],
    'find': ['finds', 'finding', 'found'],
    'feel': ['feels', 'feeling', 'felt'],
    'put': ['puts', 'putting', 'put'],
    'keep': ['keeps', 'keeping', 'kept'],
    'let': ['lets', 'letting', 'let'],
    'begin': ['begins', 'beginning', 'began', 'begun'],
    'break': ['breaks', 'breaking', 'broke', 'broken'],
    'bring': ['brings', 'bringing', 'brought'],
    'build': ['builds', 'building', 'built'],
    'buy': ['buys', 'buying', 'bought'],
    'catch': ['catches', 'catching', 'caught'],
    'choose': ['chooses', 'choosing', 'chose', 'chosen'],
    'cut': ['cuts', 'cutting', 'cut'],
    'draw': ['draws', 'drawing', 'drew', 'drawn'],
    'drive': ['drives', 'driving', 'drove', 'driven'],
    'eat': ['eats', 'eating', 'ate', 'eaten'],
    'fall': ['falls', 'falling', 'fell', 'fallen'],
    'fight': ['fights', 'fighting', 'fought'],
    'fly': ['flies', 'flying', 'flew', 'flown'],
    'forget': ['forgets', 'forgetting', 'forgot', 'forgotten'],
    'freeze': ['freezes', 'freezing', 'froze', 'frozen'],
    'grow': ['grows', 'growing', 'grew', 'grown'],
    'hide': ['hides', 'hiding', 'hid', 'hidden'],
    'hold': ['holds', 'holding', 'held'],
    'hurt': ['hurts', 'hurting', 'hurt'],
    'lay': ['lays', 'laying', 'laid'],
    'lead': ['leads', 'leading', 'led'],
    'lend': ['lends', 'lending', 'lent'],
    'lie': ['lies', 'lying', 'lay', 'lain'],
    'lose': ['loses', 'losing', 'lost'],
    'mean': ['means', 'meaning', 'meant'],
    'meet': ['meets', 'meeting', 'met'],
    'pay': ['pays', 'paying', 'paid'],
    'read': ['reads', 'reading', 'read'],
    'ride': ['rides', 'riding', 'rode', 'ridden'],
    'ring': ['rings', 'ringing', 'rang', 'rung'],
    'rise': ['rises', 'rising', 'rose', 'risen'],
    'run': ['runs', 'running', 'ran'],
    'sell': ['sells', 'selling', 'sold'],
    'send': ['sends', 'sending', 'sent'],
    'set': ['sets', 'setting', 'set'],
    'shake': ['shakes', 'shaking', 'shook', 'shaken'],
    'shine': ['shines', 'shining', 'shone'],
    'shoot': ['shoots', 'shooting', 'shot'],
    'show': ['shows', 'showing', 'showed', 'shown'],
    'shut': ['shuts', 'shutting', 'shut'],
    'sing': ['sings', 'singing', 'sang', 'sung'],
    'sit': ['sits', 'sitting', 'sat'],
    'sleep': ['sleeps', 'sleeping', 'slept'],
    'speak': ['speaks', 'speaking', 'spoke', 'spoken'],
    'spend': ['spends', 'spending', 'spent'],
    'stand': ['stands', 'standing', 'stood'],
    'steal': ['steals', 'stealing', 'stole', 'stolen'],
    'stick': ['sticks', 'sticking', 'stuck'],
    'strike': ['strikes', 'striking', 'struck'],
    'swim': ['swims', 'swimming', 'swam', 'swum'],
    'swing': ['swings', 'swinging', 'swung'],
    'teach': ['teaches', 'teaching', 'taught'],
    'tear': ['tears', 'tearing', 'tore', 'torn'],
    'throw': ['throws', 'throwing', 'threw', 'thrown'],
    'understand': ['understands', 'understanding', 'understood'],
    'wake': ['wakes', 'waking', 'woke', 'woken'],
    'wear': ['wears', 'wearing', 'wore', 'worn'],
    'win': ['wins', 'winning', 'won'],
    'write': ['writes', 'writing', 'wrote', 'written']
  };
  
  // Handle irregular verbs
  if (irregularVerbs[wordLower]) {
    irregularVerbs[wordLower].forEach(form => variations.add(form));
  }
  
  // Handle irregular verbs (reverse lookup)
  for (const [base, forms] of Object.entries(irregularVerbs)) {
    if (forms.includes(wordLower)) {
      variations.add(base);
      forms.forEach(form => variations.add(form));
    }
  }
  
  // Handle regular patterns (only if not already handled by irregulars)
  if (!irregularPlurals[wordLower] && !irregularVerbs[wordLower]) {
    // Handle plural forms
    if (wordLower.endsWith('y') && !isVowel(wordLower[wordLower.length - 2])) {
      // Words ending in 'y' -> 'ies' (e.g., play -> plays, but baby -> babies)
      variations.add(wordLower.slice(0, -1) + 'ies');
    } else if (wordLower.endsWith('s') || wordLower.endsWith('sh') || wordLower.endsWith('ch') || 
               wordLower.endsWith('x') || wordLower.endsWith('z')) {
      // Words ending in 's', 'sh', 'ch', 'x', 'z' -> 'es'
      variations.add(wordLower + 'es');
    } else {
      // Regular plural: add 's'
      variations.add(wordLower + 's');
    }
    
    // Handle singular forms (if the word looks like a plural)
    if (wordLower.endsWith('ies')) {
      // 'ies' -> 'y'
      variations.add(wordLower.slice(0, -3) + 'y');
    } else if (wordLower.endsWith('es')) {
      // 'es' -> '' (for words ending in s, sh, ch, x, z)
      variations.add(wordLower.slice(0, -2));
    } else if (wordLower.endsWith('s')) {
      // 's' -> '' (regular singular)
      variations.add(wordLower.slice(0, -1));
    }
    
    // Handle verb forms (only for regular verbs)
    if (!wordLower.endsWith('ing') && !wordLower.endsWith('ed') && !wordLower.endsWith('s')) {
      // Add present participle
      variations.add(wordLower + 'ing');
      // Add past tense
      variations.add(wordLower + 'ed');
      // Add third person singular
      variations.add(wordLower + 's');
    }
    
    // Handle base forms from verb forms
    if (wordLower.endsWith('ing')) {
      // 'ing' -> ''
      variations.add(wordLower.slice(0, -3));
    }
    if (wordLower.endsWith('ed')) {
      // 'ed' -> ''
      variations.add(wordLower.slice(0, -2));
    }
  }
  
  return Array.from(variations);
}

/**
 * Check if a character is a vowel
 */
function isVowel(char: string): boolean {
  return ['a', 'e', 'i', 'o', 'u'].includes(char.toLowerCase());
}
