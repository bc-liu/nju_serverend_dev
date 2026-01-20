import { router } from '../router'

/**
 * 支付工具类
 */
export class PaymentUtils {
  /**
   * 跳转到支付成功页面
   * @param orderId 订单号
   * @param amount 支付金额
   */
  static goToSuccess(orderId: string, amount: string) {
    router.push({
      name: 'paymentSuccess',
      query: {
        orderId,
        amount
      }
    })
  }

  /**
   * 跳转到支付失败页面
   * @param orderId 订单号
   * @param error 错误信息
   */
  static goToFailed(orderId: string, error: string) {
    router.push({
      name: 'paymentFailed',
      query: {
        orderId,
        error: encodeURIComponent(error)
      }
    })
  }

  /**
   * 模拟支付处理
   * @param orderId 订单号
   * @param amount 支付金额
   * @returns Promise<boolean> 支付是否成功
   */
  static async processPayment(orderId: string, amount: string): Promise<boolean> {
    return new Promise((resolve) => {
      // 模拟支付处理时间
      setTimeout(() => {
        // 模拟90%的成功率
        const isSuccess = Math.random() > 0.1
        
        if (isSuccess) {
          this.goToSuccess(orderId, amount)
        } else {
          this.goToFailed(orderId, '支付超时，请重试')
        }
        
        resolve(isSuccess)
      }, 2000)
    })
  }
}

/**
 * 支付状态枚举
 */
export enum PaymentStatus {
  PENDING = 'pending',
  SUCCESS = 'success',
  FAILED = 'failed',
  CANCELLED = 'cancelled'
}

/**
 * 支付方式枚举
 */
export enum PaymentMethod {
  ALIPAY = 'alipay',
  WECHAT = 'wechat',
  BANK_CARD = 'bank_card',
  BALANCE = 'balance'
} 