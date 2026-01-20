import {PRODUCT_MODULE} from "../api/_prefix";
import {axios} from "../utils/request.ts";
import {Product, Specification, StockPile} from "../type";

export type updateProductInfo = Product

export type addProductInfo = Product


export type addStock = StockPile
export type updateStock = StockPile

export const getAllProductInfo = () => {
    return axios.get(`${PRODUCT_MODULE}`)
        .then(res => {
            return res
        })
}

export const getProductInfo = (id: number) => {
    return axios.get(`${PRODUCT_MODULE}/${id}`)
        .then(res => {
            return res
        })
}

export const updateProductInfo = (productInfo: updateProductInfo) => {
    return axios.put(`${PRODUCT_MODULE}`, productInfo,
        {headers: {'Content-Type': 'application/json'}})
        .then(res => {
            return res
        })
}

export const addProductInfo = (productInfo: addProductInfo) => {
    return axios.post(`${PRODUCT_MODULE}`, productInfo,
        {headers: {'Content-Type': 'application/json'}})
        .then(res => {
            return res
        })
}

export const delProductInfo = (id: number) => {
    return axios.delete(`${PRODUCT_MODULE}/${id}`)
        .then(res => {
            return res
        })
}


export const updateStockpile = (productId: number, updateStock: updateStock) => {
    return axios.patch(`${PRODUCT_MODULE}/stockpile/${productId}`,updateStock,
        {headers: {'Content-Type': 'application/json'}}).then(res => {
            return res
        })
}


export const getStockpile = (productId: number) => {
    return axios.get(`${PRODUCT_MODULE}/stockpile/${productId}`)
        .then(res => {
            return res
        })
}

export const addStockPile = (addStock: addStock) => {
    return axios.post(`${PRODUCT_MODULE}/stockpile`, addStock,
        {headers: {'Content-Type': 'application/json'}})
        .then(res => {
            return res
        })
}

export const getMyProducts = () => {
    return axios.get(`${PRODUCT_MODULE}/myProduct`).then(res => {
        return res;
    })
}





