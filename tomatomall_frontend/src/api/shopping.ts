import {SHOPPING_MODULE} from "../api/_prefix";
import {axios} from "../utils/request.ts";


type updateCartItemQuantity = {
    quantity: number,
}

export const addProductToCart = (productId: number, quantity: number) => {
    return axios.post(`${SHOPPING_MODULE}`, null, {
        params: {productId, quantity }
    })
        .then(res => {
            return res
        })
}

export const deleteCartItemById = (cartItemId: number) => {
    return axios.delete(`${SHOPPING_MODULE}/${cartItemId}`)
        .then(res => {
            return res;
        })
}

export const updateQuantity = (cartItemId: number, quantity: updateCartItemQuantity) => {
    return axios.patch(`${SHOPPING_MODULE}/${cartItemId}`,quantity,
        {headers: {'Content-Type': 'application/json'}})
        .then(res => {
            return res
        })
}


export const getCartList = () => {
    return axios.get(`${SHOPPING_MODULE}`)
        .then(res => {
            return res;
        })
}

export const submitOrder = (cartItemId: number[], shoppingAddress: string, paymentMethod: string) => {
    const formData = new URLSearchParams();
    cartItemId.forEach(id => formData.append('cartItemId', id));
    formData.append('shoppingAddress', shoppingAddress);
    formData.append('paymentMethod', paymentMethod);
    return axios.post(`${SHOPPING_MODULE}/checkout`, formData, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' }})
        .then(res => {
            return res
        })
}




