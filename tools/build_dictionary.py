"""Builds the autocorrect dictionaries in app/src/main/assets/dict/ from FrequencyWords lists.

Source: https://github.com/hermitdave/FrequencyWords (content/2018/<lang>/<lang>_50k.txt),
data licensed CC-BY-SA 4.0. Run once and commit the output:

    python tools/build_dictionary.py <es_50k.txt> es app/src/main/assets/dict/es.tsv
    python tools/build_dictionary.py <en_50k.txt> en app/src/main/assets/dict/en.tsv

Subtitle text contains common misspellings ("tambien", "cancion", "dont"). They must not be in
the dictionary, otherwise the keyboard would consider them correct and never fix them:
- Spanish: an unaccented word is dropped when its accented form is at least ACCENT_RATIO times
  more frequent, unless it is a real word of its own (KEEP_ES). Pairs where both forms are common
  (esta/está, como/cómo, si/sí) survive and are only offered as suggestions.
- English: apostrophes are missing from the source, so contractions are added and their
  apostrophe-less spellings ("dont", "im") are dropped unless they are real words.
"""

import sys
import unicodedata
from collections import defaultdict

ACCENT_RATIO = 5.0

# Unaccented words that are valid on their own even though the accented form is far more common.
KEEP_ES = {
    'papa', 'seria', 'sabia', 'publico', 'ingles', 'aun', 'adonde', 'lastima', 'practica',
    'fabrica', 'rio', 'ordenes', 'mate', 'continuo', 'perdida', 'opera', 'record', 'termino',
    'note', 'super', 'tomate', 'bebe', 'bebes', 'cuan', 'ejercito', 'numero', 'medico',
    'ultimo', 'ultima', 'critica', 'pagina', 'liquido', 'animo', 'transito', 'deposito',
}
# Misspellings whose accented form is not frequent enough for the ratio rule (voseo, mostly).
DROP_ES = {'tenes', 'queres', 'podes', 'oir', 'reir', 'eramos', 'lider', 'union', 'jesus', 'maria'}
# Single letters that are words.
SINGLE_LETTERS = {'es': {'a', 'e', 'o', 'u', 'y'}, 'en': {'a', 'i'}}

CONTRACTIONS_EN = [
    "don't", "doesn't", "didn't", "isn't", "aren't", "wasn't", "weren't", "haven't", "hasn't",
    "hadn't", "won't", "wouldn't", "can't", "couldn't", "shouldn't", "mustn't", "needn't",
    "I'm", "I've", "I'll", "I'd", "you're", "you've", "you'll", "you'd", "he's", "he'll",
    "he'd", "she's", "she'll", "she'd", "it's", "it'll", "we're", "we've", "we'll", "we'd",
    "they're", "they've", "they'll", "they'd", "that's", "there's", "here's", "what's",
    "who's", "where's", "how's", "let's", "y'all", "ain't", "o'clock",
]
# Apostrophe-less spellings that are real words and must stay.
REAL_EN = {'wont', 'ill', 'its', 'were', 'well', 'hell', 'shell', 'id', 'wed', 'shed', 'cant', 'hed', 'lets', 'whos', 'hes'}


def fold(word):
    """Lowercase without accents; ñ stays distinct (it is a different letter, not an accent)."""
    word = word.lower().replace('ñ', '\0')
    stripped = ''.join(c for c in unicodedata.normalize('NFD', word) if unicodedata.category(c) != 'Mn')
    return stripped.replace('\0', 'ñ')


def read(path):
    freq = {}
    for line in open(path, encoding='utf-8'):
        word, count = line.rsplit(' ', 1)
        word = word.strip().lower()
        if word.isalpha():
            freq[word] = freq.get(word, 0) + int(count)
    return freq


def clean_es(freq):
    groups = defaultdict(list)
    for word, count in freq.items():
        groups[fold(word)].append((count, word))
    for key, words in groups.items():
        if len(words) < 2 or key not in freq or key in KEEP_ES:
            continue
        best = max(count for count, word in words if word != key)
        if best >= ACCENT_RATIO * freq[key]:
            del freq[key]
    for word in DROP_ES:
        freq.pop(word, None)
    return freq


# The source tokenizer split "don't" into "don" + "t": these stems are artifacts, and their count
# is the real frequency of the contraction.
SPLIT_STEMS_EN = {
    'don', 'doesn', 'didn', 'isn', 'aren', 'wasn', 'weren', 'haven', 'hasn', 'hadn', 'couldn',
    'wouldn', 'shouldn', 'mustn', 'needn', 'ain', 'll', 've', 're',
}


def clean_en(freq):
    stems = {stem: freq.pop(stem, 0) for stem in SPLIT_STEMS_EN}
    for contraction in CONTRACTIONS_EN:
        bare = contraction.replace("'", '').lower()
        stem = contraction.lower().split("'")[0]
        count = max(freq.get(bare, 0), stems.get(stem, 0))
        if bare not in REAL_EN:
            freq.pop(bare, None)
        # Contractions are very common in chat; give rare ones a reasonable floor.
        freq[contraction] = max(count, 10_000)
    freq['I'] = freq.pop('i', 1)
    return freq


def main():
    source, lang, out = sys.argv[1:4]
    freq = read(source)
    freq = {w: c for w, c in freq.items() if len(w) > 1 or w in SINGLE_LETTERS[lang]}
    freq = clean_es(freq) if lang == 'es' else clean_en(freq)
    with open(out, 'w', encoding='utf-8', newline='\n') as f:
        for word, count in sorted(freq.items(), key=lambda item: (-item[1], item[0])):
            f.write(f'{word}\t{count}\n')
    print(f'{lang}: {len(freq)} words -> {out}')


if __name__ == '__main__':
    main()
