import {createRouter, createWebHistory} from 'vue-router';
import Home from '../views/user/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import ProductDetail from '../views/product/ProductDetail.vue'
import Cart from '../views/user/Cart.vue'
import Profile from '../views/user/Profile.vue'
import MyBooks from '../views/user/MyBooks.vue'
import Advertisement from '../views/user/Advertisement.vue'
import Ranking from '../views/user/Ranking.vue'
import PendingOrder from "../views/user/PendingOrder.vue";
import PaymentSuccess from "../views/user/PaymentSuccess.vue";
import PaymentFailed from "../views/user/PaymentFailed.vue";

const router = createRouter({
    history: createWebHistory(),
    routes: [{
        path: '/',
        name: 'home',
        component: Home
    }, {
        path: '/login',
        name: 'login',
        component: Login
    }, {
        path: '/register',
        name: 'register',
        component: Register
    }, {
        path: '/products/:id',
        name: 'product-detail',
        component: ProductDetail
    }, {
        path: '/user/cart',
        name: 'cart',
        component: Cart,
        meta: {
            requiresAuth: true,
            role: 'USER'
        }
    }, {
        path: '/user/profile',
        name: 'profile',
        component: Profile,
        meta: {
            requiresAuth: true
        }
    }, {
        path: '/user/MyBooks',
        name: 'my-books',
        component: MyBooks,
        meta: {
            requiresAuth: true,
            role: 'USER'
        }
    }, {
        path: '/user/advertisement',
        name: 'advertisement',
        component: Advertisement,
        meta: {
            requiresAuth: true,
            role: 'ADMIN'
        }
    }, {
        path: '/user/ranking',
        name: 'ranking',
        component: Ranking
    }, {
        path: '/home',
        component: () => import('../views/user/Home.vue'),
        meta: {title: 'home页'}
    }, {
        path: '/404',
        name: '404',
        component: () => import('../views/NotFound.vue'),
        meta: {title: '404'}
    }, {
        path: '/admin/home',
        name: 'adminHome',
        component: () => import('../views/admin/Home.vue'),
        meta: {title: 'adminHome'}
    }, {
        path: '/user/pendingOrders',
        name: 'pendingOrders',
        component: PendingOrder,
    }, {
        path: '/payment/success',
        name: 'paymentSuccess',
        component: PaymentSuccess,
        meta: {
            requiresAuth: true,
            title: '支付成功'
        }
    }]
})

router.beforeEach((to, _, next) => {
    const token: string | null = sessionStorage.getItem('token');
    const role: string | null = sessionStorage.getItem('role')

    if (to.meta.title) {
        document.title = to.meta.title
    }

    if (token) {
        if (to.meta.permission) {
            if (to.meta.permission.includes(role!)) {
                next()
            } else {
                next('/404')
            }
        } else {
            next()
        }
    } else {
        if (to.path === '/login') {
            next();
        } else if (to.path === '/register') {
            next()
        } else if(to.path === '/payment/success') {
            next()
        }
        else {
            next('/login')
        }
    }
})

export {router};
