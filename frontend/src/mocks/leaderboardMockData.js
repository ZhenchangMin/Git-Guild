const adventurers = {
  shadowblade: { userId: -1001, name: 'shadowblade', level: 13, title: '公会大师' },
  novaLin: { userId: -1002, name: 'NovaLin', level: 10, title: '公会精英' },
  byteweaver: { userId: -1003, name: 'byteweaver', level: 9, title: '公会精英' },
  miraChen: { userId: -1004, name: 'MiraChen', level: 8, title: '公会精英' },
  cloudMason: { userId: -1005, name: 'cloud_mason', level: 7, title: '代码工匠' },
  yukiDev: { userId: -1006, name: 'YukiDev', level: 6, title: '代码工匠' },
  atlasCode: { userId: -1007, name: 'AtlasCode', level: 5, title: '代码工匠' },
  minAdven: { userId: -1008, name: 'min_adven', level: 4, title: '前端协作学徒' },
  boven: { userId: -1009, name: 'Boven', level: 3, title: '前端协作学徒' },
  branchSage: { userId: -1010, name: 'branch_sage', level: 2, title: '协作学徒' },
}

const additionalNames = [
  'CommitRanger',
  'HazelQi',
  'merge_mage',
  'LeoStack',
  'RuneCoder',
  'orbit_dev',
  'SageXu',
  'emberbyte',
  'KaiForge',
  'pixel_smith',
  'LunaBranch',
  'dev_nori',
  'async_lee',
  'DeltaCoder',
  'patchworks',
  'CedarLin',
  'stack_rover',
  'clean_commit',
  'route_keeper',
  'syntax_sage',
  'code_nomad',
  'dev_mallow',
  'arch_maker',
  'refactor_ray',
  'origin_zero',
  'cherry_pick',
  'build_runner',
  'test_keeper',
  'scope_guard',
  'quartz_dev',
  'lint_master',
  'api_walker',
  'state_maker',
  'loop_smith',
  'type_keeper',
  'cache_mason',
  'query_crafter',
  'ui_weaver',
  'task_runner',
  'dev_harbor',
]

function titleForLevel(level) {
  if (level >= 12) return '公会大师'
  if (level >= 8) return '公会精英'
  if (level >= 5) return '代码工匠'
  if (level >= 3) return '前端协作学徒'
  if (level >= 2) return '协作学徒'
  return '见习冒险者'
}

const additionalAdventurers = additionalNames.map((name, index) => {
  const level = Math.max(2, 8 - Math.floor(index / 6))
  return {
    userId: -1011 - index,
    name,
    level,
    title: titleForLevel(level),
  }
})

const topLeaderboardByPeriod = {
  WEEKLY: [
    [adventurers.novaLin, 14],
    [adventurers.shadowblade, 12],
    [adventurers.miraChen, 10],
    [adventurers.byteweaver, 9],
    [adventurers.cloudMason, 8],
    [adventurers.yukiDev, 7],
    [adventurers.atlasCode, 6],
    [adventurers.branchSage, 5],
    [adventurers.minAdven, 4],
    [adventurers.boven, 3],
  ],
  MONTHLY: [
    [adventurers.shadowblade, 46],
    [adventurers.byteweaver, 41],
    [adventurers.novaLin, 38],
    [adventurers.cloudMason, 34],
    [adventurers.miraChen, 31],
    [adventurers.yukiDev, 27],
    [adventurers.atlasCode, 23],
    [adventurers.minAdven, 18],
    [adventurers.boven, 15],
    [adventurers.branchSage, 12],
  ],
  ALL_TIME: [
    [adventurers.shadowblade, 186],
    [adventurers.novaLin, 164],
    [adventurers.byteweaver, 153],
    [adventurers.miraChen, 139],
    [adventurers.cloudMason, 124],
    [adventurers.yukiDev, 108],
    [adventurers.atlasCode, 92],
    [adventurers.minAdven, 71],
    [adventurers.boven, 59],
    [adventurers.branchSage, 47],
  ],
}

const completedQuestFactories = {
  WEEKLY: (index) => Math.max(1, 3 - Math.floor(index / 14)),
  MONTHLY: (index) => Math.max(2, 11 - Math.floor(index / 4)),
  ALL_TIME: (index) => 45 - index,
}

export function getMockLeaderboard(period) {
  const normalizedPeriod = topLeaderboardByPeriod[period] ? period : 'ALL_TIME'
  const extraRows = additionalAdventurers.map((adventurer, index) => [
    adventurer,
    completedQuestFactories[normalizedPeriod](index),
  ])
  const rows = [...topLeaderboardByPeriod[normalizedPeriod], ...extraRows]
  return rows.map(([adventurer, completed], index) => ({
    ...adventurer,
    rank: index + 1,
    completed,
  }))
}
