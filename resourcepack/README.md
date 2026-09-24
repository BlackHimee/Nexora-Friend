# Nexora Social - resource pack

Retextures the **background** of every Nexora-Friend GUI (they're all
vanilla chest-style inventories under the hood: 27/45/54 slots + the Anvil
search screen) with a dark indigo/gold "arcane" panel instead of the vanilla
oak chest texture.

This is a pure **resource pack** - it overrides
`assets/minecraft/textures/gui/container/generic_54.png` and `anvil.png`,
the same base-game files every plain chest and every other 27/45/54-slot
inventory on the server uses. **No plugin code was changed** and none is
needed: item positions/hitboxes are computed by the client from fixed
vanilla constants, independent of this artwork, so this is purely cosmetic
and can't break functionality.

## What it does / doesn't cover

- ✅ Covers: the panel background, borders, slot outlines for every
  Nexora-Friend GUI (and, as a side effect, every other plain chest on the
  server - see "Scoping it to just this plugin" below if that's unwanted).
- ❌ Doesn't cover: individual item icons (player heads, compass, written
  book, etc.) - those keep their normal vanilla look. Re-skinning individual
  buttons needs a separate technique (CustomModelData + custom item models)
  which is a bigger job - ask if you want that too.

## Testing it yourself (client-side)

1. Copy `Nexora-Social-Texture.zip` into `.minecraft/resourcepacks/`.
2. In Minecraft: Options → Resource Packs → move it to the active list.
3. Open `/social` and look at the panel. **The exact pixel offsets for the
   player-inventory block (your own inventory rows at the bottom of the
   GUI) were my best-effort reconstruction of the vanilla layout** - I
   couldn't render real Minecraft to verify pixel-perfect alignment. If
   those bottom rows look shifted against their slot art, tell me exactly
   how (e.g. "slot outlines are 2px too high") and I'll correct the offset.

## Applying it for everyone on the server

Host the zip somewhere with a direct HTTPS link (GitHub raw, your own
webserver...) then in `server.properties`:

```properties
resource-pack=https://your-host/Nexora-Social-Texture.zip
resource-pack-sha1=<sha1 of the zip>
require-resource-pack=false
resource-pack-prompt=Texture Nexora Social (optionnelle)
```

Get the SHA1 with `sha1sum Nexora-Social-Texture.zip` (Linux/macOS) or
`Get-FileHash -Algorithm SHA1` (PowerShell) and update it every time you
regenerate the zip.

## Scoping it to just this plugin (avoid retexturing every chest)

Because this overrides the base `generic_54.png`/`anvil.png`, it will also
reskin **every plain chest and anvil** players open in the world, not just
`/social`. If you only want Nexora-Friend's menus textured and vanilla
chests untouched, that needs the CustomModelData/per-item approach instead
(re-skinning a dedicated filler item rather than the base container
texture) - say the word and I'll wire that into the plugin (`gui.yml` would
get a `custom-model-data` field per button/filler, `ItemBuilder` gets a
setter for it, and this resource pack gets restructured around item models
instead of the container texture).

## Regenerating / editing the art

The textures were generated with `gen.py` (Pillow) rather than hand-drawn -
ask if you want the palette changed (currently dark indigo/gold, matching
the plugin's `&8`/`&b`/`&6` message colors) or the script handed over so you
can tweak it yourself.
