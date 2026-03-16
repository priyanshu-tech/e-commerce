package com.example.demo.service.impl;

import com.example.demo.service.PaymentService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.payment.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Override
    public PaymentVO processPayment(PaymentVO paymentVO) {
        LogUtils.info(log, "Processing payment", paymentVO);
        LogUtils.info(log, "Payment processed", paymentVO);
        return paymentVO;
    }

    @Override
    public PaymentVO getPaymentById(Long paymentId) {
        log.info("Getting payment by paymentId: {}", paymentId);
        PaymentVO result = PaymentVO.builder().paymentId(paymentId).build();
        LogUtils.info(log, "Fetched payment", result);
        return result;
    }

    @Override
    public PaymentVO getPaymentByOrderId(Long orderId) {
        log.info("Getting payment for orderId: {}", orderId);
        PaymentVO result = PaymentVO.builder().orderId(orderId).build();
        LogUtils.info(log, "Fetched payment by orderId", result);
        return result;
    }

    @Override
    public PaymentVO refundPayment(Long paymentId) {
        log.info("Refunding paymentId: {}", paymentId);
        PaymentVO result = PaymentVO.builder().paymentId(paymentId).status("REFUNDED").build();
        LogUtils.info(log, "Payment refunded", result);
        return result;
    }
}
