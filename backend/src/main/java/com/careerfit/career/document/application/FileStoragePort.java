package com.careerfit.career.document.application;

public interface FileStoragePort {

    void store(String storageReference, byte[] content);

    byte[] read(String storageReference);

    void delete(String storageReference);
}
