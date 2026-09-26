package com.p28.userservice.logic;

public class CourierElgibility {
    private boolean isCourierEligible;

    public CourierElgibility(boolean isCourierEligible) {
        this.isCourierEligible = isCourierEligible;
    }

    // Getter
    public boolean getIsCourierEligible() {
        return this.isCourierEligible;
    }

    // Setter
    public void setIsCourierEligible(boolean isCourierEligible) {
        this.isCourierEligible = isCourierEligible;
        return;
    }
}
