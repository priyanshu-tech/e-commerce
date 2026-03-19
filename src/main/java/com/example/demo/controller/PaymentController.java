package com.example.demo.controller;

import com.example.demo.service.PaymentService;
import com.example.demo.vo.payment.PaymentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentVO createOrder(@RequestParam String orderNumber,
                                 @RequestParam(defaultValue = "INR") String currency) {
        return paymentService.createOrder(orderNumber, currency);
    }

    @PostMapping("/verify")
    public PaymentVO verifyPayment(@RequestParam String razorpayOrderId,
                                   @RequestParam String razorpayPaymentId,
                                   @RequestParam String razorpaySignature) {
        return paymentService.verifyPayment(razorpayOrderId, razorpayPaymentId, razorpaySignature);
    }

    @GetMapping("/{orderNumber}")
    public PaymentVO getPayment(@PathVariable String orderNumber) {
        return paymentService.getPaymentByOrderNumber(orderNumber);
    }

    @PostMapping("/{orderNumber}/refund")
    public Map<String, String> refundPayment(@PathVariable String orderNumber) {
        return paymentService.refundPayment(orderNumber);
    }
}
