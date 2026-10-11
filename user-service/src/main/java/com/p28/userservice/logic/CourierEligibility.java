package com.p28.userservice.logic;

public class CourierEligibility {
    private boolean isCourierEligible;

    public CourierEligibility() {}
    
    public CourierEligibility(boolean isCourierEligible) {
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
