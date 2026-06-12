<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import hallImg from '../../assets/hall.webp'
import HallEntryTransition from '../../components/HallEntryTransition.vue'
import NotificationBell from '../../components/NotificationBell.vue'
import { questApi } from '../../api/questApi'
import { clearSession, hasLoginSession, sessionStore } from '../../stores/sessionStore'

const router = useRouter()

// 仅登录且非访客时展示通知中心——访客没有可投递的关键状态变化。
const showNotificationBell = computed(() => hasLoginSession() && sessionStore.role !== 'VISITOR')

// 真实可接取委托数（PUBLISHED）；接口不可用时回退为 0，不再用 mock。
const openQuestCount = ref(0)
const hallViewport = ref(null)
const hallTrack = ref(null)
const hallOffset = ref(0)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartOffset = ref(0)
const activeHotspotId = ref('')
const shapeTooltipPosition = ref({ x: 0, y: 0, below: false })

const HALL_IMAGE_WIDTH = 6000
const HALL_IMAGE_HEIGHT = 1800
const HALL_ENTRY_EXIT_MS = 620
const HALL_ENTRY_FROM_GATE_KEY = 'gitguild.hallEntryFromGate'

const hallImageReady = ref(false)
const showHallEntry = ref(true)
const isHallEntryLeaving = ref(false)
let hallEntryExitTimer = 0
const hallEntryStatusText = ref('正在点亮公会大厅...')
const shouldFadeHallEntry = ref(false)

const baseRooms = [
  {
    id: 'submission',
    label: '提交柜台',
    hint: '登记成果并提交审核',
    routeName: 'submission-counter',
    left: 9.2,
    top: 22,
    width: 11.8,
    height: 40,
    shape: {
      x: 526,
      y: 109,
      width: 917.5,
      height: 1624.5,
      viewBoxWidth: 920,
      viewBoxHeight: 1627,
      path: 'M490.5 1492C396.5 1528.83 192.1 1607.1 126.5 1625.5L116.5 1619.5V1167V1129.5L107.5 1120V1062.5L1 1042V1006.5L32.5 996.5V190.5V159.5L174.5 190.5L203 177L189.5 159.5L174.5 133L184 103.5L203 86V64L215 57L245 42L253.5 47L269 57H282.5L289 42L298.5 30.5H313H324L332 47L341.5 30.5L356 24L379 13L399 7.5H423L431 1H441L446.5 17.5L462 30.5L474 47L502.5 64L495 75L502.5 86V104.5H515.5L521.5 112.5V123.5H530H541L546.5 138.5V150H554H565L579.5 162V184L586.5 212.5H594L604 221.5L614 232L632.5 242V314L707.5 335V371L702 388.5L697 738.5H749.5L752 741L768 870.5L771 877L777.5 867.5L781.5 857V843V836.5L777.5 828.5L788.5 816.5L797.5 808.5V795.5L800.5 789L803.5 785.5L811 792V798.5V808.5L816 813L825.5 816.5L828.5 821.5V828.5L825.5 832.5V836.5L828.5 857L833.5 870.5L838.5 877H866.5L885.5 880L897.5 883.5L909.5 890.5L918.5 898V943.5L903.5 961.5V976.5V980L901 1175L906.5 1182.5L903.5 1205L906.5 1211V1223.5V1232.5L903.5 1253.5L897 1268L881.5 1289L855.5 1315.5L822.5 1341L788.5 1362L733.5 1391L566.5 1465.5V1471L499 1497L490.5 1492Z',
    },
  },
  {
    id: 'quest',
    label: '悬赏任务板',
    hint: '浏览可接取的 Issue 关联任务',
    routeName: 'quest-board',
    left: 23.6,
    top: 16,
    width: 13.8,
    height: 45,
    shape: {
      x: 1293,
      y: 195,
      width: 756.5,
      height: 835,
      viewBoxWidth: 759,
      viewBoxHeight: 838,
      path: 'M35.5 271V700.5L44 706V722.5L49.5 727L59 730L61.5 735.5V742.5L59 747.5L62.5 771.5L67 784.5L72 790.5H100.5L131 797L142.5 803.5L152 812V836L697 787L726 783L723.5 537.5L726 534.5V529.5L723.5 526V462V354.5L727 350V279.5V274.5L730 272V261.5L732.5 255.5L742.5 241.5H751V236.5L752.5 234.5L757.5 229V223.5L753.5 219.5H739.5L737.5 216.5H730L727 212H721L717.5 214V218H713V212L708 210L693.5 207L684 204.5L673 202.5L659 199.5H650.5V195L653.5 193L655.5 187.5V182L652 177L646 173.5H630.5L624.5 171L618.5 168H614V162.5L617 158.5V150.5L614 146L608 142H601.5L598 146L594 142L585 132.5L581.5 125L575.5 120L572 123H567.5L564.5 118L559.5 116.5L554.5 120L550.5 125L548.5 118L541 106L534 95L525 80L517.5 70.5L509.5 61L495.5 47L489 42L480 34.5L468.5 26.5L459.5 21L452 18.5L443.5 15.5L439 10V1H435V10L433.5 12L409 10H399H384L372.5 12L362 15.5L347 21L337.5 26L322 36L311 44L303 52L300.5 56.5H296L287 46L276 42.5H266.5L258 49H253L248 54L253 61V65.5L249.5 67L204 49H195L186.5 54L181 59L175 67L31 27L16.5 35H12.5L1 43.5V67L12.5 80L18 82L21.5 89L23.5 96.5V115.5L34 129.5V134V233.5H29.5L23.5 240V264H27.5L29.5 266L35.5 271Z',
    },
  },
  {
    id: 'desk',
    label: '前台向导',
    hint: '新手引导、仓库接入和异常求助',
    routeName: 'repository-sync',
    left: 43.8,
    top: 37,
    width: 16.2,
    height: 50,
    shape: {
      x: 2441.5,
      y: 331.5,
      width: 1391.5,
      height: 1082.5,
      viewBoxWidth: 1394,
      viewBoxHeight: 1085,
      path: 'M522.5 1083.79L377.5 1074.79H373.5L366 1078.29L307.5 1074.79L286.5 1072.29V1064.79L237.5 1057.79L204.5 1052.79L180.5 1047.79L157 1041.79L135.5 1036.79L117.5 1032.29L99.5 1026.79L83 1020.79L66 1014.29L52 1006.79L38 997.794L25.5 989.294L15.5 975.794L11.5 967.294V938.294L20 928.794V810.794L17.5 808.794V788.794L11.5 785.294L4.5 782.794L1 777.794V751.294L4.5 748.794L14 741.794L27.5 737.794L39 734.794L51.5 732.794H63.5L65 722.794L72 724.794L69.5 718.794V711.794H72V706.794L65 701.294L67 696.794L72 694.294L77.5 696.794V639.794L72 633.294V628.294L73.5 624.294H79.5V600.294L72 592.294V576.294L69.5 570.794L77.5 565.794L83 567.794H89L91.5 570.794L95.5 573.794L99.5 572.294L104 576.294L134 578.294V404.294L130 399.794V383.794V261.794L128 256.294V241.294L126.5 236.294H122.5L120 232.794V225.794L117.5 223.294V213.794H178.5V198.294L183.5 193.294L186.5 183.794V177.794L195 166.294L197.5 158.794L208 152.294L212 144.794L222 137.794H243H252H260.5L267.5 140.794L274 135.294H282.5L286 140.794L274 148.294L282.5 155.794V148.294L296.5 144.794L301 150.294L290.5 155.794L298.5 158.794H312.5L310.5 166.294H301L298.5 172.794H310.5L321 183.794L316.5 187.294L306 177.794H298.5L308 193.294V198.294V216.294H328L337.5 228.794V19.7939V12.7939L347.5 7.79395L352 1.79395H359L365 7.79395H863L872.5 1.29395L879 7.79395H883.5L891 12.2939V227.794L901 214.794L910 206.294L905 191.794L917.5 177.794L913 173.294L903 175.294L913 164.294L922 161.294L932.5 164.294L934.5 158.794L926 152.294L917.5 150.294L926 142.294L937 146.294L941.5 154.294L946 148.294L953.5 146.294L946 136.794L966.5 142.294L981 139.294L986 132.794L995 129.294L1000.5 136.794L1014 146.294L1007 158.794L1026.5 154.294L1038 158.794L1046 150.294L1048 139.294L1063 148.294L1082 150.294L1077.5 212.794H1115.5V220.794L1118.5 227.794C1117.17 229.461 1114.3 233.094 1113.5 234.294C1112.7 235.494 1106.83 234.794 1104 234.294L1105.5 238.794L1100.5 245.294V255.294L1095.5 263.294V345.294L1098 533.794V620.794L1161.5 623.794L1166.5 627.294L1181.5 630.794L1201 632.794V603.794L1208.5 595.794H1200L1191.5 597.794L1185 601.294L1178 605.294L1169.5 614.294H1164.5V610.794C1165.5 609.627 1167.5 606.894 1167.5 605.294C1167.5 603.694 1166.83 601.961 1166.5 601.294L1161.5 598.794L1153.5 603.794L1145 607.294V603.794L1150 594.794L1155.5 588.294L1164.5 583.294L1175 578.294L1181 575.794L1185 579.294H1193L1200 583.294H1207.5L1205.5 579.294L1202.5 574.294L1207.5 571.294L1210.5 574.294L1217 579.294L1219 575.794L1222 570.294V566.794L1210.5 564.794L1195 562.794L1189.5 556.294L1181 550.294H1169.5L1167.5 544.794L1175 540.294H1202.5L1212.5 544.794L1220 552.794L1226 559.794L1224.5 552.794C1224.67 551.127 1225.2 547.494 1226 546.294C1227 544.794 1228.5 540.294 1230 538.794C1231.2 537.594 1234.17 538.294 1235.5 538.794L1234 530.294L1239 525.294L1245.5 522.794L1252.5 516.794L1260 514.794L1268 516.794L1266.5 522.794L1257 525.294L1253.5 533.794L1249.5 543.294L1242 551.294L1239 557.794V564.794L1245.5 562.794L1249.5 557.794L1255 556.294H1259L1262 551.294L1264.5 547.794L1260 543.294L1259 538.794L1262 533.794H1266.5L1276.5 538.794L1282.5 541.794L1288 546.294L1293 550.294L1294.5 546.294L1295.5 540.294L1299.5 535.794L1308 529.294H1319L1322 533.794L1319 538.794H1313.5L1315 544.794L1316.5 557.794H1330H1339L1343 552.794H1353L1360.5 556.294V562.794L1356.5 564.794L1351 562.794L1335.5 564.794L1333 569.294V583.794H1348.5L1360.5 578.294L1371.5 583.794L1382 591.794L1389.5 601.794L1392.5 610.294L1387.5 611.794L1378 606.794L1369 601.794L1360.5 602.794L1354.5 599.794L1343 591.794L1339 596.294L1343 601.794V607.794L1331.5 614.294L1330 623.294L1340.5 618.294H1351L1360.5 625.794L1371.5 638.294V647.794H1369L1353 638.294L1343 632.794L1335.5 634.794L1331.5 647.794L1346 655.294L1353 666.794L1356.5 677.794V693.294L1353 697.794L1333 673.294L1328 665.294H1323L1319 669.294L1316.5 670.794L1320.5 673.294L1325 680.794V690.794V702.794L1323 712.294L1319 722.794L1313 731.294H1333L1346 733.294L1354.5 736.794L1366 740.294L1375 744.294L1383 748.794V774.294L1377 777.794L1370 785.794V807.294H1366L1357.5 817.794L1361 821.294V900.794L1359.5 898.794L1353.5 897.294L1347.5 898.794L1342 902.294L1337 906.794L1336 911.794V925.794L1333.5 929.294L1336 933.794L1348 1001.79L1323.5 1012.29H1321L1246.5 1037.29L1190 1048.29L1129.5 1058.29L1028.5 1068.29L945.5 1074.29L941 1080.29L862.5 1083.79L858.5 1077.79L721 1083.79H680H616H522.5Z',
    },
  },
  {
    id: 'workbench',
    label: '工作台',
    hint: '任务、仓库、分支、commit 和 PR',
    routeName: 'adventurer-workbench',
    left: 64.3,
    top: 18,
    width: 14.5,
    height: 53,
    shape: {
      x: 4011,
      y: 350,
      width: 894,
      height: 1002,
      viewBoxWidth: 897,
      viewBoxHeight: 1004,
      path: 'M1 655.5L5 649.5H25.5L30.5 653.5L48.5 652.5C49.3333 651.167 51 648.4 51 648C51 647.6 52.6667 644.5 53.5 643L62 637.5L69.5 635L78.5 629.5L89.5 616H96.5V608H80L68 620.5L63.5 623L69.5 611.5V603.5L68 599.5L55.5 602.5L51 603.5L41 611.5L38 608L47 599.5L51 593.5H56.5L58.5 588.5L62 580L66 586L73.5 575.5L75.5 577.5H80L84 580H95L101.5 584L104.5 575.5L98.5 561.5L91.5 557L78.5 550.5L62 554.5L78.5 537.5H104.5L120.5 550.5V524.5L101.5 512V507.5L120.5 512V493.5V295V124V88L115 82L109 85L110.5 75.5L104 69V61.5L141 50.5H146.5H151.5L273.5 28L278 17L302.5 11L309.5 13V33.5L311 43.5V53V69L354 61.5L510 31.5L532.5 24.5L590.5 8.5L603 2.5L620 1H633L718 22.5L751 19L757.5 13L797.5 7V22.5L799.5 26.5L797.5 33.5L799.5 38L797.5 43V194.5L873 189L879 185.5H889L895 192.5L889 199.5H879V426L831 478.5L797 441.5V673L811.5 694L836 698L758.5 717V747.5V966.5L745 970L735 977L725 985.5L718 994L710.5 1003H702.5L694 996L683 994H674.5L574.5 974L554 982.5H551.5L499 970L484 893.5L485 887.5V869V850L482.5 846L473 842L461.5 840L450 838.5L439 837L428 835.5H413H395.5L381 837L367 838.5L354.5 842L346 847.5V864L342 871.5V884.5L349.5 890.5V896L345 901.5L346 907L342 913V920L333 922L324.5 926L308.5 925L303.5 918.5L305 912L316.5 913L320.5 907L308.5 893.5L314.5 889V882.5L310.5 878L305 874L295.5 876V867L302 864L299 858.5L305 854L313 850L310.5 845H302L298.5 842H286.5V835.5C287.5 833.833 289.7 830.5 290.5 830.5C291.3 830.5 292.5 825.5 293 823L288.5 820L281.5 821.5L274 826L262 821.5L277 808.5L273 803L256 815L255 801.5L249.5 799L245.5 800.5L234.5 817.5L232.5 810.5H226V817.5L232.5 824.5L229.5 827.5V835.5L221.5 827.5L215.5 823L204.5 817.5L198 815L195.5 819L200.5 824.5L209.5 835.5L205.5 838.5L192 832.5L184.5 830.5L186 835.5L205.5 845L200.5 847.5L192 857L186 859L179.5 854L168 857L161 863H146L141.5 860.5H128.5L124.5 863L118 860.5L108.5 859L103 852.5L98.5 850H88C86.8 850 86.1667 848.333 86 847.5H77L73.5 843.5H61L41.5 838.5L37.5 832.5L36 823V815L40 807L45.5 803V683L5 679L3.5 676V662L1 660V655.5Z',
    },
  },
  {
    id: 'leaderboard',
    label: '排行榜墙',
    hint: '展示成长记录和阶段性成果',
    routeName: 'leaderboard',
    left: 82.2,
    top: 17,
    width: 11.4,
    height: 56,
    shape: {
      x: 4918,
      y: 155.5,
      width: 669.5,
      height: 1202.5,
      viewBoxWidth: 672,
      viewBoxHeight: 1205,
      path: 'M118 1180L137 1172.5L298.5 1203.5H304.5L305 1194.5L302 1185.5V1178.5L305 1172.5L308 1166L313.5 1165L316 1162V1156.5L323.5 1155H338L353 1154L364.5 1148L372 1144L382 1139.5H391.5L395.5 1137.5L406 1132.5L413 1129.5L520 1156.5L521.5 1151.5L525.5 1146L530.5 1141L536 1135L540.5 1132L545 1134L594 1137L595.5 1132L599 1126.5L604.5 1121L611 1115.5L619.5 1112V1019L609 1009L596 993.5L590 983L583.5 973L578 960L575 951L572 940.5L570.5 929V918.5V907L572 897.5L575 889L580 878.5L584.5 871L589 862.5L595 855L600 848L606 842.5L615.5 835L623 830.5L629.5 826.5L636.5 824V224L650.5 215L667 203V195.5L670.5 190V182L668.5 174.5L661 167.5V161.5L657.5 156.5L641 152.5H622L611 159.5L596 161.5C594.333 156.667 590.9 146.6 590.5 145C590.1 143.4 585 131.333 582.5 125.5L570.5 105.5L557 95.5L542 91.5L523.5 89L514 91.5L507 94L505 86L498 72.5L488 59L480 49L469 37.5L455.5 28L440.5 18.5L429 13L413 7L396.5 3.5L382.5 1H363.5H348.5L335 2L322.5 5L310.5 9L299 13L287 18.5L274 26L261 35L249.5 44L240 53L231.5 61.5L224.5 69.5L217 79L210 89L205 98L200.5 104L195.5 112.5L189.5 123L186.5 131.5V141.5L182.5 142.5L178 150L172 147.5H163L153.5 150L145 154.5L137.5 159.5L129.5 165.5L125 170L122.5 175.5L117.5 183.5L112 194L107 204L104.5 214V224.5L101 225.5L98 246.5L76.5 248L67.5 250L33.5 252.5L29.5 256.5L43.5 278.5V284.5L57 301V355L50.5 360.5L43.5 374L46 382.5L36.5 396.5L27.5 401V410L34 415V922H25L21 928V947.5L28 953V957.5L30.5 961V1052V1065.5L25 1067.5V1072.5H19.5L15.5 1078V1099H10.5L1 1104.5V1108.5V1170L8.5 1177.5H17L23 1180H32.5L42 1183L56.5 1185.5L66.5 1190H85L87.5 1185.5L94 1180H109H118Z',
    },
  },
]

const rooms = computed(() =>
  baseRooms.map((room) =>
    room.id === 'workbench'
      ? {
          ...room,
          routeName: sessionStore.role === 'MAINTAINER' ? 'maintainer-workbench' : 'adventurer-workbench',
          hint:
            sessionStore.role === 'MAINTAINER'
              ? '接取任务、提交成果，并进入成果审核台'
              : room.hint,
        }
      : room,
  ),
)
const shapeRooms = computed(() => rooms.value.filter((room) => room.shape))
const rectangleRooms = computed(() => rooms.value.filter((room) => !room.shape))

function shapeTransform(shape) {
  return `translate(${shape.x} ${shape.y}) scale(${shape.width / shape.viewBoxWidth} ${shape.height / shape.viewBoxHeight})`
}

function moveShapeTooltip(event) {
  const track = hallTrack.value
  if (!track) return
  const rect = track.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  shapeTooltipPosition.value = {
    x,
    y,
    below: y < 120,
  }
}

function focusShapeHotspot(room) {
  activeHotspotId.value = room.id
  shapeTooltipPosition.value = {
    x: room.shape.x + room.shape.width / 2,
    y: Math.max(120, room.shape.y),
    below: false,
  }
}

function shapeTooltipStyle() {
  return {
    left: `${shapeTooltipPosition.value.x}px`,
    top: `${shapeTooltipPosition.value.y}px`,
  }
}

function clampHallOffset(offset) {
  const viewport = hallViewport.value
  const track = hallTrack.value
  if (!viewport || !track) return offset
  const min = Math.min(0, viewport.clientWidth - track.offsetWidth)
  return Math.max(min, Math.min(0, offset))
}

function centerHall() {
  const viewport = hallViewport.value
  const track = hallTrack.value
  if (!viewport || !track) return
  hallOffset.value = clampHallOffset((viewport.clientWidth - track.offsetWidth) / 2)
}

function finishHallEntryIfReady() {
  if (!showHallEntry.value || isHallEntryLeaving.value || !hallImageReady.value) return

  if (!shouldFadeHallEntry.value) {
    showHallEntry.value = false
    nextTick(centerHall)
    return
  }

  isHallEntryLeaving.value = true
  window.clearTimeout(hallEntryExitTimer)
  hallEntryExitTimer = window.setTimeout(() => {
    showHallEntry.value = false
    nextTick(centerHall)
  }, HALL_ENTRY_EXIT_MS)
}

function markHallImageReady() {
  hallImageReady.value = true
  nextTick(centerHall)
  finishHallEntryIfReady()
}

function markHallImageUnavailable() {
  hallEntryStatusText.value = '公会大厅加载失败，请刷新后重试'
}

function consumeHallEntrySource() {
  try {
    shouldFadeHallEntry.value = window.sessionStorage.getItem(HALL_ENTRY_FROM_GATE_KEY) === 'true'
    window.sessionStorage.removeItem(HALL_ENTRY_FROM_GATE_KEY)
  } catch {
    shouldFadeHallEntry.value = false
  }
}

function isHallHotspotTarget(target) {
  return Boolean(target?.closest?.('.hotspot, .hall-hotspot-path'))
}

function beginHallDrag(event) {
  if (isHallHotspotTarget(event.target)) return
  isDragging.value = true
  dragStartX.value = event.clientX
  dragStartOffset.value = hallOffset.value
  hallViewport.value?.setPointerCapture(event.pointerId)
}

function dragHall(event) {
  if (!isDragging.value) return
  const delta = event.clientX - dragStartX.value
  hallOffset.value = clampHallOffset(dragStartOffset.value + delta)
}

function endHallDrag(event) {
  if (!isDragging.value) return
  isDragging.value = false
  hallViewport.value?.releasePointerCapture(event.pointerId)
}

function openRoute(routeName) {
  router.push({ name: routeName })
}

function openRoom(room) {
  openRoute(room.routeName)
}

function logout() {
  clearSession()
  router.push({ name: 'login' })
}

onMounted(async () => {
  consumeHallEntrySource()
  window.addEventListener('resize', centerHall)
  nextTick(centerHall)
  try {
    const res = await questApi.list({ status: 'PUBLISHED', page: 1, size: 1 })
    openQuestCount.value = res?.data?.totalItems ?? 0
  } catch {
    openQuestCount.value = 0
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', centerHall)
  window.clearTimeout(hallEntryExitTimer)
})
</script>

<template>
  <main class="app-shell">
    <button class="help-orb" type="button" aria-label="打开 Git Guild 使用教程" @click="openRoute('help')">?</button>

    <section class="hall-scene" :class="{ 'is-entry-loading': showHallEntry, 'is-hall-ready': hallImageReady }">
      <HallEntryTransition
        v-if="showHallEntry"
        :leaving="shouldFadeHallEntry && isHallEntryLeaving"
        :status-text="hallEntryStatusText"
      />

      <div class="session-action-stack" aria-label="账号与成长入口" :aria-hidden="showHallEntry">
        <NotificationBell v-if="showNotificationBell" />
        <button class="back-orb growth-orb" type="button" aria-label="打开成长档案" @click="openRoute('profile')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 3 15 9l6 .8-4.5 4.3 1.1 6.1L12 17.2 6.4 20.2l1.1-6.1L3 9.8 9 9z" />
          </svg>
          <span>成长档案</span>
        </button>
        <button class="back-orb logout-orb" type="button" aria-label="退出登录" @click="logout">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10 7V5a2 2 0 0 1 2-2h6v18h-6a2 2 0 0 1-2-2v-2" />
            <path d="M3 12h10" />
            <path d="m6 9-3 3 3 3" />
          </svg>
          <span>退出登录</span>
        </button>
      </div>

      <div
        ref="hallViewport"
        class="hall-viewport"
        :class="{ dragging: isDragging }"
        @pointerdown="beginHallDrag"
        @pointermove="dragHall"
        @pointerup="endHallDrag"
        @pointercancel="endHallDrag"
      >
        <div ref="hallTrack" class="hall-track" :style="{ transform: `translateX(${hallOffset}px)` }">
          <img
            class="hall-image"
            :src="hallImg"
            alt="Git Guild 公会大厅"
            draggable="false"
            @load="markHallImageReady"
            @error="markHallImageUnavailable"
          />
          <svg class="hall-hotspot-map" :viewBox="`0 0 ${HALL_IMAGE_WIDTH} ${HALL_IMAGE_HEIGHT}`">
            <path
              v-for="room in shapeRooms"
              :key="room.id"
              class="hall-hotspot-path"
              role="button"
              tabindex="0"
              :aria-label="room.label"
              :d="room.shape.path"
              :transform="shapeTransform(room.shape)"
              @pointerdown.stop
              @click.stop="openRoom(room)"
              @keydown.enter.prevent="openRoom(room)"
              @keydown.space.prevent="openRoom(room)"
              @mouseenter="activeHotspotId = room.id; moveShapeTooltip($event)"
              @mousemove="moveShapeTooltip"
              @mouseleave="activeHotspotId = ''"
              @focus="focusShapeHotspot(room)"
              @blur="activeHotspotId = ''"
            />
          </svg>

          <span
            v-for="room in shapeRooms"
            :key="`${room.id}-tooltip`"
            class="tooltip shape-tooltip"
            :class="{ active: activeHotspotId === room.id, below: shapeTooltipPosition.below }"
            :style="shapeTooltipStyle()"
          >
            <strong>{{ room.label }}</strong>
            <small>{{ room.hint }}</small>
            <em v-if="room.id === 'quest'" class="tooltip-cta">{{ openQuestCount }} 份委托可接取</em>
          </span>

          <button
            v-for="room in rectangleRooms"
            :key="room.id"
            class="hotspot"
            type="button"
            :aria-label="room.label"
            :style="{ left: `${room.left}%`, top: `${room.top}%`, width: `${room.width}%`, height: `${room.height}%` }"
            @click="openRoom(room)"
          >
            <span class="tooltip">
              <strong>{{ room.label }}</strong>
              <small>{{ room.hint }}</small>
            </span>
          </button>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.growth-orb {
  border-color: rgba(255, 219, 145, 0.56);
  background:
    linear-gradient(135deg, rgba(88, 48, 18, 0.86), rgba(20, 10, 3, 0.78)),
    radial-gradient(circle at 22% 20%, rgba(255, 219, 145, 0.22), transparent 0 42%);
}

.growth-orb span {
  color: #ffe2a0;
}

.growth-orb:hover,
.growth-orb:focus-visible {
  border-color: rgba(255, 226, 160, 0.88);
  box-shadow: 0 0 26px rgba(255, 197, 89, 0.34);
}

.hall-scene.is-entry-loading .hall-viewport,
.hall-scene.is-entry-loading .session-action-stack {
  pointer-events: none;
}

.hall-scene:not(.is-hall-ready) .session-action-stack,
.hall-scene:not(.is-hall-ready) .hall-hotspot-map,
.hall-scene:not(.is-hall-ready) .hotspot,
.hall-scene:not(.is-hall-ready) .shape-tooltip {
  opacity: 0;
  visibility: hidden;
}

.session-action-stack,
.hall-hotspot-map,
.hotspot,
.shape-tooltip {
  transition: opacity 240ms ease, visibility 240ms ease;
}

.hall-hotspot-map {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 2;
  width: calc(100svh * 10 / 3);
  height: 100svh;
  overflow: visible;
  pointer-events: none;
}

.hall-hotspot-path {
  fill: rgba(255, 220, 132, 0.04);
  stroke: rgba(255, 211, 116, 0.32);
  stroke-width: 2;
  vector-effect: non-scaling-stroke;
  stroke-linejoin: round;
  stroke-linecap: round;
  filter: drop-shadow(0 0 5px rgba(255, 190, 82, 0.18));
  pointer-events: all;
  cursor: pointer;
  transition: stroke 220ms ease, fill 220ms ease, filter 220ms ease;
}

.hall-hotspot-path:hover,
.hall-hotspot-path:focus-visible {
  fill: rgba(255, 220, 132, 0.12);
  stroke: rgba(255, 217, 128, 0.92);
  filter: drop-shadow(0 0 10px rgba(255, 190, 82, 0.55));
  outline: none;
}

.shape-tooltip {
  top: auto;
  bottom: auto;
  z-index: 3;
  pointer-events: none;
  transform: translate(16px, calc(-100% - 14px));
}

.shape-tooltip.active {
  opacity: 1;
  visibility: visible;
  transform: translate(18px, calc(-100% - 16px));
}

.shape-tooltip.below {
  transform: translate(16px, 16px);
}

.shape-tooltip.below.active {
  transform: translate(18px, 18px);
}

.tooltip-cta {
  margin-top: 4px;
  padding-top: 7px;
  border-top: 1px solid rgba(244, 190, 92, 0.32);
  color: #ffd98a;
  font-size: 0.8rem;
  font-style: normal;
  font-weight: 700;
}

</style>
