import AdminConsoleLayout from '../pages/admin/AdminConsoleLayout.vue'
import AdminExceptionsPage from '../pages/admin/AdminExceptionsPage.vue'
import AdminReviewPage from '../pages/admin/AdminReviewPage.vue'
import AdminTaxonomyPage from '../pages/admin/AdminTaxonomyPage.vue'
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
    component: AdminConsoleLayout,
    children: [
      { path: '', redirect: { name: 'admin-review' } },
      { path: 'review', name: 'admin-review', component: AdminReviewPage },
      { path: 'exceptions', name: 'admin-exceptions', component: AdminExceptionsPage },
      { path: 'taxonomy', name: 'admin-taxonomy', component: AdminTaxonomyPage },
    ],
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
