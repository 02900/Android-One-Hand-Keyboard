# Actualizaciones y releases

Solo el build de debug (el que se publica) se actualiza solo; el de release no tiene permiso de red.

## Cómo se actualiza la app

La app busca el build más nuevo en dos lugares y se queda con el más reciente:

1. **GitHub Releases** (desde cualquier red). La app lee `releases/latest/download/update.json`, que no pasa por la API de GitHub, así que no hay límite de cupo. Cómo publicar: [Publicar una versión](#publicar-una-versión-github-actions).
2. **Tu PC en la red local**, para probar un build antes de publicarlo:

```bash
python tools/serve_updates.py   # compila, publica y sirve; Ctrl+C para parar
```

- **Sin IP fija:** el teléfono pregunta por broadcast UDP (puerto 47800) y el script responde, así que sigue funcionando aunque el router le cambie la IP a la PC. Si el script no está corriendo, el teclado simplemente no encuentra actualizaciones.
- **Ajustes → Versión** busca al abrir y muestra **Actualizar** si hay un build más nuevo (otro commit, no más viejo). La primera vez Android pide confirmar y permitir "instalar apps de esta fuente".
- **Actualizar sola** (Android 12+): después de la primera actualización desde la app, el teclado busca cada 10 min mientras escribes, descarga en segundo plano e instala **al ocultarse**. Si Android pide confirmar, aparece "⬆ actualización" junto a la versión del teclado.
- El APK se verifica con el SHA-256 de `update.json`, y Android solo lo instala si tiene la misma firma que la app instalada.
- Solo el build de debug (el que se publica) tiene permiso de red (`src/debug/AndroidManifest.xml`); el de release sigue sin red.
- La primera vez que corre el script, Windows puede preguntar si Python puede usar redes privadas: hay que permitirlo.

### Clave de firma

Todas las builds publicadas se firman con la misma clave, para que cada actualización se instale sobre la anterior:

- **Local:** `keystore.properties` en la raíz del repo (no está en git). Ver [Restaurar la clave de firma](DESARROLLO.md#4-restaurar-la-clave-de-firma). Sin ese archivo, Gradle usa la clave de debug de la máquina.
- **GitHub Actions:** los secretos `OHK_KEYSTORE_*`. Ver [Publicar una versión](#publicar-una-versión-github-actions).
- ⚠️ **Haz una copia de seguridad del `.jks` y de `keystore.properties`.** Si se pierde la clave, las apps instaladas ya no aceptan actualizaciones: hay que desinstalar y volver a instalar.

## Publicar una versión (GitHub Actions)

El workflow [`.github/workflows/release.yml`](../.github/workflows/release.yml) compila el APK, lo firma con la clave fija y publica una **GitHub Release** con el APK y su `update.json`. Los teléfonos lo detectan solos (ver [Cómo se actualiza la app](#cómo-se-actualiza-la-app)).

**Cuándo se ejecuta**

| Evento | Nombre de la release |
|---|---|
| Push a `main` (incluye mergear un PR) | `build-<commit>` |
| Push de un tag `v*` | el tag, por ejemplo `v1.0.1` |
| Manual: **Actions → Release → Run workflow** | `build-<commit>` (solo cuando el workflow ya está en `main`) |

**Publicar con un tag**

```bash
git tag -a v1.0.1 -m "Qué cambia"
git push origin v1.0.1
gh run watch                       # sigue el workflow (~5 min)
gh release list                    # la nueva aparece como "Latest"
```

**Qué hace, paso a paso:** restaura la clave desde los secretos, compila con `assembleDebug`, escribe `update.json` (`python3 tools/serve_updates.py --no-build --no-serve --dir dist`), crea la release y la marca como *Latest*. Si la release ya existía (por ejemplo, al reintentar), reemplaza los archivos. Al final borra la clave del runner.

**Secretos del repo** (Settings → Secrets and variables → Actions)

| Secreto | Contenido |
|---|---|
| `OHK_KEYSTORE_BASE64` | El `.jks` en base64 |
| `OHK_KEYSTORE_PASSWORD` | Contraseña del keystore |
| `OHK_KEY_ALIAS` | `onehandkeyboard` |
| `OHK_KEY_PASSWORD` | Contraseña de la clave |

Para volver a cargarlos desde una PC que tenga la clave (Git Bash):

```bash
base64 -w0 ~/.keystores/one-hand-keyboard.jks | gh secret set OHK_KEYSTORE_BASE64
gh secret set OHK_KEYSTORE_PASSWORD   # pide el valor (storePassword de keystore.properties)
gh secret set OHK_KEY_ALIAS --body onehandkeyboard
gh secret set OHK_KEY_PASSWORD        # pide el valor (keyPassword)
```

**Si una release salió mal:** `gh release delete <tag> --cleanup-tag` la borra y la anterior vuelve a ser la *Latest*. Los teléfonos que ya se actualizaron **no vuelven atrás**, porque la app solo instala commits más nuevos. Para corregir, publica un commit nuevo con el arreglo.

**Costo:** gratis. Los runners estándar de GitHub no tienen límite en repos públicos, y las descargas desde `releases/latest/download` no consumen cupo de la API.
