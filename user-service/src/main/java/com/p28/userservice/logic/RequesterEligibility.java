package com.p28.userservice.logic;

public class RequesterEligibility {
    private boolean isRequesterEligible;

    public RequesterEligibility() {}
    
    public RequesterEligibility(boolean isRequesterEligible) {
        this.isRequesterEligible = isRequesterEligible;
    }

    // Getter
    public boolean getIsRequesterEligible() {
        return this.isRequesterEligible;
    }

    // Setter
    public void setIsSupplierElgibile(boolean isRequesterEligible) {
        this.isRequesterEligible = isRequesterEligible;
        return;
    }
}
