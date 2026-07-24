package com.careerfit.identity;

public class ResourceOwnershipException extends RuntimeException {

    public ResourceOwnershipException() {
        super("현재 사용자가 소유한 리소스가 아닙니다.");
    }
}
