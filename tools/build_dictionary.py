"""Builds the autocorrect dictionaries in app/src/main/assets/dict/ from FrequencyWords lists.

Source: https://github.com/hermitdave/FrequencyWords (content/2018/<lang>/<lang>_50k.txt),
data licensed CC-BY-SA 4.0. Run once and commit the output:

    python tools/build_dictionary.py <es_50k.txt> es app/src/main/assets/dict/es.tsv <diccionario-espanol.txt>
    python tools/build_dictionary.py <en_50k.txt> en app/src/main/assets/dict/en.tsv

Subtitle text contains common misspellings ("tambien", "cancion", "dont"). They must not be in
the dictionary, otherwise the keyboard would consider them correct and never fix them:
- Spanish: an unaccented word is dropped when its accented form is at least ACCENT_RATIO times
  more frequent, unless it is a real word of its own (KEEP_ES). Pairs where both forms are common
  (esta/está, como/cómo, si/sí) survive and are only offered as suggestions.
- English: apostrophes are missing from the source, so contractions are added and their
  apostrophe-less spellings ("dont", "im") are dropped unless they are real words.

A 50k list misses the rarer forms of common verbs: it has "elimina" and "eliminar" but not
"eliminaba", which then looked like a typo (or two words, "elimina ba"). Spanish therefore also
gets the regular forms of the verbs in the list (add_verb_forms_es): only the tenses that are
regular for every verb of their kind, only for words that are verbs in the list, and with a
frequency below any listed word, so they never outrank a real entry. Given a full word list
(the one build_word_set.py uses), a derived form is only kept if that list has it too; forms
with a pronoun attached (eliminarlo) are not in such lists and are kept as they are.
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


# Frequency of a derived verb form: below the rarest listed word (185), so real entries win.
DERIVED_FREQUENCY = 100

# Endings added to the stem (the infinitive without -ar), regular for every -ar verb.
AR_FORMS = [
    'aba', 'abas', 'ábamos', 'abais', 'aban',  # imperfect
    'aré', 'arás', 'ará', 'aremos', 'aréis', 'arán',  # future
    'aría', 'arías', 'aríamos', 'aríais', 'arían',  # conditional
    'amos', 'áis', 'ando', 'ado', 'ada', 'ados', 'adas',
]
# Preterite and imperfect subjunctive: regular for every -ar verb but these.
AR_PAST_FORMS = ['aste', 'ó', 'asteis', 'aron', 'ara', 'aras', 'áramos', 'arais', 'aran']
AR_IRREGULAR_PAST = {'estar', 'andar', 'desandar'}
# Imperfect, regular for every -er/-ir verb except ser, ir and ver (veía) with its compounds.
ER_IR_IMPERFECT = ['ía', 'ías', 'íamos', 'íais', 'ían']
ER_IR_IRREGULAR_IMPERFECT = {'prever', 'entrever', 'rever'}
# Future and conditional: added to the whole infinitive; these verbs (and compounds) shorten it.
FUTURE_ENDINGS = ['é', 'ás', 'á', 'emos', 'éis', 'án', 'ía', 'ías', 'íamos', 'íais', 'ían']
IRREGULAR_FUTURE = (
    'tener', 'poner', 'salir', 'valer', 'venir', 'decir', 'hacer', 'facer', 'querer', 'caber',
    'haber', 'poder', 'saber',
)
# Pronouns attached to an infinitive (hacerlo, decirte); with a gerund they need an accent.
CLITICS = ['lo', 'la', 'los', 'las', 'le', 'les', 'me', 'te', 'se', 'nos']
GERUND_CLITICS = ['lo', 'la', 'le', 'me', 'te', 'se']


# Conjugated forms that show an -ar / -er / -ir word is a verb; the first group only verbs have.
VERB_ONLY_AR = ('ado', 'ando', 'ó', 'aba', 'aron', 'amos')
VERB_ONLY_ER_IR = ('ido', 'iendo', 'yendo', 'ió', 'yó', 'ía', 'ían', 'emos', 'imos')
VERB_OTHER_AR = ('a', 'an', 'as', 'o', 'e', 'en')
VERB_OTHER_ER_IR = ('e', 'en', 'es', 'o')


def is_verb_es(stem, ar, freq):
    """A verb, not a noun like "lugar" or "taller": two of its forms are listed, one verb-only."""
    only = [e for e in (VERB_ONLY_AR if ar else VERB_ONLY_ER_IR) if stem + e in freq]
    other = [e for e in (VERB_OTHER_AR if ar else VERB_OTHER_ER_IR) if stem + e in freq]
    return len(only) >= 1 and len(only) + len(other) >= 2


def add_verb_forms_es(freq, valid=None):
    """Adds the regular forms of the listed verbs that the list is missing (see the docstring).

    [valid]: folded words of a full word list, to drop derived forms it doesn't have.
    """
    derived = set()
    with_pronoun = set()
    for infinitive in list(freq):
        stem, ending = infinitive[:-2], infinitive[-2:]
        if len(stem) < 2 or not stem.isalpha() or ending not in ('ar', 'er', 'ir', 'ír'):
            continue
        ar = ending == 'ar'
        if not is_verb_es(stem, ar, freq):
            continue
        if ar:
            derived.update(stem + e for e in AR_FORMS)
            if infinitive not in AR_IRREGULAR_PAST:
                derived.update(stem + e for e in AR_PAST_FORMS)
        else:
            if infinitive not in ER_IR_IRREGULAR_IMPERFECT:
                derived.update(stem + e for e in ER_IR_IMPERFECT)
            if not infinitive.endswith(IRREGULAR_FUTURE):
                derived.update(infinitive.replace('í', 'i') + e for e in FUTURE_ENDINGS)
        with_pronoun.update(infinitive + c for c in CLITICS)
        # The listed gerund with a pronoun (eliminándolo, haciéndolo, leyéndolo).
        for gerund in [stem + 'ando'] if ar else [stem + 'iendo', stem + 'yendo']:
            if gerund in freq:
                accented = gerund[:-4] + {'a': 'á', 'e': 'é'}[gerund[-4]] + 'ndo'
                with_pronoun.update(accented + c for c in GERUND_CLITICS)
        # A listed participle in its other genders and numbers (eliminado → eliminada, eliminados…).
        participle = stem + ('ado' if ar else 'ido')
        if participle in freq:
            derived.update(participle[:-1] + e for e in ('a', 'os', 'as'))
    unconfirmed = {w for w in derived if valid is not None and fold(w) not in valid}
    added = 0
    for word in (derived - unconfirmed) | with_pronoun:
        if word not in freq:
            freq[word] = DERIVED_FREQUENCY
            added += 1
    print(f'es: {added} verb forms added, {len(unconfirmed)} not in the word list dropped')
    return freq


def main():
    source, lang, out = sys.argv[1:4]
    valid = {fold(w) for w in open(sys.argv[4], encoding='utf-8').read().split()} if len(sys.argv) > 4 else None
    freq = read(source)
    freq = {w: c for w, c in freq.items() if len(w) > 1 or w in SINGLE_LETTERS[lang]}
    freq = add_verb_forms_es(clean_es(freq), valid) if lang == 'es' else clean_en(freq)
    with open(out, 'w', encoding='utf-8', newline='\n') as f:
        for word, count in sorted(freq.items(), key=lambda item: (-item[1], item[0])):
            f.write(f'{word}\t{count}\n')
    print(f'{lang}: {len(freq)} words -> {out}')


if __name__ == '__main__':
    main()
