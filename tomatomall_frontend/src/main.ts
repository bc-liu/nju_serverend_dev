import { createApp } from 'vue';
import { createPinia } from 'pinia'
import App from './App.vue';

import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import {router} from "./router";
import axios from 'axios';

const app = createApp(App);
const pinia = createPinia()

//设置后端地址（本地或服务器），会将请求转发到后端端口
axios.defaults.baseURL = ("http://localhost:8080")
axios.defaults.timeout = 30000;

app.use(router);
app.use(ElementPlus);
app.use(pinia)

app.mount('#app');
