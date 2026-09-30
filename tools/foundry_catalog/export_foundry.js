// Dumps the compendiums the character catalog is built from and sends them to receiver.py.
//
// Run it in the Gamemaster's Foundry client: paste it into the browser console (F12), or run it
// through the drawbridge MCP (foundry_execute). Start `python receiver.py` first; the files land in
// ./export next to it. Then run `python convert.py`.

const RECEIVER = 'http://127.0.0.1:8766/save';

// Full documents: classes, subclasses, features, options, feats, species and backgrounds.
const DOCUMENT_PACKS = [
  'ag-fifthpendium.classes',
  'ag-fifthpendium.feats',
  'ag-fifthpendium.origins',
  'ag-fifthpendium-dndbeyond.options',
  'ag-fifthpendium-eberron.options',
  'ag-fifthpendium-forgotten-realms.options',
  'ag-fifthpendium-ravenloft.options',
  // English SRD 5.2: the source of English names and texts for the documents both share.
  'dnd5e.classes24',
  'dnd5e.feats24',
  'dnd5e.origins24'
];

// Only an index (names, levels): spells and equipment the advancements point at.
const INDEX_PACKS = ['ag-fifthpendium.spells', 'dnd5e.spells24', 'ag-fifthpendium.equipment', 'dnd5e.equipment24'];

async function send(name, payload) {
  const response = await fetch(`${RECEIVER}?name=${encodeURIComponent(name)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain' },
    body: JSON.stringify(payload)
  });
  return response.status;
}

const report = [];
const embedTargets = new Set();
for (const id of DOCUMENT_PACKS) {
  const pack = game.packs.get(id);
  if (!pack) { report.push({ id, missing: true }); continue; }
  const docs = await pack.getDocuments();
  for (const doc of docs) {
    const html = doc.system?.description?.value ?? '';
    for (const match of html.matchAll(/@[Ee]mbed\[([^\]\s]+)/g)) embedTargets.add(match[1]);
  }
  const status = await send(`${id}.json`, {
    pack: id,
    label: pack.metadata.label,
    package: pack.metadata.packageName,
    packageVersion: game.modules.get(pack.metadata.packageName)?.version ?? game.system.version,
    exportedAt: new Date().toISOString(),
    documents: docs.map(d => ({ uuid: d.uuid, ...d.toObject() }))
  });
  report.push({ id, count: docs.length, status });
}

for (const id of INDEX_PACKS) {
  const pack = game.packs.get(id);
  if (!pack) { report.push({ id, missing: true }); continue; }
  const index = await pack.getIndex({ fields: ['system.level', 'system.school', 'system.identifier', 'system.type', 'system.source.book'] });
  const entries = index.contents.map(e => ({
    uuid: e.uuid, name: e.name, type: e.type, level: e.system?.level ?? null, school: e.system?.school ?? null,
    identifier: e.system?.identifier ?? null, book: e.system?.source?.book ?? null
  }));
  report.push({ id, count: entries.length, status: await send(`index.${id}.json`, { pack: id, index: entries }) });
}

// Tables, book pages and items shown inline in descriptions (@Embed).
const embeds = {};
for (const uuid of embedTargets) {
  const doc = await fromUuid(uuid);
  if (!doc) { embeds[uuid] = null; continue; }
  const entry = { documentName: doc.documentName, name: doc.name, type: doc.type ?? null };
  if (doc.documentName === 'Item') entry.html = doc.system?.description?.value ?? '';
  else if (doc.documentName === 'JournalEntryPage') entry.html = doc.text?.content ?? '';
  else if (doc.documentName === 'JournalEntry') entry.html = doc.pages.contents.map(p => p.text?.content ?? '').join('\n');
  else if (doc.documentName === 'RollTable') {
    entry.table = {
      formula: doc.formula,
      description: doc.description,
      results: doc.results.contents.map(r => ({ range: r.range, text: r.description || r.name || r.text || '' }))
    };
  }
  embeds[uuid] = entry;
}
report.push({ id: 'embeds', count: embedTargets.size, status: await send('embeds.json', embeds) });
return report;
