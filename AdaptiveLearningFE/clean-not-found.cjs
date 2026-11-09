const fs = require('fs');

const notFound = [
  'analyticsSummary',
  'analyticsKpiPills',
  'analyticsMaxPassRate',
  'analyticsTrendMax',
  'analyticsTrendPoints',
  'accountExamFilter',
  'accountRoleCounts',
  'paymentStatusCounts',
  'attemptExamFilter',
  'attemptSummary',
  'filteredAttempts',
  'paginatedAttempts',
  'examStatusCounts',
  'knowledgeUploading',
  'knowledgeSelectedCertificateId',
  'knowledgeSelectedTopicId',
  'filteredKnowledgeTopics',
  'knowledgeCount',
  'knowledgeTotalChunks'
];

// 1. Remove from Home.vue
let home = fs.readFileSync('AdaptiveLearningFE/src/features/Admin/pages/Home.vue', 'utf8');
notFound.forEach(nf => {
  const reg = new RegExp('^\\s*' + nf + ',?\\s*$', 'm');
  home = home.replace(reg, '');
});
fs.writeFileSync('AdaptiveLearningFE/src/features/Admin/pages/Home.vue', home, 'utf8');
console.log('Cleaned Home.vue adminContext');

// 2. Remove from views
const files = fs.readdirSync('AdaptiveLearningFE/src/features/Admin/components/views');
files.forEach(f => {
  const p = 'AdaptiveLearningFE/src/features/Admin/components/views/' + f;
  let content = fs.readFileSync(p, 'utf8');
  notFound.forEach(nf => {
    const reg = new RegExp('^\\s*' + nf + ',?\\s*$', 'm');
    content = content.replace(reg, '');
  });
  // Clean potential trailing comma or empty lines in destruct
  content = content.replace(/,\s*,/g, ',');
  content = content.replace(/,\s*\}/g, '\n}');
  fs.writeFileSync(p, content, 'utf8');
  console.log('Cleaned in', f);
});

