# Contrato: release de prueba del spike (US3)

Interfaz externa entre el repositorio y la herramienta para la prueba de actualización.

## Release

| Elemento | Valor |
|----------|-------|
| Repositorio | `sergiosahuquillo/burn-files` |
| Tag | `spike-v<versionName>`, p. ej. `spike-v0.0.2` |
| Tipo | **pre-release** (nunca release estable; el updater de producto las ignora) |
| Título | `Spike F0 test build <versionName>` |

## Assets

| Nombre | Contenido |
|--------|-----------|
| `burnfiles-spike-<versionName>.apk` | APK firmada con la clave de pruebas (`debug.keystore` del desarrollador) |
| `burnfiles-spike-<versionName>.apk.sha256` | Una línea: `<sha256 hex en minúsculas>  burnfiles-spike-<versionName>.apk` (formato `sha256sum`) |

## Consulta que hace la herramienta

```http
GET https://api.github.com/repos/sergiosahuquillo/burn-files/releases?per_page=20
Accept: application/vnd.github+json
X-GitHub-Api-Version: 2022-11-28
If-None-Match: <ETag previo, si existe>
```

- Selección: la release con `tag_name` que empiece por `spike-v` y mayor `versionCode` (derivado de
  `versionName` con `major * 10000 + minor * 100 + patch`).
- `403`/`429` con `X-RateLimit-Remaining: 0` → resultado `RATE_LIMITED`, mostrando
  `X-RateLimit-Reset` en hora local.
- Descarga: `browser_download_url` de cada asset, siguiendo redirecciones solo a `github.com` y
  `objects.githubusercontent.com` (principio VI).

## Reglas de verificación

1. El SHA-256 del archivo descargado es igual al del asset `.sha256` → si no, `VERIFY_SHA256 / MISMATCH`.
2. El conjunto de certificados de firma del APK es igual al del paquete instalado → si no,
   `VERIFY_CERT / MISMATCH`.
3. `packageName` del APK == `es.sergiosahuquillo.burnfiles.spike` y `versionCode` > instalado → si no,
   `FAILED`.
4. Si alguna regla falla, el archivo descargado se elimina de `cacheDir/updates/` antes de terminar.
