import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
const require = createRequire(import.meta.url);
const sharp = require(process.env.SHARP_MODULE || 'sharp');
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../app/src/main/assets/characters');
const catalog = JSON.parse(await fs.readFile(path.join(root, 'catalog.json'), 'utf8'));
assert.equal(catalog.heroes.length, 10);
const expected = { idle:8, run:8, attack:8, defend:8, hurt:8, stun:8, confused:8, victory:12, dead:8, skill1:36, skill2:36, ultima1:44, ultima2:44 };
let total = 0, effectTotal = 0, bytes = 0;
for (const hero of catalog.heroes) {
  const manifest = JSON.parse(await fs.readFile(path.join(root, hero.manifest), 'utf8'));
  assert.equal(manifest.totalCharacterFrames, 236);
  assert.equal(Object.keys(manifest.animations).length, 13);
  for (const [kind, collection] of [['character', manifest.animations], ['effect', manifest.effects]]) {
    for (const [name, spec] of Object.entries(collection)) {
      assert.equal(spec.frameCount, kind === 'character' ? expected[name] : 44);
      assert.equal(spec.columns * spec.rows, spec.frameCount);
      const file = path.join(root, hero.sprite, spec.file);
      const { data, info } = await sharp(file).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
      assert.equal(info.width, spec.columns * spec.frameWidth, file + ' width');
      assert.equal(info.height, spec.rows * spec.frameHeight, file + ' height');
      let transparent = false;
      const hashes = new Set();
      for (let k = 0; k < spec.frameCount; k++) {
        const hash = crypto.createHash('sha256');
        let visible = 0;
        for (let y = 0; y < spec.frameHeight; y++) {
          const row = Math.floor(k / spec.columns) * spec.frameHeight + y;
          const start = (row * info.width + k % spec.columns * spec.frameWidth) * 4;
          const slice = data.subarray(start, start + spec.frameWidth * 4);
          hash.update(slice);
          for (let x = 0; x < slice.length; x += 4) {
            if (slice[x + 3] < 5) transparent = true;
            if (slice[x + 3] > 35) visible++;
            assert.ok(!(slice[x + 3] > 240 && slice[x + 1] > 250 && slice[x] < 5 && slice[x + 2] < 5), 'Unremoved green background: ' + file);
          }
        }
        if (kind === 'character') assert.ok(visible > 100, 'Empty frame: ' + file + '#' + k);
        hashes.add(hash.digest('hex'));
      }
      assert.ok(transparent, 'Missing transparency: ' + file);
      assert.ok(hashes.size >= spec.frameCount - 1, 'Insufficient distinct frames: ' + file + ', ' + hashes.size);
      if (kind === 'character') total += spec.frameCount; else effectTotal += spec.frameCount;
      bytes += (await fs.stat(file)).size;
    }
  }
  const portrait = await sharp(path.join(root, hero.portrait)).metadata();
  assert.ok(portrait.hasAlpha);
  console.log(hero.name + ': 236 character + 88 ultimate-effect frames validated');
}
assert.equal(total, 2360); assert.equal(effectTotal, 880);
console.log(JSON.stringify({ heroes: 10, animations: 130, characterFrames: total, ultimateEffectFrames: effectTotal, atlasMiB: Number((bytes / 1048576).toFixed(1)), status: 'PASS' }, null, 2));
