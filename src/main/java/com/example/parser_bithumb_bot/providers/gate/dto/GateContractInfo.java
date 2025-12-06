package com.example.parser_bithumb_bot.providers.gate.dto;

public class GateContractInfo {
    private String name;
    private double orderSizeMin;
    private double orderSizeStep;
    private double contractMultiplier;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getOrderSizeMin() {
        return orderSizeMin;
    }

    public void setOrderSizeMin(double orderSizeMin) {
        this.orderSizeMin = orderSizeMin;
    }

    public double getOrderSizeStep() {
        return orderSizeStep;
    }

    public void setOrderSizeStep(double orderSizeStep) {
        this.orderSizeStep = orderSizeStep;
    }

    public double getContractMultiplier() {
        return contractMultiplier;
    }

    public void setContractMultiplier(double contractMultiplier) {
        this.contractMultiplier = contractMultiplier;
    }
}