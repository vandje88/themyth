import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import crypto from 'node:crypto';

const require = createRequire(import.meta.url);
const sharp = require(process.env.SHARP_MODULE || 'sharp');
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../app/src/main/assets/characters');
const source = process.argv[2];
const effectsOnly = process.argv.includes('--effects-only');
const selectedSprite = process.argv[3] && !process.argv[3].startsWith('--') ? process.argv[3] : null;
if (!source) throw new Error('Usage: node tools/generate-characters.mjs <keypose-image-directory> [sprite] [--effects-only]');
const W = 192, H = 256;
const specs = {
  idle: [8, 4, 2, 80, true], run: [8, 4, 2, 65, true], attack: [8, 4, 2, 65, false],
  defend: [8, 4, 2, 80, false], hurt: [8, 4, 2, 65, false], stun: [8, 4, 2, 80, true],
  confused: [8, 4, 2, 80, true], victory: [12, 4, 3, 80, true], dead: [8, 4, 2, 80, false],
  skill1: [36, 6, 6, 45, false], skill2: [36, 6, 6, 45, false],
  ultima1: [44, 4, 11, 50, false], ultima2: [44, 4, 11, 50, false]
};
const heroes = [
  { sprite: 'gatotkaca', id: 'gatotkaca', name: 'Gatotkaca', color: '#ffcc55', accent: '#fff7cd', chroma: 'green',
    design: 'Muscular Javanese celestial warrior, black mustache, wing-shaped golden crown, black-gold armor, red waist cloth, golden bracers.',
    skills: [
      ['Otot Kawat Tulang Besi', 'Pertahanan diri +50% selama 5 detik; memantulkan sebagian damage fisik.'],
      ['Pukulan Brajadenta', 'Tinju berapi ke musuh di depan; stun 1,5 detik.'],
      ['Terbang ke Angkasa', 'Melompat tinggi dan menubruk barisan belakang; massive physical damage area.'],
      ['Brajamusti – Amukan Petir', 'Seluruh musuh; massive physical damage; pertahanan -25% selama 4 detik. Aura petir emas, pukulan energi, ledakan petir, gelombang kejut.']
    ] },
  { sprite: 'nyairorokidul', id: 'nyai_roro_kidul', name: 'Nyai Roro Kidul', color: '#31ddd5', accent: '#bdffff', chroma: 'magenta',
    design: 'Southern ocean queen, long black hair, gold Javanese crown, emerald turquoise kebaya, batik skirt, cyan ocean shawl.',
    skills: [
      ['Gelombang Mistis', 'Ombak energi gaib menembus musuh terdepan dan mengurangi attack speed.'],
      ['Mahkota Samudra', 'Perisai air ke rekan satu tim dengan HP terendah.'],
      ['Tsunami Pantai Selatan', 'Dinding air raksasa menyapu seluruh musuh; slow dan magic damage berkala.'],
      ['Gerbang Istana Laut Selatan', 'Seluruh musuh; massive magic damage; movement speed -40% selama 5 detik; memulihkan HP tim 15%. Gerbang istana, pusaran air, ikan mistis, gelombang energi.']
    ] },
  { sprite: 'prabusiliwangi', id: 'prabu_siliwangi', name: 'Prabu Siliwangi', color: '#f5d383', accent: '#ffffff', chroma: 'green',
    design: 'Sundanese king, gold crown, white-gold armor, brown batik waist cloth, tiger fur mantle, white tiger spirit.',
    skills: [
      ['Aum Harimau Putih', 'Raungan menakuti musuh; attack power musuh terdepan berkurang.'],
      ['Cakar Maung', 'Cakar gaib; memulihkan HP diri 30% dari damage.'],
      ['Wujud Maung Siliwangi', 'Transformasi harimau putih raksasa; attack speed dan crit rate meningkat drastis.'],
      ['Murka Maung Pajajaran', 'Seluruh musuh; massive physical damage; attack power -30% selama 5 detik; physical attack tim +20% selama 5 detik. Harimau putih, aura emas, jejak cakar.']
    ] },
  { sprite: 'sipitung', id: 'si_pitung', name: 'Si Pitung', color: '#80dbff', accent: '#e4fbff', chroma: 'green',
    design: 'Athletic Betawi silat hero, black peci cap, red shirt, black vest, loose black trousers, checked sash, barefoot.',
    skills: [
      ['Jurus Silat Betawi', 'Kombinasi tendangan cepat bertubi-tubi ke satu musuh.'],
      ['Kebal Peluru', 'Kebal serangan fisik biasa selama 3 detik.'],
      ['Jagoan Rawa Belong', 'Menghilang lalu muncul di belakang musuh HP terendah; serangan fatal execute.'],
      ['Jurus Seribu Bayangan', 'Seluruh musuh; high physical damage bertubi-tubi; tiap hit peluang stun 20% selama 1 detik; dodge diri +30% selama 3 detik. Bayangan berlapis dan jejak energi.']
    ] },
  { sprite: 'lutungkasarung', id: 'lutung_kasarung', name: 'Lutung Kasarung', color: '#a5ef9a', accent: '#fff4b7', chroma: 'magenta',
    design: 'Sacred black lutung monkey, expressive noble face, curved tail, gold armlets, cream-gold waist sarong; Guru Minda prince transformation.',
    skills: [
      ['Lempar Buah Gaib', 'Buah ajaib ke musuh; blind sesaat.'],
      ['Kecerdasan Kera Suci', 'Dodge rate seluruh tim meningkat selama 4 detik.'],
      ['Transformasi Guru Minda', 'Berwujud pangeran; memulihkan seluruh HP tim dan meningkatkan serangan.'],
      ['Anugerah Hutan Kahyangan', 'Seluruh tim; shield 20% Max HP; heal 20% Max HP; magic resistance +25% selama 5 detik. Cahaya emas, sakura, daun bercahaya, roh hutan.']
    ] },
  { sprite: 'calonarang', id: 'calon_arang', name: 'Calon Arang', color: '#b575ff', accent: '#f1ccff', chroma: 'green',
    design: 'Formidable Balinese dark sorceress, aged fierce face, black silver-streaked hair, gold-black headdress, black-purple ceremonial robes; Leak spirit.',
    skills: [
      ['Kutukan Penyakit', 'Wabah hitam; racun damage over time setiap detik.'],
      ['Sihir Leak', 'Bola api gaib menyerang musuh acak sebanyak 3 kali.'],
      ['Ritual Kematian', 'Mengorbankan sedikit HP; ledakan dark magic damage besar ke semua musuh.'],
      ['Bangkitnya Ratu Leak', 'Seluruh musuh; massive dark magic damage; curse 6 detik mengurangi HP 3% Max HP tiap detik; heal diri 30% total damage kutukan. Portal, roh Leak, api ungu.']
    ] },
  { sprite: 'kenarok', id: 'ken_arok', name: 'Ken Arok', color: '#ff5265', accent: '#ffc9b5', chroma: 'green',
    design: 'Athletic ambitious Javanese warrior king, black tied hair, gold crown, burgundy black-gold armor, batik sarong, wavy crimson sacred keris.',
    skills: [
      ['Sabetan Keris Mpu Gandring', 'Serangan keris mengabaikan 30% armor musuh.'],
      ['Ambisi Takhta', 'Critical damage diri meningkat setiap berhasil membunuh musuh.'],
      ['Dendam Sang Pendiri', 'Kutukan keris; burst damage tinggi dan bleed.'],
      ['Takdir Keris Mpu Gandring', 'Seluruh musuh; massive physical dan dark magic damage; armor pierce 40%; bleed 2% Max HP tiap detik selama 4 detik; critical rate diri +5% per musuh selama 5 detik.']
    ] },
  { sprite: 'roromendut', id: 'roro_mendut', name: 'Roro Mendut', color: '#ff94c4', accent: '#ffeab8', chroma: 'green',
    design: 'Defiant adult Javanese woman, black hair with gold flowers, crimson rose-gold kebaya, dark batik skirt, flowing rose-gold shawl.',
    skills: [
      ['Pesona Rok Cita', 'Memikat musuh terdepan; tidak bisa menyerang selama 1 detik.'],
      ['Pajak Rok Rok', 'Mencuri 15% energy atau mana target.'],
      ['Perlawanan Tragis', 'Aura kesedihan dan perlawanan; magic damage area dan slow.'],
      ['Pesona Seribu Bunga', 'Seluruh musuh; magic damage; attack speed -35% selama 5 detik; menyerap 20% energy/mana seluruh musuh dan membagikan ke tim. Kelopak bunga, aura pink-emas.']
    ] },
  { sprite: 'damarwulan', id: 'damarwulan', name: 'Damarwulan', color: '#ffd760', accent: '#fffad8', chroma: 'green',
    design: 'Majapahit knight, gold winged headband, gold-bronze armor, crimson sash, batik trousers, golden mace and bow; red-gold Majapahit sun banner.',
    skills: [
      ['Tusukan Panah Emas', 'Panah cahaya ke musuh HP tertinggi.'],
      ['Wibawa Kesatria', 'Attack rekan tim barisan depan meningkat.'],
      ['Kemenangan atas Minak Jinggo', 'Gada pusaka; stun area dan damage masif.'],
      ['Titah Ksatria Majapahit', 'Seluruh tim; physical attack +35% selama 6 detik; critical rate +15% selama 6 detik; menghilangkan satu debuff tiap rekan. Panji, cahaya emas, simbol kerajaan.']
    ] },
  { sprite: 'jakatarub', id: 'jaka_tarub', name: 'Jaka Tarub', color: '#92dfff', accent: '#fff4c9', chroma: 'magenta',
    design: 'Lean Javanese mythical hunter, black tied hair, brown-gold headband, ivory-gold tunic, brown batik trousers, blue-gold celestial shawl, sacred spear.',
    skills: [
      ['Selendang Bidadari', 'Selendang gaib mengikat musuh; menghentikan gerak selama 2 detik.'],
      ['Pencuri Cahaya', 'Menghisap buff aktif musuh dan memberikannya ke diri sendiri.'],
      ['Penerbangan Kayangan', 'Kekuatan bidadari menghujani musuh dengan bulu cahaya tajam.'],
      ['Tujuh Selendang Kahyangan', 'Seluruh musuh; massive magic damage; silence 2 detik; peluang 25% menghilangkan satu buff tiap musuh. Tujuh selendang, bulu cahaya, pusaran dan ledakan cahaya.']
    ] }
];
const clamp = (v, a = 0, b = 1) => Math.max(a, Math.min(b, v));
const mix = (a, b, t) => a + (b - a) * t;
const smooth = t => { t = clamp(t); return t * t * (3 - 2 * t); };
const fract = v => v - Math.floor(v);
const rnd = n => fract(Math.sin(n * 12.9898 + 78.233) * 43758.5453);
const f = n => Number(n.toFixed(2));

function chromaKey(data, chroma) {
  for (let i = 0; i < data.length; i += 4) {
    const r = data[i], g = data[i + 1], b = data[i + 2];
    const excess = chroma === 'green' ? g - Math.max(r, b) : Math.min(r, b) - g;
    if (excess > 25) {
      data[i + 3] = Math.round(255 * (1 - clamp((excess - 25) / 95)));
      if (chroma === 'green') data[i + 1] = Math.min(g, Math.max(r, b));
      else { data[i] = Math.min(r, g + 30); data[i + 2] = Math.min(b, g + 30); }
    }
    if (!data[i + 3]) data[i] = data[i + 1] = data[i + 2] = 0;
  }
  return data;
}
function removeSpecks(data, width, height) {
  const seen = new Uint8Array(width * height);
  for (let start = 0; start < seen.length; start++) {
    if (seen[start] || data[start * 4 + 3] < 6) continue;
    const stack = [start], component = [];
    seen[start] = 1;
    while (stack.length) {
      const p = stack.pop(); component.push(p);
      const x = p % width, y = Math.floor(p / width);
      for (const n of [x > 0 ? p - 1 : -1, x < width - 1 ? p + 1 : -1, y > 0 ? p - width : -1, y < height - 1 ? p + width : -1]) {
        if (n >= 0 && !seen[n] && data[n * 4 + 3] >= 6) { seen[n] = 1; stack.push(n); }
      }
    }
    if (component.length < 75) for (const p of component) data.fill(0, p * 4, p * 4 + 4);
  }
}
function bounds(data, width, height) {
  let l = width, r = 0, t = height, b = 0;
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    if (data[(y * width + x) * 4 + 3] > 35) { l = Math.min(l, x); r = Math.max(r, x); t = Math.min(t, y); b = Math.max(b, y); }
  }
  if (l > r || t > b) throw new Error('Empty character pose');
  return { left: l, top: t, width: r - l + 1, height: b - t + 1 };
}
async function prepare(hero, filename, dir) {
  const { data, info } = await sharp(filename).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  chromaKey(data, hero.chroma);
  await sharp(data, { raw: { width: info.width, height: info.height, channels: 4 } }).png().toFile(path.join(dir, 'keyposes.png'));
  const rows = [0];
  for (let r = 1; r < 4; r++) {
    let best = Math.round(info.height * r / 4), score = Infinity;
    for (let y = best - 30; y <= best + 30; y++) {
      let count = 0;
      for (let x = 0; x < info.width; x++) if (data[(y * info.width + x) * 4 + 3] > 35) count++;
      const cost = count + Math.abs(y - info.height * r / 4) * 0.15;
      if (cost < score) { score = cost; rows[r] = y; }
    }
  }
  rows.push(info.height);
  const poses = [];
  let scale;
  for (let k = 0; k < 16; k++) {
    const col = k % 4, row = Math.floor(k / 4);
    const pw = info.width / 4, ph = rows[row + 1] - rows[row];
    const cut = await sharp(data, { raw: { width: info.width, height: info.height, channels: 4 } })
      .extract({ left: col * pw, top: rows[row], width: pw, height: ph }).raw().toBuffer();
    removeSpecks(cut, pw, ph);
    const box = bounds(cut, pw, ph);
    if (!k) scale = 217 / box.height;
    const s = Math.min(scale, 174 / box.width, 226 / box.height);
    const w = Math.max(1, Math.round(box.width * s)), h = Math.max(1, Math.round(box.height * s));
    const png = await sharp(cut, { raw: { width: pw, height: ph, channels: 4 } }).extract(box).resize(w, h).png().toBuffer();
    const normalized = await sharp({ create: { width: W, height: H, channels: 4, background: '#00000000' } })
      .composite([{ input: png, left: Math.round((W - w) / 2), top: H - 12 - h }]).raw().toBuffer();
    poses.push({ data: normalized, png: await sharp(normalized, { raw: { width: W, height: H, channels: 4 } }).png().toBuffer() });
  }
  await sharp(poses[0].png).resize(384, 512).png().toFile(path.join(dir, hero.sprite + '.png'));
  return poses;
}
function silhouette(data) {
  const result = [];
  for (let y = 0; y < H; y++) {
    let left = W, right = 0;
    for (let x = 0; x < W; x++) if (data[(y * W + x) * 4 + 3] > 35) { left = Math.min(left, x); right = Math.max(right, x); }
    result.push(left > right ? [W / 2, 8] : [(left + right) / 2, Math.max(8, (right - left) / 2)]);
  }
  return result;
}
function sample(data, x, y) {
  if (x < 0 || y < 0 || x >= W - 1 || y >= H - 1) return [0, 0, 0, 0];
  const ix = Math.floor(x), iy = Math.floor(y), tx = x - ix, ty = y - iy;
  const out = [0, 0, 0, 0];
  for (let j = 0; j < 2; j++) for (let i = 0; i < 2; i++) {
    const p = ((iy + j) * W + ix + i) * 4;
    const weight = (i ? tx : 1 - tx) * (j ? ty : 1 - ty), alpha = data[p + 3] / 255;
    for (let c = 0; c < 3; c++) out[c] += data[p + c] * alpha * weight;
    out[3] += data[p + 3] * weight;
  }
  if (out[3]) for (let c = 0; c < 3; c++) out[c] *= 255 / out[3];
  return out;
}
function timeline(hero, anim) {
  const attack = [[0, 0], [.2, 14], [.42, 3], [.65, 3], [1, 0]];
  if (anim === 'attack') return attack;
  if (anim === 'defend') return [[0, 0], [.24, 4], [.78, 4], [1, 0]];
  if (anim === 'hurt') return [[0, 0], [.23, 5], [.55, 5], [1, 0]];
  if (anim === 'skill1') return [[0, 0], [.17, 14], [.35, 10], [.76, 10], [1, 0]];
  if (anim === 'skill2') return [[0, 0], [.18, 4], [.4, 11], [.78, 11], [1, 0]];
  if (anim === 'ultima1') {
    if (['prabusiliwangi', 'lutungkasarung'].includes(hero.sprite)) return [[0, 0], [.18, 12], [.42, 12], [.58, 15], [1, 15]];
    return [[0, 0], [.18, 14], [.4, 12], [.74, 12], [.88, 15], [1, 0]];
  }
  if (anim === 'ultima2') return [[0, 0], [.18, 14], [.4, 13], [.75, 13], [.88, 3], [1, 0]];
  const p = { idle: 0, run: 1, stun: 6, confused: 7, victory: 8, dead: 9 }[anim];
  return [[0, p], [1, p]];
}
function tween(hero, poses, anim, t) {
  const keys = timeline(hero, anim);
  let a = keys[0][1], b = a, q = 0;
  for (let i = 1; i < keys.length; i++) if (t <= keys[i][0]) {
    a = keys[i - 1][1]; b = keys[i][1]; q = smooth((t - keys[i - 1][0]) / (keys[i][0] - keys[i - 1][0])); break;
  }
  const A = poses[a].data, B = poses[b].data, sa = poses[a].silhouette, sb = poses[b].silhouette;
  const out = Buffer.alloc(W * H * 4), phase = t * Math.PI * 2;
  const sin = Math.sin(phase), cos = Math.cos(phase);
  let lift = anim === 'run' ? Math.abs(sin) * 5 : anim === 'idle' ? sin * 1.2 : anim === 'victory' ? sin * 2 : 0;
  let angle = anim === 'hurt' ? Math.sin(Math.PI * t) * -.06 : anim === 'confused' ? sin * .035 : anim === 'run' ? sin * .018 : 0;
  let squash = anim === 'idle' ? 1 + sin * .009 : anim === 'dead' ? 1 - t * .015 : 1;
  if (anim === 'ultima1' && hero.sprite === 'gatotkaca') lift += 56 * Math.sin(Math.PI * smooth((t - .14) / .72));
  if (anim === 'dead') { a = b = 9; }
  const c = Math.cos(angle), s = Math.sin(angle);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    let xx = W / 2 + c * (x - W / 2) + s * (y - H + 12), yy = H - 12 + (-s * (x - W / 2) + c * (y - H + 12) + lift) / squash;
    if (anim === 'run') {
      const leg = smooth((yy / H - .52) / .4), side = Math.tanh((xx - W / 2) / 16);
      xx -= sin * side * leg * 12;
      yy -= cos * side * leg * 7;
      xx -= Math.sin(phase + yy / H * 2) * smooth((.53 - yy / H) / .3) * 3;
    } else {
      const cloth = smooth((yy / H - .4) / .5), edge = clamp(Math.abs(xx - W / 2) / 60);
      xx -= Math.sin(phase + yy / 35) * cloth * edge * (anim === 'stun' ? 1 : 2.4);
      if (anim === 'stun') xx -= sin * smooth((.44 - yy / H) / .28) * 3;
      if (anim === 'victory') xx -= sin * Math.tanh((xx - W / 2) / 20) * smooth((.5 - yy / H) / .3) * 2.5;
    }
    const row = Math.max(0, Math.min(H - 1, Math.round(yy)));
    const center = mix(sa[row][0], sb[row][0], q), radius = mix(sa[row][1], sb[row][1], q);
    const ax = sa[row][0] + (xx - center) * sa[row][1] / radius;
    const bx = sb[row][0] + (xx - center) * sb[row][1] / radius;
    const va = sample(A, ax, yy), vb = a === b ? va : sample(B, bx, yy);
    const alpha = mix(va[3], vb[3], q), idx = (y * W + x) * 4;
    if (alpha > .1) {
      for (let ch = 0; ch < 3; ch++) out[idx + ch] = Math.round((va[ch] * va[3] * (1 - q) + vb[ch] * vb[3] * q) / alpha);
      out[idx + 3] = Math.round(alpha);
    }
  }
  return out;
}
function svg(body, w, h, color, accent) {
  return `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${w}" height="${h}" viewBox="0 0 320 180" preserveAspectRatio="none"><defs><radialGradient id="halo"><stop stop-color="${accent}" stop-opacity=".8"/><stop offset=".3" stop-color="${color}" stop-opacity=".38"/><stop offset="1" stop-color="${color}" stop-opacity="0"/></radialGradient><linearGradient id="beam"><stop stop-color="${color}" stop-opacity="0"/><stop offset=".5" stop-color="${accent}"/><stop offset="1" stop-color="${color}" stop-opacity="0"/></linearGradient><filter id="glow" x="-100%" y="-100%" width="300%" height="300%"><feGaussianBlur stdDeviation="2" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs>${body}</svg>`;
}
const ellipse = (x, y, rx, ry, stroke, opacity = 1, width = 1) => `<ellipse cx="${f(x)}" cy="${f(y)}" rx="${f(Math.max(0, rx))}" ry="${f(Math.max(0, ry))}" fill="none" stroke="${stroke}" stroke-width="${width}" opacity="${f(opacity)}"/>`;
const img = (png, x, y, w, h, opacity = 1) => `<image x="${f(x)}" y="${f(y)}" width="${f(w)}" height="${f(h)}" opacity="${f(opacity)}" xlink:href="data:image/png;base64,${png.toString('base64')}"/>`;
function lightning(x1, y1, x2, y2, seed, color, opacity) {
  let d = `M${f(x1)} ${f(y1)}`;
  for (let i = 1; i <= 9; i++) { const t = i / 9, off = (rnd(seed + i) - .5) * 17 * Math.sin(t * Math.PI); d += ` L${f(mix(x1, x2, t) + off)} ${f(mix(y1, y2, t) - off)}`; }
  return `<path d="${d}" fill="none" stroke="${color}" stroke-width="1.4" opacity="${f(opacity)}" filter="url(#glow)"/>`;
}
function ring(x, y, r, t, color, opacity) {
  let body = ellipse(x, y, r, r * .52, color, opacity, 1.2) + ellipse(x, y, r * .8, r * .42, color, opacity * .6);
  for (let k = 0; k < 12; k++) { const a = k * Math.PI / 6 + t * .8, xx = x + Math.cos(a) * r, yy = y + Math.sin(a) * r * .52; body += `<path d="M${f(xx - 2)} ${f(yy - 3)} l4 0 -2 5 -2 -5" fill="none" stroke="${color}" opacity="${f(opacity)}"/>`; }
  return body;
}
function petal(x, y, rotation, color, scale = 1) {
  return `<path d="M0 -4 Q5 -5 4 0 Q3 5 0 6 Q-4 2 -3 -1 Z" fill="${color}" transform="translate(${f(x)} ${f(y)}) rotate(${f(rotation)}) scale(${f(scale)})"/>`;
}
function flower(x, y, r, rotation, color) {
  let out = '';
  for (let k = 0; k < 5; k++) out += `<ellipse cx="0" cy="${f(-r / 2)}" rx="${f(r * .3)}" ry="${f(r * .62)}" fill="${color}" opacity=".7" transform="translate(${f(x)} ${f(y)}) rotate(${f(rotation + k * 72)})"/>`;
  return out + `<circle cx="${f(x)}" cy="${f(y)}" r="${f(r * .18)}" fill="#fff4bd"/>`;
}
function fullEffect(hero, poses, number, t) {
  const C = hero.color, A = hero.accent, charge = smooth(t / .36), release = smooth((t - .32) / .24), fade = 1 - smooth((t - .78) / .22);
  const power = Math.sin(Math.PI * t), impact = Math.exp(-Math.pow((t - .57) / .095, 2));
  let out = `<g opacity="${f(fade)}"><ellipse cx="${f(90 + release * 135)}" cy="95" rx="${f(55 + charge * 170)}" ry="${f(40 + charge * 68)}" fill="url(#halo)" opacity="${f(power)}"/>`;
  out += ring(100, 145, charge * 57, t * 3, C, power * .7);
  out += ellipse(218, 130, release * 120, release * 47, A, (1 - release) * .8, 2);
  for (let k = 0; k < 42; k++) {
    const p = fract(rnd(k + 81) + t * .64), a = rnd(k + 4) * Math.PI * 2, rr = 18 + p * 135;
    const x = 180 + Math.cos(a) * rr, y = 105 + Math.sin(a) * rr * .55;
    out += `<circle cx="${f(x)}" cy="${f(y)}" r="${f(.3 + rnd(k + 31) * 1.2)}" fill="${k % 3 ? C : A}" opacity="${f(power * (1 - p))}"/>`;
  }
  switch (hero.sprite) {
    case 'gatotkaca': {
      for (let k = 0; k < 14; k++) {
        const x = 30 + k * 22;
        out += lightning(x, 5, mix(98, x, release), 144, k * 19 + Math.floor(t * 13), k % 2 ? C : A, power * (number === 2 ? .8 : .45));
      }
      if (number === 1) out += img(poses[12].png, mix(35, 192, release), 70 - Math.sin(t * Math.PI) * 140, 70, 94, power);
      else out += img(poses[13].png, 42, 22, 92, 123, power * .42);
      for (let k = 0; k < 3; k++) out += ellipse(225, 146, (release * 115 + k * 12) % 140, 15 + release * 30, C, power * (1 - release) * .7, 2);
      for (let k = 0; k < 9; k++) { const a = k * .69; out += `<path d="M225 143 l${f(Math.cos(a) * release * 90)} ${f(Math.sin(a) * release * 28)}" stroke="${C}" stroke-width="1" opacity="${f(impact)}"/>`; }
      break;
    }
    case 'nyairorokidul': {
      if (number === 1) {
        const x = -90 + release * 320;
        out += `<path d="M${f(x - 65)} 175 V${f(140 - charge * 104)} Q${f(x + 36)} -8 ${f(x + 57)} 48 Q${f(x + 28)} 80 ${f(x - 4)} 61 Q${f(x + 36)} 97 ${f(x + 90)} 110 L${f(x + 120)} 180 Z" fill="${C}" opacity="${f(power * .75)}" stroke="${A}" stroke-width="2"/>`;
        for (let k = 0; k < 5; k++) out += `<path d="M${f(x - 50)} ${60 + k * 21} Q${f(x + 30)} ${30 + k * 20} ${f(x + 66)} ${97 + k * 14}" stroke="${A}" fill="none" opacity="${f(power * .6)}"/>`;
      } else {
        out += `<g opacity="${f(charge * .8)}" stroke="${A}" fill="${C}" fill-opacity=".12" stroke-width="1.2"><path d="M127 151 V65 Q161 18 196 65 V151 M135 150 V67 Q161 30 188 67 V150 M117 151 V71 H128 M196 71 H207 V151 M111 70 L125 47 139 70 M185 70 L200 47 214 70 M140 42 L160 12 181 42 Z"/><path d="M110 151 H212 M115 157 H208 M143 149 V69 Q160 50 179 69 V149"/></g>`;
        for (let k = 0; k < 9; k++) out += ellipse(189, 128 + k * 2, 18 + k * 12 * release, 7 + k * 4, C, power * .6, 1.3);
        for (let k = 0; k < 11; k++) { const a = k * .7 + t * 6, x = 170 + Math.cos(a) * (38 + k * 4), y = 102 + Math.sin(a) * 37; out += `<path d="M${f(x)} ${f(y)} q8 -6 14 0 q-6 6 -14 0 l-5 -5 v10 Z" fill="${k % 2 ? A : C}" opacity="${f(power * .7)}"/>`; }
      }
      for (let k = 0; k < 4; k++) out += `<path d="M0 ${139 + k * 10} Q80 ${f(105 + k * 12 + Math.sin(t * 8 + k) * 8)} 160 ${140 + k * 8} T320 ${141 + k * 9}" fill="none" stroke="${k % 2 ? A : C}" stroke-width="${4 - k * .6}" opacity="${f(power * .65)}"/>`;
      break;
    }
    case 'prabusiliwangi': {
      const tiger = poses[15].png;
      if (number === 1) out += img(tiger, 74, 7, 176, 166, release * power);
      else for (let k = 2; k >= 0; k--) out += img(tiger, -110 + release * 330 - k * 38, 17 + k * 6, 186, 150, power * (1 - k * .25));
      for (let k = 0; k < 5; k++) out += `<path d="M${185 + k * 15} ${f(22 + (1 - release) * 60)} Q${145 + k * 17} 85 ${200 + k * 15} 160" stroke="${k % 2 ? A : C}" stroke-width="3" fill="none" opacity="${f(impact + power * .35)}" filter="url(#glow)"/>`;
      break;
    }
    case 'sipitung': {
      for (let k = 0; k < (number === 2 ? 7 : 3); k++) {
        const x = -68 + fract(t * 1.8 + k / 9) * 374;
        out += img(poses[k % 2 ? 10 : 3].png, x, 45 + k % 3 * 7, 76, 102, power * (k % 3 ? .26 : .65));
        out += `<path d="M${f(x - 22)} ${72 + k * 10} H${f(x + 64)}" stroke="url(#beam)" stroke-width="2" opacity="${f(power)}"/>`;
      }
      for (let k = 0; k < 5; k++) out += lightning(80, 85 + k * 7, 285, 60 + k * 21, k + 18, A, power * .42);
      break;
    }
    case 'lutungkasarung': {
      out += img(poses[15].png, 106, 7, 95, 130, power * .45);
      for (let k = 0; k < 5; k++) {
        const x = 22 + k * 70, sway = Math.sin(t * 4 + k) * 5;
        out += `<path d="M${x} 180 Q${x + 12} 115 ${f(x + sway)} 35 M${x + 3} 103 Q${x - 18} 76 ${x - 28} 62 M${x + 4} 87 Q${x + 27} 50 ${x + 38} 43" fill="none" stroke="${C}" stroke-width="2" opacity="${f(power * .38)}"/>`;
        if (number === 2) out += img(poses[0].png, x - 17, 80, 46, 62, power * .24);
      }
      for (let k = 0; k < 33; k++) {
        const x = fract(rnd(k + 19) + t * .12) * 330, y = fract(rnd(k + 91) + t * .75) * 175;
        out += k % 3 ? petal(x, y, t * 200 + k * 16, k % 2 ? '#ffe7ef' : A, .6) : `<path d="M${f(x)} ${f(y)} q9 -12 13 -3 q-2 8 -13 3" fill="${C}" opacity="${f(power)}"/>`;
      }
      for (const x of [65, 155, 248]) out += ellipse(x, 132, 24 + charge * 5, 37, A, power * .75, 1.5);
      break;
    }
    case 'calonarang': {
      out += `<ellipse cx="158" cy="95" rx="${f(charge * 64)}" ry="${f(charge * 71)}" fill="#0d0018" opacity="${f(power * .72)}" stroke="${C}" stroke-width="2"/>`;
      out += ring(159, 105, charge * 77, t * 5, C, power);
      if (number === 2) for (let k = 0; k < 5; k++) { const a = k * 1.256 + t * 4; out += img(poses[15].png, 145 + Math.cos(a) * release * 100 - 26, 65 + Math.sin(a) * 43, 53, 69, power * .8); }
      for (let k = 0; k < 13; k++) { const x = k * 26, y = 145 + Math.sin(k + t * 7) * 14; out += `<path d="M${x} 172 Q${x - 12} ${f(y)} ${x + 4} ${f(y - 25)} Q${x + 3} ${f(y)} ${x + 14} ${f(y - 5)} Q${x + 26} ${f(y + 4)} ${x + 19} 172 Z" fill="${C}" opacity="${f(power * .55)}" filter="url(#glow)"/>`; }
      break;
    }
    case 'kenarok': {
      const kx = 155;
      out += `<g transform="translate(${kx} 70) rotate(${f(-15 + t * 30)})" opacity="${f(power)}" fill="${C}" stroke="${A}" filter="url(#glow)"><path d="M-4 -64 Q12 -48 2 -38 Q-13 -29 1 -18 Q13 -7 3 3 L-2 29 -7 5 Q1 -6 -8 -17 Q-18 -29 -5 -40 Q2 -48 -4 -64 Z"/><path d="M-20 30 Q0 23 20 30 L15 34 H-15 Z M-4 34 V51 H4 V34"/></g>`;
      for (const x of [76, 146, 218, 287]) {
        out += ring(x, 112, 13 + charge * 9, t * 4, C, power);
        out += `<path d="M${x} 73 V132 M${x - 20} 95 L${x + 20} 120 M${x + 20} 95 L${x - 20} 120" stroke="${C}" opacity="${f(power * .8)}"/>`;
        out += lightning(155, 70, x, 115, x + Math.floor(t * 8), C, release * power);
      }
      for (let k = 0; k < 4; k++) out += `<path d="M0 ${80 + k * 20} Q100 ${f(30 + k * 30 + t * 30)} 180 ${90 + k * 10} T320 ${100 + k * 12}" stroke="${C}" opacity="${f(power * .34)}" stroke-width="3" fill="none"/>`;
      break;
    }
    case 'roromendut': {
      for (const x of [52, 124, 204, 280]) out += flower(x, 126, charge * (number === 2 ? 27 : 17), t * 40, C);
      for (let k = 0; k < 60; k++) { const x = fract(rnd(k + 15) + t * .32) * 340 - 10, y = fract(rnd(k + 46) + t * .48) * 180; out += `<g opacity="${f(power * .78)}">${petal(x, y, t * 290 + k * 31, k % 4 ? C : A, .5 + rnd(k + 3) * .5)}</g>`; }
      for (let k = 0; k < 4; k++) out += `<path d="M300 ${70 + k * 20} Q200 ${f(25 + t * 35 + k * 18)} 45 ${110 + k * 10}" fill="none" stroke="url(#beam)" stroke-width="3" opacity="${f(release * power)}"/>`;
      break;
    }
    case 'damarwulan': {
      if (number === 2) for (const x of [40, 135, 230]) out += img(poses[15].png, x, 12, 68, 150, power * .82);
      let rays = '';
      for (let k = 0; k < 8; k++) { const a = k * Math.PI / 4; rays += `<path d="M${f(160 + Math.cos(a) * 19)} ${f(72 + Math.sin(a) * 19)} L${f(160 + Math.cos(a) * 35)} ${f(72 + Math.sin(a) * 35)}"/>`; }
      out += `<g stroke="${A}" stroke-width="2" opacity="${f(power)}">${ellipse(160, 72, 18 + impact * 7, 18 + impact * 7, C)}${rays}</g>`;
      for (const x of [62, 158, 254]) out += `<path d="M${x - 20} 180 L${x - 6} 10 H${x + 6} L${x + 20} 180 Z" fill="url(#halo)" opacity="${f(power * .58)}"/>`;
      break;
    }
    case 'jakatarub': {
      const colors = ['#ff7878', '#ffa955', '#ffe577', '#7df2aa', '#73dfff', '#9ba8ff', '#d69bff'];
      for (let k = 0; k < 7; k++) {
        const y = 24 + k * 19, bend = Math.sin(t * 7 + k * .8) * 32;
        out += `<path d="M-15 ${y} C70 ${f(y + bend)} 138 ${f(150 - y + bend)} 210 ${f(80 + bend)} S280 ${f(y - bend)} 335 ${y + 8}" fill="none" stroke="${colors[k]}" stroke-width="${number === 2 ? 5 : 2.5}" opacity="${f(power * .65)}" filter="url(#glow)"/>`;
        out += `<path d="M-15 ${y - 1} C70 ${f(y + bend - 1)} 138 ${f(150 - y + bend - 1)} 210 ${f(79 + bend)} S280 ${f(y - bend - 1)} 335 ${y + 7}" fill="none" stroke="${A}" stroke-width=".8" opacity="${f(power * .6)}"/>`;
      }
      for (let k = 0; k < 24; k++) {
        const x = fract(rnd(k + 6) + t * .8) * 340, y = fract(rnd(k + 65) + t * .7) * 160;
        out += `<g transform="translate(${f(x)} ${f(y)}) rotate(-35)" stroke="${A}" opacity="${f(power)}"><path d="M0 -10 V10 M0 -8 Q-6 -4 0 1 Q6 -3 0 -8 M0 0 Q-7 4 0 9 Q7 4 0 0" fill="${C}" stroke-width=".7"/></g>`;
      }
      break;
    }
  }
  out += `<ellipse cx="220" cy="108" rx="${f(impact * 103)}" ry="${f(impact * 78)}" fill="url(#halo)" opacity="${f(impact * .6)}"/></g>`;
  return svg(out, 320, 180, C, A);
}
function smallEffect(hero, anim, t) {
  const C = hero.color, A = hero.accent, p = Math.sin(Math.PI * t);
  let out = '';
  if (anim === 'stun' || anim === 'confused') {
    for (let k = 0; k < 3; k++) { const a = t * Math.PI * 2 + k * 2.094, x = 160 + Math.cos(a) * 33, y = 27 + Math.sin(a) * 6; out += `<path d="M${f(x)} ${f(y - 4)} l1.5 3 3.5 0 -2.5 2.3 1 3.4 -3.5 -1.8 -3 1.8 1 -3.4 -2.5 -2.3 3.5 0 Z" fill="${anim === 'stun' ? '#ffe082' : '#cab2ff'}"/>`; }
  }
  if (anim === 'defend') out += ellipse(164, 99, 57, 70, C, p * .6, 1.3);
  if (anim === 'attack') out += `<path d="M185 46 Q260 73 217 111" stroke="${A}" fill="none" stroke-width="3" opacity="${f(p * .65)}" filter="url(#glow)"/>`;
  if (anim === 'skill1' || anim === 'skill2' || anim.startsWith('ultima')) {
    out += ring(160, 165, 50 * p, t * 5, C, p * .75);
    for (let k = 0; k < 15; k++) { const a = k * 2.4 + t * 4; out += `<circle cx="${f(164 + Math.cos(a) * (35 + p * 48))}" cy="${f(99 + Math.sin(a) * 55)}" r="1.2" fill="${k % 2 ? C : A}" opacity="${f(p * .65)}"/>`; }
    if (hero.sprite === 'gatotkaca') for (let k = 0; k < 4; k++) out += lightning(183, 84, 221 + k * 8, 61 + k * 19, k + Math.floor(t * 10), C, p * .7);
    if (hero.sprite === 'nyairorokidul') for (let k = 0; k < 3; k++) out += `<path d="M178 ${79 + k * 10} Q220 ${f(49 + k * 10 + t * 12)} 268 ${91 + k * 10}" stroke="${C}" stroke-width="2" fill="none" opacity="${f(p)}"/>`;
    if (hero.sprite === 'roromendut' || hero.sprite === 'lutungkasarung') for (let k = 0; k < 8; k++) out += `<g opacity="${f(p)}">${petal(150 + Math.cos(k + t * 4) * 74, 91 + Math.sin(k + t * 4) * 49, k * 40 + t * 120, C, .8)}</g>`;
    if (hero.sprite === 'calonarang' || hero.sprite === 'kenarok') out += `<circle cx="220" cy="82" r="${f(p * 19)}" fill="url(#halo)"/><path d="M205 83 Q221 ${f(48 + t * 13)} 235 83" stroke="${C}" fill="none" opacity="${f(p)}"/>`;
    if (hero.sprite === 'prabusiliwangi' || hero.sprite === 'sipitung') for (let k = 0; k < 3; k++) out += `<path d="M${195 + k * 12} 54 Q${230 + k * 7} 78 ${205 + k * 12} 118" stroke="${A}" stroke-width="2" fill="none" opacity="${f(p)}"/>`;
    if (hero.sprite === 'damarwulan') out += `<path d="M179 84 H269 l-9 -5 m9 5 -9 5" stroke="${C}" fill="none" opacity="${f(p)}" filter="url(#glow)"/>`;
    if (hero.sprite === 'jakatarub') out += `<path d="M130 80 Q180 ${f(32 + t * 20)} 240 91 T265 130" stroke="${C}" stroke-width="3" fill="none" opacity="${f(p)}"/>`;
  }
  return svg(out, W, H, C, A);
}
async function atlas(frames, w, h, columns, rows, destination) {
  await sharp({ create: { width: w * columns, height: h * rows, channels: 4, background: '#00000000' } })
    .composite(frames.map((input, k) => ({ input, left: k % columns * w, top: Math.floor(k / columns) * h })))
    .png({ compressionLevel: 9, adaptiveFiltering: true }).toFile(destination);
}
await fs.mkdir(root, { recursive: true });
const files = await fs.readdir(source);
let bytes = 0;
for (const hero of heroes) {
  if (selectedSprite && hero.sprite !== selectedSprite) continue;
  const file = files.find(n => n.startsWith(hero.sprite + '-keyposes_') && n.endsWith('.png'));
  if (!file) throw new Error('Missing generated source for ' + hero.sprite);
  const dir = path.join(root, hero.sprite);
  await fs.mkdir(path.join(dir, 'effects'), { recursive: true });
  const poses = await prepare(hero, path.join(source, file), dir);
  for (const pose of poses) pose.silhouette = silhouette(pose.data);
  const animations = effectsOnly ? JSON.parse(await fs.readFile(path.join(dir, 'manifest.json'), 'utf8')).animations : {};
  for (const [anim, [count, columns, rows, frameMs, loop]] of Object.entries(effectsOnly ? {} : specs)) {
    const frames = [], hashes = [];
    for (let k = 0; k < count; k++) {
      const t = loop ? k / count : k / (count - 1);
      const data = tween(hero, poses, anim, t);
      const frame = await sharp(data, { raw: { width: W, height: H, channels: 4 } })
        .composite([{ input: Buffer.from(smallEffect(hero, anim, t)) }]).png().toBuffer();
      frames.push(frame); hashes.push(crypto.createHash('sha256').update(frame).digest('hex'));
    }
    await atlas(frames, W, H, columns, rows, path.join(dir, anim + '.png'));
    animations[anim] = { file: anim + '.png', frameCount: count, columns, rows, frameWidth: W, frameHeight: H, frameMs, loop, holdLast: anim === 'dead', uniqueFrames: new Set(hashes).size };
    console.log(hero.sprite, anim, count, 'frames;', animations[anim].uniqueFrames, 'unique');
  }
  const effects = {};
  for (let n = 1; n <= 2; n++) {
    const frames = [];
    for (let k = 0; k < 44; k++) frames.push(await sharp(Buffer.from(fullEffect(hero, poses, n, k / 43))).png().toBuffer());
    await atlas(frames, 320, 180, 4, 11, path.join(dir, 'effects', 'ultima' + n + '.png'));
    effects['ultima' + n] = { file: 'effects/ultima' + n + '.png', frameCount: 44, columns: 4, rows: 11, frameWidth: 320, frameHeight: 180, frameMs: 50, fullscreen: true };
  }
  const manifest = { version: 1, id: hero.id, name: hero.name, sprite: hero.sprite, facing: 'right', alpha: 'straight', order: 'row-major', pivot: { x: .5, y: 1 }, totalCharacterFrames: 236, totalEffectFrames: 88,
    production: 'AI-generated keyposes, silhouette-morph inbetweens, mesh-deformed secondary motion, procedural magical VFX. Not 236 independently hand-drawn poses.',
    generationBrief: { useCase: '2D Android fantasy RPG production sprite keypose atlas', subject: hero.design, style: 'Detailed richly shaded hand-painted premium fantasy sprites; realistic heroic adult proportions; identical face/costume each pose.', backdrop: hero.chroma === 'green' ? '#00FF00' : '#FF00FF', composition: '1024x1536, 4x4 equal cells; sixteen full-body poses facing right; idle, run, attack, guard, hurt, stun, confused, victory, dead, skills and ultimate keyposes; no text.' },
    animations, effects, abilities: hero.skills.map(([name, description], i) => ({ animation: ['skill1', 'skill2', 'ultima1', 'ultima2'][i], name, description, mechanicsSource: 'Requested design specification; battle mechanics remain defined by SkillFactory and the turn-based engine.' })) };
  await fs.writeFile(path.join(dir, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
  for (const entry of await fs.readdir(dir, { withFileTypes: true })) if (entry.isFile()) bytes += (await fs.stat(path.join(dir, entry.name))).size;
}
await fs.writeFile(path.join(root, 'catalog.json'), JSON.stringify({ version: 1, characterFrames: 2360, ultimateEffectFrames: 880, heroes: heroes.map(h => ({ id: h.id, sprite: h.sprite, name: h.name, color: h.color, portrait: `${h.sprite}/${h.sprite}.png`, manifest: `${h.sprite}/manifest.json` })) }, null, 2) + '\n');
console.log('Character assets complete:', (bytes / 1024 / 1024).toFixed(1), 'MiB excluding effects');
