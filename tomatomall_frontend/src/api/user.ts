import {ACCOUNT_MODULE} from "./_prefix";
import {axios} from "../utils/request";

type LoginInfo = {
    username: string,
    password: string
}

type RegisterInfo = {
    username: string,
    password: string
    name: string,
    avatar: string,
    role: string,
    telephone: string,
    email: string,
    location: string,
}

type UpdateInfo = {
    username: string,
    password?: string
    name?: string,
    avatar?: string,
    role?: string,
    telephone?: string,
    email?: string,
    location?: string,
}

type UpdatePasswordInfo = {
    oldPassword: string,
    newPassword: string,
}

export const userLogin = (loginInfo: LoginInfo) => {
    return axios.post(`${ACCOUNT_MODULE}/login`,loginInfo,
        {headers: {'Content-Type': 'application/json'}})
        .then(res => {
            return res
        })
}

// 用户注册
export const userRegister = (registerInfo: RegisterInfo) => {
    return axios.post(`${ACCOUNT_MODULE}`, registerInfo,
        {headers: {'Content-Type': 'application/json'}})
        .then(res => {
            return res
        })
}


// 获取用户详情
export const userInfo = (username: string) => {
    return axios.get(`${ACCOUNT_MODULE}/${username}`)
        .then(res => {
            return res
        })
}

// 更新用户信息
export const userInfoUpdate = (updateInfo: UpdateInfo) => {
    return axios.put(`${ACCOUNT_MODULE}`, updateInfo, {headers: {'Content-Type': 'application/json'}})
        .then(res => {
            return res
        })
}

export const updatePassword = (updatePassword: UpdatePasswordInfo) => {
    return axios.put(`${ACCOUNT_MODULE}/updatePassword`, updatePassword, {headers: {'Content-Type': 'application/json'}})
}