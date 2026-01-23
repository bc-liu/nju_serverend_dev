import axios from 'axios'

const service = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 30000
})

function hasToken() {
    const t = sessionStorage.getItem('token');
    return t != null && t !== '';
}

service.interceptors.request.use(
    config => {
        if(hasToken()) {
            config.headers['token'] = sessionStorage.getItem('token')
        }
        return  config
    },
    error => {
        console.log(error);
        return Promise.reject() ;
    }
)

service.interceptors.response.use(
    response => {
        if (response.status === 200) {
            return response ;
        } else {
            return Promise.reject() ;
        }
    },
    error => {
        console.log(error);
        return Promise.reject() ;
    }
)

export {
    service as axios
}
