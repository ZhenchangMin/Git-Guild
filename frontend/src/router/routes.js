import AdminConsoleLayout from '../pages/admin/AdminConsoleLayout.vue'
import AdminExceptionsPage from '../pages/admin/AdminExceptionsPage.vue'
import AdminReviewPage from '../pages/admin/AdminReviewPage.vue'
import AdminTaxonomyPage from '../pages/admin/AdminTaxonomyPage.vue'
import LoginPage from '../pages/auth/LoginPage.vue'
import GuildHallPage from '../pages/hall/GuildHallPage.vue'
import HelpPage from '../pages/help/HelpPage.vue'
import LeaderboardPage from '../pages/leaderboard/LeaderboardPage.vue'
import ProfilePage from '../pages/profile/ProfilePage.vue'
import QuestBoardPage from '../pages/quests/QuestBoardPage.vue'
import QuestDetailPage from '../pages/quests/QuestDetailPage.vue'
import RepositorySyncPage from '../pages/repositories/RepositorySyncPage.vue'
import SubmissionCounterPage from '../pages/submissions/SubmissionCounterPage.vue'
import AdventurerWorkbenchPage from '../pages/workbench/AdventurerWorkbenchPage.vue'
import MaintainerPublishPage from '../pages/workbench/MaintainerPublishPage.vue'
import MaintainerReviewPage from '../pages/workbench/MaintainerReviewPage.vue'
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
    meta: { requiresAuth: true, roles: ['MAINTAINER', 'ADMIN'] },
  },
  {
    path: '/workbench',
    name: 'adventurer-workbench',
    component: AdventurerWorkbenchPage,
    meta: { requiresAuth: true, roles: ['ADVENTURER'] },
  },
  {
    path: '/maintainer',
    name: 'maintainer-workbench',
    component: MaintainerWorkbenchPage,
    meta: { requiresAuth: true, roles: ['MAINTAINER'] },
  },
  {
    path: '/maintainer/publish',
    name: 'maintainer-publish',
    component: MaintainerPublishPage,
    meta: { requiresAuth: true, roles: ['MAINTAINER'] },
  },
  {
    path: '/maintainer/reviews',
    name: 'maintainer-review',
    component: MaintainerReviewPage,
    meta: { requiresAuth: true, roles: ['MAINTAINER'] },
  },
  {
    path: '/submissions',
    name: 'submission-counter',
    component: SubmissionCounterPage,
    meta: { requiresAuth: true, roles: ['ADVENTURER', 'MAINTAINER'] },
  },
  {
    // The admin console uses a nested layout: AdminConsoleLayout renders the
    // shared rail + <router-view>, and each child route names itself. The
    // parent must NOT carry `name: 'admin-review'` — Vue Router 4 throws if a
    // nested route reuses an ancestor's name, which would prevent the whole
    // router from initializing and blank-screen the app.
    path: '/admin',
    component: AdminConsoleLayout,
    meta: { requiresAuth: true, roles: ['ADMIN'] },
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
    redirect: { name: 'profile' },
    meta: { requiresAuth: true, roles: ['ADVENTURER', 'MAINTAINER', 'ADMIN'] },
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfilePage,
    meta: { requiresAuth: true, roles: ['ADVENTURER', 'MAINTAINER', 'ADMIN'] },
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
