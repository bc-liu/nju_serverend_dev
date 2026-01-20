import {ADVERTISEMENT_MODULE} from "../api/_prefix";
import {axios} from "../utils/request.ts";
import {Advertisement} from "../type.ts";

type updateInfo = Advertisement
type createInfo = Advertisement


export const getAd = () => {
    return axios.get(`${ADVERTISEMENT_MODULE}`)
        .then(res => {
            return res
        })
}

export const updateAd = (inf: updateInfo) => {
    return axios.put(`${ADVERTISEMENT_MODULE}`,inf,
        {headers: {'Content-Type': 'application/json'}})
        .then(res => {
            return res
        })
}

export const createAd = (inf: createInfo) => {
    return axios.post(`${ADVERTISEMENT_MODULE}`,inf,
        {headers: {'Content-Type': 'application/json'}})
        .then(res => {
            return res
        })
}


export const delAd = (id: number) => {
    return axios.delete(`${ADVERTISEMENT_MODULE}/${id}`)
        .then(res => {
            return res
        })
}

