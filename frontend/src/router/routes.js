import AdminReviewPage from '../pages/admin/AdminReviewPage.vue'
import LoginPage from '../pages/auth/LoginPage.vue'
import GrowthProfilePage from '../pages/growth/GrowthProfilePage.vue'
import GuildHallPage from '../pages/hall/GuildHallPage.vue'
import HelpPage from '../pages/help/HelpPage.vue'
import LeaderboardPage from '../pages/leaderboard/LeaderboardPage.vue'
import ProfilePage from '../pages/profile/ProfilePage.vue'
import QuestBoardPage from '../pages/quests/QuestBoardPage.vue'
import QuestDetailPage from '../pages/quests/QuestDetailPage.vue'
import RepositorySyncPage from '../pages/repositories/RepositorySyncPage.vue'
import SubmissionCounterPage from '../pages/submissions/SubmissionCounterPage.vue'
import AdventurerWorkbenchPage from '../pages/workbench/AdventurerWorkbenchPage.vue'
import MaintainerWorkbenchPage from '../pages/workbench/MaintainerWorkbenchPage.vue'

export const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'login',
    component: LoginPage,
  },
  {
    path: '/hall',
    name: 'hall',
    component: GuildHallPage,
  },
  {
    path: '/quests',
    name: 'quest-board',
    component: QuestBoardPage,
  },
  {
    path: '/quests/:questId',
    name: 'quest-detail',
    component: QuestDetailPage,
  },
  {
    path: '/repositories/sync',
    name: 'repository-sync',
    component: RepositorySyncPage,
  },
  {
    path: '/workbench',
    name: 'adventurer-workbench',
    component: AdventurerWorkbenchPage,
  },
  {
    path: '/maintainer',
    name: 'maintainer-workbench',
    component: MaintainerWorkbenchPage,
  },
  {
    path: '/submissions',
    name: 'submission-counter',
    component: SubmissionCounterPage,
  },
  {
    path: '/admin',
    name: 'admin-review',
    component: AdminReviewPage,
  },
  {
    path: '/growth',
    name: 'growth-profile',
    component: GrowthProfilePage,
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfilePage,
  },
  {
    path: '/leaderboard',
    name: 'leaderboard',
    component: LeaderboardPage,
  },
  {
    path: '/help',
    name: 'help',
    component: HelpPage,
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
]
