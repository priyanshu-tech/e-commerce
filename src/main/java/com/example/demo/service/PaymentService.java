package com.example.demo.service;

import com.example.demo.vo.payment.PaymentVO;

import java.util.Map;

public interface PaymentService {

    PaymentVO createOrder(String orderNumber, String currency);

    PaymentVO verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature);

    PaymentVO getPaymentByOrderNumber(String orderNumber);

    Map<String, String> refundPayment(String orderNumber);
}
