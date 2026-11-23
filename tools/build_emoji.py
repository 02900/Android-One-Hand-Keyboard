"""Builds the emoji panel's data in app/src/main/assets/emoji/. Run once and commit the output:

    python tools/build_emoji.py <emoji-test.txt> <metadata.json> app/src/main/assets/emoji

emoji.tsv — the emojis of the panel, by group: "<icon>\t<name>\t<emoji> <emoji>…", one line per
group, in Unicode's order. Source: https://unicode.org/Public/emoji/latest/emoji-test.txt
(Unicode License V3). Only fully-qualified emojis, without the skin tone and hair variants
(the panel shows the default one) nor the components group. The keyboard hides the ones the
phone can't draw (too new for its emoji font).

kitchen.bin — which pairs of emojis have an Emoji Kitchen mix, and the date in the address of
its image on Google's servers. Source: the metadata.json of github.com/xsalazar/emoji-kitchen-backend
(the data behind emojikitchen.dev). The images themselves are not bundled: the keyboard
downloads them from Google when a mix is shown. Format (big-endian, read by EmojiKitchen.kt):

    "EKIT"
    u16 emoji count, then per emoji: u8 length + its code points as in the image addresses
        ("1f62e-200d-1f4a8", UTF-8)
    u8 date count, then per date: u32 yyyymmdd
    per emoji (as the left one of the mix): u16 count, then count × u16 (right emoji << 6 | date),
        sorted by right emoji

A mix image is at
https://www.gstatic.com/android/keyboard/emojikitchen/<date>/u<left>/u<left>_u<right>.png
with each "-" of the code points written "-u" (checked for every mix while building).
"""

import json
import os
import struct
import sys

# Unicode group → (tab icon, name shown in the panel). Component (skin tones alone) is left out.
GROUPS = {
    'Smileys & Emotion': ('😀', 'Caras y emociones'),
    'People & Body': ('👋', 'Personas'),
    'Animals & Nature': ('🐻', 'Animales y naturaleza'),
    'Food & Drink': ('🍔', 'Comida y bebida'),
    'Travel & Places': ('🚗', 'Viajes y lugares'),
    'Activities': ('⚽', 'Actividades'),
    'Objects': ('💡', 'Objetos'),
    'Symbols': ('🔣', 'Símbolos'),
    'Flags': ('🏳️', 'Banderas'),
}
# Skin tones and hair styles: their variants are left out.
MODIFIERS = {0x1F3FB, 0x1F3FC, 0x1F3FD, 0x1F3FE, 0x1F3FF, 0x1F9B0, 0x1F9B1, 0x1F9B2, 0x1F9B3}
URL = 'https://www.gstatic.com/android/keyboard/emojikitchen/{date}/u{left}/u{left}_u{right}.png'
RIGHT_SHIFT = 6


def read_emoji(path):
    groups = {}
    group = None
    for line in open(path, encoding='utf-8'):
        if line.startswith('# group:'):
            group = line.split(':', 1)[1].strip()
            continue
        if line.startswith('#') or ';' not in line or group not in GROUPS:
            continue
        codes, rest = line.split(';', 1)
        if rest.split('#', 1)[0].strip() != 'fully-qualified':
            continue
        points = [int(c, 16) for c in codes.split()]
        if len(points) > 1 and any(p in MODIFIERS for p in points):
            continue
        groups.setdefault(group, []).append(''.join(map(chr, points)))
    return groups


def write_emoji(groups, out):
    with open(out, 'w', encoding='utf-8', newline='\n') as f:
        for group, (icon, name) in GROUPS.items():
            f.write(f'{icon}\t{name}\t{" ".join(groups[group])}\n')
    print(f'{sum(map(len, groups.values()))} emojis in {len(groups)} groups -> {out}')


def read_kitchen(path):
    """{(left, right): date} of the latest image of every mix (left/right as in the addresses)."""
    data = json.load(open(path, encoding='utf-8'))['data']
    mixes = {}
    for emoji in data.values():
        for combos in emoji['combinations'].values():
            for combo in combos:
                if not combo.get('isLatest'):
                    continue
                left, right, date = combo['leftEmojiCodepoint'], combo['rightEmojiCodepoint'], combo['date']
                url = URL.format(date=date, left=left.replace('-', '-u'), right=right.replace('-', '-u'))
                if combo['gStaticUrl'] != url:
                    raise SystemExit(f'Unexpected address {combo["gStaticUrl"]} (expected {url})')
                mixes[(left, right)] = date
    return mixes


def write_kitchen(mixes, out):
    emojis = sorted({code for pair in mixes for code in pair})
    dates = sorted(set(mixes.values()))
    index = {code: i for i, code in enumerate(emojis)}
    date_index = {date: i for i, date in enumerate(dates)}
    if len(emojis) >= 1 << (16 - RIGHT_SHIFT) or len(dates) >= 1 << RIGHT_SHIFT:
        raise SystemExit(f'{len(emojis)} emojis / {len(dates)} dates do not fit in the format')
    by_left = [[] for _ in emojis]
    for (left, right), date in mixes.items():
        by_left[index[left]].append(index[right] << RIGHT_SHIFT | date_index[date])
    with open(out, 'wb') as f:
        f.write(b'EKIT' + struct.pack('>H', len(emojis)))
        for code in emojis:
            raw = code.encode('utf-8')
            f.write(struct.pack('>B', len(raw)) + raw)
        f.write(struct.pack('>B', len(dates)))
        for date in dates:
            f.write(struct.pack('>I', int(date)))
        for entries in by_left:
            f.write(struct.pack('>H', len(entries)))
            f.write(b''.join(struct.pack('>H', e) for e in sorted(entries)))
    print(f'{len(mixes)} mixes of {len(emojis)} emojis, {len(dates)} dates -> {out} ({os.path.getsize(out) // 1024} KB)')


def main():
    emoji_test, metadata, out_dir = sys.argv[1:4]
    os.makedirs(out_dir, exist_ok=True)
    write_emoji(read_emoji(emoji_test), os.path.join(out_dir, 'emoji.tsv'))
    write_kitchen(read_kitchen(metadata), os.path.join(out_dir, 'kitchen.bin'))


if __name__ == '__main__':
    main()
