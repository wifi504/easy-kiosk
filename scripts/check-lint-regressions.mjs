import fs from 'node:fs';
import path from 'node:path';

const [baselineRoot, currentRoot, baselineFile, currentFile] = process.argv.slice(2);
if (!currentFile) throw new Error('Missing lint report arguments');
function errors(file, root) {
  return JSON.parse(fs.readFileSync(file, 'utf8')).flatMap(result =>
    result.messages.filter(message => message.severity === 2).map(message => {
      const relative = path.relative(root, result.filePath).split(path.sep).join('/');
      // Hook diagnostics embed line numbers in prose. Moving code is not a new defect.
      const text = message.message.replace(/\bline \d+\b/g, 'line #');
      return { key: JSON.stringify([relative, message.ruleId, text]),
        location: `${relative}:${message.line}:${message.column}`, text };
    }),
  );
}
const baseline = errors(baselineFile, baselineRoot);
const current = errors(currentFile, currentRoot);
const remaining = new Map();
for (const item of baseline) remaining.set(item.key, (remaining.get(item.key) || 0) + 1);
const added = current.filter(item => {
  const count = remaining.get(item.key) || 0;
  if (!count) return true;
  remaining.set(item.key, count - 1);
  return false;
});
console.log(`ESLint: main baseline ${baseline.length} errors; dev ${current.length}; newly introduced ${added.length}.`);
for (const item of added) console.error(`${item.location} ${item.text}`);
if (process.env.GITHUB_STEP_SUMMARY) fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY,
  `### ESLint regression check\nmain: ${baseline.length} existing errors; dev: ${current.length}; new: ${added.length}.\n`);
if (added.length) process.exitCode = 1;
