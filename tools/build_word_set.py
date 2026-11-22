"""Builds a compact "is this a real word?" set (a Bloom filter) from a plain word list.

The keyboard's dictionary (build_dictionary.py) is a frequency list: it knows the ~90k most
used Spanish forms and how common each one is, but not the rare ones. This set knows every
form of a full Spanish word list (639k forms, from an open-source Scrabble project, via
github.com/02900/boggle public/diccionario-espanol.txt) without frequencies or accents, in
under 1 MB: a word typed that is in it is real, so the keyboard doesn't force a correction.

    python tools/build_word_set.py <diccionario-espanol.txt> app/src/main/assets/dict/es_words.bin

Format (read by WordSet.kt): "WSET", bit count (8 bytes) and hash count (4 bytes), big-endian,
then the bits. Words are folded like the dictionary keys (lowercase, no accents, ñ kept) and
hashed with FNV-1a 64 from two offsets, combined by double hashing.
"""

import math
import struct
import sys

from build_dictionary import fold

FALSE_POSITIVE_RATE = 0.01
MASK = (1 << 64) - 1
FNV_PRIME = 0x100000001B3
OFFSETS = (0xCBF29CE484222325, 0x84222325CBF29CE4)


def fnv1a(data, offset):
    h = offset
    for byte in data:
        h = ((h ^ byte) * FNV_PRIME) & MASK
    return h


def positions(word, bits, hashes):
    data = word.encode('utf-8')
    h1 = fnv1a(data, OFFSETS[0])
    h2 = fnv1a(data, OFFSETS[1]) | 1
    return [((h1 + i * h2) & MASK) % bits for i in range(hashes)]


def main():
    source, out = sys.argv[1:3]
    words = {fold(line.strip()) for line in open(source, encoding='utf-8') if line.strip()}
    n = len(words)
    bits = math.ceil(-n * math.log(FALSE_POSITIVE_RATE) / math.log(2) ** 2)
    hashes = max(1, round(bits / n * math.log(2)))
    table = bytearray((bits + 7) // 8)
    for word in words:
        for bit in positions(word, bits, hashes):
            table[bit >> 3] |= 1 << (bit & 7)
    with open(out, 'wb') as f:
        f.write(b'WSET' + struct.pack('>qi', bits, hashes) + bytes(table))
    print(f'{n} words -> {out} ({len(table) / 1e6:.2f} MB, {hashes} hashes)')


if __name__ == '__main__':
    main()
